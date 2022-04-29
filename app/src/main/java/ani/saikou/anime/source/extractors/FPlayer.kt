package ani.saikou.anime.source.extractors

import ani.saikou.anime.Episode
import ani.saikou.anime.source.Extractor
import ani.saikou.getSize
import ani.saikou.httpClient
import ani.saikou.others.asyncEach
import ani.saikou.others.logError
import com.fasterxml.jackson.databind.exc.MismatchedInputException

class FPlayer(private val getSize: Boolean) : Extractor() {
    override suspend fun getStreamLinks(name: String, url: String): Episode.StreamLinks {
        val apiLink = url.replace("/v/", "/api/source/")
        val tempQuality = mutableListOf<Episode.Quality>()
        try {
            val json = httpClient.post(apiLink, referer = url).parsed<Json>()

            if (json.success) {
                json.data?.asyncEach {
                    tempQuality.add(
                        Episode.Quality(
                            it.file,
                            it.label,
                            if (getSize) getSize(it.file) else null
                        )
                    )
                }
            }
        }
        catch (e: MismatchedInputException) {}
        catch (e: Exception) {
            logError(e)
        }
        return Episode.StreamLinks(
            name,
            tempQuality,
            null
        )
    }


    private data class Data(
        val file: String,
        val label: String
    )

    private data class Json(
        val success: Boolean,
        val data: List<Data>?
    )
}