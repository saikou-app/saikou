package ani.saikou.core.model.manga

import com.bumptech.glide.load.Transformation
import java.io.File
import java.io.Serializable

data class MangaChapter(
    val number: String,
    var title: String? = null,
    var link: String? = null,
    var headers: MutableMap<String, String>? = null,
    var transformation: Transformation<File>? = null,
    var images: ArrayList<String>? = null
) : Serializable
