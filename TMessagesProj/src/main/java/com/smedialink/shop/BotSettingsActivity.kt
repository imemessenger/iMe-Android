package com.smedialink.shop

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.widget.CompoundButton
import android.widget.Switch
import android.widget.TextView
import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.LocaleController
import org.telegram.messenger.MessagesController
import org.telegram.messenger.NotificationCenter
import org.telegram.messenger.R
import org.telegram.ui.ActionBar.ActionBar
import org.telegram.ui.ActionBar.BaseFragment
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.Components.SizeNotifierFrameLayout


class BotSettingsActivity : BaseFragment() {

    private lateinit var rootContainer: SizeNotifierFrameLayout


    override fun onActivityResultFragment(requestCode: Int, resultCode: Int, data: Intent?) {
        if (data != null) {
            ApplicationLoader.purchaseHelper.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun createView(context: Context?): View {
        rootContainer = SizeNotifierFrameLayout(context)
        LayoutInflater.from(context).inflate(R.layout.activity_bots_settings, rootContainer, true)

        actionBar.createMenu()
        actionBar.setBackButtonImage(R.drawable.ic_ab_back)
        actionBar.setTitle(LocaleController.getInternalString(R.string.settings_neurobots))
        actionBar.setAllowOverlayTitle(true)
        actionBar.setActionBarMenuOnItemClick(object : ActionBar.ActionBarMenuOnItemClick() {
            override fun onItemClick(id: Int) {
                if (id == -1) {
                    finishFragment()
                }
            }
        })
        val textAutoBotsLabel = rootContainer.findViewById<TextView>(R.id.textAutoBotsLabel)
        textAutoBotsLabel.text = LocaleController.getInternalString(R.string.settings_bots_label)
        val textAutoBotsDialogsLabel = rootContainer.findViewById<TextView>(R.id.textAutoBotsDialogsLabel)
        textAutoBotsDialogsLabel.text = LocaleController.getInternalString(R.string.settings_bots_automatic_dialogs)
        val textAutoBotsGroupsLabel = rootContainer.findViewById<TextView>(R.id.textAutoBotsGroupsLabel)
        textAutoBotsGroupsLabel.text = LocaleController.getInternalString(R.string.settings_bots_automatic_groups)
        val textOftenUsedLabel = rootContainer.findViewById<TextView>(R.id.textOftenUsedLabel)
        textOftenUsedLabel.text = LocaleController.getInternalString(R.string.settings_bots_often)
        val switchAutoDialogs = rootContainer.findViewById<Switch>(R.id.switchAutoDialogs)
        val switchAutoGroups = rootContainer.findViewById<Switch>(R.id.switchAutoGroups)
        val switchOftenUsed = rootContainer.findViewById<Switch>(R.id.switchOftenUsed)
        textAutoBotsLabel.setTextColor(Theme.getColor(Theme.key_dialogTextBlue2))
//        switchOftenUsed.setColors(Theme.key_switchTrack, Theme.key_switchTrackChecked, Theme.key_windowBackgroundWhite, Theme.key_windowBackgroundWhite)
//        switchAutoDialogs.setColors(Theme.key_switchTrack, Theme.key_switchTrackChecked, Theme.key_windowBackgroundWhite, Theme.key_windowBackgroundWhite)
//        switchAutoGroups.setColors(Theme.key_switchTrack, Theme.key_switchTrackChecked, Theme.key_windowBackgroundWhite, Theme.key_windowBackgroundWhite)
        val preferences = MessagesController.getGlobalMainSettings()

        val onCheckedChangeListener = CompoundButton.OnCheckedChangeListener { v, isChecked ->
            val prefKey = when (v.id) {
                R.id.switchAutoDialogs -> SHARED_PREF_KEY_AUTO_BOTS_DIALOGS_ENABLED
                R.id.switchAutoGroups -> SHARED_PREF_KEY_AUTO_BOTS_GROUPS_ENABLED
                R.id.switchOftenUsed -> SHARED_PREF_KEY_OFTEN_USED_BOTS_ENABLED
                else -> ""
            }
            val editor = preferences.edit()
            editor.putBoolean(prefKey, isChecked)
            editor.apply()
            NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.botSettingsChanged)
            NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.saveIMeBackup)

        }
        switchOftenUsed.setOnCheckedChangeListener(onCheckedChangeListener)
        switchAutoDialogs.setOnCheckedChangeListener(onCheckedChangeListener)
        switchAutoGroups.setOnCheckedChangeListener(onCheckedChangeListener)

        switchAutoDialogs.isChecked = preferences.getBoolean(SHARED_PREF_KEY_AUTO_BOTS_DIALOGS_ENABLED, AUTO_BOTS_DIALOGS_DEFAULT)
        switchAutoGroups.isChecked = preferences.getBoolean(SHARED_PREF_KEY_AUTO_BOTS_GROUPS_ENABLED, AUTO_BOTS_GROUPS_DEFAULT)
        switchOftenUsed.isChecked = preferences.getBoolean(SHARED_PREF_KEY_OFTEN_USED_BOTS_ENABLED, AUTO_BOTS_OFTEN_USED_DEFAULT)



        fragmentView = rootContainer
        return fragmentView
    }


    companion object {
        const val SHARED_PREF_KEY_AUTO_BOTS_DIALOGS_ENABLED = "SHARED_PREF_KEY_AUTO_BOTS_DIALOGS_ENABLED"
        const val SHARED_PREF_KEY_AUTO_BOTS_GROUPS_ENABLED = "SHARED_PREF_KEY_AUTO_BOTS_GROUPS_ENABLED"
        const val SHARED_PREF_KEY_OFTEN_USED_BOTS_ENABLED = "SHARED_PREF_KEY_OFTEN_USED_BOTS_ENABLED"
        const val AUTO_BOTS_DIALOGS_DEFAULT = true
        const val AUTO_BOTS_GROUPS_DEFAULT = true
        const val AUTO_BOTS_OFTEN_USED_DEFAULT = true
    }

}
