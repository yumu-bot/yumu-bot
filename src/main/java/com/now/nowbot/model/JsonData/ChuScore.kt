package com.now.nowbot.model.jsonData

import com.fasterxml.jackson.annotation.JsonProperty

class ChuScore {
    @JsonProperty("cid") 
    var chartID: Int = 0
    
     // 定数，也就是实际难度，类似于 osu 的星数
    @JsonProperty("ds") 
    var star: Double = 0.0
    
     // 纯分数
    @JsonProperty("score") 
    var score: Int = 0
    
     // 连击状态，有 FC 和 AP，空字符串就是啥都没有
    @JsonProperty("fc") 
    var combo: String = ""
    
     // 定数的实际显示，0.6-0.9 后面会多一个 +
    @JsonProperty("level") 
    var level: String = ""
    
     // 定数的位置，0-4
    @JsonProperty("level_index") 
    var index: Int = 0
    
     // 实际所属的难度分类，Basic，Advanced，Expert，Master，Ultima, World's End
    @JsonProperty("level_label") 
    var difficulty: String = ""
    
    @JsonProperty("mid") 
    var musicID: Int = 0
    
     // CHUNITHM rating，也就是 PP，通过计算向下取整
    @JsonProperty("ra") 
    var rating: Int = 0
    
    var title: String = ""
}
