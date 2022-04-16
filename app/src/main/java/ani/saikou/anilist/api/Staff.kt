package ani.saikou.anilist.api

data class Staff(
    // The id of the staff member
    var id: Int,

    // The names of the staff member
    // var name: StaffName?,

    // The primary language of the staff member. Current values: Japanese, English, Korean, Italian, Spanish, Portuguese, French, German, Hebrew, Hungarian, Chinese, Arabic, Filipino, Catalan, Finnish, Turkish, Dutch, Swedish, Thai, Tagalog, Malaysian, Indonesian, Vietnamese, Nepali, Hindi, Urdu
    var languageV2: String?,

    // The staff images
    // var image: StaffImage?,

    // A general description of the staff member
    var description: String?,

    // The person's primary occupations
    var primaryOccupations: List<String>?,

    // The staff's gender. Usually Male, Female, or Non-binary but can be any string.
    var gender: String?,

    var dateOfBirth: FuzzyDate?,

    var dateOfDeath: FuzzyDate?,

    // The person's age in years
    var age: Int?,

    // [startYear, endYear] (If the 2nd value is not present staff is still active)
    var yearsActive: List<Int>?,

    // The persons birthplace or hometown
    var homeTown: String?,

    // The persons blood type
    var bloodType: String?,

    // If the staff member is marked as favourite by the currently authenticated user
    var isFavourite: Boolean,

    // If the staff member is blocked from being added to favourites
    var isFavouriteBlocked: Boolean,

    // The url for the staff page on the AniList website
    var siteUrl: String?,

    // Media where the staff member has a production role
    var staffMedia: MediaConnection?,

    // Characters voiced by the actor
    var characters: CharacterConnection?,

    // Media the actor voiced characters in. (Same data as characters with media as node instead of characters)
    var characterMedia: MediaConnection?,

    // Staff member that the submission is referencing
    var staff: Staff?,

    // Submitter for the submission
    var submitter: User?,

    // Status of the submission
    var submissionStatus: Int?,

    // Inner details of submission status
    var submissionNotes: String?,

    // The amount of user's who have favourited the staff member
    var favourites: Int?,

    // Notes for site moderators
    var modNotes: String?,
)
data class StaffConnection(
    // var edges: List<StaffEdge>?,

    var nodes: List<Staff>?,

    // The pagination information
    // var pageInfo: PageInfo?,
)