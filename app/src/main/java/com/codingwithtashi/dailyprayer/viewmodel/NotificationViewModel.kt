package com.codingwithtashi.dailyprayer.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.codingwithtashi.dailyprayer.dao.NotificationDao
import com.codingwithtashi.dailyprayer.dao.PrayerDao
import com.codingwithtashi.dailyprayer.model.Prayer
import com.codingwithtashi.dailyprayer.model.PrayerNotification
import com.codingwithtashi.dailyprayer.utils.CommonUtils
import com.codingwithtashi.dailyprayer.utils.PrayerPreference
import com.codingwithtashi.dailyprayer.utils.PreferenceConst
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList

@HiltViewModel
class NotificationViewModel @Inject constructor(var notificationDao: NotificationDao,var context: Context) : ViewModel() {

    val notifications = notificationDao.getNotifications().asLiveData()

    fun deleteNotification(prayerNotification: PrayerNotification){
        viewModelScope.launch {
            notificationDao.delete(prayerNotification)
            Log.e("TAG", "updateCurrentPrayer: Deleted", )
        }
    }
}