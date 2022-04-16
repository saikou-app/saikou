package ani.saikou.anilist.api

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class Page(
    // The pagination information
    // var pageInfo: PageInfo?,
  
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