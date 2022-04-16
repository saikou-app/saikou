package ani.saikou.anilist

import android.app.Activity
import ani.saikou.*
import ani.saikou.anilist.api.User
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
import ani.saikou.anilist.api.Media as ApiMedia

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

                        media.description = fetchedMedia.description!!

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
                                    firstStudio.name
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
            val response = executeQuery(""" { MediaListCollection(userId: ${Anilist.userid}, type: $type, status: $status , sort: UPDATED_TIME ) { lists { entries { progress score(format:POINT_100) status media { id idMal isAdult status chapters episodes nextAiringEpisode {episode} meanScore isFavourite bannerImage coverImage{large} title { english romaji userPreferred } } } } } } """)
            val data = if(response?.get("data")!=null && response["data"] !=JsonNull) response["data"] else null
            val a = if(data?.jsonObject?.get("MediaListCollection")!=null && data.jsonObject["MediaListCollection"] !=JsonNull) data.jsonObject["MediaListCollection"] else null
            val list = a?.jsonObject?.get("lists")?.jsonArray
            if (list != null && list.isNotEmpty()) {
                list.forEach { li->
                    li.jsonObject["entries"]!!.jsonArray.reversed().forEach {
                        map[it.jsonObject["media"]!!.jsonObject["id"].toString().toInt()] =
                            Media(
                                id = it.jsonObject["media"]!!.jsonObject["id"].toString().toInt(),
                                idMAL = it.jsonObject["media"]!!.jsonObject["idMal"].toString().toIntOrNull(),
                                name = it.jsonObject["media"]!!.jsonObject["title"]!!.jsonObject["english"].toString().trim('"').replace("\\\"", "\""),
                                nameRomaji = it.jsonObject["media"]!!.jsonObject["title"]!!.jsonObject["romaji"].toString().trim('"').replace("\\\"", "\""),
                                userPreferredName = it.jsonObject["media"]!!.jsonObject["title"]!!.jsonObject["userPreferred"].toString().trim('"').replace("\\\"", "\""),
                                cover = it.jsonObject["media"]!!.jsonObject["coverImage"]!!.jsonObject["large"].toString().trim('"'),
                                banner = if (it.jsonObject["media"]!!.jsonObject["bannerImage"] != JsonNull) it.jsonObject["media"]!!.jsonObject["bannerImage"].toString().trim('"') else null,
                                status = it.jsonObject["media"]!!.jsonObject["status"].toString().trim('"').replace("_"," "),
                                meanScore = if (it.jsonObject["media"]!!.jsonObject["meanScore"] != JsonNull) it.jsonObject["media"]!!.jsonObject["meanScore"].toString().toInt() else null,
                                isFav = it.jsonObject["media"]!!.jsonObject["isFavourite"].toString() == "true",
                                isAdult = it.jsonObject["media"]!!.jsonObject["isAdult"].toString() == "true",
                                userProgress = it.jsonObject["progress"].toString().toInt(),
                                userScore = it.jsonObject["score"].toString().toInt(),
                                userStatus = it.jsonObject["status"].toString().trim('"'),
                                cameFromContinue = true,
                                anime = if (type == "ANIME") Anime(
                                    totalEpisodes = if (it.jsonObject["media"]!!.jsonObject["episodes"] != JsonNull) it.jsonObject["media"]!!.jsonObject["episodes"].toString()
                                        .toInt() else null,
                                    nextAiringEpisode = if (it.jsonObject["media"]!!.jsonObject["nextAiringEpisode"] != JsonNull)
                                        it.jsonObject["media"]!!.jsonObject["nextAiringEpisode"]!!.jsonObject["episode"].toString().toInt() - 1 else null
                                ) else null,
                                manga = if (type == "MANGA") Manga(
                                    totalChapters = if (it.jsonObject["media"]!!.jsonObject["chapters"] != JsonNull) it.jsonObject["media"]!!.jsonObject["chapters"].toString()
                                        .toInt() else null
                                ) else null,
                            )
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
            val favResponse = executeQuery("""{User(id:${Anilist.userid}){favourites{${if(anime) "anime" else "manga"}(page:0){edges{favouriteOrder node{id idMal isAdult mediaListEntry{progress score(format:POINT_100)status}chapters isFavourite episodes nextAiringEpisode{episode}meanScore isFavourite title{english romaji userPreferred}type status(version:2)bannerImage coverImage{large}}}}}}}""")
            favResponse?.jsonObject?.get("data").apply {
                if(this!=null && this!=JsonNull) this.jsonObject["User"]!!.jsonObject["favourites"]!!.jsonObject[if(anime) "anime" else "manga"]!!.jsonObject["edges"]!!.jsonArray.forEach {
                    val json = it.jsonObject["node"]!!
                    responseArray.add(
                        Media(
                            id = json.jsonObject["id"].toString().toInt(),
                            idMAL = json.jsonObject["idMal"].toString().toIntOrNull(),
                            name = json.jsonObject["title"]!!.jsonObject["english"].toString().trim('"').replace("\\\"","\""),
                            nameRomaji = json.jsonObject["title"]!!.jsonObject["romaji"].toString().trim('"').replace("\\\"","\""),
                            userPreferredName = json.jsonObject["title"]!!.jsonObject["userPreferred"].toString().trim('"').replace("\\\"","\""),
                            status = json.jsonObject["status"].toString().trim('"').replace("_"," "),
                            cover = json.jsonObject["coverImage"]!!.jsonObject["large"].toString().trim('"'),
                            banner = if(json.jsonObject["bannerImage"]!=JsonNull) json.jsonObject["bannerImage"].toString().trim('"') else null,
                            meanScore = if(json.jsonObject["meanScore"]!=JsonNull) json.jsonObject["meanScore"].toString().toInt() else null,
                            isAdult = json.jsonObject["isAdult"].toString() == "true",
                            isFav = true,
                            userProgress = if (json.jsonObject["mediaListEntry"]!=JsonNull) json.jsonObject["mediaListEntry"]!!.jsonObject["progress"].toString().toInt() else null,
                            userScore = if (json.jsonObject["mediaListEntry"]!=JsonNull) json.jsonObject["mediaListEntry"]!!.jsonObject["score"].toString().toInt() else 0,
                            userStatus = if (json.jsonObject["mediaListEntry"]!=JsonNull) json.jsonObject["mediaListEntry"]!!.jsonObject["status"].toString().trim('"') else null,
                            userFavOrder = it.jsonObject["favouriteOrder"].toString().toIntOrNull(),
                            anime = if(json.jsonObject["type"].toString().trim('"') == "ANIME") Anime(totalEpisodes = if (json.jsonObject["episodes"] != JsonNull) json.jsonObject["episodes"].toString().toInt() else null, nextAiringEpisode = if (json.jsonObject["nextAiringEpisode"] != JsonNull) json.jsonObject["nextAiringEpisode"]!!.jsonObject["episode"].toString().toInt() - 1 else null) else null,
                            manga = if(json.jsonObject["type"].toString().trim('"') == "MANGA") Manga(totalChapters = if (json.jsonObject["chapters"] != JsonNull) json.jsonObject["chapters"].toString().toInt() else null) else null,
                        )
                    )
                }
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
        if (response?.get("data")!=null && response["data"] != JsonNull)
            response["data"]?.apply {  if(this != JsonNull) jsonObject["Page"]?.apply { if(this != JsonNull) jsonObject["recommendations"]?.apply { if(this != JsonNull) jsonArray.reversed().forEach{
                val json = it.jsonObject["mediaRecommendation"]
                if(json!=null && json!=JsonNull){
                    val id =  json.jsonObject["id"]?.toString()?.toInt()?:return responseArray
                    if (id !in ids) {
                        ids.add(id)
                        responseArray.add(
                            Media(
                                id = id,
                                idMAL = json.jsonObject["idMal"]!!.toString().toIntOrNull(),
                                name = json.jsonObject["title"]!!.jsonObject["english"].toString().trim('"').replace("\\\"","\""),
                                nameRomaji = json.jsonObject["title"]!!.jsonObject["romaji"].toString().trim('"').replace("\\\"","\""),
                                userPreferredName = json.jsonObject["title"]!!.jsonObject["userPreferred"].toString().trim('"').replace("\\\"","\""),
                                status = json.jsonObject["status"].toString().trim('"').replace("_"," "),
                                cover = json.jsonObject["coverImage"]!!.jsonObject["large"].toString().trim('"'),
                                banner = if(json.jsonObject["bannerImage"]!=JsonNull) json.jsonObject["bannerImage"].toString().trim('"') else null,
                                meanScore = if(json.jsonObject["meanScore"]!=JsonNull) json.jsonObject["meanScore"].toString().toInt() else null,
                                isFav = json.jsonObject["isFavourite"].toString()=="true",
                                isAdult = json.jsonObject["isAdult"].toString() == "true",
                                userProgress = if (json.jsonObject["mediaListEntry"]!=JsonNull) json.jsonObject["mediaListEntry"]!!.jsonObject["progress"].toString().toInt() else null,
                                userScore = if (json.jsonObject["mediaListEntry"]!=JsonNull) json.jsonObject["mediaListEntry"]!!.jsonObject["score"].toString().toInt() else 0,
                                userStatus = if (json.jsonObject["mediaListEntry"]!=JsonNull) json.jsonObject["mediaListEntry"]!!.jsonObject["status"].toString().trim('"') else null,
                                relation = json.jsonObject["type"].toString().trim('"'),
                                anime = if(json.jsonObject["type"].toString().trim('"') == "ANIME") Anime(totalEpisodes = if (json.jsonObject["episodes"] != JsonNull) json.jsonObject["episodes"].toString().toInt() else null, nextAiringEpisode = if (json.jsonObject["nextAiringEpisode"] != JsonNull) json.jsonObject["nextAiringEpisode"]!!.jsonObject["episode"].toString().toInt() - 1 else null) else null,
                                manga = if(json.jsonObject["type"].toString().trim('"') == "MANGA") Manga(totalChapters = if (json.jsonObject["chapters"] != JsonNull) json.jsonObject["chapters"].toString().toInt() else null) else null,
                            )
                        )
                    }
                }
            } } } }
        return responseArray
    }
    private fun bannerImage(type: String): String? {
        var image = loadData<BannerImage>("banner_$type")
        if(image==null || image.checkTime()){
            val response = executeQuery("""{ MediaListCollection(userId: ${Anilist.userid}, type: $type, chunk:1,perChunk:25, sort: [SCORE_DESC,UPDATED_TIME_DESC]) { lists { entries{ media { bannerImage } } } } } """)
            val data = if (response!=null) response["data"] else null
            if(data!=null && data!=JsonNull) {
                val mediaListCollection = if(data.jsonObject["MediaListCollection"]!=JsonNull) data.jsonObject["MediaListCollection"] else null

                val allImages = arrayListOf<String>()
                mediaListCollection?.jsonObject?.get("lists")?.jsonArray?.forEach {
                    it.jsonObject["entries"]?.jsonArray?.forEach { entry ->
                        val imageUrl = entry.jsonObject["media"]?.jsonObject?.get("bannerImage")?.toString()?.trim('"')
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
        val collection = ((response?:return unsorted)["data"]?:return unsorted).jsonObject["MediaListCollection"]?:return unsorted
        if(collection == JsonNull) return unsorted
        collection.jsonObject["lists"]?.jsonArray?.forEach { i ->
            val name = i.jsonObject["name"].toString().trim('"')
            unsorted[name] = arrayListOf()
            i.jsonObject["entries"]!!.jsonArray.forEach {
                val a = Media(
                    id = it.jsonObject["media"]!!.jsonObject["id"].toString().toInt(),
                    idMAL = it.jsonObject["media"]!!.jsonObject["idMal"].toString().toIntOrNull(),
                    name = it.jsonObject["media"]!!.jsonObject["title"]!!.jsonObject["english"].toString().trim('"'),
                    nameRomaji = it.jsonObject["media"]!!.jsonObject["title"]!!.jsonObject["romaji"].toString().trim('"'),
                    userPreferredName = it.jsonObject["media"]!!.jsonObject["title"]!!.jsonObject["userPreferred"].toString().trim('"'),
                    cover = it.jsonObject["media"]!!.jsonObject["coverImage"]!!.jsonObject["large"].toString().trim('"'),
                    banner = if(it.jsonObject["media"]!!.jsonObject["bannerImage"]!=JsonNull) it.jsonObject["media"]!!.jsonObject["bannerImage"].toString().trim('"') else null,
                    status = it.jsonObject["media"]!!.jsonObject["status"].toString().trim('"').replace("_"," "),
                    meanScore = if(it.jsonObject["media"]!!.jsonObject["meanScore"] != JsonNull) it.jsonObject["media"]!!.jsonObject["meanScore"].toString().toInt() else null,
                    isAdult = it.jsonObject["media"]!!.jsonObject["isAdult"].toString() == "true",
                    isFav = it.jsonObject["media"]!!.jsonObject["isFavourite"].toString()=="true",
                    userScore = it.jsonObject["score"].toString().toInt(),
                    userStatus = it.jsonObject["status"].toString().trim('"'),
                    userProgress = it.jsonObject["progress"].toString().toInt(),
                    manga = if(!anime) Manga(totalChapters = if (it.jsonObject["media"]!!.jsonObject["chapters"] != JsonNull) it.jsonObject["media"]!!.jsonObject["chapters"].toString().toInt() else null) else null,
                    anime = if(anime) Anime(totalEpisodes = if(it.jsonObject["media"]!!.jsonObject["episodes"] != JsonNull) it.jsonObject["media"]!!.jsonObject["episodes"].toString().toInt() else null, nextAiringEpisode = if (it.jsonObject["media"]!!.jsonObject["nextAiringEpisode"] != JsonNull) it.jsonObject["media"]!!.jsonObject["nextAiringEpisode"]!!.jsonObject["episode"].toString().toInt()-1 else null) else null
                )
                unsorted[name]!!.add(a)
                if(!allIds.contains(a.id)) {
                    allIds.add(a.id)
                    all.add(a)
                }
            }
        }

        val options = collection.jsonObject["user"]!!.jsonObject["mediaListOptions"]!!
        options.jsonObject[if(anime) "animeList" else "mangaList"]!!.jsonObject["sectionOrder"]!!.jsonArray.forEach {
            val list = it.toString().trim('"')
            if(unsorted.containsKey(list))
                sorted[list] = unsorted[list]!!
        }
        val favResponse = executeQuery("""{User(id:$userId){favourites{${if(anime) "anime" else "manga"}(page:0){edges{favouriteOrder node{id idMal isAdult mediaListEntry{progress score(format:POINT_100)status}chapters isFavourite episodes nextAiringEpisode{episode}meanScore isFavourite title{english romaji userPreferred}type status(version:2)bannerImage coverImage{large}}}}}}}""")
        sorted["Favourites"] = arrayListOf()
        favResponse?.jsonObject?.get("data").apply {
            if(this!=null && this!=JsonNull) this.jsonObject["User"]!!.jsonObject["favourites"]!!.jsonObject[if(anime) "anime" else "manga"]!!.jsonObject["edges"]!!.jsonArray.forEach {
                val json = it.jsonObject["node"]!!
                sorted["Favourites"]!!.add(
                    Media(
                        id = json.jsonObject["id"].toString().toInt(),
                        idMAL = json.jsonObject["idMal"].toString().toIntOrNull(),
                        name = json.jsonObject["title"]!!.jsonObject["english"].toString().trim('"').replace("\\\"","\""),
                        nameRomaji = json.jsonObject["title"]!!.jsonObject["romaji"].toString().trim('"').replace("\\\"","\""),
                        userPreferredName = json.jsonObject["title"]!!.jsonObject["userPreferred"].toString().trim('"').replace("\\\"","\""),
                        status = json.jsonObject["status"].toString().trim('"').replace("_"," "),
                        cover = json.jsonObject["coverImage"]!!.jsonObject["large"].toString().trim('"'),
                        banner = if(json.jsonObject["bannerImage"]!=JsonNull) json.jsonObject["bannerImage"].toString().trim('"') else null,
                        meanScore = if(json.jsonObject["meanScore"]!=JsonNull) json.jsonObject["meanScore"].toString().toInt() else null,
                        isAdult = json.jsonObject["isAdult"].toString() == "true",
                        isFav = true,
                        userProgress = if (json.jsonObject["mediaListEntry"]!=JsonNull) json.jsonObject["mediaListEntry"]!!.jsonObject["progress"].toString().toInt() else null,
                        userScore = if (json.jsonObject["mediaListEntry"]!=JsonNull) json.jsonObject["mediaListEntry"]!!.jsonObject["score"].toString().toInt() else 0,
                        userStatus = if (json.jsonObject["mediaListEntry"]!=JsonNull) json.jsonObject["mediaListEntry"]!!.jsonObject["status"].toString().trim('"') else null,
                        userFavOrder = it.jsonObject["favouriteOrder"].toString().toIntOrNull(),
                        anime = if(json.jsonObject["type"].toString().trim('"') == "ANIME") Anime(totalEpisodes = if (json.jsonObject["episodes"] != JsonNull) json.jsonObject["episodes"].toString().toInt() else null, nextAiringEpisode = if (json.jsonObject["nextAiringEpisode"] != JsonNull) json.jsonObject["nextAiringEpisode"]!!.jsonObject["episode"].toString().toInt() - 1 else null) else null,
                        manga = if(json.jsonObject["type"].toString().trim('"') == "MANGA") Manga(totalChapters = if (json.jsonObject["chapters"] != JsonNull) json.jsonObject["chapters"].toString().toInt() else null) else null,
                    )
                )
            }
        }
        sorted["Favourites"]!!.sortWith(compareBy { it.userFavOrder })

        sorted["All"] = all

        val sort = options.jsonObject["rowOrder"]!!.toString().trim('"')
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
                            if(node.jsonObject["isAdult"].toString()=="true")
                                tags!!.add(node.jsonObject["name"]!!.toString().trim('"'))
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
                        val id = it.jsonObject["id"].toString().toInt()
                        if (genres.checkId(id) && it.jsonObject["bannerImage"] != JsonNull) {
                            genres[genre] = Genre(
                                genre,
                                id,
                                it.jsonObject["bannerImage"].toString().trim('"'),
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
        if(response!=null){
            val a = if(response["data"]!=JsonNull) response["data"] else null
            val pag = a?.jsonObject?.get("Page") ?:return null
            val responseArray = arrayListOf<Media>()
            if(pag != JsonNull) pag.jsonObject["media"]?.jsonArray?.forEach { i ->
                val userStatus = if (i.jsonObject["mediaListEntry"] != JsonNull) i.jsonObject["mediaListEntry"]!!.jsonObject["status"].toString().trim('"') else null
                val genresArr = arrayListOf<String>()
                if (i.jsonObject["genres"]!! != JsonNull) {
                    i.jsonObject["genres"]!!.jsonArray.forEach { genre ->
                        genresArr.add(genre.toString().trim('"'))
                    }
                }
                responseArray.add(
                    Media(
                        id = i.jsonObject["id"].toString().toInt(),
                        idMAL = i.jsonObject["idMal"].toString().toIntOrNull(),
                        name = i.jsonObject["title"]!!.jsonObject["english"].toString().trim('"').replace("\\\"","\""),
                        nameRomaji = i.jsonObject["title"]!!.jsonObject["romaji"].toString().trim('"').replace("\\\"","\""),
                        userPreferredName = i.jsonObject["title"]!!.jsonObject["userPreferred"].toString().trim('"').replace("\\\"","\""),
                        cover = i.jsonObject["coverImage"]!!.jsonObject[if(hd) "extraLarge" else "large"].toString().trim('"'),
                        banner = if(i.jsonObject["bannerImage"]!=JsonNull) i.jsonObject["bannerImage"].toString().trim('"') else null,
                        status = i.jsonObject["status"].toString().trim('"').replace("_"," "),
                        isAdult = i.jsonObject["isAdult"].toString() == "true",
                        isFav = i.jsonObject["isFavourite"].toString() == "true",
                        userProgress = if (i.jsonObject["mediaListEntry"] != JsonNull) i.jsonObject["mediaListEntry"]!!.jsonObject["progress"].toString().toInt() else null,
                        userScore = if (i.jsonObject["mediaListEntry"] != JsonNull) i.jsonObject["mediaListEntry"]!!.jsonObject["score"].toString().toInt() else 0,
                        userStatus = userStatus,
                        relation = if(onList==true) userStatus else null,
                        genres = genresArr,
                        meanScore = if (i.jsonObject["meanScore"].toString().trim('"') != "null") i.jsonObject["meanScore"].toString().toInt() else null,
                        anime = if (i.jsonObject["type"].toString().trim('"') == "ANIME") Anime(totalEpisodes = if (i.jsonObject["episodes"] != JsonNull) i.jsonObject["episodes"].toString().toInt() else null, nextAiringEpisode = if (i.jsonObject["nextAiringEpisode"] != JsonNull) i.jsonObject["nextAiringEpisode"]!!.jsonObject["episode"].toString().toInt() - 1 else null) else null,
                        manga = if (i.jsonObject["type"].toString().trim('"') == "MANGA") Manga(totalChapters = if (i.jsonObject["chapters"] != JsonNull) i.jsonObject["chapters"].toString().toInt() else null) else null,
                    )
                )
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
                page = pag.jsonObject["pageInfo"]!!.jsonObject["currentPage"].toString().toInt(),
                hasNextPage = pag.jsonObject["pageInfo"]!!.jsonObject["hasNextPage"].toString()=="true",
            )
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
        val a = ((response["data"]?:return null).jsonObject["Page"]?:return null).jsonObject["airingSchedules"]?:return null
        val responseArray = arrayListOf<Media>()
        val idArr = arrayListOf<Int>()
        fun addMedia(listOnly:Boolean){
            a.jsonArray.forEach {
                val i = it.jsonObject["media"]!!
                val id = i.jsonObject["id"].toString().toInt()
                if(!idArr.contains(id)) if (!listOnly && (i.jsonObject["countryOfOrigin"].toString().trim('"')=="JP" && (if(!Anilist.adult) i.jsonObject["isAdult"].toString()=="false" else true)) || (listOnly && i.jsonObject["mediaListEntry"]!=JsonNull)) {
                    idArr.add(id)
                    responseArray.add(
                        Media(
                            id = i.jsonObject["id"].toString().toInt(),
                            idMAL = i.jsonObject["idMal"].toString().toIntOrNull(),
                            name = i.jsonObject["title"]!!.jsonObject["english"].toString().trim('"').replace("\\\"","\""),
                            nameRomaji = i.jsonObject["title"]!!.jsonObject["romaji"].toString().trim('"').replace("\\\"","\""),
                            userPreferredName = i.jsonObject["title"]!!.jsonObject["userPreferred"].toString().trim('"').replace("\\\"","\""),
                            cover = i.jsonObject["coverImage"]!!.jsonObject["large"].toString().trim('"'),
                            banner = if(i.jsonObject["bannerImage"]!=JsonNull) i.jsonObject["bannerImage"].toString().trim('"') else null,
                            status = i.jsonObject["status"].toString().trim('"').replace("_"," "),
                            isAdult = i.jsonObject["isAdult"].toString() == "true",
                            isFav = i.jsonObject["isFavourite"].toString() == "true",
                            userProgress = if (i.jsonObject["mediaListEntry"] != JsonNull) i.jsonObject["mediaListEntry"]!!.jsonObject["progress"].toString().toInt() else null,
                            userScore = if (i.jsonObject["mediaListEntry"] != JsonNull) i.jsonObject["mediaListEntry"]!!.jsonObject["score"].toString().toInt() else 0,
                            userStatus = if (i.jsonObject["mediaListEntry"] != JsonNull) i.jsonObject["mediaListEntry"]!!.jsonObject["status"].toString().trim('"') else null,
                            meanScore = if (i.jsonObject["meanScore"].toString().trim('"') != "null") i.jsonObject["meanScore"].toString().toInt() else null,
                            anime = Anime(totalEpisodes = if (i.jsonObject["episodes"] != JsonNull) i.jsonObject["episodes"].toString().toInt() else null, nextAiringEpisode = if (i.jsonObject["nextAiringEpisode"] != JsonNull) i.jsonObject["nextAiringEpisode"]!!.jsonObject["episode"].toString().toInt() - 1 else null)
                        )
                    )
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
        character.age = char.jsonObject["age"].toString().trim('"')
        character.gender = char.jsonObject["gender"].toString().trim('"')
        character.description = char.jsonObject["description"].toString().trim('"').replace("\\n","\n").replace("\\\"","\"")
        character.dateOfBirth = FuzzyDate(
            if(char.jsonObject["dateOfBirth"]?.jsonObject?.get("year") !=JsonNull) char.jsonObject["dateOfBirth"]!!.jsonObject["year"].toString().toInt() else null,
            if(char.jsonObject["dateOfBirth"]?.jsonObject?.get("month") !=JsonNull) char.jsonObject["dateOfBirth"]!!.jsonObject["month"].toString().toInt() else null,
            if(char.jsonObject["dateOfBirth"]?.jsonObject?.get("day") !=JsonNull) char.jsonObject["dateOfBirth"]!!.jsonObject["day"].toString().toInt() else null
        )
        character.roles = arrayListOf()
        char.jsonObject["media"]?.jsonObject?.get("edges")?.jsonArray?.forEach { i->
            character.roles!!.add(
                Media(
                    id = i.jsonObject["node"]!!.jsonObject["id"].toString().toInt(),
                    idMAL = i.jsonObject["node"]!!.jsonObject["idMal"].toString().toIntOrNull(),
                    name = i.jsonObject["node"]!!.jsonObject["title"]!!.jsonObject["english"].toString().trim('"').replace("\\\"","\""),
                    nameRomaji = i.jsonObject["node"]!!.jsonObject["title"]!!.jsonObject["romaji"].toString().trim('"').replace("\\\"","\""),
                    userPreferredName = i.jsonObject["node"]!!.jsonObject["title"]!!.jsonObject["userPreferred"].toString().trim('"').replace("\\\"","\""),
                    cover = i.jsonObject["node"]!!.jsonObject["coverImage"]!!.jsonObject["large"].toString().trim('"'),
                    banner = if(i.jsonObject["node"]!!.jsonObject["bannerImage"]!=JsonNull) i.jsonObject["node"]!!.jsonObject["bannerImage"].toString().trim('"') else null,
                    status = i.jsonObject["node"]!!.jsonObject["status"].toString().trim('"').replace("_"," "),
                    isAdult = i.jsonObject["node"]!!.jsonObject["isAdult"].toString() == "true",
                    isFav = i.jsonObject["node"]!!.jsonObject["isFavourite"].toString()=="true",
                    userProgress = if (i.jsonObject["node"]!!.jsonObject["mediaListEntry"]!=JsonNull) i.jsonObject["node"]!!.jsonObject["mediaListEntry"]!!.jsonObject["progress"].toString().toInt() else null,
                    userScore = if (i.jsonObject["node"]!!.jsonObject["mediaListEntry"]!=JsonNull) i.jsonObject["node"]!!.jsonObject["mediaListEntry"]!!.jsonObject["score"].toString().toInt() else 0,
                    userStatus = if (i.jsonObject["node"]!!.jsonObject["mediaListEntry"]!=JsonNull) i.jsonObject["node"]!!.jsonObject["mediaListEntry"]!!.jsonObject["status"].toString().trim('"') else null,
                    meanScore = if (i.jsonObject["node"]!!.jsonObject["meanScore"].toString().trim('"')!="null") i.jsonObject["node"]!!.jsonObject["meanScore"].toString().toInt() else null,
                    relation = i.jsonObject["characterRole"].toString().trim('"'),
                    anime = if (i.jsonObject["node"]!!.jsonObject["type"].toString().trim('"')=="ANIME") Anime(totalEpisodes = if (i.jsonObject["node"]!!.jsonObject["episodes"] != JsonNull) i.jsonObject["node"]!!.jsonObject["episodes"].toString().toInt() else null, nextAiringEpisode = if(i.jsonObject["node"]!!.jsonObject["nextAiringEpisode"] != JsonNull) i.jsonObject["node"]!!.jsonObject["nextAiringEpisode"]!!.jsonObject["episode"].toString().toInt()-1 else null) else null,
                    manga = if (i.jsonObject["node"]!!.jsonObject["type"].toString().trim('"')=="MANGA") Manga(totalChapters = if (i.jsonObject["node"]!!.jsonObject["chapters"] != JsonNull) i.jsonObject["node"]!!.jsonObject["chapters"].toString().toInt() else null) else null,
                )
            )
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
            val data = response["data"]?.jsonObject?.get("Studio")?.jsonObject?.get("media")?:return studio
            hasNextPage = data.jsonObject["pageInfo"]?.jsonObject?.get("hasNextPage")?.toString() == "true"
            data.jsonObject["edges"]?.jsonArray?.forEach { i->
                val id = i.jsonObject["node"]!!.jsonObject["id"].toString().toInt()
                val status = i.jsonObject["node"]!!.jsonObject["status"].toString().trim('"').replace("_"," ")
                val year = i.jsonObject["node"]!!.jsonObject["startDate"]!!.jsonObject["year"].toString()
                val title = if(status!="CANCELLED") (if(year!="null") year else "TBA") else status
                if(!studio.yearMedia!!.containsKey(title))
                    studio.yearMedia!![title] = arrayListOf()
                studio.yearMedia!![title]!!.add(
                    Media(
                        id = id,
                        idMAL = i.jsonObject["node"]!!.jsonObject["idMal"].toString().toIntOrNull(),
                        name = i.jsonObject["node"]!!.jsonObject["title"]!!.jsonObject["english"].toString().trim('"').replace("\\\"","\""),
                        nameRomaji = i.jsonObject["node"]!!.jsonObject["title"]!!.jsonObject["romaji"].toString().trim('"').replace("\\\"","\""),
                        userPreferredName = i.jsonObject["node"]!!.jsonObject["title"]!!.jsonObject["userPreferred"].toString().trim('"').replace("\\\"","\""),
                        cover = i.jsonObject["node"]!!.jsonObject["coverImage"]!!.jsonObject["large"].toString().trim('"'),
                        banner = if(i.jsonObject["node"]!!.jsonObject["bannerImage"]!=JsonNull) i.jsonObject["node"]!!.jsonObject["bannerImage"].toString().trim('"') else null,
                        status = i.jsonObject["node"]!!.jsonObject["status"].toString().trim('"').replace("_"," "),
                        isAdult = i.jsonObject["node"]!!.jsonObject["isAdult"].toString() == "true",
                        isFav = i.jsonObject["node"]!!.jsonObject["isFavourite"].toString()=="true",
                        userProgress = if (i.jsonObject["node"]!!.jsonObject["mediaListEntry"]!=JsonNull) i.jsonObject["node"]!!.jsonObject["mediaListEntry"]!!.jsonObject["progress"].toString().toInt() else null,
                        userScore = if (i.jsonObject["node"]!!.jsonObject["mediaListEntry"]!=JsonNull) i.jsonObject["node"]!!.jsonObject["mediaListEntry"]!!.jsonObject["score"].toString().toInt() else 0,
                        userStatus = if (i.jsonObject["node"]!!.jsonObject["mediaListEntry"]!=JsonNull) i.jsonObject["node"]!!.jsonObject["mediaListEntry"]!!.jsonObject["status"].toString().trim('"') else null,
                        meanScore = if (i.jsonObject["node"]!!.jsonObject["meanScore"].toString().trim('"')!="null") i.jsonObject["node"]!!.jsonObject["meanScore"].toString().toInt() else null,
                        anime = if (i.jsonObject["node"]!!.jsonObject["type"].toString().trim('"')=="ANIME") Anime(totalEpisodes = if (i.jsonObject["node"]!!.jsonObject["episodes"] != JsonNull) i.jsonObject["node"]!!.jsonObject["episodes"].toString().toInt() else null, nextAiringEpisode = if(i.jsonObject["node"]!!.jsonObject["nextAiringEpisode"] != JsonNull) i.jsonObject["node"]!!.jsonObject["nextAiringEpisode"]!!.jsonObject["episode"].toString().toInt()-1 else null) else null,
                        manga = if (i.jsonObject["node"]!!.jsonObject["type"].toString().trim('"')=="MANGA") Manga(totalChapters = if (i.jsonObject["node"]!!.jsonObject["chapters"] != JsonNull) i.jsonObject["node"]!!.jsonObject["chapters"].toString().toInt() else null) else null,
                    )
                )
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

