package com.rakuten.tech.mobile.miniapp

import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.junit.Test

class MiniAppSpec {

    @Test
    fun `should update configuration when get instance of MiniApp`() {
        val miniApp: MiniApp = mock()
        val miniAppSdkConfig: MiniAppSdkConfig = mock()
        val miniAppCompanion = MiniApp.Companion

        miniAppCompanion.instance = miniApp
        miniAppCompanion.instance(miniAppSdkConfig)

        verify(miniApp).updateConfiguration(miniAppSdkConfig, setConfigAsDefault = true)
    }
}
