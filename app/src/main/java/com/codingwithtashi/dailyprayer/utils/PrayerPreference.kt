package com.codingwithtashi.dailyprayer.utils

import android.content.Context
import android.content.SharedPreferences

class PrayerPreference {

   companion object{
       private lateinit var context: Context
       private val PREFERENCE_NAME = "PrayerPreference"

       fun getPreference(): SharedPreferences {
           return context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
       }
       fun setContext(con: Context) {
           context=con
       }
   }

}
class PreferenceConst{
    companion object{
         val IS_APP_OPEN_FIRST = "IS_APP_OPEN_FIRST"
         val IS_ROUTINE_OPEN_FIRST = "IS_ROUTINE_OPEN_FIRST"
         val IS_NOTIFICATION_OPEN_FIRST = "IS_NOTIFICATION_OPEN_FIRST"
        val IS_FIRST_NOTIFICATION_DISPLAYED = "IS_FIRST_NOTIFICATION_DISPLAYED"


    }
}