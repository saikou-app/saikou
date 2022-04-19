package ani.saikou.app.util

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.app.DatePickerDialog
import android.app.DownloadManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources.getSystem
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.text.InputFilter
import android.text.Spanned
import android.util.AttributeSet
import android.view.*
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.animation.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat.getExternalFilesDirs
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.math.MathUtils.clamp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import ani.saikou.app.R
import ani.saikou.app.Saikou
import ani.saikou.app.android.activity.MainActivity
import ani.saikou.app.databinding.ItemCountDownBinding
import ani.saikou.app.util.anilist.anilist.Anilist
import ani.saikou.core.model.anime.Episode
import ani.saikou.core.model.media.Media
import ani.saikou.core.model.settings.UserInterfaceSettings
import ani.saikou.core.util.FuzzyDate
import com.bumptech.glide.Glide
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.google.android.exoplayer2.ui.DefaultTimeBar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.internal.ViewUtils
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import nl.joery.animatedbottombar.AnimatedBottomBar
import org.jsoup.Jsoup
import java.io.*
import java.lang.reflect.Field
import java.util.*
import kotlin.math.*

var statusBarHeight = 0
var navBarHeight = 0
val Int.dp: Float get() = (this / getSystem().displayMetrics.density)
val Float.px: Int get() = (this * getSystem().displayMetrics.density).toInt()

lateinit var bottomBar: AnimatedBottomBar
var selectedOption = 1

object Refresh {
    fun all() {
        for (i in activity) {
            activity[i.key]!!.postValue(true)
        }
    }

    val activity = mutableMapOf<Int, MutableLiveData<Boolean>>()
}

fun currActivity(): Activity? =
    Saikou.currentActivity()

var loadMedia: Int? = null
var loadIsMAL = false

fun logger(e: Any?, print: Boolean = true) {
    if (print) {
        println(e)
    }
}

fun saveData(fileName: String, data: Any?, activity: Activity? = null) {
    try {
        val a = activity ?: currActivity()
        if (a != null) {
            val fos: FileOutputStream = a.openFileOutput(fileName, Context.MODE_PRIVATE)
            val os = ObjectOutputStream(fos)
            os.writeObject(data)
            os.close()
            fos.close()
        }
    } catch (e: Exception) {
        toastString(e.toString())
    }
}

@Suppress("UNCHECKED_CAST")
fun <T> loadData(fileName: String, activity: Activity? = null, toast: Boolean = true): T? {
    val a = activity ?: currActivity()
    try {
        if (a?.fileList() != null)
            if (fileName in a.fileList()) {
                val fileIS: FileInputStream = a.openFileInput(fileName)
                val objIS = ObjectInputStream(fileIS)
                val data = objIS.readObject() as T
                objIS.close()
                fileIS.close()
                return data
            }
    } catch (e: Exception) {
        if (toast) toastString("Error loading data $fileName")
    }
    return null
}

fun initActivity(a: Activity) {
    val window = a.window
    WindowCompat.setDecorFitsSystemWindows(window, false)
    val uiSettings = loadData<UserInterfaceSettings>("ui_settings", toast = false)
        ?: UserInterfaceSettings().apply { saveData("ui_settings", this) }
    uiSettings.darkMode.apply {
        AppCompatDelegate.setDefaultNightMode(
            when (this) {
                true -> AppCompatDelegate.MODE_NIGHT_YES
                false -> AppCompatDelegate.MODE_NIGHT_NO
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
        )
    }
    if (uiSettings.immersiveMode) {
        if (navBarHeight == 0) {
            ViewCompat.getRootWindowInsets(window.decorView.findViewById(android.R.id.content))
                ?.apply {
                    navBarHeight = this.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
                }
        }
        a.hideStatusBar()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && statusBarHeight == 0 && a.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            window.decorView.rootWindowInsets?.displayCutout?.apply {
                if (boundingRects.size > 0) {
                    statusBarHeight = min(boundingRects[0].width(), boundingRects[0].height())
                }
            }
        }
    } else
        if (statusBarHeight == 0) {
            val windowInsets =
                ViewCompat.getRootWindowInsets(window.decorView.findViewById(android.R.id.content))
            if (windowInsets != null) {
                val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
                statusBarHeight = insets.top
                navBarHeight = insets.bottom
            }
        }
}

