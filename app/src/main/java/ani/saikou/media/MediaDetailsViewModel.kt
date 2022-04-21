package ani.saikou.media

import android.app.Activity
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import ani.saikou.anilist.Anilist
import ani.saikou.anime.Episode
import ani.saikou.anime.SelectorDialogFragment
import ani.saikou.anime.source.WatchSources
import ani.saikou.loadData
import ani.saikou.logger
import ani.saikou.manga.MangaChapter
import ani.saikou.manga.source.MangaReadSources
import ani.saikou.others.AnimeFillerList
import ani.saikou.others.Kitsu
import ani.saikou.saveData
import ani.saikou.toastString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class MediaDetailsViewModel:ViewModel() {
    fun saveSelected(id:Int,data:Selected,activity: Activity){
        saveData("$id-select",data,activity)
    }
    fun loadSelected(media: Media):Selected{
        return loadData<Selected>("${media.id}-select")?: Selected().let {
            it.source = if(media.isAdult) 0 else when(media.anime!=null) {
                true-> loadData("settings_default_anime_source")?:0
                else-> loadData("settings_default_manga_source")?:0
            }
            it
        }
    }

    var continueMedia:Boolean? = null
    private var loading=false

    private val media: MutableLiveData<Media> = MutableLiveData<Media>(null)
    fun getMedia(): LiveData<Media> = media
    fun loadMedia(m:Media) {
        if(!loading) {
            loading = true
            media.postValue(Anilist.query.mediaDetails(m))
        }
        loading=false
    }
    fun setMedia(m:Media) {
        media.postValue(m)
    }

    val sources = MutableLiveData<ArrayList<Source>?>(null)

    //Anime
    private val kitsuEpisodes: MutableLiveData<MutableMap<String,Episode>> = MutableLiveData<MutableMap<String,Episode>>(null)
    fun getKitsuEpisodes() : LiveData<MutableMap<String,Episode>> = kitsuEpisodes
    fun loadKitsuEpisodes(s:Media){ if (kitsuEpisodes.value==null) kitsuEpisodes.postValue(Kitsu.getKitsuEpisodesDetails(s))}

    private val fillerEpisodes: MutableLiveData<MutableMap<String,Episode>> = MutableLiveData<MutableMap<String,Episode>>(null)
    fun getFillerEpisodes() : LiveData<MutableMap<String,Episode>> = fillerEpisodes
    fun loadFillerEpisodes(s:Media){ if (fillerEpisodes.value==null) fillerEpisodes.postValue(AnimeFillerList.getFillers(s.idMAL?:return))}

    var watchAnimeWatchSources:WatchSources?=null

    private val episodes: MutableLiveData<MutableMap<Int,MutableMap<String,Episode>>> = MutableLiveData<MutableMap<Int,MutableMap<String,Episode>>>(null)
    private val epsLoaded = mutableMapOf<Int,MutableMap<String,Episode>>()
    fun getEpisodes() : LiveData<MutableMap<Int,MutableMap<String,Episode>>> = episodes
    fun loadEpisodes(media: Media,i:Int){
        if(!epsLoaded.containsKey(i)) {
            epsLoaded[i] = watchAnimeWatchSources?.get(i)!!.getEpisodes(media)
        }
        episodes.postValue(epsLoaded)
    }
    fun overrideEpisodes(i: Int, source: Source,id:Int){
        watchAnimeWatchSources?.get(i)!!.saveSource(source,id)
        epsLoaded[i] = watchAnimeWatchSources?.get(i)!!.getSlugEpisodes(source.link)
        episodes.postValue(epsLoaded)
    }

    private var episode: MutableLiveData<Episode?> = MutableLiveData<Episode?>(null)
    fun getEpisode() : LiveData<Episode?> = episode
    fun loadEpisodeStreams(ep: Episode,i:Int,post:Boolean=true){
        if(!ep.allStreams || ep.videoServers.isNullOrEmpty() || !ep.saveStreams) {
            watchAnimeWatchSources?.get(i)?.getStreams(ep)?.apply {
                this.allStreams = true
            }
        }
        if(post) {
            episode.postValue(ep)
            MainScope().launch(Dispatchers.Main) {
                episode.value = null
            }
        }

    }
    fun loadEpisodeStream(ep: Episode,selected: Selected,post: Boolean=true):Boolean{
        return if(selected.stream!=null) {
            if(ep.videoServers.isNullOrEmpty() || !ep.saveStreams) {
                watchAnimeWatchSources?.get(selected.source)?.getStream(ep, selected.stream!!)?.apply {
                    this.allStreams = false
                }
            }
            if (post) {
                episode.postValue(ep)
                MainScope().launch(Dispatchers.Main) {
                    episode.value = null
                }
            }
            true
        } else false
    }
    fun setEpisode(ep: Episode?,who:String){
        logger("set episode ${ep?.number} - $who",false)
        episode.postValue(ep)
        MainScope().launch(Dispatchers.Main) {
            episode.value = null
        }
    }

    val epChanged = MutableLiveData(true)
    fun onEpisodeClick(media: Media, i:String,manager:FragmentManager,launch:Boolean=true,prevEp:String?=null){
        Handler(Looper.getMainLooper()).post{
            if(manager.findFragmentByTag("dialog")==null && !manager.isDestroyed) {
                if (media.anime?.episodes?.get(i)!=null) {
                    media.anime.selectedEpisode = i
                }
                else {
                    toastString("Couldn't find episode : $i")
                    return@post
                }
                media.selected = this.loadSelected(media)
                val selector = SelectorDialogFragment.newInstance(media.selected!!.stream, launch, prevEp)
                selector.show(manager, "dialog")
            }
        }
    }


    //Manga
    var readMangaReadSources: MangaReadSources?=null

    private val mangaChapters: MutableLiveData<MutableMap<Int,MutableMap<String,MangaChapter>>> = MutableLiveData<MutableMap<Int,MutableMap<String,MangaChapter>>>(null)
    private val mangaLoaded = mutableMapOf<Int,MutableMap<String,MangaChapter>>()
    fun getMangaChapters() : LiveData<MutableMap<Int,MutableMap<String,MangaChapter>>> = mangaChapters
    fun loadMangaChapters(media:Media,i:Int){
        logger("Loading Manga Chapters : $mangaLoaded")
        if(!mangaLoaded.containsKey(i)){
            mangaLoaded[i] = readMangaReadSources?.get(i)!!.getChapters(media)
        }
        mangaChapters.postValue(mangaLoaded)
    }

    fun overrideMangaChapters(i: Int, source: Source,id:Int){
        readMangaReadSources?.get(i)!!.saveSource(source,id)
        mangaLoaded[i] = readMangaReadSources?.get(i)!!.getLinkChapters(source.link)
        mangaChapters.postValue(mangaLoaded)
    }

    private val mangaChapter = MutableLiveData<MangaChapter?>(null)
    fun getMangaChapter(): LiveData<MangaChapter?> = mangaChapter
    fun loadMangaChapterImages(chapter: MangaChapter,selected: Selected){
        readMangaReadSources?.get(selected.source)?.getChapter(chapter)
        mangaChapter.postValue(chapter)
    }
}