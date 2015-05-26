package com.jecstar.etm.processor.repository.cassandra;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.datastax.driver.core.BatchStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.jecstar.etm.core.parsers.ExpressionParser;
import com.jecstar.etm.core.parsers.ExpressionParserFactory;
import com.jecstar.etm.processor.repository.EndpointConfigResult;

public class CassandraStatementExecutor {
	
	private final Session session;
//	private final PreparedStatement insertTelemetryEventStatement;
//	private final PreparedStatement insertSourceIdIdStatement;
//	private final PreparedStatement insertCorrelationDataStatement;
//	private final PreparedStatement insertCorrelationToParentStatement;
//	private final PreparedStatement insertEventOccurrenceStatement;
//	private final PreparedStatement insertTransactionEventStartStatement;
//	private final PreparedStatement insertTransactionEventFinishStatement;
//	private final PreparedStatement insertMessageEventPerformanceStartStatement;
//	private final PreparedStatement insertMessageEventPerformanceFinishStatement;
//	private final PreparedStatement insertMessageEventExpirationStartStatement;
//	private final PreparedStatement insertMessageEventExpirationFinishStatement;
//	private final PreparedStatement insertSlaStartStatement;
//	private final PreparedStatement insertSlaFinishStatement;
//	private final PreparedStatement insertDataRetentionStatement;
//	private final PreparedStatement updateApplicationCounterStatement;
//	private final PreparedStatement updateEventNameCounterStatement;
//	private final PreparedStatement updateApplicationEventNameCounterStatement;
//	private final PreparedStatement updateTransactionNameCounterStatement;
//	private final PreparedStatement findParentStatementWithSourceIdAndApplication;
//	private final PreparedStatement findParentStatementWithSourceId;
	private final PreparedStatement findEndpointConfigStatement;
//	private final PreparedStatement selectTelemetryEventStatement;
	
