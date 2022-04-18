package ani.saikou.anilist

import java.io.Serializable

data class Genre(
    val name: String,
    var id: Int,
    var thumbnail: String,
    var time: Long,
) : Serializable