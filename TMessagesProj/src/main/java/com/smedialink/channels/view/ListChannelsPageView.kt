package com.smedialink.channels.view

import android.annotation.SuppressLint
import android.content.Context
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScrollerMiddle
import androidx.recyclerview.widget.RecyclerView
import com.smedialink.channels.IMeChannelsActivity
import com.smedialink.channels.view.adapter.ChannelsAdapter
import com.smedialink.channels.view.model.ChannelViewModel
import com.smedialink.channels.view.model.ChannelsViewModelMapper
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import org.telegram.messenger.*
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.Cells.DialogCell
import org.telegram.ui.Components.LayoutHelper
import org.telegram.ui.Components.LayoutHelper.MATCH_PARENT
import org.telegram.ui.Components.LayoutHelper.WRAP_CONTENT
import org.telegram.ui.Components.RecyclerListView

@SuppressLint("ViewConstructor")
class ListChannelsPageView(
        private val tab: IMeChannelsActivity.StoreTab,
        private val disposable: CompositeDisposable,
        private val onChannelClickListener: OnChannelClickListener,
        context: Context,
        currentAccount: Int
) : FrameLayout(context) {

    private var recycler: RecyclerListView = RecyclerListView(context)
    private var textEmpty1: TextView = TextView(context)
    private var textEmpty2: TextView = TextView(context)
    private var layoutEmpty: LinearLayout = LinearLayout(context)

    private var recyclerLayoutManager: LinearLayoutManager
    private val channelsViewModelMapper = ChannelsViewModelMapper()
    private var subscribeDisposable: Disposable? = null
    private var myChannelsSubject: PublishSubject<List<ChannelViewModel>> = PublishSubject.create()
    private var recyclerAdapter: ChannelsAdapter =
            ChannelsAdapter(currentAccount).apply { setHasStableIds(true) }
    private var me = MessagesController.getInstance(UserConfig.selectedAccount)
            .getUser(UserConfig.getInstance(UserConfig.selectedAccount).getClientUserId())

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
        recycler.setOnItemClickListener { view, position ->
            if (view is DialogCell) {
                onChannelClickListener.onChannelClick(view.dialogId)
            }
        }
        if (tab == IMeChannelsActivity.StoreTab.MY) {
            textEmpty1.setTextColor(Theme.getColor(Theme.key_emptyListPlaceholder))
            textEmpty1.text = LocaleController.getInternalString(R.string.empty_my_channels1)
            textEmpty1.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20f)
            textEmpty1.typeface = AndroidUtilities.getTypeface("fonts/rmedium.ttf")
            textEmpty1.gravity = Gravity.CENTER

            textEmpty2.text = LocaleController.getInternalString(R.string.empty_my_channels2)
            textEmpty2.setTextColor(Theme.getColor(Theme.key_chats_message))
            textEmpty2.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14f)
            textEmpty2.gravity = Gravity.CENTER
            textEmpty2.setLineSpacing(AndroidUtilities.dp(2f).toFloat(), 1f)
        }
        layoutEmpty.orientation=LinearLayout.VERTICAL
        layoutEmpty.addView(textEmpty1, LayoutHelper.createLinear(MATCH_PARENT, WRAP_CONTENT, 52f, 14f, 52f, 0f))
        layoutEmpty.addView(textEmpty2, LayoutHelper.createLinear(MATCH_PARENT, WRAP_CONTENT, 52f, 7f, 52f, 0f))

        addView(layoutEmpty, LayoutHelper.createFrame(MATCH_PARENT, WRAP_CONTENT.toFloat(), Gravity.TOP, 0f, 15f, 0f, 0f))
        addView(recycler, LayoutHelper.createFrame(MATCH_PARENT, MATCH_PARENT, Gravity.CENTER))

        subscribeToContent()
    }

    fun onMyChannelsChange(myChannels: List<ChannelViewModel>) =
            myChannelsSubject.onNext(myChannels)

    fun subscribeToContent() {
        subscribeDisposable?.dispose()
        when (tab) {
            IMeChannelsActivity.StoreTab.POPULAR -> ApplicationLoader.channelsManager.observePupularChannels(me.phone, LocaleController.getInstance().currentLocaleInfo.langCode)
                    .map { channelsViewModelMapper.mapItems(it) }

            IMeChannelsActivity.StoreTab.MY -> myChannelsSubject
            else -> {
                Observable.just(emptyList())
            }
        }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ items: List<ChannelViewModel> ->
                    if (items.isEmpty()) {
                        layoutEmpty.visibility = View.VISIBLE
                    } else {
                        layoutEmpty.visibility = View.GONE
                    }
                    recyclerAdapter.setContent(items)
                }, { t: Throwable -> t.printStackTrace() })
                .also {
                    subscribeDisposable?.dispose()
                    disposable.add(it)
                }
    }

    interface OnChannelClickListener {

        fun onChannelClick(dialogId: Long)
    }
}
