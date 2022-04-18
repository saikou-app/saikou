package ani.saikou.android.manga.source

abstract class MangaReadSources {
    open val names: ArrayList<String> = arrayListOf()
    abstract operator fun get(i: Int): MangaParser?
    abstract fun flushLive()
}