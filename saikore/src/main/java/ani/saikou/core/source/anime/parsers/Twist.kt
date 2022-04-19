package ani.saikou.core.source.anime.parsers

import ani.saikou.core.model.anime.Episode
import ani.saikou.core.model.media.Media
import ani.saikou.core.model.media.Source
import ani.saikou.core.service.LOG
import ani.saikou.core.service.STORE
import ani.saikou.core.source.anime.AnimeParser
import ani.saikou.core.util.extension.sortByTitle
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

class Twist(override val name: String = "twist.moe") : AnimeParser() {

    object DecodeTwistSources {
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

    override fun getStream(episode: Episode, server: String): Episode {
        return getStreams(episode)
    }

    override fun getStreams(episode: Episode): Episode {
        try {
            val url = Json.decodeFromString<JsonArray>(
                Jsoup.connect(episode.link!!).ignoreContentType(true).get().body().text()
            )[episode.number.toInt() - 1].jsonObject["source"].toString().trim('"')
            episode.streamLinks = mutableMapOf(
                "Twist" to
                        Episode.StreamLinks(
                            "Twist",
                            listOf(
                                Episode.Quality(
                                    url = "https://cdn.twist.moe${
                                        DecodeTwistSources.decryptSource(
                                            url
                                        )
                                    }",
                                    quality = "Default Quality",
                                    size = 0.0
                                )
                            ),
                            mutableMapOf("referer" to "https://twist.moe/")
                        )
            )
        } catch (e: Exception) {
            LOG.notify(e)
        }
        return episode
    }

    override fun getEpisodes(media: Media): MutableMap<String, Episode> {
        val load: Source? = STORE.loadData("twist_${media.id}")
        if (load != null) {
            setTextListener("Selected : ${load.name}")
            return getSlugEpisodes(load.link)
        }
        try {
            val animeJson =
                Jsoup.connect("https://api.twist.moe/api/anime").ignoreContentType(true).get()
                    .body().text()
            if (media.idMAL != null) {
                val slug =
                    Regex(""""mal_id": ${media.idMAL},(.|\n)+?"slug": "(.+?)"""").find(animeJson)?.destructured?.component2()
                LOG.log("Twist : Loaded : $slug")
                return if (slug != null) {
                    setTextListener("Selected : ${media.userPreferredName}")
                    getSlugEpisodes(slug)
                } else {
                    val result = search(media.nameRomaji)[0]
                    setTextListener("Found : ${result.name}")
                    saveSource(result, media.id, false)
                    getSlugEpisodes(result.link)
                }
            }
        } catch (e: Exception) {
            LOG.notify(e)
        }
        return mutableMapOf()
    }

    override fun search(string: String): ArrayList<Source> {
        val arr = arrayListOf<Source>()
        try {
            Json.decodeFromString<JsonArray>(
                Jsoup.connect("https://api.twist.moe/api/anime").ignoreContentType(true).get()
                    .body().text()
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
            LOG.notify(e)
        }
        return arr
    }

    override fun getSlugEpisodes(slug: String): MutableMap<String, Episode> {
        val responseList = mutableMapOf<String, Episode>()
        try {
            val slugURL = "https://api.twist.moe/api/anime/$slug/sources"
            (1..Json.decodeFromString<JsonArray>(
                Jsoup.connect(slugURL).ignoreContentType(true).get().body().text()
            ).size).forEach {
                responseList[it.toString()] = Episode(number = it.toString(), link = slugURL)
            }
            LOG.log("Twist Response Episodes : $responseList")
        } catch (e: Exception) {
            LOG.notify(e)
        }
        return responseList
    }

    override fun saveSource(source: Source, id: Int, selected: Boolean) {
        super.saveSource(source, id, selected)
        STORE.saveData("twist_$id", source)
    }
}


