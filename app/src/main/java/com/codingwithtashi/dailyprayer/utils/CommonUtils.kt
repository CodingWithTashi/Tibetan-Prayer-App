package com.codingwithtashi.dailyprayer.utils

import android.content.Context
import android.view.View
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.util.*

class CommonUtils {
   companion object {
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
   }

}