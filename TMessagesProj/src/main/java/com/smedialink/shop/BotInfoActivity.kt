package com.smedialink.shop

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.flexbox.FlexboxLayout
import com.smedialink.bots.data.model.BotStatus
import com.smedialink.bots.domain.model.BotLanguage
import com.smedialink.bots.domain.model.ShopItem
import com.smedialink.shop.util.pxToDp
import com.smedialink.smartpanel.extension.loadFrom
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import me.zhanghai.android.materialratingbar.MaterialRatingBar
import org.telegram.messenger.*
import org.telegram.ui.ActionBar.ActionBar
import org.telegram.ui.ActionBar.BaseFragment
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.Components.SizeNotifierFrameLayout

class BotInfoActivity(args: Bundle?) : BaseFragment(args) {

    private lateinit var rootContainer: SizeNotifierFrameLayout
    private var botId: String = ""
    private var title: String = ""

    private var avatar: ImageView? = null
    private var botName: TextView? = null
    private var botDescription: TextView? = null
    private var tagsContainer: FlexboxLayout? = null
    private var ratingValue: TextView? = null
    private var ratingLabel: TextView? = null
    private var botCurrentLanguage: TextView? = null
    private var botAnalogLanguage: TextView? = null
    private var instalsLabel: TextView? = null
    private var themesLabel: TextView? = null
    private var phrasesLabel: TextView? = null
    private var developerLabel: TextView? = null
    private var progressBar: ProgressBar? = null
    private var layoutContainer: ConstraintLayout? = null
    private var rateLabel: TextView? = null
    private var installsValue: TextView? = null
    private var themesValue: TextView? = null
    private var phrasesValue: TextView? = null
    private var dateAdded: TextView? = null
    private var dateUpdated: TextView? = null
    private var ratingBar: MaterialRatingBar? = null
    private var button: TextView? = null

    private var botAnalog: ShopItem? = null

    private val userId = UserConfig.getInstance(currentAccount).getClientUserId().toLong()

    private val disposable = CompositeDisposable()

    override fun onFragmentCreate(): Boolean {
        super.onFragmentCreate()

        if (getArguments() != null) {
            botId = getArguments().getString("botId") ?: ""
            title = getArguments().getString("title") ?: ""
        }

        return true
    }

    override fun onFragmentDestroy() {
        disposable.clear()
        super.onFragmentDestroy()
    }

