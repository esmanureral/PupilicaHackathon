package com.esmanureral.pupilicahackathon.domain.model

import com.google.gson.annotations.SerializedName

data class Badge(
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
    val category: BadgeCategory
)
