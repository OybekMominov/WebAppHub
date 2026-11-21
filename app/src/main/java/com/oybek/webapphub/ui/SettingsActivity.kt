package com.oybek.webapphub.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.oybek.webapphub.data.AppPrefs
import com.oybek.webapphub.databinding.ActivitySettingsBinding
import com.oybek.webapphub.util.Strings

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefs: AppPrefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = AppPrefs(this)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadValues()
        applyLocalization()

        binding.btnSettingsBack.setOnClickListener { finish() }

        binding.btnSaveSettings.setOnClickListener {
            val selectedLang = if (binding.rbLangUz.isChecked) "uz" else "tr"
            val dark = binding.rbThemeDark.isChecked

            prefs.language = selectedLang
            prefs.isDarkTheme = dark
            prefs.globalAdBlock = binding.swGlobalAdBlock.isChecked
            prefs.backgroundAudioEnabled = binding.rbBgPlayContinue.isChecked
            prefs.videoGesturesEnabled = binding.swVideoGestures.isChecked
            prefs.autoLandscapeFullscreen = binding.swAutoLandscape.isChecked

            AppCompatDelegate.setDefaultNightMode(
                if (dark) AppCompatDelegate.MODE_NIGHT_YES
                else AppCompatDelegate.MODE_NIGHT_NO
            )

            Toast.makeText(this, Strings.get(selectedLang, "saved"), Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun loadValues() {
        if (prefs.language == "uz") binding.rbLangUz.isChecked = true else binding.rbLangTr.isChecked = true
        if (prefs.isDarkTheme) binding.rbThemeDark.isChecked = true else binding.rbThemeLight.isChecked = true
        binding.swGlobalAdBlock.isChecked = prefs.globalAdBlock
        if (prefs.backgroundAudioEnabled) binding.rbBgPlayContinue.isChecked = true else binding.rbBgPlayStop.isChecked = true
        binding.swVideoGestures.isChecked = prefs.videoGesturesEnabled
        binding.swAutoLandscape.isChecked = prefs.autoLandscapeFullscreen
    }

    private fun applyLocalization() {
        val lang = prefs.language
        binding.tvSettingsHeader.text = Strings.get(lang, "settings_header")
        binding.tvGeneralSection.text = Strings.get(lang, "lang_theme_sec")
        binding.tvLangLabel.text = Strings.get(lang, "app_lang")
        binding.tvThemeLabel.text = Strings.get(lang, "app_theme")
        binding.rbThemeDark.text = Strings.get(lang, "dark")
        binding.rbThemeLight.text = Strings.get(lang, "light")
        binding.swGlobalAdBlock.text = Strings.get(lang, "global_adblock")
        binding.tvVideoSection.text = Strings.get(lang, "video_sec")
        binding.rbBgPlayContinue.text = Strings.get(lang, "bg_continue")
        binding.rbBgPlayStop.text = Strings.get(lang, "bg_stop")
        binding.swVideoGestures.text = Strings.get(lang, "video_gestures")
        binding.swAutoLandscape.text = Strings.get(lang, "auto_landscape")
        binding.btnSaveSettings.text = Strings.get(lang, "save_changes")
    }
}
