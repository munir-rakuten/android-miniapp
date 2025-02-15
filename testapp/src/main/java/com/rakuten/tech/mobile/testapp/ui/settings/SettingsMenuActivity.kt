package com.rakuten.tech.mobile.testapp.ui.settings

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.widget.Toolbar
import androidx.databinding.DataBindingUtil
import com.rakuten.tech.mobile.miniapp.MiniApp
import com.rakuten.tech.mobile.miniapp.MiniAppSdkException
import com.rakuten.tech.mobile.miniapp.testapp.BuildConfig
import com.rakuten.tech.mobile.miniapp.testapp.R
import com.rakuten.tech.mobile.miniapp.testapp.databinding.SettingsMenuActivityBinding
import com.rakuten.tech.mobile.testapp.AppScreen.MINI_APP_INPUT_ACTIVITY
import com.rakuten.tech.mobile.testapp.AppScreen.MINI_APP_LIST_ACTIVITY
import com.rakuten.tech.mobile.testapp.BuildVariant
import com.rakuten.tech.mobile.testapp.helper.isAvailable
import com.rakuten.tech.mobile.testapp.helper.isInputEmpty
import com.rakuten.tech.mobile.testapp.helper.isInvalidUuid
import com.rakuten.tech.mobile.testapp.helper.showAlertDialog
import com.rakuten.tech.mobile.testapp.launchActivity
import com.rakuten.tech.mobile.testapp.ui.base.BaseActivity
import com.rakuten.tech.mobile.testapp.ui.deeplink.DynamicDeepLinkActivity
import com.rakuten.tech.mobile.testapp.ui.input.MiniAppInputActivity
import com.rakuten.tech.mobile.testapp.ui.miniapplist.MiniAppListActivity
import com.rakuten.tech.mobile.testapp.ui.permission.MiniAppDownloadedListActivity
import com.rakuten.tech.mobile.testapp.ui.settings.MenuBaseActivity.Companion.MENU_SCREEN_NAME
import com.rakuten.tech.mobile.testapp.ui.userdata.*
import kotlinx.android.synthetic.main.settings_menu_activity.*
import kotlinx.coroutines.launch
import java.net.URL
import kotlin.properties.Delegates

class SettingsMenuActivity : BaseActivity() {
    override val pageName: String = this::class.simpleName ?: ""
    override val siteSection: String = this::class.simpleName ?: ""
    private lateinit var settings: AppSettings
    private lateinit var settingsProgressDialog: SettingsProgressDialog
    private lateinit var binding: SettingsMenuActivityBinding

    private var saveViewEnabled by Delegates.observable(true) { _, old, new ->
        if (new != old) {
            invalidateOptionsMenu()
        }
    }

    private val settingsTextWatcher = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {}

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            validateInputIDs()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings = AppSettings.instance
        binding = DataBindingUtil.setContentView(this, R.layout.settings_menu_activity)

