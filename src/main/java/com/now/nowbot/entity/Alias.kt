package com.now.nowbot.entity

import jakarta.persistence.*
import java.time.OffsetDateTime

@Entity
@Table(name = "alias_title")
class AliasTitle(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    var name: String,

    @ManyToMany(cascade = [CascadeType.PERSIST, CascadeType.MERGE])
    @JoinTable(
        name = "title_alias_mappings",
        joinColumns = [JoinColumn(name = "title_id")],
        inverseJoinColumns = [JoinColumn(name = "alias_id")]
    )
    @OrderColumn(name = "position")
    val aliases: MutableList<Alias> = mutableListOf(),

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now()
) {
    val primaryAlias: Alias?
        get() = aliases.firstOrNull()
}

@Entity
@Table(name = "aliases")
class Alias(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, unique = true, length = 255)
    val value: String,

    @ManyToMany(mappedBy = "aliases")
    val titles: MutableList<AliasTitle> = mutableListOf(),

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now()
) {
    // 既然已经是标准化的，可以直接比较 value
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Alias) return false
        return id != 0L && id == other.id || value == other.value
    }

    override fun hashCode(): Int = value.hashCode()
}