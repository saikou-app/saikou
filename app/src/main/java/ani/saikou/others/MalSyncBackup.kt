package ani.saikou.others

import ani.saikou.httpClient
import ani.saikou.media.Source
import com.fasterxml.jackson.annotation.JsonProperty

object MalSyncBackup {
    data class MalBackUpSync(
        @JsonProperty("Pages")
        val pages: Map<String, Map<String, Page>>? = null
    )

    data class Page(
        val identifier: String,
        val title: String,
        val url: String? = null,
        val image: String? = null,
        val active: Boolean? = null,
    )

    suspend fun get(id: Int, name: String, dub: Boolean = false): Source? {
        try {
            val json =
                httpClient.get("https://raw.githubusercontent.com/MALSync/MAL-Sync-Backup/master/data/anilist/anime/$id.json")
            if (json.text != "404: Not Found")
                json.parsed<MalBackUpSync>().pages?.get(name)?.forEach {
                    val page = it.value
                    val slug = if (dub)
                        if (page.title.lowercase().endsWith("(dub)")) {
                            page.identifier
                        } else null
                    else page.identifier
                    if(slug!=null && page.active==true){
                        return Source(slug,page.title,page.image?:"")
                    }
                }
        } catch (e: Exception) {
            logError(e)
        }
        return null
    }
}