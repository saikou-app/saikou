package ani.saikou.anilist

import android.app.Activity
import ani.saikou.*
import ani.saikou.anilist.api.MediaListCollection
import ani.saikou.anilist.api.MediaTag
import ani.saikou.anilist.api.Page
import ani.saikou.anilist.api.User
import ani.saikou.anilist.api.Media as ApiMedia
import ani.saikou.anilist.api.Character as ApiCharacter
import ani.saikou.anilist.api.Studio as ApiStudio
import ani.saikou.anime.Anime
import ani.saikou.manga.Manga
import ani.saikou.media.Character
import ani.saikou.media.Media
import ani.saikou.media.Studio
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.*
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import java.io.Serializable
import java.net.UnknownHostException
import kotlin.random.Random
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue

val httpClient =  OkHttpClient()
val mapper = jacksonObjectMapper()

fun executeQuery(query:String, variables:String="",force:Boolean=false,useToken:Boolean=true,show:Boolean=false): JsonObject? {
    try {
        val formBody: RequestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("query", query)
            .addFormDataPart("variables", variables)
            .build()

        val request = Request.Builder()
            .url("https://graphql.anilist.co/")
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json")
            .addHeader("user-agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/100.0.4896.75 Safari/537.36")
            .post(formBody)

        if (Anilist.token!=null || force) {
            if (Anilist.token!=null && useToken) request.header("Authorization", "Bearer ${Anilist.token}")
            val json = httpClient.newCall(request.build()).execute().body?.string()?:return null
            if(show) toastString("JSON : $json")
            val js = Json.decodeFromString<JsonObject>(json)
            if(js["data"]!=JsonNull)
                return js
        }
    } catch (e:Exception){
        if(e is UnknownHostException) toastString("Network error, please Retry.")
//        else toastString("$e")
    }
    return null
}

data class SearchResults(
    val type: String,
    var isAdult: Boolean,
    var onList: Boolean?=null,
    var perPage:Int?=null,
    var search: String? = null,
    var sort: String? = null,
    var genres: ArrayList<String>? = null,
    var tags: ArrayList<String>?=null,
    var format: String?=null,
    var page: Int=1,
    var results:ArrayList<Media>,
    var hasNextPage:Boolean,
):Serializable

class AnilistQueries{
    fun getUserData():Boolean{
        return try{
            val response = executeQuery("""{Viewer {name options{ displayAdultContent } avatar{medium} bannerImage id statistics{anime{episodesWatched}manga{chaptersRead}}}}""")!!["data"]!!.jsonObject["Viewer"]!!

            val user: User = mapper.readValue(response.toString());

            Anilist.userid = user.id
            Anilist.username = user.name
            Anilist.bg = user.bannerImage
            Anilist.avatar = user.avatar!!.medium
            Anilist.episodesWatched = user.statistics!!.anime!!.episodesWatched
            Anilist.chapterRead = user.statistics!!.manga!!.chaptersRead
            Anilist.adult = user.options!!.displayAdultContent ?: false
            true
        } catch (e: Exception){
            logger(e)
            false
        }
    }

    fun getMedia(id:Int,mal:Boolean=false):Media?{
        val response = executeQuery("""{Media(${if(!mal) "id:" else "idMal:"}$id){id idMal status chapters episodes nextAiringEpisode{episode}type meanScore isAdult isFavourite bannerImage coverImage{large}title{english romaji userPreferred}mediaListEntry{progress score(format:POINT_100)status}}}""", force = true)
        val i = (response?.get("data")?:return null).jsonObject["Media"]?:return null

        val fetchedMedia: ApiMedia = mapper.readValue(i.toString())

        if (i!=JsonNull){
            return Media(fetchedMedia)
        }
        return null
    }

