package com.jecstar.etm.launcher.http;

import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Iterator;

import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.get.MultiGetItemResponse;
import org.elasticsearch.action.get.MultiGetRequestBuilder;
import org.elasticsearch.action.get.MultiGetResponse;
import org.elasticsearch.client.Client;

import com.jecstar.etm.server.core.configuration.ElasticSearchLayout;
import com.jecstar.etm.server.core.domain.EtmGroup;
import com.jecstar.etm.server.core.domain.EtmPrincipal;
import com.jecstar.etm.server.core.domain.converter.EtmPrincipalConverter;
import com.jecstar.etm.server.core.domain.converter.EtmPrincipalTags;
import com.jecstar.etm.server.core.domain.converter.json.EtmPrincipalConverterJsonImpl;
import com.jecstar.etm.server.core.domain.converter.json.JsonConverter;
import com.jecstar.etm.server.core.logging.LogFactory;
import com.jecstar.etm.server.core.logging.LogWrapper;
import com.jecstar.etm.server.core.util.BCrypt;

import io.undertow.security.idm.Account;
import io.undertow.security.idm.Credential;
import io.undertow.security.idm.IdentityManager;
import io.undertow.security.idm.PasswordCredential;
import io.undertow.security.idm.X509CertificateCredential;

public class ElasticsearchIdentityManager implements IdentityManager {

	/**
	 * The <code>LogWrapper</code> for this class.
	 */
	private static final LogWrapper log = LogFactory.getLogger(ElasticsearchIdentityManager.class);
	
	private final Client client;
	private final EtmPrincipalConverter<String> etmPrincipalConverter = new EtmPrincipalConverterJsonImpl();
	private final EtmPrincipalTags etmPrincipalTags = etmPrincipalConverter.getTags();
	private final JsonConverter jsonConverter = new JsonConverter();
	
	public ElasticsearchIdentityManager(Client client) {
		this.client = client;
	}

	@Override
	public Account verify(Account account) {
		EtmAccount etmAccount = (EtmAccount) account;
		if (System.currentTimeMillis() - etmAccount.getLastUpdated() > 60000 || etmAccount.getPrincipal().forceReload) {
			EtmPrincipal principal = loadPrincipal(etmAccount.getPrincipal().getId());
			if (principal == null) {
				if (log.isDebugLevelEnabled()) {
					log.logDebugMessage("Account with id '" + etmAccount.getPrincipal().getId() + "' not found. Account will be invalidated.");
				}
				return null;
			}
			etmAccount = new EtmAccount(principal);
		}
		return etmAccount;
	}

	@Override
	public Account verify(String id, Credential credential) {
		EtmPrincipal principal = loadPrincipal(id);
		if (principal == null) {
			if (log.isDebugLevelEnabled()) {
				log.logDebugMessage("Account with id '" + id + "' not found.");
			}
			return null;
		}
		if (credential instanceof PasswordCredential) {
			boolean valid = BCrypt.checkpw(new String(((PasswordCredential) credential).getPassword()), principal.getPasswordHash());
			if (!valid) {
				if (log.isDebugLevelEnabled()) {
					log.logDebugMessage("Invalid password for account with id '" + id + "'.");
				}
				return null;
			}
			EtmAccount etmAccount = new EtmAccount(principal);
			return etmAccount;
		}
		return null;
	}

	@Override
	public Account verify(Credential credential) {
		if (credential instanceof X509CertificateCredential) {
			X509Certificate certificate = ((X509CertificateCredential) credential).getCertificate();
			EtmPrincipal principal = loadPrincipal(certificate.getSerialNumber().toString());
			if (principal == null) {
				return null;
			}
			// TODO, de public key zou hier uitgelezen moeten worden, en vergeleken met hetgeen in de DB.
			boolean valid = BCrypt.checkpw(certificate.getIssuerX500Principal().getName(), principal.getPasswordHash());
			if (!valid) {
				if (log.isDebugLevelEnabled()) {
					log.logDebugMessage("Invalid password (issuer name) for certificate with serial number '" + certificate.getSerialNumber().toString() + "'.");
				}
				return null;
			}
			EtmAccount etmAccount = new EtmAccount(principal);
			return etmAccount;
		}
		return null;
	}
	
	private EtmPrincipal loadPrincipal(String userId) {
		GetResponse getResponse = this.client.prepareGet(ElasticSearchLayout.CONFIGURATION_INDEX_NAME, ElasticSearchLayout.CONFIGURATION_INDEX_TYPE_USER, userId).get();
		if (!getResponse.isExists()) {
			return null;
		}
		EtmPrincipal principal = this.etmPrincipalConverter.readPrincipal(getResponse.getSourceAsString());
		Collection<String> groups = this.jsonConverter.getArray(this.etmPrincipalTags.getGroupsTag(), getResponse.getSource());
		if (groups != null && !groups.isEmpty()) {
			MultiGetRequestBuilder multiGetBuilder = this.client.prepareMultiGet();
			for (String group : groups) {
				multiGetBuilder.add(ElasticSearchLayout.CONFIGURATION_INDEX_NAME, ElasticSearchLayout.CONFIGURATION_INDEX_TYPE_GROUP, group);
			}
			MultiGetResponse multiGetResponse = multiGetBuilder.get();
			Iterator<MultiGetItemResponse> iterator = multiGetResponse.iterator();
			while (iterator.hasNext()) {
				MultiGetItemResponse item = iterator.next();
				EtmGroup etmGroup = this.etmPrincipalConverter.readGroup(item.getResponse().getSourceAsString());
				principal.addGroup(etmGroup);
			}
		}
		return principal;
	}

}