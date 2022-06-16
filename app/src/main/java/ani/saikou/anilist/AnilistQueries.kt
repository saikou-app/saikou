package ani.saikou.anilist

import android.app.Activity
import ani.saikou.*
import ani.saikou.anilist.api.FuzzyDate
import ani.saikou.anilist.api.Query
import ani.saikou.anilist.api.User
import ani.saikou.media.Character
import ani.saikou.media.Media
import ani.saikou.media.Studio
import ani.saikou.others.Mal
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import java.io.Serializable
import kotlin.system.measureTimeMillis

suspend inline fun <reified T : Any> executeQuery(
    query: String,
    variables: String = "",
    force: Boolean = false,
    useToken: Boolean = true,
    show: Boolean = false,
    cache: Int? = null
): T? {
    return tryWithSuspend {
        val data = mapOf(
            "query" to query,
            "variables" to variables
        )
        val headers = mutableMapOf(
            "Content-Type" to "application/json",
            "Accept" to "application/json"
        )

        if (Anilist.token != null || force) {
            if (Anilist.token != null && useToken) headers["Authorization"] = "Bearer ${Anilist.token}"

            val json = client.post("https://graphql.anilist.co/", headers, data = data, cacheTime = cache ?: 10)
            if(!json.text.startsWith("{")) throw Exception("Seems like Anilist is down, maybe try using a VPN or you can wait for it to comeback.")
            if (show) toastString("Response : ${json.text}")
            json.parsed()
        } else null
    }
}

data class SearchResults(
    val type: String,
    var isAdult: Boolean,
    var onList: Boolean? = null,
    var perPage: Int? = null,
    var search: String? = null,
    var sort: String? = null,
    var genres: ArrayList<String>? = null,
    var tags: ArrayList<String>? = null,
    var format: String? = null,
    var page: Int = 1,
    var results: ArrayList<Media>,
    var hasNextPage: Boolean,
) : Serializable

class AnilistQueries {
    suspend fun getUserData(): Boolean {
        val response: Query.Viewer?
        measureTimeMillis {
            response =
                executeQuery("""{Viewer {name options{ displayAdultContent } avatar{medium} bannerImage id statistics{anime{episodesWatched}manga{chaptersRead}}}}""")
        }.also { println("time : $it") }
        val user = response?.data?.user ?: return false

        Anilist.userid = user.id
        Anilist.username = user.name
        Anilist.bg = user.bannerImage
        Anilist.avatar = user.avatar?.medium
        Anilist.episodesWatched = user.statistics?.anime?.episodesWatched
        Anilist.chapterRead = user.statistics?.manga?.chaptersRead
        Anilist.adult = user.options?.displayAdultContent ?: false
        return true
    }

    suspend fun getMedia(id: Int, mal: Boolean = false): Media? {
        val response = executeQuery<Query.Media>(
            """{Media(${if (!mal) "id:" else "idMal:"}$id){id idMal status chapters episodes nextAiringEpisode{episode}type meanScore isAdult isFavourite bannerImage coverImage{large}title{english romaji userPreferred}mediaListEntry{progress score(format:POINT_100)status}}}""",
            force = true
        )
        val fetchedMedia = response?.data?.media ?: return null
        return Media(fetchedMedia)
    }