    fun mediaDetails(media:Media): Media {
        media.cameFromContinue=false
        val query = """{Media(id:${media.id}){mediaListEntry{id status score(format:POINT_100) progress repeat updatedAt startedAt{year month day}completedAt{year month day}}isFavourite siteUrl idMal nextAiringEpisode{episode airingAt}source countryOfOrigin format duration season seasonYear startDate{year month day}endDate{year month day}genres studios(isMain:true){nodes{id name siteUrl}}description trailer { site id } synonyms tags { name rank isMediaSpoiler } characters(sort:[ROLE,FAVOURITES_DESC],perPage:25,page:1){edges{role node{id image{medium}name{userPreferred}}}}relations{edges{relationType(version:2)node{id idMal mediaListEntry{progress score(format:POINT_100) status} episodes chapters nextAiringEpisode{episode} popularity meanScore isAdult isFavourite title{english romaji userPreferred}type status(version:2)bannerImage coverImage{large}}}}recommendations(sort:RATING_DESC){nodes{mediaRecommendation{id idMal mediaListEntry{progress score(format:POINT_100) status} episodes chapters nextAiringEpisode{episode}meanScore isAdult isFavourite title{english romaji userPreferred}type status(version:2)bannerImage coverImage{large}}}}externalLinks{url site}}}"""
        runBlocking{
            val anilist =  async {
                var response = executeQuery(query, force = true)
                if (response != null) {
                    fun parse() {
                        val it = response!!["data"]!!.jsonObject["Media"]!!

                        val fetchedMedia: ApiMedia = mapper.readValue(it.toString())

                        media.source = fetchedMedia.source.toString()
                        media.countryOfOrigin = fetchedMedia.countryOfOrigin
                        media.format = fetchedMedia.format.toString()

                        media.startDate = FuzzyDate(
                            fetchedMedia.startDate!!.year,
                            fetchedMedia.startDate!!.month,
                            fetchedMedia.startDate!!.day
                        )

                        media.endDate = FuzzyDate(
                            fetchedMedia.endDate!!.year,
                            fetchedMedia.endDate!!.month,
                            fetchedMedia.endDate!!.day
                        )

                        if (fetchedMedia.genres != null) {
                            media.genres = arrayListOf()
                            fetchedMedia.genres!!.forEach { i ->
                                media.genres.add(i)
                            }
                        }

                        media.trailer = fetchedMedia.trailer?.let { i ->
                            if (i.site != null && i.site.toString() == "youtube")
                                "https://www.youtube.com/embed/${i.id.toString().trim('"')}"
                            else null
                        }

                        fetchedMedia.synonyms?.apply {
                            media.synonyms = arrayListOf()
                            this.forEach { i ->
                                media.synonyms.add(
                                    i
                                )
                            }
                        }

                        fetchedMedia.tags?.apply {
                            media.tags = arrayListOf()
                            this.forEach { i ->
                                if (i.isMediaSpoiler == true)
                                    media.tags.add("${i.name} : ${i.rank.toString()}%")
                            }
                        }

                        media.description = fetchedMedia.description.toString()

                        if (fetchedMedia.characters != null) {
                            media.characters = arrayListOf()
                            fetchedMedia.characters!!.edges!!.forEach { i ->
                                media.characters!!.add(
                                    Character(
                                        id = i.node!!.id,
                                        name = i.node!!.name!!.userPreferred!!,
                                        image = i.node!!.image!!.medium!!,
                                        banner = media.banner ?: media.cover,
                                        role = i.role.toString()
                                    )
                                )
                            }
                        }
                        if (fetchedMedia.relations != null) {
                            media.relations = arrayListOf()
                            fetchedMedia.relations!!.edges!!.forEach { mediaEdge ->
                                val m = Media(mediaEdge)
                                media.relations!!.add(m)
                                if (m.relation == "SEQUEL") {
                                    media.sequel = if (media.sequel == null) m
                                    else {
                                        if (media.sequel!!.popularity!! < m.popularity!!) m else media.sequel
                                    }
                                } else if (m.relation == "PREQUEL") {
                                    media.prequel = if (media.prequel == null) m
                                    else {
                                        if (media.prequel!!.popularity!! < m.popularity!!) m else media.prequel
                                    }
                                }
                            }
                            media.relations!!.sortBy { it.popularity }
                        }
                        if (fetchedMedia.recommendations != null) {
                            media.recommendations = arrayListOf()
                            fetchedMedia.recommendations!!.nodes!!.forEach { i ->
                                if (i.mediaRecommendation != null) {
                                    media.recommendations!!.add(
                                        Media(i.mediaRecommendation!!)
                                    )
                                }
                            }
                        }

                        if (fetchedMedia.mediaListEntry != null) {
                            val mediaList = fetchedMedia.mediaListEntry!!
                            media.userProgress = mediaList.progress
                            media.userListId = mediaList.id
                            media.userScore = mediaList.score!!.toInt()
                            media.userStatus = mediaList.status.toString()
                            media.userRepeat = mediaList.repeat ?: 0
                            media.userUpdatedAt = mediaList.updatedAt!!.toString().toLong() * 1000
                            media.userCompletedAt = FuzzyDate(
                                mediaList.completedAt!!.year,
                                mediaList.completedAt!!.month,
                                mediaList.completedAt!!.day,
                            )
                            media.userStartedAt = FuzzyDate(
                                mediaList.startedAt!!.year,
                                mediaList.startedAt!!.month,
                                mediaList.startedAt!!.day,
                            )
                        } else {
                            media.userStatus = null
                            media.userListId = null
                            media.userProgress = null
                            media.userScore = 0
                            media.userRepeat = 0
                            media.userUpdatedAt = null
                            media.userCompletedAt = FuzzyDate()
                            media.userStartedAt = FuzzyDate()
                        }

                        if (media.anime != null) {
                            media.anime.episodeDuration = fetchedMedia.duration
                            media.anime.season = fetchedMedia.season?.toString()
                            media.anime.seasonYear = fetchedMedia.seasonYear

                            if (fetchedMedia.studios!!.nodes!!.isNotEmpty()) {
                                val firstStudio = fetchedMedia.studios!!.nodes!![0]
                                media.anime.mainStudio = Studio(
                                    firstStudio.id.toString(),
                                    firstStudio.name!!
                                )
                            }
                            media.anime.nextAiringEpisodeTime = fetchedMedia.nextAiringEpisode?.airingAt?.toLong()

                            fetchedMedia.externalLinks!!.forEach { i ->
                                if (i.site == "YouTube") {
                                    media.anime.youtube = i.url
                                }
                            }
                        } else if (media.manga != null) {
                            logger("Nothing Here lmao", false)
                        }
                        media.shareLink = fetchedMedia.siteUrl
                    }

                    if (response["data"]?.jsonObject?.get("Media").let{it != JsonNull && it!=null} ) parse() else {
                        toastString("Adult Stuff? ( ͡° ͜ʖ ͡°)")
                        response = executeQuery(query, force = true, useToken = false)
                        if (response?.get("data")?.jsonObject?.get("Media").let{it != JsonNull && it!=null}) parse() else toastString("What did you even open?")
                    }
                }
                else{
                    toastString("Error getting Data from Anilist.")
                }
            }
            val mal = async {
                if (media.idMAL != null) {
                    getMalMedia(media)
                }
            }
            awaitAll(anilist, mal)
        }
        return media
    }

