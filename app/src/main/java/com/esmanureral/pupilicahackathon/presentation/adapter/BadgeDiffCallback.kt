package com.esmanureral.pupilicahackathon.presentation

import androidx.recyclerview.widget.DiffUtil
import com.esmanureral.pupilicahackathon.domain.model.BadgeWithState

class BadgeDiffCallback : DiffUtil.ItemCallback<BadgeWithState>() {

    override fun areItemsTheSame(oldItem: BadgeWithState, newItem: BadgeWithState): Boolean {
        return oldItem.badge.id == newItem.badge.id
    }

    override fun areContentsTheSame(oldItem: BadgeWithState, newItem: BadgeWithState): Boolean {
        return oldItem == newItem
    }

    override fun getChangePayload(oldItem: BadgeWithState, newItem: BadgeWithState): Any? {
        return if (oldItem.isUnlocked != newItem.isUnlocked) {
            PAYLOAD_UNLOCK_STATUS_CHANGED
        } else {
            null
        }
    }

    companion object {
        const val PAYLOAD_UNLOCK_STATUS_CHANGED = "unlock_status_changed"
    }
}
