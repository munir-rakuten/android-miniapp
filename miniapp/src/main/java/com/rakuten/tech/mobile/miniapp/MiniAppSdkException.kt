package com.rakuten.tech.mobile.miniapp

import android.os.NetworkOnMainThreadException
import com.rakuten.tech.mobile.miniapp.js.ErrorBridgeMessage

/**
 * A custom exception class which treats the purpose of providing
 * error information to the consumer app in an unified way.
 */
open class MiniAppSdkException(message: String, cause: Throwable?) : Exception(message, cause) {

    constructor(e: Exception) : this(exceptionMessage(e), e)

    constructor(message: String) : this(message, null)
}

/**
 * Exception which is thrown when the server returns no published
 * versions for the provided mini app ID.
 */
class MiniAppHasNoPublishedVersionException(appId: String) :
    MiniAppSdkException("Server returned no published version info for the provided Mini App Id: $appId")

/**
 * Exception which is thrown when the public key used for ssl pinning is mismatched.
 */
class SSLCertificatePinningException(serverMessage: String) :
    MiniAppSdkException("$serverMessage: SSL public key mismatched.")

/**
 * Exception which is thrown when the provided project ID
 * does not have any mini app exist on the server.
 */
class MiniAppNotFoundException(serverMessage: String) :
    MiniAppSdkException("$serverMessage: Server returned no mini app for the provided project ID.")

/**
 * Exception which is thrown when the provided project ID
 * does not have any mini app exist on the server.
 */
class MiniAppHostException(serverMessage: String) :
    MiniAppSdkException("$serverMessage: Only the correct host has access to this token.")

/**
 * Exception which is thrown when cannot verify device keystore.
 */
class MiniAppVerificationException(message: String?) :
    MiniAppSdkException("MiniApp SDK cannot proceed due to security validation: $message")

/**
 * Exception which is thrown when HostApp doesn't implement requestDevicePermission interface.
 */
internal class DevicePermissionsNotImplementedException :
    MiniAppSdkException(ErrorBridgeMessage.NO_IMPLEMENT_DEVICE_PERMISSION)

/**
 * Exception which is thrown when HostApp doesn't implement requestCustomPermissions interface.
 */
internal class CustomPermissionsNotImplementedException :
    MiniAppSdkException(ErrorBridgeMessage.NO_IMPLEMENT_CUSTOM_PERMISSION)

/**
 * Exception which is thrown when the required permissions of the manifest are not granted.
 */
@Suppress("MaxLineLength")
class RequiredPermissionsNotGrantedException(appId: String, versionId: String) :
    MiniAppSdkException("Mini App has not been granted all of the required permissions " +
            "for the provided Mini App Id: $appId and the version id: $versionId")

/**
 * Exception indicating that there was an issue with network connectivity.
 *
 * This usually means the device doesn't have internet access currently,
 * so you should display the cached mini app version.
 */
class MiniAppNetException(message: String, cause: Throwable?) : MiniAppSdkException(message, cause) {

    constructor(e: Exception) : this("Found some problem, ${e.message}", e)

    constructor(message: String) : this(message, null)
}

@Suppress("FunctionMaxLength")
internal fun sdkExceptionForInternalServerError() = MiniAppSdkException("Internal server error")

@Suppress("FunctionMaxLength")
internal fun sdkExceptionForInvalidArguments(message: String = "") =
    MiniAppSdkException(
        "Invalid arguments${when {
            message.isNotBlank() -> ": $message"
            else -> ""
        }}"
    )

@Suppress("FunctionMaxLength")
internal fun sdkExceptionForNoActivityContext() =
    MiniAppSdkException("Only accept context of type Activity or ActivityCompat")

private fun exceptionMessage(exception: Exception) = when (exception) {
    is NetworkOnMainThreadException -> "Network requests must not be performed on the main thread. "
        .plus("Use Dispatchers.IO or Dispatchers.Default for MiniApp suspending functions.")
    else -> "Found some problem, ${exception.message}"
}
