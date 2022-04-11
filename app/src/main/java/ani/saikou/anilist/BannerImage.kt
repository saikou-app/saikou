package ani.saikou.anilist

import java.io.Serializable

data class BannerImage(
    val url:String,
    var time:Long,
): Serializable