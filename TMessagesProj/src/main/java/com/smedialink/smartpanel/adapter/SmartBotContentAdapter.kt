package com.smedialink.smartpanel.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.smedialink.smartpanel.model.SmartBotTab
import com.smedialink.smartpanel.model.SmartPanelTabContent
import com.smedialink.smartpanel.model.content.TabBotAnswerItem
import com.smedialink.smartpanel.model.content.TabBotMediaAnswerItem
import com.smedialink.smartpanel.model.content.TabBotNameItem
import com.smedialink.smartpanel.view.SmartBotContentView
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.bots_content_page_item_ads.*
import kotlinx.android.synthetic.main.bots_content_page_item_label.*
import kotlinx.android.synthetic.main.bots_content_page_item_normal.*
import org.telegram.messenger.*
import org.telegram.tgnet.ConnectionsManager
import org.telegram.tgnet.RequestDelegate
import org.telegram.tgnet.TLRPC
import org.telegram.ui.Cells.ContextLinkCell
import org.telegram.ui.Components.RecyclerListView

class SmartBotContentAdapter(private val dialogId: Long,private val onLoadGifListener: OnLoadGifListener) : RecyclerListView.SelectionAdapter() {

    var mediaContentOffset = 0

    private val items = mutableListOf<SmartPanelTabContent>()
    private val gifBotUserName = "gif"

