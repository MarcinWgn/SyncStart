package com.wegrzyn.marcin.ignition.viewModel

import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.ViewModel
import com.wegrzyn.marcin.ignition.Repository
import com.wegrzyn.marcin.ignition.TAG
import com.wegrzyn.marcin.ignition.bt.State
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject


@HiltViewModel
class MainViewModel @Inject constructor(
    val repo: Repository,
    private val sharedPreferences: SharedPreferences
): ViewModel() {

    val btState: StateFlow<State> = repo.btState
    val btGranted = repo.btGranted.asStateFlow()

    fun setDevice(dev: String) {
        sharedPreferences.edit().putString("DEVICE", dev).apply()
    }
    fun getDevice(): String? {
        return sharedPreferences.getString("DEVICE", "OBD")
    }
    
    init {
        Log.d(TAG, "viewmodel start: ")

    }

    fun setBtGranted(granted: Boolean){
        repo.btGranted.value = granted
    }
}