package com.oybek.webapphub.util

import android.net.Uri
import android.webkit.WebResourceResponse
import java.io.ByteArrayInputStream

object AdBlocker {
    private val blockedHosts = setOf(
        "doubleclick.net",
        "googleadservices.com",
        "googlesyndication.com",
        "adservice.google.com",
        "pagead2.googlesyndication.com",
        "tpc.googlesyndication.com",
        "adnxs.com",
        "adsrvr.org",
        "taboola.com",
        "outbrain.com",
        "criteo.com",
        "criteo.net",
        "pubmatic.com",
        "rubiconproject.com",
        "openx.net",
        "popads.net",
        "popcash.net",
        "propellerads.com",
        "exoclick.com",
        "juicyads.com",
        "trafficjunky.net",
        "adsterra.com",
        "mgid.com",
        "revcontent.com",
        "mc.yandex.ru",
        "an.yandex.ru",
        "ads.vk.com",
        "connect.facebook.net",
        "scorecardresearch.com",
        "quantserve.com",
        "hotjar.com",
        "chartbeat.com",
        "zedo.com",
        "advertising.com",
        "smartadserver.com",
        "bidswitch.net"
    )

    fun isAdUrl(url: String): Boolean {
        return try {
            val host = Uri.parse(url).host?.lowercase() ?: return false
            blockedHosts.any { blocked ->
                host == blocked || host.endsWith(".$blocked")
            }
        } catch (_: Exception) {
            false
        }
    }

    fun emptyResponse(): WebResourceResponse {
        return WebResourceResponse(
            "text/plain",
            "UTF-8",
            ByteArrayInputStream(ByteArray(0))
        )
    }

    const val COSMETIC_FILTER_JS = """
        (function() {
            try {
                var style = document.createElement('style');
                style.innerHTML = 'ins.adsbygoogle, [id^="div-gpt-ad"], .adsbygoogle, .ad-banner, .ad-container, .popup-ad, iframe[src*="doubleclick"], iframe[src*="googlesyndication"], iframe[src*="yandex"] { display: none !important; height: 0 !important; width: 0 !important; }';
                document.head.appendChild(style);
            } catch(e) {}
        })();
    """
}
