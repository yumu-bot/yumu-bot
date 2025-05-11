package com.now.nowbot.entity

import com.now.nowbot.model.json.Tag
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "beatmap_tag")
class TagLite {
    @Id
    @Column(name = "id", columnDefinition = "int")
    var id: Int? = null

    @Column(name = "category", columnDefinition = "text")
    var category: String = ""

    @Column(name = "type", columnDefinition = "text")
    var type: String = ""

    @Column(name = "ruleset_id", columnDefinition = "smallint")
    var rulesetID: Byte? = null

    @Column(name = "description", columnDefinition = "text")
    var description: String = ""

    fun toModel(): Tag {
        val t = this@TagLite

        return Tag().apply {
            id = t.id ?: 0
            name = "${t.category}/${t.type}"
            rulesetID = t.rulesetID
            description = t.description
        }
    }

    companion object {
        @JvmStatic
        fun from(tag: Tag): TagLite {
            return TagLite().apply {
                id = tag.id
                category = tag.category
                type = tag.type
                rulesetID = tag.rulesetID
                description = tag.description
            }
        }
    }
}