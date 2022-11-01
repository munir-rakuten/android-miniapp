package com.rakuten.tech.mobile.testapp.ui.miniapptabs.fragments

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.view.*
import androidx.core.app.ActivityCompat.invalidateOptionsMenu
import androidx.databinding.DataBindingUtil
import com.rakuten.tech.mobile.miniapp.js.userinfo.Contact
import com.rakuten.tech.mobile.miniapp.testapp.R
import com.rakuten.tech.mobile.miniapp.testapp.databinding.SettingsFragmentBinding
import com.rakuten.tech.mobile.testapp.helper.hideSoftKeyboard
import com.rakuten.tech.mobile.testapp.helper.isAvailable
import com.rakuten.tech.mobile.testapp.helper.isInputEmpty
import com.rakuten.tech.mobile.testapp.helper.isInvalidUuid
import com.rakuten.tech.mobile.testapp.ui.base.BaseFragment
import com.rakuten.tech.mobile.testapp.ui.deeplink.DynamicDeepLinkActivity
import com.rakuten.tech.mobile.testapp.ui.settings.AppSettings
import com.rakuten.tech.mobile.testapp.ui.settings.MiniAppConfigData
import com.rakuten.tech.mobile.testapp.ui.settings.SettingsProgressDialog
import com.rakuten.tech.mobile.testapp.ui.userdata.*
import kotlinx.android.synthetic.main.settings_fragment.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.net.URL
import java.security.SecureRandom
import java.util.*
import kotlin.properties.Delegates

@Suppress("WildcardImport", "TooManyFunctions", "Deprecation", "EmptyFunctionBlock")
class SettingsFragment : BaseFragment() {

