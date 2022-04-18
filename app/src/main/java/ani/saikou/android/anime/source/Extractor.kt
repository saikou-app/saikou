package ani.saikou.android.anime.source

import ani.saikou.core.model.anime.Episode

abstract class Extractor {
    abstract fun getStreamLinks(name: String, url: String): Episode.StreamLinks
}