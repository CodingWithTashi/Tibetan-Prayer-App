package com.codingwithtashi.dailyprayer.ui.activity
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
import com.codingwithtashi.dailyprayer.AlarmReceiver
import com.codingwithtashi.dailyprayer.R
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textview.MaterialTextView
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import android.content.ActivityNotFoundException
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatCheckBox


class AlarmActivity : AppCompatActivity(),TimePickerDialog.OnTimeSetListener {
    lateinit var setAlarmBtn:MaterialButton;
    lateinit var alarmSwitch: SwitchMaterial;
    lateinit var alarmTitle:MaterialTextView;
    lateinit var alarmDesc:MaterialTextView;
    lateinit var toolbar:MaterialToolbar;
    companion object{
        var NOTIFICATION_CHANNEL_ID = "10001"
        val ALARM1_ID = 10000
        fun scheduleNotification(context: Context, time: String, checked: Boolean) {
            if(checked){
                val calendar = Calendar.getInstance()
                var sdf =  SimpleDateFormat("HH:mm");
                var date  = sdf.parse(time);


                calendar[Calendar.HOUR_OF_DAY] = date.hours
                calendar[Calendar.MINUTE] = date.minutes
                calendar[Calendar.SECOND] = 0
                calendar[Calendar.MILLISECOND] = 0

                if (calendar.time.compareTo(Date()) < 0) calendar.add(Calendar.DAY_OF_MONTH, 1)

                val intent = Intent(context.applicationContext, AlarmReceiver::class.java)
                val pendingIntent = PendingIntent.getBroadcast(
                    context.applicationContext,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT
                )
                val alarmManager =
                    context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                Log.e("TAG", "scheduleNotification: 0"+calendar.timeInMillis, )
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    //AlarmManager.INTERVAL_DAY,
                    pendingIntent
                )
            }
        }
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
            Log.e("TAG", "onCreate: ${ prefs?.putBoolean("is_alarm_on", false)}")
            val getPrefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
            val time = getPrefs.getString("alarm_time_in_hhmm", "");
            if(!time.isNullOrEmpty()){
                triggerAlarm(time, isChecked);
            }
        }
    }

    private fun showDialogToEnableBackGroundNotification() {
        val settings = getSharedPreferences("ProtectedApps", Context.MODE_PRIVATE)
        val skipMessage = settings.getBoolean("skipProtectedAppCheck", false)

        if (!skipMessage) {
            val editor = settings.edit()
            var foundCorrectIntent = false

            foundCorrectIntent = true
            val dontShowAgain = AppCompatCheckBox(this)
            dontShowAgain.text = "Do not show again"
            dontShowAgain.setOnCheckedChangeListener { buttonView, isChecked ->
                editor.putBoolean("skipProtectedAppCheck", isChecked)
                editor.apply()
            }
            AlertDialog.Builder(this)
                .setTitle(Build.MANUFACTURER + " Notification")
                .setMessage(
                    this.getString(R.string.app_name)+" require 'Allow Background Activity' from setting to send notification in background"

                )
                .setView(dontShowAgain)
                .setPositiveButton("Go to settings"
                ) { _, _ ->
                    try {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        intent.data = Uri.parse("package:$packageName")
                        startActivity(intent)
                    } catch (e: ActivityNotFoundException) {
                        val intent = Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS)
                        startActivity(intent)
                    }
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
            if (!foundCorrectIntent) {
                editor.putBoolean("skipProtectedAppCheck", true)
                editor.apply()
            }
        }
    }

    private fun getDefaultPreference() {

        val prefs: SharedPreferences? = PreferenceManager.getDefaultSharedPreferences(this)
        var alarm_time = prefs?.getString("alarm_time", null);
        var is_alarm_on = prefs?.getBoolean("is_alarm_on", false);
        if(alarm_time!=null){
            alarmTitle.text = alarm_time
            alarmDesc.text = "Notification set on $alarm_time"
        }
        if (is_alarm_on != null) {
            alarmSwitch.isChecked = is_alarm_on
        }

    }

    override fun onTimeSet(view: TimePicker?, hourOfDay: Int, minute: Int) {
        Log.e("TAG", "onTimeSet: " + hourOfDay)
        val time = "$hourOfDay:$minute"
        val displayTime = getTimeFromString(time);
        alarmTitle.text = displayTime;
        alarmDesc.text = "Notification set on $displayTime"
        alarmSwitch.isChecked=true
        val prefs: SharedPreferences.Editor? = PreferenceManager.getDefaultSharedPreferences(this).edit()
        prefs?.putString("alarm_time", displayTime);
        prefs?.putString("alarm_time_in_hhmm", time);
        prefs?.putBoolean("is_alarm_on", alarmSwitch.isChecked);
        prefs?.apply()
        triggerAlarm(time, alarmSwitch.isChecked);
        showDialogToEnableBackGroundNotification();
    }

    private fun triggerAlarm(time: String?, checked: Boolean) {
        if(time!=null){
            scheduleNotification(
                this,
                time,
                checked
            )
        }

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
        val hour:Int;
        val minute:Int
        val prefs: SharedPreferences? = PreferenceManager.getDefaultSharedPreferences(this)
        val alarmTime = prefs?.getString("alarm_time", null);
        if(alarmTime!=null){
            val sdf = SimpleDateFormat("hh:mm a")
            val date = sdf.parse(alarmTime)
            c.time = date
            hour = c[Calendar.HOUR_OF_DAY]
            minute = c[Calendar.MINUTE]
        }else{
            hour = c[Calendar.HOUR_OF_DAY]
            minute = c[Calendar.MINUTE]
        }
            TimePickerDialog(this,
            R.style.TimePickerTheme, this, hour, minute, false).show()
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