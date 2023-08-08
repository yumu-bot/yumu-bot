package com.now.nowbot.model.yaml;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.util.List;

public class IndexYaml {
    static ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    static {
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    }

    String version = "0.6";

    String indexId;

    DocSetter docMapping;

    IndexSetter indexSettings;

    String defaultSearchFields;

    public static class DocSetter {

        String name;

        String description;

        /**
         * text, i64, u64, f64, datetime, bool, ip, bytes
         */
        String type;

        String tokenizer = "chinese_compatible";

        String record = "position";

        Boolean fieldnorms = true;


    }

    public static class IndexSetter {

        String timestampField;
    }

    public static class searchSetter {

        List<String> defaultSearchFields;
    }
}
