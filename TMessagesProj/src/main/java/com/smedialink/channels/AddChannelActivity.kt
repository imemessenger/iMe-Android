package com.smedialink.channels

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import org.telegram.messenger.LocaleController
import org.telegram.messenger.R
import org.telegram.ui.ActionBar.ActionBar
import org.telegram.ui.ActionBar.BaseFragment
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.Components.SizeNotifierFrameLayout

class AddChannelActivity : BaseFragment() {

    private lateinit var rootContainer: SizeNotifierFrameLayout
    private var layoutRules: FrameLayout? = null
    private var layoutRecommendations: FrameLayout? = null
    private var layoutLocation: FrameLayout? = null
    private var layoutCountries: FrameLayout? = null
    private var layoutRating: FrameLayout? = null
    private var layoutCategories: FrameLayout? = null
    private var textPolicyLabel: TextView? = null
    private var textEditLabel: TextView? = null
    private var textDispositionLabel: TextView? = null
    private var textCountriesLabel: TextView? = null
    private var textRatingLabel: TextView? = null
    private var textCategoriesLabel: TextView? = null

    private var buttonApply: Button? = null


    override fun createView(context: Context?): View {
        rootContainer = SizeNotifierFrameLayout(context)
        val contentView = LayoutInflater.from(context).inflate(R.layout.fragment_add_channel, rootContainer, true)
        initViewIds(contentView)
        setClicks()
        setText()
        actionBar.setBackButtonImage(R.drawable.ic_ab_back)
        actionBar.setTitle(LocaleController.getString("add_channel_title", R.string.add_channel_title))
        actionBar.setAllowOverlayTitle(true)
        actionBar.setActionBarMenuOnItemClick(object : ActionBar.ActionBarMenuOnItemClick() {
            override fun onItemClick(id: Int) {
                if (id == -1) {
                    finishFragment()
                }
            }
        })

        buttonApply?.setBackgroundColor(Theme.getColor(Theme.key_actionBarDefault))
        fragmentView = rootContainer
        return fragmentView
    }

    private fun initViewIds(contentView: View?) {
        layoutRules = contentView?.findViewById(R.id.layoutRules)
        layoutRecommendations = contentView?.findViewById(R.id.layoutRecommendations)
        layoutLocation = contentView?.findViewById(R.id.layoutLocation)
        layoutRating = contentView?.findViewById(R.id.layoutRating)
        layoutCategories = contentView?.findViewById(R.id.layoutCategories)
        buttonApply = contentView?.findViewById(R.id.buttonApply)
        layoutCountries = contentView?.findViewById(R.id.layoutCountries)
        textPolicyLabel = contentView?.findViewById(R.id.textPolicyLabel)
        textEditLabel = contentView?.findViewById(R.id.textEditLabel)
        textDispositionLabel = contentView?.findViewById(R.id.textDispositionLabel)
        textCountriesLabel = contentView?.findViewById(R.id.textCountriesLabel)
        textRatingLabel = contentView?.findViewById(R.id.textRatingLabel)
        textCategoriesLabel = contentView?.findViewById(R.id.textCategoriesLabel)
    }

    private fun setText() {
        textPolicyLabel?.text = LocaleController.getInternalString(R.string.publication_rules)
        textEditLabel?.text = LocaleController.getInternalString(R.string.design_recommendations)
        textDispositionLabel?.text = LocaleController.getInternalString(R.string.location_in_the_collection)
        textCountriesLabel?.text = LocaleController.getInternalString(R.string.countries_for_placement)
        textRatingLabel?.text = LocaleController.getInternalString(R.string.rating)
        textCategoriesLabel?.text = LocaleController.getInternalString(R.string.categories)
        buttonApply?.text = LocaleController.getInternalString(R.string.apply)

    }

    private fun setClicks() {
        val onClickListener: View.OnClickListener = View.OnClickListener {
            val infoTitle = when (it) {
                layoutRules -> {
                    LocaleController.getInternalString(R.string.publication_rules)
                }
                layoutRecommendations -> {
                    LocaleController.getInternalString(R.string.design_recommendations)
                }
                layoutLocation -> {
                    LocaleController.getInternalString(R.string.location_in_the_collection)
                }
                layoutRating -> {
                    LocaleController.getInternalString(R.string.rating)
                }
                layoutCountries -> {
                    LocaleController.getInternalString(R.string.countries_for_placement)
                }
                layoutCategories -> {
                    LocaleController.getInternalString(R.string.categories)
                }
                else -> ""
            }
            val infoText = when (it) {
                layoutRules -> {
                    LocaleController.getInternalString(R.string.publication_rules_text)
                }
                layoutRecommendations -> {
                    LocaleController.getInternalString(R.string.design_recommendations_text)
                }
                layoutLocation -> {
                    LocaleController.getInternalString(R.string.location_in_the_collection_text)
                }
                layoutCountries -> {
                    LocaleController.getInternalString(R.string.countries_for_placement_text)
                }
                layoutRating -> {
                    LocaleController.getInternalString(R.string.rating_text)
                }
                layoutCategories -> {
                    LocaleController.getInternalString(R.string.categories_text)
                }
                else -> ""
            }
            presentFragment(InfoActivity(Bundle().apply {
                putString(InfoActivity.BUNDLE_KEY_INFO_TEXT, infoText)
                putString(InfoActivity.BUNDLE_KEY_INFO_TITLE, infoTitle)
            }))
        }
        layoutRules?.setOnClickListener(onClickListener)
        layoutRecommendations?.setOnClickListener(onClickListener)
        layoutLocation?.setOnClickListener(onClickListener)
        layoutCountries?.setOnClickListener(onClickListener)
        layoutRating?.setOnClickListener(onClickListener)
        layoutCategories?.setOnClickListener(onClickListener)
        buttonApply?.setOnClickListener {
            presentFragment(InfoActivity(Bundle().apply {
                putString(InfoActivity.BUNDLE_KEY_INFO_TEXT, LocaleController.getInternalString(R.string.apply_text))
                putString(InfoActivity.BUNDLE_KEY_INFO_TITLE, LocaleController.getInternalString(R.string.apply))
                putBoolean(InfoActivity.BUNDLE_KEY_NEED_SPAN_LINK, true)
            }))
        }
    }
}