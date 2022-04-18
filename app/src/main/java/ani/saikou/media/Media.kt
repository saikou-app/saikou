package ani.saikou.media

import ani.saikou.FuzzyDate
import ani.saikou.anilist.api.MediaEdge
import ani.saikou.anilist.api.MediaList
import ani.saikou.anilist.api.Media as ApiMedia
import ani.saikou.anilist.api.MediaType
import ani.saikou.anime.Anime
import ani.saikou.manga.Manga
import java.io.Serializable

data class Media(
    val anime: Anime? = null,
    val manga: Manga? = null,
    val id: Int,

    var idMAL: Int?=null,
    var typeMAL:String?=null,

    val name: String,
    val nameRomaji: String,
    val cover: String?=null,
    val banner: String?=null,
    var relation: String? =null,
    var popularity: Int?=null,

    var isAdult: Boolean,
    var isFav: Boolean = false,
    var notify: Boolean = false,
    val userPreferredName: String,

    var userListId:Int?=null,
    var userProgress: Int? = null,
    var userStatus: String? = null,
    var userScore: Int = 0,
    var userRepeat:Int = 0,
    var userUpdatedAt: Long?=null,
    var userStartedAt : FuzzyDate = FuzzyDate(),
    var userCompletedAt : FuzzyDate=FuzzyDate(),
    var userFavOrder:Int?=null,

    val status : String? = null,
    var format:String?=null,
    var source:String? = null,
    var countryOfOrigin:String?=null,
    val meanScore: Int? = null,
    var genres:ArrayList<String> = arrayListOf(),
    var tags:ArrayList<String> = arrayListOf(),
    var description: String? = null,
    var synonyms:ArrayList<String> = arrayListOf(),
    var trailer:String?=null,
    var startDate: FuzzyDate?=null,
    var endDate: FuzzyDate?=null,

    var characters:ArrayList<Character>?=null,
    var prequel:Media?=null,
    var sequel:Media?=null,
    var relations: ArrayList<Media>?=null,
    var recommendations: ArrayList<Media>?=null,

    var nameMAL:String?=null,
    var shareLink:String?=null,
    var selected: Selected?=null,

    var cameFromContinue:Boolean=false
) : Serializable{
    constructor(apiMedia: ApiMedia): this(
        id = apiMedia.id,
        idMAL = apiMedia.idMal,
        popularity = apiMedia.popularity,
        name = apiMedia.title!!.english.toString(),
        nameRomaji = apiMedia.title!!.romaji.toString(),
        userPreferredName = apiMedia.title!!.userPreferred.toString(),
        cover = apiMedia.coverImage!!.large,
        banner = apiMedia.bannerImage,
        status = apiMedia.status.toString().replace("_", " "),
        isFav = apiMedia.isFavourite,
        isAdult = apiMedia.isAdult ?: false,
        userProgress = apiMedia.mediaListEntry?.progress,
        userScore = apiMedia.mediaListEntry?.score?.toInt() ?: 0,
        userStatus = apiMedia.mediaListEntry?.status?.toString(),
        meanScore = apiMedia.meanScore,
        anime = if (apiMedia.type!! == MediaType.ANIME) Anime(totalEpisodes = apiMedia.episodes, nextAiringEpisode = if (apiMedia.nextAiringEpisode != null) apiMedia.nextAiringEpisode!!.episode - 1 else null) else null,
        manga = if (apiMedia.type!! == MediaType.MANGA) Manga(totalChapters = apiMedia.chapters) else null,
    )

    constructor(mediaList: MediaList): this(mediaList.media!!) {
        this.userProgress = mediaList.progress
        this.userScore = mediaList.score?.toInt() ?: 0
        this.userStatus = mediaList.status.toString()
    }

    constructor(mediaEdge: MediaEdge): this(mediaEdge.node!!) {
        this.relation = mediaEdge.relationType.toString().replace("_", " ")
    }

    fun getMainName() = if (name!="null") name else nameRomaji
    fun getMangaName() = if (countryOfOrigin!="JP") getMainName() else nameRomaji
}