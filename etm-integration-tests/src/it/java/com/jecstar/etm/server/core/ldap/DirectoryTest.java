package com.jecstar.etm.server.core.ldap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.jecstar.etm.server.core.configuration.LdapConfiguration;
import com.jecstar.etm.server.core.domain.EtmGroup;
import com.jecstar.etm.server.core.domain.EtmPrincipal;

public class DirectoryTest {

	private static EmbeddableLdapServer server;
	
	@BeforeClass
	public static void setup() {
		server = new EmbeddableLdapServer();
		server.startServer();
	}
	
	@AfterClass
	public static void tearDown() {
		if (server != null) {
			server.stopServer();
		}
	}
	
	public static void main(String[] args) {
		setup();
	}
	
	
	private LdapConfiguration createLdapConfiguration() {
		LdapConfiguration ldapConfiguration = new LdapConfiguration();
		// Setup the connection.
		ldapConfiguration.setHost(EmbeddableLdapServer.HOST);
		ldapConfiguration.setPort(EmbeddableLdapServer.PORT);
		ldapConfiguration.setBindDn(EmbeddableLdapServer.BIND_DN);
		ldapConfiguration.setBindPassword(EmbeddableLdapServer.BIND_PASSWORD);
		// Setup the connection validator
		ldapConfiguration.setConnectionTestBaseDn(EmbeddableLdapServer.BIND_DN);
		ldapConfiguration.setConnectionTestSearchFilter("(objectClass=*)");
		// Setup the connection pool
		ldapConfiguration.setMinPoolSize(1);
		ldapConfiguration.setMaxPoolSize(10);
		// Setup the group filters
		ldapConfiguration.setGroupBaseDn(EmbeddableLdapServer.GROUP_BASE_DN);
		ldapConfiguration.setGroupSearchFilter("(cn={group})");
		// Setup the user filters
		ldapConfiguration.setUserBaseDn(EmbeddableLdapServer.USER_BASE_DN);
		ldapConfiguration.setUserSearchFilter("(uid={user})");
		// Setup the user attribute mapping
		ldapConfiguration.setUserIdentifierAttribute(EmbeddableLdapServer.USER_ID_ATTRIBUTE);
		ldapConfiguration.setUserFullNameAttribute(EmbeddableLdapServer.USER_NAME_ATTRIBUTE);
		ldapConfiguration.setUserEmailAttribute(EmbeddableLdapServer.USER_EMAIL_ATTRIBUTE);
		// Set the group mapping
		ldapConfiguration.setUserGroupsQueryBaseDn(EmbeddableLdapServer.GROUP_BASE_DN);
		ldapConfiguration.setUserGroupsQueryFilter("(| (member={dn}) (uniqueMember={dn}) (memberUid={uid}))");
		return ldapConfiguration;
	}
	
	@Test
	public void testAuthenticate() {
		Directory directory = new Directory(createLdapConfiguration());
		EtmPrincipal principal = directory.authenticate("etm-admin", "password");
		directory.close();
		assertEquals("ETM Administrator", principal.getName());
		assertEquals("etm-admin", principal.getId());
		assertEquals("etm-admin@localhost", principal.getEmailAddress());
		assertSame(2, principal.getGroups().size());
	}
	
	@Test
	public void testAuthenticateWithoutSubtreeSearch() {
		LdapConfiguration ldapConfig = createLdapConfiguration();
		assertFalse(ldapConfig.isUserSearchInSubtree());
		Directory directory = new Directory(ldapConfig);
		// Test user 1 is placed in user base dn and should be able to login.
		assertNotNull(directory.authenticate("test-user-1", "password"));
		// Test users 2 & 3 are a level below the base dn and should not be able to login.
		assertNull(directory.authenticate("test-user-2", "password"));
		assertNull(directory.authenticate("test-user-3", "password"));
		directory.close();
	}
	
	@Test
	public void testAuthenticateWithSubtreeSearch() {
		LdapConfiguration ldapConfig = createLdapConfiguration();
		ldapConfig.setUserSearchInSubtree(true);
		assertTrue(ldapConfig.isUserSearchInSubtree());
		Directory directory = new Directory(ldapConfig);
		// Test user 1 is placed in user base dn and should be able to login.
		assertNotNull(directory.authenticate("test-user-1", "password"));
		// Test users 2 & r are a level below the base dn and should also able to login.
		assertNotNull(directory.authenticate("test-user-2", "password"));
		assertNotNull(directory.authenticate("test-user-3", "password"));
		directory.close();
	}
	
	@Test
	public void testGetGroups() {
		Directory directory = new Directory(createLdapConfiguration());
		Set<EtmGroup> groups = directory.getGroups();
		assertEquals(2, groups.size());
		directory.close();
	}

}
