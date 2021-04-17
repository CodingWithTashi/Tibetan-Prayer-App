package com.codingwithtashi.dailyprayer.ui.activity

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import androidx.preference.PreferenceManager
import com.codingwithtashi.dailyprayer.BuildConfig
import com.codingwithtashi.dailyprayer.R
import com.codingwithtashi.dailyprayer.ui.BottomSheetDialog
import com.codingwithtashi.dailyprayer.utils.CommonUtils
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint
import java.util.*

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    companion object{
        var TAG = MainActivity::class.java.name
    }
    private lateinit var bottomNavigationView : BottomNavigationView;
    lateinit var navHostFragment: NavHostFragment;
    lateinit var commonToolbar: MaterialToolbar;
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //add shortcuts for long press app
        addShortcuts();
        initViews();
        setUpNavigation();
        bottomNavListener();
    }

    private fun addShortcuts() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            val shortcutManager =  getSystemService<ShortcutManager>(ShortcutManager::class.java)

            val sourcecode = ShortcutInfo.Builder(applicationContext, "id1")
                .setShortLabel("Code")
                .setLongLabel("Code")
                .setIcon(Icon.createWithResource(applicationContext, R.drawable.prayer))
                .setIntent(
                    Intent(Intent.ACTION_VIEW,
                        Uri.parse(CommonUtils.GITHUB_URL))
                )
                .build()

            val shortcut = ShortcutInfo.Builder(applicationContext, "id2")
                .setShortLabel("About")
                .setLongLabel("About")
                .setIcon(Icon.createWithResource(applicationContext, R.drawable.prayer))
                .setIntent(
                    Intent(Intent.ACTION_VIEW,
                        Uri.parse(CommonUtils.FACEBOOK_URL))
                )
                .build()

            shortcutManager!!.dynamicShortcuts = listOf(shortcut,sourcecode)
        }
    }

    private fun initViews() {
        commonToolbar = findViewById(R.id.common_toolbar);
        bottomNavigationView =findViewById(R.id.bottom_nav);
        setSupportActionBar(commonToolbar)
    }
    private fun setUpNavigation() {
        navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        NavigationUI.setupWithNavController(bottomNavigationView, navHostFragment.navController)
    }

    private fun bottomNavListener() {
        navHostFragment.navController.addOnDestinationChangedListener { _, destination, _ ->
            if(destination.id == R.id.detailFragment)
            {
                bottomNavigationView.visibility = GONE
                commonToolbar.visibility = GONE
            }else{
                bottomNavigationView.visibility = VISIBLE
                commonToolbar.visibility = VISIBLE
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.option_menu,menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when(item.itemId){
            R.id.settings ->{
                startActivity(Intent(this,
                    SettingsActivity::class.java));
            }
            R.id.routine ->{
                startActivity(Intent(this,
                    AlarmActivity::class.java));
            }

            R.id.about_us ->{
                val sheet =  BottomSheetDialog();
                sheet.show(supportFragmentManager,"ModalBottomSheet");
            }
            R.id.exit ->{
                finishAffinity()
            }
        }
        return super.onOptionsItemSelected(item)
    }


}