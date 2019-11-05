package com.smedialink.channels.view

import android.annotation.SuppressLint
import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScrollerMiddle
import androidx.recyclerview.widget.RecyclerView
import com.smedialink.channels.view.adapter.ChannelsCategoriesAdapter
import com.smedialink.channels.view.model.ChannelCategoryViewModelMapper
import com.smedialink.channels.view.model.ChannelsViewModel
import com.smedialink.channels.view.model.ChannelsViewModelMapper
import com.smedialink.shop.view.model.MainPageContent
import com.smedialink.storage.domain.model.ChannelCategoryCollection
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import org.telegram.messenger.*
import org.telegram.ui.Components.LayoutHelper
import org.telegram.ui.Components.RecyclerListView

@SuppressLint("ViewConstructor")
class GridChannelsPageView(
        private val disposable: CompositeDisposable,
        context: Context,
        currentAccount: Int
) : FrameLayout(context) {

    private var recycler: RecyclerListView = RecyclerListView(context)
    private var recyclerLayoutManager: LinearLayoutManager
    private val channelsViewModelMapper = ChannelsViewModelMapper()
    private var subscribeDisposable: Disposable? = null
    private var recyclerAdapter: ChannelsCategoriesAdapter =
            ChannelsCategoriesAdapter(currentAccount).apply { setHasStableIds(true) }
    private var channelCategoryViewModelMapper = ChannelCategoryViewModelMapper()
    private var progress: ProgressBar
    private var me = MessagesController.getInstance(UserConfig.selectedAccount)
            .getUser(UserConfig.getInstance(UserConfig.selectedAccount).getClientUserId())

    init {
        recycler.clipToPadding = false
        recycler.setPadding(0, AndroidUtilities.dp(14f), 0, 0)
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

        addView(recycler, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT.toFloat(), Gravity.CENTER, 0f, 0f, 0f, 0f))

        progress = ProgressBar(context)
        progress.isIndeterminate = true
        progress.visibility = View.GONE
        addView(progress, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER))

        subscribeToContent()
    }

    fun subscribeToContent() {
        subscribeDisposable?.dispose()
        ApplicationLoader.channelsManager
                .observeChannelsCategories(me.phone, LocaleController.getInstance().currentLocaleInfo.langCode)
                .map { buildChannelWithCategoriesContent(it) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe {
                    recycler.visibility = View.GONE
                    progress.visibility = View.VISIBLE
                }
                .subscribe({ content ->
                    if (progress.visibility != View.GONE) {
                        progress.visibility = View.GONE
                    }
                    if (recycler.visibility != View.VISIBLE) {
                        recycler.visibility = View.VISIBLE
                    }
                    recyclerAdapter.setContent(content)
                }, { t: Throwable -> t.printStackTrace() })
                .also {
                    disposable.add(it)
                    subscribeDisposable = it
                }
    }

    private fun buildChannelWithCategoriesContent(collections: List<ChannelCategoryCollection>): List<MainPageContent> {
        val result = mutableListOf<MainPageContent>()

        collections.forEach { category ->
            if (category.channels.isNotEmpty()) {
                result.add(channelCategoryViewModelMapper.mapToViewModel(category.category, LocaleController.getInstance().currentLocaleInfo.langCode))
                result.add(ChannelsViewModel(channelsViewModelMapper.mapItems(category.channels)))
            }
        }
        return result
    }

}
