package com.smedialink.smartpanel.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.smedialink.smartpanel.SmartPanelView
import com.smedialink.smartpanel.adapter.SmartBotContentAdapter
import com.smedialink.smartpanel.model.SmartBotTab
import com.smedialink.smartpanel.model.content.TabBotAnswerItem
import com.smedialink.smartpanel.model.content.TabBotMediaAnswerItem
import org.telegram.messenger.*
import org.telegram.tgnet.TLRPC
import org.telegram.ui.Cells.ContextLinkCell
import org.telegram.ui.Components.ExtendedGridLayoutManager
import org.telegram.ui.Components.LayoutHelper
import org.telegram.ui.Components.RecyclerListView
import org.telegram.ui.Components.Size
import org.telegram.ui.PhotoViewer

/**
 * Вью для отображения вкладки бота
 */
@SuppressLint("ViewConstructor")
class SmartBotContentView(
        context: Context,
        private val content: SmartBotTab,
        private var currentBotResponseType: BotResponseType = BotResponseType.TEXT,
        listener: SmartPanelView.Listener?,
        dialogId: Long
) : FrameLayout(context), SmartBotContentAdapter.OnLoadGifListener {

    private companion object {
        const val SPAN_COUNT = 100
    }

    private val listViewAdapter: SmartBotContentAdapter = SmartBotContentAdapter(dialogId,this)
    private val listView: RecyclerListView = RecyclerListView(context)
    private val textEmptyGif: TextView = TextView(context)
    private val listViewLayoutManager: ExtendedGridLayoutManager = object : ExtendedGridLayoutManager(context, SPAN_COUNT) {

        private val size = Size()

        override fun getSizeForItem(i: Int): Size {
            // чтобы getSpanSizeForItem из ExtendedGridLayoutManager корректно определял что текстовые
            // ответы занимают всю строку, нужно явно указать ширину которую обычно имеет итем из одной строки,
            // использовал ширину listView
            size.width = listView.width.toFloat()
            size.height = listView.width / 2f

            val item = listViewAdapter.getItem(i)
            val inlineResult = if (item is TabBotMediaAnswerItem) item.media else null
            if (inlineResult is TLRPC.BotInlineResult) {
                if (inlineResult.document != null) {
                    val thumb = FileLoader.getClosestPhotoSizeWithSize(inlineResult.document.thumbs, 90)
                    size.width = (thumb?.w ?: 100).toFloat()
                    size.height = (thumb?.h ?: 100).toFloat()
                    for (b in inlineResult.document.attributes.indices) {
                        val attribute = inlineResult.document.attributes[b]
                        if (attribute is TLRPC.TL_documentAttributeImageSize || attribute is TLRPC.TL_documentAttributeVideo) {
                            size.width = attribute.w.toFloat()
                            size.height = attribute.h.toFloat()
                            break
                        }
                    }
                } else if (inlineResult.content != null) {
                    for (b in inlineResult.content.attributes.indices) {
                        val attribute = inlineResult.content.attributes[b]
                        if (attribute is TLRPC.TL_documentAttributeImageSize || attribute is TLRPC.TL_documentAttributeVideo) {
                            size.width = attribute.w.toFloat()
                            size.height = attribute.h.toFloat()
                            break
                        }
                    }
                } else if (inlineResult.thumb != null) {
                    for (b in inlineResult.thumb.attributes.indices) {
                        val attribute = inlineResult.thumb.attributes[b]
                        if (attribute is TLRPC.TL_documentAttributeImageSize || attribute is TLRPC.TL_documentAttributeVideo) {
                            size.width = attribute.w.toFloat()
                            size.height = attribute.h.toFloat()
                            break
                        }
                    }
                } else if (inlineResult.photo != null) {
                    val photoSize = FileLoader.getClosestPhotoSizeWithSize(inlineResult.photo.sizes, AndroidUtilities.photoSize)
                    if (photoSize != null) {
                        size.width = photoSize.w.toFloat()
                        size.height = photoSize.h.toFloat()
                    }
                }
            }
            return size
        }
    }

    private val gifContextProvider = object : PhotoViewer.EmptyPhotoViewerProvider() {

        override fun getPlaceForPhoto(messageObject: MessageObject?, fileLocation: TLRPC.FileLocation?, index: Int, needPreview: Boolean): PhotoViewer.PlaceProviderObject? {
            if (index < 0 || index >= listViewAdapter.getMediaContent().size) {
                return null
            }
            val count = listViewAdapter.itemCount
            val result = listViewAdapter.getMediaContent()[index]

            for (a in 0 until count) {
                var imageReceiver: ImageReceiver? = null
                val view = listView.getChildAt(a)
                if (view is ContextLinkCell) {
                    if (view.result == result) {
                        imageReceiver = view.photoImage
                    }
                }

                if (imageReceiver != null) {
                    val coords = IntArray(2)
                    view.getLocationInWindow(coords)
                    val obj = PhotoViewer.PlaceProviderObject()
                    obj.viewX = coords[0]
                    obj.viewY = coords[1] - if (Build.VERSION.SDK_INT >= 21) 0 else AndroidUtilities.statusBarHeight
                    obj.parentView = listView
                    obj.imageReceiver = imageReceiver
                    obj.thumb = imageReceiver.bitmapSafe
                    obj.radius = imageReceiver.roundRadius
                    return obj
                }
            }
            return null
        }

        // TODO iMe
        override fun sendButtonPressed(index: Int, videoEditedInfo: VideoEditedInfo?, notify: Boolean, scheduleDate: Int) {
            listener?.sendChosenGif(index, listViewAdapter.getGifBotId(), listViewAdapter.getGifBotName())
        }
    }

    init {
        listViewAdapter.setHasStableIds(true)
        listViewAdapter.setTextData(content, currentBotResponseType)
        textEmptyGif.text = LocaleController.getString("text_empty_gif",R.string.text_empty_gif)
        textEmptyGif.setTextColor(ContextCompat.getColor(context, R.color.gray_message))
        textEmptyGif.textSize = 14f
        textEmptyGif.visibility = View.GONE
        listView.adapter = listViewAdapter
        listView.layoutManager = listViewLayoutManager

        listView.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
                outRect.left = 0
                outRect.right = 0
                outRect.top = 0
                outRect.bottom = 0
                if (parent.layoutManager == listViewLayoutManager) {
                    val position = parent.getChildAdapterPosition(view)
                    if (listViewAdapter.getItem(position) is TabBotMediaAnswerItem) {
                        outRect.top = AndroidUtilities.dp(2f)
                        outRect.right = if (listViewLayoutManager.isLastInRow(position)) 0 else AndroidUtilities.dp(2f)
                    }
                }
            }
        })

        listView.setOnItemClickListener { _, position ->
            if (position < listViewAdapter.mediaContentOffset) {
                val item = listViewAdapter.getTextContent()[position] as? TabBotAnswerItem
                item?.let { listener?.onTextAnswerSelected(it, position) }
            } else {
                val gifsList = listViewAdapter.getMediaContent()
                val gifPosition = position - listViewAdapter.mediaContentOffset
                listener?.onGifAnswerSelected(gifContextProvider, gifsList, gifPosition)
            }
        }

        listView.setOnScrollListener(object : RecyclerView.OnScrollListener() {

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val lastVisibleItem = listViewLayoutManager.findLastVisibleItemPosition()
                val visibleItemCount = if (lastVisibleItem == RecyclerView.NO_POSITION) 0 else lastVisibleItem
                if (visibleItemCount > 0 && lastVisibleItem > listViewAdapter.itemCount - 5) {
                    listViewAdapter.searchGifsForNextOffset()
                }
            }
        })

        listViewLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return if (listViewAdapter.getItem(position) is TabBotMediaAnswerItem) {
                    listViewLayoutManager.getSpanSizeForItem(position)
                } else {
                    SPAN_COUNT
                }
            }
        }

        addView(textEmptyGif, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER))
        addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.CENTER))
    }

    override fun onDetachedFromWindow() {
        listViewAdapter.onDestroy()
        super.onDetachedFromWindow()
    }

    override fun onLoadGifComplete(gifsSize: Int) {
        updateEmptyGifText(gifsSize)
    }


    fun setBotResponseType(botResponseType: BotResponseType) {
        currentBotResponseType = botResponseType
        listViewAdapter.setTextData(content, currentBotResponseType)
    }

    private fun updateEmptyGifText(gifsSize: Int) {
        if (currentBotResponseType == BotResponseType.GIF) {
            if (gifsSize==0) {
                textEmptyGif.visibility = View.VISIBLE
            }else{
                textEmptyGif.visibility = View.GONE
            }
        } else textEmptyGif.visibility = View.GONE
    }

    enum class BotResponseType {
        TEXT,
        GIF
    }
}
