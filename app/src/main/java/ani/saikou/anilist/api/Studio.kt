package ani.saikou.anilist.api

data class Studio(
    // The id of the studio
    var id: Int,

    // The name of the studio
    var name: String,

    // If the studio is an animation studio or a different kind of company
    var isAnimationStudio: Boolean,

    // The media the studio has worked on
    var media: MediaConnection?,

    // The url for the studio page on the AniList website
    var siteUrl: String?,

    // If the studio is marked as favourite by the currently authenticated user
    var isFavourite: Boolean,

    // The amount of user's who have favourited the studio
    var favourites: Int?,
)

data class StudioConnection(
    //var edges: List<StudioEdge>?,

    var nodes: List<Studio>?,

    // The pagination information
    //var pageInfo: PageInfo?,
)