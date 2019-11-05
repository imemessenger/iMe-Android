package com.smedialink.channels

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import com.google.android.flexbox.FlexboxLayout
import com.smedialink.channels.view.model.ChannelCategoryViewModelMapper
import com.smedialink.shop.util.pxToDp
import com.smedialink.smartpanel.extension.loadFrom
import com.smedialink.storage.domain.model.Channel
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import me.zhanghai.android.materialratingbar.MaterialRatingBar
import org.telegram.messenger.*
import org.telegram.tgnet.ConnectionsManager
import org.telegram.tgnet.TLRPC
import org.telegram.ui.ActionBar.ActionBar
import org.telegram.ui.ActionBar.BaseFragment
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.Components.SizeNotifierFrameLayout

class ChannelInfoActivity(args: Bundle?) : BaseFragment(args) {

    private lateinit var rootContainer: SizeNotifierFrameLayout
    private var channelId: String = ""
    private var title: String = ""

    private var avatar: ImageView? = null
    private var channelName: TextView? = null
    private var channelDescription: TextView? = null
    private var tagsContainer: FlexboxLayout? = null
    private var ratingValue: TextView? = null
    private var linkValue: TextView? = null
    private var ratingLabel: TextView? = null
    private var membersLabel: TextView? = null
    private var subscribersValue: TextView? = null
    private var ratingBar: MaterialRatingBar? = null
    private var joinButton: FrameLayout? = null
    private var joinText: TextView? = null
    private var joinProgress: ProgressBar? = null
    private val userId = UserConfig.getInstance(currentAccount).getClientUserId().toLong()
    private var channel: Channel? = null
    private var linkLabel: TextView? = null
    private var textRateLabel: TextView? = null
    private var channelCategoryViewModelMapper = ChannelCategoryViewModelMapper()

    private val disposable = CompositeDisposable()

    override fun onFragmentCreate(): Boolean {
        super.onFragmentCreate()

        if (getArguments() != null) {
            this.channelId = getArguments().getString(BUNDLE_KEY_CHANNEL_NAME) ?: ""
        }

        return true
    }

    override fun onFragmentDestroy() {
        disposable.clear()
        super.onFragmentDestroy()
    }


