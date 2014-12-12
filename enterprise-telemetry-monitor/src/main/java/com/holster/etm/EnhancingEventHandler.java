package com.holster.etm;

import java.util.List;

import com.holster.etm.parsers.ExpressionParser;
import com.holster.etm.repository.CorrelationBySourceIdResult;
import com.holster.etm.repository.EndpointConfigResult;
import com.holster.etm.repository.TelemetryEventRepository;
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
		if (event.eventTime.getTime() == 0) {
			event.eventTime.setTime(System.currentTimeMillis());
		}
		if (event.sourceCorrelationId != null && (event.correlationId == null || event.transactionId == null || event.transactionName == null)) {
			this.telemetryEventRepository.findParent(event.sourceCorrelationId, this.correlationBySourceIdResult.initialize());
			if (event.correlationId == null) {
				event.correlationId = this.correlationBySourceIdResult.id;
			}
			if (event.transactionId == null) {
				event.transactionId = this.correlationBySourceIdResult.transactionId;
			}
			if (event.transactionName == null) {
				event.transactionName = this.correlationBySourceIdResult.transactionName;
			}
			event.correlationTimeDifference = event.eventTime.getTime() - this.correlationBySourceIdResult.eventTime.getTime(); 
		}
		this.telemetryEventRepository.findEndpointConfig(event.endpoint, this.endpointConfigResult);
		if (event.endpoint != null && (event.application == null || event.eventName == null || event.eventDirection == null)) {
			if (event.application == null) {
				event.application = parseValue(this.endpointConfigResult.applicationParsers, event.content);
			}
			if (event.eventName == null && event.content != null) {
				event.eventName = parseValue(this.endpointConfigResult.eventNameParsers, event.content);
			}
			if (event.eventDirection == null) {
				event.eventDirection = this.endpointConfigResult.eventDirection;
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
