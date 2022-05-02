package ani.saikou.settings

import android.content.Intent
import android.graphics.drawable.Animatable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.setPadding
import androidx.core.view.updateLayoutParams
import ani.saikou.*
import ani.saikou.parsers.AnimeSources
import ani.saikou.databinding.ActivitySettingsBinding
import ani.saikou.manga.source.MangaSources

class SettingsActivity : AppCompatActivity() {

    lateinit var binding: ActivitySettingsBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initActivity(this)

        binding.settingsVersion.text = getString(R.string.version_current, BuildConfig.VERSION_NAME)

        binding.settingsContainer.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            topMargin = statusBarHeight
            bottomMargin = navBarHeight
        }

        binding.settingsBack.setOnClickListener {
            onBackPressed()
        }

        binding.animeSource.setText(AnimeSources.names[loadData("settings_default_anime_source") ?: 0], false)
        binding.animeSource.setAdapter(ArrayAdapter(this, R.layout.item_dropdown, AnimeSources.names))
        binding.animeSource.setOnItemClickListener { _, _, i, _ ->
            saveData("settings_default_anime_source", i)
            binding.animeSource.clearFocus()
        }

        binding.settingsPlayer.setOnClickListener {
            startActivity(Intent(this, PlayerSettingsActivity::class.java))
        }

        binding.settingsDownloadInSd.isChecked = loadData("sd_dl") ?: false
        binding.settingsDownloadInSd.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                val arrayOfFiles = ContextCompat.getExternalFilesDirs(this, null)
                if (arrayOfFiles.size > 1 && arrayOfFiles[1] != null) {
                    saveData("sd_dl", true)
                } else {
                    binding.settingsDownloadInSd.isChecked = false
                    saveData("sd_dl", false)
                    toastString(getString(R.string.noSdFound))
                }
            } else saveData("sd_dl", false)
        }

        binding.settingsRecentlyListOnly.isChecked = loadData("recently_list_only") ?: false
        binding.settingsRecentlyListOnly.setOnCheckedChangeListener { _, isChecked ->
            saveData("recently_list_only", isChecked)
        }


        binding.mangaSource.setText(MangaSources.names[loadData("settings_default_manga_source") ?: 0], false)
        binding.mangaSource.setAdapter(ArrayAdapter(this, R.layout.item_dropdown, MangaSources.names))
        binding.mangaSource.setOnItemClickListener { _, _, i, _ ->
            saveData("settings_default_manga_source", i)
            binding.mangaSource.clearFocus()
        }

        binding.settingsReader.setOnClickListener {
            toastString("Warks in Porgassss", this)
        }

        val uiSettings: UserInterfaceSettings =
            loadData("ui_settings", toast = false) ?: UserInterfaceSettings().apply { saveData("ui_settings", this) }
        var previous: View = when (uiSettings.darkMode) {
            null  -> binding.settingsUiAuto
            true  -> binding.settingsUiDark
            false -> binding.settingsUiLight
        }
        previous.alpha = 1f
        fun uiTheme(mode: Boolean?, current: View) {
            previous.alpha = 0.33f
            previous = current
            current.alpha = 1f
            uiSettings.darkMode = mode
            saveData("ui_settings", uiSettings)
            Refresh.all()
            finish()
            startActivity(Intent(this, SettingsActivity::class.java))
            initActivity(this)
        }

        binding.settingsUiAuto.setOnClickListener {
            uiTheme(null, it)
        }

        binding.settingsUiLight.setOnClickListener {
            uiTheme(false, it)
        }

        binding.settingsUiDark.setOnClickListener {
            uiTheme(true, it)
        }

        var previousStart: View = when (uiSettings.defaultStartUpTab) {
            0    -> binding.uiSettingsAnime
            1    -> binding.uiSettingsHome
            2    -> binding.uiSettingsManga
            else -> binding.uiSettingsHome
        }
        previousStart.alpha = 1f
        fun uiTheme(mode: Int, current: View) {
            previousStart.alpha = 0.33f
            previousStart = current
            current.alpha = 1f
            uiSettings.defaultStartUpTab = mode
            saveData("ui_settings", uiSettings)
            initActivity(this)
        }

        binding.uiSettingsAnime.setOnClickListener {
            uiTheme(0, it)
        }

        binding.uiSettingsHome.setOnClickListener {
            uiTheme(1, it)
        }

        binding.uiSettingsManga.setOnClickListener {
            uiTheme(2, it)
        }

        binding.settingsShowYt.isChecked = uiSettings.showYtButton
        binding.settingsShowYt.setOnCheckedChangeListener { _, isChecked ->
            uiSettings.showYtButton = isChecked
            saveData("ui_settings", uiSettings)
        }

        var previousEp: View = when (uiSettings.animeDefaultView) {
            0    -> binding.settingsEpList
            1    -> binding.settingsEpGrid
            2    -> binding.settingsEpCompact
            else -> binding.settingsEpList
        }
        previousEp.alpha = 1f
        fun uiEp(mode: Int, current: View) {
            previousEp.alpha = 0.33f
            previousEp = current
            current.alpha = 1f
            uiSettings.animeDefaultView = mode
            saveData("ui_settings", uiSettings)
        }

        binding.settingsEpList.setOnClickListener {
            uiEp(0, it)
        }

        binding.settingsEpGrid.setOnClickListener {
            uiEp(1, it)
        }

        binding.settingsEpCompact.setOnClickListener {
            uiEp(2, it)
        }

        var previousChp: View = when (uiSettings.mangaDefaultView) {
            0    -> binding.settingsChpList
            1    -> binding.settingsChpCompact
            else -> binding.settingsChpList
        }
        previousChp.alpha = 1f
        fun uiChp(mode: Int, current: View) {
            previousChp.alpha = 0.33f
            previousChp = current
            current.alpha = 1f
            uiSettings.mangaDefaultView = mode
            saveData("ui_settings", uiSettings)
        }

        binding.settingsChpList.setOnClickListener {
            uiChp(0, it)
        }

        binding.settingsChpCompact.setOnClickListener {
            uiChp(1, it)
        }


        binding.settingsInfo.setOnClickListener {
            if (binding.settingsInfo.maxLines == 3)
                binding.settingsInfo.maxLines = 100
            else
                binding.settingsInfo.maxLines = 3
        }

        binding.loginDiscord.setOnClickListener {
            openLinkInBrowser(getString(R.string.discord))
        }
        binding.loginTelegram.setOnClickListener {
            openLinkInBrowser(getString(R.string.telegram))
        }
        binding.loginGithub.setOnClickListener {
            openLinkInBrowser(getString(R.string.github))
        }

        binding.settingsUi.setOnClickListener {
            startActivity(Intent(this, UserInterfaceSettingsActivity::class.java))
        }

        (binding.settingsLogo.drawable as Animatable).start()
        val array = resources.getStringArray(R.array.tips)

        binding.settingsLogo.setSafeOnClickListener {
            (binding.settingsLogo.drawable as Animatable).start()
            if (Math.random() * 69 < 42.0)
                toastString(array[(Math.random() * array.size).toInt()], this)
        }

        binding.settingsDev.setOnClickListener {
            DevelopersDialogFragment().show(supportFragmentManager, "dialog")
        }
        binding.settingsDisclaimer.setOnClickListener {
            AlertDialog.Builder(this, R.style.DialogTheme).apply {
                setTitle(R.string.disclaimer)
                setView(ScrollView(context).apply {
                    isVerticalScrollBarEnabled = true
                    addView(TextView(context).apply {
                        setPadding(32f.px)
                        setText(R.string.full_disclaimer)
                    })
                })
                show()
            }
        }
    }
}