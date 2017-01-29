package com.jecstar.etm.server.core.domain.converter.json;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.jecstar.etm.server.core.domain.EndpointConfiguration;
import com.jecstar.etm.server.core.domain.converter.EndpointConfigurationConverter;
import com.jecstar.etm.server.core.domain.converter.EndpointConfigurationTags;
import com.jecstar.etm.server.core.enhancers.DefaultTelemetryEventEnhancer;
import com.jecstar.etm.server.core.enhancers.TelemetryEventEnhancer;
import com.jecstar.etm.server.core.logging.LogFactory;
import com.jecstar.etm.server.core.logging.LogWrapper;
import com.jecstar.etm.server.core.parsers.ExpressionParser;

public class EndpointConfigurationConverterJsonImpl implements EndpointConfigurationConverter<String> {

	/**
	 * The <code>LogWrapper</code> for this class.
	 */
	private static final LogWrapper log = LogFactory.getLogger(EndpointConfigurationConverterJsonImpl.class);
	private static final String DEFAULT_ENHANCER_TYPE = "DEFAULT";
	
	private final EndpointConfigurationTags tags = new EndpointConfigurationTagsJsonImpl();
	private final JsonConverter converter = new JsonConverter();
	
	private final ExpressionParserConverterJsonImpl expressionParserConverter = new ExpressionParserConverterJsonImpl();

	@Override
	public EndpointConfiguration read(String content) {
		return read(this.converter.toMap(content));
	}
	
	public EndpointConfiguration read(Map<String, Object> valueMap) {
		EndpointConfiguration endpointConfiguration = new EndpointConfiguration();
		endpointConfiguration.name = this.converter.getString(this.tags.getNameTag(), valueMap);
		Map<String, Object> enhancerValues = this.converter.getObject(this.tags.getEnhancerTag(), valueMap);
		if (!enhancerValues.isEmpty()) {
			String enhancerType = this.converter.getString(this.tags.getEnhancerTypeTag(), enhancerValues);
			if (!DEFAULT_ENHANCER_TYPE.equals(enhancerType)) {
				try {
					Class<?> clazz = Class.forName(enhancerType);
					Object newInstance = clazz.newInstance();
					endpointConfiguration.eventEnhancer = (TelemetryEventEnhancer) newInstance;
				} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | ClassCastException e ) {
					if (log.isErrorLevelEnabled()) {
						log.logErrorMessage("Failed to load custom enhancer '" + enhancerType + "'. Make sure the class is spelled correct and available on all processor nodes.", e);
					}
				}
			} else {
				endpointConfiguration.eventEnhancer = readDefaultEnhancer(enhancerValues);
			}
		}
		return endpointConfiguration;		
	}
	
	private DefaultTelemetryEventEnhancer readDefaultEnhancer(Map<String, Object> enhancerValues) {
		DefaultTelemetryEventEnhancer enhancer = new DefaultTelemetryEventEnhancer();
		enhancer.setEnhancePayloadFormat(this.converter.getBoolean(this.tags.getEnhancePayloadFormatTag(), enhancerValues, true));
		List<Map<String, Object>> fields = this.converter.getArray(this.tags.getFieldsTag(), enhancerValues);
		if (fields != null) {
			for (Map<String, Object> fieldValues : fields) {
				String fieldName = this.converter.getString(this.tags.getFieldTag(), fieldValues);
				List<ExpressionParser> expressionParsers = new ArrayList<>();
				List<Map<String, Object>> parsers = this.converter.getArray(this.tags.getParsersTag(), fieldValues);
				if (parsers != null) {
					for (Map<String, Object> parserValues : parsers) {
						expressionParsers.add(this.expressionParserConverter.read(parserValues));
					}
				}
				enhancer.addField(fieldName, expressionParsers);
			}
		}
		return enhancer;
	}

	@Override
	public String write(EndpointConfiguration endpointConfiguration) {
		StringBuilder result = new StringBuilder();
		result.append("{");
		this.converter.addStringElementToJsonBuffer(this.tags.getNameTag(), endpointConfiguration.name, result, true);
		if (endpointConfiguration.eventEnhancer != null) {
			result.append(",\"" + this.tags.getEnhancerTag() + "\": {");
			if (endpointConfiguration.eventEnhancer instanceof DefaultTelemetryEventEnhancer) {
				DefaultTelemetryEventEnhancer enhancer = (DefaultTelemetryEventEnhancer) endpointConfiguration.eventEnhancer;
				this.converter.addStringElementToJsonBuffer(this.tags.getEnhancerTypeTag(), DEFAULT_ENHANCER_TYPE, result, true);
				this.converter.addBooleanElementToJsonBuffer(this.tags.getEnhancePayloadFormatTag(), enhancer.isEnhancePayloadFormat(), result, false);
				result.append(",");
				result.append("\"" + this.tags.getFieldsTag() + "\": [");
				boolean first = true;
				for (Entry<String, List<ExpressionParser>> entry : enhancer.getFields().entrySet()) {
					if (entry.getValue() != null) {
						if (!first) {
							result.append(",");
						}
						result.append("{");
						this.converter.addStringElementToJsonBuffer(this.tags.getFieldTag(), entry.getKey(), result, true);
						result.append(",\"" + this.tags.getParsersTag() + "\": [");
						boolean firstParser = true;
						for (ExpressionParser expressionParser : entry.getValue()) {
							if (!firstParser) {
								result.append(",");
							}
							result.append(this.expressionParserConverter.write(expressionParser));
							firstParser = false;
						}
						result.append("]}");
						first = false;
					}
				}
				result.append("]");
			} else {
				this.converter.addStringElementToJsonBuffer(this.tags.getEnhancerTypeTag(), endpointConfiguration.getClass().getName(), result, true);
			}
			result.append("}");
		}
		result.append("}");
		return result.toString();
	}

	@Override
	public EndpointConfigurationTags getTags() {
		return this.tags;
	}

}