package com.rakuten.tech.mobile.miniapp.js

import android.app.Activity
import android.content.Intent
import android.webkit.JavascriptInterface
import androidx.annotation.VisibleForTesting
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.rakuten.tech.mobile.miniapp.CustomPermissionsNotImplementedException
import com.rakuten.tech.mobile.miniapp.DevicePermissionsNotImplementedException
import com.rakuten.tech.mobile.miniapp.MiniAppSdkException
import com.rakuten.tech.mobile.miniapp.R
import com.rakuten.tech.mobile.miniapp.ads.MiniAppAdDisplayer
import com.rakuten.tech.mobile.miniapp.closealert.MiniAppCloseAlertInfo
import com.rakuten.tech.mobile.miniapp.display.WebViewListener
import com.rakuten.tech.mobile.miniapp.errors.MiniAppBridgeErrorModel
import com.rakuten.tech.mobile.miniapp.file.MiniAppFileDownloader
import com.rakuten.tech.mobile.miniapp.file.MiniAppFileDownloaderDefault
import com.rakuten.tech.mobile.miniapp.js.ErrorBridgeMessage.ERR_CLOSE_ALERT
import com.rakuten.tech.mobile.miniapp.js.ErrorBridgeMessage.ERR_GET_ENVIRONMENT_INFO
import com.rakuten.tech.mobile.miniapp.js.chat.ChatBridge
import com.rakuten.tech.mobile.miniapp.js.chat.ChatBridgeDispatcher
import com.rakuten.tech.mobile.miniapp.js.hostenvironment.HostEnvironmentInfo
import com.rakuten.tech.mobile.miniapp.js.hostenvironment.HostEnvironmentInfoError
import com.rakuten.tech.mobile.miniapp.js.hostenvironment.isValidLocale
import com.rakuten.tech.mobile.miniapp.js.userinfo.UserInfoBridge
import com.rakuten.tech.mobile.miniapp.js.userinfo.UserInfoBridgeDispatcher
import com.rakuten.tech.mobile.miniapp.permission.MiniAppDevicePermissionType
import com.rakuten.tech.mobile.miniapp.permission.CustomPermissionBridgeDispatcher
import com.rakuten.tech.mobile.miniapp.permission.MiniAppCustomPermissionCache
import com.rakuten.tech.mobile.miniapp.permission.MiniAppCustomPermissionType
import com.rakuten.tech.mobile.miniapp.permission.MiniAppDevicePermissionResult
import com.rakuten.tech.mobile.miniapp.permission.MiniAppCustomPermissionResult
import com.rakuten.tech.mobile.miniapp.permission.ui.MiniAppCustomPermissionWindow
import com.rakuten.tech.mobile.miniapp.storage.DownloadedManifestCache

@Suppress(
    "TooGenericExceptionCaught", "TooManyFunctions", "LongMethod", "LargeClass",
    "ComplexMethod", "LongParameterList", " MaximumLineLength", "FunctionMaxLength"
)
/** Bridge interface for communicating with mini app. **/
open class MiniAppMessageBridge {
    private lateinit var bridgeExecutor: MiniAppBridgeExecutor
    private var miniAppViewInitialized = false
    private lateinit var customPermissionCache: MiniAppCustomPermissionCache
    private lateinit var downloadedManifestCache: DownloadedManifestCache
    private lateinit var miniAppId: String
    private lateinit var activity: Activity
    private val userInfoBridge = UserInfoBridge()
    private val chatBridge = ChatBridge()
    private val adBridgeDispatcher = AdBridgeDispatcher()
    private val miniAppFileDownloadDispatcher = MiniAppFileDownloadDispatcher()

    @VisibleForTesting
    internal lateinit var ratDispatcher: MessageBridgeRatDispatcher
    private lateinit var screenBridgeDispatcher: ScreenBridgeDispatcher
    private var allowScreenOrientation = false
    private lateinit var miniAppSecureStorageDispatcher: MiniAppSecureStorageDispatcher

    private var miniAppCloseAlertInfo: MiniAppCloseAlertInfo? = null
    /** provide MiniAppCloseAlertInfo to HostApp to show close alert popup. */
    fun miniAppShouldClose() = miniAppCloseAlertInfo

