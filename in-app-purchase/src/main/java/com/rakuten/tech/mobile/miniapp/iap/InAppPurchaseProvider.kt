package com.rakuten.tech.mobile.miniapp.iap

/**
 * Functionalities related to In-App purchase.
 */
interface InAppPurchaseProvider {

    /**
     * Triggered when user wants to get all the products from google store.
     * [androidStoreIds] list of product ids needs to fetch details.
     * Should invoke [onSuccess] with list of [ProductInfo] when user get products successfully.
     * Should invoke [onError] when there was an error.
     */
    fun getAllProducts(
        androidStoreIds: List<String>,
        onSuccess: (productInfos: List<ProductInfo>) -> Unit,
        onError: (errorType: MiniAppInAppPurchaseErrorType) -> Unit
    )

    /**
     * Triggered when user wants to purchase an item.
     * [androidStoreId] item to be purchased.
     * Should invoke [onSuccess] with [PurchaseData] when user can purchase an item successfully.
     * Should invoke [onError] when there was an error.
     */
    fun purchaseProductWith(
        androidStoreId: String,
        onSuccess: (purchaseData: PurchaseData) -> Unit,
        onError: (errorType: MiniAppInAppPurchaseErrorType) -> Unit
    )

    /**
     * Triggered when user wants to consume an purchased item.
     * [purhcaseToken] token of the item to be consumed.
     * Should invoke [onSuccess] with title and description when user can consume an purchase successfully.
     * Should invoke [onError] when there was an error.
     */
    fun consumePurchaseWIth(
        purhcaseToken: String,
        onSuccess: (title: String, description: String) -> Unit,
        onError: (errorType: MiniAppInAppPurchaseErrorType) -> Unit
    )

    /**
     * Triggered when user wants to end the connection with billing client.
     */
    fun onEndConnection()
}