@Suppress("DEPRECATION")
fun Activity.hideSystemBars() {
    window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            )
}

@Suppress("DEPRECATION")
fun Activity.hideStatusBar() {
    window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
}

open class BottomSheetDialogFragment : BottomSheetDialogFragment() {
    override fun onStart() {
        super.onStart()
        if (this.resources.configuration.orientation != Configuration.ORIENTATION_PORTRAIT) {
            val behavior = BottomSheetBehavior.from(requireView().parent as View)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    override fun show(manager: FragmentManager, tag: String?) {
        val ft = manager.beginTransaction()
        ft.add(this, tag)
        ft.commitAllowingStateLoss()
    }
}

fun isOnline(context: Context): Boolean {
    val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val capabilities =
                connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
            if (capabilities != null) {
                when {
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                        logger("Device on Cellular")
                        return true
                    }
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                        logger("Device on Wifi")
                        return true
                    }
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> {
                        logger("Device on Ethernet, TF man?")
                        return true
                    }
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> {
                        logger("Device on VPN")
                        return true
                    }
                }
            }
        } else return true
    } catch (e: Exception) {
        toastString(e.toString())
    }
    return false
}

fun startMainActivity(activity: Activity) {
    activity.finishAffinity()
    activity.startActivity(
        Intent(
            activity,
            MainActivity::class.java
        ).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
    )
}

class DatePickerFragment(activity: Activity, var date: FuzzyDate = FuzzyDate.today) :
    DialogFragment(), DatePickerDialog.OnDateSetListener {
    var dialog: DatePickerDialog

    init {
        val c = Calendar.getInstance()
        val year = date.year ?: c.get(Calendar.YEAR)
        val month = if (date.month != null) date.month!! - 1 else c.get(Calendar.MONTH)
        val day = date.day ?: c.get(Calendar.DAY_OF_MONTH)
        dialog = DatePickerDialog(activity, this, year, month, day)
    }

    override fun onDateSet(view: DatePicker, year: Int, month: Int, day: Int) {
        date = FuzzyDate(year, month + 1, day)
    }
}

class InputFilterMinMax(
    private val min: Double,
    private val max: Double,
    private val status: AutoCompleteTextView? = null
) : InputFilter {
    override fun filter(
        source: CharSequence,
        start: Int,
        end: Int,
        dest: Spanned,
        dstart: Int,
        dend: Int
    ): CharSequence? {
        try {
            val input = (dest.toString() + source.toString()).toDouble()
            if (isInRange(min, max, input)) return null
        } catch (nfe: NumberFormatException) {
            nfe.printStackTrace()
        }
        return ""
    }

    @SuppressLint("SetTextI18n")
    private fun isInRange(a: Double, b: Double, c: Double): Boolean {
        if (c == b) {
            status?.setText("COMPLETED", false)
            status?.parent?.requestLayout()
        }
        return if (b > a) c in a..b else c in b..a
    }
}

fun getMalMedia(media: Media): Media {
    try {
        if (media.anime != null) {
            val anime = media.anime!!

            val res =
                Jsoup.connect("https://myanimelist.net/anime/${media.idMAL}").ignoreHttpErrors(true)
                    .get()
            val a = res.select(".title-english").text()
            media.nameMAL = if (a != "") a else res.select(".title-name").text()
            media.typeMAL = if (res.select("div.spaceit_pad > a")
                    .isNotEmpty()
            ) res.select("div.spaceit_pad > a")[0].text() else null
            anime.op = mutableListOf()
            res.select(".opnening > table > tbody > tr").forEach {
                val text = it.text()
                if (!text.contains("Help improve our database"))
                    anime.op.add(it.text())
            }
            anime.ed = mutableListOf()
            res.select(".ending > table > tbody > tr").forEach {
                val text = it.text()
                if (!text.contains("Help improve our database"))
                    anime.ed.add(it.text())
            }

        } else {
            val res =
                Jsoup.connect("https://myanimelist.net/manga/${media.idMAL}").ignoreHttpErrors(true)
                    .get()
            val b = res.select(".title-english").text()
            val a = res.select(".h1-title").text().removeSuffix(b)
            media.nameMAL = a
            media.typeMAL = if (res.select("div.spaceit_pad > a")
                    .isNotEmpty()
            ) res.select("div.spaceit_pad > a")[0].text() else null
        }
    } catch (e: Exception) {
        toastString(e.message)
    }
    return media
}