    fun continueMedia(type:String): ArrayList<Media> {
        val returnArray = arrayListOf<Media>()
        val map = mutableMapOf<Int, Media>()
        val statuses = arrayOf("CURRENT","REPEATING")
        fun repeat(status:String) {
            val response = executeQuery(""" { MediaListCollection(userId: ${Anilist.userid}, type: $type, status: $status , sort: UPDATED_TIME ) { lists { entries { progress score(format:POINT_100) status media { id idMal type isAdult status chapters episodes nextAiringEpisode {episode} meanScore isFavourite bannerImage coverImage{large} title { english romaji userPreferred } } } } } } """)
            val data = if(response?.get("data")!=null && response["data"] !=JsonNull) response["data"] else null
            val a = if(data?.jsonObject?.get("MediaListCollection")!=null && data.jsonObject["MediaListCollection"] !=JsonNull) data.jsonObject["MediaListCollection"] else null
            val mediaListCollection: MediaListCollection = mapper.readValue(a.toString())
            val lists = mediaListCollection.lists
            if (lists != null && lists.isNotEmpty()) {
                lists.forEach { li->
                    li.entries!!.reversed().forEach {
                        val m = Media(it)
                        m.cameFromContinue = true
                        map[it.media!!.id] = m
                    }
                }
            }
        }

        statuses.forEach { repeat(it) }
        val set = loadData<MutableSet<Int>>("continue_$type")
        if (set != null) {
            set.reversed().forEach {
                if (map.containsKey(it)) returnArray.add(map[it]!!)
            }
            for (i in map) {
                if (i.value !in returnArray) returnArray.add(i.value)
            }
        } else returnArray.addAll(map.values)
        return returnArray
    }

