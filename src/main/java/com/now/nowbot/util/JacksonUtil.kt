package com.now.nowbot.util

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.PropertyAccessor
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.JsonNode
import tools.jackson.databind.PropertyNamingStrategies
import tools.jackson.databind.introspect.VisibilityChecker
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.type.TypeFactory
import tools.jackson.module.kotlin.KotlinFeature
import tools.jackson.module.kotlin.KotlinModule
import tools.jackson.module.kotlin.readValue
import tools.jackson.module.kotlin.treeToValue

object JacksonUtil {
    private val log: Logger = LoggerFactory.getLogger(JacksonUtil::class.java)

    // 1. 将 KotlinModule 的配置抽离为一个私有静态方法或直接内联
    private val KOTLIN_MODULE = KotlinModule.Builder()
        .enable(KotlinFeature.NullToEmptyCollection)
        .enable(KotlinFeature.NullToEmptyMap)

        .enable(KotlinFeature.NullIsSameAsDefault)
        .disable(KotlinFeature.StrictNullChecks)

        .enable(KotlinFeature.SingletonSupport)

        .enable(KotlinFeature.KotlinPropertyNameAsImplicitName)
        .enable(KotlinFeature.UseJavaDurationConversion)
        .build()

    val mapper: JsonMapper = JsonMapper.builder()
        .changeDefaultPropertyInclusion { value: JsonInclude.Value ->
            value.withValueInclusion(
                JsonInclude.Include.NON_NULL
            )
        }
        .enumNamingStrategy { _, _, name -> name }
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
        .enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT) // 可见性
        .changeDefaultVisibility { vc: VisibilityChecker -> vc
            .withVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE) // 1. 先全部关掉
            .withVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.PUBLIC_ONLY) // 2. 只开公开字段
            .withVisibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.PUBLIC_ONLY) // 3. 只开公开 Getter
            .withVisibility(PropertyAccessor.IS_GETTER, JsonAutoDetect.Visibility.PUBLIC_ONLY)
            .withVisibility(PropertyAccessor.SETTER, JsonAutoDetect.Visibility.PUBLIC_ONLY)

            // 也许需要收紧，不然 lazy 委托会扫到 synchronized 自指锁上