        initializeActionBar()
        settingsProgressDialog = SettingsProgressDialog(this)
        renderAppSettingsScreen()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.settings_menu, menu)
        menu.findItem(R.id.settings_menu_save).isEnabled = saveViewEnabled
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
         super.onOptionsItemSelected(item)
         return when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
            R.id.settings_menu_save -> {
                onSaveAction()
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun onSaveAction() {
        if (this@SettingsMenuActivity.isAvailable) {
            settingsProgressDialog.show()
        }

        updateSettings(
            binding.editProjectId.text.toString(),
            binding.editSubscriptionKey.text.toString(),
            binding.editParametersUrl.text.toString(),
            binding.switchPreviewMode.isChecked,
            binding.switchSignatureVerification.isChecked,
            binding.switchProdVersion.isChecked
        )
    }

    private fun initializeActionBar() {
        val toolBar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolBar)
        showBackIcon()
    }

    private fun renderAppSettingsScreen() {
        binding.textInfo.text = createBuildInfo()
        binding.editProjectId.setText(settings.projectId)
        binding.editSubscriptionKey.setText(settings.subscriptionKey)
        binding.editParametersUrl.setText(settings.urlParameters)
        binding.switchPreviewMode.isChecked = settings.isPreviewMode
        binding.switchSignatureVerification.isChecked = settings.requireSignatureVerification
        binding.switchProdVersion.isChecked = settings.isProdVersionEnabled
        if(BuildConfig.BUILD_TYPE == BuildVariant.RELEASE.value && !AppSettings.instance.isSettingSaved){
            switchProdVersion.isChecked = true
        }
        binding.editProjectId.addTextChangedListener(settingsTextWatcher)
        binding.editSubscriptionKey.addTextChangedListener(settingsTextWatcher)

        binding.buttonProfile.setOnClickListener {
            ProfileSettingsActivity.start(this@SettingsMenuActivity)
        }

        binding.buttonContacts.setOnClickListener {
            ContactListActivity.start(this@SettingsMenuActivity)
        }

        binding.buttonCustomPermissions.setOnClickListener {
            MiniAppDownloadedListActivity.start(this@SettingsMenuActivity)
        }

        binding.buttonAccessToken.setOnClickListener {
            AccessTokenActivity.start(this@SettingsMenuActivity)
        }

        binding.buttonPoints.setOnClickListener {
            PointsActivity.start(this@SettingsMenuActivity)
        }

        binding.buttonDeeplink.setOnClickListener {
            DynamicDeepLinkActivity.start(this@SettingsMenuActivity)
        }

        binding.buttonQA.setOnClickListener {
            QASettingsActivity.start(this@SettingsMenuActivity)
        }

        binding.switchProdVersion.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                settings.baseUrl = getString(R.string.prodBaseUrl)
                settings.projectId = getString(R.string.prodProjectId)
                settings.subscriptionKey = getString(R.string.prodSubscriptionKey)
            } else {
                settings.baseUrl = getString(R.string.stagingBaseUrl)
                settings.projectId = getString(R.string.stagingProjectId)
                settings.subscriptionKey = getString(R.string.stagingSubscriptionKey)
            }
            binding.editProjectId.setText(settings.projectId)
            binding.editSubscriptionKey.setText(settings.subscriptionKey)
        }

        validateInputIDs()
    }

    private fun createBuildInfo(): String {
        val sdkVersion = getString(R.string.miniapp_sdk_version)
        val buildVersion = getString(R.string.build_version)
        return "Build $sdkVersion - $buildVersion"
    }

    private fun validateInputIDs() {
        val isAppIdInvalid = binding.editProjectId.text.toString().isInvalidUuid()

        saveViewEnabled = !(isInputEmpty(binding.editProjectId)
                || isInputEmpty(binding.editSubscriptionKey)
                || isAppIdInvalid)

        if (isInputEmpty(binding.editProjectId) || isAppIdInvalid)
            binding.inputProjectId.error = getString(R.string.error_invalid_input)
        else binding.inputProjectId.error = null

        if (isInputEmpty(binding.editSubscriptionKey))
            binding.inputSubscriptionKey.error = getString(R.string.error_invalid_input)
        else binding.inputSubscriptionKey.error = null
    }

    private fun updateSettings(
        projectId: String,
        subscriptionKey: String,
        urlParameters: String,
        isPreviewMode: Boolean,
        requireSignatureVerification: Boolean,
        isProdVersionEnabled: Boolean
    ) {
        val appIdHolder = settings.projectId
        val subscriptionKeyHolder = settings.subscriptionKey
        val urlParametersHolder = settings.urlParameters
        val isPreviewModeHolder = settings.isPreviewMode
        val requireSignatureVerificationHolder = settings.requireSignatureVerification
        settings.projectId = projectId
        settings.subscriptionKey = subscriptionKey
        settings.urlParameters = urlParameters
        settings.isPreviewMode = isPreviewMode
        settings.requireSignatureVerification = requireSignatureVerification
        settings.isProdVersionEnabled = isProdVersionEnabled

        launch {
            try {
                MiniApp.instance(AppSettings.instance.miniAppSettings).listMiniApp()
                URL("https://www.test-param.com?$urlParameters").toURI()

                settings.isSettingSaved = true
                runOnUiThread {
                    if (this@SettingsMenuActivity.isAvailable) {
                        settingsProgressDialog.cancel()
                    }
                    navigateToPreviousScreen()
                }
            } catch (error: MiniAppSdkException) {
                onUpdateError(
                    appIdHolder,
                    subscriptionKeyHolder,
                    urlParametersHolder,
                    isPreviewModeHolder,
                    requireSignatureVerificationHolder,
                    "MiniApp SDK",
                    error.message.toString()
                )
            } catch (error: Exception) {
                onUpdateError(
                    appIdHolder,
                    subscriptionKeyHolder,
                    urlParametersHolder,
                    isPreviewModeHolder,
                    requireSignatureVerificationHolder,
                    "URL parameter",
                    error.message.toString()
                )
            }
        }
    }

    private fun onUpdateError(
        appIdHolder: String,
        subscriptionKeyHolder: String,
        urlParametersHolder: String,
        isPreviewModeHolder: Boolean,
        requireSignatureVerificationHolder: Boolean,
        errTitle: String,
        errMsg: String
    ) {
        settings.projectId = appIdHolder
        settings.subscriptionKey = subscriptionKeyHolder
        settings.urlParameters = urlParametersHolder
        settings.isPreviewMode = isPreviewModeHolder
        settings.requireSignatureVerification = requireSignatureVerificationHolder
        runOnUiThread {
            if (this@SettingsMenuActivity.isAvailable) {
                settingsProgressDialog.cancel()
            }
            showAlertDialog(this@SettingsMenuActivity, errTitle, errMsg)
        }
    }

    private fun navigateToPreviousScreen() {
        when (intent.extras?.getString(MENU_SCREEN_NAME)) {
            MINI_APP_LIST_ACTIVITY -> {
                val intent = Intent(this, MiniAppListActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                setResult(Activity.RESULT_CANCELED, intent)
                startActivity(intent)
                finish()
            }
            MINI_APP_INPUT_ACTIVITY -> {
                raceExecutor.run { launchActivity<MiniAppInputActivity>() }
            }
            else -> finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (settingsProgressDialog != null && settingsProgressDialog.isShowing) {
            settingsProgressDialog.dismiss()
        }
    }
}
