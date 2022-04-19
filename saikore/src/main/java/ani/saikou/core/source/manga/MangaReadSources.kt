package ani.saikou.core.source.manga

abstract class MangaReadSources {
    open val names: ArrayList<String> = arrayListOf()
    abstract operator fun get(i: Int): MangaParser?
    abstract fun flushLive()
}