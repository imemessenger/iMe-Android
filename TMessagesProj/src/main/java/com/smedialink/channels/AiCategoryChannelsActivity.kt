package com.smedialink.channels

import android.content.Context
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScrollerMiddle
import androidx.recyclerview.widget.RecyclerView
import com.smedialink.channels.view.ListChannelsPageView
import com.smedialink.channels.view.adapter.ChannelsAdapter
import com.smedialink.channels.view.model.ChannelViewModel
import com.smedialink.channels.view.model.ChannelsViewModelMapper
import com.smedialink.storage.domain.model.ChannelCategoryCollection
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import org.telegram.messenger.*
import org.telegram.tgnet.ConnectionsManager
import org.telegram.tgnet.TLRPC
import org.telegram.tgnet.TLRPC.*
import org.telegram.ui.ActionBar.ActionBar
import org.telegram.ui.ActionBar.ActionBarMenuItem
import org.telegram.ui.ActionBar.BaseFragment
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.ChatActivity
import org.telegram.ui.Components.LayoutHelper
import org.telegram.ui.Components.RecyclerListView
import org.telegram.ui.Components.SizeNotifierFrameLayout

class AiCategoryChannelsActivity(args: Bundle?) : BaseFragment(args), NotificationCenter.NotificationCenterDelegate,
        ListChannelsPageView.OnChannelClickListener {


    private lateinit var rootContainer: SizeNotifierFrameLayout
    private lateinit var baseViewsContainer: LinearLayout
    private lateinit var searchResultsList: RecyclerListView
    private lateinit var searchResultsAdapter: ChannelsAdapter
    private lateinit var searchListLayoutManager: LinearLayoutManager
    private lateinit var nothingFoundPlaceholder: TextView
    private val channelsViewModelMapper = ChannelsViewModelMapper()

    private val disposable: CompositeDisposable = CompositeDisposable()
    private val searchSubject: BehaviorSubject<String> = BehaviorSubject.createDefault("")

    private var categoryId: String = ""
    private var categoryTitle: String = ""
    private var me = MessagesController.getInstance(UserConfig.selectedAccount)
            .getUser(UserConfig.getInstance(UserConfig.selectedAccount).getClientUserId())


    override fun createView(context: Context): View {


        rootContainer = SizeNotifierFrameLayout(context)
        baseViewsContainer = LinearLayout(context)
        baseViewsContainer.orientation = LinearLayout.VERTICAL


        rootContainer.addView(baseViewsContainer)

        fragmentView = rootContainer

        val menu = actionBar.createMenu()

        val searchItem = menu.addItem(0, R.drawable.ic_ab_search).setIsSearchField(true).setActionBarMenuItemSearchListener(object : ActionBarMenuItem.ActionBarMenuItemSearchListener() {

            override fun onSearchExpand() {
            }

            override fun onSearchCollapse() {
                nothingFoundPlaceholder.visibility = View.GONE
            }

            override fun onTextChanged(editText: EditText?) {
                val text = editText?.text.toString()
                searchSubject.onNext(text)

            }
        })
        searchItem.setSearchFieldHint(LocaleController.getString("Search", R.string.Search))
        actionBar.setBackButtonImage(R.drawable.ic_ab_back)
        actionBar.setTitle(categoryTitle)

        actionBar.setAllowOverlayTitle(true)

        actionBar.setActionBarMenuOnItemClick(object : ActionBar.ActionBarMenuOnItemClick() {
            override fun onItemClick(id: Int) {
                if (id == -1) {
                    finishFragment()
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
        nothingFoundPlaceholder.text = LocaleController.getInternalString(R.string.empty_category)

        nothingFoundPlaceholder.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20f)
        nothingFoundPlaceholder.setTextColor(Theme.getColor(Theme.key_emptyListPlaceholder))
        nothingFoundPlaceholder.visibility = View.INVISIBLE
        nothingFoundPlaceholder.setPadding(AndroidUtilities.dp(20f), AndroidUtilities.dp(20f), AndroidUtilities.dp(18f), 0)
        rootContainer.addView(nothingFoundPlaceholder, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT.toFloat()))

        return fragmentView
    }

    override fun onChannelClick(dialogId: Long) {
        MessagesController.getInstance(currentAccount).openByUserName(
                MessagesController.getInstance(currentAccount).getChat(-dialogId.toInt())?.username
                        ?: "",
                this, 0
        )
    }

    override fun onFragmentCreate(): Boolean {
        super.onFragmentCreate()
        if (getArguments() != null) {
            this.categoryId = getArguments().getString(BUNDLE_KEY_CATEGORY_ID) ?: ""
            this.categoryTitle = getArguments().getString(BUNDLE_KEY_CATEGORY_TITLE) ?: ""

        }
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.joinChannelButtonClicked)
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.channelItemClicked)
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.dialogsNeedReload)


        subscribeSearchResults()
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

    }

    private fun subscribeSearchResults() {
        Observable.combineLatest(
                ApplicationLoader.channelsManager.observeChannelsCategory(categoryId,me.phone, LocaleController.getInstance().currentLocaleInfo.langCode).distinctUntilChanged()
                        .subscribeOn(Schedulers.io()),
                searchSubject.subscribeOn(Schedulers.io()),
                BiFunction { item: ChannelCategoryCollection, query: String ->
                    item.channels.filter { it.searchField.toLowerCase().contains(query, true) }
                })
                .map { channelsViewModelMapper.mapItems(it) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ items: List<ChannelViewModel> ->
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
                .also { disposable.add(it) }
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
                            }.toObservable()

                }
                .subscribe({}, {
                    it.printStackTrace()
                }).also {
                    disposable.add(it)
                }


    }

    private fun getJoinedChannelNames():Single<Set<String>> =
            Single.just(MessagesController.getInstance(currentAccount).myChannels.map { dialog ->
                MessagesController.getInstance(currentAccount).getChat(-(dialog?.id?.toInt()
                        ?: 0))?.username ?: ""
            }.toSet())


    private fun updateAiChannelsJoinState() {
        getJoinedChannelNames()
                .observeOn(Schedulers.io())
                .flatMapCompletable { names ->
                    ApplicationLoader.channelsManager.updateJoinedChanels(names)
                }.subscribe({}, { it.printStackTrace() })
                .also { disposable.add(it) }

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
                updateAiChannelsJoinState()
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

    companion object {
        const val BUNDLE_KEY_CATEGORY_ID = "BUNDLE_KEY_CATEGORY_ID"
        const val BUNDLE_KEY_CATEGORY_TITLE = "BUNDLE_KEY_CATEGORY_TITLE"

    }
}