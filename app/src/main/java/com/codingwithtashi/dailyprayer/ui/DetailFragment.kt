package com.codingwithtashi.dailyprayer.ui

import android.animation.Animator
import android.app.ActionBar
import android.app.Dialog
import android.content.SharedPreferences
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
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
import com.codingwithtashi.dailyprayer.utils.Constant.Companion.COMPLETED
import com.codingwithtashi.dailyprayer.utils.Constant.Companion.DOWNLOADING
import com.codingwithtashi.dailyprayer.utils.Constant.Companion.TRY_AGAIN
import com.codingwithtashi.dailyprayer.utils.STATUS
import com.codingwithtashi.dailyprayer.viewmodel.NotificationViewModel
import com.codingwithtashi.dailyprayer.viewmodel.PrayerViewModel
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textview.MaterialTextView
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import javax.inject.Inject


@AndroidEntryPoint
class DetailFragment : Fragment(), AppBarLayout.OnOffsetChangedListener{
    lateinit var title: MaterialTextView;
    lateinit var content: MaterialTextView;
    lateinit var counter: MaterialTextView;
    lateinit var incrementBtn: MaterialButton;
    lateinit var resetBtn: MaterialButton;
    lateinit var floatingActionButton: ExtendedFloatingActionButton;
    lateinit var fabLayout1: LinearLayout;
    lateinit var fab1: FloatingActionButton;
    lateinit var fab2: FloatingActionButton;
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
    var isMediaSourceInit = false;
    lateinit var googleSignInClient: GoogleSignInClient;
    val RC_SIGN_IN = 0
    lateinit var storage: FirebaseStorage;
    lateinit var storageReference: StorageReference;
    lateinit var mediaPlayer: MediaPlayer;


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
        setPreference()
        return view;
    }

    private fun setPreference() {
        val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val fontSize = prefs.getInt("prayer_font", 0);
        content.textSize = fontSize.toFloat() + 22
    }


    private fun initViews(view: View) {
        title = view.findViewById(R.id.title_detail);
        counter = view.findViewById(R.id.counter);
        content = view.findViewById(R.id.content_detail);
        floatingActionButton = view.findViewById(R.id.floating_action_button);
        fabLayout1 = view.findViewById(R.id.fabLayout1);
        fab1 = view.findViewById(R.id.fab1);
        fab2 = view.findViewById(R.id.fab2);
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
        storage = FirebaseStorage.getInstance();
        mediaPlayer = MediaPlayer()

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
                R.id.download_icon -> {
                    showDownloadDialog();
                    true;
                }
                R.id.play_icon -> {
                    mediaPlayer.start()
                    menuItem.findItem(R.id.play_icon).isVisible = false
                    menuItem.findItem(R.id.pause_icon).isVisible = true
                    mediaPlayer.setOnCompletionListener(MediaPlayer.OnCompletionListener { mp ->
                        mp.stop();
                        menuItem.findItem(R.id.play_icon).isVisible = true
                        menuItem.findItem(R.id.pause_icon).isVisible = false
                    })
                    true
                }
                R.id.pause_icon -> {
                    if(mediaPlayer.isPlaying){
                        mediaPlayer.pause()
                        menuItem.findItem(R.id.pause_icon).isVisible = false
                        menuItem.findItem(R.id.play_icon).isVisible = true
                    }

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

        incrementBtn.setOnClickListener {
            currentPrayer.count = counter.text.toString().toInt() + 1
            prayerViewModel.updateCurrentPrayer(currentPrayer)
        }
        resetBtn.setOnClickListener {
            currentPrayer.count = 0;
            prayerViewModel.updateCurrentPrayer(currentPrayer)
        }


        prayerViewModel.selected.observe(viewLifecycleOwner, Observer {

            currentPrayer = it

            menuItem = toolbar.menu;

            if(currentPrayer.downloadUrl.isEmpty()){
                menuItem.findItem(R.id.download_icon).isVisible = false
            }
            else if(currentPrayer.isDownloaded!!) {
                //if(File(currentPrayer.audioPath).exists())
                menuItem.findItem(R.id.download_icon).isVisible = false;
                menuItem.findItem(R.id.play_icon).isVisible = true
            }else{
                menuItem.findItem(R.id.download_icon).isVisible = true
            }

           if(File(currentPrayer.audioPath).exists() && !mediaPlayer.isPlaying && !isMediaSourceInit){
               isMediaSourceInit = true
               mediaPlayer.setDataSource(currentPrayer.audioPath)
               mediaPlayer.prepare();
           }
            if(mediaPlayer.isPlaying){
                menuItem.findItem(R.id.play_icon).isVisible = false
            }

            title.text = it.title
            content.text = it.content
            collapsingToolbarLayout.title = it.title
            counter.text = it.count.toString()
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
        fab1.setOnClickListener {
            appBarLayout.setExpanded(false, true)
            val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            val seekBarVal = prefs.getInt("seekbar_example", 3);
            val seekBarData = 15 - seekBarVal
            Log.e(TAG, "initListener: HEIGHT" + scrollView.getChildAt(0).height,)
            val speed = scrollView.getChildAt(0).height * seekBarData
            scrollView.post { scrollView.smoothScrollTo(0, scrollView.getChildAt(0).height, speed) }
        }
        fab2.setOnClickListener {
            context?.let { it1 ->
                CommonUtils.displayShortMessage(
                    it1,
                    getString(R.string.feature_not_available_now)
                )
            }
        }

        scrollView.viewTreeObserver.addOnScrollChangedListener {
            val bottom =
                scrollView.getChildAt(scrollView.childCount - 1).height - scrollView.height - scrollView.scrollY
            if (bottom == 0) {
                appBarLayout.setExpanded(true, true)
                scrollView.fullScroll(View.FOCUS_UP)
                scrollView.smoothScrollTo(0, 0, 1000)
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
        if (verticalOffset == 0)
            counterLayout.visibility = VISIBLE
        else
            counterLayout.visibility = GONE

    }

    private fun showDownloadDialog() {
        val dialog = activity?.let { Dialog(it) }
        dialog?.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog?.setCancelable(false)
        dialog?.setContentView(R.layout.download_progress_dialog)
        val window: Window? = dialog?.window
        window?.setLayout(ActionBar.LayoutParams.MATCH_PARENT, ActionBar.LayoutParams.WRAP_CONTENT)
        val downlodBtn = dialog?.findViewById(R.id.download_dialog_btn) as MaterialButton
        val cancelBtn = dialog.findViewById(R.id.dialog_cancel_btn) as MaterialButton
        val progressBar = dialog.findViewById(R.id.download_progress_bar) as LinearProgressIndicator
        val currentPosition = dialog.findViewById(R.id.current_position) as TextView
        val prayerName = dialog.findViewById(R.id.prayer_name) as TextView
        val linearLayout = dialog.findViewById(R.id.download_progress_layout) as LinearLayout


        downlodBtn.setOnClickListener {
            prayerName.text = "Downloading Prayer: ${currentPrayer.title}"
            linearLayout.visibility = VISIBLE;
            prayerViewModel.downloadPrayer(currentPrayer.downloadUrl);
            prayerViewModel.downloadListener.observe(viewLifecycleOwner, Observer {
                if (it.status == STATUS.DOWNLOADING) {
                    progressBar.progress = it.progress
                    currentPosition.text = "${it.progress}%"
                    downlodBtn.text = DOWNLOADING
                    downlodBtn.isEnabled = false;
                    Log.e(TAG, "initListener: Downloading....",)
                }
                if (it.status == STATUS.ERROR) {
                    downlodBtn.text = TRY_AGAIN
                    downlodBtn.isEnabled = true;
                    context?.let { it1 -> CommonUtils.displayShortMessage(it1, it.error) }
                    Log.e(TAG, "initListener: Downloading...." + it.error,)
                }
                if (it.status == STATUS.SUCCESS) {
                    Log.e(TAG, "initListener: Downloaded...." + it.error,)
                    downlodBtn.text = COMPLETED
                    downlodBtn.isEnabled = false;
                    currentPrayer.isDownloaded = true;
                    currentPrayer.audioPath = it.data
                    prayerViewModel.updateCurrentPrayer(currentPrayer)
                }
            })
        }

        cancelBtn.setOnClickListener { dialog.dismiss() }
        dialog.show()


    }



    override fun onDestroy() {
        if(mediaPlayer.isPlaying){
            mediaPlayer.stop()
        }
        mediaPlayer.release()
        super.onDestroy()

    }

}