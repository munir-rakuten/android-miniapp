package com.rakuten.tech.mobile.testapp.ui.permission

import com.rakuten.tech.mobile.miniapp.permission.MiniAppCustomPermissionResult
import com.rakuten.tech.mobile.miniapp.permission.MiniAppCustomPermissionType

fun toReadableName(type: MiniAppCustomPermissionType?): String {
    return when (type) {
        MiniAppCustomPermissionType.USER_NAME -> "User Name"
        MiniAppCustomPermissionType.PROFILE_PHOTO -> "Profile Photo"
        MiniAppCustomPermissionType.CONTACT_LIST -> "Contact List"
        MiniAppCustomPermissionType.ACCESS_TOKEN -> "Access Token"
        MiniAppCustomPermissionType.SEND_MESSAGE -> "Send Message"
        MiniAppCustomPermissionType.LOCATION -> "Device Location"
        MiniAppCustomPermissionType.POINTS -> "Rakuten Points"
        MiniAppCustomPermissionType.FILE_DOWNLOAD -> "File Download"
        else -> "Unknown"
    }
}

fun parsePermissionText(pairValues: List<Pair<MiniAppCustomPermissionType, MiniAppCustomPermissionResult>>): String {
    val permissionString = StringBuilder()
    pairValues.forEach {
        if (it.second == MiniAppCustomPermissionResult.ALLOWED) {
            permissionString.append(toReadableName(it.first)).append(", ")
        }
    }

    if (permissionString.isEmpty())
        return permissionString.append(MiniAppCustomPermissionResult.DENIED.name).toString()

    return permissionString.substring(0, permissionString.length - 2)
}
