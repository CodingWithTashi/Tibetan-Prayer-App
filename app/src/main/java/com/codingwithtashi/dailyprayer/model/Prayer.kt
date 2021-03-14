package com.codingwithtashi.dailyprayer.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.DocumentId
import kotlinx.android.parcel.Parcelize

/**
 * Created by kunchok on 10/03/2021
 */
@Entity(tableName = "PRAYER_TABLE")
@Parcelize
data class Prayer(
        @PrimaryKey(autoGenerate = true)
    var id: Int? = null,
    var title:String? = null,
    var content:String? = null,
    var imageUrl: String? = null,
    var isFavourite: Boolean? = false,
    var count: Int? = null
): Parcelable{

}