    internal fun init(
        activity: Activity,
        webViewListener: WebViewListener,
        customPermissionCache: MiniAppCustomPermissionCache,
        downloadedManifestCache: DownloadedManifestCache,
        miniAppId: String,
        ratDispatcher: MessageBridgeRatDispatcher,
        secureStorageDispatcher: MiniAppSecureStorageDispatcher
    ) {
        this.activity = activity
        this.miniAppId = miniAppId
        this.bridgeExecutor = createBridgeExecutor(webViewListener)
        this.customPermissionCache = customPermissionCache
        this.downloadedManifestCache = downloadedManifestCache
        this.screenBridgeDispatcher =
            ScreenBridgeDispatcher(activity, bridgeExecutor, allowScreenOrientation)
        this.ratDispatcher = ratDispatcher
        this.miniAppSecureStorageDispatcher = secureStorageDispatcher
        adBridgeDispatcher.setBridgeExecutor(bridgeExecutor)
        miniAppFileDownloadDispatcher.setBridgeExecutor(activity, bridgeExecutor)
        miniAppFileDownloadDispatcher.setMiniAppComponents(miniAppId, customPermissionCache)
        miniAppSecureStorageDispatcher.setBridgeExecutor(activity, bridgeExecutor)
        miniAppSecureStorageDispatcher.setMiniAppComponents(miniAppId)
        userInfoBridge.setMiniAppComponents(
            bridgeExecutor,
            customPermissionCache,
            downloadedManifestCache,
            miniAppId
        )
        chatBridge.setMiniAppComponents(bridgeExecutor, customPermissionCache, miniAppId)

        miniAppViewInitialized = true
    }

    internal fun onJsInjectionDone() {
        miniAppSecureStorageDispatcher.onLoad()
    }

    @VisibleForTesting
    internal fun createBridgeExecutor(webViewListener: WebViewListener) =
        MiniAppBridgeExecutor(webViewListener)

    @Deprecated(
        "This function has been deprecated.",
        ReplaceWith("getMessagingUniqueId(onSuccess: (uniqueId: String) -> Unit," +
                    "onError: (message: String) -> Unit)"
        )
    )
    /** Get provided id of mini app for any purpose. **/
    open fun getUniqueId(
        onSuccess: (uniqueId: String) -> Unit,
        onError: (message: String) -> Unit
    ) {
        throw MiniAppSdkException(ErrorBridgeMessage.NO_IMPL)
    }

    /** Interface that should be implemented to return alphanumeric string that uniquely identifies a device. **/
    open fun getMessagingUniqueId(
        onSuccess: (uniqueId: String) -> Unit,
        onError: (message: String) -> Unit
    ) {
        throw MiniAppSdkException(ErrorBridgeMessage.NO_IMPL)
    }

    /** Interface that should be implemented to return alphanumeric string that uniquely identifies a device. **/
    open fun getMauid(
        onSuccess: (mauId: String) -> Unit,
        onError: (message: String) -> Unit
    ) {
        throw MiniAppSdkException(ErrorBridgeMessage.NO_IMPL)
    }

    /** Post device permission request from external. **/
    open fun requestDevicePermission(
        miniAppPermissionType: MiniAppDevicePermissionType,
        callback: (isGranted: Boolean) -> Unit
    ) {
        throw DevicePermissionsNotImplementedException()
    }

    /**
     * Post custom permissions request.
     * @param permissionsWithDescription list of name and descriptions of custom permissions sent from external.
     * @param callback to invoke a list of name and grant results of custom permissions sent from hostapp.
     */
    open fun requestCustomPermissions(
        permissionsWithDescription: List<Pair<MiniAppCustomPermissionType, String>>,
        callback: (List<Pair<MiniAppCustomPermissionType, MiniAppCustomPermissionResult>>) -> Unit
    ) {
        throw CustomPermissionsNotImplementedException()
    }

