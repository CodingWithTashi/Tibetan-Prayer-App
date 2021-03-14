package com.codingwithtashi.dailyprayer.ui

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.observe
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.codingwithtashi.dailyprayer.R
import com.codingwithtashi.dailyprayer.adapter.ItemClickListener
import com.codingwithtashi.dailyprayer.adapter.PrayerListAdapter
import com.codingwithtashi.dailyprayer.model.Prayer
import com.codingwithtashi.dailyprayer.utils.FragmentType
import com.codingwithtashi.dailyprayer.viewmodel.PrayerViewMode
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.textview.MaterialTextView
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeFragment : Fragment(),ItemClickListener {
    private val TAG: String = HomeFragment::class.java.name;
    lateinit var prayerRecyclerView: RecyclerView;
    lateinit var noPrayerFoundText: MaterialTextView;
    lateinit var circularProgressBar: CircularProgressIndicator;
    private val prayerViewMode: PrayerViewMode by activityViewModels();
    lateinit var prayerAdapter: PrayerListAdapter
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view =  inflater.inflate(R.layout.fragment_home, container, false)
        var itemClickListener = this;

        initViews(view);

        circularProgressBar.visibility = VISIBLE

        prayerRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext());
            adapter = prayerAdapter
            setHasFixedSize(true)

        }
        prayerViewMode.prayers.observe(viewLifecycleOwner
        ) {

            if(it.isEmpty()){
                noPrayerFoundText.visibility = VISIBLE
                noPrayerFoundText.visibility = GONE
            }else{
                circularProgressBar.visibility = GONE
                noPrayerFoundText.visibility = GONE
                prayerAdapter.submitList(it)

            }
        }
        return view;
    }

    private fun initViews(view: View) {
        prayerRecyclerView = view.findViewById(R.id.prayer_list_container);
        circularProgressBar = view.findViewById(R.id.circular_progress);
        noPrayerFoundText = view.findViewById(R.id.no_prayer_found);
        prayerAdapter=PrayerListAdapter(this, FragmentType.HOME_FRAGMENT)
    }

    override fun onItemClick(prayer: Prayer) {
        prayerViewMode.select(prayer);
    }

}