    fun favMedia(anime:Boolean): ArrayList<Media> {
        val responseArray = arrayListOf<Media>()
        try{
            val jsonUser = executeQuery("""{User(id:${Anilist.userid}){favourites{${if(anime) "anime" else "manga"}(page:0){edges{favouriteOrder node{id idMal isAdult mediaListEntry{progress score(format:POINT_100)status}chapters isFavourite episodes nextAiringEpisode{episode}meanScore isFavourite title{english romaji userPreferred}type status(version:2)bannerImage coverImage{large}}}}}}}""")?.get("data")!!.jsonObject["User"]
            val user: User = mapper.readValue(jsonUser.toString());
            val favourites = user.favourites!!
            val apiMediaList = if (anime) favourites.anime else favourites.manga
            apiMediaList!!.edges!!.forEach {
                val media = Media(it.node!!)
                media.isFav = true
                responseArray.add(media)
            }
        }
        catch (e:Exception){
            toastString(e.toString())
        }
        return responseArray
    }

    fun recommendations(): ArrayList<Media> {
        val response = executeQuery(""" { Page(page: 1, perPage:30) { pageInfo { total currentPage hasNextPage } recommendations(sort: RATING_DESC, onList: true) { rating userRating mediaRecommendation { id idMal isAdult mediaListEntry {progress score(format:POINT_100) status} chapters isFavourite episodes nextAiringEpisode {episode} meanScore isFavourite title {english romaji userPreferred } type status(version: 2) bannerImage coverImage { large } } } } } """)
        val responseArray = arrayListOf<Media>()
        val ids = arrayListOf<Int>()
        if (response?.get("data")!=null && response["data"] != JsonNull) {
            val page: Page = mapper.readValue(response["data"]!!.jsonObject["Page"]!!.toString())
            page.apply {
                recommendations?.apply {
                    this.reversed().forEach{
                        val json = it.mediaRecommendation
                        if (json!=null && json.id !in ids) {
                            ids.add(json.id)
                            responseArray.add(Media(json))
                        }
                    }
                }
            }
        }

        return responseArray
    }
    private fun bannerImage(type: String): String? {
        var image = loadData<BannerImage>("banner_$type")
        if(image==null || image.checkTime()){
            val response = executeQuery("""{ MediaListCollection(userId: ${Anilist.userid}, type: $type, chunk:1,perChunk:25, sort: [SCORE_DESC,UPDATED_TIME_DESC]) { lists { entries{ media { bannerImage } } } } } """)
            val data = if (response!=null) response["data"] else null
            if(data!=null && data!=JsonNull) {
                val mediaListCollection: MediaListCollection = mapper.readValue(data.jsonObject["MediaListCollection"]!!.toString())
                val allImages = arrayListOf<String>()
                mediaListCollection.lists?.forEach {
                    it.entries?.forEach { entry ->
                        val imageUrl = entry.media?.bannerImage
                        if(imageUrl!=null && imageUrl!="null") allImages.add(imageUrl)
                    }
                }

                if (allImages.isNotEmpty()) {
                    val rand = Random.nextInt(0, allImages.size)
                    image = BannerImage(
                        allImages[rand],
                        System.currentTimeMillis()
                    )
                    saveData("banner_$type", image)
                    return image.url
                }
            }
        }else{
            return image.url
        }
        return null
    }

    fun getBannerImages(): ArrayList<String?> {
        val default = arrayListOf<String?>(null,null)
        default[0]=bannerImage("ANIME")
        default[1]=bannerImage("MANGA")
        return default
    }

