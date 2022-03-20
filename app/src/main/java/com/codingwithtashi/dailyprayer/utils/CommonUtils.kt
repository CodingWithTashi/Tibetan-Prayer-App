package com.codingwithtashi.dailyprayer.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.view.View
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.util.*

class CommonUtils {
   companion object {
       var INSTAGRAM_URL = "https://www.instagram.com/kharagkunchok/";
       var FACEBOOK_URL = "https://www.facebook.com/KharagEdition";
       var GITHUB_URL = "https://github.com/CodingWithTashi/Tibetan-Prayer-App";
       var PLAY_STORE_URL = "https://play.google.com/store/apps/details?id=com.codingwithtashi.dailyprayer";
       var PLAY_STORE_DICTIONARY_URL = "https://play.google.com/store/apps/details?id=com.kharagedition.tibetandictionary"
       var PLAY_STORE_CALENDAR_URL = "https://play.google.com/store/apps/details?id=com.codingwithtashi.tibetan_calender"

       fun displaySnackBar(view: View, msg:String){
           Snackbar.make(view,msg,Snackbar.LENGTH_SHORT).show();
       }
       fun displayShortMessage(context: Context,msg: String){
           Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
       }
       fun formatDateFromDate(date:Date): String {
           val format = SimpleDateFormat("hh:mm:a")
           return format.format(date)
       }
       fun isNetworkConnected(context: Context?): Boolean {
           if(context!=null){
               val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

               if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                   val n = cm.activeNetwork
                   if (n != null) {
                       val nc = cm.getNetworkCapabilities(n)
                       //It will check for both wifi and cellular network
                       return nc!!.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) || nc.hasTransport(
                           NetworkCapabilities.TRANSPORT_WIFI)
                   }
                   return false
               } else {
                   val netInfo = cm.activeNetworkInfo
                   return netInfo != null && netInfo.isConnectedOrConnecting
               }
           }
        return  false;
       }

       fun getNumberInTibetan(count: Int?): String {
           var countIntTib="";
           if(count!=null){
               var countInEng = count.toString();
               countInEng.forEach {
                   countIntTib += tibetanCharacter(it)
               }
               return countIntTib;
           }
           return "";

       }

       private fun tibetanCharacter(it: Char): String {
            when(it){
                '0'->
                    return "༠";
                '1'->
                    return "༡";
                '2'->
                    return "༢";
                '3'->
                    return "༣";
                '4'->
                    return "༤";
                '5'->
                    return "༥";
                '6'->
                    return "༦";
                '7'->
                    return "༧"
                '8'->
                    return "༨";
                '9'->
                    return "༩";
                else ->
                    return  "";
            }
       }
   }
}