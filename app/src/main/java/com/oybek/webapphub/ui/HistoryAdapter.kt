package com.oybek.webapphub.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.oybek.webapphub.R
import com.oybek.webapphub.model.HistoryItem

class HistoryAdapter(
    private val items: MutableList<HistoryItem>,
    private val onClick: (HistoryItem) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.HistoryVH>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryVH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history, parent, false)
        return HistoryVH(v)
    }

    override fun onBindViewHolder(holder: HistoryVH, position: Int) {
        val item = items[position]
        holder.tvTitle.text = item.title
        holder.tvUrl.text = item.url
        holder.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount(): Int = items.size

    class HistoryVH(v: View) : RecyclerView.ViewHolder(v) {
        val tvTitle: TextView = v.findViewById(R.id.tvHistoryTitle)
        val tvUrl: TextView = v.findViewById(R.id.tvHistoryUrl)
    }
}
