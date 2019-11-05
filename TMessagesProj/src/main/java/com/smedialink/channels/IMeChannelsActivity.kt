package com.smedialink.channels

import android.content.Context
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
import com.smedialink.channels.view.GridChannelsPageView
import com.smedialink.channels.view.ListCategoriesPageView
import com.smedialink.channels.view.ListChannelsPageView
import com.smedialink.channels.view.adapter.ChannelsAdapter
import com.smedialink.channels.view.adapter.ChannelsCategoriesPreviewAdapter
import com.smedialink.channels.view.model.AiChannelsMapper
import com.smedialink.channels.view.model.ChannelCategoryViewModel
import com.smedialink.channels.view.model.ChannelViewModel
import com.smedialink.channels.view.model.ChannelsViewModelMapper
import com.smedialink.countries.CountriesActivity
import com.smedialink.shop.view.custom.CustomTabLayout
import com.smedialink.shop.view.custom.NonSwipeableViewPager
import com.smedialink.storage.domain.model.Channel
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.ChatObject
import org.telegram.messenger.LocaleController
import org.telegram.messenger.MessagesController
import org.telegram.messenger.NotificationCenter
import org.telegram.messenger.R
import org.telegram.messenger.UserConfig
import org.telegram.tgnet.ConnectionsManager
import org.telegram.tgnet.TLRPC
import org.telegram.tgnet.TLRPC.Chat
import org.telegram.tgnet.TLRPC.TL_channelForbidden
import org.telegram.tgnet.TLRPC.TL_contacts_resolveUsername
import org.telegram.ui.ActionBar.ActionBar
import org.telegram.ui.ActionBar.ActionBarMenuItem
import org.telegram.ui.ActionBar.BaseFragment
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.ChatActivity
import org.telegram.ui.Components.LayoutHelper
import org.telegram.ui.Components.RecyclerListView
import org.telegram.ui.Components.SizeNotifierFrameLayout

