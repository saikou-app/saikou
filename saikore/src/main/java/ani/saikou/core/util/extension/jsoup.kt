package ani.saikou.core.util.extension

import okhttp3.Response
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

/**
 * Parses the response body as a [Document].
 *
 * **From**: [tachiyomi/JsoupExtensions.kt][https://github.com/tachiyomiorg/tachiyomi/blob/3c41a5e91042c260641c7fe03f010fc9b98a60f4/app/src/main/java/eu/kanade/tachiyomi/util/JsoupExtensions.kt]
 *
 * @param html the body of the response. Use only if the body was read before calling this method.
 *
 * @return a Jsoup [Document] for this response.
 */
fun Response.asJsoup(html: String? = null): Document {
    return Jsoup.parse(html ?: body!!.string(), request.url.toString())
}