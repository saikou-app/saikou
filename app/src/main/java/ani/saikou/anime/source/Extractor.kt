package ani.saikou.anime.source

import ani.saikou.anime.Episode

abstract class Extractor {
    abstract fun getStreamLinks(name: String, url: String): Episode.StreamLinks
}