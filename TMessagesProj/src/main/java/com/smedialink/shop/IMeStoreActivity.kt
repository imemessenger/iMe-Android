package com.smedialink.shop

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScrollerMiddle
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.PagerAdapter
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.tabs.TabLayout
import com.smedialink.bots.BotConstants
import com.smedialink.bots.data.model.BotStatus
import com.smedialink.bots.domain.model.BotLanguage
import com.smedialink.bots.domain.model.ShopItem
import com.smedialink.bots.domain.model.SmartTag
import com.smedialink.bots.usecase.AiBotsManager
import com.smedialink.channels.IMeChannelsActivity
import com.smedialink.channels.InfoActivity
import com.smedialink.channels.InfoActivity.Companion.BUNDLE_KEY_INFO_TEXT
import com.smedialink.channels.InfoActivity.Companion.BUNDLE_KEY_INFO_TITLE
import com.smedialink.channels.InfoActivity.Companion.BUNDLE_KEY_NEED_SPAN_LINK
import com.smedialink.languages.LanguagesActivity
import com.smedialink.shop.view.GridBotsPageView
import com.smedialink.shop.view.ListBotsPageView
import com.smedialink.shop.view.adapter.BotsAdapter
import com.smedialink.shop.view.custom.CustomTabLayout
import com.smedialink.shop.view.custom.NonSwipeableViewPager
import com.smedialink.storage.data.repository.CountriesRepository
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import org.telegram.messenger.*
import org.telegram.ui.ActionBar.ActionBar
import org.telegram.ui.ActionBar.ActionBarMenuItem
import org.telegram.ui.ActionBar.BaseFragment
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.Components.LayoutHelper
import org.telegram.ui.Components.RecyclerListView
import org.telegram.ui.Components.SizeNotifierFrameLayout

class IMeStoreActivity : BaseFragment(), NotificationCenter.NotificationCenterDelegate {

    enum class StoreTab(val resId: Int) {
        ALL(R.string.tab_all),
        POPULAR(R.string.shop_tab_popular),
        FREE(R.string.shop_tab_free),
        MY(R.string.shop_tab_my);
    }


    companion object {
        // Фильтр для вкладки Популярные
        fun isPopular(tags: List<SmartTag>): Boolean =
                tags.map { it.id }.contains(BotConstants.POPULAR_TAG)

        // Фильтр для вкладки Бесплатные
        fun isFree(sku: String?): Boolean =
                sku.isNullOrBlank()

        // Фильтр для вкладки Мои
        fun isMy(status: BotStatus): Boolean =
                setOf(
                        BotStatus.AVAILABLE,
                        BotStatus.UPDATE_AVAILABLE,
                        BotStatus.DOWNLOADING,
                        BotStatus.ENABLED,
                        BotStatus.DISABLED
                ).contains(status)

        private const val search = 104
        private const val changeCountry = 105
        private const val addChannel = 106
    }

    private lateinit var rootContainer: SizeNotifierFrameLayout
    private lateinit var baseViewsContainer: CoordinatorLayout
    private lateinit var tabLayout: CustomTabLayout
    private lateinit var viewPager: NonSwipeableViewPager
    private lateinit var pagerAdapter: PagerAdapter
    private var searchMenuItem: TextView? = null
    private var changeCountryItem: TextView? = null
    private var addChannelItem: TextView? = null
    private var otherItem: ActionBarMenuItem? = null
    private var searchItem: ActionBarMenuItem? = null
    private lateinit var searchResultsList: RecyclerListView
    private lateinit var searchResultsAdapter: BotsAdapter
    private lateinit var searchListLayoutManager: LinearLayoutManager
    private lateinit var nothingFoundPlaceholder: TextView
    private var searchDisposable: Disposable? = null

    private var me = MessagesController.getInstance(UserConfig.selectedAccount)
            .getUser(UserConfig.getInstance(UserConfig.selectedAccount).getClientUserId())

    private val tabViews = mutableMapOf<Int, View>()

    private val disposable: CompositeDisposable = CompositeDisposable()
    private val searchSubject: PublishSubject<String> = PublishSubject.create()

    private val userId = UserConfig.getInstance(currentAccount).getClientUserId()