	public CassandraStatementExecutor(final Session session) {
		this.session = session;
//		this.insertTelemetryEventStatement = session.prepare("insert into telemetry_event ("
//				+ "id, "
//				+ "application, "
//				+ "content, "
//				+ "correlationCreationTime, "
//				+ "correlationData, "
//				+ "correlationId, "
//				+ "correlationName, "
//				+ "creationTime, "
//				+ "direction, "
//				+ "endpoint, "
//				+ "expiryTime, "
//				+ "metadata, "
//				+ "name, "
//				+ "sourceCorrelationId, "
//				+ "sourceId, "
//				+ "transactionId, "
//				+ "transactionName, "
//				+ "type, "
//				+ "slaRule) values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?);");
//		this.insertSourceIdIdStatement = session.prepare("insert into sourceid_id_correlation ("
//				+ "sourceId, "
//				+ "application, "
//				+ "id) values (?,?,?);");
//		this.insertCorrelationDataStatement = session.prepare("insert into correlation_data ("
//				+ "name_timeunit, "
//				+ "timeunit, "
//				+ "name, "
//				+ "value, "
//				+ "id) values (?,?,?,?,?);");
//		this.insertCorrelationToParentStatement = session.prepare("update telemetry_event set "
//				+ "correlations = correlations + ? "
//				+ "where id = ?;");
//		this.insertEventOccurrenceStatement = session.prepare("insert into event_occurrences ("
//				+ "timeunit, "
//				+ "type, "
//				+ "name_timeframe,"
//				+ "name) values (?,?,?,?);");
//		this.insertTransactionEventStartStatement = session.prepare("insert into transaction_performance ("
//				+ "transactionName_timeunit, "
//				+ "transactionId, "
//				+ "transactionName,  "
//				+ "startTime, "
//				+ "expiryTime, "
//				+ "application) values (?,?,?,?,?,?);");
//		this.insertTransactionEventFinishStatement = session.prepare("insert into transaction_performance ("
//				+ "transactionName_timeunit, "
//				+ "transactionId, "
//				+ "startTime, "
//				+ "finishTime) values (?,?,?,?);");
//		this.insertMessageEventPerformanceStartStatement = session.prepare("insert into message_performance ("
//				+ "name_timeunit, "
//				+ "id, "
//				+ "name, "
//				+ "startTime, "
//				+ "expiryTime, "
//				+ "application) values (?,?,?,?,?,?);");
//		this.insertMessageEventPerformanceFinishStatement = session.prepare("insert into message_performance ("
//				+ "name_timeunit, "
//				+ "id, "
//				+ "startTime, "
//				+ "finishTime) values (?,?,?,?);");
//		this.insertMessageEventExpirationStartStatement = session.prepare("insert into message_expiration ("
//				+ "name_timeunit, "
//				+ "id, "
//				+ "name, "
//				+ "expiryTime, "
//				+ "startTime, "
//				+ "application) values (?,?,?,?,?,?);");
//		this.insertMessageEventExpirationFinishStatement = session.prepare("insert into message_expiration ("
//				+ "name_timeunit, "
//				+ "id, "
//				+ "expiryTime, "
//				+ "finishTime) values (?,?,?,?);");
//		this.insertSlaStartStatement = session.prepare("insert into transaction_sla ("
//				+ "transactionName_timeunit, "
//				+ "slaExpiryTime, "
//				+ "transactionId, "
//				+ "transactionName, "
//				+ "startTime, "
//				+ "application, "
//				+ "slaRule) values (?,?,?,?,?,?,?);");
//		this.insertSlaFinishStatement = session.prepare("insert into transaction_sla ("
//				+ "transactionName_timeunit, "
//				+ "slaExpiryTime, "
//				+ "transactionId, "
//				+ "finishTime) values (?,?,?,?);");
//		this.insertDataRetentionStatement = session.prepare("insert into data_retention ("
//				+ "timeunit, "
//				+ "id, "
//				+ "eventOccurrenceTimeunit, "
//				+ "sourceId, "
//				+ "partionKeySuffix, "
//				+ "applicationName, "
//				+ "eventName, "
//				+ "transactionName, "
//				+ "correlationData) values (?,?,?,?,?,?,?,?,?);");
//		this.updateApplicationCounterStatement = session.prepare("update application_counter set "
//				+ "count = count + 1, "
//				+ "messageRequestCount = messageRequestCount + ?, "
//				+ "incomingMessageRequestCount = incomingMessageRequestCount + ?, "
//				+ "outgoingMessageRequestCount = outgoingMessageRequestCount + ?, "
//				+ "messageResponseCount = messageResponseCount + ?, "
//				+ "incomingMessageResponseCount = incomingMessageResponseCount + ?, "
//				+ "outgoingMessageResponseCount = outgoingMessageResponseCount + ?, "
//				+ "messageDatagramCount = messageDatagramCount + ?, "
//				+ "incomingMessageDatagramCount = incomingMessageDatagramCount + ?, "
//				+ "outgoingMessageDatagramCount = outgoingMessageDatagramCount + ?, "
//				+ "messageResponseTime = messageResponseTime + ?, "
//				+ "incomingMessageResponseTime = incomingMessageResponseTime + ?, "
//				+ "outgoingMessageResponseTime = outgoingMessageResponseTime + ? "
//				+ "where application_timeunit = ? and timeunit = ? and application = ?;");
//		this.updateEventNameCounterStatement = session.prepare("update eventname_counter set "
//				+ "count = count + 1, "
//				+ "messageRequestCount = messageRequestCount + ?, "
//				+ "messageResponseCount = messageResponseCount + ?, "
//				+ "messageDatagramCount = messageDatagramCount + ?, "
//				+ "messageResponseTime = messageResponseTime + ? "
//				+ "where eventName_timeunit = ? and timeunit = ? and eventName = ?;");
//		this.updateApplicationEventNameCounterStatement = session.prepare("update application_event_counter set "
//				+ "count = count + 1, "
//				+ "messageRequestCount = messageRequestCount + ?, "
//				+ "incomingMessageRequestCount = incomingMessageRequestCount + ?, "
//				+ "outgoingMessageRequestCount = outgoingMessageRequestCount + ?, "
//				+ "messageResponseCount = messageResponseCount + ?, "
//				+ "incomingMessageResponseCount = incomingMessageResponseCount + ?, "
//				+ "outgoingMessageResponseCount = outgoingMessageResponseCount + ?, "
//				+ "messageDatagramCount = messageDatagramCount + ?, "
//				+ "incomingMessageDatagramCount = incomingMessageDatagramCount + ?, "
//				+ "outgoingMessageDatagramCount = outgoingMessageDatagramCount + ?, "
//				+ "messageResponseTime = messageResponseTime + ?, "
//				+ "incomingMessageResponseTime = incomingMessageResponseTime + ?, "
//				+ "outgoingMessageResponseTime = outgoingMessageResponseTime + ? "
//				+ "where application_timeunit = ? and eventName = ? and timeunit = ? and application = ?;");
//		this.updateTransactionNameCounterStatement = session.prepare("update transactionname_counter set "
//				+ "count = count + 1, "
//				+ "transactionStart = transactionStart + ?, "
//				+ "transactionFinish = transactionFinish + ?, "
//				+ "transactionResponseTime = transactionResponseTime + ? "
//				+ "where transactionName_timeunit = ? and timeunit = ? and transactionName = ?;");
//		this.findParentStatementWithSourceIdAndApplication = session.prepare("select "
//				+ "id "
//				+ "from sourceid_id_correlation where sourceId = ? and application = ?;");
//		this.findParentStatementWithSourceId = session.prepare("select "
//				+ "id "
//				+ "from sourceid_id_correlation where sourceId = ?;");
		this.findEndpointConfigStatement = session.prepare("select "
				+ "readingApplicationParsers, "
				+ "writingApplicationParsers, "
				+ "eventNameParsers, "
				+ "correlationParsers, "
				+ "transactionNameParsers "
				+ "from endpoint_config where endpoint = ?;");
//		this.selectTelemetryEventStatement = session.prepare("select "
//				+ "application, "
//				+ "content, "
//				+ "correlationCreationTime, "
//				+ "correlationData, "
//				+ "correlationId, "
//				+ "correlationName, "
//				+ "creationTime, "
//				+ "direction, "
//				+ "endpoint, "
//				+ "expiryTime, "
//				+ "metadata, "
//				+ "name, "
//				+ "sourceCorrelationId, "
//				+ "sourceId, "
//				+ "transactionId, "
//				+ "transactionName, "
//				+ "type, "
//				+ "slaRule "
//				+ "from telemetry_event where id = ?");
	}
	
//	public void addTelemetryEvent(final TelemetryEvent event, final BatchStatement batchStatement) {
//		batchStatement.add(this.insertTelemetryEventStatement.bind(
//				event.id, 
//				event.application, 
//				event.content,
//				event.correlationCreationTime.getTime() == 0 ? null : event.correlationCreationTime,
//				event.correlationData,
//				event.correlationId,
//				event.correlationName,
//				event.creationTime, 
//		        event.direction != null ? event.direction.name() : null, 
//		        event.endpoint,
//		        event.expiryTime.getTime() == 0 ? null : event.expiryTime,
//		        event.metadata,
//		        event.name, 
//		        event.sourceCorrelationId, 
//		        event.sourceId, 
//		        event.transactionId, 
//		        event.transactionName,
//		        event.type != null ? event.type.name() : null,
//				event.slaRule != null ? event.slaRule.toConfiguration() : null));
//		if (event.correlationId != null) {
//			final List<UUID> correlation = new ArrayList<UUID>(1);
//			correlation.add(event.id);
//			batchStatement.add(this.insertCorrelationToParentStatement.bind(correlation, event.correlationId));
//		}
//	}
//	
//	public void addSourceIdCorrelationData(final TelemetryEvent event, final BatchStatement batchStatement) {
//		batchStatement.add(this.insertSourceIdIdStatement.bind(
//				event.sourceId,
//				event.application == null ? "_unknown_" : event.application,
//				event.id));
//	}
//	
//	public void addCorrelationData(final TelemetryEvent event, final String key, final String correlationDataName, final String correlationDataValue, final BatchStatement batchStatement) {
//		batchStatement.add(this.insertCorrelationDataStatement.bind(
//				key,
//				event.creationTime,
//				correlationDataName, 
//				correlationDataValue,
//				event.id));
//	}
//	
//	public void addEventOccurence(final Date timestamp, final String occurrenceName, final String occurrenceValue, final String originalName, final BatchStatement batchStatement) {
//		batchStatement.add(this.insertEventOccurrenceStatement.bind(
//				timestamp, 
//				occurrenceName, 
//				occurrenceValue,
//				originalName));
//	}
//	
//	public void addTransactionEventStart(final TelemetryEvent event, final String key, final BatchStatement batchStatement) {
//		batchStatement.add(this.insertTransactionEventStartStatement.bind(
//				key,
//				event.transactionId,
//				event.transactionName,
//		        event.creationTime, 
//		        event.expiryTime,
//		        event.application));
//		if (event.slaRule != null) {
//			batchStatement.add(this.insertSlaStartStatement.bind(
//					key, 
//					new Date(event.creationTime.getTime() + event.slaRule.getSlaExpiryTime()), 
//					event.transactionId, 
//					event.transactionName, 
//					event.creationTime, 
//					event.application, 
//					event.slaRule.toConfiguration()));
//		}
//	}
//	
//	public void addTransactionEventFinish(final TelemetryEvent event, final String key, final BatchStatement batchStatement) {
//		batchStatement.add(this.insertTransactionEventFinishStatement.bind(
//				key,
//				event.transactionId,
//				event.correlationCreationTime,
//		        event.creationTime));
//		if (event.slaRule != null) {
//			// TODO -> ophalen starttijd van parent event en bepalen of de SLA is behaald of niet. Zo niet, dan alleen hier record wegschrijven, en niet ook in de addTransactionEventStart
//			batchStatement.add(this.insertSlaFinishStatement.bind(
//					key, 
//					new Date(event.correlationCreationTime.getTime() + event.slaRule.getSlaExpiryTime()),
//					event.transactionId,
//					event.creationTime));
//		}		
//	}
//
//	public void addMessageEventStart(final TelemetryEvent event, final String key, final BatchStatement batchStatement) {
//		batchStatement.add(this.insertMessageEventPerformanceStartStatement.bind(
//				key,
//				event.id, 
//				event.name,
//		        event.creationTime, 
//		        event.expiryTime,
//		        event.application));
//		batchStatement.add(this.insertMessageEventExpirationStartStatement.bind(
//				key,
//				event.id,
//				event.name,
//		        event.expiryTime,
//		        event.creationTime,
//		        event.application));
//	}
//	
//	public void addMessageEventFinish(final TelemetryEvent event, final String key, final BatchStatement batchStatement) {
//		batchStatement.add(this.insertMessageEventPerformanceFinishStatement.bind(
//				key,
//				event.correlationId,
//				event.correlationCreationTime,
//		        event.creationTime));
//		batchStatement.add(this.insertMessageEventExpirationFinishStatement.bind(
//				key,
//				event.correlationId,
//				event.correlationExpiryTime,
//		        event.creationTime));
//	}
//
//	public void addDataRetention(final DataRetention dataRetention, final BatchStatement batchStatement) {
//		batchStatement.add(this.insertDataRetentionStatement.bind(
//				dataRetention.retentionTimestamp,
//				dataRetention.id,
//				dataRetention.eventOccurrenceTimestamp,
//				dataRetention.sourceId,
//				dataRetention.partionKeySuffix,
//				dataRetention.applicationName,
//				dataRetention.eventName,
//				dataRetention.transactionName,
//				dataRetention.correlationData));
//	}
//	
//	public void addApplicationCounter(final long requestCount, final long incomingRequestCount, final long outgoingRequestCount,
//	        final long responseCount, final long incomingResponseCount, final long outgoingResponseCount, final long datagramCount,
//	        final long incomingDatagramCount, final long outgoingDatagramCount, final long responseTime, final long incomingResponseTime,
//	        final long outgoingResponseTime, final String application, final Date timestamp, final String key, final BatchStatement batchStatement) {
//		batchStatement.add(this.updateApplicationCounterStatement.bind(
//				requestCount, 
//				incomingRequestCount, 
//				outgoingRequestCount,
//		        responseCount, 
//		        incomingResponseCount, 
//		        outgoingResponseCount, 
//		        datagramCount, 
//		        incomingDatagramCount,
//		        outgoingDatagramCount, 
//		        responseTime, 
//		        incomingResponseTime, 
//		        outgoingResponseTime, 
//		        key,
//		        timestamp,
//		        application));
//	}
//	
//	public void addEventNameCounter(final long requestCount, final long responseCount, final long datagramCount,
//	        final long responseTime, final String eventName, final Date timestamp, final String key, final BatchStatement batchStatement) {
//		batchStatement.add(this.updateEventNameCounterStatement.bind(
//				requestCount, 
//				responseCount, 
//				datagramCount, 
//				responseTime, 
//				key, 
//				timestamp,
//				eventName));
//	}
//	
//	public void addApplicationEventNameCounter(final long requestCount, final long incomingRequestCount,
//	        final long outgoingRequestCount, final long responseCount, final long incomingResponseCount, final long outgoingResponseCount,
//	        final long datagramCount, final long incomingDatagramCount, final long outgoingDatagramCount, final long responseTime,
//	        final long incomingResponseTime, final long outgoingResponseTime, final String application, final String eventName,
//	        final Date timestamp, final String key, final BatchStatement batchStatement) {
//		batchStatement.add(this.updateApplicationEventNameCounterStatement.bind(
//				requestCount, 
//				incomingRequestCount, 
//				outgoingRequestCount,
//		        responseCount, 
//		        incomingResponseCount, 
//		        outgoingResponseCount, 
//		        datagramCount, 
//		        incomingDatagramCount,
//		        outgoingDatagramCount, 
//		        responseTime, 
//		        incomingResponseTime, 
//		        outgoingResponseTime, 
//				key, 
//				eventName, 
//				timestamp,
//				application));
//	}
//	
//	public void addTransactionNameCounter(final long requestCount, final long responseCount, final long responseTime,
//	        final String transactionName, final Date timestamp, final String key, final BatchStatement batchStatement) {
//		batchStatement.add(this.updateTransactionNameCounterStatement.bind(
//				requestCount, 
//				responseCount, 
//				responseTime, 
//				key, 
//				timestamp,
//				transactionName));
//	}
//	
//	
//	public TelemetryEvent findParent(final String sourceId, String application) {
//		ResultSet resultSet = this.session.execute(this.findParentStatementWithSourceIdAndApplication.bind(sourceId, application == null ? "_unknown_" : application));
//		Row row = resultSet.one();
//		if (row != null) {
//			return getTelemetryEvent(row.getUUID(0));
//		} else {
//			resultSet = this.session.execute(this.findParentStatementWithSourceId.bind(sourceId));
//			row = resultSet.one();
//			if (row != null) {
//				return getTelemetryEvent(row.getUUID(0));
//			}
//		}
//		return null;
//	}
//	
//	private TelemetryEvent getTelemetryEvent(UUID id) {
//	    ResultSet resultSet = this.session.execute(this.selectTelemetryEventStatement.bind(id));
//		Row row = resultSet.one();
//		if (row != null) {
//			TelemetryEvent event = new TelemetryEvent();
//			event.id = id;
//			event.application = row.getString(0);
//			event.content = row.getString(1);
//			event.correlationCreationTime = row.getDate(2);
//			event.correlationData = row.getMap(3, String.class, String.class);
//			event.correlationId = row.getUUID(4);
//			event.correlationName = row.getString(5);
//			event.creationTime = row.getDate(6);
//			String direction = row.getString(7);
//			event.direction = direction == null ? null : TelemetryEventDirection.valueOf(direction);
//			event.endpoint = row.getString(8);
//			event.expiryTime = row.getDate(9);
//			event.metadata = row.getMap(10, String.class, String.class);
//			event.name = row.getString(11);
//			event.sourceCorrelationId = row.getString(12);
//			event.sourceId = row.getString(13);
//			event.transactionId = row.getUUID(14);
//			event.transactionName = row.getString(15);
//			String type = row.getString(16);
//			event.type = type == null ? null : TelemetryMessageEventType.valueOf(type);
//			event.slaRule = SlaRule.fromConfiguration(row.getString(17));
//			return event;
//		}	    
//	    return null;
//    }
//
	public void findAndMergeEndpointConfig(final String endpoint, final EndpointConfigResult result) {
		if (endpoint == null) {
			return;
		}
		final ResultSet resultSet = this.session.execute(this.findEndpointConfigStatement.bind(endpoint));
		Row row = resultSet.one();
		if (row != null) {
			mergeRowIntoEndpointConfig(row, result);
		}
	}
	
