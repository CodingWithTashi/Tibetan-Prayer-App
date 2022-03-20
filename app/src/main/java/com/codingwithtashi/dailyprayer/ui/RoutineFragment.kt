package com.codingwithtashi.dailyprayer.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.observe
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.codingwithtashi.dailyprayer.R
import com.codingwithtashi.dailyprayer.adapter.ItemClickListener
import com.codingwithtashi.dailyprayer.adapter.PrayerListAdapter
import com.codingwithtashi.dailyprayer.model.Prayer
import com.codingwithtashi.dailyprayer.utils.FragmentType
import com.codingwithtashi.dailyprayer.utils.PrayerPreference
import com.codingwithtashi.dailyprayer.utils.PreferenceConst
import com.codingwithtashi.dailyprayer.viewmodel.PrayerViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.textview.MaterialTextView

class RoutineFragment : Fragment(), ItemClickListener {
    lateinit var noPrayerAddedText: MaterialTextView;
    lateinit var favPrayerRecyclerView: RecyclerView;
    lateinit var circularProgressBar: CircularProgressIndicator;
    private val prayerViewModel: PrayerViewModel by activityViewModels();
    lateinit var favPrayerAdapter: PrayerListAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view =  inflater.inflate(R.layout.fragment_routine, container, false)
        var itemClickListener = this;

        initViews(view);

        circularProgressBar.visibility = VISIBLE

        favPrayerRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext());
            adapter = favPrayerAdapter
            setHasFixedSize(true)

        }
        prayerViewModel.favPrayers.observe(viewLifecycleOwner
        ) {

            if(it.isEmpty()){
                circularProgressBar.visibility = View.GONE
                noPrayerAddedText.visibility = VISIBLE
            }else{
                circularProgressBar.visibility = View.GONE
                noPrayerAddedText.visibility = View.GONE
                favPrayerAdapter.submitList(it)

            }
        }

        return view;
    }

    private fun initViews(view: View) {
        favPrayerRecyclerView = view.findViewById(R.id.fav_prayer_list_container);
        circularProgressBar = view.findViewById(R.id.fav_circular_progress);
        noPrayerAddedText = view.findViewById(R.id.add_prayer);
        favPrayerAdapter=PrayerListAdapter(this, FragmentType.ROUTINE_FRAGMENT)
    }

    override fun onItemClick(prayer: Prayer) {
        prayerViewModel.select(prayer);
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        context?.let { PrayerPreference.setContext(it) };

        val pref = PrayerPreference.getPreference().getBoolean(PreferenceConst.IS_ROUTINE_OPEN_FIRST,false);
        if(!pref){
            PrayerPreference.getPreference().edit().putBoolean(PreferenceConst.IS_ROUTINE_OPEN_FIRST,true).apply()
            context?.let {
                MaterialAlertDialogBuilder(it)
                    .setTitle("Routine Page")
                    .setMessage("These is where your favourite or added routine prayer will display.\n1. Go to detail prayer and click on favourite icon on top to see change\nKharag Edition")
                    .setPositiveButton("Okay") { dialog, which ->
                        dialog.dismiss()
                    }
                    .show()
            }
        }
    }

}