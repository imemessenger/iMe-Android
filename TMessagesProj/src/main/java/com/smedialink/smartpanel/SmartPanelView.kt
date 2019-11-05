package com.smedialink.smartpanel

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Build
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.smedialink.bots.BotConstants
import com.smedialink.bots.domain.model.SmartBotResponse
import com.smedialink.shop.BotSettingsActivity
import com.smedialink.smartpanel.extension.floatToDp
import com.smedialink.smartpanel.extension.loadFrom
import com.smedialink.smartpanel.mapper.SmartContentMapper
import com.smedialink.smartpanel.model.SmartBotTab
import com.smedialink.smartpanel.model.content.TabBotAnswerItem
import com.smedialink.smartpanel.view.SmartBotContentView
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.panel_custom_tab_header.view.*
import kotlinx.android.synthetic.main.panel_view.view.*
import org.jetbrains.annotations.NotNull
import org.telegram.messenger.MessagesController
import org.telegram.messenger.NotificationCenter
import org.telegram.messenger.R
import org.telegram.tgnet.TLRPC
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.PhotoViewer

@SuppressLint("ViewConstructor")
class SmartPanelView(context: Context, val currentAccount: Int, dialogId: Long) : FrameLayout(context), ViewPager.OnPageChangeListener {

    interface Listener {
        fun onTextAnswerSelected(@NotNull answer: TabBotAnswerItem, position: Int)
        fun onGifAnswerSelected(provider: PhotoViewer.PhotoViewerProvider, gifs: List<TLRPC.BotInlineResult>, position: Int)
        fun sendChosenGif(index: Int, botId: Int, botName: String)
        fun onSmartPanelTab(isExpandable: Boolean)
        fun onBotsRefreshed()
        fun onShopClick()
        fun onBotsSettingsClick()
    }

    private var content: MutableList<SmartBotTab> = mutableListOf()

    private val mapper: SmartContentMapper = SmartContentMapper()
    private val disposables: CompositeDisposable = CompositeDisposable()
    private var listener: SmartPanelView.Listener? = null
    private var panelHeight: Int = 0
    private val contentViews: MutableList<SmartBotContentView> = mutableListOf()
    private var currentBotResponseType = SmartBotContentView.BotResponseType.TEXT


    init {
        View.inflate(context, R.layout.panel_view, this)
        viewpager.addOnPageChangeListener(this)
        tabs.setupWithViewPager(viewpager)
    }

