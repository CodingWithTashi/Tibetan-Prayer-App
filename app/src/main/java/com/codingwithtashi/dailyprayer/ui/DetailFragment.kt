package com.codingwithtashi.dailyprayer.ui

import android.animation.Animator
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.bumptech.glide.RequestManager
import com.codingwithtashi.dailyprayer.R
import com.codingwithtashi.dailyprayer.viewmodel.HomeViewModel
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.textview.MaterialTextView
import dagger.hilt.EntryPoint
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class DetailFragment : Fragment(),AppBarLayout.OnOffsetChangedListener  {
    lateinit var title : MaterialTextView;
    lateinit var content : MaterialTextView;
    lateinit var floatingActionButton: ExtendedFloatingActionButton;
    lateinit var fabLayout1: LinearLayout;
    lateinit var fabLayout2: LinearLayout;
    lateinit var fabBGLayout: View;
    lateinit var appBarLayout: AppBarLayout;
    lateinit var scrollView: NestedScrollView;
    lateinit var coverImage: ImageView;
    lateinit var collapsingToolbarLayout: CollapsingToolbarLayout;
    @Inject
    lateinit var glide : RequestManager;

    var offSetValue = 0;
    //get same viewmodel
    private val homeViewModel: HomeViewModel by activityViewModels();

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view =  inflater.inflate(R.layout.fragment_detail, container, false)
        title = view.findViewById(R.id.title_detail);
        content = view.findViewById(R.id.content_detail);
        floatingActionButton = view.findViewById(R.id.floating_action_button);
        fabLayout1 = view.findViewById(R.id.fabLayout1);
        fabLayout2 = view.findViewById(R.id.fabLayout2);
        fabBGLayout = view.findViewById(R.id.fabBGLayout);
        appBarLayout = view.findViewById(R.id.appbar_layout);
        coverImage = view.findViewById(R.id.cover_img);
        collapsingToolbarLayout = view.findViewById(R.id.collapsing_toolbar_layout);
        scrollView = view.findViewById(R.id.scroll_view);

        appBarLayout.addOnOffsetChangedListener(this);
        floatingActionButton.setOnClickListener(View.OnClickListener {
            if (View.GONE == fabBGLayout.visibility) {
                showFABMenu()
            } else {
                closeFABMenu()
            }
        })

        fabBGLayout.setOnClickListener { closeFABMenu() }

        homeViewModel.selected.observe(viewLifecycleOwner, Observer {
            glide.load(it.imageUrl).into(coverImage)
            title.text = it.title
            content.text = it.content
            collapsingToolbarLayout.title = it.title

        })

        return view;
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
        fabLayout1.visibility = View.VISIBLE
        fabLayout2.visibility = View.VISIBLE
        fabBGLayout.visibility = View.VISIBLE
        //floatingActionButton.animate().rotationBy(180F)
        if(offSetValue!=0){
            fabLayout1.animate().translationY(resources.getDimension(R.dimen.standard_75))
            fabLayout2.animate().translationY(resources.getDimension(R.dimen.standard_120))
        }else{
            fabLayout1.animate().translationY(-resources.getDimension(R.dimen.standard_75))
            fabLayout2.animate().translationY(-resources.getDimension(R.dimen.standard_120))
        }

    }
    override fun onOffsetChanged(appBarLayout: AppBarLayout?, verticalOffset: Int) {
       offSetValue = verticalOffset;
    }


}