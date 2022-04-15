package ani.saikou.others

import ani.saikou.findBetween
import ani.saikou.media.Source
import ani.saikou.toastString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.jsoup.Jsoup

object MalSyncBackup {
    operator fun get(id: Int, name: String, dub: Boolean = false): Source? {
        try {
            val json = Jsoup.connect("https://raw.githubusercontent.com/MALSync/MAL-Sync-Backup/master/data/anilist/anime/$id.json").ignoreHttpErrors(true).ignoreContentType(true).get().body().text()
            if (json != "404: Not Found")
                when (name) {
                    "Gogoanime" -> {
                        Json.decodeFromString<JsonObject>(json)["Pages"]?.jsonObject?.get(name)?.also {
                            val slug = it.toString().replace("\n", "").findBetween((if (dub) "-dub" else "") + "\":{\"identifier\":\"", "\",")
                            if (slug != null) {
                                return Source(slug, "Automatically", "")
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            toastString(e.toString())
        }
        return null
    }
}
