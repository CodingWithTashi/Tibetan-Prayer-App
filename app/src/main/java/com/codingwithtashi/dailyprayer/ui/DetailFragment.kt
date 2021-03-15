package com.codingwithtashi.dailyprayer.ui

import android.animation.Animator
import android.app.Dialog
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import com.bumptech.glide.RequestManager
import com.codingwithtashi.dailyprayer.R
import com.codingwithtashi.dailyprayer.model.Prayer
import com.codingwithtashi.dailyprayer.utils.CommonUtils
import com.codingwithtashi.dailyprayer.viewmodel.NotificationViewModel
import com.codingwithtashi.dailyprayer.viewmodel.PrayerViewModel
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textview.MaterialTextView
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


@AndroidEntryPoint
class DetailFragment : Fragment(), AppBarLayout.OnOffsetChangedListener {
    lateinit var title: MaterialTextView;
    lateinit var content: MaterialTextView;
    lateinit var counter: MaterialTextView;
    lateinit var incrementBtn: MaterialButton;
    lateinit var resetBtn: MaterialButton;
    lateinit var floatingActionButton: ExtendedFloatingActionButton;
    lateinit var fabLayout1: LinearLayout;
    lateinit var fab1: FloatingActionButton;
    lateinit var fabLayout2: LinearLayout;
    lateinit var fabBGLayout: View;
    lateinit var appBarLayout: AppBarLayout;
    lateinit var scrollView: NestedScrollView;
    lateinit var coverImage: ImageView;
    lateinit var collapsingToolbarLayout: CollapsingToolbarLayout;
    lateinit var toolbar: Toolbar;
    lateinit var coordinatorLayout: CoordinatorLayout;
    lateinit var counterLayout: LinearLayout;
    val TAG = DetailFragment::class.simpleName
    var favIcon: MenuItem? = null
    lateinit var currentPrayer: Prayer;
    lateinit var googleSignInClient: GoogleSignInClient;
    val RC_SIGN_IN = 0


    @Inject
    lateinit var glide: RequestManager;

    var offSetValue = 0;

