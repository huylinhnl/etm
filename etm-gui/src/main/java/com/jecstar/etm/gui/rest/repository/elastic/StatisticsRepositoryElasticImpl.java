package com.jecstar.etm.gui.rest.repository.elastic;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.elasticsearch.client.Client;

import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.BuiltStatement;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.jecstar.etm.core.TelemetryEventType;
import com.jecstar.etm.core.cassandra.PartitionKeySuffixCreator;
import com.jecstar.etm.core.util.DateUtils;
import com.jecstar.etm.gui.rest.repository.Average;
import com.jecstar.etm.gui.rest.repository.ExpiredMessage;
import com.jecstar.etm.gui.rest.repository.StatisticsRepository;

public class StatisticsRepositoryElasticImpl implements StatisticsRepository {

	private final Client elasticClient;

//	private final PreparedStatement selectTransactionPerformanceStatement;
//	private final PreparedStatement selectMessagePerformanceStatement;
//	private final PreparedStatement selectMessageExpirationStatement;
//	private final PreparedStatement selectApplicationCountsStatement;
//	private final PreparedStatement selectEventCorrelations;
//	private final PreparedStatement selectEventExpirationDataStatement;
//	private final PreparedStatement selectApplicationMessagesCountStatement;
//	private final PreparedStatement selectApplicationMessageNamesStatement;
//	private final PreparedStatement updateMessageExpirationStatement;
	
	 
	
	public StatisticsRepositoryElasticImpl(Client elasticClient) {
		this.elasticClient = elasticClient;
//		this.selectTransactionPerformanceStatement = this.session.prepare("select transactionName, startTime, finishTime, expiryTime from transaction_performance where transactionName_timeunit = ? and startTime >= ? and startTime <= ?");
//		this.selectMessagePerformanceStatement = this.session.prepare("select name, startTime, finishTime, expiryTime, application from message_performance where name_timeunit = ? and startTime >= ? and startTime <= ?");
//		this.selectMessageExpirationStatement = this.session.prepare("select id, name, startTime, finishTime, expiryTime, application, name_timeunit from message_expiration where name_timeunit = ? and expiryTime >= ? and expiryTime <= ?");
//		this.selectApplicationCountsStatement = this.session.prepare("select application, incomingMessageRequestCount, incomingMessageDatagramCount, outgoingMessageRequestCount, outgoingMessageDatagramCount from application_counter where application_timeunit = ? and timeunit >= ? and timeunit <= ?");
//		this.selectEventCorrelations = this.session.prepare("select correlations from telemetry_event where id = ?");
//		this.selectEventExpirationDataStatement = this.session.prepare("select creationTime, type from telemetry_event where id = ?");
//		this.selectApplicationMessagesCountStatement = this.session.prepare("select timeunit, incomingMessageRequestCount, outgoingMessageRequestCount, incomingMessageResponseCount, outgoingMessageResponseCount, incomingMessageDatagramCount, outgoingMessageDatagramCount, incomingMessageResponseTime, outgoingMessageResponseTime from application_counter where application_timeunit = ? and timeunit >= ? and timeunit <= ?");
//		this.selectApplicationMessageNamesStatement = this.session.prepare("select timeunit, eventName, count from application_event_counter where application_timeunit = ? and timeunit >= ? and timeunit <= ?");
//		this.updateMessageExpirationStatement = this.session.prepare("update message_expiration set finishTime = ? where name_timeunit = ? and expiryTime = ? and id = ?");
	}
	
	public Map<String, Map<Long, Average>> getTransactionPerformanceStatistics(Long startTime, Long endTime, int maxTransactions, TimeUnit timeUnit) {
		if (startTime > endTime) {
			return Collections.emptyMap();
		}
//		List<String> transactionNames = getTransactionNameTimeframes(startTime, endTime);
//		if (transactionNames.size() == 0) {
//			return Collections.emptyMap();
//		}
//		final Map<String, Long> highest = new HashMap<String, Long>();
//		final Map<String, Map<Long, Average>> data = new HashMap<String, Map<Long, Average>>();
//		List<ResultSetFuture> resultSets = new ArrayList<ResultSetFuture>();
//		for (String transactionName : transactionNames) {
//			resultSets.add(this.session.executeAsync(this.selectTransactionPerformanceStatement.bind(transactionName, new Date(startTime), new Date(endTime))));
//		}
//		for (ResultSetFuture resultSetFuture : resultSets) {
//			ResultSet resultSet = resultSetFuture.getUninterruptibly();
//			Iterator<Row> iterator = resultSet.iterator();
//			while (iterator.hasNext()) {
//				Row row = iterator.next();
//				String transactionName = row.getString(0);
//				if (transactionName == null) {
//					continue;
//				}
//				Date transactionStartTime = row.getDate(1);
//				Date transactionFinishTime = row.getDate(2);
//				Date transactionExpiryTime = row.getDate(3);
//				long timeUnitValue = DateUtils.normalizeTime(transactionStartTime.getTime(), timeUnit.toMillis(1));
//				long responseTime = determineResponseTime(transactionStartTime, transactionFinishTime, transactionExpiryTime);
//				if (responseTime == -1) {
//						continue;
//				}
//				storeWhenHighest(highest, transactionName, responseTime);
//				addToTimeBasedAverageDataMap(data, transactionName, timeUnitValue, responseTime);
//			}
//		}
//		filterAveragesToMaxResults(maxTransactions, highest, data);
//		return data;
    }

