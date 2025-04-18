package com.now.nowbot.util;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.kotlin.KotlinFeature;
import com.fasterxml.jackson.module.kotlin.KotlinModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unchecked")
public class JacksonUtil {

    private static final Logger       log         = LoggerFactory.getLogger(JacksonUtil.class);
    public static final  ObjectMapper mapper      = JsonMapper.builder().build().registerModules(new JavaTimeModule());
    public static        TypeFactory  typeFactory = mapper.getTypeFactory();

    static {
        var ktMode = new KotlinModule.Builder()
                .enable(KotlinFeature.NullToEmptyCollection)
                .enable(KotlinFeature.NullToEmptyMap)
                .enable(KotlinFeature.NullIsSameAsDefault)
                .enable(KotlinFeature.SingletonSupport)
                .enable(KotlinFeature.StrictNullChecks)
                .enable(KotlinFeature.KotlinPropertyNameAsImplicitName)
                .enable(KotlinFeature.UseJavaDurationConversion)
                .build();
        mapper
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(DeserializationFeature.READ_ENUMS_USING_TO_STRING, true)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                // 设置可见性
                .setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY)
                // 默认使用驼峰转下划线命名
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .registerModules(ktMode)
                .registerModules(new JavaTimeModule())
        ;
    }

    public static <T> String objectToJsonPretty(T obj) {
        if (obj == null) {
            return null;
        }
        try {
            return obj instanceof String ? (String) obj : mapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (Exception e) {
            log.warn("Parse Object to Json error", e);
            return null;
        }
    }

    public static <T> String objectToJson(T obj) {
        if (obj == null) {
            return null;
        }
        try {
            return obj instanceof String ? (String) obj : mapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.warn("Parse Object to Json error", e);
            return null;
        }
    }

    public static <T> T jsonToObject(String src, Class<T> clazz) {
        if (src == null || src.trim().isEmpty() || clazz == null) {
            return null;
        }
        try {
            return clazz.isAssignableFrom(String.class) ? (T) src : mapper.readValue(src, clazz);
        } catch (Exception e) {
            log.warn("Parse Json to Object error", e);
            return null;
        }
    }

    public static String parseString(String body, String field) {
        JsonNode node;
        try {
            node = mapper.readTree(body);
            JsonNode leaf = node.get(field);
            if (leaf != null) {
                return leaf.asText();
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }


    public static List<String> parseStringList(String body, String field) {
        JsonNode node;
        try {
            node = mapper.readTree(body);
            JsonNode leaf = node.get(field);

            if (leaf != null) {
                var type = typeFactory.constructCollectionType(List.class, String.class);
                return mapper.convertValue(leaf, type);
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }


    public static Integer parseInteger(String body, String field) {
        JsonNode node;
        try {
            node = mapper.readTree(body);
            JsonNode leaf = node.get(field);
            if (leaf != null) {
                return leaf.asInt();
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    public static List<Integer> parseIntegerList(String body, String field) {
        JsonNode node;
        try {
            node = mapper.readTree(body);
            JsonNode leaf = node.get(field);

            if (leaf != null) {
                return mapper.convertValue(leaf, typeFactory.constructCollectionType(List.class, Integer.class));
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }


    public static Boolean parseBoolean(String body, String field) {
        JsonNode node;
        try {
            node = mapper.readTree(body);
            JsonNode leaf = node.get(field);
            if (leaf != null) {
                return leaf.asBoolean();
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    public static Short parseShort(String body, String field) {
        JsonNode node;
        try {
            node = mapper.readTree(body);
            JsonNode leaf = node.get(field);
            if (leaf != null) {
                int value = leaf.asInt();
                return (short) value;
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    public static Byte parseByte(String body, String field) {
        JsonNode node;
        try {
            node = mapper.readTree(body);
            JsonNode leaf = node.get(field);
            if (leaf != null) {
                int value = leaf.asInt();
                return (byte) value;
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    public static <T> T parseObject(String body, String field, Class<T> clazz) {
        JsonNode node;
        try {
            node = mapper.readTree(body);
            node = node.get(field);
            if (clazz == JsonNode.class) return (T) node;
            return mapper.treeToValue(node, clazz);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    public static <T> T parseObject(byte[] body, Class<T> clazz) {
        JsonNode node;
        try {
            node = mapper.readTree(body);
            if (clazz == JsonNode.class) return (T) node;
            return mapper.treeToValue(node, clazz);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    public static <T> T parseObject(String body, Class<T> clazz) {
        JsonNode node;
        try {
            node = mapper.readTree(body);
            if (clazz == JsonNode.class) return (T) node;
            return mapper.treeToValue(node, clazz);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    public static <T> T parseObject(String body, TypeReference<T> obj) {
        JsonNode node;
        try {
            node = mapper.readTree(body);
            return mapper.convertValue(node, obj);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    public static <T> T parseObject(JsonNode node, String field, Class<T> clazz) {
        try {
            node = node.get(field);
            return mapper.treeToValue(node, clazz);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    public static <T> T parseObject(JsonNode node, Class<T> clazz) {
        try {
            return mapper.treeToValue(node, clazz);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    public static Object toNode(String json) {
        if (json == null) {
            return null;
        }
        try {

            return mapper.readTree(json);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }

        return null;
    }

    public static Map<String, String> toMap(String data) {
        try {
            return mapper.readValue(data, typeFactory.constructMapType(Map.class, String.class, String.class));
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    public static <T> T toObj(String data, Class<T> clazz) {
        try {
            return mapper.readValue(data, clazz);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    public static String toJson(Object data) {
        return objectToJson(data);
    }

    public static <T> List<T> parseObjectList(String body, Class<T> clazz) {

        JsonNode node = (JsonNode) toNode(body);
        if (node != null) {
            return parseObjectList(node, clazz);
        }
        return null;
    }

    public static <T> List<T> parseObjectList(JsonNode body, Class<T> clazz) {

        if (body != null && body.isArray()) {
            CollectionType collectionType = typeFactory.constructCollectionType(List.class, clazz);
            return mapper.convertValue(body, collectionType);
        }
        return null;
    }

    public static String parseSubnodeToString(String body, String field) {
        JsonNode node;
        try {
            node = mapper.readTree(body);
            JsonNode leaf = node.at(field);
            if (leaf != null) {
                return leaf.toString();
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }
}