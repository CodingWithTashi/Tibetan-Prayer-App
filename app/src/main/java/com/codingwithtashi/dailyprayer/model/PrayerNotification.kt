package com.codingwithtashi.dailyprayer.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.android.parcel.Parcelize

@Entity(tableName = "PRAYER_NOTIFICATION_TABLE")
@Parcelize
data class PrayerNotification(
    @PrimaryKey(autoGenerate = true)
    var id: Int? = null,
    var title:String,
    var content: String,
    var time:String,
    var link:String?=null
) : Parcelable