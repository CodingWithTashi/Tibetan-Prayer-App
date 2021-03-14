package com.codingwithtashi.dailyprayer.utils

import android.content.Context
import android.view.View
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar

class CommonUtils {
   companion object {
       fun displaySnackBar(view: View, msg:String){
           Snackbar.make(view,msg,Snackbar.LENGTH_SHORT).show();
       }
       fun displayShortMessage(context: Context,msg: String){
           Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
       }
   }

}