	private void mergeRowIntoEndpointConfig(final Row row, final EndpointConfigResult result) {
		if (result.readingApplicationParsers == null) {
			result.readingApplicationParsers = createExpressionParsers(row.getList(0, String.class));
		} else {
			result.readingApplicationParsers.addAll(0, createExpressionParsers(row.getList(0, String.class)));
		}
		if (result.writingApplicationParsers == null) {
			result.writingApplicationParsers = createExpressionParsers(row.getList(1, String.class));
		} else {
			result.writingApplicationParsers.addAll(0, createExpressionParsers(row.getList(1, String.class)));
		}
		if (result.eventNameParsers == null) {
			result.eventNameParsers = createExpressionParsers(row.getList(2, String.class));
		} else {
			result.eventNameParsers.addAll(0, createExpressionParsers(row.getList(2, String.class)));
		}
		Map<String, String> map = row.getMap(3, String.class, String.class);
		Iterator<String> iterator = map.keySet().iterator();
		while (iterator.hasNext()) {
			String key = iterator.next();
			result.correlationDataParsers.put(key, ExpressionParserFactory.createExpressionParserFromConfiguration(map.get(key)));
		}
		if (result.transactionNameParsers == null) {
			result.transactionNameParsers = createExpressionParsers(row.getList(4, String.class));
		} else {
			result.transactionNameParsers.addAll(0, createExpressionParsers(row.getList(4, String.class)));
		}
    }

	private List<ExpressionParser> createExpressionParsers(final List<String> expressions) {
		if (expressions == null) {
			return null;
		}
		List<ExpressionParser> expressionParsers = new ArrayList<ExpressionParser>(expressions.size());
		for (String expression : expressions) {
			expressionParsers.add(ExpressionParserFactory.createExpressionParserFromConfiguration(expression));
		}
		return expressionParsers;
	}

	public ResultSet execute(BatchStatement batchCounterStatement) {
	    return this.session.execute(batchCounterStatement);
    }
}
