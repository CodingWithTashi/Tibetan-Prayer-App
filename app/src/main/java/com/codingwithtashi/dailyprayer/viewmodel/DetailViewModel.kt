package com.codingwithtashi.dailyprayer.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.codingwithtashi.dailyprayer.dao.PrayerDao
import com.codingwithtashi.dailyprayer.model.Prayer
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class DetailViewModel @Inject constructor(prayerDao: PrayerDao) : ViewModel() {
    val currentPrayer: MutableLiveData<Prayer> = MutableLiveData();
    val prayer= prayerDao.getPrayer(1).asLiveData()
}