package com.codingwithtashi.dailyprayer.viewmodel
import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow

import androidx.lifecycle.*
import com.codingwithtashi.dailyprayer.dao.PrayerDao
import com.codingwithtashi.dailyprayer.model.Prayer
import com.codingwithtashi.dailyprayer.repository.FirebaseRepository
import com.codingwithtashi.dailyprayer.utils.DownloadResponse
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch

/**
 * Created by kunchok on 10/03/2021
 */
@HiltViewModel
class PrayerViewModel @Inject constructor(private var prayerDao: PrayerDao,var context: Context) : ViewModel() {
    val searchQuery = MutableStateFlow(Prayer())
    val prayers = prayerDao.getPrayers().asLiveData()
    val favPrayers = prayerDao.getFavouritePrayer().asLiveData()
    lateinit var selected:LiveData<Prayer>
    val updateCurrentPrayer = MutableLiveData<Prayer>()
    var downloadListener = MutableLiveData<DownloadResponse>();
    lateinit var firebaseRepository: FirebaseRepository;

    init {
        firebaseRepository = FirebaseRepository(context);
    }

    fun select(prayer: Prayer){
        selected= prayerDao.getPrayer(prayer.id).asLiveData()
         //selected.value = prayer;
     }

    //val prayer= prayerDao.getPrayer(2).asLiveData()
    fun updateCurrentPrayer(prayer: Prayer){
        viewModelScope.launch {
            prayerDao.update(prayer)
            Log.e("TAG", "updateCurrentPrayer: Updated", )
        }
    }

    fun downloadPrayer(url: String) {
        downloadListener = firebaseRepository.downloadPrayerAudio(url);
    }

}