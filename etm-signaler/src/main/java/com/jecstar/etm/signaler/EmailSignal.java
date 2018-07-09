package com.jecstar.etm.signaler;

import com.jecstar.etm.server.core.EtmException;
import com.jecstar.etm.server.core.domain.cluster.notifier.Notifier;
import com.jecstar.etm.server.core.domain.configuration.ElasticsearchLayout;
import com.jecstar.etm.server.core.domain.configuration.EtmConfiguration;
import com.jecstar.etm.server.core.domain.converter.json.JsonConverter;
import com.jecstar.etm.server.core.domain.principal.EtmGroup;
import com.jecstar.etm.server.core.domain.principal.EtmPrincipal;
import com.jecstar.etm.server.core.domain.principal.converter.EtmPrincipalConverter;
import com.jecstar.etm.server.core.domain.principal.converter.EtmPrincipalTags;
import com.jecstar.etm.server.core.domain.principal.converter.json.EtmPrincipalConverterJsonImpl;
import com.jecstar.etm.server.core.logging.LogFactory;
import com.jecstar.etm.server.core.logging.LogWrapper;
import com.jecstar.etm.server.core.persisting.ScrollableSearch;
import com.jecstar.etm.signaler.domain.Signal;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.joda.time.DateTime;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.Closeable;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Class that sends an email to all recipients of a <code>Signal</code>.
 */
class EmailSignal implements Closeable {

    /**
     * The <code>LogWrapper</code> for this class.
     */
    private static final LogWrapper log = LogFactory.getLogger(EmailSignal.class);

    private final JsonConverter jsonConverter = new JsonConverter();
    private final EtmPrincipalConverter<String> etmPrincipalConverter = new EtmPrincipalConverterJsonImpl();
    private final EtmPrincipalTags etmPrincipalTags = this.etmPrincipalConverter.getTags();
    private final SimpleDateFormat defaultDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
    private final NumberFormat defaultNumberFormat = NumberFormat.getInstance();
    private final Map<String, SessionAndTransport> serverConnections = new HashMap<>();

    /**
     * Send an exceedance notification of a <code>Signal</code> by email.
     *
     * @param client               The Elasticsearch client.
     * @param etmConfiguration     The <code>EtmConfiguration</code> instance.
     * @param clusterName          The name of the ETM cluster.
     * @param signal               The <code>Signal</code> of which the threshold is exceeded more often that the configured limit.
     * @param notifier             The <code>Notifier</code> to be used to send the email.
     * @param thresholdExceedances A <code>Map</code> with dates and their values when the threshold was exceeded.
     * @param etmGroup             An <code>EtmGroup</code> to which the <code>Signal</code> belongs. <code>null</code> if the
     */
    void sendExceedanceNotification(Client client,
                                    EtmConfiguration etmConfiguration,
                                    String clusterName,
                                    Signal signal,
                                    Notifier notifier,
                                    Map<DateTime, Double> thresholdExceedances,
                                    EtmGroup etmGroup
    ) {
        try {
            SessionAndTransport sessionAndTransport = getSessionAndTransport(notifier);
            for (String recipient : determineRecipients(client, etmConfiguration, signal, etmGroup)) {
                final String subject = "[" + clusterName + "] - Signal: " + signal.getName();
                InternetAddress toAddress = new InternetAddress(recipient);
                DateFormat dateFormat = this.defaultDateFormat;
                NumberFormat numberFormat = this.defaultNumberFormat;
                EtmPrincipal etmRecipient = getUserByEmail(client, etmConfiguration, recipient);
                if (etmRecipient != null) {
                    dateFormat = etmRecipient.getISO8601DateFormat();
                    numberFormat = etmRecipient.getNumberFormat();
                    toAddress = new InternetAddress(recipient, etmRecipient.getName());
                }
                StringBuilder messageContent = new StringBuilder();
                if (etmRecipient != null) {
                    messageContent.append("Hi " + etmRecipient.getName() + ",\r\n\r\n");
                } else {
                    messageContent.append("Hi,\r\n\r\n");
                }
                messageContent.append("The threshold (" + signal.getThreshold() + ") of signal '" + signal.getName()
                        + "' has exceeded " + thresholdExceedances.size() + " times which tops the configured limit of "
                        + signal.getLimit() + ".\r\n");
                messageContent.append("\r\n");
                messageContent.append("The following exceedances are recorded:\r\n");
                ArrayList<DateTime> dateTimes = new ArrayList<>(thresholdExceedances.keySet());
                Collections.sort(dateTimes);
                for (DateTime dateTime : dateTimes) {
                    messageContent.append(dateFormat.format(dateTime.toDate()) + ": " + numberFormat.format(thresholdExceedances.get(dateTime)) + "\r\n");
                }
                messageContent.append("\r\n");
                messageContent.append("Kind regards,\r\n");
                messageContent.append("Your Enterprise Telemetry Monitor administrator");

                MimeMessage message = createMimeMessage(sessionAndTransport.session, notifier, toAddress, subject, messageContent.toString());
                sessionAndTransport.transport.sendMessage(message, message.getAllRecipients());
            }
        } catch (UnsupportedEncodingException | MessagingException e) {
            if (log.isErrorLevelEnabled()) {
                log.logErrorMessage("Unable to send signal mail.", e);
            }
        }
    }


