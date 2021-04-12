package com.codingwithtashi.dailyprayer.ui.activity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import com.codingwithtashi.dailyprayer.R

class SocialFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_social, container, false);
        val w = view.findViewById<WebView>(R.id.webView);

//add this to your code
        w?.webViewClient = Callback()
        w?.settings?.javaScriptEnabled = true;
        w?.loadUrl("https://www.facebook.com/techtibet")
        return view;
    }

}
private class Callback : WebViewClient() {
    override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
        return false
    }
}