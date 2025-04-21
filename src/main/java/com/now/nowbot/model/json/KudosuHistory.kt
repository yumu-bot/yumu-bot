package com.now.nowbot.model.json

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime

@JsonInclude(JsonInclude.Include.NON_NULL) @JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
class KudosuHistory {
    @JvmRecord
    data class Giver(val url: String?, val username: String?)

    @JvmRecord
    data class Post(val url: String?, val title: String?)  //It'll be "[deleted beatmap]" for deleted beatmaps.

    var id: Int? = null
    var action: String? = null
    var amount: Int? = null
    var model: String? = null

    @JsonIgnoreProperties
    var created: LocalDateTime? = null

    @JsonIgnoreProperties
    var giver: Giver? = null

    @JsonIgnoreProperties
    var post: Post? = null

    @JsonProperty("created") fun setCreated(time: String) {
        this.created = LocalDateTime.from(Score.formatter.parse(time))
    }

    @JsonProperty("giver") fun setGiver(map: HashMap<String, String>?) {
        if (!map.isNullOrEmpty()) {
            val url = map["url"]
            val username = map["username"]
            this.giver = Giver(url, username)
        }
    }

    @JsonProperty("post") fun setPost(map: HashMap<String?, String?>?) {
        if (!map.isNullOrEmpty()) {
            val url = map["url"]
            val title = map["title"]
            this.post = Post(url, title)
        }
    }

    override fun toString(): String {
        return "KudosuHistory(id=$id, action=$action, amount=$amount, model=$model, created=$created, giver=$giver, post=$post)"
    }

}
