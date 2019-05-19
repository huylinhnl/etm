package com.jecstar.etm.server.core.domain.converter.json;

import com.jecstar.etm.domain.writer.TelemetryEventTags;
import com.jecstar.etm.domain.writer.json.TelemetryEventTagsJsonImpl;
import com.jecstar.etm.server.core.domain.ImportProfile;
import com.jecstar.etm.server.core.domain.configuration.ElasticsearchLayout;
import com.jecstar.etm.server.core.domain.converter.ImportProfileConverter;
import com.jecstar.etm.server.core.domain.converter.ImportProfileTags;
import com.jecstar.etm.server.core.domain.parser.ExpressionParser;
import com.jecstar.etm.server.core.domain.parser.converter.json.ExpressionParserConverterJsonImpl;
import com.jecstar.etm.server.core.enhancers.DefaultField;
import com.jecstar.etm.server.core.enhancers.DefaultField.WritePolicy;
import com.jecstar.etm.server.core.enhancers.DefaultTelemetryEventEnhancer;
import com.jecstar.etm.server.core.enhancers.DefaultTransformation;
import com.jecstar.etm.server.core.enhancers.TelemetryEventEnhancer;
import com.jecstar.etm.server.core.logging.LogFactory;
import com.jecstar.etm.server.core.logging.LogWrapper;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ImportProfileConverterJsonImpl implements ImportProfileConverter<String> {

    /**
     * The <code>LogWrapper</code> for this class.
     */
    private static final LogWrapper log = LogFactory.getLogger(ImportProfileConverterJsonImpl.class);
    private static final String DEFAULT_ENHANCER_TYPE = "DEFAULT";

    private final ImportProfileTags tags = new ImportProfileTagsJsonImpl();
    private final TelemetryEventTags eventTags = new TelemetryEventTagsJsonImpl();
    private final JsonConverter converter = new JsonConverter();

    private final ExpressionParserConverterJsonImpl expressionParserConverter = new ExpressionParserConverterJsonImpl();

    @Override
    public ImportProfile read(String content) {
        return read(this.converter.toMap(content));
    }

    public ImportProfile read(Map<String, Object> valueMap) {
        valueMap = this.converter.getObject(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_IMPORT_PROFILE, valueMap);
        ImportProfile importProfile = new ImportProfile();
        importProfile.name = this.converter.getString(this.tags.getNameTag(), valueMap);
        Map<String, Object> enhancerValues = this.converter.getObject(this.tags.getEnhancerTag(), valueMap);
        if (enhancerValues != null && !enhancerValues.isEmpty()) {
            String enhancerType = this.converter.getString(this.tags.getEnhancerTypeTag(), enhancerValues);
            if (!DEFAULT_ENHANCER_TYPE.equals(enhancerType)) {
                try {
                    Class<?> clazz = Class.forName(enhancerType);
                    Object newInstance = clazz.getDeclaredConstructor().newInstance();
                    importProfile.eventEnhancer = (TelemetryEventEnhancer) newInstance;
                } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | ClassCastException | NoSuchMethodException | InvocationTargetException e) {
                    if (log.isErrorLevelEnabled()) {
                        log.logErrorMessage("Failed to load custom enhancer '" + enhancerType + "'. Make sure the class is spelled correct and available on all processor nodes.", e);
                    }
                }
            } else {
                importProfile.eventEnhancer = readDefaultEnhancer(enhancerValues);
            }
        }
        return importProfile;
    }

    private DefaultTelemetryEventEnhancer readDefaultEnhancer(Map<String, Object> enhancerValues) {
        DefaultTelemetryEventEnhancer enhancer = new DefaultTelemetryEventEnhancer();
        enhancer.setEnhancePayloadFormat(this.converter.getBoolean(this.tags.getEnhancePayloadFormatTag(), enhancerValues, true));
        List<Map<String, Object>> fields = this.converter.getArray(this.tags.getFieldsTag(), enhancerValues);
        if (fields != null) {
            for (Map<String, Object> fieldValues : fields) {
                String fieldName = this.converter.getString(this.tags.getFieldTag(), fieldValues);
                DefaultField field = new DefaultField(fieldName);
                List<ExpressionParser> expressionParsers = new ArrayList<>();
                List<Map<String, Object>> parsers = this.converter.getArray(this.tags.getParsersTag(), fieldValues);
                if (parsers != null) {
                    for (Map<String, Object> parserValues : parsers) {
                        Map<String, Object> objectMap = new HashMap<>();
                        objectMap.put(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_PARSER, parserValues);
                        expressionParsers.add(this.expressionParserConverter.read(objectMap));
                    }
                }
                field.addParsers(expressionParsers);
                field.setWritePolicy(WritePolicy.safeValueOf(this.converter.getString(this.tags.getWritePolicyTag(), fieldValues)));
                field.setParsersSource(this.converter.getString(this.tags.getParsersSourceTag(), fieldValues, eventTags.getPayloadTag()));
                enhancer.addField(field);
            }
        }
        List<Map<String, Object>> transformations = this.converter.getArray(this.tags.getTransformationsTag(), enhancerValues);
        if (transformations != null) {
            for (Map<String, Object> transformationValues : transformations) {
                DefaultTransformation transformation = new DefaultTransformation();

                Map<String, Object> parserValues = this.converter.getObject(this.tags.getParserTag(), transformationValues);
                Map<String, Object> objectMap = new HashMap<>();
                objectMap.put(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_PARSER, parserValues);
                transformation.setExpressionParser(this.expressionParserConverter.read(objectMap));

                transformation.setReplacement(this.converter.getString(this.tags.getReplacementTag(), transformationValues));
                transformation.setReplaceAll(this.converter.getBoolean(this.tags.getReplaceAllTag(), transformationValues));
                enhancer.addTransformation(transformation);
            }
        }
        return enhancer;
    }

    @SuppressWarnings("unchecked")
    @Override
    public String write(ImportProfile importProfile) {
        StringBuilder result = new StringBuilder();
        result.append("{");
        this.converter.addStringElementToJsonBuffer(ElasticsearchLayout.ETM_TYPE_ATTRIBUTE_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_IMPORT_PROFILE, result, true);
        result.append(", " + this.converter.escapeToJson(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_IMPORT_PROFILE, true) + ": {");
        this.converter.addStringElementToJsonBuffer(this.tags.getNameTag(), importProfile.name, result, true);
        if (importProfile.eventEnhancer != null) {
            result.append(",\"").append(this.tags.getEnhancerTag()).append("\": {");
            if (importProfile.eventEnhancer instanceof DefaultTelemetryEventEnhancer) {
                DefaultTelemetryEventEnhancer enhancer = (DefaultTelemetryEventEnhancer) importProfile.eventEnhancer;
                this.converter.addStringElementToJsonBuffer(this.tags.getEnhancerTypeTag(), DEFAULT_ENHANCER_TYPE, result, true);
                this.converter.addBooleanElementToJsonBuffer(this.tags.getEnhancePayloadFormatTag(), enhancer.isEnhancePayloadFormat(), result, false);
                result.append(",");
                result.append(this.converter.escapeToJson(this.tags.getFieldsTag(), true)).append(": [");
                boolean first = true;
                for (DefaultField field : enhancer.getFields()) {
                    if (!first) {
                        result.append(",");
                    }
                    result.append("{");
                    this.converter.addStringElementToJsonBuffer(this.tags.getFieldTag(), field.getName(), result, true);
                    this.converter.addStringElementToJsonBuffer(this.tags.getWritePolicyTag(), field.getWritePolicy().name(), result, false);
                    this.converter.addStringElementToJsonBuffer(this.tags.getParsersSourceTag(), field.getParsersSource(), result, false);
                    result.append(",\"").append(this.tags.getParsersTag()).append("\": [");
                    boolean firstParser = true;
                    for (ExpressionParser expressionParser : field.getParsers()) {
                        if (!firstParser) {
                            result.append(",");
                        }
                        Map<String, Object> objectMap = this.converter.toMap(this.expressionParserConverter.write(expressionParser));
                        result.append(this.converter.toString((Map<String, Object>) objectMap.get(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_PARSER)));
                        firstParser = false;
                    }
                    result.append("]}");
                    first = false;
                }
                result.append("],");
                result.append(this.converter.escapeToJson(this.tags.getTransformationsTag(), true)).append(": [");
                first = true;
                for (DefaultTransformation transformation : enhancer.getTransformations()) {
                    if (!first) {
                        result.append(",");
                    }
                    result.append("{");
                    this.converter.addStringElementToJsonBuffer(this.tags.getReplacementTag(), transformation.getReplacement(), true, result, true);
                    this.converter.addBooleanElementToJsonBuffer(this.tags.getReplaceAllTag(), transformation.isReplaceAll(), result, false);
                    result.append(", " + this.converter.escapeToJson(this.tags.getParserTag(), true) + ": ");
                    Map<String, Object> objectMap = this.converter.toMap(this.expressionParserConverter.write(transformation.getExpressionParser()));
                    result.append(this.converter.toString((Map<String, Object>) objectMap.get(ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_PARSER)));
                    result.append("}");
                    first = false;
                }
                result.append("]");
            } else {
                this.converter.addStringElementToJsonBuffer(this.tags.getEnhancerTypeTag(), importProfile.getClass().getName(), result, true);
            }
            result.append("}");
        }
        result.append("}}");
        return result.toString();
    }

    @Override
    public ImportProfileTags getTags() {
        return this.tags;
    }

}