    fun getMediaLists(anime:Boolean,userId:Int): MutableMap<String,ArrayList<Media>> {
        val response = executeQuery("""{ MediaListCollection(userId: $userId, type: ${if(anime) "ANIME" else "MANGA"}) { lists { name entries { status progress score(format:POINT_100) media { id idMal isAdult status chapters episodes nextAiringEpisode {episode} bannerImage meanScore isFavourite coverImage{large} title {english romaji userPreferred } } } } user { mediaListOptions { rowOrder animeList { sectionOrder } mangaList { sectionOrder } } } } }""")
        val sorted = mutableMapOf<String,ArrayList<Media>>()
        val unsorted = mutableMapOf<String,ArrayList<Media>>()
        val all = arrayListOf<Media>()
        val allIds = arrayListOf<Int>()
        val collection = response?.get("data")?.jsonObject?.get("MediaListCollection")
        if(collection == JsonNull) return unsorted

        val mediaListCollection: MediaListCollection = mapper.readValue(collection.toString())
        mediaListCollection.lists?.forEach { i ->
            val name = i.name.toString().trim('"')
            unsorted[name] = arrayListOf()
            i.entries!!.forEach {
                val a = Media(it.media!!)
                unsorted[name]!!.add(a)
                if(!allIds.contains(a.id)) {
                    allIds.add(a.id)
                    all.add(a)
                }
            }
        }

        val options = mediaListCollection.user!!.mediaListOptions!!
        val mediaList = if(anime) options.animeList else options.mangaList
        mediaList!!.sectionOrder!!.forEach {
            if(unsorted.containsKey(it)) sorted[it] = unsorted[it]!!
        }
        val favResponse = executeQuery("""{User(id:$userId){favourites{${if(anime) "anime" else "manga"}(page:0){edges{favouriteOrder node{id idMal isAdult mediaListEntry{progress score(format:POINT_100)status}chapters isFavourite episodes nextAiringEpisode{episode}meanScore isFavourite title{english romaji userPreferred}type status(version:2)bannerImage coverImage{large}}}}}}}""")
        sorted["Favourites"] = arrayListOf()
        favResponse?.jsonObject?.get("data").apply {
            if(this!=null && this!=JsonNull) {
                val user: User = mapper.readValue(this.jsonObject["User"]!!.toString())
                val favourites = user.favourites!!
                val apiMediaList = if (anime) favourites.anime else favourites.manga
                apiMediaList!!.edges!!.forEach {
                    val media = Media(it.node!!)
                    media.isFav = true
                    sorted["Favourites"]!!.add(media)
            }
            }
        }
        sorted["Favourites"]!!.sortWith(compareBy { it.userFavOrder })

        sorted["All"] = all

        val sort = options.rowOrder!!
        for(i in sorted.keys) {
            when(sort) {
                "score" -> sorted[i]!!.sortWith { b, a -> compareValuesBy(a, b, { it.userScore }, { it.meanScore }) }
                "title" -> sorted[i]!!.sortWith(compareBy { it.userPreferredName })
                "updatedAt" -> sorted[i]!!.sortWith(compareBy { it.userUpdatedAt })
                "id" -> sorted[i]!!.sortWith(compareBy { it.id })
            }
        }
        return sorted
    }


    fun getGenresAndTags(activity: Activity):Boolean{
        var genres:ArrayList<String>? = loadData("genres_list",activity)
        var tags:ArrayList<String>? = loadData("tags_list",activity)

        if (genres==null) {
            executeQuery("""{GenreCollection}""", force = true, useToken = false)?.get("data")?.apply {
//                toastString(this.toString())
                if(this!=JsonNull){
                    genres = arrayListOf()
                    this.jsonObject["GenreCollection"]?.apply {
                        if(this!=JsonNull) jsonArray.forEach { genre ->
                            genres!!.add(genre.toString().trim('"'))
                        }
                    }
                    saveData("genres_list", genres!!)
                }
            }
        }
        if (tags==null){
            executeQuery("""{ MediaTagCollection { name isAdult } }""", force = true)?.get("data")?.apply {
                if(this!=JsonNull){
                    tags = arrayListOf()
                    this.jsonObject["MediaTagCollection"]?.apply {
                        if(this!=JsonNull) jsonArray.forEach{ node ->
                            val mediaTag: MediaTag = mapper.readValue(node.toString())
                            if(mediaTag.isAdult == true)
                                tags!!.add(mediaTag.name)
                        }
                    }
                    saveData("tags_list",tags!!)
                }
            }
        }
        return if(genres!=null && tags!=null) {
            Anilist.genres = genres
            Anilist.tags = tags
            true
        } else false
    }

