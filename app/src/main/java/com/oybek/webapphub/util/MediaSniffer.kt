package com.oybek.webapphub.util

import android.app.DownloadManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.webkit.CookieManager
import android.widget.Toast
import com.oybek.webapphub.model.SniffedMedia

object MediaSniffer {

    private val mediaExtensions = listOf(
        ".mp4", ".m3u8", ".mpd", ".webm", ".mkv", ".avi", ".mov",
        ".mp3", ".m4a", ".aac", ".ogg", ".wav", ".flac",
        ".pdf", ".zip", ".rar", ".7z", ".apk", ".docx", ".xlsx", ".pptx"
    )

    private val videoAudioExtensions = setOf(
        ".mp4", ".m3u8", ".mpd", ".webm", ".mkv", ".avi", ".mov",
        ".mp3", ".m4a", ".aac", ".ogg", ".wav", ".flac"
    )

    fun inspectUrl(url: String): SniffedMedia? {
        val lower = url.lowercase()
        if (lower.startsWith("data:") || lower.startsWith("blob:")) return null
        if (AdBlocker.isAdUrl(url)) return null

        val pathPart = lower.substringBefore("?").substringBefore("#")
        val matchedExt = mediaExtensions.firstOrNull { pathPart.endsWith(it) }
            ?: mediaExtensions.firstOrNull { lower.contains("$it?") || lower.contains("$it&") }
            ?: return null

        val rawName = Uri.parse(url).lastPathSegment?.takeIf { it.isNotBlank() }
            ?: "dosya_${System.currentTimeMillis()}$matchedExt"
        val isMedia = videoAudioExtensions.contains(matchedExt)
        val mime = guessMime(matchedExt)

        return SniffedMedia(
            url = url,
            fileName = rawName,
            mimeType = mime,
            isVideoOrAudio = isMedia
        )
    }

    private fun guessMime(ext: String): String {
        return when (ext) {
            ".mp4", ".webm", ".mkv", ".mov", ".avi" -> "video/*"
            ".m3u8", ".mpd" -> "application/x-mpegURL"
            ".mp3", ".m4a", ".aac", ".ogg", ".wav", ".flac" -> "audio/*"
            ".pdf" -> "application/pdf"
            ".zip", ".rar", ".7z" -> "application/zip"
            ".apk" -> "application/vnd.android.package-archive"
            else -> "*/*"
        }
    }

    fun sendToIdmOrSystem(context: Context, media: SniffedMedia, userAgent: String) {
        val uri = Uri.parse(media.url)
        val cookies = try {
            CookieManager.getInstance().getCookie(media.url)
        } catch (_: Exception) {
            null
        }

        val idmPackages = listOf(
            "idm.internet.download.manager.plus",
            "idm.internet.download.manager",
            "idm.internet.download.manager.adm.lite",
            "com.dv.adm.pay",
            "com.dv.adm"
        )

        val pm = context.packageManager
        for (pkg in idmPackages) {
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage(pkg)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra("title", media.fileName)
                putExtra("User-Agent", userAgent)
                if (!cookies.isNullOrBlank()) {
                    putExtra("Cookie", cookies)
                }
            }
            if (intent.resolveActivity(pm) != null) {
                context.startActivity(intent)
                return
            }
        }

        try {
            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val req = DownloadManager.Request(uri).apply {
                setTitle(media.fileName)
                setDescription(media.url)
                addRequestHeader("User-Agent", userAgent)
                if (!cookies.isNullOrBlank()) {
                    addRequestHeader("Cookie", cookies)
                }
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, media.fileName)
            }
            dm.enqueue(req)
            Toast.makeText(context, "İndirme başlatıldı: ${media.fileName}", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            copyLink(context, media.url, "tr")
        }
    }

    fun openInExternalPlayer(context: Context, media: SniffedMedia) {
        val uri = Uri.parse(media.url)
        val mime = if (media.mimeType.startsWith("audio")) "audio/*" else "video/*"
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mime)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra("title", media.fileName)
        }
        val chooser = Intent.createChooser(intent, media.fileName).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(chooser)
        } catch (_: Exception) {
            Toast.makeText(context, "Harici oynatıcı bulunamadı", Toast.LENGTH_SHORT).show()
        }
    }

    fun copyLink(context: Context, url: String, lang: String) {
        val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cb.setPrimaryClip(ClipData.newPlainText("URL", url))
        Toast.makeText(context, Strings.get(lang, "copied"), Toast.LENGTH_SHORT).show()
    }
}
