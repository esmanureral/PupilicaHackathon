package com.esmanureral.pupilicahackathon.data.model

import com.google.gson.annotations.SerializedName

data class OnboardingPage(
    @SerializedName("animation_res") val animationRes: Int,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String,
    @SerializedName("background_color") val backgroundColor: Int
)
