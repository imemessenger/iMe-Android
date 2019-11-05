package com.smedialink.channels.view

import android.annotation.SuppressLint
import android.content.Context
import android.view.Gravity
import android.widget.FrameLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScrollerMiddle
import androidx.recyclerview.widget.RecyclerView
import com.smedialink.channels.view.adapter.ChannelsCategoriesPreviewAdapter
import com.smedialink.channels.view.model.ChannelCategoryViewModel
import com.smedialink.channels.view.model.ChannelCategoryViewModelMapper
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.LocaleController
import org.telegram.ui.Components.LayoutHelper
import org.telegram.ui.Components.LayoutHelper.MATCH_PARENT
import org.telegram.ui.Components.RecyclerListView

@SuppressLint("ViewConstructor")
class ListCategoriesPageView(
        private val disposable: CompositeDisposable,
        onCategoryClickListener: ChannelsCategoriesPreviewAdapter.OnCategoryClickListener,
        context: Context
) : FrameLayout(context) {

    private var recycler: RecyclerListView = RecyclerListView(context)
    private var recyclerLayoutManager: LinearLayoutManager
    private var subscribeDisposable: Disposable? = null
    private var channelCategoryViewModelMapper= ChannelCategoryViewModelMapper()

    private var recyclerAdapter: ChannelsCategoriesPreviewAdapter =
            ChannelsCategoriesPreviewAdapter(onCategoryClickListener).apply { setHasStableIds(true) }

    init {
        recycler.isVerticalScrollBarEnabled = true
        recycler.itemAnimator = null
        recycler.setInstantClick(true)
        recycler.layoutAnimation = null
        recyclerLayoutManager = object : LinearLayoutManager(context) {
            override fun supportsPredictiveItemAnimations(): Boolean {
                return false
            }

            override fun smoothScrollToPosition(recyclerView: RecyclerView, state: RecyclerView.State, position: Int) {
                val linearSmoothScroller = LinearSmoothScrollerMiddle(recyclerView.context)
                linearSmoothScroller.targetPosition = position
                startSmoothScroll(linearSmoothScroller)
            }
        }
        recyclerLayoutManager.orientation = LinearLayoutManager.VERTICAL
        recycler.layoutManager = recyclerLayoutManager
        recycler.adapter = recyclerAdapter

        addView(recycler, LayoutHelper.createFrame(MATCH_PARENT, MATCH_PARENT, Gravity.CENTER))

        subscribeToContent()
    }

    fun subscribeToContent() {
        subscribeDisposable?.dispose()
        ApplicationLoader.channelsManager.observeCategories()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map { channelCategoryViewModelMapper.mapToViewModel(it,LocaleController.getInstance().currentLocaleInfo.langCode) }
                .subscribe({ items: List<ChannelCategoryViewModel> ->
                    recyclerAdapter.setContent(items)
                }, { t: Throwable -> t.printStackTrace() })
                .also {
                    disposable.add(it)
                    subscribeDisposable=it
                }
    }


}