    fun mediaDetails(media: Media): Media {
        media.cameFromContinue = false
        val query =
            """{Media(id:${media.id}){mediaListEntry{id status score(format:POINT_100) progress repeat updatedAt startedAt{year month day}completedAt{year month day}}isFavourite siteUrl idMal nextAiringEpisode{episode airingAt}source countryOfOrigin format duration season seasonYear startDate{year month day}endDate{year month day}genres studios(isMain:true){nodes{id name siteUrl}}description trailer { site id } synonyms tags { name rank isMediaSpoiler } characters(sort:[ROLE,FAVOURITES_DESC],perPage:25,page:1){edges{role node{id image{medium}name{userPreferred}}}}relations{edges{relationType(version:2)node{id idMal mediaListEntry{progress score(format:POINT_100) status} episodes chapters nextAiringEpisode{episode} popularity meanScore isAdult isFavourite title{english romaji userPreferred}type status(version:2)bannerImage coverImage{large}}}}recommendations(sort:RATING_DESC){nodes{mediaRecommendation{id idMal mediaListEntry{progress score(format:POINT_100) status} episodes chapters nextAiringEpisode{episode}meanScore isAdult isFavourite title{english romaji userPreferred}type status(version:2)bannerImage coverImage{large}}}}externalLinks{url site}}}"""
        runBlocking {
            val anilist = async {
                var response = executeQuery<Query.Media>(query, force = true)
                if (response != null) {
                    fun parse() {
                        val fetchedMedia = response?.data?.media ?: return

                        media.source = fetchedMedia.source.toString()
                        media.countryOfOrigin = fetchedMedia.countryOfOrigin
                        media.format = fetchedMedia.format.toString()

                        if (fetchedMedia.genres != null) {
                            media.genres = arrayListOf()
                            fetchedMedia.genres?.forEach { i ->
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
                                if (i.isMediaSpoiler == false)
                                    media.tags.add("${i.name} : ${i.rank.toString()}%")
                            }
                        }

                        media.description = fetchedMedia.description.toString()

                        if (fetchedMedia.characters != null) {
                            media.characters = arrayListOf()
                            fetchedMedia.characters?.edges?.forEach { i ->
                                i.node?.apply {
                                    media.characters?.add(
                                        Character(
                                            id = id,
                                            name = i.node?.name?.userPreferred,
                                            image = i.node?.image?.medium,
                                            banner = media.banner ?: media.cover,
                                            role = i.role.toString()
                                        )
                                    )
                                }
                            }
                        }
                        if (fetchedMedia.relations != null) {
                            media.relations = arrayListOf()
                            fetchedMedia.relations?.edges?.forEach { mediaEdge ->
                                val m = Media(mediaEdge)
                                media.relations?.add(m)
                                if (m.relation == "SEQUEL") {
                                    media.sequel = if ((media.sequel?.popularity ?: 0) < (m.popularity ?: 0)) m else media.sequel

                                } else if (m.relation == "PREQUEL") {
                                    media.prequel =
                                        if ((media.prequel?.popularity ?: 0) < (m.popularity ?: 0)) m else media.prequel
                                }
                            }
                            media.relations?.sortByDescending { it.popularity }
                            media.relations?.sortByDescending { it.startDate?.year }
                            media.relations?.sortBy { it.relation }
                        }
                        if (fetchedMedia.recommendations != null) {
                            media.recommendations = arrayListOf()
                            fetchedMedia.recommendations?.nodes?.forEach { i ->
                                i.mediaRecommendation?.apply {
                                    media.recommendations?.add(
                                        Media(this)
                                    )
                                }
                            }
                        }

                        if (fetchedMedia.mediaListEntry != null) {
                            fetchedMedia.mediaListEntry?.apply {
                                media.userProgress = progress
                                media.userListId = id
                                media.userScore = score?.toInt() ?: 0
                                media.userStatus = status?.toString()
                                media.userRepeat = repeat ?: 0
                                media.userUpdatedAt = updatedAt?.toString()?.toLong()?.times(1000)
                                media.userCompletedAt = completedAt ?: FuzzyDate()
                                media.userStartedAt = startedAt ?: FuzzyDate()
                            }
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
                            media.startDate = fetchedMedia.startDate
                            media.endDate = fetchedMedia.endDate

                            fetchedMedia.studios?.nodes?.apply {
                                if (isNotEmpty()) {
                                    val firstStudio = get(0)
                                    media.anime.mainStudio = Studio(
                                        firstStudio.id.toString(),
                                        firstStudio.name ?: "N/A"
                                    )
                                }
                            }

                            media.anime.nextAiringEpisodeTime = fetchedMedia.nextAiringEpisode?.airingAt?.toLong()

                            fetchedMedia.externalLinks?.forEach { i ->
                                when (i.site) {
                                    "Youtube" -> media.anime.youtube = i.url
                                    "Crunchyroll" -> media.crunchySlug = i.url?.split("/")?.getOrNull(3)
                                    "VRV" -> media.vrvId = i.url?.split("/")?.getOrNull(4)
                                }
                            }
                        } else if (media.manga != null) {
                            logger("Nothing Here lmao", false)
                        }
                        media.shareLink = fetchedMedia.siteUrl
                    }

                    if (response.data?.media != null) parse()
                    else {
                        toastString("Adult Stuff? ( ͡° ͜ʖ ͡°)")
                        response = executeQuery(query, force = true, useToken = false)
                        if (response?.data?.media != null) parse()
                        else toastString("What did you even open?")
                    }
                } else {
                    toastString("Error getting Data from Anilist.")
                }
            }
            val mal = async {
                if (media.idMAL != null) {
                    Mal.loadMedia(media)
                }
            }
            awaitAll(anilist, mal)
        }
        return media
    }

