package ani.saikou.tv

import android.content.ComponentName
import android.os.Bundle
import android.support.v4.media.session.MediaSessionCompat
import android.view.View
import androidx.fragment.app.viewModels
import androidx.leanback.app.VideoSupportFragment
import androidx.leanback.app.VideoSupportFragmentGlueHost
import androidx.leanback.media.PlaybackGlue
import androidx.media.session.MediaButtonReceiver
import ani.saikou.anime.Episode
import ani.saikou.anime.VideoCache
import ani.saikou.anime.source.AnimeSources
import ani.saikou.anime.source.HAnimeSources
import ani.saikou.ignoreAllSSLErrors
import ani.saikou.loadData
import ani.saikou.media.Media
import ani.saikou.media.MediaDetailsViewModel
import ani.saikou.saveData
import ani.saikou.settings.PlayerSettings
import ani.saikou.settings.UserInterfaceSettings
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.ext.leanback.LeanbackPlayerAdapter
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ext.okhttp.OkHttpDataSource
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.HttpDataSource
import com.google.android.exoplayer2.upstream.cache.CacheDataSource
import okhttp3.OkHttpClient

class TVMediaPlayer: VideoSupportFragment(), VideoPlayerGlue.OnActionClickedListener {

    private lateinit var media: Media
    private lateinit var exoPlayer: ExoPlayer
    private lateinit var cacheFactory : CacheDataSource.Factory
    private lateinit var playbackParameters: PlaybackParameters
    private lateinit var trackSelector: DefaultTrackSelector
    private lateinit var mediaItem : MediaItem

    private lateinit var episode: Episode
    private lateinit var episodes: MutableMap<String,Episode>
    private lateinit var playerGlue : VideoPlayerGlue

    private val model: MediaDetailsViewModel by viewModels()

    private var playbackPosition: Long = 0

    private var settings = PlayerSettings()
    private var uiSettings = UserInterfaceSettings()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        settings = loadData("player_settings") ?: PlayerSettings().apply { saveData("player_settings",this) }
        uiSettings = loadData("ui_settings") ?: UserInterfaceSettings().apply { saveData("ui_settings",this) }

        val episodeObserverRunnable = Runnable {
            model.getEpisode().observe(this) {
                if (it != null) {
                    episode = it
                    media.selected = model.loadSelected(media)
                    model.setMedia(media)


                    val stream = episode.streamLinks[episode.selectedStream]
                    val url = stream?.quality?.get(episode.selectedQuality)
                    mediaItem =  MediaItem.Builder().setUri(url?.url).build()
                    initVideo()
                }
            }
        }

        episodeObserverRunnable.run()

        //Handle Media
        media = requireActivity().intent.getSerializableExtra("media")!! as Media
        model.setMedia(media)
        episodes = media.anime!!.episodes!!

        model.watchAnimeWatchSources = if(media.isAdult) HAnimeSources else AnimeSources

        //Set Episode, to invoke getEpisode() at Start
        model.setEpisode(episodes[media.anime!!.selectedEpisode!!]!!,"invoke")

    }

    fun initVideo() {

        val simpleCache = VideoCache.getInstance(requireContext())

        val stream = episode.streamLinks[episode.selectedStream]?: return

        val httpClient = OkHttpClient().newBuilder().ignoreAllSSLErrors().apply {
            followRedirects(true)
            followSslRedirects(true)
        }.build()
        val dataSourceFactory = DataSource.Factory {
            val dataSource: HttpDataSource = OkHttpDataSource.Factory(httpClient).createDataSource()
            if(stream.headers!=null)
                stream.headers.forEach {
                    dataSource.setRequestProperty(it.key, it.value)
                }
            dataSource
        }
        cacheFactory = CacheDataSource.Factory().apply {
            setCache(simpleCache)
            setUpstreamDataSourceFactory(dataSourceFactory)
        }
        trackSelector = DefaultTrackSelector(requireContext())
        trackSelector.setParameters(
            trackSelector.buildUponParameters()
                .setMinVideoSize(
                    loadData("maxWidth",requireActivity()) ?:720, loadData("maxHeight",requireActivity())
                        ?:480)
                .setMaxVideoSize(1,1)
        )


        //Speed
        val speeds     =  if(settings.cursedSpeeds) arrayOf(1f , 1.25f , 1.5f , 1.75f , 2f , 2.5f , 3f , 4f, 5f , 10f , 25f, 50f) else arrayOf( 0.25f , 0.33f , 0.5f , 0.66f , 0.75f , 1f , 1.25f , 1.33f , 1.5f , 1.66f , 1.75f , 2f )
        val speedsName = speeds.map { "${it}x" }.toTypedArray()
        var curSpeed   = loadData("${media.id}_speed",requireActivity()) ?:settings.defaultSpeed

        playbackParameters = PlaybackParameters(speeds[curSpeed])

        exoPlayer = ExoPlayer.Builder(requireContext())
            .setMediaSourceFactory(DefaultMediaSourceFactory(cacheFactory))
            .setTrackSelector(trackSelector)
            .build().apply {
                playWhenReady = true
                this.playbackParameters = this@TVMediaPlayer.playbackParameters
                setMediaItem(mediaItem)
                prepare()
                seekTo(playbackPosition)
            }

        val mediaButtonReceiver = ComponentName(requireActivity(), MediaButtonReceiver::class.java)
        MediaSessionCompat(requireContext(), "Player", mediaButtonReceiver, null).let { media ->
            val mediaSessionConnector = MediaSessionConnector(media)
            mediaSessionConnector.setPlayer(exoPlayer)
            media.isActive = true
        }

        playerGlue = VideoPlayerGlue(requireActivity(), LeanbackPlayerAdapter(requireActivity(), exoPlayer, 16), this)
        playerGlue.host = VideoSupportFragmentGlueHost(this)
        playerGlue.addPlayerCallback(object : PlaybackGlue.PlayerCallback() {
            override fun onPreparedStateChanged(glue: PlaybackGlue) {
                if (glue.isPrepared()) {
                    playerGlue.play()
                }
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        exoPlayer.release()
        VideoCache.release()
    }

    override fun onPause() {
        super.onPause()
        exoPlayer.pause()
    }
    override fun onPrevious() {}

    override fun onNext() {}
}