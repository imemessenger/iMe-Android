package com.smedialink.shop

import android.app.Activity
import android.content.Intent
import android.util.Log
import com.smedialink.bots.domain.model.ShopProduct
import com.smedialink.bots.usecase.AiBotsManager
import com.smedialink.shop.configuration.BillingProvider
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import org.solovyev.android.checkout.*

/**
 * Created by Oleg Sheliakin on 14.01.2019.
 * Contact me by email - olegsheliakin@gmail.com
 */

//Обертка над библиотекой Checkout
class PurchaseHelper private constructor(private val aigramBotsManager: AiBotsManager) {

    private var uiCheckout: UiCheckout? = null
    private val cachedPurchases: MutableList<ShopProduct> = mutableListOf()
    private val disposable: CompositeDisposable = CompositeDisposable()

    val products: List<ShopProduct>
        get() = cachedPurchases

    private var isPurchaseFlowActive = false
    private var isPurchaseHelperActive = false

    companion object {
        fun getInstance(aigramBotsManager: AiBotsManager): PurchaseHelper {
            if (INSTANCE == null) {
                INSTANCE = PurchaseHelper(aigramBotsManager)
            }

            return INSTANCE!!
        }

        private var INSTANCE: PurchaseHelper? = null
    }

    fun initWith(activity: Activity) {
        val billing = (activity.application as BillingProvider).provideBilling()
        uiCheckout = Checkout.forActivity(activity, billing)
    }

    fun start() {
        uiCheckout?.start()
        isPurchaseHelperActive = true
    }

    fun preloadPurchasesInfo() {
        aigramBotsManager.getAvailableSkus()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ list ->
                list?.let { loadSkuDetails(it) }
            }, { t ->
                t.printStackTrace()
            })
            .also { disposable.add(it) }
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        uiCheckout?.onActivityResult(requestCode, resultCode, data)
    }

    fun purchase(skuId: String): Single<Purchase> {
        if (isPurchaseFlowActive) {
            uiCheckout?.destroyPurchaseFlow()
        }
        if (skuId.isEmpty()) {
            return Single.error(Exception("sku can't be empty"))
        }
        val callback = Callback()
        uiCheckout?.apply {
            startPurchaseFlow(ProductTypes.IN_APP, skuId, null, callback)
        }

        //на успехе обновляем кэш
        return callback.single.doOnSuccess { purchase ->
            val previousProduct = cachedPurchases.find { previousPurchase ->
                previousPurchase.sku == purchase.sku
            } ?: throw IllegalStateException("Cannot find product")

            cachedPurchases.remove(previousProduct)
            cachedPurchases.add(
                ShopProduct(
                    sku = previousProduct.price,
                    price = previousProduct.price,
                    isPurchased = true,
                    receipt = ShopProduct.Receipt(
                        orderId = purchase.orderId,
                        packageName = purchase.packageName,
                        productId = purchase.sku,
                        purchaseTime = purchase.time,
                        purchaseState = purchase.state.id,
                        purchaseToken = purchase.token
                    )
                )
            )
            storeProductsInfo()
        }
            .doOnSubscribe { isPurchaseFlowActive = true }
            .doFinally { isPurchaseFlowActive = false }
    }

    private fun loadSkuDetails(skus: List<String>, productType: String = ProductTypes.IN_APP) {
        if (!isPurchaseHelperActive) {
            return
        }

        val request = Inventory.Request.create()
        request.loadAllPurchases()
        request.loadSkus(productType, skus.distinct())
        uiCheckout?.loadInventory(request) { products ->
            val newProducts = mutableSetOf<ShopProduct>()
            val inAppProduct = products[productType]
            inAppProduct.skus.forEach {
                newProducts.add(ShopProduct(
                    sku = it.id.code,
                    price = it.price,
                    isPurchased = inAppProduct.isPurchased(it),
                    receipt = getReceipt(it.id.code, inAppProduct.purchases)
                ))
            }

            cachedPurchases.clear()
            cachedPurchases.addAll(newProducts)
            storeProductsInfo()
        }

    }

    private fun getReceipt(sku: String, purchases: List<Purchase>): ShopProduct.Receipt? {
        val receipt = purchases.firstOrNull { it.sku == sku }
        return receipt?.let {
            ShopProduct.Receipt(
                orderId = it.orderId,
                packageName = it.packageName,
                productId = it.sku,
                purchaseTime = it.time,
                purchaseState = it.state.id,
                purchaseToken = it.token
            )
        }
    }

    fun stop() {
        uiCheckout?.stop()
        disposable.clear()
        isPurchaseHelperActive = false
    }

    private fun validatePurchases() {
        aigramBotsManager.validateReceipts(products.mapNotNull { it.receipt } )
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ validations ->
                Log.d("PurchaseHelper", "Purchases validated")
                storeProductsInfo()
            }, { t ->
                t.printStackTrace()
            })
            .also { disposable.add(it) }
    }

    private fun storeProductsInfo() {
        aigramBotsManager.storeActualPurchases(products)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                Log.d("PurchaseHelper", "Purchases info updated")
            }, { t ->
                t.printStackTrace()
            })
            .also { disposable.add(it) }
    }

    private class Callback : RequestListener<Purchase> {

        private val subject: PublishSubject<Purchase> = PublishSubject.create()

        val single: Single<Purchase> = subject.firstOrError()

        override fun onSuccess(result: Purchase) {
            subject.onNext(result)
        }

        override fun onError(response: Int, e: java.lang.Exception) {
            subject.onError(e)
        }
    }
}