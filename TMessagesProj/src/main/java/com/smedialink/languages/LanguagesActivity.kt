package com.smedialink.languages

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.smedialink.storage.data.repository.CountriesRepository
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import org.telegram.messenger.*
import org.telegram.ui.ActionBar.ActionBar
import org.telegram.ui.ActionBar.ActionBarMenuItem
import org.telegram.ui.ActionBar.BaseFragment
import org.telegram.ui.Components.SizeNotifierFrameLayout

class LanguagesActivity : BaseFragment(), LanguagesAdapter.OnLanguageClickListener {


    private lateinit var rootContainer: SizeNotifierFrameLayout
    private val disposable = CompositeDisposable()
    private val countriesDisposable: Disposable? = null
    private var searchItem: ActionBarMenuItem? = null

    private var recyclerLanguages: RecyclerView? = null
    private var languagesAdapter = LanguagesAdapter(this)
    private var me = MessagesController.getInstance(UserConfig.selectedAccount)
            .getUser(UserConfig.getInstance(UserConfig.selectedAccount).getClientUserId())

    override fun createView(context: Context?): View {
        rootContainer = SizeNotifierFrameLayout(context)
        val contentView = LayoutInflater.from(context).inflate(R.layout.fragment_countries, rootContainer, true)
        recyclerLanguages = contentView.findViewById(R.id.recyclerCountries)
        recyclerLanguages?.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = languagesAdapter
        }

        actionBar.setBackButtonImage(R.drawable.ic_ab_back)
        actionBar.setTitle(LocaleController.getString("bots_language", R.string.bots_language))
        actionBar.setAllowOverlayTitle(true)
        actionBar.setActionBarMenuOnItemClick(object : ActionBar.ActionBarMenuOnItemClick() {
            override fun onItemClick(id: Int) {
                if (id == -1) {
                    finishFragment()
                }
            }
        })
        showLanguages()
        fragmentView = rootContainer
        return fragmentView
    }


    override fun onFragmentDestroy() {
        disposable.clear()
        super.onFragmentDestroy()
    }


    override fun onLanguageClick(language: LanguageViewModel) {
        CountriesRepository.getInstance(parentActivity)
                .setCurrentBotLanguage(language.id)
        showLanguages()
        NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.botLanguageChanged)
    }

    private fun showLanguages() {
        val currentLanguage = CountriesRepository.getInstance(parentActivity).getCurrentBotLanguage(
                phone = me.phone,
                locale = LocaleController.getInstance().currentLocaleInfo.langCode
        )
        val languages = listOf(
                LanguageViewModel(
                        id = "ru",
                        name = LocaleController.getString("language_russian", R.string.language_russian)
                ),
                LanguageViewModel(
                        id = "eng",
                        name = LocaleController.getString("language_english", R.string.language_english)
                )
        )
        languages.map {
            it.apply {
                checked = currentLanguage == this.id
            }
        }
        languagesAdapter.setLanguages(languages)
    }


}