package ani.saikou.app.android.activity

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnticipateInterpolator
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.doOnEnd
import androidx.core.view.doOnAttach
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import ani.saikou.app.android.activity.media.MediaDetailsActivity
import ani.saikou.app.android.fragment.AnimeFragment
import ani.saikou.app.android.fragment.HomeFragment
import ani.saikou.app.android.fragment.LoginFragment
import ani.saikou.app.android.fragment.MangaFragment
import ani.saikou.app.anilist.Anilist
import ani.saikou.app.anilist.AnilistHomeViewModel
import ani.saikou.app.databinding.ActivityMainBinding
import ani.saikou.app.others.AppUpdater
import ani.saikou.app.others.DisableFirebase
import ani.saikou.app.util.*
import ani.saikou.core.model.settings.UserInterfaceSettings
import ani.saikou.core.service.LOG
import ani.saikou.core.service.STORE
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nl.joery.animatedbottombar.AnimatedBottomBar
import java.io.Serializable

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val scope = lifecycleScope
    private var load = false

    private var uiSettings = UserInterfaceSettings()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        DisableFirebase.handle()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.root.isMotionEventSplittingEnabled = false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            splashScreen.setOnExitAnimationListener { splashScreenView ->
                // Create your custom animation.
                val slideUp = ObjectAnimator.ofFloat(
                    splashScreenView,
                    View.TRANSLATION_Y,
                    0f,
                    -splashScreenView.height.toFloat()
                )
                slideUp.interpolator = AnticipateInterpolator()
                slideUp.duration = 200L

                // Call SplashScreenView.remove at the end of your custom animation.
                slideUp.doOnEnd { splashScreenView.remove() }

                // Run your animation.
                slideUp.start()
            }
        }

        binding.root.doOnAttach {
            initActivity(this)
            uiSettings = STORE.loadData("ui_settings") ?: uiSettings
            selectedOption = uiSettings.defaultStartUpTab
            binding.navbarContainer.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = navBarHeight
            }
        }

        if (!isOnline(this)) {
            LOG.notify("No Internet Connection")
            startActivity(Intent(this, NoInternetActivity::class.java))
        } else {
            val model: AnilistHomeViewModel by viewModels()
            model.genres.observe(this) {
                if (it != null) {
                    if (it) {
                        val navbar = binding.navbar
                        bottomBar = navbar
                        navbar.visibility = View.VISIBLE
                        binding.mainProgressBar.visibility = View.GONE
                        val mainViewPager = binding.viewpager
                        mainViewPager.isUserInputEnabled = false
                        mainViewPager.adapter = ViewPagerAdapter(supportFragmentManager, lifecycle)
                        mainViewPager.setPageTransformer(ZoomOutPageTransformer(uiSettings))
                        navbar.setOnTabSelectListener(object :
                            AnimatedBottomBar.OnTabSelectListener {
                            override fun onTabSelected(
                                lastIndex: Int,
                                lastTab: AnimatedBottomBar.Tab?,
                                newIndex: Int,
                                newTab: AnimatedBottomBar.Tab
                            ) {
                                navbar.animate().translationZ(12f).setDuration(200).start()
                                selectedOption = newIndex
                                mainViewPager.setCurrentItem(newIndex, false)
                            }
                        })
                        navbar.selectTabAt(selectedOption)
                        mainViewPager.post { mainViewPager.setCurrentItem(selectedOption, false) }

                        if (loadMedia != null) {
                            scope.launch {
                                val media = withContext(Dispatchers.IO) {
                                    Anilist.query.getMedia(
                                        loadMedia!!,
                                        loadIsMAL
                                    )
                                }
                                if (media != null) {
                                    startActivity(
                                        Intent(
                                            this@MainActivity,
                                            MediaDetailsActivity::class.java
                                        ).putExtra("media", media as Serializable)
                                    )
                                } else {
                                    toastString("Seems like that wasn't found on Anilist.")
                                }
                            }
                        }
                    } else {
                        binding.mainProgressBar.visibility = View.GONE
//                        toastString("Error Loading Tags & Genres.")
                    }
                }
            }
            //Load Data
            if (!load) {
                Anilist.getSavedToken(this)
                scope.launch(Dispatchers.IO) {
                    AppUpdater.check(this@MainActivity)
                    model.genres.postValue(Anilist.query.getGenresAndTags(this@MainActivity))
                }
                load = true
            }
        }
    }

    //Double Tap Back
    private var doubleBackToExitPressedOnce = false
    override fun onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed()
            return
        }
        this.doubleBackToExitPressedOnce = true
        val snackBar =
            Snackbar.make(binding.root, "Please click BACK again to exit", Snackbar.LENGTH_LONG)
        snackBar.view.translationY =
            -navBarHeight.dp - if (binding.navbar.scaleX == 1f) binding.navbar.height - 2f else 0f
        snackBar.show()

        Handler(Looper.getMainLooper()).postDelayed({ doubleBackToExitPressedOnce = false }, 2000)
    }

    //ViewPager
    private class ViewPagerAdapter(fragmentManager: FragmentManager, lifecycle: Lifecycle) :
        FragmentStateAdapter(fragmentManager, lifecycle) {

        override fun getItemCount(): Int = 3

        override fun createFragment(position: Int): Fragment {
            when (position) {
                0 -> return AnimeFragment()
                1 -> return if (Anilist.token != null) HomeFragment() else LoginFragment()
                2 -> return MangaFragment()
            }
            return LoginFragment()
        }
    }

}