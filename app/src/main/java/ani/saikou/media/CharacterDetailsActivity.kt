package ani.saikou.media

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.math.MathUtils.clamp
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.GridLayoutManager
import ani.saikou.*
import ani.saikou.databinding.ActivityCharacterBinding
import ani.saikou.settings.UserInterfaceSettings
import com.google.android.material.appbar.AppBarLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.abs

class CharacterDetailsActivity : AppCompatActivity(), AppBarLayout.OnOffsetChangedListener {
    private lateinit var binding: ActivityCharacterBinding
    private val scope = lifecycleScope
    private val model: OtherDetailsViewModel by viewModels()
    private lateinit var character: Character
    private var loaded = false
    val uiSettings = loadData<UserInterfaceSettings>("ui_settings") ?: UserInterfaceSettings()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCharacterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initActivity(this)
        screenWidth = resources.displayMetrics.run { widthPixels / density }
        if (uiSettings.immersiveMode) this.window.statusBarColor =
            ContextCompat.getColor(this, R.color.status)

        val banner =
            if (uiSettings.bannerAnimations) binding.characterBanner else binding.characterBannerNoKen

        banner.updateLayoutParams { height += statusBarHeight }
        binding.characterClose.updateLayoutParams<ViewGroup.MarginLayoutParams> { topMargin += statusBarHeight }
        binding.characterCollapsing.minimumHeight = statusBarHeight
        binding.characterCover.updateLayoutParams<ViewGroup.MarginLayoutParams> { topMargin += statusBarHeight }
        binding.characterRecyclerView.updatePadding(bottom = 64f.px + navBarHeight)
        binding.characterTitle.isSelected = true
        binding.characterAppBar.addOnOffsetChangedListener(this)

        binding.characterClose.setOnClickListener {
            onBackPressed()
        }
        character = intent.getSerializableExtra("character") as? Character ?: return
        binding.characterTitle.text = character.name
        banner.loadImage(character.banner)
        binding.characterCoverImage.loadImage(character.image)
        binding.characterCoverImage.setOnLongClickListener { (openLinkInBrowser(character.image)); true }

        model.getCharacter().observe(this) {
            if (it != null && !loaded) {
                character = it
                loaded = true
                binding.characterProgress.visibility = View.GONE
                binding.characterRecyclerView.visibility = View.VISIBLE

                val roles = character.roles
                if (roles != null) {
                    val mediaAdaptor = MediaAdaptor(0, roles, this, matchParent = true)
                    val concatAdaptor =
                        ConcatAdapter(CharacterDetailsAdapter(character, this), mediaAdaptor)

                    val gridSize = (screenWidth / 124f).toInt()
                    val gridLayoutManager = GridLayoutManager(this, gridSize)
                    gridLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                        override fun getSpanSize(position: Int): Int {
                            return when (position) {
                                0 -> gridSize
                                else -> 1
                            }
                        }
                    }
                    binding.characterRecyclerView.adapter = concatAdaptor
                    binding.characterRecyclerView.layoutManager = gridLayoutManager
                }
            }
        }

        val live = Refresh.activity.getOrPut(this.hashCode()) { MutableLiveData(true) }
        live.observe(this) {
            scope.launch(Dispatchers.IO) {
                model.loadCharacter(character)
            }
        }
    }

    override fun onResume() {
        binding.characterProgress.visibility = if (!loaded) View.VISIBLE else View.GONE
        super.onResume()
    }

    private var isCollapsed = false
    private val percent = 30
    private var mMaxScrollSize = 0
    private var screenWidth: Float = 0f

    override fun onOffsetChanged(appBar: AppBarLayout, i: Int) {
        if (mMaxScrollSize == 0) mMaxScrollSize = appBar.totalScrollRange
        val percentage = abs(i) * 100 / mMaxScrollSize
        val cap = clamp((percent - percentage) / percent.toFloat(), 0f, 1f)

        binding.characterCover.scaleX = 1f * cap
        binding.characterCover.scaleY = 1f * cap
        binding.characterCover.cardElevation = 32f * cap

        binding.characterCover.visibility =
            if (binding.characterCover.scaleX == 0f) View.GONE else View.VISIBLE

        if (percentage >= percent && !isCollapsed) {
            isCollapsed = true
            if (uiSettings.immersiveMode) this.window.statusBarColor =
                ContextCompat.getColor(this, R.color.nav_bg)
            binding.characterAppBar.setBackgroundResource(R.color.nav_bg)
        }
        if (percentage <= percent && isCollapsed) {
            isCollapsed = false
            if (uiSettings.immersiveMode) this.window.statusBarColor =
                ContextCompat.getColor(this, R.color.status)
            binding.characterAppBar.setBackgroundResource(R.color.bg)
        }
    }
}