    fun getGenres(genres: ArrayList<String>,listener: ((Pair<String,String>)->Unit)){
        genres.forEach {
            getGenreThumbnail(it).apply {
                if(this!=null) {
                    listener.invoke(it to this.thumbnail)
                }
            }
        }
    }

    private fun getGenreThumbnail(genre:String):Genre?{
        val genres = loadData<MutableMap<String,Genre>>("genre_thumb")?: mutableMapOf()
        if(genres.checkGenreTime(genre)){
            try {
                val genreQuery = """{ Page(perPage: 10){media(genre:"$genre", sort: TRENDING_DESC, type: ANIME, countryOfOrigin:"JP") {id bannerImage } } }"""
                val response = executeQuery(genreQuery, force = true)!!["data"]!!.jsonObject["Page"]!!
                if (response.jsonObject["media"] != JsonNull) {
                    response.jsonObject["media"]!!.jsonArray.forEach {
                        val media: ApiMedia = mapper.readValue(it.toString())
                        if (genres.checkId(media.id) && media.bannerImage != null) {
                            genres[genre] = Genre(
                                genre,
                                media.id,
                                media.bannerImage!!,
                                System.currentTimeMillis()
                            )
                            saveData("genre_thumb",genres)
                            return genres[genre]
                        }
                    }
                }
            } catch (e: Exception) {
                toastString(e.toString())
            }
        }else{
            return genres[genre]!!
        }
        return null
    }