	public Map<String, Map<Long, Average>> getMessagesPerformanceStatistics(Long startTime, Long endTime, int maxMessages, TimeUnit timeUnit) {
		if (startTime > endTime) {
			return Collections.emptyMap();
		}
		List<String> messageNames = getMessageNameTimeframes(startTime, endTime);
		if (messageNames.size() == 0) {
			return Collections.emptyMap();
		}
		final Map<String, Long> highest = new HashMap<String, Long>();
		final Map<String, Map<Long, Average>> data = new HashMap<String, Map<Long, Average>>();
		List<ResultSetFuture> resultSets = new ArrayList<ResultSetFuture>();
		for (String messageName : messageNames) {
			resultSets.add(this.session.executeAsync(this.selectMessagePerformanceStatement.bind(messageName, new Date(startTime), new Date(endTime))));
		}
		for (ResultSetFuture resultSetFuture : resultSets) {
			ResultSet resultSet = resultSetFuture.getUninterruptibly();
			Iterator<Row> iterator = resultSet.iterator();
			while (iterator.hasNext()) {
				Row row = iterator.next();
				String messageName = row.getString(0);
				if (messageName == null) {
					continue;
				}
				Date messageStartTime = row.getDate(1);
				Date messageFinishTime = row.getDate(2);
				Date messageExpiryTime = row.getDate(3);
				long timeUnitValue = DateUtils.normalizeTime(messageStartTime.getTime(), timeUnit.toMillis(1));
				long responseTime = determineResponseTime(messageStartTime, messageFinishTime, messageExpiryTime);
				if (responseTime == -1) {
						continue;
				}
				storeWhenHighest(highest, messageName, responseTime);
				addToTimeBasedAverageDataMap(data, messageName, timeUnitValue, responseTime);
			}		
		}
		filterAveragesToMaxResults(maxMessages, highest, data);
		return data;
    }
	
	public Map<String, Map<Long, Long>> getApplicationMessagesCountStatistics(String application, Long startTime, Long endTime, TimeUnit timeUnit) {
		if (startTime > endTime) {
			return Collections.emptyMap();
		}
		List<String> applicationNames = getApplicationNameTimeframes(application, startTime, endTime);
		if (applicationNames.size() == 0) {
			return Collections.emptyMap();
		}
		final Map<String, Map<Long, Long>> data = new HashMap<String, Map<Long, Long>>();
		List<ResultSetFuture> resultSets = new ArrayList<ResultSetFuture>();
		for (String applicationName : applicationNames) {
			resultSets.add(this.session.executeAsync(this.selectApplicationMessagesCountStatement.bind(applicationName, new Date(startTime), new Date(endTime))));
		}
		for (ResultSetFuture resultSetFuture : resultSets) {
			ResultSet resultSet = resultSetFuture.getUninterruptibly();
			Iterator<Row> iterator = resultSet.iterator();
			while (iterator.hasNext()) {
				Row row = iterator.next();
				long timeUnitValue = DateUtils.normalizeTime(row.getDate(0).getTime(), timeUnit.toMillis(1));
				addToTimeBasedCounterDataMap(data, "Incoming request messages", timeUnitValue, row.getLong(1));
				addToTimeBasedCounterDataMap(data, "Outgoing request messages", timeUnitValue, row.getLong(2));
				addToTimeBasedCounterDataMap(data, "Incoming response messages", timeUnitValue, row.getLong(3));
				addToTimeBasedCounterDataMap(data, "Outgoing response messages", timeUnitValue, row.getLong(4));
				addToTimeBasedCounterDataMap(data, "Incoming datagram messages", timeUnitValue, row.getLong(5));
				addToTimeBasedCounterDataMap(data, "Outgoing datagram messages", timeUnitValue, row.getLong(6));
				
			}
		}
	    return data;
    }
	
