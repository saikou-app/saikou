package ani.saikou.anime.source.parsers


import ani.saikou.*
import ani.saikou.anime.Episode
import ani.saikou.anime.source.AnimeParser
import ani.saikou.media.Media
import ani.saikou.media.Source
import ani.saikou.others.logError
import com.fasterxml.jackson.annotation.JsonProperty
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class Twist(override val name: String = "twist.moe") : AnimeParser() {

    private val secret = "267041df55ca2b36f2e322d05ee2c9cf".toByteArray()
    private fun base64decode(oriString: String): ByteArray {
        return android.util.Base64.decode(oriString, android.util.Base64.DEFAULT)
    }

    private fun md5(input: ByteArray): ByteArray {
        return MessageDigest.getInstance("MD5").digest(input)
    }

    private fun generateKey(salt: ByteArray): ByteArray {
        var key = md5(secret + salt)
        var currentKey = key
        while (currentKey.size < 48) {
            key = md5(key + secret + salt)
            currentKey += key
        }
        return currentKey
    }

    private fun decryptSourceUrl(decryptionKey: ByteArray, sourceUrl: String): String {
        val cipherData = base64decode(sourceUrl)
        val encrypted = cipherData.copyOfRange(16, cipherData.size)
        val aesCBC = Cipher.getInstance("AES/CBC/PKCS5Padding")

        Objects.requireNonNull(aesCBC).init(
            Cipher.DECRYPT_MODE, SecretKeySpec(
                decryptionKey.copyOfRange(0, 32),
                "AES"
            ),
            IvParameterSpec(decryptionKey.copyOfRange(32, decryptionKey.size))
        )
        val decryptedData = aesCBC!!.doFinal(encrypted)
        return String(decryptedData, StandardCharsets.UTF_8)
    }

    private fun decryptSource(input: String): String {
        return decryptSourceUrl(generateKey(base64decode(input).copyOfRange(8, 16)), input)
    }


    override suspend fun getStream(episode: Episode, server: String): Episode {
        return getStreams(episode)
    }

    override suspend fun getStreams(episode: Episode): Episode {
        try {
            val url = "https://cdn.twist.moe${decryptSource(episode.link?:return episode)}"
            episode.streamLinks = mutableMapOf(
                "Twist" to Episode.StreamLinks(
                    "Twist",
                    listOf(
                        Episode.Quality(
                            url = url,
                            quality = "Default Quality",
                            size = getSize(url)
                        )
                    ),
                    mutableMapOf("referer" to "https://twist.moe/")
                )
            )
        } catch (e: Exception) {
            logError(e)
        }
        return episode
    }

    override suspend fun getEpisodes(media: Media): MutableMap<String, Episode> {
        val load: Source? = loadData("twist_${media.id}")
        if (load != null) {
            setTextListener("Selected : ${load.name}")
            return getSlugEpisodes(load.link)
        }
        try {
            if (media.idMAL != null) {
                val source = getSearchData()[media.idMAL]?: return mutableMapOf()
                setTextListener("Selected : ${source.name}")
                return getSlugEpisodes(source.link)
            }
        } catch (e: Exception) {
            logError(e)
        }
        return mutableMapOf()
    }

    override suspend fun search(string: String): ArrayList<Source> {
        val arr = arrayListOf<Source>()
        try {
            arr.addAll(getSearchData().values)
            arr.sortByTitle(string)
        } catch (e: Exception) {
            logError(e)
        }
        return arr
    }

    override suspend fun getSlugEpisodes(slug: String): MutableMap<String, Episode> {
        val responseList = mutableMapOf<String, Episode>()
        try {
            val slugURL = "https://api.twist.moe/api/anime/$slug/sources"

            httpClient.get(slugURL).parsed<List<Sources>>().forEach {
                responseList[it.number.toString()] = Episode(number = it.number.toString(), link = it.source)
            }
            logger("Twist Response Episodes : $responseList")
        } catch (e: Exception) {
            logError(e)
        }
        return responseList
    }

    override fun saveSource(source: Source, id: Int, selected: Boolean) {
        super.saveSource(source, id, selected)
        saveData("twist_$id", source)
    }

    companion object {
        private var host: Map<Int,Source>? = null
        suspend fun getSearchData(): Map<Int,Source> {
            host =
                if (host != null) host ?: mapOf()
                else {
                    httpClient.get("https://api.twist.moe/api/anime").parsed<ArrayList<ResponseElement>>().associate {
                        it.malID to Source(
                            it.slug.slug,
                            it.title,
                            "https://s4.anilist.co/file/anilistcdn/media/anime/cover/medium/default.jpg"
                        )
                    }
                }
            return host ?: mapOf()
        }
    }

    private data class ResponseElement (
        val id: Int,
        val title: String,
        @JsonProperty("alt_title")
        val altTitle: String? = null,
        val hidden: Int?,
        @JsonProperty("mal_id")
        val malID: Int,
        val slug: Slug
    ) {
        data class Slug(
            val slug: String
        )
    }

    data class Sources (
        val source: String,
        val number: Int
    )
}


