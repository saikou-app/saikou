package ani.saikou.anilist.api

data class Media(// The id of the media
    var id: Int,

    // The mal id of the media
    var idMal: Int?,

    // The official titles of the media in various languages
    var title: MediaTitle?,

    // The type of the media; anime or manga
    var type: MediaType?,

    // The format the media was released in
    //var format: MediaFormat?,

    // The current releasing status of the media
    var status: MediaStatus?,

    // Short description of the media's story and characters
    // var description: String?,

    // The first official release date of the media
    // var startDate: FuzzyDate?,

    // The last official release date of the media
    // var endDate: FuzzyDate?,

    // The season the media was initially released in
    // var season: MediaSeason?,

    // The season year the media was initially released in
    var seasonYear: Int?,

    // The year & season the media was initially released in
    var seasonInt: Int?,

    // The amount of episodes the anime has when complete
    var episodes: Int?,

    // The general length of each anime episode in minutes
    var duration: Int?,

    // The amount of chapters the manga has when complete
    var chapters: Int?,

    // The amount of volumes the manga has when complete
    var volumes: Int?,

    // Where the media was created. (ISO 3166-1 alpha-2)
    // var countryOfOrigin: CountryCode?,

    // If the media is officially licensed or a self-published doujin release
    var isLicensed: Boolean?,

    // Source type the media was adapted from.
    // var source: MediaSource?,

    // Official Twitter hashtags for the media
    var hashtag: String?,

    // Media trailer or advertisement
    // var trailer: MediaTrailer?,

    // When the media's data was last updated
    var updatedAt: Int?,

    // The cover images of the media
    var coverImage: MediaCoverImage?,

    // The banner image of the media
    var bannerImage: String?,

    // The genres of the media
    var genres: List<String>?,

    // Alternative titles of the media
    var synonyms: List<String>?,

    // A weighted average score of all the user's scores of the media
    var averageScore: Int?,

    // Mean score of all the user's scores of the media
    var meanScore: Int?,

    // The number of users with the media on their list
    var popularity: Int?,

    // Locked media may not be added to lists our favorited. This may be due to the entry pending for deletion or other reasons.
    var isLocked: Boolean?,

    // The amount of related activity in the past hour
    var trending: Int?,

    // The amount of user's who have favourited the media
    var favourites: Int?,

    // List of tags that describes elements and themes of the media
    // var tags: List<MediaTag>?,

    // Other media in the same or connecting franchise
    // var relations: MediaConnection?,

    // The characters in the media
    // var characters: CharacterConnection?,

    // The staff who produced the media
    // var staff: StaffConnection?,

    // The companies who produced the media
    // var studios: StudioConnection?,

    // If the media is marked as favourite by the current authenticated user
    var isFavourite: Boolean,

    // If the media is blocked from being added to favourites
    var isFavouriteBlocked: Boolean,

    // If the media is intended only for 18+ adult audiences
    var isAdult: Boolean?,

    // The media's next episode airing schedule
    var nextAiringEpisode: AiringSchedule?,

    // The media's entire airing schedule
    // var airingSchedule: AiringScheduleConnection?,

    // The media's daily trend stats
    // var trends: MediaTrendConnection?,

    // External links to another site related to the media
    // var externalLinks: List<MediaExternalLink>?,

    // Data and links to legal streaming episodes on external sites
    // var streamingEpisodes: List<MediaStreamingEpisode>?,

    // The ranking of the media in a particular time span and format compared to other media
    // var rankings: List<MediaRank>?,

    // The authenticated user's media list entry for the media
    var mediaListEntry: MediaList?,

    // User reviews of the media
    // var reviews: ReviewConnection?,

    // User recommendations for similar media
    // var recommendations: RecommendationConnection?,

    //
    // var stats: MediaStats?,

    // The url for the media page on the AniList website
    var siteUrl: String?,

    // If the media should have forum thread automatically created for it on airing episode release
    var autoCreateForumThread: Boolean?,

    // If the media is blocked from being recommended to/from
    var isRecommendationBlocked: Boolean?,

    // If the media is blocked from being reviewed
    var isReviewBlocked: Boolean?,

    // Notes for site moderators
    var modNotes: String?,
)

data class MediaTitle(
    // The romanization of the native language title
    var romaji: String?,

    // The official english title
    var english: String?,

    // Official title in it's native language
    var native: String?,

    // The currently authenticated users preferred title language. Default romaji for non-authenticated
    var userPreferred: String?,
)

enum class MediaType() {
    ANIME, MANGA
}

enum class MediaStatus() {
    FINISHED, RELEASING, NOT_YET_RELEASED, CANCELLED, HIATUS
}

data class AiringSchedule(
    // The id of the airing schedule item
    var id: Int,

    // The time the episode airs at
    var airingAt: Int,

    // Seconds until episode starts airing
    var timeUntilAiring: Int,

    // The airing episode number
    var episode: Int,

    // The associate media id of the airing episode
    var mediaId: Int,

    // The associate media of the airing episode
    var media: Media?,
)

data class MediaCoverImage(
    // The cover image url of the media at its largest size. If this size isn't available, large will be provided instead.
    var extraLarge: String?,

    // The cover image url of the media at a large size
    var large: String?,

    // The cover image url of the media at medium size
    var medium: String?,

    // Average #hex color of cover image
    var color: String?,
)

data class MediaList(
    // The id of the list entry
    // var id: Int,

    // The id of the user owner of the list entry
    // var userId: Int,

    // The id of the media
    // var mediaId: Int,

    // The watching/reading status
    var status: MediaListStatus?,

    // The score of the entry
    var score: Float?,

    // The amount of episodes/chapters consumed by the user
    var progress: Int?,

    // The amount of volumes read by the user
    // var progressVolumes: Int?,

    // The amount of times the user has rewatched/read the media
    // var repeat: Int?,

    // Priority of planning
    // var priority: Int?,

    // If the entry should only be visible to authenticated user
    // var private: Boolean?,

    // Text notes
    // var notes: String?,

    // If the entry shown be hidden from non-custom lists
    // var hiddenFromStatusLists: Boolean?,

    // Map of booleans for which custom lists the entry are in
    // var customLists: Json?,

    // Map of advanced scores with name keys
    // var advancedScores: Json?,

    // When the entry was started by the user
    // var startedAt: FuzzyDate?,

    // When the entry was completed by the user
    // var completedAt: FuzzyDate?,

    // When the entry data was last updated
    // var updatedAt: Int?,

    // When the entry data was created
    // var createdAt: Int?,

    //var media: Media?,

    //var user: User?
)

enum class MediaListStatus() {
    CURRENT, PLANNING, COMPLETED, DROPPED, PAUSED, REPEATING
}