package ani.saikou

import com.lagradost.nicehttp.Requests
import okhttp3.OkHttpClient

val okHttpClient = OkHttpClient()
val httpClient = Requests(okHttpClient)