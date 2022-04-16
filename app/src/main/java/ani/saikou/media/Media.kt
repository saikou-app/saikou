package ani.saikou.media

import ani.saikou.FuzzyDate
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
    constructor(apiMedia: ani.saikou.anilist.api.Media): this(
        id = apiMedia.id,
        idMAL = apiMedia.idMal,
        popularity = apiMedia.popularity,
        name = apiMedia.title!!.english!!,
        nameRomaji = apiMedia.title!!.romaji!!,
        userPreferredName = apiMedia.title!!.userPreferred!!,
        cover = apiMedia.coverImage!!.large,
        banner = apiMedia.bannerImage,
        status = apiMedia.status.toString().replace("_", " "),
        isFav = apiMedia.isFavourite,
        isAdult = apiMedia.isAdult ?: false,
        userProgress = if (apiMedia.mediaListEntry != null) apiMedia.mediaListEntry!!.progress else null,
        userScore = if (apiMedia.mediaListEntry != null) apiMedia.mediaListEntry!!.score!!.toInt() else 0,
        userStatus = if (apiMedia.mediaListEntry != null) apiMedia.mediaListEntry!!.status!!.toString() else null,
        meanScore = if (apiMedia.meanScore != null) apiMedia.meanScore!! else null,
        anime = if (apiMedia.type!! == MediaType.ANIME) Anime(totalEpisodes = apiMedia.episodes, nextAiringEpisode = if (apiMedia.nextAiringEpisode != null) apiMedia.nextAiringEpisode!!.episode - 1 else null) else null,
        manga = if (apiMedia.type!! == MediaType.MANGA) Manga(totalChapters = apiMedia.chapters) else null,
    )

    constructor(mediaEdge: ani.saikou.anilist.api.MediaEdge): this(
        id = mediaEdge.node!!.id,
        idMAL = mediaEdge.node!!.idMal,
        popularity = mediaEdge.node!!.popularity,
        name = mediaEdge.node!!.title!!.english!!,
        nameRomaji = mediaEdge.node!!.title!!.romaji!!,
        userPreferredName = mediaEdge.node!!.title!!.userPreferred!!,
        cover = mediaEdge.node!!.coverImage!!.large,
        banner = mediaEdge.node!!.bannerImage,
        status = mediaEdge.node!!.status.toString().replace("_", " "),
        isFav = mediaEdge.node!!.isFavourite,
        isAdult = mediaEdge.node!!.isAdult ?: false,
        userProgress = if (mediaEdge.node!!.mediaListEntry != null) mediaEdge.node!!.mediaListEntry!!.progress else null,
        userScore = if (mediaEdge.node!!.mediaListEntry != null) mediaEdge.node!!.mediaListEntry!!.score!!.toInt() else 0,
        userStatus = if (mediaEdge.node!!.mediaListEntry != null) mediaEdge.node!!.mediaListEntry!!.status!!.toString() else null,
        meanScore = if (mediaEdge.node!!.meanScore != null) mediaEdge.node!!.meanScore!! else null,
        relation = mediaEdge.relationType.toString(),
        anime = if (mediaEdge.node!!.type!! == MediaType.ANIME) Anime(totalEpisodes = mediaEdge.node!!.episodes, nextAiringEpisode = if (mediaEdge.node!!.nextAiringEpisode != null) mediaEdge.node!!.nextAiringEpisode!!.episode - 1 else null) else null,
        manga = if (mediaEdge.node!!.type!! == MediaType.MANGA) Manga(totalChapters = mediaEdge.node!!.chapters) else null,
    )
    fun getMainName() = if (name!="null") name else nameRomaji
    fun getMangaName() = if (countryOfOrigin!="JP") getMainName() else nameRomaji
}