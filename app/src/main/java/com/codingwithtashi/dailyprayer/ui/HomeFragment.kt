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
import com.codingwithtashi.dailyprayer.utils.PrayerPreference
import com.codingwithtashi.dailyprayer.utils.PreferenceConst
import com.codingwithtashi.dailyprayer.viewmodel.PrayerViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.textview.MaterialTextView
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeFragment : Fragment(),ItemClickListener {
    private val TAG: String = HomeFragment::class.java.name;
    lateinit var prayerRecyclerView: RecyclerView;
    lateinit var noPrayerFoundText: MaterialTextView;
    lateinit var circularProgressBar: CircularProgressIndicator;
    private val prayerViewModel: PrayerViewModel by activityViewModels();
    lateinit var prayerAdapter: PrayerListAdapter
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view =  inflater.inflate(R.layout.fragment_home, container, false)

        initViews(view);
        circularProgressBar.visibility = VISIBLE

        prayerRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext());
            adapter = prayerAdapter
            setHasFixedSize(true)
        }
        addListener();
        return view;
    }

    private fun addListener() {
        prayerViewModel.prayers.observe(viewLifecycleOwner
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
    }

    private fun initViews(view: View) {
        prayerRecyclerView = view.findViewById(R.id.prayer_list_container);
        circularProgressBar = view.findViewById(R.id.circular_progress);
        noPrayerFoundText = view.findViewById(R.id.no_prayer_found);
        prayerAdapter=PrayerListAdapter(this, FragmentType.HOME_FRAGMENT)
    }

    override fun onItemClick(prayer: Prayer) {
        prayerViewModel.select(prayer);
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        context?.let { PrayerPreference.setContext(it) };

        val pref = PrayerPreference.getPreference().getBoolean(PreferenceConst.IS_APP_OPEN_FIRST,false);
        if(!pref){
            PrayerPreference.getPreference().edit().putBoolean(PreferenceConst.IS_APP_OPEN_FIRST,true).apply()
            context?.let {
                MaterialAlertDialogBuilder(it)
                    .setTitle("Welcome Folks")
                    .setMessage("Thanks for choosing Tibetan Prayer. With this Prayer App \n1. You can add routine\n2. You can count each prayer\n3.You can send prayer request\nTeam Tibetan Prayer")
                    .setPositiveButton("Okay") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
            }
        }
    }

}