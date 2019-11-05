package com.smedialink.shop.configuration

import org.solovyev.android.checkout.Billing

/**
 * Created by Oleg Sheliakin on 14.01.2019.
 * Contact me by email - olegsheliakin@gmail.com
 */
interface BillingProvider {
    fun provideBilling() : Billing
}