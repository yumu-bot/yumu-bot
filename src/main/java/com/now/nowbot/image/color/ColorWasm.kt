package com.now.nowbot.image.color

import com.dylibso.chicory.runtime.ExecutionListener
import com.dylibso.chicory.runtime.HostFunction
import com.dylibso.chicory.runtime.ImportValues
import com.dylibso.chicory.runtime.Instance
import com.dylibso.chicory.runtime.WasmFunctionHandle
import com.dylibso.chicory.wasm.Parser
import com.dylibso.chicory.wasm.WasmModule
import com.dylibso.chicory.wasm.types.FunctionType
import com.dylibso.chicory.wasm.types.MemoryLimits
import com.dylibso.chicory.wasm.types.ValType
import com.dylibso.chicory.wasm.types.Value
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import java.io.ByteArrayInputStream
import java.security.MessageDigest
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * Runs the original img-color wasm module in Chicory. The module is unchanged; only the
 * wasm-bindgen host shim (three imports and the Rgb pointer/externref table) is adapted.
 */
object ColorWasm {
    private const val MODULE_PATH = "/wasm/img-color.wasm"
    private const val MODULE_SHA256 = "c2019bba2fec844be621c3ba3f813ddc056518d7faf222a699ed7f653b4584a4"
    private const val MAX_INPUT_BYTES = 16 * 1024 * 1024
    private const val MAX_MEMORY_PAGES = 4096
    private const val MAX_INSTANCES = 2
    private const val TIMEOUT_MILLIS = 8_000L
    private const val MAX_CACHE_WEIGHT = 16L * 1024 * 1024
    private const val MAX_CACHE_ENTRIES = 512L
    private const val TABLE_INDEX = 1
    private const val UNDEFINED_TOKEN = -2
    private const val TRUE_TOKEN = -3
    private const val FALSE_TOKEN = -4

    private data class InstanceEntry(
        val instance: Instance,
        val budget: ExecutionBudget,
    )

    private class ExecutionBudget {
        val deadlineNanos = AtomicLong(Long.MAX_VALUE)
        private val counter = AtomicInteger()

        fun check() {
            if ((counter.incrementAndGet() and 1023) != 0) return
            if (System.nanoTime() > deadlineNanos.get()) {
                throw ColorTimeoutException("颜色提取超时（${TIMEOUT_MILLIS}ms）")
            }
            if (Thread.currentThread().isInterrupted) {
                throw ColorTimeoutException("颜色提取被中断")
            }
        }
    }

    private open class ColorWasmException(message: String, cause: Throwable? = null) :
        RuntimeException(message, cause)

    private class ColorTimeoutException(message: String) : ColorWasmException(message)

    private val cache: Cache<String, String> = Caffeine.newBuilder()
        .maximumSize(MAX_CACHE_ENTRIES)
        .maximumWeight(MAX_CACHE_WEIGHT)
        .weigher<String, String> { key, value -> key.length + value.length }
        .expireAfterAccess(1, TimeUnit.HOURS)
        .build()

    private val permits = Semaphore(MAX_INSTANCES)
    private val pool = ArrayBlockingQueue<InstanceEntry>(MAX_INSTANCES)
    private val module: WasmModule by lazy { loadModule() }

    fun topHex(imageBytes: ByteArray, count: Int = 5): String {
        require(count in 1..16) { "颜色数量必须在 1..16" }
        require(imageBytes.isNotEmpty()) { "图片数据为空" }
        require(imageBytes.size <= MAX_INPUT_BYTES) { "图片数据超过 ${MAX_INPUT_BYTES} 字节" }

        val key = sha256(imageBytes)
        cache.getIfPresent(key)?.let { return it }
        val hex = withInstance { entry -> extract(entry, imageBytes, count) }
        cache.put(key, hex)
        return hex
    }

    private fun extract(entry: InstanceEntry, imageBytes: ByteArray, count: Int): String {
        val instance = entry.instance
        val malloc = instance.export("__wbindgen_malloc")
        val pointer = malloc.apply(imageBytes.size.toLong(), 1L).first().toInt()
        if (pointer <= 0) throw ColorWasmException("img-color malloc 返回非法指针")
        instance.memory().write(pointer, imageBytes)

        val result = instance.export("call_core").apply(
            pointer.toLong(),
            imageBytes.size.toLong(),
            count.toLong(),
        )
        if (result.size < 2) throw ColorWasmException("call_core 返回值数量非法")
        val arrayPointer = result[0].toInt()
        val arrayLength = result[1].toInt()
        if (arrayLength <= 0 || arrayLength > count) {
            throw ColorWasmException("call_core 返回颜色数量非法: $arrayLength")
        }
        if (arrayPointer <= 0) throw ColorWasmException("call_core 返回非法数组指针")

        val tokens = IntArray(arrayLength)
        for (index in 0 until arrayLength) {
            val tableIndex = instance.memory().readI32(arrayPointer + index * 4).toInt()
            if (tableIndex < 0) throw ColorWasmException("call_core 返回非法 externref 表索引")
            tokens[index] = instance.table(TABLE_INDEX).ref(tableIndex)
        }

        instance.export("__externref_drop_slice").apply(arrayPointer.toLong(), arrayLength.toLong())
        instance.export("__wbindgen_free").apply(arrayPointer.toLong(), (arrayLength * 4).toLong(), 4L)

        var hex: String? = null
        for ((index, token) in tokens.withIndex()) {
            if (token <= 0) throw ColorWasmException("call_core 返回非法 Rgb 指针")
            if (index == 0) {
                hex = rgbToHex(instance, token)
            }
            instance.export("__wbg_rgb_free").apply(token.toLong(), 1L)
        }
        return hex ?: throw ColorWasmException("call_core 没有返回颜色")
    }

