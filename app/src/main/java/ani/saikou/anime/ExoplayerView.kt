package ani.saikou.anime

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.app.PictureInPictureParams
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.Animatable
import android.hardware.SensorManager
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings.System
import android.support.v4.media.session.MediaSessionCompat
import android.util.Rational
import android.util.TypedValue
import android.view.*
import android.view.animation.AnimationUtils
import android.widget.AdapterView
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.core.math.MathUtils.clamp
import androidx.core.view.WindowCompat
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import androidx.media.session.MediaButtonReceiver
import ani.saikou.*
import ani.saikou.anilist.Anilist
import ani.saikou.anime.source.AnimeSources
import ani.saikou.anime.source.HAnimeSources
import ani.saikou.databinding.ActivityExoplayerBinding
import ani.saikou.media.Media
import ani.saikou.media.MediaDetailsViewModel
import ani.saikou.settings.PlayerSettings
import ani.saikou.settings.UserInterfaceSettings
import com.bumptech.glide.Glide
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ext.okhttp.OkHttpDataSource
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.MappingTrackSelector
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.CaptionStyleCompat
import com.google.android.exoplayer2.ui.CaptionStyleCompat.EDGE_TYPE_OUTLINE
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.google.android.exoplayer2.ui.TrackSelectionDialogBuilder
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.HttpDataSource
import com.google.android.exoplayer2.upstream.cache.CacheDataSource
import com.google.android.exoplayer2.util.MimeTypes
import com.google.android.material.slider.Slider
import com.lagradost.nicehttp.ignoreAllSSLErrors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@SuppressLint("SetTextI18n", "ClickableViewAccessibility")
class ExoplayerView : AppCompatActivity(), Player.Listener {
    private val resumeWindow = "resumeWindow"
    private val resumePosition = "resumePosition"
    private val playerFullscreen = "playerFullscreen"
    private val playerOnPlay = "playerOnPlay"

    private lateinit var exoPlayer: ExoPlayer
    private lateinit var trackSelector: DefaultTrackSelector
    private lateinit var cacheFactory: CacheDataSource.Factory
    private lateinit var playbackParameters: PlaybackParameters
    private lateinit var mediaItem: MediaItem

    private lateinit var binding: ActivityExoplayerBinding
    private lateinit var playerView: StyledPlayerView
    private lateinit var exoPlay: ImageButton
    private lateinit var exoSource: ImageButton
    private lateinit var exoRotate: ImageButton
    private lateinit var exoQuality: ImageButton
    private lateinit var exoSpeed: ImageButton
    private lateinit var exoScreen: ImageButton
    private lateinit var exoNext: ImageButton
    private lateinit var exoPrev: ImageButton
    private lateinit var exoPip: ImageButton
    private lateinit var exoBrightness: Slider
    private lateinit var exoVolume: Slider
    private lateinit var exoBrightnessCont: View
    private lateinit var exoVolumeCont: View
    private lateinit var animeTitle: TextView
    private lateinit var videoInfo: TextView
    private lateinit var serverInfo: TextView
    private lateinit var episodeTitle: Spinner

    private var orientationListener: OrientationEventListener? = null

    private lateinit var media: Media
    private lateinit var episode: Episode
    private lateinit var episodes: MutableMap<String, Episode>
    private lateinit var episodeArr: List<String>
    private lateinit var episodeTitleArr: ArrayList<String>
    private var currentEpisodeIndex = 0
    private var epChanging = false

    private var notchHeight: Int = 0
    private var currentWindow = 0
    private var playbackPosition: Long = 0
    private var episodeLength: Float = 0f
    private var isFullscreen: Int = 0
    private var isInitialized = false
    private var isPlayerPlaying = true
    private var changingServer = false
    private var interacted = false

    private var pipEnabled = false
    private var aspectRatio = Rational(16, 9)

    private var t1 = Timer()
    private var t2 = Timer()

    private var settings = PlayerSettings()
    private var uiSettings = UserInterfaceSettings()

    private val handler = Handler(Looper.getMainLooper())
    private val model: MediaDetailsViewModel by viewModels()

    var rotation = 0

