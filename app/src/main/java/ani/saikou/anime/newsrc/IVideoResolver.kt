package ani.saikou.anime.newsrc

import ani.saikou.Named
import ani.saikou.anime.Episode

/**
 * Extracts proper links to anime content.
 */
interface IVideoResolver: Named {
    /**
     * Resolves the [VideoServer][Episode.VideoServer] from the provided link.
     *
     * @param serverName The name of the server.
     * @param url The URL to resolve, as a [String].
     * @param fetchSize Whether to fetch the filesize of the videos.
     *
     * @return The resolved [VideoServer][Episode.VideoServer].
     */
    fun resolveServer(serverName: String, url: String, fetchSize: Boolean): Episode.VideoServer

    /**
     * Whether this extractor supports the provided link.
     *
     * @param url The URL to check.
     *
     * @return Whether this extractor supports the provided link.
     */
    fun canResolve(url: String): Boolean

    /**
     * Finds the domain of the provided URL.
     *
     * @param url The URL to find the domain of, as a [String].
     *
     * @return The domain of the provided URL.
     */
    fun findDomain(url: String): String =
        AnimeExtractorRegistry.findDomain(url).lowercase()
}