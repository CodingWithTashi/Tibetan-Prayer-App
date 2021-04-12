package com.codingwithtashi.dailyprayer.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.codingwithtashi.dailyprayer.BuildConfig
import com.codingwithtashi.dailyprayer.R
import com.codingwithtashi.dailyprayer.utils.CommonUtils

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [AboutFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class AboutFragment : Fragment() {


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_about, container, false)
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