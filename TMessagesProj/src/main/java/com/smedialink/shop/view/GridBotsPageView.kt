package com.smedialink.shop.view

import android.annotation.SuppressLint
import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScrollerMiddle
import androidx.recyclerview.widget.RecyclerView
import com.smedialink.bots.domain.model.BotLanguage
import com.smedialink.bots.domain.model.ShopItem
import com.smedialink.bots.domain.model.SmartBotCategory
import com.smedialink.shop.view.adapter.BotsCategoriesAdapter
import com.smedialink.shop.view.model.DisplayingBots
import com.smedialink.shop.view.model.DisplayingBotsCategory
import com.smedialink.shop.view.model.MainPageContent
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.LocaleController
import org.telegram.ui.Components.LayoutHelper
import org.telegram.ui.Components.RecyclerListView

@SuppressLint("ViewConstructor")
class GridBotsPageView(
        private val disposable: CompositeDisposable,
        context: Context,
        var botLanguage: BotLanguage,
        currentAccount: Int
) : FrameLayout(context) {

    private var recycler: RecyclerListView = RecyclerListView(context)
    private var recyclerLayoutManager: LinearLayoutManager
    private var subscribeDisposable: Disposable? = null
    private var recyclerAdapter: BotsCategoriesAdapter =
            BotsCategoriesAdapter(currentAccount).apply { setHasStableIds(true) }

    private var progress: ProgressBar

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
        Observable
                .combineLatest(
                        ApplicationLoader.smartBotsManager.getAllBotsObservable(botLanguage, LocaleController.getInstance().currentLocaleInfo.langCode).distinctUntilChanged(),
                        ApplicationLoader.smartBotsManager.getAvailableCategories(LocaleController.getInstance().currentLocaleInfo.langCode).distinctUntilChanged(),
                        BiFunction { bots: List<ShopItem>, categories: List<SmartBotCategory> ->
                            buildShopContent(bots, categories)
                        })
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

    private fun buildShopContent(bots: List<ShopItem>, categories: List<SmartBotCategory>): List<MainPageContent> {
        val result = mutableListOf<MainPageContent>()

        categories.forEach { category ->
            if (category.tags.isNotEmpty()) {
                val items = findItemsByTags(category.tags, bots).sortedByDescending { bot -> bot.installs }
                if (items.isNotEmpty()) {
                    result.add(DisplayingBotsCategory(category.title))
                    result.add(DisplayingBots(items))
                }
            }
        }

        return result
    }

    private fun findItemsByTags(categoryTags: List<String>, items: List<ShopItem>): List<ShopItem> {
        val result = mutableListOf<ShopItem>()

        // Сравниваем по id тегов, не по названиям
        items.forEach { item ->
            item.tags.map { it.id }.forEach { tag ->
                if (categoryTags.contains(tag)) {
                    result.add(item)
                }
            }
        }

        return result
    }
}