class ZoomOutPageTransformer(private val uiSettings: UserInterfaceSettings) :
    ViewPager2.PageTransformer {
    override fun transformPage(view: View, position: Float) {
        if (position == 0.0f && uiSettings.layoutAnimations) {
            setAnimation(
                view.context,
                view,
                uiSettings,
                300,
                floatArrayOf(1.3f, 1f, 1.3f, 1f),
                0.5f to 0f
            )
            ObjectAnimator.ofFloat(view, "alpha", 0f, 1.0f)
                .setDuration((200 * uiSettings.animationSpeed).toLong()).start()
        }
    }
}

fun setAnimation(
    context: Context,
    viewToAnimate: View,
    uiSettings: UserInterfaceSettings,
    duration: Long = 150,
    list: FloatArray = floatArrayOf(0.0f, 1.0f, 0.0f, 1.0f),
    pivot: Pair<Float, Float> = 0.5f to 0.5f
) {
    if (uiSettings.layoutAnimations) {
        val anim = ScaleAnimation(
            list[0],
            list[1],
            list[2],
            list[3],
            Animation.RELATIVE_TO_SELF,
            pivot.first,
            Animation.RELATIVE_TO_SELF,
            pivot.second
        )
        anim.duration = (duration * uiSettings.animationSpeed).toLong()
        anim.setInterpolator(context, R.anim.over_shoot)
        viewToAnimate.startAnimation(anim)
    }
}


fun ImageView.loadImage(url: String?, size: Int = 0, headers: MutableMap<String, String>? = null) {
    if (!url.isNullOrEmpty()) {
        try {
            val glideUrl = GlideUrl(url) { headers ?: mutableMapOf() }
            Glide.with(this).load(glideUrl).transition(withCrossFade()).override(size).into(this)
        } catch (e: Exception) {
            logger(e.localizedMessage)
        }
    }
}

class SafeClickListener(
    private var defaultInterval: Int = 1000,
    private val onSafeCLick: (View) -> Unit
) : View.OnClickListener {

    private var lastTimeClicked: Long = 0

    override fun onClick(v: View) {
        if (SystemClock.elapsedRealtime() - lastTimeClicked < defaultInterval) {
            return
        }
        lastTimeClicked = SystemClock.elapsedRealtime()
        onSafeCLick(v)
    }
}

fun View.setSafeOnClickListener(onSafeClick: (View) -> Unit) {
    val safeClickListener = SafeClickListener {
        onSafeClick(it)
    }
    setOnClickListener(safeClickListener)
}

class FTActivityLifecycleCallbacks : Application.ActivityLifecycleCallbacks {
    var currentActivity: Activity? = null

    override fun onActivityCreated(p0: Activity, p1: Bundle?) {}
    override fun onActivityStarted(p0: Activity) {}
    override fun onActivityResumed(p0: Activity) {
        currentActivity = p0
    }

    override fun onActivityPaused(p0: Activity) {}
    override fun onActivityStopped(p0: Activity) {}
    override fun onActivitySaveInstanceState(p0: Activity, p1: Bundle) {}
    override fun onActivityDestroyed(p0: Activity) {}
}

@SuppressLint("ViewConstructor")
class ExtendedTimeBar(
    context: Context,
    attrs: AttributeSet?
) : DefaultTimeBar(context, attrs) {
    private var enabled = false
    private var forceDisabled = false
    override fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
        super.setEnabled(!forceDisabled && this.enabled)
    }

    fun setForceDisabled(forceDisabled: Boolean) {
        this.forceDisabled = forceDisabled
        isEnabled = enabled
    }
}

