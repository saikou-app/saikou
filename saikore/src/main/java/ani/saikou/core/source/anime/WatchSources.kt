package ani.saikou.core.source.anime

abstract class WatchSources {
    open val names: ArrayList<String> = arrayListOf()
    abstract operator fun get(i: Int): AnimeParser?
    abstract fun flushLive()
}