package ani.saikou.core.util.extension

import ani.saikou.core.model.anilist.Banner
import ani.saikou.core.model.anilist.Genre
import ani.saikou.core.model.media.Source
import kotlin.math.min

fun MutableList<Source>.sortByTitle(string: String) {
    val temp: MutableMap<Int, Int> = mutableMapOf()
    for (i in 0 until this.size) {
        temp[i] = levenshtein(string.lowercase(), this[i].name.lowercase())
    }
    val c = temp.toList().sortedBy { (_, value) -> value }.toMap()
    val a = ArrayList(c.keys.toList().subList(0, min(this.size, 25)))
    val b = c.values.toList().subList(0, min(this.size, 25))
    for (i in b.indices.reversed()) {
        if (b[i] > 18 && i < a.size) {
            a.removeAt(i)
        }
    }
    val temp2 = arrayListOf<Source>()
    temp2.addAll(this)
    this.clear()
    for (i in a.indices) {
        add(temp2[a[i]])
    }
}

fun MutableMap<String, Genre>.checkId(id: Int): Boolean {
    forEach {
        if (it.value.id == id) {
            return false
        }
    }
    return true
}

fun MutableMap<String, Genre>.checkGenreTime(genre: String): Boolean {
    if (containsKey(genre)) {
        return (System.currentTimeMillis() - get(genre)!!.time) >= (1000 * 60 * 60 * 24 * 7)
    }
    return true
}

fun MutableMap<String, Banner>.checkBannerTime(type: String): Boolean {
    if (containsKey(type)) {
        return (System.currentTimeMillis() - get(type)!!.time) >= (1000 * 60 * 60 * 6)
    }
    return true
}