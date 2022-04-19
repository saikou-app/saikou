package ani.saikou.core.model.anilist

import java.io.Serializable

data class Banner(
    val url: String?,
    var time: Long,
) : Serializable {
    fun checkTime(): Boolean {
        return (System.currentTimeMillis() - time) >= (1000 * 60 * 60 * 6)
    }
}