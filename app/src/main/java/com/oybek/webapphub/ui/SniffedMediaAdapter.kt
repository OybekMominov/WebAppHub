package com.oybek.webapphub.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.oybek.webapphub.R
import com.oybek.webapphub.model.SniffedMedia
import com.oybek.webapphub.util.Strings

class SniffedMediaAdapter(
    private val items: List<SniffedMedia>,
    private val lang: String,
    private val onDownloadIdm: (SniffedMedia) -> Unit,
    private val onOpenPlayer: (SniffedMedia) -> Unit,
    private val onCopyLink: (SniffedMedia) -> Unit
) : RecyclerView.Adapter<SniffedMediaAdapter.MediaVH>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaVH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_sniffed_media, parent, false)
        return MediaVH(v)
    }

    override fun onBindViewHolder(holder: MediaVH, position: Int) {
        val item = items[position]
        holder.tvName.text = item.fileName
        holder.tvUrl.text = item.url

        holder.btnIdm.text = Strings.get(lang, "download_with_idm")
        holder.btnPlayer.text = Strings.get(lang, "open_in_player")
        holder.btnCopy.text = Strings.get(lang, "copy")
        holder.btnPlayer.visibility = if (item.isVideoOrAudio) View.VISIBLE else View.GONE

        holder.btnIdm.setOnClickListener { onDownloadIdm(item) }
        holder.btnPlayer.setOnClickListener { onOpenPlayer(item) }
        holder.btnCopy.setOnClickListener { onCopyLink(item) }
    }

    override fun getItemCount(): Int = items.size

    class MediaVH(v: View) : RecyclerView.ViewHolder(v) {
        val tvName: TextView = v.findViewById(R.id.tvMediaName)
        val tvUrl: TextView = v.findViewById(R.id.tvMediaUrl)
        val btnIdm: MaterialButton = v.findViewById(R.id.btnSendToIdm)
        val btnPlayer: MaterialButton = v.findViewById(R.id.btnOpenPlayer)
        val btnCopy: MaterialButton = v.findViewById(R.id.btnCopyLink)
    }
}
