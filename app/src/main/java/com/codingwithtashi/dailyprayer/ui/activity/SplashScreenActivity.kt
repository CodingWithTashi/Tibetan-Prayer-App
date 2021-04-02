package com.codingwithtashi.dailyprayer.ui.activity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import com.codingwithtashi.dailyprayer.R
import com.codingwithtashi.dailyprayer.dao.PrayerDatabase
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SplashScreenActivity : AppCompatActivity() {
    lateinit var topAnimation :Animation ;
    lateinit var bottomAnimation: Animation;
    lateinit var image: ImageView;
    lateinit var textView: TextView;
    lateinit var progressBar: ProgressBar;
    var  SECOND : Long = 3000;
    @Inject
    lateinit var db: PrayerDatabase;
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        db.prayerDao();
        db.notificationDao();
        topAnimation = AnimationUtils.loadAnimation(this,R.anim.top_animation)
        bottomAnimation = AnimationUtils.loadAnimation(this,R.anim.bottom_animation)
        image = findViewById(R.id.splash_image);
        textView = findViewById(R.id.splash_text);
        progressBar = findViewById(R.id.splash_progress);

        image.animation =topAnimation
        textView.animation = bottomAnimation
        progressBar.animation = bottomAnimation

        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        },SECOND)
    }

}