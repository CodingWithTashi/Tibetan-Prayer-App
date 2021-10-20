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
        }
    }
}