//            vc.withVisibility(
//                PropertyAccessor.FIELD, JsonAutoDetect.Visibility.PUBLIC_ONLY
//            )
        }
        .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
        .addModule(KOTLIN_MODULE)
        .build()

    var typeFactory: TypeFactory = mapper.typeFactory

    fun <T> objectToJsonPretty(obj: T?): String {
        if (obj == null) {
            return "null"
        }

        try {
            return if (obj is String) {
                obj as String
            } else {
                mapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj)
            }
        } catch (e: RuntimeException) {
            log.warn("Parse Object to Json error", e)
            return "null"
        }
    }

    fun <T> objectToJson(obj: T?): String {
        if (obj == null) {
            return "null"
        }

        try {
            return obj as? String ?: mapper.writeValueAsString(obj)
        } catch (e: RuntimeException) {
            log.warn("Parse Object to Json error", e)
            return "null"
        }
    }

    inline fun <reified T> jsonToObject(src: String?, clazz: Class<T>?): T? {
        if (src == null || src.trim { it <= ' ' }.isEmpty() || clazz == null) {
            return null
        }

        return if (clazz.isAssignableFrom(String::class.java)) {
            src as? T
        } else {
            mapper.readValue<T>(src, clazz)
        }
    }

    fun parseString(body: String, field: String): String? {
        val node: JsonNode
        try {
            node = mapper.readTree(body)
            val leaf = node.get(field)
            if (leaf != null) {
                return leaf.asString()
            }
        } catch (e: RuntimeException) {
            log.error(e.message, e)
        }
        return null
    }


    fun parseStringList(body: String, field: String): MutableList<String>? {
        val node: JsonNode
        try {
            node = mapper.readTree(body)
            val leaf = node.get(field)

            if (leaf != null) {
                val type = typeFactory.constructCollectionType(MutableList::class.java, String::class.java)
                return mapper.convertValue<MutableList<String>>(leaf, type)
            }
        } catch (e: RuntimeException) {
            log.error(e.message, e)
        }
        return null
    }


    fun parseInteger(body: String, field: String): Int? {
        val node: JsonNode
        try {
            node = mapper.readTree(body)
            val leaf = node.get(field)
            if (leaf != null) {
                return leaf.asInt()
            }
        } catch (e: RuntimeException) {
            log.error(e.message, e)
        }
        return null
    }

    fun parseIntegerList(body: String, field: String): MutableList<Int>? {
        val node: JsonNode
        try {
            node = mapper.readTree(body)
            val leaf = node.get(field)

            if (leaf != null) {
                return mapper.convertValue<MutableList<Int>>(
                    leaf,
                    typeFactory.constructCollectionType(MutableList::class.java, Int::class.java)
                )
            }
        } catch (e: RuntimeException) {
            log.error(e.message, e)
        }
        return null
    }


    fun parseBoolean(body: String, field: String): Boolean? {
        val node: JsonNode
        try {
            node = mapper.readTree(body)
            val leaf = node.get(field)
            if (leaf != null) {
                return leaf.asBoolean()
            }
        } catch (e: RuntimeException) {
            log.error(e.message, e)
        }
        return null
    }

    fun parseShort(body: String, field: String): Short? {
        val node: JsonNode
        try {
            node = mapper.readTree(body)
            val leaf = node.get(field)
            if (leaf != null) {
                val value = leaf.asInt()
                return value.toShort()
            }
        } catch (e: RuntimeException) {
            log.error(e.message, e)
        }
        return null
    }

    fun parseByte(body: String, field: String): Byte? {
        val node: JsonNode
        try {
            node = mapper.readTree(body)
            val leaf = node.get(field)
            if (leaf != null) {
                val value = leaf.asInt()
                return value.toByte()
            }
        } catch (e: RuntimeException) {
            log.error(e.message, e)
        }
        return null
    }

    inline fun <reified T> parseObject(body: String, field: String): T? {
        val node: JsonNode = mapper.readTree(body).get(field)
        if (T::class.java == JsonNode::class.java) return node as? T
        return mapper.treeToValue<T>(node)
    }

    inline fun <reified T> parseObject(body: ByteArray): T? {
        val node: JsonNode = mapper.readTree(body)
        if (T::class.java == JsonNode::class.java) return node as? T
        return mapper.treeToValue<T>(node)
    }

    inline fun <reified T> parseObject(body: String): T? {
        return mapper.readValue<T>(body)
    }

    inline fun <reified T> parseObject(node: JsonNode?, field: String): T? {
        if (node == null || node.isNull) return null
        val parser = mapper.treeAsTokens(node.get(field))
        return mapper.readValue<T>(parser)
    }

    inline fun <reified T> parseObject(node: JsonNode?): T? {
        if (node == null || node.isNull) return null
        val parser = mapper.treeAsTokens(node)
        return mapper.readValue<T>(parser)
    }

    fun toNode(json: String?): JsonNode {
        return mapper.readTree(json)
    }

    fun toNode(byteArray: ByteArray?): JsonNode {
        return mapper.readTree(byteArray)
    }

    fun toMap(data: String?): MutableMap<String, String>? {
        try {
            return mapper.readValue<MutableMap<String, String>>(
                data,
                typeFactory.constructMapType(MutableMap::class.java, String::class.java, String::class.java)
            )
        } catch (e: Exception) {
            log.error(e.message, e)
        }
        return null
    }

    inline fun <reified T> toValue(data: String): T? {
        return mapper.readValue<T>(data)
    }

    fun toJson(data: Any?): String {
        return objectToJson(data)
    }

    fun <T> parseObjectList(body: String?, clazz: Class<T>): List<T> {
        val node = toNode(body)
        return parseObjectList(node, clazz)
    }

    fun <T> parseObjectList(body: JsonNode?, clazz: Class<T>): List<T> {
        if (body != null && body.isArray) {
            val collectionType = typeFactory.constructCollectionType(MutableList::class.java, clazz)
            return mapper.convertValue<List<T>>(body, collectionType)
        }
        return listOf()
    }

    fun parseSubnodeToString(body: String?, field: String?): String? {
        val node: JsonNode
        try {
            node = mapper.readTree(body)
            val leaf = node.at(field)
            if (leaf != null) {
                return leaf.toString()
            }
        } catch (e: RuntimeException) {
            log.error(e.message, e)
        }
        return null
    }

    inline fun <reified T> String.toTypedObject(): T? {
        return parseObject<T>(this)
    }

    inline fun <reified T> JsonNode.json(): T? {
        if (this.isMissingNode || this.isNull) return null

        return mapper.convertValue(this, T::class.java)
    }
}