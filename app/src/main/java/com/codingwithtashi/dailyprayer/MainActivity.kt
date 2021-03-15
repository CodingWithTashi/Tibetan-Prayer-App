package com.codingwithtashi.dailyprayer

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceManager
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.EntryPoint
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var bottomNavigationView : BottomNavigationView;
    lateinit var navHostFragment: NavHostFragment;
    lateinit var commonToolbar: MaterialToolbar;
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initViews();
        setUpNavigation();
        bottomNavListener();
        setPreference()
    }

    private fun initViews() {
        commonToolbar = findViewById(R.id.common_toolbar);
        bottomNavigationView =findViewById(R.id.bottom_nav);
        setSupportActionBar(commonToolbar)

    }
    private fun setPreference() {
        val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val name = prefs.getString("signature", null);
        if(name==null){
            commonToolbar.setTitle(R.string.app_name)
        }else
            commonToolbar.title = "Hi $name"

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
            R.id.settings->{
                startActivity(Intent(this,SettingsActivity::class.java));
            }
        }
        return super.onOptionsItemSelected(item)
    }

}