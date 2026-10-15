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
     * 通过标题名称获取该标题下的所有别名值列表
     * @param titleName 标题名称
     * @return 别名字符串列表（如果标题不存在返回空列表）
     */
    @Transactional
    fun getAliasesByTitle(titleName: String): List<String> {
        val title = titleRepository.findByNameWithAliases(titleName) ?: return emptyList()
        return title.aliases.map { it.value }
    }

    /**
     * 通过标题 ID 获取该标题下的所有别名值列表
     * @param titleId 标题 ID
     * @return 别名字符串列表（如果标题不存在返回空列表）
     */
    @Transactional
    fun getAliasesByTitleId(titleId: Long): List<String> {
        val title = titleRepository.findByIdWithAliases(titleId) ?: return emptyList()
        return title.aliases.map { it.value }
    }

    /**
     * 通过标题名称获取该标题下的所有 Alias 实体列表（包含审核状态等详细信息）
     * @param titleName 标题名称
     * @return Alias 实体列表
     */
    @Transactional
    fun getAliasEntitiesByTitle(titleName: String): List<Alias> {
        val title = titleRepository.findByNameWithAliases(titleName) ?: return emptyList()
        return title.aliases.toList()
    }

    /**
     * 1. 通过 Title ID 添加别名
     */
    @Transactional
    @CanIgnoreReturnValue
    fun addAliasToTitle(titleId: Long, rawAliasValue: String, setAsPrimary: Boolean, approved: Boolean = false, from: Long): AliasTitle {
        val title = titleRepository.findById(titleId)
            .orElseThrow { IllegalArgumentException("Title not found with id: $titleId") }
        return addAliasInternal(title, rawAliasValue, setAsPrimary, approved, from)
    }

    /**
     * 2. 直接通过 Title 实体添加别名
     */
    @Transactional
    @CanIgnoreReturnValue
    fun addAliasToTitle(title: AliasTitle, rawAliasValue: String, setAsPrimary: Boolean, approved: Boolean = false, from: Long): AliasTitle {
        return addAliasInternal(title, rawAliasValue, setAsPrimary, approved, from)
    }

    @Transactional
    fun addAliasToTitle(titleName: String, aliasValue: String, setAsPrimary: Boolean, approved: Boolean, from: Long) {
        // 1. 【关键点】使用带 JOIN FETCH 的方法查询标题，将现有的 aliases 完整加载出来
        var aliasTitle = titleRepository.findByNameWithAliases(titleName)

        // 2. 如果标题不存在，则新建一个
        if (aliasTitle == null) {
            aliasTitle = AliasTitle(name = titleName)
            titleRepository.save(aliasTitle)
        }

        // 3. 查找或创建 Alias 实体
        var alias = aliasRepository.findByValue(aliasValue)
        if (alias == null) {
            alias = Alias(value = aliasValue, approved = approved, qq = from)
            aliasRepository.save(alias)
        } else {
            // 如果别名已存在，可根据需要更新其状态
            alias.approved = approved
        }

        // 4. 将别名加入到标题的 aliases 列表中
        // 由于 aliases 已经被完整加载，Hibernate 能正确计算出当前列表的 size，
        // 从而生成正确的 position（例如当前有 2 个，新加的就是第 3 个，position = 2），不会再发生主键冲突。
        if (!aliasTitle.aliases.contains(alias)) {
            if (setAsPrimary) {
                aliasTitle.aliases.add(0, alias) // 插在最前面作为首选
            } else {
                aliasTitle.aliases.add(alias)  // 加在末尾
            }
            titleRepository.save(aliasTitle)
        }
    }

    /**
     * 删除某个标题下的别名。
     *
     * 如果删除后该别名已经没有任何标题引用，则连同别名一起删除。
     *
     * @return 是否真的删除了（标题或别名不存在时返回 false）
     */
    @Transactional
    fun deleteAliasFromTitle(titleName: String, rawAliasValue: String): Boolean {
        val normalizedValue = rawAliasValue.removeSongSuffix().standardised()

        val title = titleRepository.findByName(titleName) ?: return false
        val alias = aliasRepository.findByValue(normalizedValue) ?: return false

        val removed = title.aliases.removeAll { it.id == alias.id || it.value == alias.value }
        if (!removed) return false

        titleRepository.save(title)

        // 如果这个别名已经没有任何标题引用了，就顺手删掉，避免留下孤儿数据
        if (titleRepository.findByAliasValue(normalizedValue).isEmpty()) {
            aliasRepository.delete(alias)
        }

        return true
    }

    /**
     * 删除某个标题下的所有别名（包括还没通过审核的）。
     *
     * 删除后不再被任何标题引用的别名会连同一起删除。
     *
     * @return 实际删除的别名数量（标题不存在或该标题没有任何别名时返回 0）
     */
    @Transactional
    fun deleteAllAliasesFromTitle(titleName: String): Int {
        val title = titleRepository.findByName(titleName) ?: return 0
        if (title.aliases.isEmpty()) return 0

        val aliasValues = title.aliases.map { it.value }

        title.aliases.clear()
        titleRepository.save(title)

        // 如果这些别名已经没有任何标题引用了，就顺手删掉，避免留下孤儿数据
        aliasValues.forEach { rawValue ->
            if (titleRepository.findByAliasValue(rawValue).isEmpty()) {
                aliasRepository.findByValue(rawValue)?.let { aliasRepository.delete(it) }
            }
        }

        return aliasValues.size
    }

    /**
     * 查询所有待审核的别名（含关联的标题名），供管理员审核时展示。
     */
    @Transactional
    fun getPendingAliases(): List<PendingAlias> {
        return aliasRepository.findAllUnapproved().map { alias ->
            PendingAlias(
                id = alias.id,
                value = alias.value,
                titles = alias.titles.map { it.name },
                qq = alias.qq
            )
        }
    }

    /**
     * 批量审核别名。
     *
     * 通过的直接置为已通过；不通过的会连同它与所有标题的绑定一起删除。
     *
     * @param approvedIds 通过审核的别名 ID
     * @param rejectedIds 不通过、需要删除的别名 ID
     * @return Pair(通过的数量, 不通过并删除的数量)
     */
    @Transactional
    fun reviewAliases(approvedIds: Collection<Long>, rejectedIds: Collection<Long>): Pair<Int, Int> {
        var approved = 0
        if (approvedIds.isNotEmpty()) {
            val aliases = aliasRepository.findAllById(approvedIds)
            aliases.forEach { it.approved = true }
            aliasRepository.saveAll(aliases)
            approved = aliases.size
        }

        var rejected = 0
        if (rejectedIds.isNotEmpty()) {
            val aliases = aliasRepository.findAllById(rejectedIds)

            // 先解除与所有标题的绑定并 flush，避免删除别名时触发外键冲突
            aliases.forEach { alias ->
                alias.titles.forEach { title ->
                    title.aliases.removeAll { it.id == alias.id || it.value == alias.value }
                }
            }
            titleRepository.flush()

            aliasRepository.deleteAll(aliases)
            rejected = aliases.size
        }

        return approved to rejected
    }

    /**
     * 内部核心逻辑：处理规范化、查找/创建别名、维护顺序（第一个为主）
     */
    private fun addAliasInternal(title: AliasTitle, rawAliasValue: String, setAsPrimary: Boolean, approved: Boolean, from: Long): AliasTitle {
        // 1. 存入前强制规范化
        val normalizedValue = rawAliasValue.removeSongSuffix().standardised()

        val alias = aliasRepository.findByValue(normalizedValue)
            ?: aliasRepository.save(Alias(value = normalizedValue, approved = approved, qq = from))

        // 1. 如果该别名已经在当前 Title 的别名列表中，先把它从列表中移除（防止重复或占位冲突）
        val existingIndex = title.aliases.indexOfFirst { it.id == alias.id }
        if (existingIndex != -1) {
            title.aliases.removeAt(existingIndex)
        }

        // 2. 根据是否为主别名，决定插入位置
        if (setAsPrimary) {
            // 设为主别名：硬插入到第 0 位
            title.aliases.add(0, alias)
        } else {
            // 普通添加：放到末尾
            title.aliases.add(alias)
        }

        // 5. 保存并返回更新后的 Title 实体
        return titleRepository.save(title)
    }
}

/**
 * 审核用的待审核别名视图。
 */
data class PendingAlias(
    val id: Long,
    val value: String,
    val titles: List<String>,
    val qq: Long,
)