    private fun rgbToHex(instance: Instance, pointer: Int): String {
        val result = instance.export("rgb_to_hex").apply(pointer.toLong())
        if (result.size < 2) throw ColorWasmException("rgb_to_hex 返回值数量非法")
        val stringPointer = result[0].toInt()
        val stringLength = result[1].toInt()
        if (stringPointer < 0 || stringLength < 0 || stringLength > 64) {
            throw ColorWasmException("rgb_to_hex 返回非法字符串")
        }
        val hex = try {
            instance.memory().readString(stringPointer, stringLength)
        } finally {
            instance.export("__wbindgen_free").apply(stringPointer.toLong(), stringLength.toLong(), 1L)
        }
        if (!HEX_PATTERN.matches(hex)) throw ColorWasmException("rgb_to_hex 返回非法颜色: $hex")
        return hex
    }

    private fun withInstance(block: (InstanceEntry) -> String): String {
        permits.acquire()
        var entry: InstanceEntry? = null
        var success = false
        try {
            entry = pool.poll() ?: createInstance()
            entry.budget.deadlineNanos.set(System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(TIMEOUT_MILLIS))
            val result = block(entry)
            success = true
            return result
        } finally {
            permits.release()
            if (entry != null) {
                if (success) {
                    pool.offer(entry)
                }
            }
        }
    }

    private fun createInstance(): InstanceEntry {
        val budget = ExecutionBudget()
        val rgbNew = HostFunction(
            MODULE_NAME,
            "__wbg_rgb_new",
            FunctionType.of(listOf(ValType.I32), listOf(ValType.ExternRef)),
            WasmFunctionHandle { _, args ->
                val pointer = args[0].toInt()
                if (pointer <= 0) throw ColorWasmException("Rgb 指针非法")
                longArrayOf(Value.externRef(pointer.toLong()).raw())
            },
        )
        val throwError = HostFunction(
            MODULE_NAME,
            "__wbg___wbindgen_throw_dd24417ed36fc46e",
            FunctionType.of(listOf(ValType.I32, ValType.I32), emptyList()),
            WasmFunctionHandle { instance, args ->
                val message = instance.memory().readString(args[0].toInt(), args[1].toInt())
                throw ColorWasmException(message)
            },
        )
        val initTable = HostFunction(
            MODULE_NAME,
            "__wbindgen_init_externref_table",
            FunctionType.of(emptyList(), emptyList()),
            WasmFunctionHandle { instance, _ ->
                val table = instance.table(TABLE_INDEX)
                val offset = table.grow(4, Value.REF_NULL_VALUE, instance)
                if (offset < 0) throw ColorWasmException("externref table grow 失败")
                table.setRef(0, UNDEFINED_TOKEN, instance)
                table.setRef(offset, UNDEFINED_TOKEN, instance)
                table.setRef(offset + 1, Value.REF_NULL_VALUE, instance)
                table.setRef(offset + 2, TRUE_TOKEN, instance)
                table.setRef(offset + 3, FALSE_TOKEN, instance)
                longArrayOf()
            },
        )
        val imports = ImportValues.builder()
            .addFunction(rgbNew, throwError, initTable)
            .build()
        val instance = Instance.builder(module)
            .withImportValues(imports)
            .withMemoryLimits(MemoryLimits(21, MAX_MEMORY_PAGES))
            // Required only to enforce a bounded instruction/deadline budget on the experimental interpreter API.
            .withUnsafeExecutionListener(ExecutionListener { _, _ -> budget.check() })
            .build()
        instance.export("__wbindgen_start").apply()
        return InstanceEntry(instance, budget)
    }

    private fun loadModule(): WasmModule {
        val bytes = ColorWasm::class.java.getResourceAsStream(MODULE_PATH)?.use { it.readBytes() }
            ?: throw IllegalStateException("找不到颜色模块: $MODULE_PATH")
        val actual = sha256(bytes)
        check(actual == MODULE_SHA256) { "颜色模块 hash 不匹配: $actual" }
        return Parser.parse(ByteArrayInputStream(bytes))
    }

    private fun sha256(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }

    private val HEX_PATTERN = Regex("^#[0-9A-Fa-f]{6}$")
    private const val MODULE_NAME = "__wbindgen_placeholder__"
}
