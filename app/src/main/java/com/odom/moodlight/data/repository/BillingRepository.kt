package com.odom.moodlight.data.repository

import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BillingRepository @Inject constructor(
    private val securePrefs: SharedPreferences
) {
    companion object {
        const val KEY_IS_PRO = "is_pro"
    }

    private val _isPro = MutableStateFlow(securePrefs.getBoolean(KEY_IS_PRO, false))
    val isPro: StateFlow<Boolean> = _isPro

    fun setProStatus(value: Boolean) {
        securePrefs.edit().putBoolean(KEY_IS_PRO, value).apply()
        _isPro.value = value
    }
}
