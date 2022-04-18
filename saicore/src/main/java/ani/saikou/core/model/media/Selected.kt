package ani.saikou.core.model.media

import java.io.Serializable

data class Selected(
    var window: Int = 0,
    var recyclerStyle: Int? = null,
    var recyclerReversed: Boolean = false,
    var source: Int = 0,
    var stream: String? = null,
    var quality: Int = 0,
//    var positions: MutableMap<String,Long>?=null,
    var chip: Int = 0,
) : Serializable
