package com.jecstar.etm.server.core.domain.converter.json;

import com.jecstar.etm.server.core.domain.converter.EndpointConfigurationTags;

public class EndpointConfigurationTagsJsonImpl implements EndpointConfigurationTags {

    @Override
    public String getEnhancerTag() {
        return "enhancer";
    }

    @Override
    public String getNameTag() {
        return "name";
    }

    @Override
    public String getEnhancerTypeTag() {
        return "type";
    }

    @Override
    public String getEnhancePayloadFormatTag() {
        return "enhance_payload_format";
    }

    @Override
    public String getFieldsTag() {
        return "fields";
    }

    @Override
    public String getWritePolicyTag() {
        return "write_policy";
    }

    @Override
    public String getParsersSourceTag() {
        return "parsers_source";
    }

    @Override
    public String getFieldTag() {
        return "field";
    }

    @Override
    public String getParsersTag() {
        return "parsers";
    }

    @Override
    public String getParserTag() {
        return "parser";
    }

    @Override
    public String getReplaceAllTag() {
        return "replace_all";
    }

    @Override
    public String getReplacementTag() {
        return "replacement";
    }

    @Override
    public String getTransformationsTag() {
        return "transformations";
    }
}
