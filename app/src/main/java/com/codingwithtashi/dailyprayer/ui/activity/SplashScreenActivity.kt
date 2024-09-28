package com.codingwithtashi.dailyprayer.ui.activity

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import com.codingwithtashi.dailyprayer.R
import com.codingwithtashi.dailyprayer.dao.PrayerDatabase
import com.codingwithtashi.dailyprayer.utils.PrayerPreference
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SplashScreenActivity : AppCompatActivity() {
    lateinit var topAnimation :Animation ;
    lateinit var bottomAnimation: Animation;
    lateinit var image: ImageView;
    lateinit var version: TextView;
    lateinit var splashText: TextView;
    lateinit var authorText: TextView;
    lateinit var progressBar: ProgressBar;
    var  SECOND : Long = 1500;
    lateinit var prefs: SharedPreferences;

    @Inject
    lateinit var db: PrayerDatabase;
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)
        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        db.prayerDao();
        db.notificationDao();
        topAnimation = AnimationUtils.loadAnimation(this,R.anim.top_animation)
        bottomAnimation = AnimationUtils.loadAnimation(this,R.anim.bottom_animation)
        image = findViewById(R.id.splash_image);
        splashText = findViewById(R.id.splash_text);
        version = findViewById(R.id.version);
        progressBar = findViewById(R.id.splash_progress);
        authorText = findViewById(R.id.author_text);
        image.animation =topAnimation
        splashText.animation = bottomAnimation
        authorText.animation = bottomAnimation
        //progressBar.animation = bottomAnimation
        setVersion();
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        },SECOND)
    }

    private fun setVersion() {
        try {
            val versionName: String? = this.packageManager
                .getPackageInfo(this.packageName, 0).versionName
            version.text = "Version: $versionName";
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
    }

    override fun onStart() {
        if(prefs.getBoolean("themeDark",false)){
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }

        super.onStart()
    }

}