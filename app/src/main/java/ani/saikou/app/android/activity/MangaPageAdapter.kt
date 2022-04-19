package ani.saikou.app.android.activity

import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LayoutAnimationController
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import ani.saikou.app.android.activity.media.SearchActivity
import ani.saikou.app.android.adapter.media.MediaAdapter
import ani.saikou.app.android.fragment.settings.SettingsDialogFragment
import ani.saikou.app.anilist.Anilist
import ani.saikou.app.databinding.ItemMangaPageBinding
import ani.saikou.app.util.*
import ani.saikou.core.model.settings.UserInterfaceSettings
import ani.saikou.core.service.STORE

class MangaPageAdapter : RecyclerView.Adapter<MangaPageAdapter.MangaPageViewHolder>() {
    val ready = MutableLiveData(false)
    lateinit var binding: ItemMangaPageBinding
    private var trendHandler: Handler? = null
    private lateinit var trendRun: Runnable
    var trendingViewPager: ViewPager2? = null
    private var uiSettings: UserInterfaceSettings =
        STORE.loadData("ui_settings") ?: UserInterfaceSettings()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MangaPageViewHolder {
        val binding =
            ItemMangaPageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MangaPageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MangaPageViewHolder, position: Int) {
        binding = holder.binding
        trendingViewPager = binding.mangaTrendingViewPager

        binding.mangaTitleContainer.updatePadding(top = statusBarHeight)

        if (uiSettings.smallView) binding.mangaTrendingContainer.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            bottomMargin = (-108f).px
        }

        updateAvatar()

        binding.mangaSearchBar.hint = "MANGA"
        binding.mangaSearchBarText.setOnClickListener {
            ContextCompat.startActivity(
                it.context,
                Intent(it.context, SearchActivity::class.java).putExtra("type", "MANGA"),
                null
            )
        }

        binding.mangaUserAvatar.setSafeOnClickListener {
            SettingsDialogFragment().show(
                (it.context as AppCompatActivity).supportFragmentManager,
                "dialog"
            )
        }

        binding.mangaSearchBar.setEndIconOnClickListener {
            binding.mangaSearchBarText.performClick()
        }

        binding.mangaGenreImage.loadImage("https://bit.ly/31bsIHq")
        binding.mangaTopScoreImage.loadImage("https://bit.ly/2ZGfcuG")

        binding.mangaGenre.setOnClickListener {
            ContextCompat.startActivity(
                it.context,
                Intent(it.context, GenreActivity::class.java).putExtra("type", "MANGA"),
                null
            )
        }
        binding.mangaTopScore.setOnClickListener {
            ContextCompat.startActivity(
                it.context,
                Intent(it.context, SearchActivity::class.java).putExtra("type", "MANGA")
                    .putExtra("sortBy", "Score"),
                null
            )
        }
        if (ready.value == false)
            ready.postValue(true)
    }

    override fun getItemCount(): Int = 1

    fun updateHeight() {
        trendingViewPager!!.updateLayoutParams { height += statusBarHeight }
    }

    fun updateTrending(adapter: MediaAdapter) {
        binding.mangaTrendingProgressBar.visibility = View.GONE
        binding.mangaTrendingViewPager.adapter = adapter
        binding.mangaTrendingViewPager.offscreenPageLimit = 3
        binding.mangaTrendingViewPager.getChildAt(0).overScrollMode = RecyclerView.OVER_SCROLL_NEVER
        binding.mangaTrendingViewPager.setPageTransformer(MediaPageTransformer())
        trendHandler = Handler(Looper.getMainLooper())
        trendRun = Runnable {
            binding.mangaTrendingViewPager.currentItem =
                binding.mangaTrendingViewPager.currentItem + 1
        }
        binding.mangaTrendingViewPager.registerOnPageChangeCallback(
            object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    trendHandler!!.removeCallbacks(trendRun)
                    trendHandler!!.postDelayed(trendRun, 4000)
                }
            }
        )

        binding.mangaTrendingViewPager.layoutAnimation =
            LayoutAnimationController(setSlideIn(uiSettings), 0.25f)
        binding.mangaTitleContainer.startAnimation(setSlideUp(uiSettings))
        binding.mangaListContainer.layoutAnimation =
            LayoutAnimationController(setSlideIn(uiSettings), 0.25f)
    }

    fun updateNovel(adapter: MediaAdapter) {
        binding.mangaNovelProgressBar.visibility = View.GONE
        binding.mangaNovelRecyclerView.adapter = adapter
        binding.mangaNovelRecyclerView.layoutManager = LinearLayoutManager(
            binding.mangaNovelRecyclerView.context,
            LinearLayoutManager.HORIZONTAL,
            false
        )
        binding.mangaNovelRecyclerView.visibility = View.VISIBLE

        binding.mangaNovel.visibility = View.VISIBLE
        binding.mangaNovel.startAnimation(setSlideUp(uiSettings))
        binding.mangaNovelRecyclerView.layoutAnimation =
            LayoutAnimationController(setSlideIn(uiSettings), 0.25f)
        binding.mangaPopular.visibility = View.VISIBLE
        binding.mangaPopular.startAnimation(setSlideUp(uiSettings))
    }

    fun updateAvatar() {
        if (Anilist.avatar != null && ready.value == true) {
            binding.mangaUserAvatar.loadImage(Anilist.avatar)
        }
    }

    inner class MangaPageViewHolder(val binding: ItemMangaPageBinding) :
        RecyclerView.ViewHolder(binding.root)
}
