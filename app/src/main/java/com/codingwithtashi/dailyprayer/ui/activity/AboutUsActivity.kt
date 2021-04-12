package com.codingwithtashi.dailyprayer.ui.activity

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import androidx.viewpager.widget.ViewPager
import androidx.viewpager2.widget.ViewPager2
import com.codingwithtashi.dailyprayer.BuildConfig
import com.codingwithtashi.dailyprayer.R
import com.codingwithtashi.dailyprayer.adapter.ViewPagerAdapter
import com.codingwithtashi.dailyprayer.ui.AboutFragment
import com.codingwithtashi.dailyprayer.utils.CommonUtils
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.tabs.TabLayout

class AboutUsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about_us)
        val toolbar: MaterialToolbar = findViewById(R.id.about_toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        //
        var viewPager: ViewPager = findViewById(R.id.viewPager)
        var tabs: TabLayout = findViewById(R.id.tab)
        val adapter = ViewPagerAdapter(supportFragmentManager)
        adapter.addFragment(AboutFragment(), "About Us")
        adapter.addFragment(SocialFragment(), "Social")
        viewPager.adapter = adapter
        tabs.setupWithViewPager(viewPager)
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