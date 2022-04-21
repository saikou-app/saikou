package ani.saikou.anime.newsrc

import ani.saikou.anime.Episode
import ani.saikou.media.Media
import ani.saikou.media.Source
import ani.saikou.media.source.IMediaProvider

/**
 * Abstract provider for anime, also known as an Anime Parser.
 */
abstract class AnimeProvider : IMediaProvider {
    /**
     * @inheritDoc
     */
    override val mediaType: Media.Type
        get() = Media.Type.ANIME

    /**
     * Which dubbing languages are supported by this provider.
     */
    val dubbedLanguages: List<String>
        get() = emptyList()

    /**
     * The current provider status.
     */
    var status: String = ""

    /**
     * A status listener.
     */
    var statusListener: (String) -> Unit = {}

    /**
     * Get the list of available [VideoServer][Episode.VideoServer]s for the episode.
     *
     * @param episode The [Episode] to get the [VideoServer][Episode.VideoServer]s for.
     *
     * @return Map of [VideoServer][Episode.VideoServer]s to their server name.
     */
    abstract fun fetchVideoServers(episode: Episode): Map<String, Episode.VideoServer>

    /**
     * Get the list of available [VideoServer][Episode.VideoServer]s
     * for the episode from the provided server.
     *
     * @param episode The [Episode] to get the [VideoServer][Episode.VideoServer]s for.
     * @param server The server to get the [VideoServer][Episode.VideoServer]s from.
     *
     * @return Map of [VideoServer][Episode.VideoServer]s to their server name.
     */
    abstract fun fetchVideoServer(episode: Episode, server: String): Map<String, Episode.VideoServer>

    /**
     * Puts the list of available [VideoServer][Episode.VideoServer]s for the episode.
     *
     * @param episode The [Episode] to get the servers for.
     *
     * @return The [Episode], modified with the list of [VideoServer][Episode.VideoServer]s.
     */
    @Deprecated(
        "Use findUsableServers instead",
        ReplaceWith("episode.videoServers = fetchVideoServers(episode)")
    )
    fun getStreams(episode: Episode): Episode = episode.apply {
        videoServers = fetchVideoServers(this)
    }

    /**
     * Get the episode list for the given [Media].
     *
     * @param media The media to get the episode list for.
     *
     * @return Map of [Episode]s to their Number, as a [String].
     */
    abstract fun getEpisodes(media: Media): Map<String, Episode>

    /**
     * Get the list of available [Episode]s for the given [animeId].
     *
     * @param animeId The anime id.
     *
     * @return Map of [Episode]s to their Number, as a [String].
     */
    abstract fun getEpisodes(animeId: String): Map<String, Episode>

    /**
     * Searches the provider for the given query.
     *
     * @param string The query to search for.
     *
     * @return A list of sources.
     */
    abstract fun search(string: String): List<Source>

    /**
     * Resolves the [VideoServer][Episode.VideoServer] from the provided link.
     *
     * @param serverName The name of the server.
     * @param url The URL to resolve, as a [String].
     * @param fetchSize Whether to fetch the filesize of the videos.
     *
     * @return The resolved [VideoServer][Episode.VideoServer].
     */
    protected fun resolveVideoServer(serverName: String, url: String, fetchSize: Boolean = true): Episode.VideoServer? {
        val extractor = AnimeExtractorRegistry.findFor(url)
        val videoServer = extractor?.resolveServer(serverName, url, fetchSize)
        if (videoServer != null && videoServer.videoQuality.isNotEmpty()) {
            return videoServer
        }
        return null
    }

    protected open fun saveSource(source: Source, id: Int, selected: Boolean = true) {
        updateStatus("${if (selected) "Selected" else "Found"} : ${source.name}")
    }

    protected fun updateStatus(string: String) {
        status = string
        statusListener.invoke(string)
    }

}