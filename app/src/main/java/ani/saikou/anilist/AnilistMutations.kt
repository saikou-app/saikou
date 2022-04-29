package ani.saikou.anilist

import ani.saikou.anilist.api.FuzzyDate

class AnilistMutations {

    suspend fun toggleFav(anime: Boolean = true, id: Int) {
        val query =
            """mutation (${"$"}animeId: Int,${"$"}mangaId:Int) { ToggleFavourite(animeId:${"$"}animeId,mangaId:${"$"}mangaId){ anime { edges { id } } manga { edges { id } } } }"""
        val variables = if (anime) """{"animeId":"$id"}""" else """{"mangaId":"$id"}"""
        executeQuery(query, variables)
    }

    suspend fun editList(
        mediaID: Int,
        progress: Int? = null,
        score: Int? = null,
        status: String? = null,
        startedAt: FuzzyDate? = null,
        completedAt: FuzzyDate? = null
    ) {
        val query = """
            mutation ( ${"$"}mediaID: Int, ${"$"}progress: Int, ${"$"}scoreRaw:Int, ${"$"}status:MediaListStatus, ${"$"}start:FuzzyDateInput${if (startedAt != null) "=" + startedAt.toVariableString() else ""}, ${"$"}completed:FuzzyDateInput${if (completedAt != null) "=" + completedAt.toVariableString() else ""} ) {
                SaveMediaListEntry( mediaId: ${"$"}mediaID, progress: ${"$"}progress, scoreRaw: ${"$"}scoreRaw, status:${"$"}status, startedAt: ${"$"}start, completedAt: ${"$"}completed ) {
                    score(format:POINT_10_DECIMAL) startedAt{year month day} completedAt{year month day}
                }
            }
        """.replace("\n", "").replace("""    """, "")

        val variables = """{"mediaID":$mediaID
            ${if (progress != null) ""","progress":$progress""" else ""}
            ${if (score != null) ""","scoreRaw":$score""" else ""}
            ${if (status != null) ""","status":"$status"""" else ""}
            }""".replace("\n", "").replace("""    """, "")
        executeQuery(query, variables)
    }

    suspend fun deleteList(listId: Int) {
        val query = "mutation(${"$"}id:Int){DeleteMediaListEntry(id:${"$"}id){deleted}}"
        val variables = """{"id":"$listId"}"""
        executeQuery(query, variables)
    }
}