    override fun onActivityResultFragment(requestCode: Int, resultCode: Int, data: Intent?) {
        if (data != null) {
            ApplicationLoader.purchaseHelper.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun createView(context: Context?): View {
        rootContainer = SizeNotifierFrameLayout(context)
        val contentView = LayoutInflater.from(context).inflate(R.layout.bots_description_content, rootContainer, true)
        initViewIds(contentView)

        actionBar.createMenu()
        actionBar.setBackButtonImage(R.drawable.ic_ab_back)
        actionBar.setTitle(title)
        actionBar.setAllowOverlayTitle(true)
        actionBar.setActionBarMenuOnItemClick(object : ActionBar.ActionBarMenuOnItemClick() {
            override fun onItemClick(id: Int) {
                if (id == -1) {
                    finishFragment()
                }
            }
        })
        showProgress(true)
        subscribeToBotContent()

        fragmentView = rootContainer
        return fragmentView
    }

    private fun initViewIds(contentView: View?) {
        avatar = contentView?.findViewById(R.id.bot_avatar)
        botName = contentView?.findViewById(R.id.bot_name)
        botDescription = contentView?.findViewById(R.id.bot_description)
        tagsContainer = contentView?.findViewById(R.id.tags_container)
        ratingValue = contentView?.findViewById(R.id.rating_number)
        ratingLabel = contentView?.findViewById(R.id.rating_label)
        instalsLabel = contentView?.findViewById(R.id.installs_label)
        themesLabel = contentView?.findViewById(R.id.themes_label)
        developerLabel = contentView?.findViewById(R.id.bot_developer)
        botCurrentLanguage = contentView?.findViewById(R.id.bot_current_language)
        botAnalogLanguage = contentView?.findViewById(R.id.bot_analog_language)
        phrasesLabel = contentView?.findViewById(R.id.phrases_label)
        installsValue = contentView?.findViewById(R.id.installs_counter)
        themesValue = contentView?.findViewById(R.id.themes_counter)
        phrasesValue = contentView?.findViewById(R.id.phrases_counter)
        dateAdded = contentView?.findViewById(R.id.bot_date_added)
        dateUpdated = contentView?.findViewById(R.id.bot_date_updated)
        ratingBar = contentView?.findViewById(R.id.rating)
        button = contentView?.findViewById(R.id.bot_info_button)
        rateLabel = contentView?.findViewById(R.id.textRateBot)
        layoutContainer = contentView?.findViewById(R.id.layoutContainer)
        progressBar= contentView?.findViewById(R.id.progressBar)
    }

    private fun showProgress(isLoading: Boolean) {
        val visibility = if (isLoading.not()) View.VISIBLE else View.GONE
        layoutContainer?.visibility = visibility
        button?.visibility = visibility
        progressBar?.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun subscribeToBotContent() {
        ApplicationLoader.smartBotsManager
                .getSingleBotObservable(botId, LocaleController.getInstance().currentLocaleInfo.langCode)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ item ->
                    displayBotItem(item)
                }, { t ->
                    t.printStackTrace()
                })
                .also { disposable.add(it) }
    }

    @SuppressLint("SetTextI18n")
    private fun displayBotItem(item: ShopItem) {
        showProgress(false)
        observeBotAnalog(item)
        button?.setBackgroundColor(Theme.getColor(Theme.key_actionBarDefault))
        avatar?.loadFrom(item.avatar, parentActivity)
        botName?.text = item.title
        botDescription?.text = item.description
        ratingValue?.text = "%.1f".format(item.rating)
        ratingLabel?.text = LocaleController.formatPluralString("Ratings", item.reviews.toInt())
        themesLabel?.text = LocaleController.getString("themes", R.string.themes)
        phrasesLabel?.text = LocaleController.getString("phrases", R.string.phrases)
        developerLabel?.text = LocaleController.getString("developer", R.string.developer)
        instalsLabel?.text = LocaleController.getString("installs", R.string.installs)
        themesValue?.text = item.themes.toString()
        phrasesValue?.text = item.phrases.toString()
        installsValue?.text = item.installs.toString()
        rateLabel?.text = LocaleController.getString("rate_bot", R.string.rate_bot)
        val currentLanguageString = when (item.language) {
            BotLanguage.RU -> LocaleController.getInternalString(R.string.russian)
            BotLanguage.EN -> LocaleController.getInternalString(R.string.english)
        }
        botCurrentLanguage?.text = "${LocaleController.getInternalString(R.string.supported_languages)}: \n$currentLanguageString"
        botAnalogLanguage?.setOnClickListener {
            botAnalog?.let { analog ->
                val arguments = Bundle()
                arguments.putString("botId", analog.botId)
                arguments.putString("title", analog.title)
                presentFragment(BotInfoActivity(arguments), true)
            }
        }

        if (item.ownRating == 0) {
            ratingBar?.setIsIndicator(false)
            ratingBar?.rating = 0f
            ratingBar?.setOnRatingChangeListener { ratingBar, ratingValue ->
                ratingBar.rating = ratingValue
                ratingBar.setIsIndicator(true)
                ApplicationLoader.smartBotsManager.sendBotRatingEvent(botId, userId, ratingValue.toInt())
            }
        } else {
            ratingBar?.setIsIndicator(true)
            ratingBar?.rating = item.ownRating.toFloat()
        }


        tagsContainer?.removeAllViews()
        parentActivity?.let { context ->
            // Даты
            dateAdded?.text = "${LocaleController.getString("neuro_bot_info_date_added_placeholder", R.string.neuro_bot_info_date_added_placeholder)} ${item.created}"
            dateUpdated?.text = "${LocaleController.getString("neuro_bot_info_date_updated_placeholder", R.string.neuro_bot_info_date_updated_placeholder)} ${item.updated}"

            // Теги
            val margin = context.pxToDp(16)
            item.tags
                    .filter { !it.hidden }
                    .forEach { tag ->
                        val tagTextView = TextView(context)
                        tagTextView.text = tag.title
                        tagTextView.setBackgroundResource(R.drawable.bg_tag)
                        tagsContainer?.addView(tagTextView)
                        val params = tagTextView.layoutParams as FlexboxLayout.LayoutParams
                        params.setMargins(margin, margin, margin, margin)
                        tagTextView.layoutParams = params
                    }

            // Стейт кнопки
            button?.text =
                    when (item.status) {
                        BotStatus.PAID -> item.price ?: "Free"
                        BotStatus.AVAILABLE -> LocaleController.getString("shop_button_label_download", R.string.shop_button_label_download)
                        BotStatus.UPDATE_AVAILABLE -> LocaleController.getString("shop_button_label_update", R.string.shop_button_label_update)
                        BotStatus.DOWNLOADING -> LocaleController.getString("shop_button_label_downloading", R.string.shop_button_label_downloading)
                        BotStatus.ENABLED -> LocaleController.getString("shop_button_label_disable", R.string.shop_button_label_disable)
                        BotStatus.DISABLED -> LocaleController.getString("shop_button_label_enable", R.string.shop_button_label_enable)
                    }
        }

        button?.setOnClickListener {
            NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.botButtonClicked, item)
        }
    }

    private fun observeBotAnalog(currentBot: ShopItem) {
        ApplicationLoader.smartBotsManager
                .getAllBotsObservable(LocaleController.getInstance().currentLocaleInfo.langCode)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ items ->
                    botAnalog = items.findLast {
                        when (currentBot.language) {
                            BotLanguage.RU -> {
                                it.botId == currentBot.botId + "_eng"
                            }
                            BotLanguage.EN -> {
                                it.botId == currentBot.botId.replace("_eng", "")
                            }
                        }
                    }
                    botAnalog?.let {
                        botAnalogLanguage?.text = when (it.language) {
                            BotLanguage.RU -> LocaleController.getInternalString(R.string.russian)
                            BotLanguage.EN -> LocaleController.getInternalString(R.string.english)
                        }
                    }
                }, { t ->
                    t.printStackTrace()
                })
                .also { disposable.add(it) }
    }
}
