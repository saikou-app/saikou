package ani.saikou.anime.newsrc

import ani.saikou.Registry
import ani.saikou.anime.source.parsers.*

/**
 * [Registry] implementation for anime providers.
 */
object AnimeSourceRegistry: Registry<AnimeProvider>() {
    // Add defaults
    init {
        this += Gogo()
        this += Gogo(true)
        this += NineAnime()
        this += NineAnime(true)
        this += Tenshi()
        this += Twist
        this += Zoro

        // Adult
        this += Haho
//        this += HentaiFF
    }

    fun flush() {
        this.values().forEach {
            it.status = ""
        }
    }
}