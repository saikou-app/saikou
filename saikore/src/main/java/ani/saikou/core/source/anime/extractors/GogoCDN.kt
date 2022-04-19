package ani.saikou.core.source.anime.extractors

import android.net.Uri
import android.util.Base64
import ani.saikou.core.model.anime.Episode
import ani.saikou.core.service.LOG
import ani.saikou.core.source.anime.Extractor
import ani.saikou.core.util.extension.asJsoup
import ani.saikou.core.util.extension.findBetween
import ani.saikou.core.util.getSize
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Connection
import org.jsoup.Jsoup
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class GogoCDN(val host: String) : Extractor() {
    override fun getStreamLinks(name: String, url: String): Episode.StreamLinks {
        val list = arrayListOf<Episode.Quality>()
        try {
            val response = Jsoup.connect(url)
                .ignoreContentType(true)
                .ignoreHttpErrors(true)
                .get()
            if (url.contains("streaming.php")) {
                fetchKeys(url)?.apply {
                    val keys = this
                    response.select("script[data-name=\"episode\"]").attr("data-value").also {
                        val decrypted =
                            cryptoHandler(it, keys.first, keys.third, false)!!.replace("\t", "")
                        val id = decrypted.findBetween("", "&")!!
                        val end = decrypted.substringAfter(id)
                        Jsoup.connect(
                            "https://${Uri.parse(url).host}/encrypt-ajax.php?id=${
                                cryptoHandler(
                                    id,
                                    keys.first,
                                    keys.third,
                                    true
                                )
                            }$end&alias=$id"
                        )
                            .ignoreHttpErrors(true).ignoreContentType(true)
                            .header("X-Requested-With", "XMLHttpRequest").header("referer", host)
                            .get().body().toString().apply {
                                cryptoHandler(
                                    this.findBetween("""{"data":"""", "\"}")!!,
                                    keys.second,
                                    keys.third,
                                    false
                                )!!
                                    .replace("""o"<P{#meme":""", """e":[{"file":""").apply {
                                        val json =
                                            this.dropLast(this.length - this.lastIndexOf('}') - 1)
                                        val a = arrayListOf<Deferred<*>>()
                                        runBlocking {
                                            fun add(i: JsonElement, backup: Boolean) {
                                                a.add(async {
                                                    val label =
                                                        i.jsonObject["label"].toString().lowercase()
                                                            .trim('"')
                                                    val fileURL =
                                                        i.jsonObject["file"].toString().trim('"')
                                                    if (label != "auto p" && label != "hls p") {
                                                        if (label != "auto") list.add(
                                                            Episode.Quality(
                                                                fileURL,
                                                                label.replace(" ", ""),
                                                                if (!backup) getSize(
                                                                    fileURL,
                                                                    mutableMapOf("referer" to url)
                                                                ) else null,
                                                                if (backup) "Backup" else null
                                                            )
                                                        ) else null
                                                    } else list.add(
                                                        Episode.Quality(
                                                            fileURL,
                                                            "Multi Quality",
                                                            null,
                                                            if (backup) "Backup" else null
                                                        )
                                                    )
                                                })
                                            }
                                            Json.decodeFromString<JsonObject>(json).apply {
                                                jsonObject["source"]!!.jsonArray.forEach { i ->
                                                    add(i, false)
                                                }
                                                jsonObject["source_bk"]?.jsonArray?.forEach { i ->
                                                    add(i, true)
                                                }
                                            }
                                            a.awaitAll()
                                        }
                                    }
                            }
                    }
                }
            } else if (url.contains("embedplus")) {
                val fileURL = response.toString().findBetween("sources:[{file: '", "',")
                if (fileURL != null && try {
                        Jsoup.connect(fileURL).method(Connection.Method.HEAD).execute();true
                    } catch (e: Exception) {
                        false
                    }
                ) {
                    list.add(
                        Episode.Quality(
                            fileURL,
                            "Multi Quality",
                            null
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return Episode.StreamLinks(name, list, mutableMapOf("referer" to url))
    }

    //KR(animdl) lord & saviour
    private fun cryptoHandler(
        string: String,
        key: String,
        iv: String,
        encrypt: Boolean = true
    ): String? {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        return if (!encrypt) {
            cipher.init(
                Cipher.DECRYPT_MODE,
                SecretKeySpec(key.toByteArray(), "AES"),
                IvParameterSpec(iv.toByteArray())
            )
            String(cipher.doFinal(Base64.decode(string, Base64.NO_WRAP)))
        } else {
            cipher.init(
                Cipher.ENCRYPT_MODE,
                SecretKeySpec(key.toByteArray(), "AES"),
                IvParameterSpec(iv.toByteArray())
            )
            Base64.encodeToString(cipher.doFinal(string.toByteArray()), Base64.NO_WRAP)
        }
    }

    companion object {
        private val httpClient = OkHttpClient()

        /**
         * Fetches the decryption keys from the DOM.
         *
         * **See**: [jmir1/aniyomi-extensions][https://github.com/jmir1/aniyomi-extensions/blob/c62c6be6cba7b1645b939897520ebe07218fec4d/src/en/gogoanime/src/eu/kanade/tachiyomi/animeextension/en/gogoanime/extractors/GogoCdnExtractor.kt#L27]
         * @author jmir#9379
         *
         * @param url The URL to fetch the keys from.
         *
         * @return The decryption keys.
         */
        private fun fetchKeys(url: String): Triple<String, String, String>? {
            val request = Request.Builder().url(url).build()
            return httpClient.newCall(request).execute().let { response ->
                try {
                    val document = response.asJsoup()

                    val iv = document.select("div.wrapper")
                        .attr("class").substringAfter("container-")
                        .filter { it.isDigit() }
                    val secretKey = document.select("body[class]")
                        .attr("class").substringAfter("container-")
                        .filter { it.isDigit() }
                    val decryptionKey = document.select("div.videocontent")
                        .attr("class").substringAfter("videocontent-")
                        .filter { it.isDigit() }

                    Triple(secretKey, decryptionKey, iv)
                } catch (throwable: Throwable) {
                    LOG.notify(throwable)
                    null
                }
            }
        }
    }
}