package com.codingwithtashi.dailyprayer.repository

import android.util.Log
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import com.codingwithtashi.dailyprayer.model.Prayer
import com.codingwithtashi.dailyprayer.utils.Constant
import com.codingwithtashi.dailyprayer.utils.Constant.Companion.FAV_PRAYER_COLLECTION
import com.codingwithtashi.dailyprayer.utils.Response
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException

/**
 * Created by kunchok on 10/03/2021
 */
class FirebaseRepository() {
    var prayerListMutableLiveData = MutableLiveData<List<Prayer>>()
    var firestore: FirebaseFirestore;
    var firebaseAuth: FirebaseAuth;

    init {
        firestore = FirebaseFirestore.getInstance()
        firebaseAuth = FirebaseAuth.getInstance()
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