    /**
     * Share content info [ShareInfo]. This info is provided by mini app.
     * @param content The content property of [ShareInfo] object.
     * @param callback The executed action status should be notified back to mini app.
     **/
    open fun shareContent(
        content: String,
        callback: (isSuccess: Boolean, message: String?) -> Unit
    ) {
        when {
            content.trim().isEmpty() -> callback.invoke(false, "content is empty")
            else -> {
                val sendIntent: Intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, content)
                    type = "text/plain"
                }
                activity.startActivity(Intent.createChooser(sendIntent, null))

                callback.invoke(true, SUCCESS)
            }
        }
    }

    /**
     * Get environment info from host app.
     * You can also throw an [Exception] from this method to pass an error message to the mini app.
     */
    open fun getHostEnvironmentInfo(
        onSuccess: (info: HostEnvironmentInfo) -> Unit,
        onError: (infoError: HostEnvironmentInfoError) -> Unit
    ) {
        var locale = activity.getString(R.string.miniapp_sdk_android_locale)
        if (!locale.isValidLocale())
            locale = ""

        val hostEnvironmentInfo = HostEnvironmentInfo(activity = activity, hostLocale = locale)

        onSuccess.invoke(hostEnvironmentInfo)
    }

    @SuppressWarnings("UndocumentedPublicFunction")
    @JavascriptInterface
    fun postMessage(jsonStr: String) {
        val callbackObj = Gson().fromJson(jsonStr, CallbackObj::class.java)
        when (callbackObj.action) {
            ActionType.GET_UNIQUE_ID.action -> onGetUniqueId(callbackObj)
            ActionType.GET_MESSAGING_UNIQUE_ID.action -> onGetMessagingUniqueId(callbackObj)
            ActionType.GET_MAUID.action -> onGetMauid(callbackObj)
            ActionType.REQUEST_PERMISSION.action -> onRequestDevicePermission(callbackObj)
            ActionType.REQUEST_CUSTOM_PERMISSIONS.action -> onRequestCustomPermissions(jsonStr)
            ActionType.SHARE_INFO.action -> onShareContent(callbackObj.id, jsonStr)
            ActionType.LOAD_AD.action -> adBridgeDispatcher.onLoadAd(callbackObj.id, jsonStr)
            ActionType.SHOW_AD.action -> adBridgeDispatcher.onShowAd(callbackObj.id, jsonStr)
            ActionType.GET_USER_NAME.action -> userInfoBridge.onGetUserName(callbackObj.id)
            ActionType.GET_PROFILE_PHOTO.action -> userInfoBridge.onGetProfilePhoto(callbackObj.id)
            ActionType.GET_ACCESS_TOKEN.action -> userInfoBridge.onGetAccessToken(
                callbackObj.id,
                jsonStr
            )
            ActionType.GET_POINTS.action -> userInfoBridge.onGetPoints(callbackObj.id)
            ActionType.SET_SCREEN_ORIENTATION.action -> screenBridgeDispatcher.onScreenRequest(
                callbackObj
            )
            ActionType.GET_CONTACTS.action -> userInfoBridge.onGetContacts(callbackObj.id)
            ActionType.SEND_MESSAGE_TO_CONTACT.action -> chatBridge.onSendMessageToContact(
                callbackObj.id, jsonStr
            )
            ActionType.SEND_MESSAGE_TO_CONTACT_ID.action -> chatBridge.onSendMessageToContactId(
                callbackObj.id, jsonStr
            )
            ActionType.SEND_MESSAGE_TO_MULTIPLE_CONTACTS.action -> chatBridge.onSendMessageToMultipleContacts(
                callbackObj.id, jsonStr
            )
            ActionType.GET_HOST_ENVIRONMENT_INFO.action -> onGetHostEnvironmentInfo(callbackObj.id)
            ActionType.FILE_DOWNLOAD.action -> miniAppFileDownloadDispatcher.onFileDownload(
                callbackObj.id,
                jsonStr
            )
            ActionType.SECURE_STORAGE_SET_ITEMS.action -> miniAppSecureStorageDispatcher.onSetItems(
                callbackObj.id,
                jsonStr
            )
            ActionType.SECURE_STORAGE_GET_ITEM.action -> miniAppSecureStorageDispatcher.onGetItem(
                callbackObj.id,
                jsonStr
            )
            ActionType.SECURE_STORAGE_REMOVE_ITEMS.action -> miniAppSecureStorageDispatcher.onRemoveItems(
                callbackObj.id,
                jsonStr
            )
            ActionType.SECURE_STORAGE_CLEAR.action -> miniAppSecureStorageDispatcher.onClearAll(
                callbackObj.id
            )
            ActionType.SECURE_STORAGE_SIZE.action -> miniAppSecureStorageDispatcher.onSize(
                callbackObj.id
            )
            ActionType.SET_CLOSE_ALERT.action -> onMiniAppShouldClose(callbackObj.id, jsonStr)
        }
        if (this::ratDispatcher.isInitialized)
            ratDispatcher.sendAnalyticsSdkFeature(callbackObj.action)
    }

    /** Set implemented ads displayer. Can use the default provided class from sdk [AdMobDisplayer]. **/
    fun setAdMobDisplayer(adDisplayer: MiniAppAdDisplayer) =
        adBridgeDispatcher.setAdMobDisplayer(adDisplayer)

    /** Set implemented file downloader. Can use the default provided class from sdk [MiniAppFileDownloaderDefault]. **/
    fun setMiniAppFileDownloader(miniAppFileDownloader: MiniAppFileDownloader) =
        miniAppFileDownloadDispatcher.setFileDownloader(miniAppFileDownloader)

    /**
     * Set implemented userInfoBridgeDispatcher.
     * Can use the default provided class from sdk [UserInfoBridgeDispatcher].
     **/
    fun setUserInfoBridgeDispatcher(bridgeDispatcher: UserInfoBridgeDispatcher) =
        userInfoBridge.setUserInfoBridgeDispatcher(bridgeDispatcher)

    /**
     * Set implemented chatBridgeDispatcher.
     * Can use the default provided class from sdk [ChatBridgeDispatcher].
     **/
    fun setChatBridgeDispatcher(bridgeDispatcher: ChatBridgeDispatcher) =
        chatBridge.setChatBridgeDispatcher(bridgeDispatcher)

    /**
     * Dispatch Native events to miniapp.
     **/
    fun dispatchNativeEvent(eventType: NativeEventType, value: String = "") {
        if (this::bridgeExecutor.isInitialized)
            bridgeExecutor.dispatchEvent(eventType = eventType.value, value = value)
    }

    private fun onGetUniqueId(callbackObj: CallbackObj) = try {
        val successCallback = { uniqueId: String ->
            bridgeExecutor.postValue(callbackObj.id, uniqueId)
        }
        val errorCallback = { message: String ->
            bridgeExecutor.postError(
                callbackObj.id, "${ErrorBridgeMessage.ERR_UNIQUE_ID} $message"
            )
        }

        getUniqueId(successCallback, errorCallback)
    } catch (e: Exception) {
        bridgeExecutor.postError(callbackObj.id, "${ErrorBridgeMessage.ERR_UNIQUE_ID} ${e.message}")
    }

    private fun onGetMessagingUniqueId(callbackObj: CallbackObj) = try {
        val successCallback = { uniqueId: String ->
            bridgeExecutor.postValue(callbackObj.id, uniqueId)
        }
        val errorCallback = { message: String ->
            bridgeExecutor.postError(
                callbackObj.id, "${ErrorBridgeMessage.ERR_MESSAGING_UNIQUE_ID} $message"
            )
        }

        getMessagingUniqueId(successCallback, errorCallback)
    } catch (e: Exception) {
        bridgeExecutor.postError(callbackObj.id, "${ErrorBridgeMessage.ERR_MESSAGING_UNIQUE_ID} ${e.message}")
    }

    private fun onGetMauid(callbackObj: CallbackObj) = try {
        val successCallback = { mauId: String ->
            bridgeExecutor.postValue(callbackObj.id, mauId)
        }
        val errorCallback = { message: String ->
            bridgeExecutor.postError(
                callbackObj.id, "${ErrorBridgeMessage.ERR_MAUID} $message"
            )
        }

        getMauid(successCallback, errorCallback)
    } catch (e: Exception) {
        bridgeExecutor.postError(callbackObj.id, "${ErrorBridgeMessage.ERR_MAUID} ${e.message}")
    }

    private fun onRequestDevicePermission(callbackObj: CallbackObj) {
        try {
            val permissionParam = Gson().fromJson<DevicePermission>(
                callbackObj.param.toString(),
                object : TypeToken<DevicePermission>() {}.type
            )

            requestDevicePermission(
                MiniAppDevicePermissionType.getValue(permissionParam.permission)
            ) { isGranted ->
                onRequestDevicePermissionsResult(
                    callbackId = callbackObj.id,
                    isGranted = isGranted
                )
            }
        } catch (e: Exception) {
            bridgeExecutor.postError(
                callbackObj.id,
                "${ErrorBridgeMessage.ERR_REQ_DEVICE_PERMISSION} ${e.message}"
            )
        }
    }

    @Suppress("SwallowedException")
    private fun onRequestCustomPermissions(jsonStr: String) {
        val customPermissionBridgeDispatcher = CustomPermissionBridgeDispatcher(
            bridgeExecutor = bridgeExecutor,
            customPermissionCache = customPermissionCache,
            downloadedManifestCache = downloadedManifestCache,
            miniAppId = miniAppId,
            jsonStr = jsonStr
        )
        val customPermissionWindow = MiniAppCustomPermissionWindow(
            activity,
            customPermissionBridgeDispatcher
        )

        // check if there is any denied permission
        val deniedPermissions = customPermissionBridgeDispatcher.filterDeniedPermissions()
        if (deniedPermissions.isNotEmpty()) {
            try {
                requestCustomPermissions(
                    deniedPermissions
                ) { permissionsWithResult ->
                    customPermissionBridgeDispatcher.sendHostAppCustomPermissions(
                        permissionsWithResult
                    )
                }
            } catch (e: CustomPermissionsNotImplementedException) {
                customPermissionWindow.displayPermissions(miniAppId, deniedPermissions)
            } catch (e: Exception) {
                customPermissionBridgeDispatcher.postCustomPermissionError(e.message.toString())
            }
        } else {
            customPermissionBridgeDispatcher.sendCachedCustomPermissions()
        }
    }

    private fun onShareContent(callbackId: String, jsonStr: String) = try {
        val callbackObj = Gson().fromJson(jsonStr, ShareInfoCallbackObj::class.java)

        shareContent(callbackObj.param.shareInfo.content) { isSuccess, message ->
            if (isSuccess)
                bridgeExecutor.postValue(callbackId, message ?: SUCCESS)
            else
                bridgeExecutor.postError(
                    callbackId,
                    message
                        ?: "${ErrorBridgeMessage.ERR_SHARE_CONTENT} Unknown error message from hostapp."
                )
        }
    } catch (e: Exception) {
        bridgeExecutor.postError(callbackId, "${ErrorBridgeMessage.ERR_SHARE_CONTENT} ${e.message}")
    }

    @SuppressWarnings("TooGenericExceptionCaught")
    @VisibleForTesting
    internal fun onGetHostEnvironmentInfo(callbackId: String) = try {
        val successCallback = { info: HostEnvironmentInfo ->
            bridgeExecutor.postValue(callbackId, Gson().toJson(info))
        }
        val errorCallback = { callback: HostEnvironmentInfoError ->
            val errorBridgeModel = MiniAppBridgeErrorModel(callback.type, callback.message)
            bridgeExecutor.postError(callbackId, Gson().toJson(errorBridgeModel))
        }

        getHostEnvironmentInfo(successCallback, errorCallback)
    } catch (e: Exception) {
        bridgeExecutor.postError(
            callbackId,
            Gson().toJson(MiniAppBridgeErrorModel("$ERR_GET_ENVIRONMENT_INFO ${e.message}"))
        )
    }

    @VisibleForTesting
    @Suppress("FunctionMaxLength")
    /** Inform the permission request result to MiniApp. **/
    internal fun onRequestDevicePermissionsResult(callbackId: String, isGranted: Boolean) {
        if (isGranted)
            bridgeExecutor.postValue(
                callbackId,
                MiniAppDevicePermissionResult.getValue(isGranted).type
            )
        else
            bridgeExecutor.postError(
                callbackId,
                MiniAppDevicePermissionResult.getValue(isGranted).type
            )
    }

    internal fun onWebViewDetach() {
        screenBridgeDispatcher.releaseLock()
    }

    /** Allow miniapp to change screen orientation. The default setting is false. */
    fun allowScreenOrientation(isAllowed: Boolean) {
        allowScreenOrientation = isAllowed
        if (this::screenBridgeDispatcher.isInitialized)
            screenBridgeDispatcher.allowScreenOrientation = allowScreenOrientation
    }

    @SuppressWarnings("SwallowedException")
    @VisibleForTesting
    internal fun onMiniAppShouldClose(callbackId: String, jsonStr: String) = try {
        val callbackObj = Gson().fromJson(jsonStr, CloseAlertInfoCallbackObj::class.java)
        val alertInfo = callbackObj.param.closeAlertInfo
        this.miniAppCloseAlertInfo = alertInfo
    } catch (e: Exception) {
        bridgeExecutor.postError(callbackId, ERR_CLOSE_ALERT)
    }
}

