package ani.saikou.anime.newsrc

import ani.saikou.Registry
import ani.saikou.anime.source.extractors.FPlayer
import ani.saikou.anime.source.extractors.GogoCDN
import ani.saikou.anime.source.extractors.RapidCloud
import ani.saikou.anime.source.extractors.StreamSB

/**
 * [Registry] implementation for anime extractors.
 */
object AnimeExtractorRegistry : Registry<IAnimeExtractor>() {
    init {
        this += FPlayer
        this += GogoCDN
        this += StreamSB
        this += RapidCloud
    }

    fun findFor(url: String): IAnimeExtractor? {
        val httpsUrl = toHttps(url)
        return this.values().firstOrNull { it.canResolve(httpsUrl) }
    }

    fun findDomain(url: String): String =
        Regex("""(?<=^http[s]?://).+?(?=/)""").find(url)!!.value

    private fun toHttps(url: String): String {
        return if (url.take(2) == "//") "https:$url"
        else url
    }
}