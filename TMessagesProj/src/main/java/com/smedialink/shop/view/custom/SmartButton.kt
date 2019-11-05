package com.smedialink.shop.view.custom

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import com.smedialink.bots.data.model.BotStatus
import com.smedialink.shop.util.textColor
import me.grantland.widget.AutofitTextView
import org.telegram.messenger.R

class SmartButton : AutofitTextView {

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    companion object {
        fun resolveState(from: BotStatus): State =
                when (from) {
                    BotStatus.AVAILABLE -> State.FREE
                    BotStatus.PAID -> State.PAID
                    BotStatus.UPDATE_AVAILABLE -> State.UPDATE_AVAILABLE
                    BotStatus.DOWNLOADING -> State.DOWNLOAD_IN_PROGRESS
                    BotStatus.ENABLED -> State.ENABLED
                    BotStatus.DISABLED -> State.DISABLED
                }

        fun resolveState(joinedChannel: Boolean): State =
                if (joinedChannel) State.ENABLED
                else State.FREE
    }

    enum class State(@ColorRes val colorRes: Int, @DrawableRes val backgroundRes: Int) {
        FREE(R.color.colorShopButtonTextLight, R.drawable.bg_shop_button_available),
        PAID(R.color.colorShopButtonTextLight, R.drawable.bg_shop_button_available),
        UPDATE_AVAILABLE(R.color.colorShopButtonTextLight, R.drawable.bg_shop_button_available),
        ENABLED(R.color.colorShopButtonTextDisable, R.drawable.bg_shop_button_enabled),
        DISABLED(R.color.colorThemeBlue, R.drawable.bg_shop_button_disabled),
        DOWNLOAD_IN_PROGRESS(R.color.colorShopButtonTextDisable, 0);
    }

    var buttonState: State
        set(value) {
            field = value
            textColor = value.colorRes
            setBackgroundResource(value.backgroundRes)
        }

    var buttonText: String
        set(value) {
            field = value
            text = buttonText
        }

    init {
        isAllCaps = true
        buttonState = State.ENABLED
        buttonText = ""
    }
}
