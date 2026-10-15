package com.now.nowbot.dao

import com.google.errorprone.annotations.CanIgnoreReturnValue
import com.now.nowbot.entity.Alias
import com.now.nowbot.entity.AliasTitle
import com.now.nowbot.mapper.AliasRepository
import com.now.nowbot.mapper.TitleRepository
import com.now.nowbot.util.StringUtil.removeSongSuffix
import com.now.nowbot.util.StringUtil.standardised
import jakarta.transaction.Transactional
import org.springframework.stereotype.Component

@Component
class AliasDao(
    private val titleRepository: TitleRepository,
    private val aliasRepository: AliasRepository
) {

    /**
     * 1. 通过 Title ID 添加别名
     */
    @Transactional
    @CanIgnoreReturnValue
    fun addAliasToTitle(titleId: Long, rawAliasValue: String, setAsPrimary: Boolean): AliasTitle {
        val title = titleRepository.findById(titleId)
            .orElseThrow { IllegalArgumentException("Title not found with id: $titleId") }
        return addAliasInternal(title, rawAliasValue, setAsPrimary)
    }

    /**
     * 2. 直接通过 Title 实体添加别名
     */
    @Transactional
    @CanIgnoreReturnValue
    fun addAliasToTitle(title: AliasTitle, rawAliasValue: String, setAsPrimary: Boolean): AliasTitle {
        return addAliasInternal(title, rawAliasValue, setAsPrimary)
    }

    /**
     * 2. 通过 Title 字符串名称添加别名
     * 逻辑：如果该名称的 Title 已存在则直接使用，不存在则自动创建一条新的 Title
     */
    @Transactional
    @CanIgnoreReturnValue
    fun addAliasToTitle(titleName: String, rawAliasValue: String, setAsPrimary: Boolean): AliasTitle {
        // 假设 TitleRepository 有 findByName 方法。
        // 如果你的 Title 实体属性名不是 name，请换成你对应的属性（例如 titleName 等）
        val title = titleRepository.findByName(titleName)
            ?: titleRepository.save(AliasTitle(name = titleName))

        return addAliasInternal(title, rawAliasValue, setAsPrimary)
    }

    /**
     * 内部核心逻辑：处理规范化、查找/创建别名、维护顺序（第一个为主）
     */
    private fun addAliasInternal(title: AliasTitle, rawAliasValue: String, setAsPrimary: Boolean): AliasTitle {
        // 1. 存入前强制规范化
        val normalizedValue = rawAliasValue.removeSongSuffix().standardised()

        // 2. 查找或创建规范化后的全局别名
        val alias = aliasRepository.findByValue(normalizedValue)
            ?: aliasRepository.save(Alias(value = normalizedValue))

        // 3. 避免重复绑定：如果已经存在，直接返回当前的 title
        if (title.aliases.any { it.id == alias.id }) {
            return title
        }

        // 4. 维护顺序（第一个为主）
        if (setAsPrimary) {
            title.aliases.add(0, alias)
        } else {
            title.aliases.add(alias)
        }

        // 5. 保存并返回更新后的 Title 实体
        return titleRepository.save(title)
    }
}