	public Map<String, Map<Long, Average>> getApplicationMessagesPerformanceStatistics(String application, Long startTime, Long endTime, TimeUnit timeUnit) {
		if (startTime > endTime) {
			return Collections.emptyMap();
		}
		if (application == null) {
			return Collections.emptyMap();
		}
		List<String> messageNames = getMessageNameTimeframes(startTime, endTime);
		if (messageNames.size() == 0) {
			return Collections.emptyMap();
		}
		final Map<String, Map<Long, Average>> data = new HashMap<String, Map<Long, Average>>();
		List<ResultSetFuture> resultSets = new ArrayList<ResultSetFuture>();
		for (String messageName : messageNames) {
			resultSets.add(this.session.executeAsync(this.selectMessagePerformanceStatement.bind(messageName, new Date(startTime), new Date(endTime))));
		}
		for (ResultSetFuture resultSetFuture : resultSets) {
			ResultSet resultSet = resultSetFuture.getUninterruptibly();
			Iterator<Row> iterator = resultSet.iterator();
			while (iterator.hasNext()) {
				Row row = iterator.next();
				if (!application.equals(row.getString(4))) {
					continue;
				}
				String messageName = row.getString(0);
				if (messageName == null) {
					continue;
				}
				Date messageStartTime = row.getDate(1);
				Date messageFinishTime = row.getDate(2);
				Date messageExpiryTime = row.getDate(3);
				long timeUnitValue = DateUtils.normalizeTime(messageStartTime.getTime(), timeUnit.toMillis(1));
				long responseTime = determineResponseTime(messageStartTime, messageFinishTime, messageExpiryTime);
				if (responseTime == -1) {
						continue;
				}
				addToTimeBasedAverageDataMap(data, messageName, timeUnitValue, responseTime);
			}		
		}
		return data;
    }
	
	public Map<String, Map<Long, Long>> getApplicationMessageNamesStatistics(String application, Long startTime, Long endTime, TimeUnit timeUnit) {
		if (startTime > endTime) {
			return Collections.emptyMap();
		}
		List<String> applicationNames = getApplicationNameTimeframes(application, startTime, endTime);
		if (applicationNames.size() == 0) {
			return Collections.emptyMap();
		}
		final Map<String, Map<Long, Long>> data = new HashMap<String, Map<Long, Long>>();
		List<ResultSetFuture> resultSets = new ArrayList<ResultSetFuture>();
		for (String applicationName : applicationNames) {
			resultSets.add(this.session.executeAsync(this.selectApplicationMessageNamesStatement.bind(applicationName, new Date(startTime), new Date(endTime))));
		}
		for (ResultSetFuture resultSetFuture : resultSets) {
			ResultSet resultSet = resultSetFuture.getUninterruptibly();
			Iterator<Row> iterator = resultSet.iterator();
			while (iterator.hasNext()) {
				Row row = iterator.next();
				long timeUnitValue = DateUtils.normalizeTime(row.getDate(0).getTime(), timeUnit.toMillis(1));
				String eventName = row.getString(1);
				addToTimeBasedCounterDataMap(data, eventName, timeUnitValue, row.getLong(2));
			}
		}
	    return data;
    }
	
	public List<ExpiredMessage> getApplicationMessagesExpirationStatistics(String application, Long startTime, Long endTime, int maxExpirations) {
		if (application == null) {
			return Collections.emptyList();
		}
		return getExpiredMessages(application, startTime, endTime, maxExpirations);
    }
	
	public List<ExpiredMessage> getMessagesExpirationStatistics(Long startTime, Long endTime, int maxExpirations) {
		return getExpiredMessages(null, startTime, endTime, maxExpirations);
    }
	