    override fun createView(context: Context): View {
        swipeBackEnabled = false

        // View для вкладок магазина
        val botLanguage = BotLanguage.fromValue(CountriesRepository.getInstance(context).getCurrentBotLanguage(me.phone, LocaleController.getInstance().currentLocaleInfo.langCode))
        StoreTab.values().forEachIndexed { index, storeTab ->
            if (storeTab == StoreTab.ALL) {
                tabViews[index] = GridBotsPageView(disposable, context, botLanguage, currentAccount)
            } else {
                tabViews[index] = ListBotsPageView(storeTab, disposable, botLanguage, context, currentAccount)
            }
        }

        rootContainer = SizeNotifierFrameLayout(context)
        baseViewsContainer = CoordinatorLayout(context)
        rootContainer.addView(baseViewsContainer, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.TOP))

        tabLayout = CustomTabLayout(context)
        if (Build.VERSION.SDK_INT >= 21) {
            tabLayout.elevation = 6f
            tabLayout.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite))
            tabLayout.setSelectedTabIndicatorColor(Theme.getColor(Theme.key_actionBarDefault))
        }
        tabLayout.tabMode = TabLayout.MODE_SCROLLABLE
        viewPager = NonSwipeableViewPager(context)
        pagerAdapter = buildNewTabsAdapter()
        viewPager.adapter = pagerAdapter
        viewPager.offscreenPageLimit = 3
        tabLayout.setupWithViewPager(viewPager)
        val tabsAppbar = AppBarLayout(context)
        tabsAppbar.addView(tabLayout, LayoutHelper.createAppBar(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))
        val params = tabLayout.layoutParams as AppBarLayout.LayoutParams
        params.scrollFlags = (AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL
                or AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS
                or AppBarLayout.LayoutParams.SCROLL_FLAG_SNAP)
        baseViewsContainer.addView(tabsAppbar, LayoutHelper.createCoordinator(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))
        baseViewsContainer.addView(viewPager, LayoutHelper.createCoordinator(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT))
        val tabsViewPagerParams = viewPager.layoutParams as CoordinatorLayout.LayoutParams
        tabsViewPagerParams.behavior = AppBarLayout.ScrollingViewBehavior()
        viewPager.requestLayout()

        fragmentView = rootContainer

        val menu = actionBar.createMenu()
        otherItem = menu.addItem(0, R.drawable.ic_ab_other)
        searchMenuItem = otherItem?.addSubItem(search, LocaleController.getInternalString(R.string.search))
        changeCountryItem = otherItem?.addSubItem(changeCountry, LocaleController.getInternalString(R.string.bots_language))
        addChannelItem = otherItem?.addSubItem(addChannel, LocaleController.getInternalString(R.string.add_bot))

        searchItem = menu.addItem(0, R.drawable.ic_ab_search).setIsSearchField(true).setActionBarMenuItemSearchListener(object : ActionBarMenuItem.ActionBarMenuItemSearchListener() {

            override fun onSearchExpand() {
                tabLayout.visibility = View.GONE
                listenForSearchResults()
                otherItem?.visibility = View.GONE
                searchItem?.searchField?.postDelayed({
                    AndroidUtilities.showKeyboard(searchItem?.searchField)
                }, 250)
            }

            override fun onSearchCollapse() {
                nothingFoundPlaceholder.visibility = View.GONE
                searchResultsList.visibility = View.GONE
                baseViewsContainer.visibility = View.VISIBLE
                tabLayout.visibility = View.VISIBLE
                searchDisposable?.dispose()
                otherItem?.visibility = View.VISIBLE
                searchItem?.visibility = View.GONE
            }

            override fun onTextChanged(editText: EditText?) {
                val text = editText?.text.toString()
                if (text.isNotEmpty()) {
                    searchSubject.onNext(text)
                }
            }
        })
        searchItem?.setSearchFieldHint(LocaleController.getString("Search", R.string.Search))
        searchItem?.visibility = View.GONE
        actionBar.setBackButtonImage(R.drawable.ic_ab_back)
        actionBar.setTitle(LocaleController.getInternalString(R.string.iMeStore))

        actionBar.setAllowOverlayTitle(true)

        actionBar.setActionBarMenuOnItemClick(object : ActionBar.ActionBarMenuOnItemClick() {
            override fun onItemClick(id: Int) {
                when (id) {
                    -1 -> {
                        finishFragment()
                    }
                    addChannel -> {
                        presentFragment(InfoActivity(Bundle().apply {
                            putString(BUNDLE_KEY_INFO_TITLE, LocaleController.getInternalString(R.string.add_bot))
                            putString(BUNDLE_KEY_INFO_TEXT, LocaleController.getInternalString(R.string.add_bot_info))
                            putBoolean(BUNDLE_KEY_NEED_SPAN_LINK, true)

                        }))
                    }
                    changeCountry -> {
                        presentFragment(LanguagesActivity())
                    }
                    search -> {
                        actionBar.openSearchField("", false)
                        AndroidUtilities.showKeyboard(rootContainer)
                    }
                }
            }
        })

        searchResultsList = RecyclerListView(context)
        searchResultsAdapter = BotsAdapter(currentAccount).apply { setHasStableIds(true) }
        searchListLayoutManager = object : LinearLayoutManager(context) {
            override fun supportsPredictiveItemAnimations(): Boolean {
                return false
            }

            override fun smoothScrollToPosition(recyclerView: RecyclerView, state: RecyclerView.State, position: Int) {
                val linearSmoothScroller = LinearSmoothScrollerMiddle(recyclerView.context)
                linearSmoothScroller.targetPosition = position
                startSmoothScroll(linearSmoothScroller)
            }
        }

        searchListLayoutManager.orientation = LinearLayoutManager.VERTICAL
        searchResultsList.layoutManager = searchListLayoutManager
        searchResultsList.adapter = searchResultsAdapter
        searchResultsList.visibility = View.GONE

        rootContainer.addView(searchResultsList, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT))

        nothingFoundPlaceholder = TextView(context)
        nothingFoundPlaceholder.text = LocaleController.getInternalString(R.string.NotFound)

        nothingFoundPlaceholder.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20f)
        nothingFoundPlaceholder.setTextColor(Theme.getColor(Theme.key_emptyListPlaceholder))
        nothingFoundPlaceholder.visibility = View.INVISIBLE
        nothingFoundPlaceholder.setPadding(AndroidUtilities.dp(20f), AndroidUtilities.dp(20f), AndroidUtilities.dp(18f), 0)
        rootContainer.addView(nothingFoundPlaceholder, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT.toFloat()))

        return fragmentView
    }

    override fun onActivityResultFragment(requestCode: Int, resultCode: Int, data: Intent?) {
        if (data != null) {
            ApplicationLoader.purchaseHelper.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onFragmentCreate(): Boolean {
        super.onFragmentCreate()
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.botButtonClicked)
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.botItemClicked)
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.botLanguageChanged)
        ApplicationLoader.smartBotsManager.listenForRemoteBotUpdates(object:AiBotsManager.FirebaseSnapshotCallback {
            override fun onSuccess() {
                ApplicationLoader.purchaseHelper.preloadPurchasesInfo()
            }
        })
        return true
    }

    override fun onFragmentDestroy() {
        super.onFragmentDestroy()
        disposable.clear()
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.botButtonClicked)
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.botItemClicked)
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.botLanguageChanged)
    }

    override fun didReceivedNotification(id: Int, account: Int, vararg args: Any?) {
        when (id) {
            NotificationCenter.botButtonClicked -> {
                val item = args[0] as ShopItem
                when (item.status) {
                    // Включить
                    BotStatus.DISABLED -> updateBotStatus(item.botId, BotStatus.ENABLED)
                    // Выключить
                    BotStatus.ENABLED -> updateBotStatus(item.botId, BotStatus.DISABLED)
                    // Купить
                    BotStatus.PAID -> item.sku?.let { purchaseItem(it) }
                    // Скачать
                    BotStatus.AVAILABLE, BotStatus.UPDATE_AVAILABLE -> initiateBotDownloading(item)
                    // Идет скачивание, пропускаем клик
                    BotStatus.DOWNLOADING -> {
                    }
                }
            }
            NotificationCenter.botItemClicked -> {
                val item = args[0] as ShopItem
                val arguments = Bundle().apply {
                    putString("botId", item.botId)
                    putString("title", item.title)
                }
                presentFragment(BotInfoActivity(arguments))
            }
            NotificationCenter.botLanguageChanged -> {
                val botLanguage = BotLanguage.fromValue(CountriesRepository.getInstance(parentActivity).getCurrentBotLanguage(me.phone, LocaleController.getInstance().currentLocaleInfo.langCode))

                IMeChannelsActivity.StoreTab.values().forEachIndexed { index, storeTab ->
                    when (storeTab) {
                        IMeChannelsActivity.StoreTab.ALL -> (tabViews[index] as GridBotsPageView).apply {
                            this.botLanguage = botLanguage
                        }.subscribeToContent()
                        else -> (tabViews[index] as ListBotsPageView).apply {
                            this.botLanguage = botLanguage
                        }.subscribeToContent()
                    }
                }
            }
        }
    }

    private fun listenForSearchResults() {
        searchDisposable?.dispose()
        Observable.combineLatest(
                ApplicationLoader.smartBotsManager.getAllBotsObservable(BotLanguage.fromValue(CountriesRepository.getInstance(parentActivity).getCurrentBotLanguage(me.phone, LocaleController.getInstance().currentLocaleInfo.langCode)),LocaleController.getInstance().currentLocaleInfo.langCode).distinctUntilChanged().subscribeOn(Schedulers.io()),
                searchSubject.subscribeOn(Schedulers.io()),
                BiFunction { items: List<ShopItem>, query: String ->
                    items.filter { it.searchField.toLowerCase().contains(query) }
                })
                .map { it.sortedByDescending { bot -> bot.installs } }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ items: List<ShopItem> ->
                    if (baseViewsContainer.visibility != View.GONE) {
                        baseViewsContainer.visibility = View.GONE
                    }

                    if (items.isNotEmpty()) {
                        if (nothingFoundPlaceholder.visibility != View.GONE) {
                            nothingFoundPlaceholder.visibility = View.GONE
                        }
                        if (searchResultsList.visibility != View.VISIBLE) {
                            searchResultsList.visibility = View.VISIBLE
                        }
                        searchResultsAdapter.setContent(items)
                    } else {
                        if (searchResultsList.visibility != View.GONE) {
                            searchResultsList.visibility = View.GONE
                        }
                        if (nothingFoundPlaceholder.visibility != View.VISIBLE) {
                            nothingFoundPlaceholder.visibility = View.VISIBLE
                        }
                    }
                }, { t: Throwable ->
                    t.printStackTrace()
                })
                .also {
                    disposable.add(it)
                    searchDisposable = it
                }
    }

    private fun initiateBotDownloading(item: ShopItem) {
        ApplicationLoader.smartBotsManager.startBotDownloading(item.botId, item.title, item.downloadLink, userId)
    }

    private fun updateBotStatus(botId: String, status: BotStatus) {
        ApplicationLoader.smartBotsManager
                .updateBotStatus(botId, status)
                .subscribeOn(Schedulers.io())
                .subscribe({}, { it.printStackTrace() })
                .also { disposable.add(it) }
    }

    private fun purchaseItem(sku: String) {
        ApplicationLoader.purchaseHelper
                .purchase(sku)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ purchase ->
                    ApplicationLoader.smartBotsManager.downloadPurchase(purchase.sku, userId)
                }, { t -> t.printStackTrace() })
                .also { disposable.add(it) }
    }

    private fun buildNewTabsAdapter(): PagerAdapter =
            object : PagerAdapter() {
                override fun getCount(): Int =
                        tabViews.size

                override fun isViewFromObject(view: View, o: Any): Boolean {
                    return view == o
                }

                override fun instantiateItem(container: ViewGroup, position: Int): Any {
                    val view = tabViews[position]
                    container.addView(view)
                    return view as View
                }

                override fun destroyItem(container: ViewGroup, position: Int, child: Any) {
                    container.removeView(child as View)
                }

                override fun getPageTitle(position: Int): CharSequence {
                    return LocaleController.getInternalString(StoreTab.values()[position].resId)
                            ?: ""
                }
            }
}