    override fun onAttachedToWindow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val displayCutout = window.decorView.rootWindowInsets.displayCutout
            if (displayCutout != null) {
                if (displayCutout.boundingRects.size > 0) {
                    notchHeight = min(displayCutout.boundingRects[0].width(), displayCutout.boundingRects[0].height())
                    checkNotch()
                }
            }
        }
        super.onAttachedToWindow()
    }

    private fun checkNotch() {
        if (notchHeight != 0) {
            val orientation = resources.configuration.orientation
            playerView.findViewById<View>(R.id.exo_controller_margin).updateLayoutParams<ViewGroup.MarginLayoutParams> {
                if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    marginStart = notchHeight
                    marginEnd = notchHeight
                    topMargin = 0
                } else {
                    topMargin = notchHeight
                    marginStart = 0
                    marginEnd = 0
                }
            }
            playerView.findViewById<View>(R.id.exo_buffering).translationY =
                (if (orientation == Configuration.ORIENTATION_LANDSCAPE) 0 else (notchHeight + 8f.px)).dp
            exoBrightnessCont.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                marginEnd = if (orientation == Configuration.ORIENTATION_LANDSCAPE) notchHeight else 0
            }
            exoVolumeCont.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                marginStart = if (orientation == Configuration.ORIENTATION_LANDSCAPE) notchHeight else 0
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExoplayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Initialize
        WindowCompat.setDecorFitsSystemWindows(window, false)
        hideSystemBars()

        settings = loadData("player_settings") ?: PlayerSettings().apply { saveData("player_settings", this) }
        uiSettings = loadData("ui_settings") ?: UserInterfaceSettings().apply { saveData("ui_settings", this) }

        playerView = findViewById(R.id.player_view)
        exoQuality = playerView.findViewById(R.id.exo_quality)
        exoPlay = playerView.findViewById(R.id.exo_play)
        exoSource = playerView.findViewById(R.id.exo_source)
        exoRotate = playerView.findViewById(R.id.exo_rotate)
        exoSpeed = playerView.findViewById(R.id.exo_playback_speed)
        exoScreen = playerView.findViewById(R.id.exo_screen)
        exoBrightness = playerView.findViewById(R.id.exo_brightness)
        exoVolume = playerView.findViewById(R.id.exo_volume)
        exoBrightnessCont = playerView.findViewById(R.id.exo_brightness_cont)
        exoVolumeCont = playerView.findViewById(R.id.exo_volume_cont)
        exoPip = playerView.findViewById(R.id.exo_pip)

        animeTitle = playerView.findViewById(R.id.exo_anime_title)
        episodeTitle = playerView.findViewById(R.id.exo_ep_sel)

        playerView.controllerShowTimeoutMs = 5000

        val audioManager = applicationContext.getSystemService(AUDIO_SERVICE) as AudioManager
        if (System.getInt(contentResolver, System.ACCELEROMETER_ROTATION, 0) != 1) {
            requestedOrientation = rotation
            exoRotate.setOnClickListener {
                requestedOrientation = rotation
                it.visibility = View.GONE
            }
            orientationListener = object : OrientationEventListener(this, SensorManager.SENSOR_DELAY_UI) {
                override fun onOrientationChanged(orientation: Int) {
                    if (orientation in 45..135) {
                        if (rotation != ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE) exoRotate.visibility = View.VISIBLE
                        rotation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
                    } else if (orientation in 225..315) {
                        if (rotation != ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) exoRotate.visibility = View.VISIBLE
                        rotation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    }
                }
            }
            orientationListener?.enable()
        }

        playerView.subtitleView?.setStyle(
            CaptionStyleCompat(
                Color.WHITE,
                Color.TRANSPARENT,
                Color.TRANSPARENT,
                EDGE_TYPE_OUTLINE,
                Color.BLACK,
                ResourcesCompat.getFont(this, R.font.poppins_bold)
            )
        )
        playerView.subtitleView?.setFixedTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)

        if (savedInstanceState != null) {
            currentWindow = savedInstanceState.getInt(resumeWindow)
            playbackPosition = savedInstanceState.getLong(resumePosition)
            isFullscreen = savedInstanceState.getInt(playerFullscreen)
            isPlayerPlaying = savedInstanceState.getBoolean(playerOnPlay)
        }

        //BackButton
        playerView.findViewById<ImageButton>(R.id.exo_back).setOnClickListener {
            onBackPressed()
        }

        //Play Pause
        exoPlay.setOnClickListener {
            if (isInitialized) {
                isPlayerPlaying = exoPlayer.isPlaying
                (exoPlay.drawable as Animatable?)?.start()
                if (isPlayerPlaying) {
                    Glide.with(this).load(R.drawable.anim_play_to_pause).into(exoPlay)
                    exoPlayer.pause()
                } else {
                    Glide.with(this).load(R.drawable.anim_pause_to_play).into(exoPlay)
                    exoPlayer.play()
                }
            }
        }

        // Picture-in-picture
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            pipEnabled = packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE) && settings.pip
            if (pipEnabled) {
                exoPip.visibility = View.VISIBLE
                exoPip.setOnClickListener {
                    enterPipMode()
                }
            } else exoPip.visibility = View.GONE
        }


        //Lock Button
        var locked = false
        val container = playerView.findViewById<View>(R.id.exo_controller_cont)
        val screen = playerView.findViewById<View>(R.id.exo_black_screen)
        val lockButton = playerView.findViewById<ImageButton>(R.id.exo_unlock)
        val timeline = playerView.findViewById<ExtendedTimeBar>(R.id.exo_progress)
        playerView.findViewById<ImageButton>(R.id.exo_lock).setOnClickListener {
            locked = true
            screen.visibility = View.GONE
            container.visibility = View.GONE
            lockButton.visibility = View.VISIBLE
            timeline.setForceDisabled(true)
        }
        lockButton.setOnClickListener {
            locked = false
            screen.visibility = View.VISIBLE
            container.visibility = View.VISIBLE
            it.visibility = View.GONE
            timeline.setForceDisabled(false)
        }

        //Skip Time Button
        if (settings.skipTime > 0) {
            playerView.findViewById<TextView>(R.id.exo_skip_time).text = settings.skipTime.toString()
            playerView.findViewById<View>(R.id.exo_skip).setOnClickListener {
                if (isInitialized)
                    exoPlayer.seekTo(exoPlayer.currentPosition + settings.skipTime * 1000)
            }
        } else {
            playerView.findViewById<View>(R.id.exo_skip).visibility = View.GONE
        }

        val gestureSpeed = (300 * uiSettings.animationSpeed).toLong()
        //Player UI Visibility Handler
        val brightnessRunnable = Runnable {
            if (exoBrightnessCont.alpha == 1f)
                lifecycleScope.launch {
                    ObjectAnimator.ofFloat(exoBrightnessCont, "alpha", 1f, 0f).setDuration(gestureSpeed).start()
                    delay(gestureSpeed)
                    exoBrightnessCont.visibility = View.GONE
                    checkNotch()
                }
        }
        val volumeRunnable = Runnable {
            if (exoVolumeCont.alpha == 1f)
                lifecycleScope.launch {
                    ObjectAnimator.ofFloat(exoVolumeCont, "alpha", 1f, 0f).setDuration(gestureSpeed).start()
                    delay(gestureSpeed)
                    exoVolumeCont.visibility = View.GONE
                    checkNotch()
                }
        }
        playerView.setControllerVisibilityListener {
            if (it == View.GONE) {
                hideSystemBars()
                brightnessRunnable.run()
                volumeRunnable.run()
            }
        }
        val overshoot = AnimationUtils.loadInterpolator(this, R.anim.over_shoot)
        val controllerDuration = (uiSettings.animationSpeed * 200).toLong()
        fun handleController() {
            if (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) !isInPictureInPictureMode else true) {
                if (playerView.isControllerFullyVisible) {
                    ObjectAnimator.ofFloat(playerView.findViewById(R.id.exo_controller), "alpha", 1f, 0f)
                        .setDuration(controllerDuration).start()
                    ObjectAnimator.ofFloat(playerView.findViewById(R.id.exo_bottom_cont), "translationY", 0f, 128f)
                        .apply { interpolator = overshoot;duration = controllerDuration;start() }
                    ObjectAnimator.ofFloat(playerView.findViewById(R.id.exo_timeline_cont), "translationY", 0f, 128f)
                        .apply { interpolator = overshoot;duration = controllerDuration;start() }
                    ObjectAnimator.ofFloat(playerView.findViewById(R.id.exo_top_cont), "translationY", 0f, -128f)
                        .apply { interpolator = overshoot;duration = controllerDuration;start() }
                    playerView.postDelayed({ playerView.hideController() }, controllerDuration)
                } else {
                    checkNotch()
                    playerView.showController()
                    ObjectAnimator.ofFloat(playerView.findViewById(R.id.exo_controller), "alpha", 0f, 1f)
                        .setDuration(controllerDuration).start()
                    ObjectAnimator.ofFloat(playerView.findViewById(R.id.exo_bottom_cont), "translationY", 128f, 0f)
                        .apply { interpolator = overshoot;duration = controllerDuration;start() }
                    ObjectAnimator.ofFloat(playerView.findViewById(R.id.exo_timeline_cont), "translationY", 128f, 0f)
                        .apply { interpolator = overshoot;duration = controllerDuration;start() }
                    ObjectAnimator.ofFloat(playerView.findViewById(R.id.exo_top_cont), "translationY", -128f, 0f)
                        .apply { interpolator = overshoot;duration = controllerDuration;start() }
                }
            }
        }

        playerView.findViewById<View>(R.id.exo_full_area).setOnClickListener {
            handleController()
        }

        val rewindText = playerView.findViewById<TextView>(R.id.exo_fast_rewind_anim)
        rewindText.text = "-${settings.seekTime}"
        val forwardText = playerView.findViewById<TextView>(R.id.exo_fast_forward_anim)
        forwardText.text = "+${settings.seekTime}"

        val fastForwardCard = playerView.findViewById<View>(R.id.exo_fast_forward)
        val fastRewindCard = playerView.findViewById<View>(R.id.exo_fast_rewind)

        //Screen Gestures
        if (settings.gestures || settings.doubleTap) {
            //Brightness
            var brightnessTimer = Timer()
            exoBrightnessCont.visibility = View.GONE

            fun brightnessHide() {
                brightnessTimer.cancel()
                brightnessTimer.purge()
                val timerTask: TimerTask = object : TimerTask() {
                    override fun run() {
                        handler.post(brightnessRunnable)
                    }
                }
                brightnessTimer = Timer()
                brightnessTimer.schedule(timerTask, 3000)
            }
            exoBrightness.value = (getCurrentBrightnessValue(this) * 10f)

            exoBrightness.addOnChangeListener { _, value, _ ->
                val lp = window.attributes
                lp.screenBrightness = brightnessConverter(value / 10, false)
                window.attributes = lp
                brightnessHide()
            }

            //FastRewind (Left Panel)

            val fastRewindDetector = GestureDetector(this, object : DoubleClickListener() {
                override fun onDoubleClick(event: MotionEvent?) {
                    if (!locked && isInitialized && settings.doubleTap) {
                        exoPlayer.seekTo(exoPlayer.currentPosition - settings.seekTime * 1000)
                        viewDoubleTapped(
                            fastRewindCard,
                            event,
                            rewindText
                        )
                    }
                }

                override fun onScrollYClick(y: Float) {
                    if (!locked && settings.gestures) {
                        exoBrightness.value = clamp(exoBrightness.value + y / 100, 0f, 10f)
                        if (exoBrightnessCont.visibility != View.VISIBLE) {
                            exoBrightnessCont.visibility = View.VISIBLE
                        }
                        exoBrightnessCont.alpha = 1f
                    }
                }

                override fun onSingleClick(event: MotionEvent?) = handleController()
            })
            val rewindArea = playerView.findViewById<View>(R.id.exo_rewind_area)
            rewindArea.isClickable = true
            rewindArea.setOnTouchListener { v, event ->
                fastRewindDetector.onTouchEvent(event)
                v.performClick()
                true
            }

            //Volume
            var volumeTimer = Timer()
            exoVolumeCont.visibility = View.GONE

            val volumeMax = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            exoVolume.value = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / volumeMax * 10
            fun volumeHide() {
                volumeTimer.cancel()
                volumeTimer.purge()
                val timerTask: TimerTask = object : TimerTask() {
                    override fun run() {
                        handler.post(volumeRunnable)
                    }
                }
                volumeTimer = Timer()
                volumeTimer.schedule(timerTask, 3000)
            }
            exoVolume.addOnChangeListener { _, value, _ ->
                val volume = (value / 10 * volumeMax).roundToInt()
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0)
                volumeHide()
            }

            //FastForward (Right Panel)

            val fastForwardDetector = GestureDetector(this, object : DoubleClickListener() {
                override fun onDoubleClick(event: MotionEvent?) {
                    if (!locked && isInitialized && settings.doubleTap) {
                        exoPlayer.seekTo(exoPlayer.currentPosition + settings.seekTime * 1000)
                        viewDoubleTapped(
                            fastForwardCard,
                            event,
                            forwardText
                        )
                    }
                }

                override fun onScrollYClick(y: Float) {
                    if (!locked && settings.gestures) {
                        exoVolume.value = clamp(exoVolume.value + y / 100, 0f, 10f)
                        if (exoVolumeCont.visibility != View.VISIBLE) {
                            exoVolumeCont.visibility = View.VISIBLE
                        }
                        exoVolumeCont.alpha = 1f
                    }
                }

                override fun onSingleClick(event: MotionEvent?) = handleController()
            })
            val forwardArea = playerView.findViewById<View>(R.id.exo_forward_area)
            forwardArea.isClickable = true
            forwardArea.setOnTouchListener { v, event ->
                fastForwardDetector.onTouchEvent(event)
                v.performClick()
                true
            }
        }

        if (!settings.doubleTap) {

            playerView.findViewById<View>(R.id.exo_fast_forward_button_cont).visibility = View.VISIBLE
            playerView.findViewById<View>(R.id.exo_fast_rewind_button_cont).visibility = View.VISIBLE
            playerView.findViewById<ImageButton>(R.id.exo_fast_forward_button).setOnClickListener {
                if (isInitialized) {
                    exoPlayer.seekTo(exoPlayer.currentPosition + settings.seekTime * 1000)
                    viewDoubleTapped(fastForwardCard, text = forwardText)
                }
            }
            playerView.findViewById<ImageButton>(R.id.exo_fast_rewind_button).setOnClickListener {
                if (isInitialized) {
                    exoPlayer.seekTo(exoPlayer.currentPosition - settings.seekTime * 1000)
                    viewDoubleTapped(fastRewindCard, text = rewindText)
                }
            }
        }

        //Handle Media
        try {
            media = (intent.getSerializableExtra("media") as? Media) ?: return
        } catch (e: Exception) {
            toast(e.toString())
            return
        }
        model.setMedia(media)
        title = media.userPreferredName
        episodes = media.anime?.episodes ?: return

        model.watchAnimeWatchSources = if (media.isAdult) HAnimeSources else AnimeSources

        videoInfo = playerView.findViewById(R.id.exo_video_info)
        serverInfo = playerView.findViewById(R.id.exo_server_info)

        if (!settings.videoInfo) {
            videoInfo.visibility = View.GONE
            serverInfo.visibility = View.GONE
        }
        serverInfo.text = model.watchAnimeWatchSources!!.names[media.selected!!.source]

        model.epChanged.observe(this) {
            epChanging = !it
        }

        //Anime Title
        animeTitle.text = media.userPreferredName

        episodeArr = episodes.keys.toList()
        currentEpisodeIndex = episodeArr.indexOf(media.anime!!.selectedEpisode!!)

        episodeTitleArr = arrayListOf()
        episodes.forEach {
            val episode = it.value
            episodeTitleArr.add("${if (!episode.title.isNullOrEmpty() && episode.title != "null") "" else "Episode "}${episode.number}${if (episode.filler) " [Filler]" else ""}${if (!episode.title.isNullOrEmpty() && episode.title != "null") " : " + episode.title else ""}")
        }

        //Episode Change
        fun change(index: Int) {
            if (isInitialized) {
                changingServer = false
                saveData(
                    "${media.id}_${episodeArr[currentEpisodeIndex]}",
                    exoPlayer.currentPosition,
                    this
                )
                val prev = episodeArr[currentEpisodeIndex]
                episodeLength = 0f
                media.anime!!.selectedEpisode = episodeArr[index]
                model.setMedia(media)
                model.epChanged.postValue(false)
                model.setEpisode(episodes[media.anime!!.selectedEpisode!!]!!, "change")
                model.onEpisodeClick(
                    media, media.anime!!.selectedEpisode!!, this.supportFragmentManager,
                    false,
                    prev
                )
            }
        }

        //EpisodeSelector
        episodeTitle.adapter = NoPaddingArrayAdapter(this, R.layout.item_dropdown, episodeTitleArr)
        episodeTitle.setSelection(currentEpisodeIndex)
        episodeTitle.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                if (position != currentEpisodeIndex) change(position)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        //Next Episode
        exoNext = playerView.findViewById(R.id.exo_next_ep)
        exoNext.setOnClickListener {
            if (isInitialized) {
                nextEpisode { i ->
                    updateAniProgress()
                    change(currentEpisodeIndex + i)
                }
            }
        }
        //Prev Episode
        exoPrev = playerView.findViewById(R.id.exo_prev_ep)
        exoPrev.setOnClickListener {
            if (currentEpisodeIndex > 0) {
                change(currentEpisodeIndex - 1)
            } else
                toastString("This is the 1st Episode!")
        }

        model.getEpisode().observe(this) {
            hideSystemBars()
            if (it != null && !epChanging) {
                episode = it
                media.selected = model.loadSelected(media)
                model.setMedia(media)
                currentEpisodeIndex = episodeArr.indexOf(it.number)
                episodeTitle.setSelection(currentEpisodeIndex)
                if (isInitialized) releasePlayer()
                playbackPosition = loadData("${media.id}_${it.number}", this) ?: 0
                initPlayer()
                preloading = false
                updateProgress()
            }
        }

        //FullScreen
        isFullscreen = loadData("${media.id}_fullscreenInt", this) ?: isFullscreen
        playerView.resizeMode = when (isFullscreen) {
            0    -> AspectRatioFrameLayout.RESIZE_MODE_FIT
            1    -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            2    -> AspectRatioFrameLayout.RESIZE_MODE_FILL
            else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
        }

        exoScreen.setOnClickListener {
            if (isFullscreen < 2) isFullscreen += 1 else isFullscreen = 0
            playerView.resizeMode = when (isFullscreen) {
                0    -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                1    -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                2    -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
            }
            toastString(
                when (isFullscreen) {
                    0    -> "Original"
                    1    -> "Zoom"
                    2    -> "Stretch"
                    else -> "Original"
                }
            )
            saveData("${media.id}_fullscreenInt", isFullscreen, this)
        }

        //Cast
        if (settings.cast) {
            playerView.findViewById<ImageButton>(R.id.exo_cast).apply {
                visibility = View.VISIBLE
                setSafeOnClickListener {
                    cast()
                }
            }
        }

        //Speed
        val speeds = if (settings.cursedSpeeds) arrayOf(1f, 1.25f, 1.5f, 1.75f, 2f, 2.5f, 3f, 4f, 5f, 10f, 25f, 50f) else arrayOf(
            0.25f,
            0.33f,
            0.5f,
            0.66f,
            0.75f,
            1f,
            1.25f,
            1.33f,
            1.5f,
            1.66f,
            1.75f,
            2f
        )
        val speedsName = speeds.map { "${it}x" }.toTypedArray()
        var curSpeed = loadData("${media.id}_speed", this) ?: settings.defaultSpeed

        playbackParameters = PlaybackParameters(speeds[curSpeed])
        var speed: Float
        val speedDialog = AlertDialog.Builder(this, R.style.DialogTheme).setTitle("Speed")
        exoSpeed.setOnClickListener {
            speedDialog.setSingleChoiceItems(speedsName, curSpeed) { dialog, i ->
                if (isInitialized) {
                    saveData("${media.id}_speed", i, this)
                    speed = speeds[i]
                    curSpeed = i
                    playbackParameters = PlaybackParameters(speed)
                    exoPlayer.playbackParameters = playbackParameters
                    dialog.dismiss()
                    hideSystemBars()
                }
            }.show()
        }
        speedDialog.setOnCancelListener { hideSystemBars() }

        if (settings.autoPlay) {
            var touchTimer = Timer()
            fun touched() {
                interacted = true
                touchTimer.apply {
                    cancel()
                    purge()
                }
                touchTimer = Timer()
                touchTimer.schedule(object : TimerTask() {
                    override fun run() {
                        interacted = false
                    }
                }, 1000 * 60 * 60)
            }
            playerView.findViewById<View>(R.id.exo_touch_view).setOnTouchListener { _, _ ->
                touched()
                false
            }
        }

        preloading = false
        val showProgressDialog = if (settings.askIndividual) loadData<Boolean>("${media.id}_progressDialog") ?: true else false
        if (showProgressDialog && Anilist.userid != null && if (media.isAdult) settings.updateForH else true)
            AlertDialog.Builder(this, R.style.DialogTheme).setTitle("Auto Update progress for ${media.userPreferredName}?")
                .apply {
                    //                setMultiChoiceItems(arrayOf("Don't ask again for "), booleanArrayOf(true)) { _, _, isChecked ->
                    //                    if (isChecked) {
                    //                        saveData("${media.id}_progressDialog", isChecked)
                    //                    }
                    //                    showProgressDialog = isChecked
                    //                }
                    setOnCancelListener { hideSystemBars() }
                    setCancelable(false)
                    setPositiveButton("Yes") { dialog, _ ->
                        saveData("${media.id}_progressDialog", false)
                        saveData("${media.id}_save_progress", true)
                        dialog.dismiss()
                        model.setEpisode(episodes[media.anime!!.selectedEpisode!!]!!, "invoke")
                    }
                    setNegativeButton("No") { dialog, _ ->
                        saveData("${media.id}_progressDialog", false)
                        saveData("${media.id}_save_progress", false)
                        toast("You can long click List Editor button to Reset Auto Update")
                        dialog.dismiss()
                        model.setEpisode(episodes[media.anime!!.selectedEpisode!!]!!, "invoke")
                    }
                    show()
                }
        else model.setEpisode(episodes[media.anime!!.selectedEpisode!!]!!, "invoke")
    }

    private fun initPlayer() {
        checkNotch()

        saveData("${media.id}_current_ep", media.anime!!.selectedEpisode!!, this)

        val set = loadData<MutableSet<Int>>("continue_ANIME", this) ?: mutableSetOf()
        if (set.contains(media.id)) set.remove(media.id)
        set.add(media.id)
        saveData("continue_ANIME", set, this)

        val stream = episode.streamLinks[episode.selectedStream] ?: return

        val simpleCache = VideoCache.getInstance(this)
        val httpClient = okHttpClient.newBuilder().apply {
            ignoreAllSSLErrors()
            followRedirects(true)
            followSslRedirects(true)
        }.build()
        val dataSourceFactory = DataSource.Factory {
            val dataSource: HttpDataSource = OkHttpDataSource.Factory(httpClient).createDataSource()
            if (stream.headers != null)
                stream.headers.forEach {
                    dataSource.setRequestProperty(it.key, it.value)
                }
            dataSource
        }
        cacheFactory = CacheDataSource.Factory().apply {
            setCache(simpleCache)
            setUpstreamDataSourceFactory(dataSourceFactory)
        }

        //Subtitles
        val a = stream.subtitles
        val subtitle: MediaItem.SubtitleConfiguration? = if (a != null && a.contains("English"))
            MediaItem.SubtitleConfiguration.Builder(Uri.parse(a["English"]))
                .setMimeType(MimeTypes.TEXT_VTT).setSelectionFlags(C.SELECTION_FLAG_FORCED)
                .build()
        else null

        val url = if (episode.selectedQuality < stream.quality.size) stream.quality[episode.selectedQuality] else return
        val but = playerView.findViewById<ImageButton>(R.id.exo_download)
        if (url.quality != "Multi Quality") {
            but.visibility = View.VISIBLE
            but.setOnClickListener {
                download(this, episode, animeTitle.text.toString())
            }
        } else but.visibility = View.GONE

        val builder = MediaItem.Builder().setUri(url.url)
        if (subtitle != null) builder.setSubtitleConfigurations(mutableListOf(subtitle))
        mediaItem = builder.build()

        //Source
        exoSource.setOnClickListener {
            sourceClick()
        }

        //Quality Track
        trackSelector = DefaultTrackSelector(this)
        trackSelector.setParameters(
            trackSelector.buildUponParameters()
                .setMinVideoSize(loadData("maxWidth", this) ?: 720, loadData("maxHeight", this) ?: 480)
                .setMaxVideoSize(1, 1)
        )

        if (playbackPosition != 0L && !changingServer && !settings.alwaysContinue) {
            val time = String.format(
                "%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(playbackPosition),
                TimeUnit.MILLISECONDS.toMinutes(playbackPosition) - TimeUnit.HOURS.toMinutes(
                    TimeUnit.MILLISECONDS.toHours(
                        playbackPosition
                    )
                ),
                TimeUnit.MILLISECONDS.toSeconds(playbackPosition) - TimeUnit.MINUTES.toSeconds(
                    TimeUnit.MILLISECONDS.toMinutes(
                        playbackPosition
                    )
                )
            )
            AlertDialog.Builder(this, R.style.DialogTheme).setTitle("Continue from ${time}?").apply {
                setCancelable(false)
                setPositiveButton("Yes") { d, _ ->
                    buildExoplayer()
                    d.dismiss()
                }
                setNegativeButton("No") { d, _ ->
                    playbackPosition = 0L
                    buildExoplayer()
                    d.dismiss()
                }
            }.show()
        } else buildExoplayer()
    }

    private fun buildExoplayer() {
        //Player
        hideSystemBars()
        exoPlayer = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(cacheFactory))
            .setTrackSelector(trackSelector)
            .build().apply {
                playWhenReady = true
                this.playbackParameters = this@ExoplayerView.playbackParameters
                setMediaItem(mediaItem)
                prepare()
                loadData<Long>("${media.id}_${media.anime!!.selectedEpisode}_max")?.apply {
                    if (this <= playbackPosition) playbackPosition = max(0, this - 5)
                }
                seekTo(playbackPosition)
            }
        playerView.player = exoPlayer

        try {
            val mediaButtonReceiver = ComponentName(this, MediaButtonReceiver::class.java)
            MediaSessionCompat(this, "Player", mediaButtonReceiver, null).let { media ->
                val mediaSessionConnector = MediaSessionConnector(media)
                mediaSessionConnector.setPlayer(exoPlayer)
                media.isActive = true
            }
        } catch (e: Exception) {
            toast(e.toString())
        }

        exoPlayer.addListener(this)
        isInitialized = true
    }

    private fun releasePlayer() {
        isPlayerPlaying = exoPlayer.playWhenReady
        playbackPosition = exoPlayer.currentPosition
        exoPlayer.release()
        VideoCache.release()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        if (isInitialized) {
            outState.putInt(resumeWindow, exoPlayer.currentMediaItemIndex)
            outState.putLong(resumePosition, exoPlayer.currentPosition)
        }
        outState.putInt(playerFullscreen, isFullscreen)
        outState.putBoolean(playerOnPlay, isPlayerPlaying)
        super.onSaveInstanceState(outState)
    }

    private fun sourceClick(saveStreams: Boolean = true) {
        changingServer = true

        if (saveStreams) media.selected!!.stream = null
        saveData("${media.id}_${media.anime!!.selectedEpisode}", exoPlayer.currentPosition, this)
        model.saveSelected(media.id, media.selected!!, this)
        model.onEpisodeClick(
            media, episode.number, this.supportFragmentManager,
            launch = false
        )
    }

    override fun onPause() {
        super.onPause()
        orientationListener?.disable()
        if (isInitialized) {
            playerView.player?.pause()
            saveData("${media.id}_${media.anime!!.selectedEpisode}", exoPlayer.currentPosition, this)
        }
    }

    override fun onResume() {
        super.onResume()
        orientationListener?.enable()
        hideSystemBars()
        if (isInitialized) {
            playerView.onResume()
            playerView.useController = true
        }
    }

    override fun onStop() {
        playerView.player?.pause()
        super.onStop()
    }

    private var wasPlaying = false
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        if (settings.focusPause && !epChanging) {
            if (isInitialized && !hasFocus) wasPlaying = exoPlayer.isPlaying
            if (hasFocus) {
                if (isInitialized && wasPlaying) exoPlayer.play()
            } else {
                if (isInitialized) exoPlayer.pause()
            }
        }
        super.onWindowFocusChanged(hasFocus)
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        if (!isBuffering) {
            isPlayerPlaying = isPlaying
            playerView.keepScreenOn = isPlaying
            (exoPlay.drawable as Animatable?)?.start()
            if (!this.isDestroyed) Glide.with(this)
                .load(if (isPlaying) R.drawable.anim_play_to_pause else R.drawable.anim_pause_to_play).into(exoPlay)
        }
    }

    override fun onRenderedFirstFrame() {
        super.onRenderedFirstFrame()
        saveData("${media.id}_${media.anime!!.selectedEpisode}_max", exoPlayer.duration, this)
        val height = (exoPlayer.videoFormat ?: return).height
        val width = (exoPlayer.videoFormat ?: return).width

        if (episode.streamLinks[episode.selectedStream]?.quality?.get(episode.selectedQuality)?.quality == "Multi Quality") {
            saveData("maxHeight", height)
            saveData("maxWidth", width)
        }

        aspectRatio = Rational(width, height)
        //        exoPlayer.playbackParameters = playbackParameters

        videoInfo.text = "${episode.selectedStream}\n$width x $height"

        if (exoPlayer.duration < playbackPosition)
            exoPlayer.seekTo(0)
    }

    private var preloading = false
    private fun updateProgress() {
        if (isInitialized) {
            if (exoPlayer.currentPosition.toFloat() / exoPlayer.duration > settings.watchPercentage) {
                preloading = true
                nextEpisode(false) { i ->
                    val ep = episodes[episodeArr[currentEpisodeIndex + i]] ?: return@nextEpisode
                    val selected = media.selected ?: return@nextEpisode
                    lifecycleScope.launch(Dispatchers.IO) {
                        if (media.selected!!.stream != null)
                            model.loadEpisodeStream(ep, selected, false)
                        else
                            model.loadEpisodeStreams(ep, selected.source, false)
                    }
                }
            }
        }
        if (!preloading) handler.postDelayed({
            updateProgress()
        }, 2500)
    }

    override fun onTracksInfoChanged(tracksInfo: TracksInfo) {
        if (tracksInfo.trackGroupInfos.size <= 2) exoQuality.visibility = View.GONE
        else {
            exoQuality.visibility = View.VISIBLE
            exoQuality.setOnClickListener {
                initPopupQuality(trackSelector)?.show()
            }
        }
    }

    override fun onPlayerError(error: PlaybackException) {
        when (error.errorCode) {
            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS -> {
                toast("Source Exception : ${error.message}")
                isPlayerPlaying = true
                if (isInitialized) sourceClick(episode.saveStreams)
            }
            else                                            -> toast("Player Error ${error.errorCode} (${error.errorCodeName}) : ${error.message}")
        }
    }

    private var isBuffering = true
    override fun onPlaybackStateChanged(playbackState: Int) {
        if (playbackState == ExoPlayer.STATE_READY) {
            exoPlayer.play()
            if (episodeLength == 0f) {
                episodeLength = exoPlayer.duration.toFloat()
            }
        }
        isBuffering = playbackState == Player.STATE_BUFFERING
        if (playbackState == Player.STATE_ENDED && settings.autoPlay) {
            if (interacted) exoNext.performClick()
            else toast("Autoplay cancelled, no Interaction for more than 1 Hour.")
        }
        super.onPlaybackStateChanged(playbackState)
    }

    private fun updateAniProgress() {
        if (exoPlayer.currentPosition / episodeLength > settings.watchPercentage && Anilist.userid != null)
            if (loadData<Boolean>("${media.id}_save_progress") != false && if (media.isAdult) settings.updateForH else true) {
                media.anime!!.selectedEpisode?.apply {
                    updateAnilistProgress(media, this)
                }
            }
    }

    private fun nextEpisode(toast: Boolean = true, runnable: ((Int) -> Unit)) {
        var isFiller = true
        var i = 1
        while (isFiller) {
            if (episodeArr.size > currentEpisodeIndex + i) {
                isFiller = if (settings.autoSkipFiller) episodes[episodeArr[currentEpisodeIndex + i]]?.filler ?: false else false
                if (!isFiller) runnable.invoke(i)
                i++
            } else {
                if (toast)
                    toast("No next Episode Found!")
                isFiller = false
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        finishAndRemoveTask()
        startActivity(intent)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finishAndRemoveTask()
    }

    override fun onDestroy() {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

        if (isInitialized) {
            updateAniProgress()
            releasePlayer()
        }

        super.onDestroy()
        finishAndRemoveTask()
    }

    // QUALITY SELECTOR
    private fun initPopupQuality(trackSelector: DefaultTrackSelector): Dialog? {
        val mappedTrackInfo = trackSelector.currentMappedTrackInfo ?: return null
        var videoRenderer: Int? = null

        fun isVideoRenderer(mappedTrackInfo: MappingTrackSelector.MappedTrackInfo, rendererIndex: Int): Boolean {
            if (mappedTrackInfo.getTrackGroups(rendererIndex).length == 0) return false
            return C.TRACK_TYPE_VIDEO == mappedTrackInfo.getRendererType(rendererIndex)
        }

        for (i in 0 until mappedTrackInfo.rendererCount)
            if (isVideoRenderer(mappedTrackInfo, i))
                videoRenderer = i

        val trackSelectionDialogBuilder =
            TrackSelectionDialogBuilder(this, "Available Qualities", trackSelector, videoRenderer ?: return null)
        trackSelectionDialogBuilder.setTheme(R.style.DialogTheme)
        trackSelectionDialogBuilder.setTrackNameProvider {
            if (it.frameRate > 0f) it.height.toString() + "p" else it.height.toString() + "p (fps : N/A)"
        }
        val trackDialog = trackSelectionDialogBuilder.build()
        trackDialog.setOnDismissListener { hideSystemBars() }
        return trackDialog
    }

    //Double Tap Animation
    private fun hideLayer(v: View, text: View) {
        val timerTask = object : TimerTask() {
            override fun run() {
                handler.post {
                    ObjectAnimator.ofFloat(v, "alpha", v.alpha, 0f).setDuration(150).start()
                    ObjectAnimator.ofFloat(text, "alpha", 1f, 0f).setDuration(150).start()
                }
            }
        }
        if (v.id == R.id.exo_fast_forward) {
            t1.cancel()
            t1.purge()
            t1 = Timer()
            t1.schedule(timerTask, 450)
        } else {
            t2.cancel()
            t2.purge()
            t2 = Timer()
            t2.schedule(timerTask, 450)
        }
    }

    private fun viewDoubleTapped(v: View, event: MotionEvent? = null, text: TextView) {
        ObjectAnimator.ofFloat(text, "alpha", 1f, 1f).setDuration(600).start()
        ObjectAnimator.ofFloat(text, "alpha", 0f, 1f).setDuration(150).start()

        (text.compoundDrawables[1] as Animatable).apply {
            if (!isRunning) start()
        }

        if (event != null) {
            playerView.hideController()
            v.circularReveal(event.x.toInt(), event.y.toInt(), 300)
            ObjectAnimator.ofFloat(v, "alpha", 1f, 1f).setDuration(600).start()
            ObjectAnimator.ofFloat(v, "alpha", 0f, 1f).setDuration(300).start()
        }

        v.postDelayed({
            hideLayer(v, text)
        }, 450)
    }

    private fun cast() {
        val stream = episode.streamLinks[episode.selectedStream] ?: return
        if (episode.selectedQuality >= stream.quality.size) return
        val videoURL = stream.quality[episode.selectedQuality].url
        val shareVideo = Intent(Intent.ACTION_VIEW)
        shareVideo.setDataAndType(Uri.parse(videoURL), "video/*")
        shareVideo.setPackage("com.instantbits.cast.webvideo")
        if (stream.subtitles != null) shareVideo.putExtra("subtitle", stream.subtitles["English"])
        shareVideo.putExtra("title", media.userPreferredName + " : Ep " + episodeTitleArr[currentEpisodeIndex])
        shareVideo.putExtra("poster", episode.thumb ?: media.cover)
        val headers = Bundle()
        stream.headers?.forEach {
            headers.putString(it.key, it.value)
        }
        shareVideo.putExtra("android.media.intent.extra.HTTP_HEADERS", headers)
        shareVideo.putExtra("headers", headers)
        shareVideo.putExtra("secure_uri", true)
        try {
            startActivity(shareVideo)
        } catch (ex: ActivityNotFoundException) {
            val intent = Intent(Intent.ACTION_VIEW)
            val uriString = "market://details?id=com.instantbits.cast.webvideo"
            intent.data = Uri.parse(uriString)
            startActivity(intent)
        }
    }

    // Enter PiP Mode
    @Suppress("DEPRECATION")
    @RequiresApi(Build.VERSION_CODES.N)
    private fun enterPipMode() {
        if (!pipEnabled) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                enterPictureInPictureMode(
                    PictureInPictureParams
                        .Builder()
                        .setAspectRatio(aspectRatio)
                        .build()
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                enterPictureInPictureMode()
            }
        } catch (e: Exception) {
            toastString(e.toString())
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration?) {
        playerView.useController = !isInPictureInPictureMode
        saveData("${media.id}_${episode.number}", exoPlayer.currentPosition, this)
        if (isInPictureInPictureMode) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            orientationListener?.disable()
        } else {
            orientationListener?.enable()
        }
        if (isInitialized) exoPlayer.play()
        saveData("${media.id}_${episode.number}", exoPlayer.currentPosition, this)
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
    }

}