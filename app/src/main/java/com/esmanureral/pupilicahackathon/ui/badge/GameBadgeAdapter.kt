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
        val binding = ItemBadgeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BadgeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BadgeViewHolder, position: Int) {
        val badge = badges[position]
        with(holder.binding) {
            badgeImage.setImageResource(badge.resourceId)
            badgeText.text = badge.name
            scoreValue.text = root.context.getString(
                R.string.badge_range_format,
                badge.minValue,
                badge.maxValue
            )
            val highlighted = currentScore in badge.minValue..badge.maxValue
            val bgColor = if (highlighted) R.color.correct_answer else R.color.icon_color
            root.setBackgroundColor(ContextCompat.getColor(root.context, bgColor))
        }
    }

    override fun getItemCount(): Int = badges.size
}


