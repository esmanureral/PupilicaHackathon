package com.esmanureral.pupilicahackathon.ui.home

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class HomeViewModel : ViewModel() {

    private val _capturedImage = MutableLiveData<Bitmap?>()
    val capturedImage: LiveData<Bitmap?> get() = _capturedImage

    fun setCapturedImage(bitmap: Bitmap) {
        _capturedImage.value = bitmap
    }

    fun processImage(bitmap: Bitmap) {

    }
}
