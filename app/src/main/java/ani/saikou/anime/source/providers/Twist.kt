package ani.saikou.anime.source.providers


import ani.saikou.*
import ani.saikou.anime.Episode
import ani.saikou.anime.newsrc.AnimeProvider
import ani.saikou.media.Media
import ani.saikou.media.Source
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonObject
import org.jsoup.Jsoup
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object Twist : AnimeProvider() {
    override val name: String = "twist.moe"
    override val isAdult: Boolean = false

    override fun fetchVideoServer(episode: Episode, server: String): Map<String, Episode.VideoServer> =
        fetchVideoServers(episode)

    override fun fetchVideoServers(episode: Episode): Map<String, Episode.VideoServer> {
        try {
            val url = Json.decodeFromString<JsonArray>(
                Jsoup.connect(episode.link!!).ignoreContentType(true).get().body().text()
            )[episode.number.toInt() - 1].jsonObject["source"].toString().trim('"')
            return mapOf(
                "Twist" to
                        Episode.VideoServer(
                            "Twist",
                            listOf(
                                Episode.VideoQuality(
                                    videoUrl = "https://cdn.twist.moe${TwistDecoder.decryptSource(url)}",
                                    quality = "Default Quality",
                                    size = 0
                                )
                            ),
                            mutableMapOf("referer" to "https://twist.moe/")
                        )
            )
        } catch (e: Exception) {
            toastString(e.toString())
        }
        return emptyMap()
    }

    override fun getEpisodes(media: Media): MutableMap<String, Episode> {
        val load: Source? = loadData("twist_${media.id}")
        if (load != null) {
            updateStatus("Selected : ${load.name}")
            return getEpisodes(load.link)
        }
        try {
            val animeJson = Jsoup.connect("https://api.twist.moe/api/anime").ignoreContentType(true).get().body().text()
            if (media.idMAL != null) {
                val id =
                    Regex(""""mal_id": ${media.idMAL},(.|\n)+?"slug": "(.+?)"""").find(animeJson)?.destructured?.component2()
                logger("Twist : Loaded : $id")

                return if (id != null) {
                    updateStatus("Selected : ${media.userPreferredName}")
                    getEpisodes(id)
                } else {
                    val result = search(media.nameRomaji)[0]
                    updateStatus("Found : ${result.name}")
                    saveSource(result, media.id, false)
                    getEpisodes(result.link)
                }
            }
        } catch (e: Exception) {
            toastString(e.toString())
        }
        return mutableMapOf()
    }

    override fun search(string: String): ArrayList<Source> {
        val arr = arrayListOf<Source>()
        try {
            Json.decodeFromString<JsonArray>(
                Jsoup.connect("https://api.twist.moe/api/anime").ignoreContentType(true).get().body().text()
            ).forEach {
                arr.add(
                    Source(
                        it.jsonObject["slug"]!!.jsonObject["slug"].toString().trim('"'),
                        it.jsonObject["title"].toString().trim('"'),
                        "https://s4.anilist.co/file/anilistcdn/media/anime/cover/medium/default.jpg"
                    )
                )
            }
            arr.sortByTitle(string)
        } catch (e: Exception) {
            toastString(e.toString())
        }
        return arr
    }

    override fun getEpisodes(animeId: String): MutableMap<String, Episode> {
        val responseList = mutableMapOf<String, Episode>()
        try {
            val slugURL = "https://api.twist.moe/api/anime/$animeId/sources"
            val nbEp = Json.decodeFromString<JsonArray>(
                Jsoup.connect(slugURL).ignoreContentType(true).get().body().text()
            ).size
            (1..nbEp).forEach {
                responseList[it.toString()] = Episode(number = it.toString(), link = slugURL)
            }
            logger("Twist Response Episodes : $responseList")
        } catch (e: Exception) {
            toastString(e.toString())
        }
        return responseList
    }

    override fun saveSource(source: Source, id: Int, selected: Boolean) {
        super.saveSource(source, id, selected)
        saveData("twist_$id", source)
    }

    object TwistDecoder {
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

        fun decryptSource(input: String): String {
            return decryptSourceUrl(generateKey(base64decode(input).copyOfRange(8, 16)), input)
        }
    }
}


