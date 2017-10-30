package com.jecstar.etm.processor.ibmmq;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;
import com.ibm.mq.*;
import com.ibm.mq.constants.CMQC;
import com.jecstar.etm.processor.core.TelemetryCommandProcessor;
import com.jecstar.etm.processor.ibmmq.configuration.Destination;
import com.jecstar.etm.processor.ibmmq.configuration.QueueManager;
import com.jecstar.etm.processor.ibmmq.handler.ClonedMessageHandler;
import com.jecstar.etm.processor.ibmmq.handler.EtmEventHandler;
import com.jecstar.etm.processor.ibmmq.handler.HandlerResult;
import com.jecstar.etm.processor.ibmmq.handler.IIBEventHandler;
import com.jecstar.etm.processor.internal.persisting.BusinessEventLogger;
import com.jecstar.etm.server.core.logging.LogFactory;
import com.jecstar.etm.server.core.logging.LogWrapper;
import com.jecstar.etm.server.core.ssl.SSLContextBuilder;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Hashtable;

class DestinationReader implements Runnable {
	
	private static final LogWrapper log = LogFactory.getLogger(DestinationReader.class);

	private final String configurationName;
	private final StringBuilder byteArrayBuilder = new StringBuilder();
	
	private final QueueManager queueManager;
	private final Destination destination;
	
	private final int waitInterval = 5000;
	private long lastCommitTime;
	
	private boolean stop = false;
	
	private MQQueueManager mqQueueManager;
	private MQDestination mqDestination;
	
	private int counter = 0;
	
	// Handlers
	private final EtmEventHandler etmEventHandler;
	private final IIBEventHandler iibEventHandler;
	private final ClonedMessageHandler clonedMessageEventHandler;

	private final Timer mqGetTimer;
	
	public DestinationReader(String configurationName, final TelemetryCommandProcessor processor, MetricRegistry metricRegistry, final QueueManager queueManager, final Destination destination) {
		this.configurationName = configurationName;
		this.queueManager = queueManager;
		this.destination = destination;
		this.etmEventHandler = new EtmEventHandler(processor);
		this.iibEventHandler = new IIBEventHandler(processor);
		this.clonedMessageEventHandler = new ClonedMessageHandler(processor);
		this.mqGetTimer = metricRegistry.timer("ibmmq-processor.mqget." + destination.getName().replaceAll("\\.", "_"));
	}
	@Override
	public void run() {
		connect();
		MQGetMessageOptions getOptions = new MQGetMessageOptions();
		getOptions.waitInterval = this.waitInterval; // Wait interval in milliseconds.
		getOptions.options = this.destination.getDestinationGetOptions();
		this.lastCommitTime = System.currentTimeMillis();
		while (!this.stop) {
			MQMessage message = null;
			try {
				message = new MQMessage();
				boolean continueProcessing = true;
				final Context mqGetContext = this.mqGetTimer.time();
				try {
					this.mqDestination.get(message, getOptions, this.destination.getMaxMessageSize());
				} catch (MQException e) {
					continueProcessing = handleMQException(e, message);
				} finally {
					mqGetContext.stop();
				}
				if (!continueProcessing) {
					continue;
				}
				if (log.isDebugLevelEnabled()) {
					log.logDebugMessage("Read message with id '" + byteArrayToString(message.messageId) + "'.");
				}
				HandlerResult result;
				if ("etmevent".equalsIgnoreCase(this.destination.getMessagesType())) {
					result = this.etmEventHandler.handleMessage(message);
				} else if ("iibevent".equalsIgnoreCase(this.destination.getMessagesType())) {
					result = this.iibEventHandler.handleMessage(message);
				} else if ("clone".equalsIgnoreCase(this.destination.getMessagesType())) {
					result = this.clonedMessageEventHandler.handleMessage(message);
				} else {
					result = this.etmEventHandler.handleMessage(message);
					if (HandlerResult.PARSE_FAILURE.equals(result)) {
						result = this.iibEventHandler.handleMessage(message);
					}
					if (HandlerResult.PARSE_FAILURE.equals(result)) {
						result = this.clonedMessageEventHandler.handleMessage(message);
					}
				}
				if (!HandlerResult.PROCESSED.equals(result)) {
					tryBackout(message);
				}
				this.counter++;
				if (shouldCommit()) {
					commit();
				}
			} catch (Error e) {
				if (log.isFatalLevelEnabled()) {
					log.logFatalMessage("Error detected while processing message with id '" + byteArrayToString(message.messageId) + "'. Stopping reader to prevent further unexpected behaviour.", e);
				}
				BusinessEventLogger.logMqProcessorEmergencyShutdown(e);
				this.stop = true;
			} catch (Exception e) {
				if (log.isWarningLevelEnabled()) {
					log.logWarningMessage("Failed to process message with id '" + byteArrayToString(message.messageId) + "'. Trying to put it on the backout queue.", e);
				}
				tryBackout(message);
				this.counter++;
				if (shouldCommit()) {
					commit();
				}
			} finally {
				if (message != null) {
					try {
						message.clearMessage();
					} catch (IOException e) {
					}
				}
			}
			if (Thread.currentThread().isInterrupted()) {
				this.stop = true;
			}
		}
		commit();
		disconnect();
	}
	