	private List<ExpiredMessage> getExpiredMessages(String application, Long startTime, Long endTime, int maxExpirations) {
		if (startTime > endTime) {
			return Collections.emptyList();
		}
		List<String> messageNames = getMessageNameTimeframes(startTime, endTime);
		if (messageNames.size() == 0) {
			return Collections.emptyList();
		}
		List<ExpiredMessage> expiredMessages =  new ArrayList<ExpiredMessage>();
		List<ResultSetFuture> resultSets = new ArrayList<ResultSetFuture>();
		for (String messageName : messageNames) {
			resultSets.add(this.session.executeAsync(this.selectMessageExpirationStatement.bind(messageName, new Date(startTime), new Date(endTime))));
		}
		for (ResultSetFuture resultSetFuture : resultSets) {
			ResultSet resultSet = resultSetFuture.getUninterruptibly();
			Iterator<Row> iterator = resultSet.iterator();
			while (iterator.hasNext()) {
				Row row = iterator.next();
				UUID id = row.getUUID(0);
				if (id == null) {
					continue;
				}
				String eventApplication = row.getString(5);
				if (application != null && !application.equals(eventApplication)) {
					continue;
				}
				String messageName = row.getString(1);
				if (messageName == null) {
					messageName = "undefined";
				}
				Date messageStartTime = row.getDate(2);
				Date messageFinishTime = row.getDate(3);
				Date messageExpiryTime = row.getDate(4);
				String rowKey = row.getString(6);
				if (messageFinishTime == null || messageFinishTime.getTime() == 0) {
					Row eventRow = this.session.execute(this.selectEventCorrelations.bind(id)).one();
					if (eventRow != null) {
						List<UUID> childIds = eventRow.getList(0, UUID.class);
						if (childIds != null) {
							for (UUID childId : childIds) {
								Row childRow = this.session.execute(this.selectEventExpirationDataStatement.bind(childId)).one();
								if (childRow != null) {
									TelemetryEventType type = null;
									try {
										type = TelemetryEventType.valueOf(childRow.getString(1));
									} catch (Exception e) {
										continue;
									}
									if (TelemetryEventType.MESSAGE_RESPONSE.equals(type)) {
										// False positive, update the expiration table
										messageFinishTime = childRow.getDate(0);
										this.session.executeAsync(this.updateMessageExpirationStatement.bind(messageFinishTime, rowKey, messageExpiryTime, id));
										break;
									}
								}
							}
						}
					}
				}
				if (messageExpiryTime != null && messageExpiryTime.getTime() > 0) {
					if ((messageFinishTime == null || messageFinishTime.getTime() == 0) && new Date().after(messageExpiryTime)) {
						synchronized (expiredMessages) {
							expiredMessages.add(new ExpiredMessage(id, messageName, messageStartTime, messageExpiryTime, eventApplication));
                        }
					} else if (messageFinishTime != null && messageFinishTime.getTime() > 0 && messageFinishTime.after(messageExpiryTime)) {
						synchronized (expiredMessages) {
							expiredMessages.add(new ExpiredMessage(id, messageName, messageStartTime, messageExpiryTime, eventApplication));
                        }
					}
				}
			}
		}
		return expiredMessages.stream().sorted((e1, e2) -> e2.getExpirationTime().compareTo(e1.getExpirationTime())).limit(maxExpirations).collect(Collectors.toList());		
	}
	
	public Map<String, Map<String, Long>> getApplicationCountStatistics(Long startTime, Long endTime, int maxApplications) {
		if (startTime > endTime) {
			return Collections.emptyMap();
		}
		List<String> applicationNames = getApplicationNameTimeframes(startTime, endTime);
		if (applicationNames.size() == 0) {
			return Collections.emptyMap();
		}
		final Map<String, Long> totals = new HashMap<String, Long>();
		final Map<String, Map<String, Long>> data = new HashMap<String, Map<String, Long>>();
		List<ResultSetFuture> resultSets = new ArrayList<ResultSetFuture>();
		
		for (String applicationName : applicationNames) {
			resultSets.add(this.session.executeAsync(this.selectApplicationCountsStatement.bind(applicationName, new Date(startTime), new Date(endTime))));
		}
		for (ResultSetFuture resultSetFuture : resultSets) {
			ResultSet resultSet = resultSetFuture.getUninterruptibly();
			Iterator<Row> iterator = resultSet.iterator();
			while (iterator.hasNext()) {
				Row row = iterator.next();
				String applicationName = row.getString(0);
				if (applicationName == null) {
					continue;
				}
				long incomingMessageRequestCount = row.getLong(1);
				long incomingMessageDatagramCount = row.getLong(2);
				long outgoingMessageRequestCount = row.getLong(3);
				long outgoingMessageDatagramCount = row.getLong(4);
				long total = incomingMessageRequestCount + incomingMessageDatagramCount + outgoingMessageRequestCount + outgoingMessageDatagramCount;
				if (total == 0) {
					continue;
				}
				if (!totals.containsKey(applicationName)) {
					totals.put(applicationName, total);
				} else {
					Long currentValue = totals.get(applicationName);
					totals.put(applicationName, currentValue + total);
				}
				if (!data.containsKey(applicationName)) {
					Map<String, Long> appTotals = new HashMap<String, Long>();
					appTotals.put("incomingMessageRequest", incomingMessageRequestCount);
					appTotals.put("incomingDatagramRequest", incomingMessageDatagramCount);
					appTotals.put("outgoingMessageRequest", outgoingMessageRequestCount);
					appTotals.put("outgoingDatagramRequest", outgoingMessageDatagramCount);
					data.put(applicationName, appTotals);
				} else {
					Map<String, Long> currentValues = data.get(applicationName);
					currentValues.put("incomingMessageRequest", currentValues.get("incomingMessageRequest") + incomingMessageRequestCount);
					currentValues.put("incomingDatagramRequest", currentValues.get("incomingDatagramRequest") + incomingMessageDatagramCount);
					currentValues.put("outgoingMessageRequest", currentValues.get("outgoingMessageRequest") + outgoingMessageRequestCount);
					currentValues.put("outgoingDatagramRequest", currentValues.get("outgoingDatagramRequest") + outgoingMessageDatagramCount);				
				}
			}
		}
		filterCountsToMaxResults(maxApplications, totals, data);
		return data;
    }
	