    suspend fun continueMedia(type: String): ArrayList<Media> {
        val returnArray = arrayListOf<Media>()
        val map = mutableMapOf<Int, Media>()
        val statuses = arrayOf("CURRENT", "REPEATING")
        suspend fun repeat(status: String) {
            val response =
                executeQuery<Query.MediaListCollection>(""" { MediaListCollection(userId: ${Anilist.userid}, type: $type, status: $status , sort: UPDATED_TIME ) { lists { entries { progress score(format:POINT_100) status media { id idMal type isAdult status chapters episodes nextAiringEpisode {episode} meanScore isFavourite bannerImage coverImage{large} title { english romaji userPreferred } } } } } } """)
            response?.data?.mediaListCollection?.lists?.forEach { li ->
                li.entries?.reversed()?.forEach {
                    val m = Media(it)
                    m.cameFromContinue = true
                    map[m.id] = m
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

    suspend fun favMedia(anime: Boolean): ArrayList<Media> {
        val responseArray = arrayListOf<Media>()
        try {
            val response =
                executeQuery<Query.User>("""{User(id:${Anilist.userid}){favourites{${if (anime) "anime" else "manga"}(page:0){edges{favouriteOrder node{id idMal isAdult mediaListEntry{progress score(format:POINT_100)status}chapters isFavourite episodes nextAiringEpisode{episode}meanScore isFavourite title{english romaji userPreferred}type status(version:2)bannerImage coverImage{large}}}}}}}""")
            val user: User = response?.data?.user ?: return responseArray
            val favourites = user.favourites
            val apiMediaList = if (anime) favourites?.anime else favourites?.manga
            apiMediaList?.edges?.forEach {
                it.node?.apply {
                    val media = Media(this)
                    media.isFav = true
                    responseArray.add(media)
                }
            }
        } catch (e: Exception) {
            logError(e)
        }
        return responseArray
    }

    suspend fun recommendations(): ArrayList<Media> {
        val response =
            executeQuery<Query.Page>(""" { Page(page: 1, perPage:30) { pageInfo { total currentPage hasNextPage } recommendations(sort: RATING_DESC, onList: true) { rating userRating mediaRecommendation { id idMal isAdult mediaListEntry {progress score(format:POINT_100) status} chapters isFavourite episodes nextAiringEpisode {episode} popularity meanScore isFavourite title {english romaji userPreferred } type status(version: 2) bannerImage coverImage { large } } } } } """)
        val map = mutableMapOf<Int, Media>()
        response?.data?.page?.apply {
            recommendations?.onEach {
                val json = it.mediaRecommendation
                if (json != null) {
                    val m = Media(json)
                    m.relation = json.type?.toString()
                    map[m.id] = m
                }
            }
        }

        val types = arrayOf("ANIME", "MANGA")
        suspend fun repeat(type: String) {
            val res =
                executeQuery<Query.MediaListCollection>(""" { MediaListCollection(userId: ${Anilist.userid}, type: $type, status: PLANNING , sort: MEDIA_POPULARITY_DESC ) { lists { entries { media { id mediaListEntry {progress score(format:POINT_100) status} idMal type isAdult popularity status(version: 2) chapters episodes nextAiringEpisode {episode} meanScore isFavourite bannerImage coverImage{large} title { english romaji userPreferred } } } } } } """)
            res?.data?.mediaListCollection?.lists?.forEach { li ->
                li.entries?.forEach {
                    val m = Media(it)
                    if (m.status == "RELEASING" || m.status == "FINISHED") {
                        m.relation = it.media?.type?.toString()
                        map[m.id] = m
                    }
                }
            }
        }
        types.forEach { repeat(it) }

        val list = ArrayList(map.values.toList())
        list.sortByDescending { it.meanScore }
        return list
    }

    private suspend fun bannerImage(type: String): String? {
        var image = loadData<BannerImage>("banner_$type")
        if (image == null || image.checkTime()) {
            val response =
                executeQuery<Query.MediaListCollection>("""{ MediaListCollection(userId: ${Anilist.userid}, type: $type, chunk:1,perChunk:25, sort: [SCORE_DESC,UPDATED_TIME_DESC]) { lists { entries{ media { bannerImage } } } } } """)
            val random = response?.data?.mediaListCollection?.lists?.mapNotNull {
                it.entries?.mapNotNull { entry ->
                    val imageUrl = entry.media?.bannerImage
                    if (imageUrl != null && imageUrl != "null") imageUrl
                    else null
                }
            }?.flatten()?.randomOrNull() ?: return null

            image = BannerImage(
                random,
                System.currentTimeMillis()
            )
            saveData("banner_$type", image)
            return image.url
        } else return image.url
    }

    suspend fun getBannerImages(): ArrayList<String?> {
        val default = arrayListOf<String?>(null, null)
        default[0] = bannerImage("ANIME")
        default[1] = bannerImage("MANGA")
        return default
    }

    suspend fun getMediaLists(anime: Boolean, userId: Int, sortOrder: String? = null): MutableMap<String, ArrayList<Media>> {
        val response =
            executeQuery<Query.MediaListCollection>("""{ MediaListCollection(userId: $userId, type: ${if (anime) "ANIME" else "MANGA"}) { lists { name entries { status progress score(format:POINT_100) updatedAt media { id idMal isAdult type status chapters episodes nextAiringEpisode {episode} bannerImage meanScore isFavourite coverImage{large} title {english romaji userPreferred } } } } user { mediaListOptions { rowOrder animeList { sectionOrder } mangaList { sectionOrder } } } } }""")
        val sorted = mutableMapOf<String, ArrayList<Media>>()
        val unsorted = mutableMapOf<String, ArrayList<Media>>()
        val all = arrayListOf<Media>()
        val allIds = arrayListOf<Int>()

        response?.data?.mediaListCollection?.lists?.forEach { i ->
            val name = i.name.toString().trim('"')
            unsorted[name] = arrayListOf()
            i.entries?.forEach {
                val a = Media(it)
                unsorted[name]?.add(a)
                if (!allIds.contains(a.id)) {
                    allIds.add(a.id)
                    all.add(a)
                }
            }
        }

        val options = response?.data?.mediaListCollection?.user?.mediaListOptions
        val mediaList = if (anime) options?.animeList else options?.mangaList
        mediaList?.sectionOrder?.forEach {
            if (unsorted.containsKey(it)) sorted[it] = unsorted[it]!!
        }
        val favResponse =
            executeQuery<Query.User>("""{User(id:$userId){favourites{${if (anime) "anime" else "manga"}(page:0){edges{favouriteOrder node{id idMal isAdult mediaListEntry{progress score(format:POINT_100)status}chapters isFavourite episodes nextAiringEpisode{episode}meanScore isFavourite title{english romaji userPreferred}type status(version:2)bannerImage coverImage{large}}}}}}}""")


        favResponse?.data?.user?.apply {
            sorted["Favourites"] = arrayListOf()
            val apiMediaList = if (anime) favourites?.anime else favourites?.manga
            apiMediaList?.edges?.forEach {
                it.node?.apply {
                    val media = Media(this)
                    media.isFav = true
                    sorted["Favourites"]?.add(media)
                }
            }
        }

        sorted["Favourites"]?.sortWith(compareBy { it.userFavOrder })

        sorted["All"] = all

        val sort = sortOrder ?: options?.rowOrder
        for (i in sorted.keys) {
            when (sort) {
                "score"     -> sorted[i]?.sortWith { b, a -> compareValuesBy(a, b, { it.userScore }, { it.meanScore }) }
                "title"     -> sorted[i]?.sortWith(compareBy { it.userPreferredName })
                "updatedAt" -> sorted[i]?.sortWith(compareByDescending { it.userUpdatedAt })
                "id"        -> sorted[i]?.sortWith(compareBy { it.id })
            }
        }
        return sorted
    }


    suspend fun getGenresAndTags(activity: Activity): Boolean {
        var genres: ArrayList<String>? = loadData("genres_list", activity)
        var tags: ArrayList<String>? = loadData("tags_list", activity)

        if (genres == null) {
            executeQuery<Query.GenreCollection>(
                """{GenreCollection}""",
                force = true,
                useToken = false
            )?.data?.genreCollection?.apply {
                genres = arrayListOf()
                forEach {
                    genres?.add(it)
                }
                saveData("genres_list", genres!!)
            }
        }
        if (tags == null) {
            executeQuery<Query.MediaTagCollection>(
                """{ MediaTagCollection { name isAdult } }""",
                force = true
            )?.data?.mediaTagCollection?.apply {
                tags = arrayListOf()
                forEach { node ->
                    if (node.isAdult == true) tags?.add(node.name)
                }
                saveData("tags_list", tags)
            }
        }
        return if (genres != null && tags != null) {
            Anilist.genres = genres
            Anilist.tags = tags
            true
        } else false
    }

    suspend fun getGenres(genres: ArrayList<String>, listener: ((Pair<String, String>) -> Unit)) {
        genres.forEach {
            getGenreThumbnail(it).apply {
                if (this != null) {
                    listener.invoke(it to this.thumbnail)
                }
            }
        }
    }

    private suspend fun getGenreThumbnail(genre: String): Genre? {
        val genres = loadData<MutableMap<String, Genre>>("genre_thumb") ?: mutableMapOf()
        if (genres.checkGenreTime(genre)) {
            try {
                val genreQuery =
                    """{ Page(perPage: 10){media(genre:"$genre", sort: TRENDING_DESC, type: ANIME, countryOfOrigin:"JP") {id bannerImage title{english romaji userPreferred} } } }"""
                executeQuery<Query.Page>(genreQuery, force = true)?.data?.page?.media?.forEach {
                    if (genres.checkId(it.id) && it.bannerImage != null) {
                        genres[genre] = Genre(
                            genre,
                            it.id,
                            it.bannerImage!!,
                            System.currentTimeMillis()
                        )
                        saveData("genre_thumb", genres)
                        return genres[genre]
                    }
                }
            } catch (e: Exception) {
                logError(e)
            }
        } else {
            return genres[genre]
        }
        return null
    }

    suspend fun search(
        type: String,
        page: Int? = null,
        perPage: Int? = null,
        search: String? = null,
        sort: String? = null,
        genres: ArrayList<String>? = null,
        tags: ArrayList<String>? = null,
        format: String? = null,
        isAdult: Boolean = false,
        onList: Boolean? = null,
        id: Int? = null,
        hd: Boolean = false
    ): SearchResults? {
        val query = """
query (${"$"}page: Int = 1, ${"$"}id: Int, ${"$"}type: MediaType, ${"$"}isAdult: Boolean = false, ${"$"}search: String, ${"$"}format: [MediaFormat], ${"$"}status: MediaStatus, ${"$"}countryOfOrigin: CountryCode, ${"$"}source: MediaSource, ${"$"}season: MediaSeason, ${"$"}seasonYear: Int, ${"$"}year: String, ${"$"}onList: Boolean, ${"$"}yearLesser: FuzzyDateInt, ${"$"}yearGreater: FuzzyDateInt, ${"$"}episodeLesser: Int, ${"$"}episodeGreater: Int, ${"$"}durationLesser: Int, ${"$"}durationGreater: Int, ${"$"}chapterLesser: Int, ${"$"}chapterGreater: Int, ${"$"}volumeLesser: Int, ${"$"}volumeGreater: Int, ${"$"}licensedBy: [String], ${"$"}isLicensed: Boolean, ${"$"}genres: [String], ${"$"}excludedGenres: [String], ${"$"}tags: [String], ${"$"}excludedTags: [String], ${"$"}minimumTagRank: Int, ${"$"}sort: [MediaSort] = [POPULARITY_DESC, SCORE_DESC]) {
  Page(page: ${"$"}page, perPage: ${perPage ?: 50}) {
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
        val response = executeQuery<Query.Page>(query, variables, true)?.data?.page
        if (response?.media != null) {
            val responseArray = arrayListOf<Media>()
            response.media?.forEach { i ->
                val userStatus = i.mediaListEntry?.status.toString()
                val genresArr = arrayListOf<String>()
                if (i.genres != null) {
                    i.genres?.forEach { genre ->
                        genresArr.add(genre)
                    }
                }
                val media = Media(i)
                if (!hd) media.cover = i.coverImage?.large
                media.relation = if (onList == true) userStatus else null
                media.genres = genresArr
                responseArray.add(media)
            }

            val pageInfo = response.pageInfo ?: return null

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
                page = pageInfo.currentPage.toString().toIntOrNull() ?: 0,
                hasNextPage = pageInfo.hasNextPage == true,
            )
        } else toastString("Empty Response, Does your internet perhaps suck?")
        return null
    }

    suspend fun recentlyUpdated(): ArrayList<Media>? {
        val query = """{
Page(page:1,perPage:50) {
    pageInfo {
        hasNextPage
        total
    }
    airingSchedules(
        airingAt_greater: 0
        airingAt_lesser: ${System.currentTimeMillis() / 1000 - 10000}
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
        val response = executeQuery<Query.Page>(query, force = true)?.data?.page?.airingSchedules ?: return null

        val responseArray = arrayListOf<Media>()
        val idArr = arrayListOf<Int>()
        fun addMedia(listOnly: Boolean) {
            response.forEach {
                it.media?.apply {
                    if (!idArr.contains(id)) if (!listOnly && (countryOfOrigin == "JP" && (if (!Anilist.adult) isAdult == false else true)) || (listOnly && mediaListEntry != null)) {
                        idArr.add(id)
                        responseArray.add(Media(this))
                    }
                }
            }
        }
        addMedia(loadData("recently_list_only") ?: false)
        return responseArray
    }

    suspend fun getCharacterDetails(character: Character): Character {
        val query = """ {
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
        executeQuery<Query.Character>(query, force = true)?.data?.character?.apply {
            character.age = age
            character.gender = gender
            character.description = description
            character.dateOfBirth = dateOfBirth
            character.roles = arrayListOf()
            media?.edges?.forEach { i ->
                val m = Media(i)
                m.relation = i.characterRole.toString()
                character.roles?.add(m)
            }
        }
        return character
    }

    suspend fun getStudioDetails(studio: Studio): Studio {
        fun query(page: Int = 0) = """ {
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

        var hasNextPage = true
        val yearMedia = mutableMapOf<String, ArrayList<Media>>()
        var page = 0
        while (hasNextPage) {
            page++
            executeQuery<Query.Studio>(query(page), force = true)?.data?.studio?.media?.apply {
                hasNextPage = pageInfo?.hasNextPage == true
                edges?.forEach { i ->
                    i.node?.apply {
                        val status = status.toString()
                        val year = startDate?.year?.toString() ?: "TBA"
                        val title = if (status != "CANCELLED") year else status
                        if (!yearMedia.containsKey(title))
                            yearMedia[title] = arrayListOf()
                        yearMedia[title]?.add(Media(this))
                    }
                }
            }
        }

        if (yearMedia.contains("CANCELLED")) {
            val a = yearMedia["CANCELLED"]!!
            yearMedia.remove("CANCELLED")
            yearMedia["CANCELLED"] = a
        }
        studio.yearMedia = yearMedia
        return studio
    }

}