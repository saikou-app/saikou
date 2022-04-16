package ani.saikou.anilist.api


data class User(
    // The id of the user
    var id: Int,

    // The name of the user
    var name: String,

    // The bio written by user (Markdown)
    var about: String?,

    // The user's avatar images
    var avatar: UserAvatar?,

    // The user's banner images
    var bannerImage: String?,

    // If the authenticated user if following this user
    var isFollowing: Boolean?,

    // If this user if following the authenticated user
    var isFollower: Boolean?,

    // If the user is blocked by the authenticated user
    var isBlocked: Boolean?,

    // FIXME: No documentation is provided for "Json"
    // var bans: Json?,

    // The user's general options
    var options: UserOptions?,

    // The user's media list options
    // var mediaListOptions: MediaListOptions?,

    // The users favourites
    // var favourites: Favourites?,

    // The users anime & manga list statistics
    var statistics: UserStatisticTypes?,

    // The number of unread notifications the user has
    var unreadNotificationCount: Int?,

    // The url for the user page on the AniList website
    var siteUrl: String?,

    // The donation tier of the user
    var donatorTier: Int?,

    // Custom donation badge text
    var donatorBadge: String?,

    // The user's moderator roles if they are a site moderator
    // var moderatorRoles: List<ModRole>?,

    // When the user's account was created. (Does not exist for accounts created before 2020)
    var createdAt: Int?,

    // When the user's data was last updated
    var updatedAt: Int?,

    // The user's previously used names.
    // var previousNames: List<UserPreviousName>?,

)

data class UserOptions(
    // The language the user wants to see media titles in
    // var titleLanguage: UserTitleLanguage?,

    // Whether the user has enabled viewing of 18+ content
    var displayAdultContent: Boolean?,

    // Whether the user receives notifications when a show they are watching aires
    var airingNotifications: Boolean?,

    // Profile highlight color (blue, purple, pink, orange, red, green, gray)
    var profileColor: String?,

    // Notification options
    // var notificationOptions: List<NotificationOption>?,

    // The user's timezone offset (Auth user only)
    var timezone: String?,

    // Minutes between activity for them to be merged together. 0 is Never, Above 2 weeks (20160 mins) is Always.
    var activityMergeTime: Int?,

    // The language the user wants to see staff and character names in
    // var staffNameLanguage: UserStaffNameLanguage?,

    // Whether the user only allow messages from users they follow
    var restrictMessagesToFollowing: Boolean?,

    // The list activity types the user has disabled from being created from list updates
    // var disabledListActivity: List<ListActivityOption>?,
)

data class UserAvatar( // The avatar of user at its largest size
    var large: String?,

// The avatar of user at medium size
    var medium: String?,
)

data class UserStatisticTypes(
    var anime: UserStatistics?,
    var manga: UserStatistics?
)

data class UserStatistics( //
    var count: Int,
    var meanScore: Float,
    var standardDeviation: Float,
    var minutesWatched: Int,
    var episodesWatched: Int,
    var chaptersRead: Int,
    var volumesRead: Int,
//    var formats: List<UserFormatStatistic>?,
//    var statuses: List<UserStatusStatistic>?,
//    var scores: List<UserScoreStatistic>?,
//    var lengths: List<UserLengthStatistic>?,
//    var releaseYears: List<UserReleaseYearStatistic>?,
//    var startYears: List<UserStartYearStatistic>?,
//    var genres: List<UserGenreStatistic>?,
//    var tags: List<UserTagStatistic>?,
//    var countries: List<UserCountryStatistic>?,
//    var voiceActors: List<UserVoiceActorStatistic>?,
//    var staff: List<UserStaffStatistic>?,
//    var studios: List<UserStudioStatistic>?,
)