	private void filterCountsToMaxResults(int maxResults, Map<String, Long> totals, Map<String, Map<String, Long>> data) {
		List<Long> values = new ArrayList<>(totals.values().size());
		values.addAll(totals.values());
		Collections.sort(values);
		Collections.reverse(values);
		if (values.size() > maxResults) {
			for (int i = maxResults; i < values.size(); i++) {
				Long valueToRemove = values.get(i);
				for (String name : totals.keySet()) {
					if (totals.get(name).equals(valueToRemove)) {
						data.remove(name);
						totals.remove(name);
						break;
					}
				}
			}
		}
	}
	
	private void filterAveragesToMaxResults(int maxResults, Map<String, Long> highest, Map<String, Map<Long, Average>> data) {
		List<Long> values = new ArrayList<>(highest.values().size());
		values.addAll(highest.values());
		Collections.sort(values);
		Collections.reverse(values);
		if (values.size() > maxResults) {
			for (int i = maxResults; i < values.size(); i++) {
				Long valueToRemove = values.get(i);
				for (String name : highest.keySet()) {
					if (highest.get(name).equals(valueToRemove)) {
						data.remove(name);
						highest.remove(name);
						break;
					}
				}
			}
		}
	}
	
	private void addToTimeBasedCounterDataMap(Map<String, Map<Long, Long>> data, String key, Long timeUnitValue, Long count) {
		if (count == 0) {
			return;
		}
		if (!data.containsKey(key)) {
			Map<Long, Long> values = new HashMap<Long, Long>();
			values.put(timeUnitValue, new Long(count));
			data.put(key, values);
		} else {
			Map<Long, Long> values = data.get(key);
			if (!values.containsKey(timeUnitValue)) {
				values.put(timeUnitValue, new Long(count));
			} else {
				Long currentValue = values.get(timeUnitValue);
				values.put(timeUnitValue, new Long(currentValue + count));
			}
		}		
	}
	
	private void addToTimeBasedAverageDataMap(Map<String, Map<Long, Average>> data, String key, Long timeUnitValue, Long responseTime) {
		if (!data.containsKey(key)) {
			Map<Long, Average> values = new HashMap<Long, Average>();
			values.put(timeUnitValue, new Average(responseTime));
			data.put(key, values);
		} else {
			Map<Long, Average> values = data.get(key);
			if (!values.containsKey(timeUnitValue)) {
				values.put(timeUnitValue, new Average(responseTime));
			} else {
				Average currentValue = values.get(timeUnitValue);
				currentValue.add(responseTime);
			}
		}
	}
	
	private void storeWhenHighest(Map<String, Long> highest, String key, long value) {
		if (!highest.containsKey(key)) {
			highest.put(key, value);
		} else {
			Long currentValue = highest.get(key);
			if (value > currentValue) {
				highest.put(key, value);
			}
		}
    }
	
