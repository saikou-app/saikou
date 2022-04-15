package ani.saikou.anilist

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import ani.saikou.media.Media

class AnilistHomeViewModel : ViewModel() {
    private val listImages: MutableLiveData<ArrayList<String?>> = MutableLiveData<ArrayList<String?>>(arrayListOf())
    fun getListImages(): LiveData<ArrayList<String?>> = listImages
    fun setListImages() = listImages.postValue(Anilist.query.getBannerImages())

    private val animeContinue: MutableLiveData<ArrayList<Media>> = MutableLiveData<ArrayList<Media>>(null)
    fun getAnimeContinue(): LiveData<ArrayList<Media>> = animeContinue
    fun setAnimeContinue() = animeContinue.postValue(Anilist.query.continueMedia("ANIME"))

    private val animeFav: MutableLiveData<ArrayList<Media>> = MutableLiveData<ArrayList<Media>>(null)
    fun getAnimeFav(): LiveData<ArrayList<Media>> = animeFav
    fun setAnimeFav() = animeFav.postValue(Anilist.query.favMedia(true))

    private val mangaContinue: MutableLiveData<ArrayList<Media>> = MutableLiveData<ArrayList<Media>>(null)
    fun getMangaContinue(): LiveData<ArrayList<Media>> = mangaContinue
    fun setMangaContinue() = mangaContinue.postValue(Anilist.query.continueMedia("MANGA"))

    private val mangaFav: MutableLiveData<ArrayList<Media>> = MutableLiveData<ArrayList<Media>>(null)
    fun getMangaFav(): LiveData<ArrayList<Media>> = mangaFav
    fun setMangaFav() = mangaFav.postValue(Anilist.query.favMedia(false))

    private val recommendation: MutableLiveData<ArrayList<Media>> = MutableLiveData<ArrayList<Media>>(null)
    fun getRecommendation(): LiveData<ArrayList<Media>> = recommendation
    fun setRecommendation() = recommendation.postValue(Anilist.query.recommendations())

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
    fun loadTrending() = trending.postValue(Anilist.query.search(type, perPage = 10, sort = "Trending", hd = true)?.results)

    private val updated: MutableLiveData<ArrayList<Media>> = MutableLiveData<ArrayList<Media>>(null)
    fun getUpdated(): LiveData<ArrayList<Media>> = updated
    fun loadUpdated() = updated.postValue(Anilist.query.recentlyUpdated())

    private val animePopular = MutableLiveData<SearchResults?>(null)
    fun getPopular(): LiveData<SearchResults?> = animePopular
    fun loadPopular(type: String, search_val: String? = null, genres: ArrayList<String>? = null, sort: String = "Popular") = animePopular.postValue(Anilist.query.search(type, search = search_val, sort = sort, genres = genres))
    fun loadNextPage(r: SearchResults) = animePopular.postValue(Anilist.query.search(r.type, r.page + 1, r.perPage, r.search, r.sort, r.genres, r.tags, r.format, r.isAdult, r.onList))

    var loaded: Boolean = false
}

class AnilistMangaViewModel : ViewModel() {
    var searched = false
    var notSet = true
    lateinit var searchResults: SearchResults
    private val type = "MANGA"
    private val trending: MutableLiveData<ArrayList<Media>> = MutableLiveData<ArrayList<Media>>(null)
    fun getTrending(): LiveData<ArrayList<Media>> = trending
    fun loadTrending() = trending.postValue(Anilist.query.search(type, perPage = 10, sort = "Trending", hd = true)?.results)

    private val updated: MutableLiveData<ArrayList<Media>> = MutableLiveData<ArrayList<Media>>(null)
    fun getTrendingNovel(): LiveData<ArrayList<Media>> = updated
    fun loadTrendingNovel() = updated.postValue(Anilist.query.search(type, perPage = 10, sort = "Trending", format = "NOVEL")?.results)

    private val mangaPopular = MutableLiveData<SearchResults?>(null)
    fun getPopular(): LiveData<SearchResults?> = mangaPopular
    fun loadPopular(type: String, search_val: String? = null, genres: ArrayList<String>? = null, sort: String? = "Popular") = mangaPopular.postValue(Anilist.query.search(type, search = search_val, sort = sort, genres = genres))
    fun loadNextPage(r: SearchResults) = mangaPopular.postValue(Anilist.query.search(r.type, r.page + 1, r.perPage, r.search, r.sort, r.genres, r.tags, r.format, r.isAdult, r.onList))

    var loaded: Boolean = false
}

class AnilistSearch : ViewModel() {
    var searched = false
    var notSet = true
    lateinit var searchResults: SearchResults
    private val search: MutableLiveData<SearchResults?> = MutableLiveData<SearchResults?>(null)

    fun getSearch(): LiveData<SearchResults?> = search
    fun loadSearch(type: String, search_val: String? = null, genres: ArrayList<String>? = null, tags: ArrayList<String>? = null, sort: String? = "", adult: Boolean = false, listOnly: Boolean? = null) = search.postValue(Anilist.query.search(type, search = search_val, sort = sort, genres = genres, tags = tags, isAdult = adult, onList = listOnly))

    fun loadNextPage(r: SearchResults) = search.postValue(Anilist.query.search(r.type, r.page + 1, r.perPage, r.search, r.sort, r.genres, r.tags, r.format, r.isAdult, r.onList))
}

class GenresViewModel : ViewModel() {
    var genres: MutableMap<String, String>? = null
    var done = false
    var doneListener: (() -> Unit)? = null
    fun loadGenres(genre: ArrayList<String>, listener: (Pair<String, String>) -> Unit) {
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