    //get same viewmodel
    private val prayerViewModel: PrayerViewModel by activityViewModels();
    private val notificationViewModel: NotificationViewModel by activityViewModels();

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_detail, container, false)
        initViews(view);
        initListener(view);

        return view;
    }


    private fun initViews(view: View) {
        title = view.findViewById(R.id.title_detail);
        counter = view.findViewById(R.id.counter);
        content = view.findViewById(R.id.content_detail);
        floatingActionButton = view.findViewById(R.id.floating_action_button);
        fabLayout1 = view.findViewById(R.id.fabLayout1);
        fab1 = view.findViewById(R.id.fab1);
        fabLayout2 = view.findViewById(R.id.fabLayout2);
        fabBGLayout = view.findViewById(R.id.fabBGLayout);
        appBarLayout = view.findViewById(R.id.appbar_layout);
        coverImage = view.findViewById(R.id.cover_img);
        collapsingToolbarLayout = view.findViewById(R.id.collapsing_toolbar_layout);
        scrollView = view.findViewById(R.id.scroll_view);
        toolbar = view.findViewById(R.id.detail_toolbar);
        coordinatorLayout = view.findViewById(R.id.detail_container);
        counterLayout = view.findViewById(R.id.counter_layout);
        incrementBtn = view.findViewById(R.id.increment_btn);
        resetBtn = view.findViewById(R.id.reset_btn);
        counterLayout = view.findViewById(R.id.counter_layout);

        toolbar.inflateMenu(R.menu.detail_menu)
        toolbar.setNavigationIcon(R.drawable.ic_baseline_arrow_back_24)
        currentPrayer = Prayer();


    }


    private fun initListener(view: View?) {
        toolbar.setNavigationOnClickListener {
            this.findNavController().popBackStack()
        }
        var menuItem = toolbar.menu;
        favIcon = menuItem.findItem(R.id.favourite);
        toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.favourite -> {
                    if (currentPrayer.isFavourite!!) {
                        currentPrayer.isFavourite = false
                        CommonUtils.displaySnackBar(coordinatorLayout, "Favourite Removed")
                    } else {
                        CommonUtils.displaySnackBar(coordinatorLayout, "Favourite Added")
                        currentPrayer.isFavourite = true
                    }

                    prayerViewModel.updateCurrentPrayer(currentPrayer)
                    Log.e(TAG, "onOptionsItemSelected: ")

                    true
                }
                else ->
                    true
            }
        }

        appBarLayout.addOnOffsetChangedListener(this);

        floatingActionButton.setOnClickListener(View.OnClickListener {

            if (GONE == fabBGLayout.visibility) {
                showFABMenu()
            } else {
                closeFABMenu()
            }
        })

        fabBGLayout.setOnClickListener { closeFABMenu() }

        incrementBtn.setOnClickListener{
            currentPrayer.count = counter.text.toString().toInt()+1
            prayerViewModel.updateCurrentPrayer(currentPrayer)
        }
        resetBtn.setOnClickListener{
            currentPrayer.count = 0;
            prayerViewModel.updateCurrentPrayer(currentPrayer)
        }


        prayerViewModel.selected.observe(viewLifecycleOwner, Observer {
            currentPrayer = it
            title.text = it.title
            content.text = it.content
            collapsingToolbarLayout.title = it.title
            counter.text=it.count.toString()
            glide.load(it.imageUrl).into(coverImage)
            favIcon = menuItem.findItem(R.id.favourite);
            if (it.isFavourite!!) {
                favIcon?.icon = context?.let { it1 ->
                    ContextCompat.getDrawable(
                        it1,
                        R.drawable.ic_baseline_favorite_24_red
                    )
                };

            } else {
                favIcon?.icon = context?.let { it1 ->
                    ContextCompat.getDrawable(
                        it1,
                        R.drawable.ic_baseline_favorite_24
                    )
                };
            }
        })

        content.viewTreeObserver.addOnGlobalLayoutListener(OnGlobalLayoutListener {
            val height: Int = content.getMeasuredHeight()

        })
        fab1.setOnClickListener{
            appBarLayout.setExpanded(false, true)
            val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            val seekBarVal = prefs.getInt("seekbar_example", 3);
            val speed = seekBarVal
           // scrollView.post { scrollView.smoothScrollTo(0, scrollView.getChildAt(0).height, 16000) }
        }

        scrollView.viewTreeObserver.addOnScrollChangedListener {
            val bottom = scrollView.getChildAt(scrollView.childCount - 1).height - scrollView.height - scrollView.scrollY
            if (bottom == 0) {
                appBarLayout.setExpanded(true, true)
                scrollView.fullScroll(View.FOCUS_UP)
                scrollView.smoothScrollTo(0,0,1000)
            }
        }
    }


    private fun closeFABMenu() {
        fabBGLayout.visibility = View.GONE
        //floatingActionButton.animate().rotation(0F)
        fabLayout1.animate().translationY(0f)
        fabLayout2.animate().translationY(0f)
            .setListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animator: Animator) {}
                override fun onAnimationEnd(animator: Animator) {
                    if (View.GONE == fabBGLayout.visibility) {
                        fabLayout1.visibility = View.GONE
                        fabLayout2.visibility = View.GONE
                    }
                }

                override fun onAnimationCancel(animator: Animator) {}
                override fun onAnimationRepeat(animator: Animator) {}
            })
    }

    private fun showFABMenu() {
        fabLayout1.visibility = VISIBLE
        fabLayout2.visibility = VISIBLE
        fabBGLayout.visibility = VISIBLE
        //floatingActionButton.animate().rotationBy(180F)
        if (offSetValue != 0) {
            fabLayout1.animate().translationY(resources.getDimension(R.dimen.standard_75))
            fabLayout2.animate().translationY(resources.getDimension(R.dimen.standard_120))
        } else {
            fabLayout1.animate().translationY(-resources.getDimension(R.dimen.standard_75))
            fabLayout2.animate().translationY(-resources.getDimension(R.dimen.standard_120))
        }

    }

    override fun onOffsetChanged(appBarLayout: AppBarLayout?, verticalOffset: Int) {
        offSetValue = verticalOffset;
        favIcon?.isVisible = verticalOffset == 0
        if(verticalOffset==0)
            counterLayout.visibility= VISIBLE
        else
            counterLayout.visibility=GONE

    }
    private fun showDialog() {
        var count = 0;
        val dialog = activity?.let { Dialog(it) }
        dialog?.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog?.setCancelable(false)
        dialog?.setContentView(R.layout.seek_bar_dialog)
        val selectBtn = dialog?.findViewById(R.id.select_btn) as MaterialButton
        val cancelBtn = dialog?.findViewById(R.id.cancel_btn) as MaterialButton
        val seekBar = dialog.findViewById(R.id.your_dialog_seekbar) as SeekBar
        selectBtn.setOnClickListener {
            dialog.dismiss()
            var speed = count*10000;
            Log.e("TAG", "showDialog: SPPED"+speed, )
            appBarLayout.setExpanded(false, true)
            scrollView.post { scrollView.smoothScrollTo(0, scrollView.getChildAt(0).height, (speed)) }
        }
        cancelBtn.setOnClickListener { dialog.dismiss() }
        dialog.show()
        val yourSeekBarListener: SeekBar.OnSeekBarChangeListener = object :
            SeekBar.OnSeekBarChangeListener {
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                //add code here
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                //add code here
            }

            override fun onProgressChanged(seekBark: SeekBar, progress: Int, fromUser: Boolean) {
                count = progress;
                Log.e("TAG", "onProgressChanged: "+progress, )
            }
        }
        seekBar.setOnSeekBarChangeListener(yourSeekBarListener);


    }

}