	private boolean handleMQException(MQException e, MQMessage message) {
		if (e.completionCode == CMQC.MQCC_FAILED && e.reasonCode == CMQC.MQRC_NO_MSG_AVAILABLE) {
			// No message available, retry
			if (shouldCommit()) {
				commit();
			}
			return false;
		}
		switch (e.reasonCode) {
		    case CMQC.MQRC_TRUNCATED_MSG_ACCEPTED:
			    if (log.isInfoLevelEnabled()) {
				    log.logInfoMessage("Accepted a truncated message with id '" + byteArrayToString(message.messageId) + "'.");
			    }
			    return true;
			case CMQC.MQRC_TRUNCATED_MSG_FAILED:
                if (log.isInfoLevelEnabled()) {
                    log.logInfoMessage("Message with id '" + byteArrayToString(message.messageId) + "' is too big for configured buffer. Message will be ignored.");
                }
                removeMessage(message.messageId);
			    return false;
		    case CMQC.MQRC_CONNECTION_BROKEN:
            case CMQC.MQRC_CONNECTION_QUIESCING:
            case CMQC.MQRC_CONNECTION_STOPPING:
            case CMQC.MQRC_Q_MGR_QUIESCING:
            case CMQC.MQRC_Q_MGR_STOPPING:
            case CMQC.MQRC_Q_MGR_NOT_AVAILABLE:
            case CMQC.MQRC_Q_MGR_NOT_ACTIVE:
            case CMQC.MQRC_CLIENT_CONN_ERROR:
            case CMQC.MQRC_CHANNEL_STOPPED_BY_USER:
            case CMQC.MQRC_HCONN_ERROR:
            case CMQC.MQRC_HOBJ_ERROR:
            case CMQC.MQRC_UNEXPECTED_ERROR:
                if (log.isInfoLevelEnabled()) {
                    log.logInfoMessage("Detected MQ error with reason '" + e.reasonCode+ "'. Trying to reconnect.");
                }
                disconnect();
                try {
                    Thread.sleep(this.waitInterval);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                }
                connect();
                return false;
			default:
				if (log.isInfoLevelEnabled()) {
					log.logInfoMessage("Detected MQ error with reason '" + e.reasonCode+ "'. Ignoring message with id '" + byteArrayToString(message.messageId) + "'.");
				}
				return false;
		}
	}
	private void commit() {
		if (this.mqQueueManager != null) {
			try {
				if (log.isDebugLevelEnabled()) {
					log.logDebugMessage("Committing messages from queuemanager '" + this.mqQueueManager.getName() + "'.");
				}
				this.mqQueueManager.commit();
				if (log.isDebugLevelEnabled()) {
					log.logDebugMessage("Messages from queuemanager '" + this.mqQueueManager.getName() + "' committed.");
				}
			} catch (MQException e) {
				if (log.isErrorLevelEnabled()) {
					log.logErrorMessage("Unable to execute commit on queuemanager.", e);
				}
			}
		}
		this.counter = 0;
		this.lastCommitTime = System.currentTimeMillis();		
	}
	
