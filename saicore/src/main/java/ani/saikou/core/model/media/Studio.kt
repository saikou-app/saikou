package ani.saikou.core.model.media

import java.io.Serializable

data class Studio(
    val id: String,
    val name: String,
    var yearMedia: MutableMap<String, MutableList<Media>>? = null
) : Serializable
