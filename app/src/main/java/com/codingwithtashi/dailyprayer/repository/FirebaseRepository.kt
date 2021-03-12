package com.codingwithtashi.dailyprayer.repository

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.codingwithtashi.dailyprayer.model.Prayer
import com.codingwithtashi.dailyprayer.utils.Constant
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Created by kunchok on 10/03/2021
 */
class FirebaseRepository() {
    var prayerListMutableLiveData = MutableLiveData<List<Prayer>>()
    var firestore: FirebaseFirestore;

    init {
        firestore = FirebaseFirestore.getInstance()
        prayerListMutableLiveData()
    }

    private fun prayerListMutableLiveData() {
        firestore.collection(Constant.PRAYER_COLLECTION).addSnapshotListener { dataList, error ->
            if(error!=null){
                Log.e("TAG", "prayerListMutableLiveData: "+error.localizedMessage )

            }else{
                val prayerList: ArrayList<Prayer> = ArrayList();
                for(data in dataList!!.documentChanges){
                    val prayer: Prayer = data.document.toObject(Prayer::class.java);
                    prayerList.add(prayer);
                }
                prayerListMutableLiveData.postValue(prayerList);
            }
        }
    }
}