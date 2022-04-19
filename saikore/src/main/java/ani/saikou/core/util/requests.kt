package ani.saikou.core.util

import org.jsoup.Connection
import org.jsoup.Jsoup

fun getSize(url: String, headers: MutableMap<String, String>? = null): Double? {
    return try {
        Jsoup.connect(url)
            .ignoreContentType(true)
            .ignoreHttpErrors(true).timeout(1000)
            .followRedirects(true)
            .headers(headers ?: mutableMapOf())
            .method(Connection.Method.HEAD)
            .execute().header("Content-Length")?.toDouble()?.div(1048576)
    } catch (e: Exception) {
        null
    }
}