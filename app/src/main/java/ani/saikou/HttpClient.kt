package ani.saikou

import com.lagradost.nicehttp.Requests
import okhttp3.OkHttpClient

val defaultHeaders = mapOf(
    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/98.0.4758.102 Safari/537.36"
)
val okHttpClient = OkHttpClient()
val httpClient = Requests(okHttpClient, defaultHeaders)