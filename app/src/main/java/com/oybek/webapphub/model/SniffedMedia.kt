package com.oybek.webapphub.model

data class SniffedMedia(
    val url: String,
    val fileName: String,
    val mimeType: String,
    val isVideoOrAudio: Boolean
)
