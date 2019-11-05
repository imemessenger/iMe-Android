package com.smedialink.shop.configuration

import org.solovyev.android.checkout.Billing
import org.solovyev.android.checkout.PurchaseVerifier

/**
 * Created by Oleg Sheliakin on 14.01.2019.
 * Contact me by email - olegsheliakin@gmail.com
 */
class BillingConfiguration : Billing.DefaultConfiguration() {
    override fun getPublicKey(): String {
        val s = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAjNETw+rBz7l/9U7vTAyf98JT/6OMwFDHaIP3zUJtEahsZ4mjvJ3E/zhb/ji94hqaiHYpQ6ubLBT0uqgydidKHCucP4bLFVcV+SM5tGJR6ZmF8yUjm6VN/5DyePuiSQCMerNOoIocMecZmH6/WCAj+dr5jLgO4V2K/FfAYMhNBh0NBY30O67Md2XiSIujS/bAg8cBaMUk3/nlbUePWn/GZRotDjMQ6cS0pFfm7v7gpAHZve8u2b4AQ7GJKcNtqteSfJ4PwxenlIZgwumyQO7XfFY5RdMHFWQFtCJV5LTHmmRmaqrHo9qfvjrw5MdAEsSsi1TAIpAEhQnYHHdnsxN91QIDAQAB"

        return s
    }

    override fun getPurchaseVerifier(): PurchaseVerifier {
        return super.getPurchaseVerifier()
    }
}