    override fun onPageScrollStateChanged(state: Int) {
    }

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
    }

    override fun onPageSelected(position: Int) {
        listener?.onSmartPanelTab(isExpandable = isCurrentTabExpandable(position))
        updateBaseIcons()
        updateIndicator()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val height = if (panelHeight != 0) panelHeight else layoutParams.height

        super.onMeasure(
                View.MeasureSpec.makeMeasureSpec(layoutParams.width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (parent != null) {
            parent.requestDisallowInterceptTouchEvent(true)
        }
        return super.onInterceptTouchEvent(ev)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        disposables.clear()
    }


    // Служебный метод для управления высотой панели
    fun setHeight(newHeight: Int) {
        panelHeight = newHeight
        this.layoutParams.height = newHeight
    }

    fun setListener(listener: Listener) {
        this.listener = listener
    }

    fun setData(list: List<SmartBotResponse>?, resetTabs: Boolean) {

        content.clear()
        content.addAll(mapper.mapToTabs(list, shouldShowRecent()))

        if (content.isEmpty()) {
            textEmptyBots.visibility = View.VISIBLE
            appbar.visibility = View.GONE
        } else {
            textEmptyBots.visibility = View.GONE
            appbar.visibility = View.VISIBLE
        }
        if (resetTabs) {
            currentBotResponseType = SmartBotContentView.BotResponseType.TEXT
            contentViews.forEach { it.setBotResponseType(currentBotResponseType) }
        }
        viewpager.adapter = pagerAdapter
        pagerAdapter.notifyDataSetChanged()

        setupLayoutBottom()
        updateIcons()
        updateIndicator()
        updateTabListeners()
        wrapTabIndicatorToTitle()
        moveToInitialPosition()
        firePageSelectedEvent()
    }

    private fun shouldShowRecent() =
            MessagesController.getGlobalMainSettings().getBoolean(BotSettingsActivity.SHARED_PREF_KEY_OFTEN_USED_BOTS_ENABLED, BotSettingsActivity.AUTO_BOTS_OFTEN_USED_DEFAULT)

    private fun updateIcons() {
        content.forEachIndexed { index, item ->
            tabs.getTabAt(index)?.customView = View.inflate(context, R.layout.panel_custom_tab_header, null)

            when {
                item.iconRes != 0 -> tabs.getTabAt(index)?.customView?.avatar?.setImageResource(item.iconRes)
                item.iconUrl.isNotEmpty() -> tabs.getTabAt(index)?.customView?.avatar?.loadFrom(item.iconUrl, context)
                else -> tabs.getTabAt(index)?.customView?.avatar?.setImageResource(R.drawable.bot_avatar_any)
            }
        }
    }

    private fun updateIndicator() {
        content.forEachIndexed { index, item ->
            if (index == viewpager.currentItem && item.botId != BotConstants.FREQUENT_ANSWERS_ID) {
                tabs.getTabAt(index)?.customView?.avatar_bg?.visibility = View.VISIBLE
            } else {
                tabs.getTabAt(index)?.customView?.avatar_bg?.visibility = View.INVISIBLE
            }
        }
    }

    private fun updateBaseIcons() {
        val recent = content.indexOfFirst { it.botId == BotConstants.FREQUENT_ANSWERS_ID }
        if (recent != -1) {
            if (viewpager.currentItem == recent) {
                tabs.getTabAt(recent)?.customView?.avatar?.setImageResource(R.drawable.ic_bots_recent_active)
            } else {
                tabs.getTabAt(recent)?.customView?.avatar?.setImageResource(R.drawable.ic_bots_recent)
            }
        }
    }

    private fun updateTabListeners() {
        val tabStrip = tabs.getChildAt(0)
        if (tabStrip is ViewGroup) {
            val childCount = tabStrip.childCount
            for (i in 0 until childCount) {
                tabStrip.getChildAt(i).setOnLongClickListener { v ->
                    val position = tabStrip.indexOfChild(v)
                    val item = content[position]
                    if (item.botId != BotConstants.FREQUENT_ANSWERS_ID) {
                        NotificationCenter.getInstance(currentAccount)
                                .postNotificationName(NotificationCenter.botContextMenu, item.botId, item.botName)
                        true
                    } else {
                        false
                    }
                }
            }
        }
    }

    private fun wrapTabIndicatorToTitle() {
        val padding = floatToDp(14f)
        val tabStrip = tabs.getChildAt(0)
        if (tabStrip is ViewGroup) {
            val childCount = tabStrip.childCount
            for (i in 0 until childCount) {
                val tabView = tabStrip.getChildAt(i)
                tabView.minimumWidth = 0
                tabView.setPadding(padding, tabView.paddingTop, padding, tabView.paddingBottom)
                if (tabView.layoutParams is MarginLayoutParams) {
                    val layoutParams = tabView.layoutParams as MarginLayoutParams
                    setTabMargin(layoutParams, 0, 0)
                }
            }

            tabs.requestLayout()
        }
    }

    private fun setTabMargin(layoutParams: MarginLayoutParams, start: Int, end: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            layoutParams.marginStart = start
            layoutParams.marginEnd = end
            layoutParams.leftMargin = start
            layoutParams.rightMargin = end
        } else {
            layoutParams.leftMargin = start
            layoutParams.rightMargin = end
        }
    }

    private fun setupLayoutBottom() {
        layoutBottom.setBackgroundColor(Theme.getColor(Theme.key_chat_emojiPanelBackground))

        val drawableResponses = Theme.createEmojiIconSelectorDrawable(context, R.drawable.ic_bots_responses, Theme.getColor(Theme.key_chat_emojiBottomPanelIcon), Theme.getColor(Theme.key_chat_emojiPanelIconSelected))
        Theme.setEmojiDrawableColor(drawableResponses, Theme.getColor(Theme.key_chat_emojiBottomPanelIcon), false)
        Theme.setEmojiDrawableColor(drawableResponses, Theme.getColor(Theme.key_chat_emojiPanelIconSelected), true)
        imageTextResponses.setImageDrawable(drawableResponses)
        val drawableGif = Theme.createEmojiIconSelectorDrawable(context, R.drawable.smiles_tab_gif, Theme.getColor(Theme.key_chat_emojiBottomPanelIcon), Theme.getColor(Theme.key_chat_emojiPanelIconSelected))
        Theme.setEmojiDrawableColor(drawableGif, Theme.getColor(Theme.key_chat_emojiBottomPanelIcon), false)
        Theme.setEmojiDrawableColor(drawableGif, Theme.getColor(Theme.key_chat_emojiPanelIconSelected), true)
        imageGifResponses.setImageDrawable(drawableGif)
        imageGifResponses.isSelected = true
        imageTextResponses.isSelected = true
        imageBots.colorFilter = PorterDuffColorFilter(Theme.getColor(Theme.key_chat_emojiPanelBackspace), PorterDuff.Mode.MULTIPLY)
        imageSettings.colorFilter = PorterDuffColorFilter(Theme.getColor(Theme.key_chats_menuItemIcon), PorterDuff.Mode.MULTIPLY)
        imageTextResponses.setOnClickListener {
            imageTextResponses.isSelected = true
            imageGifResponses.isSelected = false
            currentBotResponseType = SmartBotContentView.BotResponseType.TEXT
            contentViews.forEach { it.setBotResponseType(currentBotResponseType) }
        }
        imageGifResponses.setOnClickListener {
            imageTextResponses.isSelected = false
            imageGifResponses.isSelected = true
            currentBotResponseType = SmartBotContentView.BotResponseType.GIF
            contentViews.forEach { it.setBotResponseType(currentBotResponseType) }
        }
        imageBots.setOnClickListener {
            listener?.onShopClick()
        }
        imageSettings.setOnClickListener {
            listener?.onBotsSettingsClick()
        }
        imageGifResponses.isSelected = currentBotResponseType == SmartBotContentView.BotResponseType.GIF
        imageTextResponses.isSelected = currentBotResponseType == SmartBotContentView.BotResponseType.TEXT
    }

    private fun moveToInitialPosition() {
        viewpager.setCurrentItem(0, true)
    }

    private fun firePageSelectedEvent() {
        viewpager.post { onPageSelected(viewpager.currentItem) }
    }

    /**
     * Проверка должна ли текущая вкладка быть раскрываемой на весь экран
     */
    private fun isCurrentTabExpandable(position: Int): Boolean = true
    // TODO AIGRAM раскрытие нижней панели
    // временно? сейчас каждая вкладка может быть раскрыта, изначально только с гифками
    // content[position].gif.isNotEmpty()

    private val pagerAdapter = object : PagerAdapter() {

        override fun isViewFromObject(view: View, obj: Any): Boolean =
                view == obj

        override fun getCount(): Int =
                content.size

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            val contentItem = content[position]
            val view: ViewGroup
            view = SmartBotContentView(container.context, contentItem, currentBotResponseType, listener, dialogId)
            container.addView(view)
            contentViews.add(view)
            return view
        }

        override fun getPageTitle(position: Int): CharSequence? = null

        override fun destroyItem(container: ViewGroup, position: Int, obj: Any) {
            container.removeView(obj as View)
        }
    }
}