    /**
     * Send a no longer exceedance notification of a <code>Signal</code> by email.
     *
     * @param client           The Elasticsearch client.
     * @param etmConfiguration The <code>EtmConfiguration</code> instance.
     * @param clusterName      The name of the ETM cluster.
     * @param signal           The <code>Signal</code> of which the threshold is no longer exceeded.
     * @param notifier         The <code>Notifier</code> to be used to send the email.
     * @param etmGroup         An <code>EtmGroup</code> to which the <code>Signal</code> belongs. <code>null</code> if the
     */
    public void sendNoLongerExceededNotification(Client client,
                                                 EtmConfiguration etmConfiguration,
                                                 String clusterName,
                                                 Signal signal,
                                                 Notifier notifier,
                                                 EtmGroup etmGroup
    ) {
        try {
            SessionAndTransport sessionAndTransport = getSessionAndTransport(notifier);
            for (String recipient : determineRecipients(client, etmConfiguration, signal, etmGroup)) {
                final String subject = "[" + clusterName + "] - Signal fixed: " + signal.getName();
                InternetAddress toAddress = new InternetAddress(recipient);
                EtmPrincipal etmRecipient = getUserByEmail(client, etmConfiguration, recipient);
                if (etmRecipient != null) {
                    toAddress = new InternetAddress(recipient, etmRecipient.getName());
                }
                StringBuilder messageContent = new StringBuilder();
                if (etmRecipient != null) {
                    messageContent.append("Hi " + etmRecipient.getName() + ",\r\n\r\n");
                } else {
                    messageContent.append("Hi,\r\n\r\n");
                }
                messageContent.append("The threshold (" + signal.getThreshold() + ") of signal '" + signal.getName()
                        + "' is no longer exceeded.\r\n");
                messageContent.append("\r\n");
                messageContent.append("Kind regards,\r\n");
                messageContent.append("Your Enterprise Telemetry Monitor administrator");

                MimeMessage message = createMimeMessage(sessionAndTransport.session, notifier, toAddress, subject, messageContent.toString());
                sessionAndTransport.transport.sendMessage(message, message.getAllRecipients());
            }
        } catch (UnsupportedEncodingException | MessagingException e) {
            if (log.isErrorLevelEnabled()) {
                log.logErrorMessage("Unable to send signal mail.", e);
            }
        }

    }

    /**
     * Creates a default <code>MimeMessage</code>.
     *
     * @param session   The <code>Session</code> used to create the <code>MimeMessage</code>
     * @param notifier  The <code>Notifier</code> used to set the from address.
     * @param toAddress The recipient.
     * @param subject   The subject of the email.
     * @param body      The text body of the email.
     * @return A saved <code>MimeMessage</code>.
     * @throws UnsupportedEncodingException If the name of sender cannot be encoded in the default encoding.
     * @throws MessagingException           When any other failure occurs.
     */
    private MimeMessage createMimeMessage(Session session, Notifier notifier, InternetAddress toAddress, String subject, String body) throws UnsupportedEncodingException, MessagingException {
        MimeMessage message = new MimeMessage(session);
        if (notifier.getName() != null) {
            message.setFrom(new InternetAddress(notifier.getFromAddress(), notifier.getFromName()));
        } else {
            message.setFrom(new InternetAddress(notifier.getFromAddress()));
        }

        message.addRecipient(Message.RecipientType.TO, toAddress);
        message.setSubject(subject);
        message.setText(body);
        message.saveChanges();
        return message;
    }


