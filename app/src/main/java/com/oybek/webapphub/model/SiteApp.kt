package com.oybek.webapphub.model

data class SiteApp(
    val id: String,
    val name: String,
    val url: String,
    val adBlockEnabled: Boolean = true,
    val desktopMode: Boolean = false
)
