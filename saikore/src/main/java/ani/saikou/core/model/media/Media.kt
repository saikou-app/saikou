package ani.saikou.core.model.media

import ani.saikou.core.model.anime.Anime
import ani.saikou.core.model.manga.Manga
import ani.saikou.core.utils.FuzzyDate
import java.io.Serializable

data class Media(
    val anime: Anime? = null,
    val manga: Manga? = null,
    val id: Int,

    var idMAL: Int? = null,
    var typeMAL: String? = null,

    val name: String,
    val nameRomaji: String,
    val cover: String? = null,
    val banner: String? = null,
    var relation: String? = null,
    var popularity: Int? = null,

    var isAdult: Boolean,
    var isFav: Boolean = false,
    var notify: Boolean = false,
    val userPreferredName: String,

    var userListId: Int? = null,
    var userProgress: Int? = null,
    var userStatus: String? = null,
    var userScore: Int = 0,
    var userRepeat: Int = 0,
    var userUpdatedAt: Long? = null,
    var userStartedAt: FuzzyDate = FuzzyDate(),
    var userCompletedAt: FuzzyDate = FuzzyDate(),
    var userFavOrder: Int? = null,

    val status: String? = null,
    var format: String? = null,
    var source: String? = null,
    var countryOfOrigin: String? = null,
    val meanScore: Int? = null,
    var genres: MutableList<String> = mutableListOf(),
    var tags: MutableList<String> = mutableListOf(),
    var description: String? = null,
    var synonyms: MutableList<String> = mutableListOf(),
    var trailer: String? = null,
    var startDate: FuzzyDate? = null,
    var endDate: FuzzyDate? = null,

    var characters: MutableList<Character>? = null,
    var prequel: Media? = null,
    var sequel: Media? = null,
    var relations: MutableList<Media>? = null,
    var recommendations: MutableList<Media>? = null,

    var nameMAL: String? = null,
    var shareLink: String? = null,
    var selected: Selected? = null,

    var cameFromContinue: Boolean = false
) : Serializable {
    val mainName: String
        get() = if (name != "null") name else nameRomaji

    val mangaName: String
        get() = if (countryOfOrigin != "JP") mainName else nameRomaji
}