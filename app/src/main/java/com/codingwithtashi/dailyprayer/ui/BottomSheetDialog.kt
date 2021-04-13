package com.codingwithtashi.dailyprayer.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.codingwithtashi.dailyprayer.R
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomsheet.BottomSheetDialogFragment


class BottomSheetDialog : BottomSheetDialogFragment(),View.OnClickListener {
    lateinit var backIcon: ImageView;

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
        backIcon.setOnClickListener(this);
        return view;
    }

    override fun onClick(v: View?) {
        when(v?.id){
            R.id.back_icon->{
                dismiss()
            }
        }
    }
}