abstract class DoubleClickListener : GestureDetector.SimpleOnGestureListener() {
    private var timer: Timer? = null //at class level;
    private val delay: Long = 200

    override fun onSingleTapUp(e: MotionEvent?): Boolean {
        processSingleClickEvent(e)
        return super.onSingleTapUp(e)
    }

    override fun onLongPress(e: MotionEvent?) {
        processLongClickEvent(e)
        super.onLongPress(e)
    }

    override fun onDoubleTap(e: MotionEvent?): Boolean {
        processDoubleClickEvent(e)
        return super.onDoubleTap(e)
    }

    override fun onScroll(
        e1: MotionEvent?,
        e2: MotionEvent?,
        distanceX: Float,
        distanceY: Float
    ): Boolean {
        onScrollYClick(distanceY)
        return super.onScroll(e1, e2, distanceX, distanceY)
    }

    private fun processSingleClickEvent(e: MotionEvent?) {
        val handler = Handler(Looper.getMainLooper())
        val mRunnable = Runnable {
            onSingleClick(e)
        }
        timer = Timer().apply {
            schedule(object : TimerTask() {
                override fun run() {
                    handler.post(mRunnable)
                }
            }, delay)
        }
    }

    private fun processDoubleClickEvent(e: MotionEvent?) {
        timer?.apply {
            cancel()
            purge()
        }
        onDoubleClick(e) //Do what ever u want on Double Click
    }

    private fun processLongClickEvent(e: MotionEvent?) {
        timer?.apply {
            cancel()
            purge()
        }
        onLongClick(e) //Do what ever u want on Double Click
    }

    open fun onSingleClick(event: MotionEvent?) {}
    abstract fun onDoubleClick(event: MotionEvent?)
    open fun onScrollYClick(y: Float) {}
    open fun onLongClick(event: MotionEvent?) {}
}

fun View.circularReveal(x: Int, y: Int, time: Long) {
    ViewAnimationUtils.createCircularReveal(this, x, y, 0f, max(height, width).toFloat())
        .setDuration(time).start()
}

fun openLinkInBrowser(link: String?) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
        currActivity()?.startActivity(intent)
    } catch (e: Exception) {
        toastString(e.toString())
    }
}

fun download(activity: Activity, episode: Episode, animeTitle: String) {
    val manager = activity.getSystemService(AppCompatActivity.DOWNLOAD_SERVICE) as DownloadManager
    val stream = episode.streamLinks[episode.selectedStream] ?: return
    val uri =
        if (stream.quality.size > episode.selectedQuality) Uri.parse(stream.quality[episode.selectedQuality].url) else return
    val regex = "[\\\\/:*?\"<>|]".toRegex()
    val aTitle = animeTitle.replace(regex, "")
    val request: DownloadManager.Request = DownloadManager.Request(uri)

    stream.headers?.forEach {
        request.addRequestHeader(it.key, it.value)
    }

    val title =
        "Episode ${episode.number}${if (episode.title != null) " - ${episode.title}" else ""}".replace(
            regex,
            ""
        )

    CoroutineScope(Dispatchers.IO).launch {
        try {
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

            val arrayOfFiles = getExternalFilesDirs(activity, null)
            if (loadData<Boolean>("sd_dl") == true && arrayOfFiles.size > 1 && arrayOfFiles[0] != null && arrayOfFiles[1] != null) {
                val parentDirectory =
                    arrayOfFiles[1].toString() + "/Anime/${aTitle}/".also { println("external $it") }
                val direct = File(parentDirectory)
                if (!direct.exists()) direct.mkdirs()
                request.setDestinationUri(Uri.fromFile(File("$parentDirectory$title (${stream.quality[episode.selectedQuality].quality}).mp4")))
            } else {
                val direct = File(Environment.DIRECTORY_DOWNLOADS + "/Saikou/Anime/${aTitle}/")
                if (!direct.exists()) direct.mkdirs()
                request.setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS,
                    "/Saikou/Anime/${aTitle}/$title (${stream.quality[episode.selectedQuality].quality}).mp4"
                )
            }
            request.setTitle("$title:$aTitle")
            manager.enqueue(request)
            toast("Started Downloading\n$title : $aTitle")
        } catch (e: SecurityException) {
            toast("Please give permission to access Files & Folders from Settings, & Try again.")
        } catch (e: Exception) {
            toast(e.toString())
        }
    }
}

