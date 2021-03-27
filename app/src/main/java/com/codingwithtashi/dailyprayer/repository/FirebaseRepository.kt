package com.codingwithtashi.dailyprayer.repository

import android.content.Context
import android.os.Environment
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.codingwithtashi.dailyprayer.model.Prayer
import com.codingwithtashi.dailyprayer.utils.DownloadResponse
import com.codingwithtashi.dailyprayer.utils.STATUS
import com.google.android.gms.tasks.OnFailureListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.File

/**
 * Created by kunchok on 10/03/2021
 */
class FirebaseRepository(var context: Context) {

    var prayerListMutableLiveData = MutableLiveData<List<Prayer>>()
    lateinit var storage: FirebaseStorage;
    lateinit var storageReference: StorageReference;
    var downloadListener = MutableLiveData<DownloadResponse>();

    init {
        storage = FirebaseStorage.getInstance();
    }
    fun downloadPrayerAudio(url: String): MutableLiveData<DownloadResponse> {
        storageReference = storage.getReferenceFromUrl(url)
        var  directory : File? = null;
        if (Environment.getExternalStorageState() == null) {
            //create new file directory object
            directory = File(Environment.getDataDirectory().toString() + "/prayer_audio/"
            )
            if (!directory.exists()) {
                directory.mkdir()
            }

            // if phone DOES have sd card
        } else if (Environment.getExternalStorageState() != null) {
            directory = File(
                context.getExternalFilesDir(null)
                    .toString() + "/prayer_audio/"
            )
            if (!directory.exists()) {
                directory.mkdir()
            }
        } //
        val localFile = File(directory, "audio.mp3")
        //val localFile = File(rootPath, "${url}.mp3")
        var  storageTask = storageReference.getFile(localFile).addOnSuccessListener {
            downloadListener.postValue(DownloadResponse(STATUS.SUCCESS,100,"",localFile.absolutePath));
            Log.e("TAG", "onOptionsItemSelected: SUCCESS", )
        }.addOnFailureListener(OnFailureListener {
            downloadListener.postValue(DownloadResponse(STATUS.ERROR,0,it.localizedMessage));
            Log.e("TAG", "onOptionsItemSelected: FAILED"+it.localizedMessage, )
        }).addOnProgressListener {

            val progress = 100.0 * it.bytesTransferred / it.totalByteCount
            downloadListener.postValue(DownloadResponse(STATUS.DOWNLOADING,progress.toInt(),""));


            //displaying percentage in progress dialog
            //yourProgressDialog.setMessage("Uploaded " + progress.toInt() + "%...")
        }
        return downloadListener
    }







}