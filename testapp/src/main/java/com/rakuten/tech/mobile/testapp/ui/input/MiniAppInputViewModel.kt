package com.rakuten.tech.mobile.testapp.ui.input

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rakuten.tech.mobile.miniapp.MiniApp
import com.rakuten.tech.mobile.miniapp.MiniAppSdkException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MiniAppInputViewModel constructor(private val miniApp: MiniApp) : ViewModel() {
    private val _miniAppVersionId = MutableLiveData<String>()
    private val _versionIdErrorData = MutableLiveData<String>()

    val miniAppVersionId: LiveData<String>
        get() = _miniAppVersionId
    val versionIdErrorData: LiveData<String>
        get() = _versionIdErrorData

    fun getMiniAppVersionId(miniAppId: String) = viewModelScope.launch(Dispatchers.IO) {
        try {
            val miniAppInfo = miniApp.fetchInfo(miniAppId)
            _miniAppVersionId.postValue(miniAppInfo.version.versionId)
        } catch (error: MiniAppSdkException) {
            _versionIdErrorData.postValue(error.message)
        }
    }
}
