package com.codingwithtashi.dailyprayer.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.codingwithtashi.dailyprayer.R
import com.google.android.material.textview.MaterialTextView

class NotificationFragment : Fragment() {
    lateinit var notificationMsg: MaterialTextView;
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view =  inflater.inflate(R.layout.fragment_notification, container, false)
        notificationMsg = view.findViewById(R.id.notification_text);
        notificationMsg.visibility= View.VISIBLE;
        return view;
    }

}