fun updateAnilistProgress(media: Media, number: String) {
    if (Anilist.userid != null) {
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            val a = number.toFloatOrNull()?.roundToInt()
            if (a != media.userProgress) {
                Anilist.mutation.editList(
                    media.id,
                    a,
                    status = if (media.userStatus == "REPEATING") media.userStatus else "CURRENT"
                )
                toast("Setting progress to $a")
            }
            media.userProgress = a
            Refresh.all()
        }
    } else {
        toast("Please Login into anilist account!")
    }
}

class MediaPageTransformer : ViewPager2.PageTransformer {
    private fun parallax(view: View, position: Float) {
        if (position > -1 && position < 1) {
            val width = view.width.toFloat()
            view.translationX = -(position * width * 0.8f)
        }
    }

    override fun transformPage(view: View, position: Float) {

        val bannerContainer = view.findViewById<View>(R.id.itemCompactBanner)
        parallax(bannerContainer, position)
    }
}

fun copyToClipboard(string: String, toast: Boolean = true) {
    val activity = currActivity() ?: return
    val clipboard = getSystemService(activity, ClipboardManager::class.java)
    val clip = ClipData.newPlainText("label", string)
    clipboard?.setPrimaryClip(clip)
    if (toast) toastString("Copied \"$string\"")
}

@SuppressLint("SetTextI18n")
fun countDown(media: Media, view: ViewGroup) {
    val anime = media.anime
    if (anime?.nextAiringEpisode != null && anime.nextAiringEpisodeTime != null && (anime.nextAiringEpisodeTime!! - System.currentTimeMillis() / 1000) <= 86400 * 7.toLong()) {
        val v = ItemCountDownBinding.inflate(LayoutInflater.from(view.context), view, false)
        view.addView(v.root, 0)
        v.mediaCountdownText.text =
            "Episode ${anime.nextAiringEpisode!! + 1} will be released in"
        object : CountDownTimer(
            (anime.nextAiringEpisodeTime!! + 10000) * 1000 - System.currentTimeMillis(),
            1000
        ) {
            override fun onTick(millisUntilFinished: Long) {
                val a = millisUntilFinished / 1000
                v.mediaCountdown.text =
                    "${a / 86400} days ${a % 86400 / 3600} hrs ${a % 86400 % 3600 / 60} mins ${a % 86400 % 3600 % 60} secs"
            }

            override fun onFinish() {
                v.mediaCountdownContainer.visibility = View.GONE
                toastString("Congrats Vro")
            }
        }.start()
    }
}

fun setSlideIn(uiSettings: UserInterfaceSettings) = AnimationSet(false).apply {
    if (uiSettings.layoutAnimations) {
        var animation: Animation = AlphaAnimation(0.0f, 1.0f)
        animation.duration = (500 * uiSettings.animationSpeed).toLong()
        animation.interpolator = AccelerateDecelerateInterpolator()
        addAnimation(animation)

        animation = TranslateAnimation(
            Animation.RELATIVE_TO_SELF, 1.0f,
            Animation.RELATIVE_TO_SELF, 0f,
            Animation.RELATIVE_TO_SELF, 0.0f,
            Animation.RELATIVE_TO_SELF, 0f
        )

        animation.duration = (750 * uiSettings.animationSpeed).toLong()
        animation.interpolator = OvershootInterpolator(1.1f)
        addAnimation(animation)
    }
}

