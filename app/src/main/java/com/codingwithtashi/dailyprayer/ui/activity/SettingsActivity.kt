package com.codingwithtashi.dailyprayer.ui.activity

import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.SeekBarPreference
import com.codingwithtashi.dailyprayer.R
import com.google.android.material.appbar.MaterialToolbar


class SettingsActivity : AppCompatActivity() {
    lateinit var materialToolbar:MaterialToolbar
    lateinit var settingLayout:LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        materialToolbar = findViewById(R.id.settings_toolbar)
        settingLayout = findViewById(R.id.setting_layout)
        setBackground();
        setSupportActionBar(materialToolbar)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        if (savedInstanceState == null) {
            supportFragmentManager
                    .beginTransaction()
                    .replace(
                        R.id.settings,
                        SettingsFragment(settingLayout)
                    )
                    .commit()
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun setBackground() {
        val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val brightness = prefs.getString("prayer_brightness_color", "");
        if(!brightness.isNullOrEmpty()){
            settingLayout.setBackgroundColor(Color.parseColor(brightness))

        }else{
            settingLayout.setBackgroundColor(ContextCompat.getColor(this,R.color.purple_700))
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        Log.e("TAG", "onBackPressed: BACK PRESSWS")
        finish()

    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        onBackPressed()
        return super.onOptionsItemSelected(item)
    }

    class SettingsFragment(var settingLayout: LinearLayout) : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
           /* val prefs: SharedPreferences.Editor? = PreferenceManager.getDefaultSharedPreferences(context).edit()
            val brightnessSeekbar: SeekBarPreference? = findPreference("prayer_brightness")
            Log.d("TAG", "onCreatePreferences: ")
            brightnessSeekbar?.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { _, value ->
                    Log.e("TAG", "PREF CHANGED:$value");
                    when(value){
                        0->{
                            prefs?.putString("prayer_brightness_color", "#F6C970")
                            settingLayout.setBackgroundColor(Color.parseColor("F6C970"))
                        }
                        1->{
                            prefs?.putString("prayer_brightness_color", "#deb564")
                            settingLayout.setBackgroundColor(Color.parseColor("#deb564"))
                        }
                        2->{
                            prefs?.putString("prayer_brightness_color", "#cfa85d")
                            settingLayout.setBackgroundColor(Color.parseColor("#cfa85d"))
                        }
                        3->{
                            prefs?.putString("prayer_brightness_color", "#bd9851")
                            settingLayout.setBackgroundColor(Color.parseColor("#bd9851"))
                        }
                        4->{
                            prefs?.putString("prayer_brightness_color", "#b3904d")
                            settingLayout.setBackgroundColor(Color.parseColor("#b3904d"))
                        }
                        5->{
                            prefs?.putString("prayer_brightness_color", "#a18145")
                            settingLayout.setBackgroundColor(Color.parseColor("#a18145"))
                        }
                    }
                    prefs?.apply();
                    print(value);
                    true
                }*/

        }
    }
}