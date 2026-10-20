package com.now.nowbot.entity

import jakarta.persistence.*
import java.time.OffsetDateTime

@Entity
@Table(name = "alias_title")
class AliasTitle(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,

    @Column(nullable = false)
    var name: String,

    @ManyToMany(cascade = [CascadeType.PERSIST, CascadeType.MERGE])
    @JoinTable(
        name = "title_alias_mappings",
        joinColumns = [JoinColumn(name = "title_id")],
        inverseJoinColumns = [JoinColumn(name = "alias_id")]
    )
    @OrderColumn(name = "position")
    var aliases: MutableList<Alias> = mutableListOf(),

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: OffsetDateTime = OffsetDateTime.now()
) {
    val primaryAlias: Alias?
        get() = aliases.firstOrNull()

    /**
     * 只包含已通过审核的别名，保持原本的展示顺序。
     *
     * 对外展示、导出、匹配歌曲时都必须使用这个视图；
     * 未审核的别名只允许在 `AliasService.review` 的审核流程里出现。
     * 这里不修改 [aliases] 本身，写操作仍然需要看到完整列表以避免重复绑定。
     */
    val approvedAliases: List<Alias>
        get() = aliases.filter { it.approved }

    /**
     * 第一个已通过审核的别名。
     *
     * 导出的首选别名必须使用它：如果原来的首位别名还在待审核，
     * 不能把它泄露出去，也不能让该标题因为首位未审核而整个消失。
     */
    val primaryApprovedAlias: Alias?
        get() = approvedAliases.firstOrNull()
}

@Entity
@Table(name = "aliases")
class Alias(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,

    @Column(nullable = false, unique = true, length = 255)
    var value: String,

    @ManyToMany(mappedBy = "aliases")
    var titles: MutableList<AliasTitle> = mutableListOf(),

    @Column(name = "qq", nullable = false, updatable = false)
    var qq: Long = 0,

    /**
     * 是否已经通过审核。
     *
     * 普通用户添加的别名默认为 false（待审核），管理员（含超级管理员）添加的默认为 true。
     * 列默认值是为了兼容 ddl-auto=update 时已存在的数据行。
     */
    @Column(name = "approved", nullable = false, columnDefinition = "boolean default false")
    var approved: Boolean = false,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: OffsetDateTime = OffsetDateTime.now()
) {
    // 既然已经是标准化的，可以直接比较 value
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Alias) return false
        return id != 0L && id == other.id || value == other.value
    }

    override fun hashCode(): Int = value.hashCode()
}