    override val pageName: String = this::class.simpleName ?: ""
    override val siteSection: String = this::class.simpleName ?: ""
    private lateinit var binding: SettingsFragmentBinding
    private lateinit var settings: AppSettings
    private lateinit var settingsProgressDialog: SettingsProgressDialog
    private var isTab1Checked = true
    private val settingsTextWatcher = object : TextWatcher {
        private var old_text = ""
        override fun afterTextChanged(s: Editable?) {
            //empty function intended
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            old_text = binding.editProjectId.text.toString()
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            validateInputIDs()
        }
    }
    private var saveViewEnabled by Delegates.observable(true) { _, old, new ->
        if (new != old) {
            invalidateOptionsMenu(requireActivity())
            if (isTab1Checked) {
                binding.toggleList2.isEnabled = new
            } else {
                binding.toggleList1.isEnabled = new
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.settings_menu, menu)
        menu.findItem(R.id.settings_menu_save).isEnabled = saveViewEnabled
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        super.onOptionsItemSelected(item)
        return when (item.itemId) {
            R.id.settings_menu_save -> {
                onSaveAction()
                item.isEnabled = false
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onStart() {
        super.onStart()
        renderAppSettingsScreen()
        validateInputIDs()
    }

    private fun onSaveAction() {
        if (requireActivity().isAvailable) {
            settingsProgressDialog.show()
        }

        val currentConfigData = getCurrentTypedConfigData()

        if (isTab1Checked) {
            settings.setTempTab1ConfigData(currentConfigData)
        } else {
            settings.setTempTab2ConfigData(currentConfigData)
        }

        settings.saveData()
        updateSettings()
    }

    private fun updateSettings() {
        settings.urlParameters = binding.editParametersUrl.text.toString()

        launch {
            URL("https://www.test-param.com?${binding.editParametersUrl.text.toString()}").toURI()
            settings.isSettingSaved = true
            with(requireActivity()) {
                currentFocus?.let {
                    hideSoftKeyboard(it)
                }
                runOnUiThread {
                    if (isAvailable) {
                        settingsProgressDialog.cancel()
                    }
                    validateInputIDs()
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        setHasOptionsMenu(true)
        settings = AppSettings.instance
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(
            inflater, R.layout.settings_fragment, container, false
        )
        settingsProgressDialog = SettingsProgressDialog(requireActivity())
        setViewsListener()
        return binding.root
    }

    private fun validateInputIDs() {
        val isInputValid = isInputIDValid()

        if (isInputEmpty(binding.editProjectId) || !isInputValid) binding.inputProjectId.error =
            getString(R.string.error_invalid_input)
        else binding.inputProjectId.error = null

        if (isInputEmpty(binding.editSubscriptionKey)) binding.inputSubscriptionKey.error =
            getString(R.string.error_invalid_input)
        else binding.inputSubscriptionKey.error = null


        saveViewEnabled =
            binding.inputProjectId.error == null && binding.inputSubscriptionKey.error == null
    }

    private fun isInputIDValid(): Boolean {
        return !binding.editProjectId.text.toString().isInvalidUuid()
    }

    @Suppress("LongMethod")
    private fun setViewsListener() {
        binding.textInfo.text = createBuildInfo()
        binding.editProjectId.addTextChangedListener(settingsTextWatcher)
        binding.editSubscriptionKey.addTextChangedListener(settingsTextWatcher)

        binding.buttonProfile.setOnClickListener {
            ProfileSettingsActivity.start(requireActivity())
        }

        binding.buttonContacts.setOnClickListener {
            ContactListActivity.start(requireActivity())
        }

        binding.buttonAccessToken.setOnClickListener {
            AccessTokenActivity.start(requireActivity())
        }

        binding.buttonPoints.setOnClickListener {
            PointsActivity.start(requireActivity())
        }

        binding.buttonDeeplink.setOnClickListener {
            DynamicDeepLinkActivity.start(requireActivity())
        }

        binding.buttonQA.setOnClickListener {
            QASettingsActivity.start(requireActivity())
        }

        binding.switchPreviewModeTab.setOnCheckedChangeListener { _, isChecked ->
            if (isTab1Checked) {
                settings.setTempTab1IsPreviewMode(isChecked)
            } else {
                settings.setTempTab2IsPreviewMode(isChecked)
            }
            binding.switchPreviewModeTab.isChecked = isChecked
        }

        binding.switchProdVersion.setOnCheckedChangeListener { _, isChecked ->
            if (isTab1Checked) {
                settings.setTempTab1IsProduction(isChecked)
            } else {
                settings.setTempTab2IsProduction(isChecked)
            }
            binding.switchProdVersion.isChecked = isChecked
            updateTabProjectIdAndSubscription()
        }

        binding.switchSignatureVerification.setOnCheckedChangeListener { _, isChecked ->
            if (isTab1Checked) {
                settings.setTempTab1IsVerificationRequired(isChecked)
            } else {
                settings.setTempTab2IsVerificationRequired(isChecked)
            }
            binding.switchSignatureVerification.isChecked = isChecked
        }

        binding.toggleList1.setOnClickListener {
            if (isTab1Checked) {
                binding.toggleList1.isChecked = true
                return@setOnClickListener
            }
            isTab1Checked = true
            settings.setTempTab2ConfigData(getCurrentTypedConfigData())
            updateTabProjectIdAndSubscription()
        }

        binding.toggleList2.setOnClickListener {
            if (!isTab1Checked) {
                binding.toggleList2.isChecked = true
                return@setOnClickListener
            }
            isTab1Checked = false
            settings.setTempTab1ConfigData(getCurrentTypedConfigData())
            updateTabProjectIdAndSubscription()
        }
    }

    private fun renderAppSettingsScreen() {
        binding.editParametersUrl.setText(settings.urlParameters)

        val defaultConfigData = settings.getDefaultConfigData()
        setupConfigDataToView(defaultConfigData)
        // add the default profile pic initially.
        updateProfileImageBase64()
        // add the default contacts initially.
        addDefaultContactList()
    }

    private fun getCurrentTypedConfigData(): MiniAppConfigData {
        return MiniAppConfigData(
            isProduction = binding.switchProdVersion.isChecked,
            isPreviewMode = binding.switchPreviewModeTab.isChecked,
            isVerificationRequired = binding.switchSignatureVerification.isChecked,
            projectId = binding.editProjectId.text.toString().trim(),
            subscriptionId = binding.editSubscriptionKey.text.toString().trim()
        )
    }

    private fun updateTabProjectIdAndSubscription() {
        val configData: MiniAppConfigData = if (isTab1Checked) {
            settings.getCurrentTab1ConfigData()
        } else {
            settings.getCurrentTab2ConfigData()
        }
        setupConfigDataToView(configData)
    }

    private fun setupConfigDataToView(configData: MiniAppConfigData) {
        binding.switchProdVersion.isChecked = configData.isProduction
        binding.switchPreviewModeTab.isChecked = configData.isPreviewMode
        binding.switchSignatureVerification.isChecked = configData.isVerificationRequired
        binding.editProjectId.setText(configData.projectId)
        binding.editSubscriptionKey.setText(configData.subscriptionId)
    }

    private fun createBuildInfo(): String {
        val sdkVersion = getString(R.string.miniapp_sdk_version)
        val buildVersion = getString(R.string.build_version)
        return "Build $sdkVersion - $buildVersion"
    }

    private fun updateProfileImageBase64() {
        if (AppSettings.instance.profilePictureUrlBase64 == "") {
            encodeImageForMiniApp()
        }
    }

    @Suppress("MagicNumber")
    private fun encodeImageForMiniApp() {
        launch {
            try {
                withContext(Dispatchers.IO) {
                    val bitmap =
                        BitmapFactory.decodeResource(resources, R.drawable.r_logo_default_profile)
                    val byteStream = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteStream)
                    val byteArray = byteStream.toByteArray()
                    val base64DataPrefix = "data:image/png;base64,"
                    val profileUrlBase64 = base64DataPrefix + Base64.encodeToString(
                        byteArray, Base64.DEFAULT
                    )
                    AppSettings.instance.profilePictureUrlBase64 = profileUrlBase64
                }
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun addDefaultContactList() {
        if (!settings.isContactsSaved) {
            settings.contacts = createRandomContactList()
        }
    }

    @Suppress("UnusedPrivateMember", "MagicNumber")
    private fun createRandomContactList(): ArrayList<Contact> = ArrayList<Contact>().apply {
        for (i in 1..10) {
            this.add(createRandomContact())
        }
    }

    @Suppress("MaxLineLength")
    private fun createRandomContact(): Contact {
        val firstName =
            AppSettings.fakeFirstNames[(SecureRandom().nextDouble() * AppSettings.fakeFirstNames.size).toInt()]
        val lastName =
            AppSettings.fakeLastNames[(SecureRandom().nextDouble() * AppSettings.fakeLastNames.size).toInt()]
        val email =
            firstName.toLowerCase(Locale.ROOT) + "." + lastName.toLowerCase(Locale.ROOT) + "@example.com"
        return Contact(UUID.randomUUID().toString().trimEnd(), "$firstName $lastName", email)
    }
}
