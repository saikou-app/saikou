package ani.saikou.settings

import android.app.AlertDialog
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.updateLayoutParams
import androidx.core.widget.addTextChangedListener
import ani.saikou.*
import ani.saikou.databinding.ActivityPlayerSettingsBinding
import kotlin.math.roundToInt


class PlayerSettingsActivity : AppCompatActivity() {
    lateinit var binding: ActivityPlayerSettingsBinding
    private val player = "player_settings"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initActivity(this)
        binding.playerSettingsContainer.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            topMargin = statusBarHeight
            bottomMargin = navBarHeight
        }

        val settings = loadData<PlayerSettings>(player, toast = false) ?: PlayerSettings().apply { saveData(player, this) }

        binding.playerSettingsBack.setOnClickListener {
            onBackPressed()
        }

        //Video
        binding.playerSettingsVideoInfo.isChecked = settings.videoInfo
        binding.playerSettingsVideoInfo.setOnCheckedChangeListener { _, isChecked ->
            settings.videoInfo = isChecked
            saveData(player, settings)
        }

        binding.playerSettingsQualityHeight.setText((loadData<Int>("maxHeight", toast = false) ?: 480).toString())
        binding.playerSettingsQualityHeight.addTextChangedListener {
            val height = binding.playerSettingsQualityHeight.text.toString().toIntOrNull()
            saveData("maxHeight", height)
        }
        binding.playerSettingsQualityWidth.setText((loadData<Int>("maxWidth", toast = false) ?: 720).toString())
        binding.playerSettingsQualityWidth.addTextChangedListener {
            val height = binding.playerSettingsQualityWidth.text.toString().toIntOrNull()
            saveData("maxWidth", height)
        }


        val speeds = arrayOf(0.25f, 0.33f, 0.5f, 0.66f, 0.75f, 1f, 1.25f, 1.33f, 1.5f, 1.66f, 1.75f, 2f)
        val cursedSpeeds = arrayOf(1f, 1.25f, 1.5f, 1.75f, 2f, 2.5f, 3f, 4f, 5f, 10f, 25f, 50f)
        var curSpeedArr = if (settings.cursedSpeeds) cursedSpeeds else speeds
        var speedsName = curSpeedArr.map { "${it}x" }.toTypedArray()
        binding.playerSettingsSpeed.text = getString(R.string.default_playback_speed, speedsName[settings.defaultSpeed])
        val speedDialog = AlertDialog.Builder(this, R.style.DialogTheme).setTitle("Default Speed")
        binding.playerSettingsSpeed.setOnClickListener {
            speedDialog.setSingleChoiceItems(speedsName, settings.defaultSpeed) { dialog, i ->
                settings.defaultSpeed = i
                binding.playerSettingsSpeed.text = getString(R.string.default_playback_speed, speedsName[i])
                saveData(player, settings)
                dialog.dismiss()
            }.show()
        }

        binding.playerSettingsCursedSpeeds.isChecked = settings.cursedSpeeds
        binding.playerSettingsCursedSpeeds.setOnCheckedChangeListener { _, isChecked ->
            settings.cursedSpeeds = isChecked
            curSpeedArr = if (settings.cursedSpeeds) cursedSpeeds else speeds
            settings.defaultSpeed = if (settings.cursedSpeeds) 0 else 5
            speedsName = curSpeedArr.map { "${it}x" }.toTypedArray()
            binding.playerSettingsSpeed.text = getString(R.string.default_playback_speed, speedsName[settings.defaultSpeed])
            saveData(player, settings)
        }

        //Auto
        binding.playerSettingsAutoPlay.isChecked = settings.autoPlay
        binding.playerSettingsAutoPlay.setOnCheckedChangeListener { _, isChecked ->
            settings.autoPlay = isChecked
            saveData(player, settings)
        }
        binding.playerSettingsAutoSkip.isChecked = settings.autoSkipFiller
        binding.playerSettingsAutoSkip.setOnCheckedChangeListener { _, isChecked ->
            settings.autoSkipFiller = isChecked
            saveData(player, settings)
        }

        //Update Progress
        binding.playerSettingsAskUpdateProgress.isChecked = settings.askIndividual
        binding.playerSettingsAskUpdateProgress.setOnCheckedChangeListener { _, isChecked ->
            settings.askIndividual = isChecked
            saveData(player, settings)
        }
        binding.playerSettingsAskUpdateHentai.isChecked = settings.updateForH
        binding.playerSettingsAskUpdateHentai.setOnCheckedChangeListener { _, isChecked ->
            settings.updateForH = isChecked
            if (isChecked) toastString(getString(R.string.very_bold))
            saveData(player, settings)
        }
        binding.playerSettingsCompletePercentage.value = (settings.watchPercentage * 100).roundToInt().toFloat()
        binding.playerSettingsCompletePercentage.addOnChangeListener { _, value, _ ->
            settings.watchPercentage = value / 100
            saveData(player, settings)
        }

        //Behaviour
        binding.playerSettingsAlwaysContinue.isChecked = settings.alwaysContinue
        binding.playerSettingsAlwaysContinue.setOnCheckedChangeListener { _, isChecked ->
            settings.alwaysContinue = isChecked
            saveData(player, settings)
        }

        binding.playerSettingsPauseVideo.isChecked = settings.focusPause
        binding.playerSettingsPauseVideo.setOnCheckedChangeListener { _, isChecked ->
            settings.focusPause = isChecked
            saveData(player, settings)
        }
        binding.playerSettingsVerticalGestures.isChecked = settings.gestures
        binding.playerSettingsVerticalGestures.setOnCheckedChangeListener { _, isChecked ->
            settings.gestures = isChecked
            saveData(player, settings)
        }

        binding.playerSettingsDoubleTap.isChecked = settings.doubleTap
        binding.playerSettingsDoubleTap.setOnCheckedChangeListener { _, isChecked ->
            settings.doubleTap = isChecked
            saveData(player, settings)
        }

        binding.playerSettingsSeekTime.value = settings.seekTime.toFloat()
        binding.playerSettingsSeekTime.addOnChangeListener { _, value, _ ->
            settings.seekTime = value.toInt()
            saveData(player, settings)
        }

        binding.exoSkipTime.setText(settings.skipTime.toString())
        binding.exoSkipTime.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                binding.exoSkipTime.clearFocus()
            }
            false
        }
        binding.exoSkipTime.addTextChangedListener {
            val time = binding.exoSkipTime.text.toString().toIntOrNull()
            if (time != null) {
                settings.skipTime = time
                saveData(player, settings)
            }
        }


        binding.playerSettingsPiP.apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                visibility = View.VISIBLE
                isChecked = settings.pip
                setOnCheckedChangeListener { _, isChecked ->
                    settings.pip = isChecked
                    saveData(player, settings)
                }
            } else visibility = View.GONE
        }

        binding.playerSettingsAlwaysMinimize.isChecked = settings.alwaysMinimize
        binding.playerSettingsAlwaysMinimize.setOnCheckedChangeListener { _, isChecked ->
            settings.alwaysMinimize = isChecked
            saveData(player, settings)
        }

        binding.playerSettingsCast.isChecked = settings.cast
        binding.playerSettingsCast.setOnCheckedChangeListener { _, isChecked ->
            settings.cast = isChecked
            saveData(player, settings)
        }

        val resizeModes = arrayOf("Original", "Zoom", "Stretch")
        val resizeDialog = AlertDialog.Builder(this, R.style.DialogTheme).setTitle("Default Resize Mode")
        binding.playerResizeMode.setOnClickListener {
            resizeDialog.setSingleChoiceItems(resizeModes, settings.resize) { dialog, count ->
                settings.resize = count
                saveData(player, settings)
                dialog.dismiss()
            }.show()
        }
        val colorsPrimary = arrayOf("Black","Dark Gray","Gray","Light Gray","White","Red","Yellow","Green","Cyan","Blue","Magenta")
        val primaryColorDialog = AlertDialog.Builder(this, R.style.DialogTheme).setTitle("Primary Sub Color")
        binding.videoSubColorPrimary.setOnClickListener {
            primaryColorDialog.setSingleChoiceItems(colorsPrimary, settings.primaryColor) { dialog, count1 ->
                settings.primaryColor = count1
                saveData(player, settings)
                dialog.dismiss()
            }.show()
        }
        val colorsSecondary = arrayOf("Black","Dark Gray","Gray","Light Gray","White","Red","Yellow","Green","Cyan","Blue","Magenta","Transparent")
        val secondaryColorDialog = AlertDialog.Builder(this, R.style.DialogTheme).setTitle("Outline Sub Color")
        binding.videoSubColorSecondary.setOnClickListener {
            secondaryColorDialog.setSingleChoiceItems(colorsSecondary, settings.secondaryColor) { dialog, count2 ->
                settings.secondaryColor = count2
                saveData(player, settings)
                dialog.dismiss()
            }.show()
        }
        val typesOutline = arrayOf("Outline","Shine","Drop Shadow","None")
        val outlineDialog = AlertDialog.Builder(this, R.style.DialogTheme).setTitle("Outline Type")
        binding.videoSubOutline.setOnClickListener {
            outlineDialog.setSingleChoiceItems(typesOutline, settings.outline) { dialog, count3 ->
                settings.outline = count3
                saveData(player, settings)
                dialog.dismiss()
            }.show()
        }
    }
}