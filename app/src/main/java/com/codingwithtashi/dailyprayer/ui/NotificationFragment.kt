package com.codingwithtashi.dailyprayer.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.codingwithtashi.dailyprayer.R
import com.codingwithtashi.dailyprayer.adapter.NotificationAdapter
import com.codingwithtashi.dailyprayer.utils.CommonUtils
import com.codingwithtashi.dailyprayer.utils.SwipeToDeleteCallback
import com.google.android.material.textview.MaterialTextView


class NotificationFragment : Fragment() {
    lateinit var notificationMsg: MaterialTextView;
   lateinit var recyclerView:RecyclerView;
    lateinit var relativeLayout:RelativeLayout;
    private val notificationAdapter = NotificationAdapter((1..5).map { "Notification Sample: $it" }.toMutableList())
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view =  inflater.inflate(R.layout.fragment_notification, container, false)
        notificationMsg = view.findViewById(R.id.notification_text);
        relativeLayout = view.findViewById(R.id.notification_container)
        notificationMsg.visibility= View.VISIBLE;


        //
        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.apply {
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            layoutManager = LinearLayoutManager(context)
            adapter = notificationAdapter
        }


        val swipeHandler = object : SwipeToDeleteCallback(context) {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val adapter = recyclerView.adapter as NotificationAdapter
                adapter.removeAt(viewHolder.adapterPosition)
                CommonUtils.displaySnackBar(relativeLayout,"Deleted");
            }
        }
        val itemTouchHelper = ItemTouchHelper(swipeHandler)
        itemTouchHelper.attachToRecyclerView(recyclerView)

        return view;
    }


}