internal object ErrorBridgeMessage {
    const val NO_IMPL = "no implementation by the Host App."
    const val ERR_NO_SUPPORT_HOSTAPP = "No support from hostapp"
    const val ERR_UNIQUE_ID = "Cannot get unique id:"
    const val ERR_MESSAGING_UNIQUE_ID = "Cannot get messaging unique id:"
    const val ERR_MAUID = "Cannot get mauid:"
    const val ERR_REQ_DEVICE_PERMISSION = "Cannot request device permission:"
    const val ERR_REQ_CUSTOM_PERMISSION = "Cannot request custom permissions:"
    const val NO_IMPLEMENT_DEVICE_PERMISSION =
        "The `MiniAppMessageBridge.requestDevicePermission` $NO_IMPL"
    const val NO_IMPLEMENT_CUSTOM_PERMISSION =
        "The `MiniAppMessageBridge.requestCustomPermissions` $NO_IMPL"
    const val ERR_SHARE_CONTENT = "Cannot share content:"
    const val ERR_LOAD_AD = "Cannot load ad:"
    const val ERR_SHOW_AD = "Cannot show ad:"
    const val ERR_SCREEN_ACTION = "Cannot request screen action:"
    const val ERR_GET_ENVIRONMENT_INFO = "Cannot get host environment info:"
    const val ERR_CLOSE_ALERT = "There is an error occurred when setting close alert info."
}
