package com.codingwithtashi.dailyprayer.ui.activity

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import com.codingwithtashi.dailyprayer.BuildConfig
import com.codingwithtashi.dailyprayer.R
import com.codingwithtashi.dailyprayer.utils.CommonUtils
import com.google.android.material.appbar.MaterialToolbar

class AboutUsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about_us)
        val toolbar: MaterialToolbar = findViewById(R.id.about_toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        onBackPressed()
        return super.onOptionsItemSelected(item)

    }
    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }

    fun openPage(view: View) {
        Log.d("TAG", "openPage: CALLED OPENPAE")
        when (view.id){
            R.id.instagram->{
                openView(CommonUtils.INSTAGRAM_URL);
            }
            R.id.facebook->{
                openView(CommonUtils.FACEBOOK_URL);
            }
            R.id.github->{
                openView(CommonUtils.GITHUB_URL);
            }
            R.id.gmail->{
                val intent = Intent(Intent.ACTION_SENDTO)
                intent.type = "text/plain";
                intent.data = Uri.parse("mailto:developer.kharag@gmail.com");
                //intent.putExtra(Intent.EXTRA_EMAIL, "developer.kharag@gmail.com")
                intent.putExtra(Intent.EXTRA_SUBJECT, "Prayer Request")
                startActivity(Intent.createChooser(intent, "Send Email"))
            }
            R.id.share->{
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
            R.id.rate->{
                openView(CommonUtils.PLAY_STORE_URL)
            }
            R.id.paypal->{
                openView(CommonUtils.PAYPAL_URL)
            }
        }
    }

    private fun openView(url: String) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
}