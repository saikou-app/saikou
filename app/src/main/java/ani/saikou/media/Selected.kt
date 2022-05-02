package ani.saikou.media

import java.io.Serializable

data class Selected(
    var window: Int = 0,
    var recyclerStyle: Int? = null,
    var recyclerReversed: Boolean = false,
    var source: Int = 0,
    var server: String? = null,
    var video: Int = 0,
    var chip: Int = 0,
) : Serializable
