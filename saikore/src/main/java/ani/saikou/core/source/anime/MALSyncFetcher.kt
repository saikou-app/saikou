package ani.saikou.core.source.anime

import ani.saikou.core.model.media.Source
import ani.saikou.core.service.LOG
import ani.saikou.core.util.extension.findBetween
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.jsoup.Jsoup
import java.util.*

/**
 * Allows for retrieving of anime metadata from the MALSync database.
 */
object MALSyncFetcher {
    private const val BASE_URL =
        "https://raw.githubusercontent.com/MALSync/MAL-Sync-Backup/master/data/anilist/anime/%s.json"

    val fetchers: MutableMap<String, MALSyncFetcherInterface> =
        mutableMapOf(
            "gogoanime" to GogoAnimeFetcher,
        )

    private val lookupCache: MutableMap<Int, Source?> =
        mutableMapOf()

    /**
     * Retrieves the anime metadata for the given anime ID.
     *
     * @param animeId The anime ID.
     * @param hostname The host website name.
     * @param dub Whether the anime is dubbed.
     *
     * @return The anime source, or null if unavailable.
     */
    fun fetch(animeId: Int, hostname: String, dub: Boolean = false): Source? = run {
        val hash = Objects.hash(animeId, hostname, dub)

        lookupCache[hash] ?: run {
            fetchSource(hash, animeId, hostname, dub)
        }
    }

    private fun fetchSource(hash: Int, animeId: Int, hostname: String, dub: Boolean = false): Source? {
        val source = run {
            try {
                val json = Jsoup.connect(BASE_URL.format(animeId))
                    .ignoreHttpErrors(true)
                    .ignoreContentType(true)
                    .get()
                    .body()
                    .text()
                if (json.contains("404") && json.lowercase().contains("not found")) {
                    return null
                }

                val fetcher = fetchers[hostname.lowercase()]
                if (fetcher == null) {
                    LOG.notify("No fetcher found for: $hostname")
                    return null
                }

                return@run fetcher.fetch(Json.decodeFromString(json), dub)
            } catch (e: Exception) {
                LOG.notify(e.toString())
            }
            return null
        }

        lookupCache[hash] = source
        return source
    }
}

fun interface MALSyncFetcherInterface {
    fun fetch(jsonObject: JsonObject, dub: Boolean): Source?
}

object GogoAnimeFetcher : MALSyncFetcherInterface {
    override fun fetch(jsonObject: JsonObject, dub: Boolean): Source? {
        val base = jsonObject["Pages"]?.jsonObject?.get("Gogoanime").toString()
        val slug = base.replace("\n", "").findBetween(
            (if (dub) "-dub" else "") + "\":{\"identifier\":\"",
            "\","
        )
        return slug?.let { Source(slug, "Automatically", "") }
    }
}