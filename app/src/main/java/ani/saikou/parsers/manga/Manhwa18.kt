package ani.saikou.parsers.manga

import androidx.core.text.isDigitsOnly
import ani.saikou.FileUrl
import ani.saikou.client
import ani.saikou.media.Media
import ani.saikou.parsers.MangaChapter
import ani.saikou.parsers.MangaImage
import ani.saikou.parsers.MangaParser
import ani.saikou.parsers.ShowResponse


class Manhwa18 : MangaParser() {
    override val name = "Manhwa18"
    override val saveName = "manhwa_18"
    override val hostUrl = "https://manhwa18.cc/"
    override val isNSFW = true

    override suspend fun loadChapters(mangaLink: String, extra: Map<String, String>?): List<MangaChapter> {
        return client.get(mangaLink).document.select("#chapterlist > ul > li > a").reversed().map {
            val chap = it.text().replace("Chapter ", "")
            MangaChapter(chap, hostUrl + it.attr("href"))
        }
    }

    override suspend fun loadImages(chapterLink: String): List<MangaImage> {
        return client.get(chapterLink).document.select("div.read-content.wleft.tcenter > img").map {
            MangaImage(FileUrl(it.attr("src"),
                mapOf("referer" to "https://manhwa18.cc/",
                    "authority" to "img01.iwa-18cc.xyz",
                    "user-agent" to "Mozilla/5.0 (iPhone; CPU iPhone OS 13_2_3 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/13.0.3 Mobile/15E148 Safari/604.1")))
        }
    }

    override suspend fun search(query: String): List<ShowResponse> {
        val link = "$hostUrl/search?q=${encode(query.replace("'", ""))}"
        return client.get(link).document.select(".manga-item").map {
            ShowResponse(
                it.select(".manga-item > div > div.data > h3").text(),
                hostUrl + it.select(".manga-item .thumb > a").attr("href"),
                it.select("img").attr("src")
            )
        }
    }

    override suspend fun autoSearch(mediaObj: Media): ShowResponse? {
        var response = loadSavedShowResponse(mediaObj.id)
        if (response != null) {
            saveShowResponse(mediaObj.id, response, true)
        } else {
            setUserText("Searching : ${mediaObj.mangaName()}")
            response = search(mediaObj.mangaName()).let { if (it.isNotEmpty()) it[0] else null }

            if (response == null) {
                setUserText("Searching : ${mediaObj.nameRomaji}")
                response = search(mediaObj.nameRomaji).let { if (it.isNotEmpty()) it[0] else null }
            }
            if (response == null){
                for (it in mediaObj.synonyms){
                    setUserText("Searching : ${it}")
                    response = search(it).let { if (it.isNotEmpty()) it[0] else null }
                    if (response !=null)break
                }
            }
            saveShowResponse(mediaObj.id, response)
        }
        return response
    }
}