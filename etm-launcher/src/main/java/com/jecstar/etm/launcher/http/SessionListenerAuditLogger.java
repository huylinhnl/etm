package com.jecstar.etm.launcher.http;

import com.jecstar.etm.server.core.domain.audit.builder.LogoutAuditLogBuilder;
import com.jecstar.etm.server.core.domain.audit.converter.LogoutAuditLogConverter;
import com.jecstar.etm.server.core.domain.configuration.ElasticsearchLayout;
import com.jecstar.etm.server.core.domain.configuration.EtmConfiguration;
import com.jecstar.etm.server.core.domain.principal.EtmPrincipal;
import com.jecstar.etm.server.core.util.DateUtils;
import io.undertow.security.api.AuthenticatedSessionManager.AuthenticatedSession;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.session.Session;
import io.undertow.server.session.SessionListener;
import io.undertow.servlet.handlers.security.CachedAuthenticatedSessionHandler;
import org.elasticsearch.action.support.ActiveShardCount;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

class SessionListenerAuditLogger implements SessionListener {

    private static final DateTimeFormatter dateTimeFormatterIndexPerDay = DateUtils.getIndexPerDayFormatter();

    private final Client client;
    private final EtmConfiguration etmConfiguration;
    private final LogoutAuditLogConverter auditLogConverter = new LogoutAuditLogConverter();

    public SessionListenerAuditLogger(Client client, EtmConfiguration etmConfiguration) {
        this.client = client;
        this.etmConfiguration = etmConfiguration;
    }

    private void logLogout(String id, boolean expired) {
        ZonedDateTime now = ZonedDateTime.now();
        LogoutAuditLogBuilder auditLogBuilder = new LogoutAuditLogBuilder().setTimestamp(now).setHandlingTime(now)
                .setPrincipalId(id).setExpired(expired);
        client.prepareIndex(ElasticsearchLayout.AUDIT_LOG_INDEX_PREFIX + dateTimeFormatterIndexPerDay.format(now),
                ElasticsearchLayout.ETM_DEFAULT_TYPE)
                .setWaitForActiveShards(getActiveShardCount(etmConfiguration))
                .setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()))
                .setSource(this.auditLogConverter.write(auditLogBuilder.build()), XContentType.JSON).execute();
    }

    private ActiveShardCount getActiveShardCount(EtmConfiguration etmConfiguration) {
        if (-1 == etmConfiguration.getWaitForActiveShards()) {
            return ActiveShardCount.ALL;
        } else if (0 == etmConfiguration.getWaitForActiveShards()) {
            return ActiveShardCount.NONE;
        }
        return ActiveShardCount.from(etmConfiguration.getWaitForActiveShards());
    }

    @Override
    public void sessionCreated(Session session, HttpServerExchange exchange) {
    }

    @Override
    public void sessionDestroyed(Session session, HttpServerExchange exchange, SessionDestroyedReason reason) {
        String id = null;
        if (exchange != null && exchange.getSecurityContext().getAuthenticatedAccount() != null) {
            id = ((EtmPrincipal) exchange.getSecurityContext().getAuthenticatedAccount().getPrincipal()).getId();
        } else {
            AuthenticatedSession authenticatedSession = (AuthenticatedSession) session.getAttribute(CachedAuthenticatedSessionHandler.ATTRIBUTE_NAME);
            if (authenticatedSession != null) {
                id = ((EtmPrincipal) authenticatedSession.getAccount().getPrincipal()).getId();
            }
        }
        if (id == null) {
            return;
        }
        switch (reason) {
            case INVALIDATED:
                logLogout(id, false);
                break;
            case TIMEOUT:
                logLogout(id, true);
                break;
            case UNDEPLOY:
                logLogout(id, false);
                break;
        }
    }

    @Override
    public void attributeAdded(Session session, String name, Object value) {
    }

    @Override
    public void attributeUpdated(Session session, String name, Object newValue, Object oldValue) {
    }

    @Override
    public void attributeRemoved(Session session, String name, Object oldValue) {
    }

    @Override
    public void sessionIdChanged(Session session, String oldSessionId) {
    }

}
