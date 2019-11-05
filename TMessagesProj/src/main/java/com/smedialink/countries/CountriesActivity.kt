package com.smedialink.countries

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.smedialink.countries.adapter.CountriesAdapter
import com.smedialink.countries.model.CountryViewModel
import com.smedialink.countries.model.CountryViewModelMapper
import com.smedialink.storage.data.repository.CountriesRepository
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import org.telegram.messenger.*
import org.telegram.ui.ActionBar.ActionBar
import org.telegram.ui.ActionBar.ActionBarMenuItem
import org.telegram.ui.ActionBar.BaseFragment
import org.telegram.ui.Components.SizeNotifierFrameLayout

class CountriesActivity : BaseFragment(), CountriesAdapter.OnCountryClickListener {


    private lateinit var rootContainer: SizeNotifierFrameLayout
    private val disposable = CompositeDisposable()
    private val countriesDisposable: Disposable? = null
    private var searchItem: ActionBarMenuItem? = null
    private var countryChanged: Boolean = false

    private var recyclerCountries: RecyclerView? = null
    private var countriesAdapter = CountriesAdapter(this)
    private var countryViewModelMapper = CountryViewModelMapper()
    private var me = MessagesController.getInstance(UserConfig.selectedAccount)
            .getUser(UserConfig.getInstance(UserConfig.selectedAccount).getClientUserId())

    override fun createView(context: Context?): View {
        rootContainer = SizeNotifierFrameLayout(context)
        val contentView = LayoutInflater.from(context).inflate(R.layout.fragment_countries, rootContainer, true)
        recyclerCountries = contentView.findViewById(R.id.recyclerCountries)
        recyclerCountries?.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = countriesAdapter
        }
        val menu = actionBar.createMenu()
        searchItem = menu.addItem(0, R.drawable.ic_ab_search).setIsSearchField(true).setActionBarMenuItemSearchListener(object : ActionBarMenuItem.ActionBarMenuItemSearchListener() {

            override fun onSearchExpand() {
            }

            override fun onSearchCollapse() {
                countriesAdapter.filter("")
            }

            override fun onTextChanged(editText: EditText?) {
                val text = editText?.text.toString()
                if (text.isNotEmpty()) {
                    countriesAdapter.filter(text)
                }
            }
        })
        searchItem?.setSearchFieldHint(LocaleController.getString("Search", R.string.Search))
        actionBar.setBackButtonImage(R.drawable.ic_ab_back)
        actionBar.setTitle(LocaleController.getString("channel_country", R.string.channel_country))
        actionBar.setAllowOverlayTitle(true)
        actionBar.setActionBarMenuOnItemClick(object : ActionBar.ActionBarMenuOnItemClick() {
            override fun onItemClick(id: Int) {
                if (id == -1) {
                    finishFragment()
                }
            }
        })
        observeRemoteCountries()
        subscribeCountries()
        fragmentView = rootContainer
        return fragmentView
    }


    override fun onFragmentDestroy() {
        disposable.clear()
        super.onFragmentDestroy()
    }

    override fun onCountryClick(country: CountryViewModel) {
        countryChanged = true
        CountriesRepository.getInstance(parentActivity)
                .setCurrentCountry(country.id)
        subscribeCountries()
        NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.channelsCountryChanged)

    }

    private fun subscribeCountries() {
        countriesDisposable?.dispose()
        CountriesRepository.getInstance(parentActivity)
                .observeCountries(me.phone, LocaleController.getInstance().currentLocaleInfo.langCode)
                .map { countries -> if (!countryChanged) countries.sortedByDescending { it.checked } else countries }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    countriesAdapter.setCountries(countryViewModelMapper.mapItems(it, LocaleController.getInstance().currentLocaleInfo.langCode))
                }, {})
                .also {
                    disposable.add(it)
                    countriesDisposable?.dispose()
                }
    }

    private fun observeRemoteCountries() {
        CountriesRepository.getInstance(parentActivity)
                .observeRemoteCountries(me.phone, LocaleController.getInstance().currentLocaleInfo.langCode)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe({}, { it.printStackTrace() })
                .also { disposable.add(it) }
    }

}