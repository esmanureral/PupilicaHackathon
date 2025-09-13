package com.esmanureral.pupilicahackathon.ui.badge

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.esmanureral.pupilicahackathon.R
import com.esmanureral.pupilicahackathon.databinding.ItemBadgeBinding
import com.esmanureral.pupilicahackathon.domain.Badge

class GameBadgeAdapter(
    private val badges: List<Badge>,
    private val currentScore: Int
) : RecyclerView.Adapter<GameBadgeAdapter.BadgeViewHolder>() {

    class BadgeViewHolder(val binding: ItemBadgeBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BadgeViewHolder {
        return BadgeViewHolder(
            ItemBadgeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: BadgeViewHolder, position: Int) {
        val badge = badges[position]
        bindBadge(holder, badge)
    }

    override fun getItemCount(): Int = badges.size
    private fun bindBadge(holder: BadgeViewHolder, badge: Badge) {
        with(holder.binding) {
            badgeImage.setImageResource(badge.resourceId)
            badgeText.text = badge.name
            scoreValue.text = holder.itemView.context.getString(
                R.string.badge_range_format,
                badge.minValue,
                badge.maxValue
            )
            setItemBackground(root, badge)
        }
    }

    private fun setItemBackground(rootView: ViewGroup, badge: Badge) {
        val highlighted = currentScore in badge.minValue..badge.maxValue
        val bgColorRes = if (highlighted) R.color.correct_answer else R.color.icon_color
        rootView.setBackgroundColor(ContextCompat.getColor(rootView.context, bgColorRes))
    }
}