    private var currentAccount = UserConfig.selectedAccount
    private var foundContextBot: TLRPC.User? = null
    private var contextMedia: Boolean = false
    private var currentBotResponceType: SmartBotContentView.BotResponseType = SmartBotContentView.BotResponseType.TEXT
    private var searchingContextQuery: String? = null
    private var nextQueryOffset: String? = null
    private var contextUsernameReqId: Int = 0
    private var contextQueryReqId: Int = 0
    private var contextQueryRunnable: Runnable? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
            when (viewType) {
                SmartPanelTabContent.Type.ADVERT_BOT_ANSWER.value -> {
                    val view = LayoutInflater.from(parent.context)
                            .inflate(R.layout.bots_content_page_item_ads, parent, false)
                    AdsViewHolder(view)
                }
                SmartPanelTabContent.Type.BOT_MEDIA_ANSWER.value -> {
                    val view = ContextLinkCell(parent.context)
                    RecyclerListView.Holder(view)
                }
                SmartPanelTabContent.Type.NORMAL_BOT_ANSWER.value -> {
                    val view = LayoutInflater.from(parent.context)
                            .inflate(R.layout.bots_content_page_item_normal, parent, false)
                    NormalViewHolder(view)
                }
                else -> {
                    val view = LayoutInflater.from(parent.context)
                            .inflate(R.layout.bots_content_page_item_label, parent, false)
                    BotNameViewHolder(view)
                }
            }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (items[position].contentType) {
            SmartPanelTabContent.Type.ADVERT_BOT_ANSWER -> {
                holder as AdsViewHolder
                holder.bindTo(items[position])
            }
            SmartPanelTabContent.Type.NORMAL_BOT_ANSWER -> {
                holder as NormalViewHolder
                holder.bindTo(items[position])
            }
            SmartPanelTabContent.Type.NORMAL_BOT_LABEL -> {
                holder as BotNameViewHolder
                holder.bindTo(items[position])
            }
            else -> {
                val item = items[position]
                if (item is TabBotMediaAnswerItem) {
                    (holder.itemView as ContextLinkCell).setLink(item.media, contextMedia, true, true)
                }
            }
        }
    }

    override fun isEnabled(holder: RecyclerView.ViewHolder?): Boolean =
            holder?.itemViewType != SmartPanelTabContent.Type.NORMAL_BOT_LABEL.value

    override fun getItemViewType(position: Int): Int =
            items[position].contentType.value

    override fun getItemCount(): Int =
            items.size

    override fun getItemId(position: Int): Long =
            position.toLong()

    fun setTextData(content: SmartBotTab, botResponseType: SmartBotContentView.BotResponseType) {
        currentBotResponceType = botResponseType
        items.clear()
        val answers = mutableListOf<SmartPanelTabContent>()
        answers.addAll(content.answers.filter {
            (it.contentType == SmartPanelTabContent.Type.NORMAL_BOT_ANSWER &&
                    botResponseType == SmartBotContentView.BotResponseType.TEXT) ||
                    (it.contentType == SmartPanelTabContent.Type.BOT_MEDIA_ANSWER &&
                            botResponseType == SmartBotContentView.BotResponseType.GIF) ||
                    it.contentType == SmartPanelTabContent.Type.NORMAL_BOT_LABEL ||
                    it.contentType == SmartPanelTabContent.Type.ADVERT_BOT_ANSWER

        })
        items.addAll(answers)
        mediaContentOffset = items.size
        notifyDataSetChanged()

        if (content.gif.isNotEmpty() && botResponseType == SmartBotContentView.BotResponseType.GIF) {
            prepareGifRequest(gifBotUserName, content.gif)
        }else if(content.gif.isEmpty()){
            onLoadGifListener.onLoadGifComplete(0)
        }
    }

    fun getItem(i: Int): Any? {
        return items[i]
    }

    fun getGifBotId(): Int {
        return foundContextBot?.id ?: 0
    }

    fun getGifBotName(): String {
        return foundContextBot?.username ?: ""
    }

    fun getTextContent(): List<SmartPanelTabContent> =
            items.subList(0, mediaContentOffset)

    fun getMediaContent(): List<TLRPC.BotInlineResult> =
            items.subList(mediaContentOffset, items.lastIndex)
                    .filter { it is TabBotMediaAnswerItem }
                    .map { (it as TabBotMediaAnswerItem).media }

    fun searchGifsForNextOffset() {
        if (contextQueryReqId != 0 || nextQueryOffset == null || nextQueryOffset?.isEmpty() == true || foundContextBot == null || searchingContextQuery == null) {
            return
        }
        searchForGifResults(true, foundContextBot, searchingContextQuery, nextQueryOffset)
    }

    fun onDestroy() {
        if (contextQueryRunnable != null) {
            AndroidUtilities.cancelRunOnUIThread(contextQueryRunnable)
            contextQueryRunnable = null
        }
        if (contextUsernameReqId != 0) {
            ConnectionsManager.getInstance(currentAccount).cancelRequest(contextUsernameReqId, true)
            contextUsernameReqId = 0
        }
        if (contextQueryReqId != 0) {
            ConnectionsManager.getInstance(currentAccount).cancelRequest(contextQueryReqId, true)
            contextQueryReqId = 0
        }
    }

    private fun prepareGifRequest(username: String, query: String) {

        if (contextQueryRunnable != null) {
            AndroidUtilities.cancelRunOnUIThread(contextQueryRunnable)
            contextQueryRunnable = null
        }
        val messagesController = MessagesController.getInstance(currentAccount)
        val messagesStorage = MessagesStorage.getInstance(currentAccount)
        searchingContextQuery = query

        contextQueryRunnable = object : Runnable {
            override fun run() {
                if (contextQueryRunnable != this) {
                    return
                }
                contextQueryRunnable = null
                if (foundContextBot != null) {
                    searchForGifResults(true, foundContextBot, query, "")
                } else {
                    val resultingUser = messagesController.getUserOrChat(username)
                    if (resultingUser is TLRPC.User) {
                        processFoundUser(resultingUser)
                    } else {
                        val req = TLRPC.TL_contacts_resolveUsername()
                        req.username = username
                        contextUsernameReqId = ConnectionsManager.getInstance(currentAccount).sendRequest(req) { response, error ->
                            AndroidUtilities.runOnUIThread {
                                var user: TLRPC.User? = null
                                if (error == null) {
                                    val res = response as TLRPC.TL_contacts_resolvedPeer
                                    if (!res.users.isEmpty()) {
                                        user = res.users[0]
                                        messagesController.putUser(user, false)
                                        messagesStorage.putUsersAndChats(res.users, null, true, true)
                                    }
                                }
                                processFoundUser(user)
                            }
                        }
                    }
                }
            }
        }
        AndroidUtilities.runOnUIThread(contextQueryRunnable, 400)
    }

    private fun processFoundUser(user: TLRPC.User?) {
        contextUsernameReqId = 0
        if (user != null && user.bot && user.bot_inline_placeholder != null) {
            foundContextBot = user

        } else {
            foundContextBot = null
        }
        searchForGifResults(true, foundContextBot, searchingContextQuery, "")
    }

    private fun searchForGifResults(cache: Boolean, user: TLRPC.User?, query: String?, offset: String?) {
        if (currentBotResponceType != SmartBotContentView.BotResponseType.GIF) {
            return
        }
        if (contextQueryReqId != 0) {
            ConnectionsManager.getInstance(currentAccount).cancelRequest(contextQueryReqId, true)
            contextQueryReqId = 0
        }
        if (query == null || user == null) {
            searchingContextQuery = null
            return
        }
        val key = query + "_" + offset + "_" + user.id
        val messagesStorage = MessagesStorage.getInstance(currentAccount)
        val requestDelegate = RequestDelegate { response, _ ->

            AndroidUtilities.runOnUIThread {
                if (searchingContextQuery == null || query != searchingContextQuery) {
                    return@runOnUIThread
                }
                contextQueryReqId = 0
                if (cache && response == null) {
                    searchForGifResults(false, user, query, offset)
                }
                if (response is TLRPC.TL_messages_botResults) {
                    if (!cache && response.cache_time != 0) {
                        messagesStorage.saveBotCache(key, response)
                    }
                    nextQueryOffset = response.next_offset
                    var a = 0
                    while (a < response.results.size) {
                        val result = response.results[a]
                        if (result.document !is TLRPC.TL_document && result.photo !is TLRPC.TL_photo && result.content == null && result.send_message is TLRPC.TL_botInlineMessageMediaAuto) {
                            response.results.removeAt(a)
                            a--
                        }
                        result.query_id = response.query_id
                        a++
                    }

                    val insertingPosition = items.size
                    items.addAll(insertingPosition, TabBotMediaAnswerItem.map(response.results))
                    if(insertingPosition==1){
                        onLoadGifListener.onLoadGifComplete(response.results.size)
                    }
                    if (insertingPosition != items.size) {
                        notifyItemRangeInserted(insertingPosition, items.size)
                    }
                    contextMedia = response.gallery
                    if (response.results.isEmpty()) {
                        nextQueryOffset = ""
                    }
                }
            }
        }

        if (cache) {
            messagesStorage.getBotCache(key, requestDelegate)
        } else {
            val req = TLRPC.TL_messages_getInlineBotResults()
            req.bot = MessagesController.getInstance(currentAccount).getInputUser(user)
            req.query = query
            req.offset = offset

            val lowerId = dialogId.toInt()
            if (lowerId != 0) {
                req.peer = MessagesController.getInstance(currentAccount).getInputPeer(lowerId)
            } else {
                req.peer = TLRPC.TL_inputPeerEmpty()
            }
            contextQueryReqId = ConnectionsManager.getInstance(currentAccount).sendRequest(req, requestDelegate, ConnectionsManager.RequestFlagFailOnServerErrors)
        }
    }

    inner class AdsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), LayoutContainer {

        override val containerView: View = itemView

        fun bindTo(content: SmartPanelTabContent) {
            content as TabBotAnswerItem
            ads_phrase.text = content.phrase
        }
    }

    inner class NormalViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), LayoutContainer {

        override val containerView: View = itemView

        fun bindTo(content: SmartPanelTabContent) {
            content as TabBotAnswerItem
            normal_phrase.text = content.phrase
        }
    }

    inner class BotNameViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), LayoutContainer {

        override val containerView: View = itemView

        fun bindTo(content: SmartPanelTabContent) {
            content as TabBotNameItem
            bot_name.text = content.botName
        }
    }

    interface OnLoadGifListener{
        fun onLoadGifComplete(gifsSize: Int)
    }
}
