package com.codingwithtashi.dailyprayer.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.codingwithtashi.dailyprayer.model.Prayer
import com.codingwithtashi.dailyprayer.repository.FirebaseRepository

/**
 * Created by kunchok on 10/03/2021
 */
class HomeViewModel() : ViewModel() {


    val selected = MutableLiveData<Prayer>()
     fun select(prayer: Prayer){
         selected.value = prayer;
     }

    var currentPrayerMutableLiveData =  MutableLiveData<Prayer>();


    var prayerListMutableLiveData = MutableLiveData<List<Prayer>>()

    var firebaseRepository =  FirebaseRepository();

    init {
        prayerListMutableLiveData = firebaseRepository.prayerListMutableLiveData
    }


}