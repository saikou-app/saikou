package ani.saikou.app.anime

import android.content.Context
import com.google.android.exoplayer2.database.StandaloneDatabaseProvider
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import java.io.File

object VideoCache {
    private var simpleCache: SimpleCache? = null
    fun getInstance(context: Context): SimpleCache {
        val databaseProvider = StandaloneDatabaseProvider(context)
        if (simpleCache == null)
            simpleCache = SimpleCache(
                File(
                    context.cacheDir,
                    "exoplayer"
                ).also { it.deleteOnExit() }, // Ensures always fresh file
                LeastRecentlyUsedCacheEvictor(300L * 1024L * 1024L),
                databaseProvider
            )
        return simpleCache as SimpleCache
    }

    fun release() {
        simpleCache?.release()
        simpleCache = null
    }
}