package ani.saikou.anilist.api

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class Recommendation(
    // The id of the recommendation
    var id: Int?,

    // Users rating of the recommendation
    var rating: Int?,

    // The rating of the recommendation by currently authenticated user
    // var userRating: RecommendationRating?,

    // The media the recommendation is from
    var media: Media?,

    // The recommended media
    var mediaRecommendation: Media?,

    // The user that first created the recommendation
    var user: User?,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class RecommendationConnection(
    //var edges: List<RecommendationEdge>?,

    var nodes: List<Recommendation>?,

    // The pagination information
    //var pageInfo: PageInfo?,

)