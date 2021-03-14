package com.codingwithtashi.dailyprayer.viewmodel
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.*
import com.codingwithtashi.dailyprayer.dao.PrayerDao
import com.codingwithtashi.dailyprayer.model.Prayer
import com.codingwithtashi.dailyprayer.repository.FirebaseRepository
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.launch

/**
 * Created by kunchok on 10/03/2021
 */
@HiltViewModel
class PrayerViewMode @Inject constructor(private var prayerDao: PrayerDao) : ViewModel() {
    val searchQuery = MutableStateFlow(Prayer())
    val prayers = prayerDao.getPrayers().asLiveData()
    val favPrayers = prayerDao.getFavouritePrayer().asLiveData()
    lateinit var selected:LiveData<Prayer>
    val updateCurrentPrayer = MutableLiveData<Prayer>()

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



}