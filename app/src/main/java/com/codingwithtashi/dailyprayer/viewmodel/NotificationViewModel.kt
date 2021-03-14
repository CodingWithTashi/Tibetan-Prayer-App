package com.codingwithtashi.dailyprayer.viewmodel

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.codingwithtashi.dailyprayer.dao.PrayerDao
import com.codingwithtashi.dailyprayer.model.Prayer
import com.codingwithtashi.dailyprayer.model.PrayerNotification
import com.codingwithtashi.dailyprayer.utils.CommonUtils
import com.codingwithtashi.dailyprayer.utils.PrayerPreference
import com.codingwithtashi.dailyprayer.utils.PreferenceConst
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList

@HiltViewModel
class NotificationViewModel @Inject constructor(prayerDao: PrayerDao,var context: Context) : ViewModel() {
    val notifications: MutableLiveData<MutableList<PrayerNotification>> = MutableLiveData();
    val notificationList = mutableListOf<PrayerNotification>()
    init {

        //first time
        context.let { PrayerPreference.setContext(it) };

        val pref = PrayerPreference.getPreference().getBoolean(PreferenceConst.IS_FIRST_NOTIFICATION_DISPLAYED,false);
        if(!pref){
            PrayerPreference.getPreference().edit().putBoolean(PreferenceConst.IS_FIRST_NOTIFICATION_DISPLAYED,true).apply()
            val prayerNotification = PrayerNotification("Hello There","Welcome to Daily Prayer,Thanks for using our app",
                CommonUtils.formatDateFromDate(Date()),"")
            notificationList.add(prayerNotification)
            notifications.postValue(notificationList)
        }

    }
}