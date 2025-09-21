package com.esmanureral.pupilicahackathon.presentation.badge

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ListAdapter
import com.esmanureral.pupilicahackathon.R
import com.esmanureral.pupilicahackathon.data.model.BadgeWithState
import com.esmanureral.pupilicahackathon.databinding.ItemBadgeBinding

class GameBadgeAdapter :
    ListAdapter<BadgeWithState, GameBadgeAdapter.BadgeViewHolder>(BadgeDiffCallback()) {

    class BadgeViewHolder(val binding: ItemBadgeBinding) :
        androidx.recyclerview.widget.RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BadgeViewHolder {
        return BadgeViewHolder(
            ItemBadgeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: BadgeViewHolder, position: Int) {
        val badgeWithState = getItem(position)
        bindBadge(holder, badgeWithState)
    }

    override fun onBindViewHolder(
        holder: BadgeViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
        } else {
            val badgeWithState = getItem(position)
            when {
                payloads.contains(BadgeDiffCallback.PAYLOAD_UNLOCK_STATUS_CHANGED) -> {
                    updateItemBackground(holder.itemView, badgeWithState.isUnlocked)
                }

                else -> {
                    super.onBindViewHolder(holder, position, payloads)
                }
            }
        }
    }

    private fun bindBadge(holder: BadgeViewHolder, badgeWithState: BadgeWithState) {
        val badge = badgeWithState.badge
        with(holder.binding) {
            badgeImage.setImageResource(badge.resourceId)
            badgeText.text = holder.itemView.context.getString(badge.nameResId)
            scoreValue.text = holder.itemView.context.getString(
                R.string.badge_range_format,
                badge.minValue,
                badge.maxValue
            )
            updateItemBackground(holder.itemView, badgeWithState.isUnlocked)
        }
    }

    private fun updateItemBackground(view: View, isUnlocked: Boolean) {
        val bgColorRes = if (isUnlocked) R.color.green_light else R.color.smile_background_light
        view.setBackgroundColor(ContextCompat.getColor(view.context, bgColorRes))
        
        if (view is com.google.android.material.card.MaterialCardView) {
            view.setCardBackgroundColor(ContextCompat.getColor(view.context, bgColorRes))
        }
    }
}
