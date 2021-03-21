package com.codingwithtashi.dailyprayer

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import androidx.preference.PreferenceManager
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint

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

        var data = getIntent().getExtras();
        Log.e(TAG, "onCreate: $data", )
        if(data!=null){
            Log.e(TAG, "onCreate: "+data.getString("title"), )
            Log.e(TAG, "onCreate: "+data.getString("body"), )
        }

        initViews();
        setUpNavigation();
        bottomNavListener();
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
            R.id.routine->{
                startActivity(Intent(this,AlarmActivity::class.java));
            }
            R.id.rate_us->{
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")))
                } catch (e: ActivityNotFoundException) {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")))
                }
            }
            R.id.share_app->{
                try {
                    val shareIntent = Intent(Intent.ACTION_SEND)
                    shareIntent.type = "text/plain"
                    shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name))
                    var shareMessage = "\nCheck out this prayer application.\n\n"
                    shareMessage =
                        """
                        ${shareMessage}https://play.google.com/store/apps/details?id=${BuildConfig.APPLICATION_ID}
                        
                        
                        """.trimIndent()
                    shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage)
                    startActivity(Intent.createChooser(shareIntent, "choose one"))
                } catch (e: Exception) {
                    //e.toString();
                }
            }
            R.id.contact_us->{
                val intent = Intent(Intent.ACTION_SENDTO)
                intent.type = "text/plain";
                intent.data = Uri.parse("mailto:developer.kharag@gmail.com");
                //intent.putExtra(Intent.EXTRA_EMAIL, "developer.kharag@gmail.com")
                intent.putExtra(Intent.EXTRA_SUBJECT, "Prayer Request")
                startActivity(Intent.createChooser(intent, "Send Email"))
            }
            R.id.more->{
                val browserIntent =
                    Intent(Intent.ACTION_VIEW, Uri.parse("https://www.facebook.com/techtibet"))
                startActivity(browserIntent)
            }
        }
        return super.onOptionsItemSelected(item)
    }

}