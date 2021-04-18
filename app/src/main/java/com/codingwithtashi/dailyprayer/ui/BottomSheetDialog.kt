package com.codingwithtashi.dailyprayer.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.codingwithtashi.dailyprayer.R
import com.codingwithtashi.dailyprayer.utils.CommonUtils
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton


class BottomSheetDialog : BottomSheetDialogFragment(),View.OnClickListener {
    lateinit var backIcon: ImageView;
    lateinit var shareBtn: MaterialButton;
    lateinit var rateBtn: MaterialButton;
    lateinit var gmailBtn: MaterialButton;
    lateinit var gitHubBtn: MaterialButton;
    lateinit var instaBtn: MaterialButton;
    lateinit var facebookBtn: MaterialButton;

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view: View = inflater.inflate(
            R.layout.bottom_sheet_layout,
            container, false
        )
        backIcon = view.findViewById(R.id.back_icon)
        shareBtn = view.findViewById(R.id.share)
        rateBtn = view.findViewById(R.id.rate)
        gmailBtn = view.findViewById(R.id.gmail)
        gitHubBtn = view.findViewById(R.id.github)
        instaBtn = view.findViewById(R.id.instagram)
        facebookBtn = view.findViewById(R.id.facebook)

        backIcon.setOnClickListener(this);
        shareBtn.setOnClickListener(this);
        rateBtn.setOnClickListener(this);
        gmailBtn.setOnClickListener(this);
        gitHubBtn.setOnClickListener(this);
        instaBtn.setOnClickListener(this);
        facebookBtn.setOnClickListener(this);
        return view;
    }
    override fun onClick(v: View?) {
        when(v?.id){
            R.id.back_icon->{
                dismiss()
            }
            R.id.share->{
                try {
                    val shareIntent = Intent(Intent.ACTION_SEND)
                    shareIntent.type = "text/plain"
                    shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name))
                    var shareMessage = "\nCheck out this prayer application.\n\n"
                    shareMessage =
                        """
                        ${shareMessage}https://play.google.com/store/apps/details?id=com.codingwithtashi.dailyprayer
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
            R.id.gmail->{
                val intent = Intent(Intent.ACTION_SENDTO)
                intent.type = "text/plain";
                intent.data = Uri.parse("mailto:developer.kharag@gmail.com");
                //intent.putExtra(Intent.EXTRA_EMAIL, "developer.kharag@gmail.com")
                intent.putExtra(Intent.EXTRA_SUBJECT, "Prayer Request")
                startActivity(Intent.createChooser(intent, "Send Email"))
            }
            R.id.github->{
                openView(CommonUtils.GITHUB_URL);
            }
            R.id.instagram->{
                openView(CommonUtils.INSTAGRAM_URL);
            }
            R.id.facebook->{
                openView(CommonUtils.FACEBOOK_URL);
            }
        }
    }
    private fun openView(url: String) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
}