    fun search(
        type: String,
        page: Int? = null,
        perPage:Int?=null,
        search: String? = null,
        sort: String? = null,
        genres: ArrayList<String>? = null,
        tags: ArrayList<String>? = null,
        format:String?=null,
        isAdult:Boolean=false,
        onList: Boolean?=null,
        id: Int?=null,
        hd:Boolean=false
    ): SearchResults? {
        val query = """
query (${"$"}page: Int = 1, ${"$"}id: Int, ${"$"}type: MediaType, ${"$"}isAdult: Boolean = false, ${"$"}search: String, ${"$"}format: [MediaFormat], ${"$"}status: MediaStatus, ${"$"}countryOfOrigin: CountryCode, ${"$"}source: MediaSource, ${"$"}season: MediaSeason, ${"$"}seasonYear: Int, ${"$"}year: String, ${"$"}onList: Boolean, ${"$"}yearLesser: FuzzyDateInt, ${"$"}yearGreater: FuzzyDateInt, ${"$"}episodeLesser: Int, ${"$"}episodeGreater: Int, ${"$"}durationLesser: Int, ${"$"}durationGreater: Int, ${"$"}chapterLesser: Int, ${"$"}chapterGreater: Int, ${"$"}volumeLesser: Int, ${"$"}volumeGreater: Int, ${"$"}licensedBy: [String], ${"$"}isLicensed: Boolean, ${"$"}genres: [String], ${"$"}excludedGenres: [String], ${"$"}tags: [String], ${"$"}excludedTags: [String], ${"$"}minimumTagRank: Int, ${"$"}sort: [MediaSort] = [POPULARITY_DESC, SCORE_DESC]) {
  Page(page: ${"$"}page, perPage: ${perPage?:50}) {
    pageInfo {
      total
      perPage
      currentPage
      lastPage
      hasNextPage
    }
    media(id: ${"$"}id, type: ${"$"}type, season: ${"$"}season, format_in: ${"$"}format, status: ${"$"}status, countryOfOrigin: ${"$"}countryOfOrigin, source: ${"$"}source, search: ${"$"}search, onList: ${"$"}onList, seasonYear: ${"$"}seasonYear, startDate_like: ${"$"}year, startDate_lesser: ${"$"}yearLesser, startDate_greater: ${"$"}yearGreater, episodes_lesser: ${"$"}episodeLesser, episodes_greater: ${"$"}episodeGreater, duration_lesser: ${"$"}durationLesser, duration_greater: ${"$"}durationGreater, chapters_lesser: ${"$"}chapterLesser, chapters_greater: ${"$"}chapterGreater, volumes_lesser: ${"$"}volumeLesser, volumes_greater: ${"$"}volumeGreater, licensedBy_in: ${"$"}licensedBy, isLicensed: ${"$"}isLicensed, genre_in: ${"$"}genres, genre_not_in: ${"$"}excludedGenres, tag_in: ${"$"}tags, tag_not_in: ${"$"}excludedTags, minimumTagRank: ${"$"}minimumTagRank, sort: ${"$"}sort, isAdult: ${"$"}isAdult) {
      id
      idMal
      isAdult
      status
      chapters
      episodes
      nextAiringEpisode {
        episode
      }
      type
      genres
      meanScore
      isFavourite
      bannerImage
      coverImage {
        large
        extraLarge
      }
      title {
        english
        romaji
        userPreferred
      }
      mediaListEntry {
        progress
        score(format: POINT_100)
        status
      }
    }
  }
}
        """.replace("\n", " ").replace("""  """, "")
        val variables = """{"type":"$type","isAdult":$isAdult
            ${if (onList != null) ""","onList":$onList""" else ""}
            ${if (page != null) ""","page":"$page"""" else ""}
            ${if (id != null) ""","id":"$id"""" else ""}
            ${if (search != null) ""","search":"$search"""" else ""}
            ${if (Anilist.sortBy.containsKey(sort)) ""","sort":"${Anilist.sortBy[sort]}"""" else ""}
            ${if (format != null) ""","format":"$format"""" else ""}
            ${if (genres?.isNotEmpty() == true) ""","genres":"${genres[0]}"""" else ""}
            ${if (tags?.isNotEmpty() == true) ""","tags":"${tags[0]}"""" else ""}
            }""".replace("\n", " ").replace("""  """, "")
        val response = executeQuery(query, variables, true)
        if(response!=null) {
            val a = if(response["data"]!=JsonNull) response["data"] else null
            val pag = a?.jsonObject?.get("Page") ?:return null

            val responseArray = arrayListOf<Media>()
            if(pag != JsonNull) {
                val fetchedPage: Page = mapper.readValue(pag.toString())

                fetchedPage.media?.forEach { i ->
                    val userStatus = i.mediaListEntry?.status.toString()
                    val genresArr = arrayListOf<String>()
                    if (i.genres != null) {
                        i.genres!!.forEach { genre ->
                            genresArr.add(genre)
                        }
                    }
                    val media = Media(i)
                    media.relation = if(onList==true) userStatus else null
                    media.genres = genresArr
                    responseArray.add(media)
                }

                return SearchResults(
                    type = type,
                    perPage = perPage,
                    search = search,
                    sort = sort,
                    isAdult = isAdult,
                    onList = onList,
                    genres = genres,
                    tags = tags,
                    format = format,
                    results = responseArray,
                    page = fetchedPage.pageInfo!!.currentPage.toString().toInt(),
                    hasNextPage = fetchedPage.pageInfo!!.hasNextPage == true,
                )
            }
        } else{
            toastString("Empty Response, Does your internet perhaps suck?")
        }
        return null
    }

    fun recentlyUpdated(): ArrayList<Media>? {
        val query="""{
Page(page:1,perPage:50) {
    pageInfo {
        hasNextPage
        total
    }
    airingSchedules(
        airingAt_greater: 0
        airingAt_lesser: ${System.currentTimeMillis()/1000-10000}
        sort:TIME_DESC
    ) {
        media {
            id
            idMal
            status
            chapters
            episodes
            nextAiringEpisode { episode }
            isAdult
            type
            meanScore
            isFavourite
            bannerImage
            countryOfOrigin
            coverImage { large }
            title {
                english
                romaji
                userPreferred
            }
            mediaListEntry {
                progress
                score(format: POINT_100)
                status
            }
        }
    }
}
        }""".replace("\n", " ").replace("""  """, "")
        val response = executeQuery(query, force = true)?:return null
        val pag = response["data"]?.jsonObject?.get("Page") ?: return null
        val page: Page = mapper.readValue(pag.toString())
        if (page.airingSchedules == null) return null

        val responseArray = arrayListOf<Media>()
        val idArr = arrayListOf<Int>()
        fun addMedia(listOnly:Boolean){
            page.airingSchedules!!.forEach {
                val i = it.media!!
                val id = i.id
                if(!idArr.contains(id)) if (!listOnly && (i.countryOfOrigin =="JP" && (if(!Anilist.adult) i.isAdult == false else true)) || (listOnly && i.mediaListEntry!=null)) {
                    idArr.add(id)
                    responseArray.add(Media(i))
                }
            }
        }
        addMedia(loadData("recently_list_only")?:false)
//        if(responseArray.isEmpty()) addMedia(false)
        return responseArray
    }

