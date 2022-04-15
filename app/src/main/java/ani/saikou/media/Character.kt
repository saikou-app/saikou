package ani.saikou.media

import ani.saikou.FuzzyDate
import java.io.Serializable
import kotlin.collections.ArrayList

data class Character(
    val id: Int,
    val name: String,
    val image: String,
    val banner: String?,
    val role: String,

    var description: String? = null,
    var age: String? = null,
    var gender: String? = null,
    var dateOfBirth: FuzzyDate? = null,
    var roles: ArrayList<Media>? = null
) : Serializable