class IMeChannelsActivity : BaseFragment(), NotificationCenter.NotificationCenterDelegate,
        ListChannelsPageView.OnChannelClickListener,
        ChannelsCategoriesPreviewAdapter.OnCategoryClickListener {


    enum class StoreTab(val resId: Int) {
        ALL(R.string.tab_all),
        POPULAR(R.string.shop_tab_popular),
        CATEGORIES(R.string.categories),
        MY(R.string.shop_tab_my);
    }

    companion object {
        private const val search = 104
        private const val changeCountry = 105
        private const val addChannel = 106
    }

    private lateinit var rootContainer: SizeNotifierFrameLayout
    private lateinit var baseViewsContainer: CoordinatorLayout
    private lateinit var tabLayout: CustomTabLayout
    private lateinit var viewPager: NonSwipeableViewPager
    private lateinit var pagerAdapter: PagerAdapter
    private lateinit var searchResultsList: RecyclerListView
    private lateinit var searchResultsAdapter: ChannelsAdapter
    private lateinit var searchListLayoutManager: LinearLayoutManager
    private lateinit var nothingFoundPlaceholder: TextView
    private val tabViews = mutableMapOf<Int, View>()
    private val channelsViewModelMapper = ChannelsViewModelMapper()
    private var searchMenuItem: TextView? = null
    private var changeCountryItem: TextView? = null
    private var addChannelItem: TextView? = null
    private var otherItem: ActionBarMenuItem? = null
    private var searchItem: ActionBarMenuItem? = null
    private val disposable: CompositeDisposable = CompositeDisposable()
    private val searchSubject: PublishSubject<String> = PublishSubject.create()
    private val aiChannelsMapper = AiChannelsMapper()
    private var searchDisposable: Disposable? = null
    private var me = MessagesController.getInstance(UserConfig.selectedAccount)
            .getUser(UserConfig.getInstance(UserConfig.selectedAccount).getClientUserId())

    override fun createView(context: Context): View {
        swipeBackEnabled = false

        // View для вкладок магазина
        StoreTab.values().forEachIndexed { index, storeTab ->
            if (storeTab == StoreTab.ALL) {
                tabViews[index] = GridChannelsPageView(disposable, context, currentAccount)
            } else if (storeTab == StoreTab.CATEGORIES) {
                tabViews[index] = ListCategoriesPageView(disposable, this, context)
            } else {
                tabViews[index] = ListChannelsPageView(storeTab, disposable, this, context, currentAccount)
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
        viewPager.offscreenPageLimit = 4
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
        changeCountryItem = otherItem?.addSubItem(changeCountry, LocaleController.getInternalString(R.string.change_country))
        addChannelItem = otherItem?.addSubItem(addChannel, LocaleController.getInternalString(R.string.add_channel))


        searchItem = menu.addItem(0, R.drawable.ic_ab_search).setIsSearchField(true).setActionBarMenuItemSearchListener(object : ActionBarMenuItem.ActionBarMenuItemSearchListener() {

            override fun onSearchExpand() {
                tabLayout.visibility = View.GONE
                subscribeSearchResults()
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
        actionBar.setTitle(LocaleController.getInternalString(R.string.tabChannels))

        actionBar.setAllowOverlayTitle(true)

        actionBar.setActionBarMenuOnItemClick(object : ActionBar.ActionBarMenuOnItemClick() {
            override fun onItemClick(id: Int) {
                when (id) {
                    -1 -> {
                        finishFragment()
                    }
                    addChannel -> {
                        presentFragment(AddChannelActivity())
                    }
                    changeCountry -> {
                        presentFragment(CountriesActivity())
                    }
                    search -> {
                        actionBar.openSearchField("", false)
                        AndroidUtilities.showKeyboard(rootContainer)
                    }
                }
            }
        })

        searchResultsList = RecyclerListView(context)
        searchResultsAdapter = ChannelsAdapter(currentAccount).apply { setHasStableIds(true) }
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
        updateMyChannels()

        return fragmentView
    }

    override fun onChannelClick(dialogId: Long) {
        MessagesController.getInstance(currentAccount).openByUserName(
                MessagesController.getInstance(currentAccount).getChat(-dialogId.toInt())?.username
                        ?: "",
                this, 0
        )
    }

    override fun onCategoryClick(category: ChannelCategoryViewModel) {
        presentFragment(AiCategoryChannelsActivity(Bundle().apply {
            putString(AiCategoryChannelsActivity.BUNDLE_KEY_CATEGORY_ID, category.id)
            putString(AiCategoryChannelsActivity.BUNDLE_KEY_CATEGORY_TITLE, category.title)
        }))
    }

    override fun onFragmentCreate(): Boolean {
        super.onFragmentCreate()
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.joinChannelButtonClicked)
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.channelItemClicked)
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.dialogsNeedReload)
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.channelsCountryChanged)


        subscribeChannelsUpdates()
        ApplicationLoader.channelsManager.subscribeRemoteChannelCategories()
                .also { disposable.add(it) }
        return true
    }

    override fun onFragmentDestroy() {
        super.onFragmentDestroy()
        disposable.clear()
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.joinChannelButtonClicked)
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.channelItemClicked)
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.dialogsNeedReload)
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.channelsCountryChanged)

    }

    private fun subscribeSearchResults() {
        searchDisposable?.dispose()
        Observable.combineLatest(
                ApplicationLoader.channelsManager.observeAllChannels(me.phone, LocaleController.getInstance().currentLocaleInfo.langCode).distinctUntilChanged()
                        .subscribeOn(Schedulers.io()),
                searchSubject.subscribeOn(Schedulers.io()),
                BiFunction { items: List<Channel>, query: String ->
                    items.filter { it.searchField.toLowerCase().contains(query, true) }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ items: List<Channel> ->
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
                        searchResultsAdapter.setContent(channelsViewModelMapper.mapItems(items))
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
                    searchDisposable = it
                    disposable.add(it)
                }
    }

    private fun subscribeChannelsUpdates() {
        ApplicationLoader.channelsManager.observeRemoteChannels()
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .flatMap {
                    getJoinedChannelNames()
                            .flatMap {subscribedChannels->
                                ApplicationLoader.channelsManager.saveChannels(it,subscribedChannels)
                                        .toSingleDefault(it)
                            }
                            .toObservable()
                }
                .subscribe({}, {
                    it.printStackTrace()
                }).also {
                    disposable.add(it)
                }


    }

    private fun getJoinedChannelNames(): Single<Set<String>> =
            Single.just(MessagesController.getInstance(currentAccount).myChannels.map { dialog ->
                MessagesController.getInstance(currentAccount).getChat(-(dialog?.id?.toInt()
                        ?: 0))?.username ?: ""
            }.toSet())

    private fun updateMyChannels() {
        (tabViews[3] as ListChannelsPageView).onMyChannelsChange(
                aiChannelsMapper.mapItems(MessagesController.getInstance(currentAccount).myChannels, true, MessagesController.getInstance(currentAccount)))
    }

    private fun updateAiChannelsJoinState() {
        getJoinedChannelNames()
                .observeOn(Schedulers.io())
                .flatMapCompletable { names ->
                    ApplicationLoader.channelsManager.updateJoinedChanels(names)
                }.subscribe({}, { it.printStackTrace() })
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


    override fun didReceivedNotification(id: Int, account: Int, vararg args: Any?) {
        when (id) {
            NotificationCenter.joinChannelButtonClicked -> {
                val channelViewModel = args[0] as ChannelViewModel
                if (channelViewModel.dialog != null) {
                    onChannelJoinChange(-channelViewModel.dialog.id)
                } else {
                    var chat = (MessagesController.getInstance(currentAccount).getUserOrChat(channelViewModel.id) as Chat?)
                    if (chat != null) {
                        onChannelJoinChange(chat.id.toLong())
                    } else {
                        loadChannel(channelViewModel.id)
                                .subscribe({
                                    onChannelJoinChange(it?.id?.toLong() ?: 0)
                                }, {})
                                .also { disposable.add(it) }
                    }
                }
            }
            NotificationCenter.channelItemClicked -> {
                val channelViewModel = args[0] as ChannelViewModel
                if (channelViewModel.dialog != null) {
                    val argsOpen = Bundle()
                    argsOpen.putInt("chat_id", -channelViewModel.dialog.id.toInt())
                    if (MessagesController.getInstance(currentAccount).checkCanOpenChat(argsOpen, this)) {
                        presentFragment(ChatActivity(argsOpen))
                    }
                } else {
                    presentFragment(ChannelInfoActivity(Bundle().apply {
                        putString(ChannelInfoActivity.BUNDLE_KEY_CHANNEL_NAME, channelViewModel.id)
                    }))
                }
            }
            NotificationCenter.dialogsNeedReload -> {
                updateMyChannels()
                updateAiChannelsJoinState()
            }
            NotificationCenter.channelsCountryChanged -> {
                StoreTab.values().forEachIndexed { index, storeTab ->
                    when (storeTab) {
                        StoreTab.ALL -> (tabViews[index] as GridChannelsPageView).subscribeToContent()
                        StoreTab.CATEGORIES -> (tabViews[index] as ListCategoriesPageView).subscribeToContent()
                        else -> (tabViews[index] as ListChannelsPageView).subscribeToContent()
                    }
                }
            }
        }
    }


    private fun onChannelJoinChange(chatId: Long) {
        val chat = MessagesController.getInstance(currentAccount).getChat(chatId.toInt())
        if (ChatObject.isChannel(chat) && chat !is TL_channelForbidden) {
            if (ChatObject.isNotInChat(chat)) {
                MessagesController.getInstance(currentAccount).addUserToChat(chat.id, UserConfig.getInstance(currentAccount).currentUser, null, 0, null, this, null)
            } else {
                MessagesController.getInstance(currentAccount).deleteUserFromChat(chat.id, UserConfig.getInstance(currentAccount).currentUser, null)
            }
        }
    }


    private fun loadChannel(username: String): Single<Chat> =
            Single.create<Chat> {

                val req = TL_contacts_resolveUsername()
                req.username = username
                val reqId = ConnectionsManager.getInstance(currentAccount).sendRequest(req) { response, error ->
                    if (error != null) {
                        it.tryOnError(Throwable())
                    } else {
                        val res = response as TLRPC.TL_contacts_resolvedPeer
                        MessagesController.getInstance(currentAccount).putChats(res.chats, false)
                        if (res.chats.isNotEmpty()) {
                            it.onSuccess(res.chats[0])
                        } else it.tryOnError(Throwable())
                    }
                }
                it.setCancellable {
                    ConnectionsManager.getInstance(currentAccount).cancelRequest(reqId, true)
                }
            }.subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.io())


}