    override fun createView(context: Context?): View {
        rootContainer = SizeNotifierFrameLayout(context)
        val contentView = LayoutInflater.from(context).inflate(R.layout.channels_description_content, rootContainer, true)
        initViewIds(contentView)

        val menu = actionBar.createMenu()
        menu.addItem(9, R.drawable.ic_share_white).setOnClickListener { v ->
            channel?.let {
                val sendIntent: Intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, "${it.title}\n${it.description}\n$TELEGRAM_ENDPOINT${it.id}")
                    type = "text/plain"
                }
                parentActivity.startActivity(sendIntent)
            }

        }
        actionBar.setBackButtonImage(R.drawable.ic_ab_back)
        actionBar.setTitle(LocaleController.getString("Channell", R.string.Channel))
        actionBar.setAllowOverlayTitle(true)
        actionBar.setActionBarMenuOnItemClick(object : ActionBar.ActionBarMenuOnItemClick() {
            override fun onItemClick(id: Int) {
                if (id == -1) {
                    finishFragment()
                }
            }
        })

        subscribeToChannelContent()
        joinButton?.setBackgroundColor(Theme.getColor(Theme.key_actionBarDefault))
        fragmentView = rootContainer
        return fragmentView
    }

    private fun initViewIds(contentView: View?) {
        avatar = contentView?.findViewById(R.id.channel_avatar)
        channelName = contentView?.findViewById(R.id.channel_name)
        tagsContainer = contentView?.findViewById(R.id.tags_container)
        channelDescription = contentView?.findViewById(R.id.channel_description)
        ratingValue = contentView?.findViewById(R.id.rating_number)
        linkValue = contentView?.findViewById(R.id.channel_link)
        ratingLabel = contentView?.findViewById(R.id.rating_label)
        membersLabel = contentView?.findViewById(R.id.members_label)
        subscribersValue = contentView?.findViewById(R.id.installs_counter)
        ratingBar = contentView?.findViewById(R.id.rating)
        joinButton = contentView?.findViewById(R.id.channel_join_button)
        joinText = contentView?.findViewById(R.id.channel_join_text)
        joinProgress = contentView?.findViewById(R.id.progressBar)
        linkLabel = contentView?.findViewById(R.id.channel_link_label)
        textRateLabel = contentView?.findViewById(R.id.textRateLabel)

    }

    private fun subscribeToChannelContent() {
        ApplicationLoader.channelsManager
                .observeChannel(channelId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ item ->
                    displayChannelItem(item)
                }, { t ->
                    t.printStackTrace()
                })
                .also { disposable.add(it) }
    }

    @SuppressLint("SetTextI18n")
    private fun displayChannelItem(item: Channel) {
        channel = item
        joinProgress?.visibility = View.INVISIBLE
        joinText?.visibility = View.VISIBLE
        avatar?.loadFrom(item.imageUrl, parentActivity)
        this.channelName?.text = item.title
        channelDescription?.movementMethod= LinkMovementMethod.getInstance()
        channelDescription?.text = item.description
        membersLabel?.text = LocaleController.getString("Members", R.string.ChannelMembers)
        textRateLabel?.text = LocaleController.getString("rate_channel", R.string.rate_channel)
        linkLabel?.text = LocaleController.getString("link", R.string.link)
        ratingValue?.text = "%.1f".format(item.rating)

        ratingLabel?.text = "${LocaleController.formatPluralString("Ratings", item.reviews)}"
        subscribersValue?.text = item.subscribers.toString()

        if (item.ownRating == 0) {
            ratingBar?.setIsIndicator(false)
            ratingBar?.rating = 0f
            ratingBar?.setOnRatingChangeListener { ratingBar, ratingValue ->
                ratingBar.rating = ratingValue
                ratingBar.setIsIndicator(true)
                ApplicationLoader.channelsManager.sendChannelRating(item.id, userId, ratingValue.toInt())
            }
        } else {
            ratingBar?.setIsIndicator(true)
            ratingBar?.rating = item.ownRating.toFloat()
        }

        linkValue?.text = "@${item.id}"
        linkValue?.setOnClickListener {
            MessagesController.getInstance(currentAccount).openByUserName(
                    item.id,
                    this, 0
            )
        }

        parentActivity?.let { context ->
            // Стейт кнопки
            joinText?.text =
                    if (item.joined) {
                        LocaleController.getString("AiLeaveChannel", R.string.AiLeaveChannel)
                    } else {
                        LocaleController.getString("ChannelJoin", R.string.ChannelJoin)
                    }
            joinButton?.setOnClickListener {
                joinText?.visibility = View.INVISIBLE
                joinProgress?.visibility = View.VISIBLE
                var chat = (MessagesController.getInstance(currentAccount).getUserOrChat(item.id) as TLRPC.Chat?)
                if (chat != null) {
                    onChannelJoinChange(chat.id.toLong())
                } else {
                    loadChannel(item.id)
                            .subscribe({
                                onChannelJoinChange(it?.id?.toLong() ?: 0)
                            }, {})
                            .also { disposable.add(it) }
                }
            }
        }
        loadChannelCategories(item)
    }


    private fun loadChannelCategories(channel: Channel) {
        if (tagsContainer?.flexItemCount == 0) {

            parentActivity?.let { context ->
                ApplicationLoader.channelsManager.getChannelCategories(channel.tags)
                        .map { channelCategoryViewModelMapper.mapToViewModel(it,LocaleController.getInstance().currentLocaleInfo.langCode) }
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({
                            val margin = context.pxToDp(16)
                            it
                                    .forEach { category ->
                                        val tagTextView = TextView(context)
                                        tagTextView.text = category.title
                                        tagTextView.setBackgroundResource(R.drawable.bg_tag)
                                        tagTextView.setOnClickListener {
                                            presentFragment(AiCategoryChannelsActivity(Bundle().apply {
                                                putString(AiCategoryChannelsActivity.BUNDLE_KEY_CATEGORY_ID, category.id)
                                                putString(AiCategoryChannelsActivity.BUNDLE_KEY_CATEGORY_TITLE, category.title)
                                            }))
                                        }
                                        tagsContainer?.addView(tagTextView)
                                        val params = tagTextView.layoutParams as FlexboxLayout.LayoutParams
                                        params.setMargins(margin, margin, margin, margin)
                                        tagTextView.layoutParams = params
                                    }

                        }, { it.printStackTrace() })

            }
        }
    }


    private fun onChannelJoinChange(dialogId: Long) {
        val chat = MessagesController.getInstance(currentAccount).getChat(dialogId.toInt())
        if (ChatObject.isChannel(chat) && chat !is TLRPC.TL_channelForbidden) {
            if (ChatObject.isNotInChat(chat)) {
                MessagesController.getInstance(currentAccount).addUserToChat(chat.id, UserConfig.getInstance(currentAccount).currentUser, null, 0, null, this, null)
            } else {
                MessagesController.getInstance(currentAccount).deleteUserFromChat(chat.id, UserConfig.getInstance(currentAccount).currentUser, null)
            }
        }
    }


    private fun loadChannel(username: String): Single<TLRPC.Chat> =
            Single.create<TLRPC.Chat> {

                val req = TLRPC.TL_contacts_resolveUsername()
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
        const val BUNDLE_KEY_CHANNEL_NAME = "BUNDLE_KEY_CHANNEL_NAME"
        const val TELEGRAM_ENDPOINT = "https://t.me/"
    }
}
