package com.holster.etm.processor.processor;

import java.util.List;

import com.holster.etm.core.TelemetryEventType;
import com.holster.etm.processor.TelemetryEvent;
import com.holster.etm.processor.parsers.ExpressionParser;
import com.holster.etm.processor.repository.CorrelationBySourceIdResult;
import com.holster.etm.processor.repository.EndpointConfigResult;
import com.holster.etm.processor.repository.TelemetryEventRepository;
import com.lmax.disruptor.EventHandler;

public class EnhancingEventHandler implements EventHandler<TelemetryEvent> {

	
	private final long ordinal;
	private final long numberOfConsumers;
	
	private final TelemetryEventRepository telemetryEventRepository;
	private final CorrelationBySourceIdResult correlationBySourceIdResult;
	private final EndpointConfigResult endpointConfigResult;
	
	public EnhancingEventHandler(final TelemetryEventRepository telemetryEventRepository, final long ordinal, final long numberOfConsumers) {
		this.telemetryEventRepository = telemetryEventRepository;
		this.ordinal = ordinal;
		this.numberOfConsumers = numberOfConsumers;
		this.correlationBySourceIdResult = new CorrelationBySourceIdResult();
		this.endpointConfigResult = new EndpointConfigResult();
	}

	@Override
	public void onEvent(final TelemetryEvent event, final long sequence, final boolean endOfBatch) throws Exception {
		if (event.ignore || (sequence % this.numberOfConsumers) != this.ordinal) {
			return;
		}
//		long start = System.nanoTime();
		if (needsCorrelation(event)) {
			// Find the correlation event.
			this.telemetryEventRepository.findParent(event.sourceCorrelationId, this.correlationBySourceIdResult.initialize());
			if (event.correlationId == null) {
				event.correlationId = this.correlationBySourceIdResult.id;
			}
			if (TelemetryEventType.MESSAGE_RESPONSE.equals(event.type)) {
				// if this is a response, set the correlating data from the request on the response.
				if (event.transactionId == null) {
					event.transactionId = this.correlationBySourceIdResult.transactionId;
				}
				if (event.transactionName == null) {
					event.transactionName = this.correlationBySourceIdResult.transactionName;
				}
				if (event.correlationCreationTime.getTime() == 0) {
					event.correlationCreationTime.setTime(this.correlationBySourceIdResult.creationTime.getTime());
				}
				if (event.correlationExpiryTime.getTime() == 0) {
					event.correlationExpiryTime.setTime(this.correlationBySourceIdResult.expiryTime.getTime());
				}
				if (event.correlationName == null) {
					event.correlationName = this.correlationBySourceIdResult.name;
				}
			
			}
		}
		this.telemetryEventRepository.findEndpointConfig(event.endpoint, this.endpointConfigResult);
		if (event.endpoint != null && (event.application == null || event.name == null || event.direction == null || event.transactionName == null)) {
			if (event.application == null) {
				event.application = parseValue(this.endpointConfigResult.applicationParsers, event.content);
			}
			if (event.name == null && event.content != null) {
				event.name = parseValue(this.endpointConfigResult.eventNameParsers, event.content);
			}
			if (event.direction == null) {
				event.direction = this.endpointConfigResult.eventDirection;
			}
			if (event.transactionName == null) {
				event.transactionName = parseValue(this.endpointConfigResult.transactionNameParsers, event.content);
				if (event.transactionName != null) {
					event.transactionId = event.id;
				}
			}
 		}
		if (!this.endpointConfigResult.correlationDataParsers.isEmpty()) {
			this.endpointConfigResult.correlationDataParsers.forEach((k,v) -> {
				String parsedValue = parseValue(v, event.content);
				if (parsedValue != null) {
					event.correlationData.put(k, parsedValue);
				}
			});
		}
//		Statistics.enhancingTime.addAndGet(System.nanoTime() - start);
	}
	
	private boolean needsCorrelation(final TelemetryEvent event) {
		if (event.sourceCorrelationId != null && event.correlationId == null) {
			return true;
		} else if (event.sourceCorrelationId != null
		        && ((event.transactionId == null || event.transactionName == null || event.correlationCreationTime.getTime() == 0 || event.correlationExpiryTime
		                .getTime() == 0) && TelemetryEventType.MESSAGE_RESPONSE.equals(event.type))) {
			return true;
		}
		return false;
	}

	private String parseValue(List<ExpressionParser> expressionParsers, String content) {
		if (content == null || expressionParsers == null) {
			return null;
		}
		for (ExpressionParser expressionParser : expressionParsers) {
			String value = parseValue(expressionParser, content);
			if (value != null) {
				return value;
			}
		}
		return null;
    }
	
	private String parseValue(ExpressionParser expressionParser, String content) {
		if (expressionParser == null || content == null) {
			return null;
		}
		String value = expressionParser.evaluate(content);
		if (value != null && value.trim().length() > 0) {
			return value;
		}
		return null;
	}
}