	private long determineResponseTime(Date startTime, Date finishTime, Date expiryTime) {
		if (finishTime != null && finishTime.getTime() != 0) {
			return finishTime.getTime() - startTime.getTime(); 
		} else if (expiryTime != null && expiryTime.getTime() != 0) {
			if (expiryTime.getTime() < System.currentTimeMillis()) {
				// transaction expired, set response time to expiry time.
				return expiryTime.getTime() - startTime.getTime();
			} else {
				// transaction not yet finished, and transaction not expired. Ignore this one.
				return -1;
			}
		} 
		// transaction not yet finished, and no expiry time available. Ignore this one.
		return -1;
	}
	
	private List<String> getTransactionNameTimeframes(long startTime, long endTime) {
		BuiltStatement builtStatement = QueryBuilder.select("name_timeframe").from("event_occurrences").where(QueryBuilder.in("timeunit", determineTimeFrame(startTime, endTime))).and(QueryBuilder.eq("type", "TransactionName"));
		List<String> transactionNames = new ArrayList<String>();
		ResultSet resultSet = this.session.execute(builtStatement);
		Iterator<Row> iterator = resultSet.iterator();
		while (iterator.hasNext()) {
			Row row = iterator.next();
			String name = row.getString(0);
			if (!transactionNames.contains(name)) {
				transactionNames.add(name);
			}
		}
		return transactionNames;
	}
	
	private List<String> getMessageNameTimeframes(long startTime, long endTime) {
		BuiltStatement builtStatement = QueryBuilder.select("name_timeframe").from("event_occurrences").where(QueryBuilder.in("timeunit", determineTimeFrame(startTime, endTime))).and(QueryBuilder.eq("type", "MessageName"));
		List<String> eventNames = new ArrayList<String>();
		ResultSet resultSet = this.session.execute(builtStatement);
		Iterator<Row> iterator = resultSet.iterator();
		while (iterator.hasNext()) {
			Row row = iterator.next();
			String name = row.getString(0);
			if (!eventNames.contains(name)) {
				eventNames.add(name);
			}
		}
		return eventNames;
	}
	
	private List<String> getApplicationNameTimeframes(long startTime, long endTime) {
		BuiltStatement builtStatement = QueryBuilder.select("name_timeframe").from("event_occurrences").where(QueryBuilder.in("timeunit", determineTimeFrame(startTime, endTime))).and(QueryBuilder.eq("type", "Application"));
		List<String> eventNames = new ArrayList<String>();
		ResultSet resultSet = this.session.execute(builtStatement);
		Iterator<Row> iterator = resultSet.iterator();
		while (iterator.hasNext()) {
			Row row = iterator.next();
			String name = row.getString(0);
			if (!eventNames.contains(name)) {
				eventNames.add(name);
			}
		}
		return eventNames;
	}
	
	private List<String> getApplicationNameTimeframes(String applicationName, long startTime, long endTime) {
		BuiltStatement builtStatement = QueryBuilder.select("name_timeframe", "name").from("event_occurrences").where(QueryBuilder.in("timeunit", determineTimeFrame(startTime, endTime))).and(QueryBuilder.eq("type", "Application"));
		List<String> eventNames = new ArrayList<String>();
		ResultSet resultSet = this.session.execute(builtStatement);
		Iterator<Row> iterator = resultSet.iterator();
		while (iterator.hasNext()) {
			Row row = iterator.next();
			String name = row.getString(0);
			if (!eventNames.contains(name) && applicationName.equals(row.getString(1))) {
				eventNames.add(name);
			}
		}
		return eventNames;
	}
	
	private List<Object> determineTimeFrame(long startTime, long endTime) {
	    Calendar startCalendar = Calendar.getInstance();
	    Calendar endCalendar = Calendar.getInstance();
	    startCalendar.setTimeInMillis(startTime);
	    endCalendar.setTimeInMillis(endTime);
	    // For unknown reasons cassandra gives an error when the arraylist is created with the Date generic type.
	    List<Object> result = new ArrayList<Object>();
	    do {
	    	result.add(new Date(DateUtils.normalizeTime(startCalendar.getTime().getTime(), PartitionKeySuffixCreator.SMALLEST_TIMUNIT_UNIT.toMillis(1))));
	    	startCalendar.add(PartitionKeySuffixCreator.SMALLEST_CALENDAR_UNIT, 1);
	    } while (startCalendar.before(endCalendar) || (!startCalendar.before(endCalendar) && startCalendar.get(PartitionKeySuffixCreator.SMALLEST_CALENDAR_UNIT) == endCalendar.get(PartitionKeySuffixCreator.SMALLEST_CALENDAR_UNIT)));
	    return result;
    }
}