fun setSlideUp(uiSettings: UserInterfaceSettings) = AnimationSet(false).apply {
    if (uiSettings.layoutAnimations) {
        var animation: Animation = AlphaAnimation(0.0f, 1.0f)
        animation.duration = (500 * uiSettings.animationSpeed).toLong()
        animation.interpolator = AccelerateDecelerateInterpolator()
        addAnimation(animation)

        animation = TranslateAnimation(
            Animation.RELATIVE_TO_SELF, 0.0f,
            Animation.RELATIVE_TO_SELF, 0f,
            Animation.RELATIVE_TO_SELF, 1.0f,
            Animation.RELATIVE_TO_SELF, 0f
        )

        animation.duration = (750 * uiSettings.animationSpeed).toLong()
        animation.interpolator = OvershootInterpolator(1.1f)
        addAnimation(animation)
    }
}

class EmptyAdapter(private val count: Int) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return EmptyViewHolder(View(parent.context))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {}

    override fun getItemCount(): Int = count

    inner class EmptyViewHolder(view: View) : RecyclerView.ViewHolder(view)
}

fun toast(string: String?, activity: Activity? = null) {
    if (string != null) {
        (activity ?: currActivity())?.apply {
            runOnUiThread {
                Toast.makeText(this, string, Toast.LENGTH_SHORT).show()
            }
        }
        logger(string)
    }
}

fun toastString(s: String?, activity: Activity? = null) {
    if (s != null) {
        (activity ?: currActivity())?.apply {
            runOnUiThread {
                val snackBar = Snackbar.make(
                    window.decorView.findViewById(android.R.id.content),
                    s,
                    Snackbar.LENGTH_LONG
                )
                snackBar.view.updateLayoutParams<FrameLayout.LayoutParams> {
                    gravity = (Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM)
                    width = WRAP_CONTENT
                }
                snackBar.view.translationY = -(navBarHeight.dp + 32f)
                snackBar.view.setOnClickListener {
                    snackBar.dismiss()
                }
                snackBar.view.setOnLongClickListener {
                    copyToClipboard(s, false)
                    toast("Copied to Clipboard")
                    true
                }
                snackBar.show()
            }
        }
        logger(s)
    }
}

open class NoPaddingArrayAdapter<T>(context: Context, layoutId: Int, items: List<T>) :
    ArrayAdapter<T>(context, layoutId, items) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getView(position, convertView, parent)
        view.setPadding(0, view.paddingTop, view.paddingRight, view.paddingBottom)
        (view as TextView).setTextColor(Color.WHITE)
        return view
    }
}

@SuppressLint("ClickableViewAccessibility")
class SpinnerNoSwipe : androidx.appcompat.widget.AppCompatSpinner {
    private var mGestureDetector: GestureDetector? = null

    constructor(context: Context) : super(context) {
        setup()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        setup()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        setup()
    }

    private fun setup() {
        mGestureDetector =
            GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
                override fun onSingleTapUp(e: MotionEvent): Boolean {
                    return performClick()
                }
            })
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        mGestureDetector!!.onTouchEvent(event)
        return true
    }
}

@SuppressLint("RestrictedApi")
class CustomBottomNavBar @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : BottomNavigationView(context, attrs) {
    init {
        ViewUtils.doOnApplyWindowInsets(
            this
        ) { view, insets, initialPadding ->
            initialPadding.bottom = 0
            updateLayoutParams<MarginLayoutParams> { bottomMargin = navBarHeight }
            initialPadding.applyToView(view)
            insets
        }
    }
}

fun getCurrentBrightnessValue(context: Context): Float {
    fun getMax(): Int {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager

        val fields: Array<Field> = powerManager.javaClass.declaredFields
        for (field in fields) {
            if (field.name.equals("BRIGHTNESS_ON")) {
                field.isAccessible = true
                return try {
                    field.get(powerManager)?.toString()?.toInt() ?: 255
                } catch (e: IllegalAccessException) {
                    255
                }
            }
        }
        return 255
    }

    fun getCur(): Float {
        return Settings.System.getInt(
            context.contentResolver,
            Settings.System.SCREEN_BRIGHTNESS,
            127
        ).toFloat()
    }

    return brightnessConverter(getCur() / getMax(), true)
}

fun brightnessConverter(it: Float, fromLog: Boolean) =
    clamp(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
            if (fromLog) log2((it * 256f)) * 12.5f / 100f else 2f.pow(it * 100f / 12.5f) / 256f
        else it, 0.001f, 1f
    )