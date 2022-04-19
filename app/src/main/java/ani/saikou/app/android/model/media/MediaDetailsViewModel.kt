package ani.saikou.app.android.model.media

import android.app.Activity
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import ani.saikou.app.android.fragment.anime.SelectorDialogFragment
import ani.saikou.app.util.anilist.anilist.Anilist
import ani.saikou.app.util.loadData
import ani.saikou.app.util.logger
import ani.saikou.app.util.others.AnimeFillerList
import ani.saikou.app.util.others.Kitsu
import ani.saikou.app.util.saveData
import ani.saikou.app.util.toastString
import ani.saikou.core.model.anime.Episode
import ani.saikou.core.model.manga.MangaChapter
import ani.saikou.core.model.media.Media
import ani.saikou.core.model.media.Selected
import ani.saikou.core.model.media.Source
import ani.saikou.core.source.anime.WatchSources
import ani.saikou.core.source.manga.MangaReadSources
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class MediaDetailsViewModel : ViewModel() {
    var continueMedia: Boolean? = null
    private var loading = false

    private val _media: MutableLiveData<Media> = MutableLiveData<Media>(null)
    val media: LiveData<Media>
        get() = _media

    val sources = MutableLiveData<List<Source>?>(null)

    // Anime
    private val _kitsuEpisodes: MutableLiveData<MutableMap<String, Episode>> =
        MutableLiveData<MutableMap<String, Episode>>(null)
    val kitsuEpisodes: LiveData<MutableMap<String, Episode>>
        get() = _kitsuEpisodes

    private val _fillerEpisodes: MutableLiveData<MutableMap<String, Episode>> =
        MutableLiveData<MutableMap<String, Episode>>(null)
    val fillerEpisodes: LiveData<MutableMap<String, Episode>>
        get() = _fillerEpisodes

    var watchAnimeWatchSources: WatchSources? = null

    private val _episodes: MutableLiveData<MutableMap<Int, MutableMap<String, Episode>>> =
        MutableLiveData<MutableMap<Int, MutableMap<String, Episode>>>(null)
    val episodes: LiveData<MutableMap<Int, MutableMap<String, Episode>>>
        get() = _episodes

    private var _currentEpsiode: MutableLiveData<Episode?> = MutableLiveData<Episode?>(null)
    val currentEpisode: LiveData<Episode?>
        get() = _currentEpsiode

    private val epsLoaded = mutableMapOf<Int, MutableMap<String, Episode>>()

    // Manga
    var readMangaReadSources: MangaReadSources? = null

    private val _mangaChapters: MutableLiveData<MutableMap<Int, MutableMap<String, MangaChapter>>> =
        MutableLiveData<MutableMap<Int, MutableMap<String, MangaChapter>>>(null)
    val mangaChapters: LiveData<MutableMap<Int, MutableMap<String, MangaChapter>>>
        get() = _mangaChapters

    private val _currentChapter = MutableLiveData<MangaChapter?>(null)
    val currentChapter: LiveData<MangaChapter?>
        get() = _currentChapter

    private val mangaLoaded = mutableMapOf<Int, MutableMap<String, MangaChapter>>()

    fun saveSelected(id: Int, data: Selected, activity: Activity) {
        saveData("$id-select", data, activity)
    }

    fun loadSelected(media: Media): Selected {
        return loadData<Selected>("${media.id}-select") ?: Selected().let {
            it.source = if (media.isAdult) 0 else when (media.anime != null) {
                true -> loadData("settings_default_anime_source") ?: 0
                else -> loadData("settings_default_manga_source") ?: 0
            }
            it
        }
    }

    fun loadMedia(m: Media) {
        if (!loading) {
            loading = true
            _media.postValue(Anilist.query.mediaDetails(m))
        }
        loading = false
    }

    fun setMedia(m: Media) {
        _media.postValue(m)
    }

    fun loadKitsuEpisodes(s: Media) {
        if (_kitsuEpisodes.value == null) {
            _kitsuEpisodes.postValue(Kitsu.getKitsuEpisodesDetails(s))
        }
    }

    fun loadFillerEpisodes(s: Media) {
        if (_fillerEpisodes.value == null) _fillerEpisodes.postValue(
            AnimeFillerList.getFillers(
                s.idMAL ?: return
            )
        )
    }

    fun loadEpisodes(media: Media, i: Int) {
        if (!epsLoaded.containsKey(i)) {
            epsLoaded[i] = watchAnimeWatchSources?.get(i)!!.getEpisodes(media)
        }
        _episodes.postValue(epsLoaded)
    }

    fun overrideEpisodes(i: Int, source: Source, id: Int) {
        watchAnimeWatchSources?.get(i)!!.saveSource(source, id)
        epsLoaded[i] = watchAnimeWatchSources?.get(i)!!.getSlugEpisodes(source.link)
        _episodes.postValue(epsLoaded)
    }

    fun loadEpisodeStreams(ep: Episode, i: Int, post: Boolean = true) {
        if (!ep.allStreams || ep.streamLinks.isNullOrEmpty() || !ep.saveStreams) {
            watchAnimeWatchSources?.get(i)?.getStreams(ep)?.apply {
                this.allStreams = true
            }
        }
        if (post) {
            _currentEpsiode.postValue(ep)
            MainScope().launch(Dispatchers.Main) {
                _currentEpsiode.value = null
            }
        }

    }

    fun loadEpisodeStream(ep: Episode, selected: Selected, post: Boolean = true): Boolean {
        return if (selected.stream != null) {
            if (ep.streamLinks.isNullOrEmpty() || !ep.saveStreams) {
                watchAnimeWatchSources?.get(selected.source)?.getStream(ep, selected.stream!!)
                    ?.apply {
                        this.allStreams = false
                    }
            }
            if (post) {
                _currentEpsiode.postValue(ep)
                MainScope().launch(Dispatchers.Main) {
                    _currentEpsiode.value = null
                }
            }
            true
        } else false
    }

    fun setEpisode(ep: Episode?, who: String) {
        logger("set episode ${ep?.number} - $who", false)
        _currentEpsiode.postValue(ep)
        MainScope().launch(Dispatchers.Main) {
            _currentEpsiode.value = null
        }
    }

    val epChanged = MutableLiveData(true)

    fun onEpisodeClick(
        media: Media,
        i: String,
        manager: FragmentManager,
        launch: Boolean = true,
        prevEp: String? = null
    ) {
        Handler(Looper.getMainLooper()).post {
            if (manager.findFragmentByTag("dialog") == null && !manager.isDestroyed) {
                if (media.anime?.episodes?.get(i) != null) {
                    media.anime!!.selectedEpisode = i
                } else {
                    toastString("Couldn't find episode : $i")
                    return@post
                }
                media.selected = this.loadSelected(media)
                val selector =
                    SelectorDialogFragment.newInstance(media.selected!!.stream, launch, prevEp)
                selector.show(manager, "dialog")
            }
        }
    }

    fun loadMangaChapters(media: Media, i: Int) {
        logger("Loading Manga Chapters : $mangaLoaded")
        if (!mangaLoaded.containsKey(i)) {
            mangaLoaded[i] = readMangaReadSources?.get(i)!!.getChapters(media)
        }
        _mangaChapters.postValue(mangaLoaded)
    }

    fun overrideMangaChapters(i: Int, source: Source, id: Int) {
        readMangaReadSources?.get(i)!!.saveSource(source, id)
        mangaLoaded[i] = readMangaReadSources?.get(i)!!.getLinkChapters(source.link)
        _mangaChapters.postValue(mangaLoaded)
    }

    fun loadMangaChapterImages(chapter: MangaChapter, selected: Selected) {
        readMangaReadSources?.get(selected.source)?.getChapter(chapter)
        _currentChapter.postValue(chapter)
    }
}