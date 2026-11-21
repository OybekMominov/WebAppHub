package com.oybek.webapphub.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.oybek.webapphub.model.SiteApp
import com.oybek.webapphub.ui.WebViewActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.abs

object FaviconLoader {

    private val palette = listOf(
        "#1D9BF0", "#10B981", "#F59E0B", "#EF4444",
        "#8B5CF6", "#EC4899", "#06B6D4", "#6366F1"
    )

    fun loadInto(context: Context, site: SiteApp, imageView: ImageView) {
        val fallback = createLetterBitmap(site.name)
        imageView.setImageBitmap(fallback)

        val host = try {
            Uri.parse(site.url).host ?: return
        } catch (_: Exception) {
            return
        }

        val cacheFile = File(context.cacheDir, "fav_${host.hashCode()}.png")
        if (cacheFile.exists()) {
            BitmapFactory.decodeFile(cacheFile.absolutePath)?.let {
                imageView.setImageBitmap(it)
                return
            }
        }

        CoroutineScope(Dispatchers.IO).launch {
            val bmp = downloadFavicon(host)
            if (bmp != null) {
                try {
                    FileOutputStream(cacheFile).use { out ->
                        bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                } catch (_: Exception) {
                }
                withContext(Dispatchers.Main) {
                    imageView.setImageBitmap(bmp)
                }
            }
        }
    }

    private fun downloadFavicon(host: String): Bitmap? {
        val candidates = listOf(
            "https://www.google.com/s2/favicons?domain=$host&sz=128",
            "https://$host/favicon.ico"
        )
        for (iconUrl in candidates) {
            try {
                val conn = URL(iconUrl).openConnection() as HttpURLConnection
                conn.connectTimeout = 4000
                conn.readTimeout = 4000
                conn.instanceFollowRedirects = true
                if (conn.responseCode == 200) {
                    val bmp = BitmapFactory.decodeStream(conn.inputStream)
                    if (bmp != null && bmp.width >= 16) {
                        return bmp
                    }
                }
            } catch (_: Exception) {
            }
        }
        return null
    }

    fun createLetterBitmap(name: String): Bitmap {
        val size = 128
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val colorHex = palette[abs(name.hashCode()) % palette.size]
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(colorHex)
        }
        canvas.drawRoundRect(RectF(0f, 0f, size.toFloat(), size.toFloat()), 32f, 32f, bgPaint)

        val letter = name.trim().firstOrNull()?.uppercase() ?: "W"
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 58f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
        val yPos = (canvas.height / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f)
        canvas.drawText(letter, canvas.width / 2f, yPos, textPaint)
        return bmp
    }

    fun pinSiteShortcut(context: Context, site: SiteApp, lang: String) {
        if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
            Toast.makeText(context, "Kısayol desteklenmiyor", Toast.LENGTH_SHORT).show()
            return
        }
        val launchIntent = Intent(context, WebViewActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra("site_name", site.name)
            putExtra("site_url", site.url)
            putExtra("site_adblock", site.adBlockEnabled)
            putExtra("site_desktop", site.desktopMode)
        }

        val host = try {
            Uri.parse(site.url).host ?: ""
        } catch (_: Exception) {
            ""
        }
        val cacheFile = File(context.cacheDir, "fav_${host.hashCode()}.png")
        val bmp = if (cacheFile.exists()) {
            BitmapFactory.decodeFile(cacheFile.absolutePath) ?: createLetterBitmap(site.name)
        } else {
            createLetterBitmap(site.name)
        }

        val shortcut = ShortcutInfoCompat.Builder(context, "shortcut_${site.id}")
            .setShortLabel(site.name)
            .setLongLabel(site.name)
            .setIcon(IconCompat.createWithBitmap(bmp))
            .setIntent(launchIntent)
            .build()

        ShortcutManagerCompat.requestPinShortcut(context, shortcut, null)
        Toast.makeText(context, Strings.get(lang, "shortcut_added"), Toast.LENGTH_SHORT).show()
    }
}
