package com.now.nowbot.qq.interactions

import com.now.nowbot.qq.message.KeyboardMessage
import com.now.nowbot.qq.message.MessageChain
import com.yumu.qq.message.Keyboard

/**
 * QQ 官方按钮的线框样式，对应 [Keyboard.ButtonRender.style]。
 *
 * 官方只有两种取值，用枚举代替裸数字：
 * - [GREY]：0，灰色线框，适合次要操作
 * - [BLUE]：1，蓝色线框，默认样式，适合主要操作
 */
enum class ButtonStyle(val code: Int) {
    GREY(0),
    BLUE(1),
}

/**
 * 按钮点击后的行为，对应 [Keyboard.ButtonAction.type]。
 * - [LINK]：0，跳转链接 / scheme
 * - [CALLBACK]：1，回调后台接口
 * - [COMMAND]：2，指令按钮，点击后把 data 填入输入框（默认）
 */
enum class ButtonActionType(val code: Int) {
    LINK(0),
    CALLBACK(1),
    COMMAND(2),
}

/**
 * 按钮的可操作范围，对应 [Keyboard.Permission.type]。
 * - [USER]：0，指定用户
 * - [ADMIN]：1，仅管理员
 * - [EVERYONE]：2，所有人（默认）
 */
enum class ButtonPermission(val code: Int) {
    USER(0),
    ADMIN(1),
    EVERYONE(2),
}

/** 限制 [KeyboardBuilder] / [KeyboardRowBuilder] 的隐式接收者作用域，避免嵌套时调错层。 */
@DslMarker
annotation class KeyboardDsl

/**
 * 一行按钮。官方限制每行最多 5 个。
 *
 * 一般通过 [KeyboardBuilder.row] 创建，不需要直接实例化。
 */
@KeyboardDsl
class KeyboardRowBuilder internal constructor() {
    private val buttons = mutableListOf<Keyboard.Button>()

    /**
     * 添加一个按钮。
     *
     * @param label 按钮文字
     * @param data 指令内容（[ButtonActionType.COMMAND] 时点击后插入输入框；[ButtonActionType.LINK] 时为 http(s) 链接）
     * @param style 线框样式，默认蓝色
     * @param actionType 点击行为，默认指令按钮
     * @param permission 可操作范围，默认所有人
     * @param visitedLabel 点击后显示的文字，默认与 [label] 相同
     * @param enter 点击后是否直接发送 data（仅指令按钮有效）
     * @param reply 点击后是否带引用回复本消息（仅指令按钮有效）
     * @param tips 客户端不支持该 action 时的提示文案
     */
    fun button(
        label: String,
        data: String,
        style: ButtonStyle = ButtonStyle.BLUE,
        actionType: ButtonActionType = ButtonActionType.COMMAND,
        permission: ButtonPermission = ButtonPermission.EVERYONE,
        visitedLabel: String = label,
        enter: Boolean = false,
        reply: Boolean = false,
        tips: String = "",
    ): Keyboard.Button {
        require(buttons.size < MAX_BUTTONS_PER_ROW) {
            "每行最多 $MAX_BUTTONS_PER_ROW 个按钮，当前已有 ${buttons.size} 个"
        }

        if (actionType == ButtonActionType.LINK) {
            require(data.startsWith("http://") || data.startsWith("https://")) {
                "LINK 按钮的 data 必须是 http(s) 链接，当前为: $data"
            }
        }

        val button = Keyboard.Button(
            render = Keyboard.ButtonRender(
                label = label,
                visitedLabel = visitedLabel,
                style = style.code,
            ),
            action = Keyboard.ButtonAction(
                data = data,
                type = actionType.code,
                tips = tips,
                reply = reply.takeIf { it },
                enter = enter.takeIf { it },
                permission = Keyboard.Permission(type = permission.code),
            ),
        )
        buttons.add(button)
        return button
    }

    internal fun build(): List<Keyboard.Button> {
        require(buttons.isNotEmpty()) { "按钮行不能为空" }
        return buttons.toList()
    }

    companion object {
        const val MAX_BUTTONS_PER_ROW = 5
    }
}

/**
 * 键盘（按钮组）。官方限制最多 5 行，每行最多 5 个按钮。
 *
 * 用法：
 * ```
 * val kb = keyboard {
 *     row {
 *         button("我的成绩", "/s")
 *         button("群内成绩", "/sg", style = ButtonStyle.GREY)
 *     }
 *     row {
 *         button("今日BP", "/t", enter = true)
 *     }
 * }
 * ```
 */
@KeyboardDsl
class KeyboardBuilder internal constructor() {
    private val rows = mutableListOf<List<Keyboard.Button>>()

    /** 添加一行按钮。 */
    fun row(block: KeyboardRowBuilder.() -> Unit) {
        addRow(KeyboardRowBuilder().apply(block).build())
    }

    /** 添加一行现成的按钮。 */
    fun row(vararg buttons: Keyboard.Button) {
        addRow(buttons.toList())
    }

    private fun addRow(row: List<Keyboard.Button>) {
        require(row.isNotEmpty()) { "按钮行不能为空" }
        require(row.size <= KeyboardRowBuilder.MAX_BUTTONS_PER_ROW) {
            "每行最多 ${KeyboardRowBuilder.MAX_BUTTONS_PER_ROW} 个按钮，当前 ${row.size} 个"
        }
        require(rows.size < MAX_ROWS) { "按钮最多 $MAX_ROWS 行" }
        rows.add(row)
    }

    fun build(): Keyboard {
        require(rows.isNotEmpty()) { "键盘至少需要一行按钮" }
        return Keyboard(keyboards = rows.toList())
    }

    companion object {
        const val MAX_ROWS = 5
    }
}

/** 构建一个 [Keyboard]。 */
fun keyboard(block: KeyboardBuilder.() -> Unit): Keyboard = KeyboardBuilder().apply(block).build()

/** 构建一个 [com.now.nowbot.qq.message.KeyboardMessage]，可以直接 `addMessage`。 */
fun keyboardMessage(block: KeyboardBuilder.() -> Unit): KeyboardMessage = KeyboardMessage(keyboard(block))

/** `MessageChain.builder().addKeyboard { ... }` 的 DSL 版本。 */
fun MessageChain.MessageChainBuilder.addKeyboard(
    block: KeyboardBuilder.() -> Unit,
): MessageChain.MessageChainBuilder = addKeyboard(keyboard(block))
