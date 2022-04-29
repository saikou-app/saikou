package ani.saikou.anime.source.extractors

import android.net.Uri
import android.util.Base64
import ani.saikou.anime.Episode
import ani.saikou.anime.source.Extractor
import ani.saikou.findBetween
import ani.saikou.getSize
import ani.saikou.httpClient
import ani.saikou.others.asyncEach
import ani.saikou.others.logError
import ani.saikou.toastString
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.readValue
import com.lagradost.nicehttp.Requests
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class GogoCDN(val host: String) : Extractor() {
    override suspend fun getStreamLinks(name: String, url: String): Episode.StreamLinks {
        val list = arrayListOf<Episode.Quality>()
        try {
            val response = httpClient.get(url).document
            if (url.contains("streaming.php")) {
                getKeysAndIv()?.apply {
                    val keys = this
                    response.select("script[data-name=\"episode\"]").attr("data-value").also {
                        val decrypted = cryptoHandler(it, keys.key, keys.iv, false)!!.replace("\t", "")
                        val id = decrypted.findBetween("", "&")!!
                        val end = decrypted.substringAfter(id)

                        val encryptedUrl = "https://${Uri.parse(url).host}/encrypt-ajax.php?id=${
                            cryptoHandler(id, keys.key, keys.iv, true)
                        }$end&alias=$id"
                        val encrypted = httpClient.get(
                            encryptedUrl,
                            mapOf("X-Requested-With" to "XMLHttpRequest"),
                            host
                        ).text.findBetween("""{"data":"""", "\"}")!!

                        val jumbledJson = cryptoHandler(encrypted, keys.secondKey, keys.iv, false)!!
                            .replace("""o"<P{#meme":""", """e":[{"file":""")
                        jumbledJson.apply {
                            val json = Requests.mapper.readValue<SourceResponse>(dropLast(length - lastIndexOf('}') - 1))
                            suspend fun add(i: SourceResponse.Source, backup: Boolean) {
                                val label = i.label?.lowercase() ?: return
                                val fileURL = i.file ?: return
                                if (label != "auto p" && label != "hls p") {
                                    list.add(
                                        Episode.Quality(
                                            fileURL,
                                            label.replace(" ", ""),
                                            if (!backup) getSize(
                                                fileURL,
                                                mutableMapOf("referer" to url)
                                            ) else null,
                                            if (backup) "Backup" else null
                                        )
                                    )
                                } else list.add(
                                    Episode.Quality(
                                        fileURL,
                                        "Multi Quality",
                                        null,
                                        if (backup) "Backup" else null
                                    )
                                )
                            }
                            json.source?.asyncEach { i ->
                                add(i, false)
                            }
                            json.sourceBk?.asyncEach { i ->
                                add(i, true)
                            }
                        }
                    }
                }
            } else if (url.contains("embedplus")) {
                val fileURL = response.toString().findBetween("sources:[{file: '", "',")
                if (fileURL != null && try {
                        httpClient.head(fileURL);true
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
            logError(e)
        }
        return Episode.StreamLinks(name, list, mutableMapOf("referer" to url))
    }

    //KR(animdl) lord & saviour
    private fun cryptoHandler(string: String, key: String, iv: String, encrypt: Boolean = true): String? {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        return if (!encrypt) {
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key.toByteArray(), "AES"), IvParameterSpec(iv.toByteArray()))
            String(cipher.doFinal(Base64.decode(string, Base64.NO_WRAP)))
        } else {
            cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key.toByteArray(), "AES"), IvParameterSpec(iv.toByteArray()))
            Base64.encodeToString(cipher.doFinal(string.toByteArray()), Base64.NO_WRAP)
        }
    }

    companion object {
        private var keysAndIv: Keys? = null
        private suspend fun getKeysAndIv(): Keys? {
            keysAndIv = keysAndIv
                ?: httpClient.get("https://raw.githubusercontent.com/justfoolingaround/animdl-provider-benchmarks/master/api/gogoanime.json")
                    .parsed()
            return keysAndIv
        }
    }

    private data class Keys(
        val key: String,
        @JsonProperty("second_key")
        val secondKey: String,
        val iv: String
    )

    private data class SourceResponse(
        val source: List<Source>? = null,
        @JsonProperty("source_bk")
        val sourceBk: List<Source>? = null
    ) {
        data class Source(
            val file: String? = null,
            val label: String? = null,
            val type: String? = null
        )
    }


}