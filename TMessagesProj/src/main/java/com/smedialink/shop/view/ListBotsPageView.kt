package com.smedialink.shop.view

import android.annotation.SuppressLint
import android.content.Context
import android.view.Gravity
import android.widget.FrameLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScrollerMiddle
import androidx.recyclerview.widget.RecyclerView
import com.smedialink.bots.domain.model.BotLanguage
import com.smedialink.bots.domain.model.ShopItem
import com.smedialink.shop.IMeStoreActivity
import com.smedialink.shop.view.adapter.BotsAdapter
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
class ListBotsPageView(
        private val tab: IMeStoreActivity.StoreTab,
        private val disposable: CompositeDisposable,
        var botLanguage: BotLanguage,
        context: Context,
        currentAccount: Int
) : FrameLayout(context) {

    private var recycler: RecyclerListView = RecyclerListView(context)
    private var recyclerLayoutManager: LinearLayoutManager
    private var subscribeDisposable: Disposable? = null
    private var recyclerAdapter: BotsAdapter =
            BotsAdapter(currentAccount).apply { setHasStableIds(true) }

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
        ApplicationLoader.smartBotsManager.getAllBotsObservable(botLanguage, LocaleController.getInstance().currentLocaleInfo.langCode)
                .subscribeOn(Schedulers.io())
                .map { list ->
                    list.filter { item ->
                        when (tab) {
                            IMeStoreActivity.StoreTab.POPULAR -> IMeStoreActivity.isPopular(item.tags)
                            IMeStoreActivity.StoreTab.FREE -> IMeStoreActivity.isFree(item.sku)
                            IMeStoreActivity.StoreTab.MY -> IMeStoreActivity.isMy(item.status)
                            else -> false
                        }
                    }
                }.map {
                    when (tab) {
                        IMeStoreActivity.StoreTab.ALL -> it.sortedByDescending { bot -> bot.installs }
                        IMeStoreActivity.StoreTab.POPULAR -> it.sortedByDescending { bot -> bot.installs }

                        else -> it
                    }
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ items: List<ShopItem> ->
                    recyclerAdapter.setContent(items)
                }, { t: Throwable -> t.printStackTrace() })
                .also {
                    disposable.add(it)
                    subscribeDisposable = it
                }
    }
}
