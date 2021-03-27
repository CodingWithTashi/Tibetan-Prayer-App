package com.codingwithtashi.dailyprayer

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.TimePicker
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textview.MaterialTextView
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*


class AlarmActivity : AppCompatActivity(),TimePickerDialog.OnTimeSetListener {
    lateinit var setAlarmBtn:MaterialButton;
    lateinit var alarmSwitch: SwitchMaterial;
    lateinit var alarmTitle:MaterialTextView;
    lateinit var alarmDesc:MaterialTextView;
    lateinit var toolbar:MaterialToolbar;
    companion object{
        var NOTIFICATION_CHANNEL_ID = "10001"
        val ALARM1_ID = 10000
    }
    private val default_notification_channel_id = "default"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alarm)
        setAlarmBtn = findViewById(R.id.set_alarm_btn)
        alarmSwitch = findViewById(R.id.switch_alarm)
        alarmTitle = findViewById(R.id.alarm_title)
        alarmDesc = findViewById(R.id.alarm_desc)
        toolbar = findViewById(R.id.toolbar3)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        getDefaultPreference();
        setAlarmBtn.setOnClickListener{
            showTimePickerDialog()
        }
        alarmSwitch.setOnCheckedChangeListener { _, isChecked ->
            val prefs: SharedPreferences.Editor? = PreferenceManager.getDefaultSharedPreferences(
                this
            ).edit()

            if (isChecked) {
                prefs?.putBoolean("is_alarm_on", true);
            } else {
                prefs?.putBoolean("is_alarm_on", false);
            }
            prefs?.apply()
            Log.e("TAG", "onCreate: CALLED SWITCH CHANGE", )
            //triggerAlarm(null, isChecked);
        }
    }

    private fun getDefaultPreference() {

        val prefs: SharedPreferences? = PreferenceManager.getDefaultSharedPreferences(this)
        var alarm_time = prefs?.getString("alarm_time", null);
        var is_alarm_on = prefs?.getBoolean("is_alarm_on", false);
        if(alarm_time!=null){
            alarmTitle.text = alarm_time
            alarmDesc.text = "Alarm set at $alarm_time"
        }
        if (is_alarm_on != null) {
            alarmSwitch.isChecked = is_alarm_on
        }

    }

    override fun onTimeSet(view: TimePicker?, hourOfDay: Int, minute: Int) {
        Log.e("TAG", "onTimeSet: " + hourOfDay)
        var time = "$hourOfDay:$minute"
        var displayTime = getTimeFromString(time);
        alarmTitle.text = displayTime;
        alarmDesc.text = "Alarm set at $displayTime"
        alarmSwitch.isChecked=true
        val prefs: SharedPreferences.Editor? = PreferenceManager.getDefaultSharedPreferences(this).edit()
        prefs?.putString("alarm_time", displayTime);
        prefs?.putBoolean("is_alarm_on", alarmSwitch.isChecked);
        prefs?.apply()
        triggerAlarm(time, alarmSwitch.isChecked)
    }

    private fun triggerAlarm(time: String?, checked: Boolean) {
        if(time!=null){
            scheduleNotification(time, checked)
        }

    }

    private fun scheduleNotification(time: String, checked: Boolean) {

        val calendar = Calendar.getInstance()
        var sdf =  SimpleDateFormat("HH:mm");
        var date  = sdf.parse(time);


        calendar[Calendar.HOUR_OF_DAY] = date.hours
        calendar[Calendar.MINUTE] = date.minutes
        calendar[Calendar.SECOND] = 0
        calendar[Calendar.MILLISECOND] = 0

        if (calendar.time.compareTo(Date()) < 0) calendar.add(Calendar.DAY_OF_MONTH, 1)

        val intent = Intent(applicationContext, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        val alarmManager =
            getSystemService(Context.ALARM_SERVICE) as AlarmManager
        Log.e("TAG", "scheduleNotification: 0"+calendar.timeInMillis, )
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }



    private fun getTimeFromString(time: String): String? {
        var returnString: String? =null
        try {
            val sdf = SimpleDateFormat("HH:mm")
            var date = sdf.parse(time)
            val sdf1 = SimpleDateFormat("hh:mm a")
            returnString = sdf1.format(date)
            System.out.println(returnString)
            System.out.println(SimpleDateFormat("K:mm").format(date))
        } catch (e: ParseException) {
            e.printStackTrace()
        }
        return returnString;
    }

    private fun showTimePickerDialog() {
        val   c = Calendar.getInstance()
        var hour:Int;
        var minute:Int
        val prefs: SharedPreferences? = PreferenceManager.getDefaultSharedPreferences(this)
        var alarm_time = prefs?.getString("alarm_time", null);
        if(alarm_time!=null){
            val sdf = SimpleDateFormat("hh:mm a")
            var date = sdf.parse(alarm_time)
            c.time = date
            hour = c[Calendar.HOUR_OF_DAY]
            minute = c[Calendar.MINUTE]
        }else{
            hour = c[Calendar.HOUR_OF_DAY]
            minute = c[Calendar.MINUTE]
        }

        var dialog = TimePickerDialog(this,R.style.TimePickerTheme, this, hour, minute, false).show()
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        onBackPressed()
        return super.onOptionsItemSelected(item)

    }
    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }

}