package ani.saikou.anime.source

abstract class WatchSources {
    open val names: List<String> = arrayListOf()
    abstract operator fun get(i: Int): AnimeParser?
    abstract fun flushLive()
}