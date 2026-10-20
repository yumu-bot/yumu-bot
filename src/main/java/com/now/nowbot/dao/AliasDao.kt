package com.now.nowbot.dao

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.google.errorprone.annotations.CanIgnoreReturnValue
import com.now.nowbot.entity.Alias
import com.now.nowbot.entity.AliasTitle
import com.now.nowbot.mapper.AliasRepository
import com.now.nowbot.mapper.TitleRepository
import com.now.nowbot.util.StringUtil.formatted
import com.now.nowbot.util.StringUtil.removeSongSuffix
import jakarta.transaction.Transactional
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.OffsetDateTime

@Component
class AliasDao(
    private val titleRepository: TitleRepository,
    private val aliasRepository: AliasRepository
) {
    companion object {
        /**
         * 别名极少变动，但每条成绩消息都要查一次，因此这里做一层短期缓存。
         *
         * 本类是所有别名写操作的唯一入口，写完后会立即失效对应标题，
         * 万一有别的途径（例如直接改库）写入，最迟 TTL 之后也会自动纠正。
         */
        private val aliasCache: Cache<String, List<String>> = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofHours(1))
            .maximumSize(8192)
            .build()

        /**
         * 缓存键与 [AliasTitle.name] 保持一致：
         * 原始标题去掉歌曲后缀、再经 formatted 标准化。formatted 是幂等的，
         * 所以调用方传处理过的标题进来当 key 也完全等价。
         */
        private fun normalizedTitle(rawTitle: String): String = rawTitle.removeSongSuffix().formatted()

        private fun invalidateTitleCache(vararg rawTitles: String) {
            aliasCache.invalidateAll(rawTitles.map { normalizedTitle(it) })
        }
    }

    /**
     * 批量按标题名称获取别名值列表，一次查询解决多个标题，避免逐条标题查库导致的 N+1。
     *
     * 只会返回已通过审核的别名；待审核数据仅允许在审核流程里出现。
     *
     * @param normalizedTitles 已经 removeSongSuffix + formatted 处理过的标题（即 [AliasTitle.name] 的形态）
     * @return key 为标题，value 为该标题下已通过审核的别名值列表；没有已审核别名的标题对应空列表
     */
    @Transactional
    fun getAliasesByNormalizedTitles(normalizedTitles: Collection<String>): Map<String, List<String>> {
        val keys = normalizedTitles.toSet()

        // 缓存里只放「库里查到的标题」，所以 getIfPresent == null 就是还没查过的
        val missed = keys.filter { aliasCache.getIfPresent(it) == null }

        // 命中缓存的先收下，缺的那些等会儿一次 IN 查询补齐
        val result = HashMap<String, List<String>>(keys.size)
        keys.forEach { cached ->
            aliasCache.getIfPresent(cached)?.let { result[cached] = it }
        }

        if (missed.isNotEmpty()) {
            titleRepository.findByNameInWithAliases(missed).forEach { title ->
                // 只把已审核的别名放进缓存和返回值，待审核数据不能对外泄露
                val aliases = title.approvedAliases.map { it.value }

                // 直接拿查询结果填充返回值，绝不要在查完之后再从缓存回读
                result[title.name] = aliases
                aliasCache.put(title.name, aliases)
            }
        }

        // 库里确实不存在的标题不写缓存：否则会把上面刚查到的结果覆盖成空列表。
        // 返回值统一按空列表处理即可。
        return keys.associateWith { result[it].orEmpty() }
    }

    /**
     * 通过标题名称获取该标题下已通过审核的别名值列表
     * @param titleName 标题名称
     * @return 已审核别名字符串列表（如果标题不存在或没有已审核别名返回空列表）
     */
    @Transactional
    fun getAliasesByTitle(titleName: String): List<String> {
        val title = titleRepository.findByNameWithAliases(titleName) ?: return emptyList()
        return title.approvedAliases.map { it.value }
    }

    /**
     * 通过标题 ID 获取该标题下已通过审核的别名值列表
     * @param titleId 标题 ID
     * @return 已审核别名字符串列表（如果标题不存在或没有已审核别名返回空列表）
     */
    @Transactional
    fun getAliasesByTitleId(titleId: Long): List<String> {
        val title = titleRepository.findByIdWithAliases(titleId) ?: return emptyList()
        return title.approvedAliases.map { it.value }
    }

    /**
     * 通过标题名称获取该标题下已通过审核的 Alias 实体列表
     * @param titleName 标题名称
     * @return 已审核 Alias 实体列表，不会包含 approved = false 的数据
     */
    @Transactional
    fun getAliasEntitiesByTitle(titleName: String): List<Alias> {
        val title = titleRepository.findByNameWithAliases(titleName) ?: return emptyList()
        return title.approvedAliases
    }

    /**
     * 删除流程专用：获取标题下的所有别名（包含还没有通过审核的别名）。
     *
     * 删除指令是超级管理员专属操作，管理员需要能看到并删除待审核别名，
     * 因此这里刻意不按 approved 过滤。除删除流程外不要调用本方法。
     *
     * @param titleName 标题名称
     * @return 标题下的全部别名值（标题不存在时返回空列表）
     */
    @Transactional
    fun getAliasesByTitleIncludingUnapproved(titleName: String): List<String> {
        val title = titleRepository.findByNameWithAliases(titleName) ?: return emptyList()
        return title.aliases.map { it.value }
    }

    /**
     * 按最近更新时间倒序取若干个含有多个别名的标题及其全部别名，供 `!aj` 无参数时展示。
     *
     * 只有一个别名的标题无法调整顺序，查询时已经排除。
     * 这是管理员指令，返回的别名包含待审核项。
     */
    @Transactional
    fun getRecentTitles(limit: Int): List<AliasTitleSummary> {
        if (limit <= 0) return emptyList()

        return titleRepository.findRecentTitles(PageRequest.of(0, limit)).map { title ->
            AliasTitleSummary(
                title = title.name,
                aliases = title.aliases.map { it.value },
            )
        }
    }

    /**
     * 按给定顺序重排某个标题下的别名。
     *
     * [orderedValues] 必须和数据库里的别名集合完全一致（数量、值都相同），
     * 否则说明会话期间别名被其他人改过，返回 false 让调用方提示用户重新查询。
     */
    @Transactional
    fun reorderAliases(titleName: String, orderedValues: List<String>): Boolean {
        val title = titleRepository.findByNameWithAliases(titleName) ?: return false

        if (title.aliases.size != orderedValues.size) return false

        val aliasesByValue = title.aliases.associateBy { it.value }
        if (aliasesByValue.size != title.aliases.size) return false

        val ordered = orderedValues.map { value ->
            aliasesByValue[value] ?: return false
        }

        if (ordered.distinctBy { it.id }.size != title.aliases.size) return false

        title.aliases.clear()
        title.aliases.addAll(ordered)
        title.updatedAt = OffsetDateTime.now()
        titleRepository.save(title)

        invalidateTitleCache(titleName)

        return true
    }

    /**
     * 统计某个标题下还没有通过审核的别名数量。
     *
     * 供 `!am` 的普通用户预览使用：普通用户看不到待审核别名本身，
     * 但需要知道还有多少条正在等待审核。
     *
     * @param titleName 标题名称
     * @return 待审核别名数量（标题不存在时返回 0）
     */
    @Transactional
    fun countUnapprovedAliasesByTitle(titleName: String): Int {
        val title = titleRepository.findByNameWithAliases(titleName) ?: return 0
        return title.aliases.count { !it.approved }
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
    fun addAliasToTitle(titleName: String, rawAliasValue: String, setAsPrimary: Boolean, approved: Boolean, from: Long) {
        // 别名只做 trim，其余字符原样保存（和 deleteAliasFromTitle / deleteAliasesFromTitle 的比较规则保持一致）
        val aliasValue = rawAliasValue.trim()

        // 1. 【关键点】使用带 JOIN FETCH 的方法查询标题，将现有的 aliases 完整加载出来
        var aliasTitle = titleRepository.findByNameWithAliases(titleName)

        // 2. 如果标题不存在，则新建一个
        if (aliasTitle == null) {
            aliasTitle = AliasTitle(name = titleName)
            titleRepository.save(aliasTitle)
        }

        // 3. 查找或创建 Alias 实体。
        //    只有新建时才写 approved 和 qq；已存在的行一律不动，和 addAliasInternal 保持一致。
        //    别名的审核状态只能由审核流程（reviewAliases）改变，
        //    用户重复输入同一个别名是彻底的空操作，不能把已通过的别名打回待审核。
        var alias = aliasRepository.findByValue(aliasValue)
        if (alias == null) {
            alias = Alias(value = aliasValue, approved = approved, qq = from)
            aliasRepository.save(alias)
        }

        // 4. 将别名加入到标题的 aliases 列表中
        // 由于 aliases 已经被完整加载，Hibernate 能正确计算出当前列表的 size，
        // 从而生成正确的 position（例如当前有 2 个，新加的就是第 3 个，position = 2），不会再发生主键冲突。
        // 该标题下已经有这个别名时，整体跳过：不新增绑定，也不改任何状态。
        if (!aliasTitle.aliases.contains(alias)) {
            if (setAsPrimary) {
                aliasTitle.aliases.add(0, alias) // 插在最前面作为首选
            } else {
                aliasTitle.aliases.add(alias)  // 加在末尾
            }
            aliasTitle.updatedAt = OffsetDateTime.now()
            titleRepository.save(aliasTitle)
        }

        invalidateTitleCache(aliasTitle.name)
    }

    /**
     * 批量导入「标题 -> 别名」映射，供 !ai 使用。
     *
     * 标题必须已经过 removeSongSuffix + formatted 标准化；
     * 别名只做 trim，和 [addAliasToTitle] 的存储规则一致。
     *
     * 已存在的绑定会跳过，不会改变它在标题下的顺序（因此不会影响 !fetch a 导出的首选别名）。
     * 已存在但还没审核的别名，在 [approved] 为真时会一并置为已通过。
     *
     * 所有条目在同一个事务里完成，避免逐条导入时反复提交。
     *
     * @return 新增的绑定数量和跳过的已存在绑定数量
     */
    @Transactional
    @CanIgnoreReturnValue
    fun importAliases(mapping: Map<String, String>, approved: Boolean, from: Long): AliasImportResult {
        if (mapping.isEmpty()) return AliasImportResult(0, 0)

        // 先按 trim 后的值准备别名，批量查询和写入都用同一份值，避免查不到已存在的别名
        val trimmedMapping = mapping.mapValues { (_, rawAliasValue) -> rawAliasValue.trim() }

        // 一次把相关的标题和别名都查出来，避免逐条导入时的 N+1
        val titles = titleRepository.findByNameInWithAliases(mapping.keys)
            .associateByTo(HashMap<String, AliasTitle>()) { it.name }
        val aliases = aliasRepository.findAllByValueIn(trimmedMapping.values.toSet())
            .associateByTo(HashMap<String, Alias>()) { it.value }

        var imported = 0
        var skipped = 0
        val updatedTitles = HashSet<AliasTitle>()

        trimmedMapping.forEach { (titleName, aliasValue) ->
            val title = titles.getOrPut(titleName) { AliasTitle(name = titleName) }

            val alias = aliases.getOrPut(aliasValue) {
                Alias(value = aliasValue, approved = approved, qq = from)
            }

            // 导入的别名视为已通过审核
            if (approved && !alias.approved) {
                alias.approved = true
                updatedTitles += title
            }

            val exists = title.aliases.any { it.id == alias.id || it.value == alias.value }

            if (exists) {
                skipped++
            } else {
                title.aliases.add(alias)
                updatedTitles += title
                imported++
            }
        }

        val now = OffsetDateTime.now()
        updatedTitles.forEach { it.updatedAt = now }

        // 先保存别名拿到 ID，再保存标题与别名的绑定关系
        aliasRepository.saveAll(aliases.values)
        titleRepository.saveAll(titles.values)

        invalidateTitleCache(*mapping.keys.toTypedArray())

        return AliasImportResult(imported, skipped)
    }

    /**
     * 删除某个标题下的别名。
     *
     * 别名只做 trim，其余字符（大小写、全角、假名等）原样参与比较。
     * 别名是给人看的短名字，不能像标题那样 standardised，
     * 否则会出现「列表里明明有、删除时却说没有这个别名」的问题。
     *
     * 如果删除后该别名已经没有任何标题引用，则连同别名一起删除。
     *
     * @return 是否真的删除了（标题或别名不存在时返回 false）
     */
    @Transactional
    fun deleteAliasFromTitle(titleName: String, rawAliasValue: String): Boolean {
        val value = rawAliasValue.trim()

        val title = titleRepository.findByNameWithAliases(titleName) ?: return false

        // 直接在该标题已加载的别名里比对，避免再走一次可能受数据库排序规则影响的 SQL 查询
        val alias = title.aliases.firstOrNull { it.value.trim() == value } ?: return false

        title.aliases.removeAll { it.id == alias.id }

        title.updatedAt = OffsetDateTime.now()
        titleRepository.save(title)

        // 如果这个别名已经没有任何标题引用了，就顺手删掉，避免留下孤儿数据
        if (titleRepository.findByAliasValue(alias.value).isEmpty()) {
            aliasRepository.delete(alias)
        }

        invalidateTitleCache(titleName)

        return true
    }

    /**
     * 删除某个标题下指定的若干个别名（包括还没通过审核的）。
     *
     * 别名只做 trim，比较时两边都 trim，其余字符原样比较（和 [addAliasToTitle] 的存储规则一致）。
     * 标题下不存在的值会被忽略。删除后不再被任何标题引用的别名会连同一起删除。
     *
     * @return 实际删除的别名数量
     */
    @Transactional
    fun deleteAliasesFromTitle(titleName: String, rawAliasValues: Collection<String>): Int {
        if (rawAliasValues.isEmpty()) return 0

        val title = titleRepository.findByNameWithAliases(titleName) ?: return 0

        val values = rawAliasValues.map { it.trim() }.toSet()

        // filter 会先把关联列表读出来，之后才能安全地在列表上 removeAll
        val removed = title.aliases.filter { it.value.trim() in values }

        if (removed.isEmpty()) return 0

        // 按 id 删除，避免值比较的差异导致实际没删掉
        val removedIds = removed.map { it.id }.toSet()
        title.aliases.removeAll { it.id in removedIds }
        title.updatedAt = OffsetDateTime.now()
        titleRepository.save(title)

        // 如果这些别名已经没有任何标题引用了，就顺手删掉，避免留下孤儿数据
        removed.forEach { alias ->
            if (titleRepository.findByAliasValue(alias.value).isEmpty()) {
                aliasRepository.delete(alias)
            }
        }

        invalidateTitleCache(titleName)

        return removed.size
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
        title.updatedAt = OffsetDateTime.now()
        titleRepository.save(title)

        // 如果这些别名已经没有任何标题引用了，就顺手删掉，避免留下孤儿数据
        aliasValues.forEach { rawValue ->
            if (titleRepository.findByAliasValue(rawValue).isEmpty()) {
                aliasRepository.findByValue(rawValue)?.let { aliasRepository.delete(it) }
            }
        }

        invalidateTitleCache(titleName)

        return aliasValues.size
    }

    /**
     * 查询所有标题以及它的第一个已审核别名，供导出使用。
     *
     * 取的是 [AliasTitle.primaryApprovedAlias]，即 @OrderColumn(position) 排序后
     * 第一个 approved = true 的别名。首位别名还在待审核时会顺延到下一个已审核别名，
     * 因此未审核数据不会进入导出文件。
     *
     * @return 有序 Map：key 是标题，value 是第一个已审核别名；没有已审核别名的标题不会出现
     */
    @Transactional
    fun getTitlePrimaryAliases(): Map<String, String> {
        return titleRepository.findAll()
            // 首选别名也必须从已审核列表里取，待审核的别名不能进入导出文件
            .mapNotNull { title -> title.primaryApprovedAlias?.value?.let { alias -> title.name to alias } }
            .toMap()
    }

    /**
     * 查询所有待审核的别名（含关联的标题名）。
     *
     * 这是唯一允许返回 approved = false 数据的读取方法，只能由
     * AliasService 的 review 审核流程调用，禁止用于任何对外展示。
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
        val now = OffsetDateTime.now()
        var approved = 0
        if (approvedIds.isNotEmpty()) {
            val aliases = aliasRepository.findAllById(approvedIds)
            aliases.forEach { alias ->
                alias.approved = true
                alias.titles.forEach { it.updatedAt = now }
            }
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
                    title.updatedAt = now
                }
            }
            titleRepository.flush()

            aliasRepository.deleteAll(aliases)
            rejected = aliases.size
        }

        // 涉及多个标题的别名变动，直接整体失效最省心
        aliasCache.invalidateAll()

        return approved to rejected
    }

    /**
     * 内部核心逻辑：处理规范化、查找/创建别名、维护顺序（第一个为主）
     */
    private fun addAliasInternal(title: AliasTitle, rawAliasValue: String, setAsPrimary: Boolean, approved: Boolean, from: Long): AliasTitle {
        // 1. 存入前只做 trim（别名不能 standardised，否则会和删除时的比较不一致）
        val normalizedValue = rawAliasValue.trim()

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
        title.updatedAt = OffsetDateTime.now()
        val saved = titleRepository.save(title)

        invalidateTitleCache(title.name)

        // 写操作需要完整的 aliases 才能避免重复绑定，但返回值可能被直接展示，
        // 所以只把已审核的别名交给调用方。
        return saved.toApprovedView()
    }

    /**
     * 生成一个只包含已审核别名的安全副本，供写操作的返回值使用。
     *
     * 这里刻意返回副本而不是直接修改 [AliasTitle.aliases]：
     * 原实体仍是 JPA 管理对象，删掉待审核别名会被当成真正的解绑。
     */
    private fun AliasTitle.toApprovedView(): AliasTitle = AliasTitle(
        id = id,
        name = name,
        aliases = approvedAliases.toMutableList(),
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}

/**
 * `!aj` 无参数时展示的标题摘要。
 */
data class AliasTitleSummary(
    val title: String,
    val aliases: List<String>,
)

/**
 * 审核用的待审核别名视图。
 */
data class PendingAlias(
    val id: Long,
    val value: String,
    val titles: List<String>,
    val qq: Long,
)

/**
 * 批量导入的结果。
 *
 * @param imported 新增的绑定数量
 * @param skipped 已存在、被跳过的绑定数量
 */
data class AliasImportResult(
    val imported: Int,
    val skipped: Int,
)
