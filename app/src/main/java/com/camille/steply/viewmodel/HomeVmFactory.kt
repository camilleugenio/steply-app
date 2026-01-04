package com.camille.steply.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.camille.steply.data.location.LocationRepository
import com.google.android.gms.location.LocationServices

class HomeVmFactory(
    private val app: Application
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {

            val fused = LocationServices.getFusedLocationProviderClient(app)
            val repo = LocationRepository(app.applicationContext, fused)

            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(
                    app.applicationContext,
            repo
            ) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class: $modelClass")
    }
}
