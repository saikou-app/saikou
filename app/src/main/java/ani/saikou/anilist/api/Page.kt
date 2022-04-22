package ani.saikou.anilist.api

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class Page(
    // The pagination information
    var pageInfo: PageInfo?,
  
    var users: List<User>?,
  
    var media: List<Media>?,
  
    var characters: List<Character>?,
  
    var staff: List<Staff>?,
  
    var studios: List<Studio>?,
  
    var mediaList: List<MediaList>?,
  
    var airingSchedules: List<AiringSchedule>?,
  
    // var mediaTrends: List<MediaTrend>?,
  
    // var notifications: List<NotificationUnion>?,
  
    var followers: List<User>?,
  
    var following: List<User>?,
  
    // var activities: List<ActivityUnion>?,
  
    // var activityReplies: List<ActivityReply>?,
  
    var threads: List<Thread>?,
  
    // var threadComments: List<ThreadComment>?,
  
    // var reviews: List<Review>?,
  
    var recommendations: List<Recommendation>?,
  
    var likes: List<User>?,
)

data class PageInfo(
    // The total number of items. Note: This value is not guaranteed to be accurate, do not rely on this for logic
    var total: Int?,

    // The count on a page
    var perPage: Int?,

    // The current page
    var currentPage: Int?,

    // The last page
    var lastPage: Int?,

    // If there is another page
    var hasNextPage: Boolean?,
)