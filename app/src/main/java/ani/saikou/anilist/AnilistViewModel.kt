package ani.saikou.anilist

import android.app.Activity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import ani.saikou.media.Media
import ani.saikou.others.AppUpdater
import ani.saikou.toastString


suspend fun getUserId(update: Runnable){
    if (Anilist.userid == null && Anilist.token!=null) {
        if (Anilist.query.getUserData())
            update.run()
        else
            toastString("Error loading Data")
    }
    else update.run()
}

class AnilistHomeViewModel : ViewModel() {
    private val listImages: MutableLiveData<ArrayList<String?>> = MutableLiveData<ArrayList<String?>>(arrayListOf())
    fun getListImages(): LiveData<ArrayList<String?>> = listImages
    suspend fun setListImages() = listImages.postValue(Anilist.query.getBannerImages())

    private val animeContinue: MutableLiveData<ArrayList<Media>> = MutableLiveData<ArrayList<Media>>(null)
    fun getAnimeContinue(): LiveData<ArrayList<Media>> = animeContinue
    suspend fun setAnimeContinue() = animeContinue.postValue(Anilist.query.continueMedia("ANIME"))

    private val animeFav: MutableLiveData<ArrayList<Media>> = MutableLiveData<ArrayList<Media>>(null)
    fun getAnimeFav(): LiveData<ArrayList<Media>> = animeFav
    suspend fun setAnimeFav() = animeFav.postValue(Anilist.query.favMedia(true))

    private val mangaContinue: MutableLiveData<ArrayList<Media>> = MutableLiveData<ArrayList<Media>>(null)
    fun getMangaContinue(): LiveData<ArrayList<Media>> = mangaContinue
    suspend fun setMangaContinue() = mangaContinue.postValue(Anilist.query.continueMedia("MANGA"))

    private val mangaFav: MutableLiveData<ArrayList<Media>> = MutableLiveData<ArrayList<Media>>(null)
    fun getMangaFav(): LiveData<ArrayList<Media>> = mangaFav
    suspend fun setMangaFav() = mangaFav.postValue(Anilist.query.favMedia(false))

    private val recommendation: MutableLiveData<ArrayList<Media>> = MutableLiveData<ArrayList<Media>>(null)
    fun getRecommendation(): LiveData<ArrayList<Media>> = recommendation
    suspend fun setRecommendation() = recommendation.postValue(Anilist.query.recommendations())

    suspend fun loadMain(context: Activity) {
        Anilist.getSavedToken(context)
        AppUpdater.check(context)
        genres.postValue(Anilist.query.getGenresAndTags(context))
    }

    val empty = MutableLiveData<Boolean>(null)

    var loaded: Boolean = false
    val genres: MutableLiveData<Boolean?> = MutableLiveData(null)
}

class AnilistAnimeViewModel : ViewModel() {
    var searched = false
    var notSet = true
    lateinit var searchResults: SearchResults
    private val type = "ANIME"
    private val trending: MutableLiveData<ArrayList<Media>> = MutableLiveData<ArrayList<Media>>(null)
    fun getTrending(): LiveData<ArrayList<Media>> = trending
    suspend fun loadTrending() = trending.postValue(Anilist.query.search(type, perPage = 10, sort = "Trending", hd = true)?.results)

    private val updated: MutableLiveData<ArrayList<Media>> = MutableLiveData<ArrayList<Media>>(null)
    fun getUpdated(): LiveData<ArrayList<Media>> = updated
    suspend fun loadUpdated() = updated.postValue(Anilist.query.recentlyUpdated())

    private val animePopular = MutableLiveData<SearchResults?>(null)
    fun getPopular(): LiveData<SearchResults?> = animePopular
    suspend fun loadPopular(type: String, search_val: String? = null, genres: ArrayList<String>? = null, sort: String = "Popular") =
        animePopular.postValue(Anilist.query.search(type, search = search_val, sort = sort, genres = genres))

    suspend fun loadNextPage(r: SearchResults) = animePopular.postValue(
        Anilist.query.search(
            r.type,
            r.page + 1,
            r.perPage,
            r.search,
            r.sort,
            r.genres,
            r.tags,
            r.format,
            r.isAdult,
            r.onList
        )
    )

    var loaded: Boolean = false
}

class AnilistMangaViewModel : ViewModel() {
    var searched = false
    var notSet = true
    lateinit var searchResults: SearchResults
    private val type = "MANGA"
    private val trending: MutableLiveData<ArrayList<Media>> = MutableLiveData<ArrayList<Media>>(null)
    fun getTrending(): LiveData<ArrayList<Media>> = trending
    suspend fun loadTrending() = trending.postValue(Anilist.query.search(type, perPage = 10, sort = "Trending", hd = true)?.results)

    private val updated: MutableLiveData<ArrayList<Media>> = MutableLiveData<ArrayList<Media>>(null)
    fun getTrendingNovel(): LiveData<ArrayList<Media>> = updated
    suspend fun loadTrendingNovel() =
        updated.postValue(Anilist.query.search(type, perPage = 10, sort = "Trending", format = "NOVEL")?.results)

    private val mangaPopular = MutableLiveData<SearchResults?>(null)
    fun getPopular(): LiveData<SearchResults?> = mangaPopular
    suspend fun loadPopular(type: String, search_val: String? = null, genres: ArrayList<String>? = null, sort: String? = "Popular") =
        mangaPopular.postValue(Anilist.query.search(type, search = search_val, sort = sort, genres = genres))

    suspend fun loadNextPage(r: SearchResults) = mangaPopular.postValue(
        Anilist.query.search(
            r.type,
            r.page + 1,
            r.perPage,
            r.search,
            r.sort,
            r.genres,
            r.tags,
            r.format,
            r.isAdult,
            r.onList
        )
    )

    var loaded: Boolean = false
}

class AnilistSearch : ViewModel() {
    var searched = false
    var notSet = true
    lateinit var searchResults: SearchResults
    private val search: MutableLiveData<SearchResults?> = MutableLiveData<SearchResults?>(null)

    fun getSearch(): LiveData<SearchResults?> = search
    suspend fun loadSearch(
        type: String,
        search_val: String? = null,
        genres: ArrayList<String>? = null,
        tags: ArrayList<String>? = null,
        sort: String? = "",
        adult: Boolean = false,
        listOnly: Boolean? = null
    ) = search.postValue(
        Anilist.query.search(
            type,
            search = search_val,
            sort = sort,
            genres = genres,
            tags = tags,
            isAdult = adult,
            onList = listOnly
        )
    )

    suspend fun loadNextPage(r: SearchResults) = search.postValue(
        Anilist.query.search(
            r.type,
            r.page + 1,
            r.perPage,
            r.search,
            r.sort,
            r.genres,
            r.tags,
            r.format,
            r.isAdult,
            r.onList
        )
    )
}

class GenresViewModel : ViewModel() {
    var genres: MutableMap<String, String>? = null
    var done = false
    var doneListener: (() -> Unit)? = null
    suspend fun loadGenres(genre: ArrayList<String>, listener: (Pair<String, String>) -> Unit) {
        if (genres == null) {
            genres = mutableMapOf()
            Anilist.query.getGenres(genre) {
                genres!![it.first] = it.second
                listener.invoke(it)
                if (genres!!.size == genre.size) {
                    done = true
                    doneListener?.invoke()
                }
            }
        }
    }
}