	private boolean shouldCommit() {
		return (this.counter >= this.destination.getCommitSize()) || (System.currentTimeMillis() - this.lastCommitTime > this.destination.getCommitInterval());
	}
	
	private void connect() {
		if (log.isDebugLevelEnabled()) {
			log.logDebugMessage("Connecting to queuemanager '" + this.queueManager.getName() + "' and " + this.destination.getType() + " '" + this.destination.getName() + "'");
		}
		try {
			Hashtable<String, Object> connectionProperties = new Hashtable<>();
			connectionProperties.put(CMQC.TRANSPORT_PROPERTY, CMQC.TRANSPORT_MQSERIES_CLIENT);
			connectionProperties.put(CMQC.HOST_NAME_PROPERTY, this.queueManager.getHost());
			connectionProperties.put(CMQC.APPNAME_PROPERTY, "ETM-" + this.configurationName);
			connectionProperties.put(CMQC.PORT_PROPERTY, this.queueManager.getPort());
			setConnectionPropertyWhenNotEmpty(CMQC.USER_ID_PROPERTY, this.queueManager.getUserId(), connectionProperties);
			setConnectionPropertyWhenNotEmpty(CMQC.PASSWORD_PROPERTY, this.queueManager.getPassword(), connectionProperties);
			setConnectionPropertyWhenNotEmpty(CMQC.CHANNEL_PROPERTY, this.queueManager.getChannel(), connectionProperties);
			if (this.queueManager.getSslCipherSuite() != null) {
				try {
					SSLContext sslContext = new SSLContextBuilder().createSslContext(
							queueManager.getSslProtocol(), 
							queueManager.getSslKeystoreLocation(), 
							queueManager.getSslKeystoreType(), 
							queueManager.getSslKeystorePassword() == null ? null : queueManager.getSslKeystorePassword().toCharArray(), 
							queueManager.getSslTruststoreLocation(), 
							queueManager.getSslTruststoreType(), 
							queueManager.getSslTruststorePassword() == null ? null : queueManager.getSslTruststorePassword().toCharArray()); 
					connectionProperties.put(CMQC.SSL_CIPHER_SUITE_PROPERTY, this.queueManager.getSslCipherSuite());
					connectionProperties.put(CMQC.SSL_SOCKET_FACTORY_PROPERTY, sslContext.getSocketFactory());
				} catch (KeyManagementException | UnrecoverableKeyException | NoSuchAlgorithmException | CertificateException | KeyStoreException | IOException e) {
					if (log.isErrorLevelEnabled()) {
						log.logErrorMessage("Unable to create SSL context. Fallback to insecure connection to queuemanager '" + this.queueManager.getName()+ "'.", e);
					}
				}
			}
			this.mqQueueManager = new MQQueueManager(this.queueManager.getName(), connectionProperties);
			if ("topic".equals(this.destination.getType())) {
				this.mqDestination = this.mqQueueManager.accessTopic(this.destination.getName(), null, CMQC.MQSO_CREATE, null, "Enterprise Telemetry Monitor - " + this.configurationName);
			} else {
				this.mqDestination = this.mqQueueManager.accessQueue(this.destination.getName(), this.destination.getDestinationOpenOptions());
			}
			if (log.isDebugLevelEnabled()) {
				log.logDebugMessage("Connected to queuemanager '" + this.queueManager.getName() + "' and " + this.destination.getType() + " '" + this.destination.getName() + "'");
			}
		} catch (MQException e) {
			if (log.isWarningLevelEnabled()) {
				log.logWarningMessage("Failed to connect to queuemanager '" + this.queueManager.getName() + "' and/or " + this.destination.getType() + " '" + this.destination.getName() + "'" , e);
			}
		}
	}
	
