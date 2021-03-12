package com.codingwithtashi.dailyprayer.ui

import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Toast
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.codingwithtashi.dailyprayer.R
import com.codingwithtashi.dailyprayer.adapter.ItemClickListener
import com.codingwithtashi.dailyprayer.adapter.PrayerListAdapter
import com.codingwithtashi.dailyprayer.model.Prayer
import com.codingwithtashi.dailyprayer.viewmodel.HomeViewModel
import com.google.android.material.progressindicator.CircularProgressIndicator

class HomeFragment : Fragment(),ItemClickListener {
    private val TAG: String = HomeFragment::class.java.name;
    lateinit var prayerRecyclerView: RecyclerView;
    lateinit var circularProgressBar: CircularProgressIndicator;
    private val homeViewModel: HomeViewModel by activityViewModels();
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view =  inflater.inflate(R.layout.fragment_home, container, false)
        prayerRecyclerView = view.findViewById(R.id.prayer_list_container);
        circularProgressBar = view.findViewById(R.id.circular_progress);
        circularProgressBar.visibility = VISIBLE
        var itemClickListener = this;


        homeViewModel.prayerListMutableLiveData.observe(viewLifecycleOwner,
            {
                prayerRecyclerView.apply {
                    circularProgressBar.visibility = GONE
                    layoutManager = LinearLayoutManager(context);
                    adapter = PrayerListAdapter(it as ArrayList<Prayer>,itemClickListener);

                }
            })

        return view;
    }

    override fun onItemClick(prayer: Prayer) {
        homeViewModel.select(prayer);
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.option_menu,menu);
        return super.onCreateOptionsMenu(menu,inflater);
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.theme->{

            }
        }
        return super.onOptionsItemSelected(item)
    }

}