    /**
     * Determine all email recipients of a <code>Signal</code> and the owning <code>EtmGroup</code>.
     *
     * @param client           The Elasticsearch client.
     * @param etmConfiguration The <code>EtmConfiguration</code> instance.
     * @param signal           The <code>Signal</code> to retrieve the recipients for.
     * @param etmGroup         The <code>EtmGroup</code> if the given <code>Signal</code> is owned by an <code>EtmGroup</code>.
     * @return A <code>Set</code> with email addresses that should receive an email.
     */
    private Set<String> determineRecipients(Client client, EtmConfiguration etmConfiguration, Signal signal, EtmGroup etmGroup) {
        Set<String> recipients = new HashSet<>(signal.getEmailRecipients());
        if (signal.isEmailAllEtmGroupMembers() && etmGroup != null) {
            ScrollableSearch scrollableSearch = new ScrollableSearch(client, client.prepareSearch(ElasticsearchLayout.CONFIGURATION_INDEX_NAME)
                    .setQuery(QueryBuilders.boolQuery()
                            .must(QueryBuilders.termQuery(ElasticsearchLayout.ETM_TYPE_ATTRIBUTE_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER))
                            .must(QueryBuilders.termQuery(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER + "." + this.etmPrincipalTags.getGroupsTag(), etmGroup.getName()))
                    )
                    .setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()))
                    .setFetchSource(new String[]{ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER + "." + this.etmPrincipalTags.getEmailTag()}, null)
            );
            for (SearchHit searchHit : scrollableSearch) {
                Map<String, Object> userValues = this.jsonConverter.getObject(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER, searchHit.getSourceAsMap());
                String email = this.jsonConverter.getString(this.etmPrincipalTags.getEmailTag(), userValues);
                if (email != null) {
                    recipients.add(email);
                }
            }
        }
        return recipients;
    }

    /**
     * Get a user by it's email address.
     *
     * @param client           The Elasticsearch client.
     * @param etmConfiguration The <code>EtmConfiguration</code> instance.
     * @param email            The email address.
     * @return The first <code>EtmPrincipal</code> with the given email address, or <code>null</code> if no such principal found.
     */
    private EtmPrincipal getUserByEmail(Client client, EtmConfiguration etmConfiguration, String email) {
        SearchResponse searchResponse = client.prepareSearch(ElasticsearchLayout.CONFIGURATION_INDEX_NAME)
                .setQuery(QueryBuilders.boolQuery()
                        .must(QueryBuilders.termQuery(ElasticsearchLayout.ETM_TYPE_ATTRIBUTE_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER))
                        .must(QueryBuilders.termQuery(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER + "." + this.etmPrincipalTags.getEmailTag(), email))
                )
                .setTimeout(TimeValue.timeValueMillis(etmConfiguration.getQueryTimeout()))
                .setFetchSource(true)
                .get();
        if (searchResponse.getHits().getHits().length > 0) {
            return this.etmPrincipalConverter.readPrincipal(searchResponse.getHits().getAt(0).getSourceAsString());
        }
        return null;
    }

    /**
     * Gives a <code>SessionAndTransport</code> instance that belongs to a certain <code>Notifier</code>.
     *
     * @param notifier The <code>Notifier</code> to retrieve the <code>SessionAndTransport</code> for.
     * @return The <code>SessionAndTransport</code> instance.
     */
    private SessionAndTransport getSessionAndTransport(Notifier notifier) {
        if (this.serverConnections.containsKey(notifier.getName())) {
            return this.serverConnections.get(notifier.getName());
        }
        Properties mailProps = new Properties();
        mailProps.put("mail.transport.protocol", "smtp");
        mailProps.put("mail.smtp.host", notifier.getSmtpHost());
        mailProps.put("mail.smtp.port", notifier.getSmtpPort());
        mailProps.put("mail.smtp.auth", notifier.getUsername() != null || notifier.getPassword() != null ? "true" : "false");
        if (Notifier.ConnectionSecurity.STARTTLS.equals(notifier.getConnectionSecurity())) {
            mailProps.put("mail.smtp.starttls.enable", "true");
            mailProps.put("mail.smtp.ssl.trust", "*");
        } else if (Notifier.ConnectionSecurity.SSL_TLS.equals(notifier.getConnectionSecurity())) {
            mailProps.put("mail.smtp.ssl.enable", "true");
            mailProps.put("mail.smtp.ssl.trust", "*");
        }

        Session mailSession;
        if (notifier.getUsername() != null || notifier.getPassword() != null) {
            mailSession = Session.getInstance(mailProps, new Authenticator() {

                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(notifier.getUsername(), notifier.getPassword());
                }

            });
        } else {
            mailSession = Session.getInstance(mailProps);
        }
        try {
            Transport transport = mailSession.getTransport();
            transport.connect();
            SessionAndTransport sessionAndTransport = new SessionAndTransport(mailSession, transport);
            this.serverConnections.put(notifier.getName(), sessionAndTransport);
            return sessionAndTransport;
        } catch (MessagingException e) {
            throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
        }
    }

    @Override
    public void close() {
        this.serverConnections.values().forEach(SessionAndTransport::close);
        this.serverConnections.clear();
    }

    /**
     * Class that holds a mail <code>Session</code> and a corresponding <code>Transport</code>
     */
    private class SessionAndTransport implements Closeable {

        private final Session session;
        private final Transport transport;

        SessionAndTransport(Session session, Transport transport) {
            this.session = session;
            this.transport = transport;
        }


        @Override
        public void close() {
            try {
                this.transport.close();
            } catch (MessagingException e) {
                if (log.isDebugLevelEnabled()) {
                    log.logDebugMessage("Failed to close smtp connection", e);
                }
            }
        }
    }

}