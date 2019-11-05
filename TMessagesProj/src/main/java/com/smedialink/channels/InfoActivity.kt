package com.smedialink.channels

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import org.telegram.messenger.MessagesController
import org.telegram.messenger.R
import org.telegram.ui.ActionBar.ActionBar
import org.telegram.ui.ActionBar.BaseFragment
import org.telegram.ui.Components.SizeNotifierFrameLayout


class InfoActivity(args: Bundle? = null) : BaseFragment(args) {
    private lateinit var rootContainer: SizeNotifierFrameLayout
    private var textInfo: TextView? = null
    private var infoTitle: String? = null
    private var infoText: String? = null
    private var needSpanLink: Boolean = false

    override fun onFragmentCreate(): Boolean {
        super.onFragmentCreate()

        if (getArguments() != null) {
            this.infoTitle = getArguments().getString(BUNDLE_KEY_INFO_TITLE)
            this.infoText = getArguments().getString(BUNDLE_KEY_INFO_TEXT)
            this.needSpanLink = getArguments().getBoolean(BUNDLE_KEY_NEED_SPAN_LINK)
        }

        return true
    }

    override fun createView(context: Context?): View {
        rootContainer = SizeNotifierFrameLayout(context)
        val contentView = LayoutInflater.from(context).inflate(R.layout.fragment_info, rootContainer, true)
        textInfo = contentView?.findViewById(R.id.textInfo)
        actionBar.setBackButtonImage(R.drawable.ic_ab_back)
        actionBar.setTitle(infoTitle ?: "")
        actionBar.setAllowOverlayTitle(true)
        actionBar.setActionBarMenuOnItemClick(object : ActionBar.ActionBarMenuOnItemClick() {
            override fun onItemClick(id: Int) {
                if (id == -1) {
                    finishFragment()
                }
            }
        })

        if(needSpanLink){
            showSpanText()
        }else{
            textInfo?.text = infoText ?: ""
        }

        fragmentView = rootContainer
        return fragmentView
    }


    private fun showSpanText() {
        val ss = SpannableString(infoText)
        val chatLink = "iMeMessenger"
        val spanText = "@$chatLink"
        val clickableSpan = object : ClickableSpan() {
            override fun onClick(textView: View) {
                MessagesController.getInstance(currentAccount).openByUserName(
                        chatLink,
                        this@InfoActivity, 0
                )
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = false
            }
        }
        val startSpanIndex = infoText?.indexOf(spanText) ?: 0
        val endSpanIndex = startSpanIndex + spanText.length
        ss.setSpan(clickableSpan, startSpanIndex, endSpanIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        textInfo?.text = ss
        textInfo?.movementMethod = LinkMovementMethod.getInstance()
        textInfo?.highlightColor = Color.TRANSPARENT
    }


    companion object {
        const val BUNDLE_KEY_INFO_TITLE = "BUNDLE_KEY_INFO_TITLE"
        const val BUNDLE_KEY_INFO_TEXT = "BUNDLE_KEY_INFO_TEXT"
        const val BUNDLE_KEY_NEED_SPAN_LINK = "BUNDLE_KEY_NEED_SPAN_LINK"

    }
}