	private void setConnectionPropertyWhenNotEmpty(String propertyKey, String propertyValue, Hashtable<String, Object> connectionProperties) {
		if (propertyValue != null) {
			connectionProperties.put(propertyKey, propertyValue);
		}
	}
	private void disconnect() {
		if (log.isDebugLevelEnabled()) {
			log.logDebugMessage("Disconnecting from queuemanager");
		}
		if (this.mqDestination != null) {
			try {
				this.mqDestination.close();
			} catch (MQException e) {
				if (log.isDebugLevelEnabled()) {
					log.logDebugMessage("Unable to close " + this.destination.getType() , e);
				}
			}
		}
		if (this.mqQueueManager != null) {
			try {
				counter = 0;
				this.lastCommitTime = System.currentTimeMillis();
				this.mqQueueManager.commit();
			} catch (MQException e1) {
				if (log.isErrorLevelEnabled()) {
					log.logErrorMessage("Unable to execute commit on queuemanager", e1);
				}
			}
			try {
				this.mqQueueManager.close();
			} catch (MQException e) {
				if (log.isDebugLevelEnabled()) {
					log.logDebugMessage("Unable to close queuemanager", e);
				}
			}
		}
		if (log.isDebugLevelEnabled()) {
			log.logDebugMessage("Disconnected from queuemanager");
		}
	}

    /**
     * Remove a message from the destination.
     *
     * @param messageId The id of the message that should be removed.
     */
	private void removeMessage(byte[] messageId) {
        if (messageId == null) {
            return;
        }
	    MQGetMessageOptions getOptions = new MQGetMessageOptions();
        getOptions.waitInterval = this.waitInterval;
        getOptions.options = Destination.DEFAULT_GET_OPTIONS + CMQC.MQGMO_ACCEPT_TRUNCATED_MSG;
        getOptions.matchOptions = CMQC.MQMO_MATCH_MSG_ID;

        MQMessage message = new MQMessage();
        message.messageId = messageId;

        try {
            // We don't want to load the message into memory, so the maxMsgSize is set to zero.
            this.mqDestination.get(message, getOptions, 0);
        } catch (MQException e) {
            if (e.reasonCode == CMQC.MQRC_TRUNCATED_MSG_ACCEPTED) {
                // Because the maxMsgSize is set to zero, the message will be truncated.
                this.counter++;
                if (shouldCommit()) {
                    commit();
                }
            } else {
                if (log.isInfoLevelEnabled()) {
                    log.logInfoMessage("Detected MQ error with reason '" + e.reasonCode+ "'. Failed to remove message with id '" + byteArrayToString(messageId) + "'.");
                }
            }
        }
    }
	
	private boolean tryBackout(MQMessage message) {
		MQQueue backoutQueue = null;
		try {
			String backoutQueueName = this.mqDestination.getAttributeString(CMQC.MQCA_BACKOUT_REQ_Q_NAME, 48);
			if (backoutQueueName != null && backoutQueueName.trim().length() > 0) {
				backoutQueue = this.mqQueueManager.accessQueue(backoutQueueName.trim(), CMQC.MQOO_OUTPUT + CMQC.MQOO_FAIL_IF_QUIESCING);
				backoutQueue.put(message);
				backoutQueue.close();
				return true;
			} else {
				if (log.isWarningLevelEnabled()) {
					log.logWarningMessage("No backout queue defined on destination '" + this.mqDestination.getName() + "'. Unable to backout messages.");
				}				
			}
		} catch (MQException e) {
			if (log.isWarningLevelEnabled()) {
				log.logWarningMessage("Failed to put message with id '" + byteArrayToString(message.messageId) + "' to the configured backout queue", e);
			}
		} finally {
			if (backoutQueue != null) {
				try {
					backoutQueue.close();
				} catch (MQException e1) {
				}
			}
		}
		return false;
	}

	
	public void stop() {
		this.stop = true;
	}
	
	private String byteArrayToString(byte[] bytes) {
		if (bytes == null) {
			return null;
		}
		this.byteArrayBuilder.setLength(0);
		boolean allZero = true;
		for (byte aByte : bytes) {
			this.byteArrayBuilder.append(String.format("%02x", aByte));
			if (aByte != 0) {
				allZero = false;
			}
		}
		return allZero ? null : this.byteArrayBuilder.toString();
	}
	
}
