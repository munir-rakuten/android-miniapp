package com.rakuten.tech.mobile.miniapp.permission.ui

import android.app.Activity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rakuten.tech.mobile.miniapp.R
import com.rakuten.tech.mobile.miniapp.permission.CustomPermissionBridgeDispatcher
import com.rakuten.tech.mobile.miniapp.permission.MiniAppCustomPermissionResult
import com.rakuten.tech.mobile.miniapp.permission.MiniAppCustomPermissionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * A class to show default custom permissions UI to manage permissions in this SDK.
 */

@SuppressWarnings("LongMethod")
internal class MiniAppCustomPermissionWindow(
    private val activity: Activity,
    private val customPermissionBridgeDispatcher: CustomPermissionBridgeDispatcher,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {

    @VisibleForTesting
    internal lateinit var customPermissionAlertDialog: AlertDialog

    @VisibleForTesting
    internal lateinit var customPermissionAdapter: MiniAppCustomPermissionAdapter

    @VisibleForTesting
    internal lateinit var customPermissionLayout: View

    fun displayPermissions(
        miniAppId: String,
        deniedPermissions: List<Pair<MiniAppCustomPermissionType, String>>
    ) {
        if (miniAppId.isEmpty())
            return

        scope.launch {
            // show permission default UI if there is any denied permission
            if (deniedPermissions.isNotEmpty()) {
                // initialize permission view after ensuring if there is any denied permission
                initCustomPermissionLayout()
                val recyclerView = getRecyclerView()
                initAdapterAndDialog(recyclerView)
                prepareDataForAdapter(deniedPermissions)

                // add action listeners
                addPermissionClickListeners()

                // preview dialog
                customPermissionAlertDialog.show()
            }
        }
    }

    @VisibleForTesting
    internal fun initCustomPermissionLayout() {
        customPermissionLayout =
            LayoutInflater.from(activity).inflate(R.layout.window_custom_permission, null)
    }

    @VisibleForTesting
    internal fun getRecyclerView(): RecyclerView {
        val permissionRecyclerView = customPermissionLayout.findViewById<RecyclerView>(R.id.listCustomPermission)
        permissionRecyclerView.layoutManager = LinearLayoutManager(activity)
        permissionRecyclerView.addItemDecoration(
            DividerItemDecoration(activity, DividerItemDecoration.VERTICAL)
        )
        return permissionRecyclerView
    }

    @VisibleForTesting
    internal fun initAdapterAndDialog(permissionRecyclerView: RecyclerView) {
        customPermissionAdapter = MiniAppCustomPermissionAdapter()
        permissionRecyclerView.adapter = customPermissionAdapter

        customPermissionAlertDialog =
            AlertDialog.Builder(activity, R.style.AppTheme_CustomPermissionDialog).create()
        customPermissionAlertDialog.setView(customPermissionLayout)
    }

    @VisibleForTesting
    internal fun prepareDataForAdapter(deniedPermissions: List<Pair<MiniAppCustomPermissionType, String>>) {
        val namesForAdapter: ArrayList<MiniAppCustomPermissionType> = arrayListOf()
        val resultsForAdapter: ArrayList<MiniAppCustomPermissionResult> = arrayListOf()
        val descriptionForAdapter: ArrayList<String> = arrayListOf()

        deniedPermissions.forEach {
            namesForAdapter.add(it.first)
            descriptionForAdapter.add(it.second)
            resultsForAdapter.add(MiniAppCustomPermissionResult.ALLOWED)
        }

        customPermissionAdapter.addPermissionList(
            namesForAdapter,
            resultsForAdapter,
            descriptionForAdapter
        )
    }

    @VisibleForTesting
    internal fun addPermissionClickListeners() {
        customPermissionLayout.findViewById<TextView>(R.id.permissionSave).setOnClickListener {
            customPermissionBridgeDispatcher.sendHostAppCustomPermissions(customPermissionAdapter.permissionPairs)
            customPermissionAlertDialog.dismiss()
        }

        customPermissionLayout.findViewById<TextView>(R.id.permissionCloseWindow)
            .setOnClickListener {
                onNoPermissionsSaved()
            }

        customPermissionAlertDialog.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                onNoPermissionsSaved()
                true
            } else false
        }
    }

    @VisibleForTesting
    internal fun onNoPermissionsSaved() {
        customPermissionBridgeDispatcher.sendCachedCustomPermissions()
        customPermissionAlertDialog.dismiss()
    }
}
