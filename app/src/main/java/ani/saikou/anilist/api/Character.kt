package ani.saikou.anilist.api

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

data class Character(
    // The id of the character
    var id: Int,

    // The names of the character
    var name: CharacterName?,

    // Character images
    var image: CharacterImage?,

    // A general description of the character
    var description: String?,

    // The character's gender. Usually Male, Female, or Non-binary but can be any string.
    var gender: String?,

    // The character's birth date
    var dateOfBirth: FuzzyDate?,

    // The character's age. Note this is a string, not an int, it may contain further text and additional ages.
    var age: String?,

    // The characters blood type
    var bloodType: String?,

    // If the character is marked as favourite by the currently authenticated user
    var isFavourite: Boolean,

    // If the character is blocked from being added to favourites
    var isFavouriteBlocked: Boolean,

    // The url for the character page on the AniList website
    var siteUrl: String?,

    // Media that includes the character
    var media: MediaConnection?,

    // The amount of user's who have favourited the character
    var favourites: Int?,

    // Notes for site moderators
    var modNotes: String?,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CharacterConnection(
    var edges: List<CharacterEdge>?,

    var nodes: List<Character>?,

    // The pagination information
    // var pageInfo: PageInfo?,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CharacterEdge(
    var node: Character?,

    // The id of the connection
    var id: Int?,

    // The characters role in the media
    var role: CharacterRole?,

    // Media specific character name
    var name: String?,

    // The voice actors of the character
    // var voiceActors: List<Staff>?,

    // The voice actors of the character with role date
    // var voiceActorRoles: List<StaffRoleType>?,

    // The media the character is in
    var media: List<Media>?,

    // The order the character should be displayed from the users favourites
    var favouriteOrder: Int?,
)

enum class CharacterRole() {
    MAIN, SUPPORTING, BACKGROUND
}

data class CharacterName(
    // The character's given name
    var first: String?,

    // The character's middle name
    var middle: String?,

    // The character's surname
    var last: String?,

    // The character's first and last name
    var full: String?,

    // The character's full name in their native language
    var native: String?,

    // Other names the character might be referred to as
    var alternative: List<String>?,

    // Other names the character might be referred to as but are spoilers
    var alternativeSpoiler: List<String>?,

    // The currently authenticated users preferred name language. Default romaji for non-authenticated
    var userPreferred: String?,
)

data class CharacterImage(
    // The character's image of media at its largest size
    var large: String?,

    // The character's image of media at medium size
    var medium: String?,

    )