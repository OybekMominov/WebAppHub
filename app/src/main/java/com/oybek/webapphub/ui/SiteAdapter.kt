package com.oybek.webapphub.ui

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.oybek.webapphub.R
import com.oybek.webapphub.model.SiteApp
import com.oybek.webapphub.util.FaviconLoader
import com.oybek.webapphub.util.Strings

class SiteAdapter(
    private val items: MutableList<SiteApp>,
    private var lang: String,
    private val onSiteClick: (SiteApp) -> Unit,
    private val onSiteMenuClick: (SiteApp, View) -> Unit
) : RecyclerView.Adapter<SiteAdapter.SiteViewHolder>() {

    fun updateLanguage(newLang: String) {
        lang = newLang
        notifyDataSetChanged()
    }

    fun submitList(newItems: List<SiteApp>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SiteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_site_card, parent, false)
        return SiteViewHolder(view)
    }

    override fun onBindViewHolder(holder: SiteViewHolder, position: Int) {
        val site = items[position]
        holder.tvName.text = site.name
        holder.tvDomain.text = try {
            Uri.parse(site.url).host ?: site.url
        } catch (_: Exception) {
            site.url
        }

        if (site.adBlockEnabled) {
            holder.tvAdBadge.text = Strings.get(lang, "ad_shield")
            holder.tvAdBadge.alpha = 1.0f
        } else {
            holder.tvAdBadge.text = Strings.get(lang, "ad_off")
            holder.tvAdBadge.alpha = 0.5f
        }

        FaviconLoader.loadInto(holder.itemView.context, site, holder.ivIcon)

        holder.itemView.setOnClickListener { onSiteClick(site) }
        holder.btnMore.setOnClickListener { onSiteMenuClick(site, it) }
    }

    override fun getItemCount(): Int = items.size

    class SiteViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvAdBadge: TextView = view.findViewById(R.id.tvAdBadge)
        val btnMore: TextView = view.findViewById(R.id.btnCardMore)
        val ivIcon: ImageView = view.findViewById(R.id.ivSiteIcon)
        val tvName: TextView = view.findViewById(R.id.tvSiteName)
        val tvDomain: TextView = view.findViewById(R.id.tvSiteDomain)
    }
}
