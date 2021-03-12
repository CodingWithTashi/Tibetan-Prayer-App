package com.codingwithtashi.dailyprayer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView


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
    }

    private fun initViews() {
        val view = layoutInflater.inflate(R.layout.tool_bar,null);
        commonToolbar = view.findViewById(R.id.common_toolbar);
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
                bottomNavigationView.visibility = View.GONE
             else
                bottomNavigationView.visibility = View.VISIBLE
        }
    }

}