    fun getCharacterDetails(character: Character):Character{
        val query=""" {
  Character(id: ${character.id}) {
    id
    age
    gender
    description
    dateOfBirth {
      year
      month
      day
    }
    media(page: 0,sort:[POPULARITY_DESC,SCORE_DESC]) {
      pageInfo {
        total
        perPage
        currentPage
        lastPage
        hasNextPage
      }
      edges {
        id
        characterRole
        node {
          id
          idMal
          isAdult
          status
          chapters
          episodes
          nextAiringEpisode { episode }
          type
          meanScore
          isFavourite
          bannerImage
          countryOfOrigin
          coverImage { large }
          title {
              english
              romaji
              userPreferred
          }
          mediaListEntry {
              progress
              score(format: POINT_100)
              status
          }
        }
      }
    }
  }
}""".replace("\n", " ").replace("""  """, "")
        val response = executeQuery(query, force = true)?:return character
        val char = response["data"]?.jsonObject?.get("Character")?:return character
        val fetchedCharacter: ApiCharacter = mapper.readValue(char.toString())
        character.age = fetchedCharacter.age
        character.gender = fetchedCharacter.gender
        character.description = fetchedCharacter.description
        character.dateOfBirth = FuzzyDate(
            fetchedCharacter.dateOfBirth!!.year,
            fetchedCharacter.dateOfBirth!!.month,
            fetchedCharacter.dateOfBirth!!.day
        )
        character.roles = arrayListOf()
        fetchedCharacter.media?.edges?.forEach { i->
            val m = Media(i)
            m.relation = i.characterRole.toString()
            character.roles!!.add(m)
        }
        return character
    }

    fun getStudioDetails(studio: Studio): Studio {
        fun query(page:Int=0) =""" {
  Studio(id: ${studio.id}) {
    media(page: $page,sort:START_DATE_DESC) {
      pageInfo{
        hasNextPage
      }
      edges {
        id
        node {
          id
          idMal
          isAdult
          status
          chapters
          episodes
          nextAiringEpisode { episode }
          type
          meanScore
          startDate{ year }
          isFavourite
          bannerImage
          countryOfOrigin
          coverImage { large }
          title {
              english
              romaji
              userPreferred
          }
          mediaListEntry {
              progress
              score(format: POINT_100)
              status
          }
        }
      }
    }
  }
}""".replace("\n", " ").replace("""  """, "")
        var hasNextPage=true
        studio.yearMedia = mutableMapOf()
        var page = 0
        while(hasNextPage){
            page++
            val response = executeQuery(query(page), force = true)?:return studio
            val data = response["data"]?.jsonObject?.get("Studio") ?:return studio
            val fetchedStudio: ApiStudio = mapper.readValue(data.toString())
            if (fetchedStudio.media == null) return studio
            val mediaConnection = fetchedStudio.media

            hasNextPage = mediaConnection?.pageInfo?.hasNextPage == true
            mediaConnection?.edges?.forEach { i->
                val node = i.node!!
                val status = node.status.toString().replace("_"," ")
                val year = node.startDate!!.year?.toString() ?: "TBA"
                val title = if(status!="CANCELLED") year else status
                if(!studio.yearMedia!!.containsKey(title))
                    studio.yearMedia!![title] = arrayListOf()
                studio.yearMedia!![title]!!.add(Media(node))
            }
        }

        if(studio.yearMedia!!.contains("CANCELLED")){
            val a =  studio.yearMedia!!["CANCELLED"]!!
            studio.yearMedia!!.remove("CANCELLED")
            studio.yearMedia!!["CANCELLED"] = a
        }
        return studio
    }

}

