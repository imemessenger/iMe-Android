package com.smedialink.channels.view.custom

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.ImageLocation
import org.telegram.messenger.ImageReceiver
import org.telegram.tgnet.TLRPC
import org.telegram.ui.Components.AvatarDrawable

class TelegramImageView(context: Context, attr: AttributeSet?) : View(context, attr) {

    private val avatarImage = ImageReceiver(this)
    private val avatarDrawable = AvatarDrawable()

    init {
        avatarImage.roundRadius = AndroidUtilities.dp(26f)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        avatarImage.onDetachedFromWindow()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        avatarImage.onAttachedToWindow()
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        avatarImage.draw(canvas)
    }

    fun setInfo(chat: TLRPC.Chat) {
        avatarDrawable.setInfo(chat)
        avatarImage.setImage(ImageLocation.getForChat(chat, false), "50_50", avatarDrawable, null, chat, 0)
        buildLayout()
        invalidate()
    }

    fun buildLayout() {
        avatarImage.setImageCoords(0, 0, AndroidUtilities.dp(52f), AndroidUtilities.dp(52f))
    }
}