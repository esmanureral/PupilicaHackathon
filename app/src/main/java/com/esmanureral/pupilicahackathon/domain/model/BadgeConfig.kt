package com.esmanureral.pupilicahackathon.domain.model

import com.google.gson.annotations.SerializedName

data class BadgeConfig(
    @SerializedName("id")
    val id: String,
    @SerializedName("resource_id")
    val resourceId: Int,
    @SerializedName("name_res_id")
    val nameResId: Int,
    @SerializedName("min_value")
    val minValue: Int,
    @SerializedName("max_value")
    val maxValue: Int,
    @SerializedName("category")
    val category: BadgeCategory,
    @SerializedName("description_res_id")
    val descriptionResId: Int? = null,
    @SerializedName("is_active")
    val isActive: Boolean = true
)

fun BadgeConfig.toBadge(): Badge {
    return Badge(
        id = this.id,
        resourceId = this.resourceId,
        nameResId = this.nameResId,
        minValue = this.minValue,
        maxValue = this.maxValue,
        category = this.category
    )
}
