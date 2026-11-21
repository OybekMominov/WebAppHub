package com.oybek.webapphub.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.PopupMenu
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textfield.TextInputEditText
import com.oybek.webapphub.R
import com.oybek.webapphub.data.AppPrefs
import com.oybek.webapphub.databinding.ActivityMainBinding
import com.oybek.webapphub.model.SiteApp
import com.oybek.webapphub.util.FaviconLoader
import com.oybek.webapphub.util.Strings
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: AppPrefs
    private lateinit var adapter: SiteAdapter
    private var sites = mutableListOf<SiteApp>()

    override fun onCreate(savedInstanceState: Bundle?) {
        prefs = AppPrefs(this)
        AppCompatDelegate.setDefaultNightMode(
            if (prefs.isDarkTheme) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sites = prefs.getSites()
        adapter = SiteAdapter(
            items = sites.toMutableList(),
            lang = prefs.language,
            onSiteClick = { openSite(it) },
            onSiteMenuClick = { site, anchor -> showCardMenu(site, anchor) }
        )

        binding.rvSites.layoutManager = GridLayoutManager(this, 2)
        binding.rvSites.adapter = adapter

        setupListeners()
        applyLocalization()
    }

    override fun onResume() {
        super.onResume()
        sites = prefs.getSites()
        adapter.submitList(sites)
        adapter.updateLanguage(prefs.language)
        applyLocalization()
    }

    private fun applyLocalization() {
        val lang = prefs.language
        binding.btnLangToggle.text = lang.uppercase()
        binding.btnHistory.text = Strings.get(lang, "history")
        binding.btnSettings.text = Strings.get(lang, "settings")
        binding.etQuickUrl.hint = Strings.get(lang, "search_hint")
        binding.tvSectionSites.text = Strings.get(lang, "my_apps")
        binding.tvEmptySites.text = Strings.get(lang, "empty_sites")
        binding.tvEmptySites.visibility = if (sites.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun setupListeners() {
        binding.btnLangToggle.setOnClickListener {
            prefs.language = if (prefs.language == "tr") "uz" else "tr"
            adapter.updateLanguage(prefs.language)
            applyLocalization()
        }

        binding.btnHistory.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }

        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        binding.btnQuickGo.setOnClickListener {
            launchQuickInput()
        }

        binding.etQuickUrl.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_DONE) {
                launchQuickInput()
                true
            } else {
                false
            }
        }

        binding.btnAddSite.setOnClickListener {
            showAddEditDialog(null)
        }
    }

    private fun launchQuickInput() {
        val raw = binding.etQuickUrl.text?.toString()?.trim().orEmpty()
        if (raw.isBlank()) return
        val formatted = formatUrlOrSearch(raw)
        binding.etQuickUrl.setText("")
        openSite(
            SiteApp(
                id = "temp",
                name = raw,
                url = formatted,
                adBlockEnabled = prefs.globalAdBlock,
                desktopMode = false
            )
        )
    }

    private fun formatUrlOrSearch(input: String): String {
        if (input.startsWith("http://") || input.startsWith("https://")) return input
        if (input.contains(".") && !input.contains(" ")) return "https://$input"
        return "https://www.google.com/search?q=${java.net.URLEncoder.encode(input, "UTF-8")}"
    }

    private fun openSite(site: SiteApp) {
        val intent = Intent(this, WebViewActivity::class.java).apply {
            putExtra("site_id", site.id)
            putExtra("site_name", site.name)
            putExtra("site_url", site.url)
            putExtra("site_adblock", site.adBlockEnabled)
            putExtra("site_desktop", site.desktopMode)
        }
        startActivity(intent)
    }

    private fun showCardMenu(site: SiteApp, anchor: View) {
        val lang = prefs.language
        val popup = PopupMenu(this, anchor)
        popup.menu.add(0, 1, 0, Strings.get(lang, "edit"))
        popup.menu.add(0, 2, 1, Strings.get(lang, "add_shortcut"))
        popup.menu.add(0, 3, 2, Strings.get(lang, "delete"))
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> showAddEditDialog(site)
                2 -> FaviconLoader.pinSiteShortcut(this, site, lang)
                3 -> {
                    sites.removeAll { it.id == site.id }
                    prefs.saveSites(sites)
                    adapter.submitList(sites)
                    applyLocalization()
                }
            }
            true
        }
        popup.show()
    }

    private fun showAddEditDialog(existing: SiteApp?) {
        val lang = prefs.language
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_add_edit_site, null)
        val tvTitle = view.findViewById<TextView>(R.id.tvDialogTitle)
        val etName = view.findViewById<TextInputEditText>(R.id.etSiteName)
        val etUrl = view.findViewById<TextInputEditText>(R.id.etSiteUrl)
        val swAd = view.findViewById<MaterialSwitch>(R.id.swSiteAdBlock)
        val swDesktop = view.findViewById<MaterialSwitch>(R.id.swSiteDesktop)
        val btnShortcut = view.findViewById<MaterialButton>(R.id.btnAddHomeShortcut)

        tvTitle.text = if (existing == null) Strings.get(lang, "add_site") else Strings.get(lang, "edit_site")
        etName.hint = Strings.get(lang, "site_name_hint")
        etUrl.hint = Strings.get(lang, "site_url_hint")
        swAd.text = Strings.get(lang, "ad_blocker_toggle")
        swDesktop.text = Strings.get(lang, "desktop_mode")
        btnShortcut.text = Strings.get(lang, "add_shortcut")

        if (existing != null) {
            etName.setText(existing.name)
            etUrl.setText(existing.url)
            swAd.isChecked = existing.adBlockEnabled
            swDesktop.isChecked = existing.desktopMode
        } else {
            swAd.isChecked = prefs.globalAdBlock
        }

        btnShortcut.setOnClickListener {
            val name = etName.text?.toString()?.trim().orEmpty().ifBlank { "WebApp" }
            val rawUrl = etUrl.text?.toString()?.trim().orEmpty()
            if (rawUrl.isNotBlank()) {
                val url = if (rawUrl.startsWith("http")) rawUrl else "https://$rawUrl"
                val temp = SiteApp(existing?.id ?: UUID.randomUUID().toString(), name, url, swAd.isChecked, swDesktop.isChecked)
                FaviconLoader.pinSiteShortcut(this, temp, lang)
            }
        }

        AlertDialog.Builder(this)
            .setView(view)
            .setPositiveButton(Strings.get(lang, "save")) { _, _ ->
                val rawUrl = etUrl.text?.toString()?.trim().orEmpty()
                if (rawUrl.isBlank()) return@setPositiveButton
                val url = if (rawUrl.startsWith("http://") || rawUrl.startsWith("https://")) rawUrl else "https://$rawUrl"
                val name = etName.text?.toString()?.trim().orEmpty().ifBlank {
                    android.net.Uri.parse(url).host ?: "WebApp"
                }

                if (existing == null) {
                    sites.add(
                        SiteApp(
                            id = UUID.randomUUID().toString(),
                            name = name,
                            url = url,
                            adBlockEnabled = swAd.isChecked,
                            desktopMode = swDesktop.isChecked
                        )
                    )
                } else {
                    val idx = sites.indexOfFirst { it.id == existing.id }
                    if (idx >= 0) {
                        sites[idx] = existing.copy(
                            name = name,
                            url = url,
                            adBlockEnabled = swAd.isChecked,
                            desktopMode = swDesktop.isChecked
                        )
                    }
                }
                prefs.saveSites(sites)
                adapter.submitList(sites)
                applyLocalization()
            }
            .setNegativeButton(Strings.get(lang, "cancel"), null)
            .show()
    }
}
