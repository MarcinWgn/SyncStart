package com.wegrzyn.marcin.ignition

import android.Manifest
import android.os.Build
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.wegrzyn.marcin.ignition.viewModel.MainViewModel


const val TAG = "btperm"

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun BluetoothPermission(model: MainViewModel = hiltViewModel()) {

    Log.d(TAG, "BluetoothPermission: start")
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val btPermissionState =
            rememberPermissionState(permission = Manifest.permission.BLUETOOTH_CONNECT)
        if (!btPermissionState.status.isGranted) {
            Log.d(TAG, "BluetoothPermission: false")
            model.setBtGranted(false)
            Card(modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { btPermissionState.launchPermissionRequest() }
                        .padding(8.dp)
                ) {
                    Text(text = stringResource(R.string.bt_perm_is_not_granted))
                    if (btPermissionState.status.shouldShowRationale) {
                        Text(text = stringResource(R.string.perm_is_necessary))
                    }
                }
            }

        } else {
            Log.d(TAG, "BluetoothPermission: true")
            model.setBtGranted(true)
        }

    }
}