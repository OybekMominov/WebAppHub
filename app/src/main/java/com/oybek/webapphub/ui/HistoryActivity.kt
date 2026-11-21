package com.oybek.webapphub.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.oybek.webapphub.data.AppPrefs
import com.oybek.webapphub.databinding.ActivityHistoryBinding
import com.oybek.webapphub.util.Strings

class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding
    private lateinit var prefs: AppPrefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = AppPrefs(this)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val lang = prefs.language
        binding.tvHistoryHeader.text = Strings.get(lang, "history_header")
        binding.btnClearHistory.text = Strings.get(lang, "clear")
        binding.tvHistoryEmpty.text = Strings.get(lang, "history_empty")

        val list = prefs.getHistory()
        binding.rvHistory.layoutManager = LinearLayoutManager(this)
        binding.rvHistory.adapter = HistoryAdapter(list) { item ->
            val intent = Intent(this, WebViewActivity::class.java).apply {
                putExtra("site_name", item.title)
                putExtra("site_url", item.url)
                putExtra("site_adblock", prefs.globalAdBlock)
            }
            startActivity(intent)
        }

        binding.tvHistoryEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE

        binding.btnHistoryBack.setOnClickListener { finish() }
        binding.btnClearHistory.setOnClickListener {
            prefs.clearHistory()
            list.clear()
            binding.rvHistory.adapter?.notifyDataSetChanged()
            binding.tvHistoryEmpty.visibility = View.VISIBLE
        }
    }
}
