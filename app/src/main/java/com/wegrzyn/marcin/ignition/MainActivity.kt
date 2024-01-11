package com.wegrzyn.marcin.ignition

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.wegrzyn.marcin.ignition.bt.State
import com.wegrzyn.marcin.ignition.ui.theme.IgnitionTheme
import com.wegrzyn.marcin.ignition.viewModel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val model: MainViewModel by viewModels()
    private lateinit var btEnable: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            IgnitionTheme {
                val nc = rememberNavController()
                MainView(nc = nc, onClick = {
                    btOnRequest()
                })
            }
        }
        btEnable = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                model.repo.bluetooth.start(devAdr = model.getDevice() ?: "OBD")
            }
        }
    }

    private fun btOnRequest() {
        if (model.btGranted.value) {
            btEnable.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        }
    }

}

@Composable
fun MainView(nc: NavHostController, onClick: () -> Unit) {
    NavHost(
        navController = nc,
        startDestination = DestinationScreen.MainScreen.route
    ) {
        composable(DestinationScreen.MainScreen.route) { MainScr(nc = nc, onClick = onClick) }
        composable(DestinationScreen.SettingsScreen.route) { SettingsScr(nc = nc) }
    }

}

sealed class DestinationScreen(val route: String) {
    object MainScreen : DestinationScreen("main")
    object SettingsScreen : DestinationScreen("settings")
}

@Composable
fun MainScr(nc: NavController, onClick: () -> Unit, model: MainViewModel = hiltViewModel()) {

    val txt by model.repo.txt.collectAsState()


    Scaffold(topBar = { MainTopBar(nc = nc, onClick = onClick) }) {
        Column(modifier = Modifier
            .padding(it)
            .padding(8.dp)) {
            BluetoothPermission()
            RadioSelected()
            Text(
                text = txt, modifier = Modifier
                    .padding(8.dp)
                    .verticalScroll(state = rememberScrollState())
            )
        }
    }
}

@Composable
fun RadioSelected(model: MainViewModel = hiltViewModel()) {

    val active by model.repo.active.collectAsState()
    val state by model.btState.collectAsState()
    val ctx = LocalContext.current

    val rbOptions = List(3) { it }
    var selected by remember {
        mutableStateOf(rbOptions[0])
    }
    Row {
        rbOptions.forEach {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    RadioButton(selected = it == selected, onClick = {
                        selected = it
                        model.repo.stop()
                    })
                    Text(text = it.toString())
                }
            }
        }
        Button(onClick = {
            if (state == State.CONNECTED) {
                model.repo.startStop(selected = selected)
            } else {
                Toast.makeText(ctx, "Brak połączenia BT!!!", Toast.LENGTH_SHORT).show()
            }

        }, modifier = Modifier.padding(8.dp)) {
            Column() {
                Text(text = "ign", fontSize = 16.sp)
                Text(
                    fontSize = 16.sp,
                    text = if (active) {
                        "stop"
                    } else {
                        "start"
                    }
                )
            }
        }
    }
}

@Composable
fun SettingsScr(nc: NavHostController, model: MainViewModel = hiltViewModel()) {
    var openDevDialog by remember { mutableStateOf(false) }
    val btGranted by model.btGranted.collectAsState()

    Scaffold(topBar = { SettingsTopBar(nc = nc) }) {

        Column(modifier = Modifier.padding(it)) {
            Text(
                text = "Urządzenie",
                modifier = Modifier.padding(start = 50.dp, top = 8.dp, bottom = 16.dp)
            )
            DevDialog(
                openDialog = openDevDialog,
                close = { openDevDialog = false },
            )
            Text(
                text = "wybrane urzadzenie",
                Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (btGranted) {
                            openDevDialog = true
                        }
                    }
                    .padding(start = 50.dp, bottom = 16.dp, top = 16.dp),
                fontWeight = FontWeight.Bold
            )
            Divider()
        }
    }
}

@Composable
fun DevDialog(openDialog: Boolean, close: () -> Unit) {
    if (openDialog) {
        AlertDialog(
            onDismissRequest = close,
            confirmButton = {
                Button(onClick = close) {
                    Text(text = "confirm")
                }
            },
            properties = DialogProperties(),
            text = { DevRadioBtnList() },
            title = { Text(text = "selected device") },
        )
    }
}

@SuppressLint("MissingPermission")
@Composable
fun DevRadioBtnList(model: MainViewModel = hiltViewModel()) {

    val devs = model.repo.bluetooth.getPairedDevices()

    var (selectedDev, onDevSelected) = remember {
        mutableStateOf("")
    }
    model.getDevice()?.let {
        selectedDev = it
    }
    Column(
        modifier = Modifier
            .padding(8.dp)
            .selectableGroup()
            .verticalScroll(state = rememberScrollState())
    ) {
        devs?.let {
            it.forEach { dev ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()

                        .clip(CircleShape)
                        .selectable(
                            selected = (dev.address == selectedDev),
                            onClick = {
                                onDevSelected(dev.address)
                                model.setDevice(dev.address)
                            },
                            role = Role.RadioButton
                        )
                ) {
                    RadioButton(
                        selected = (dev.address == selectedDev),
                        onClick = null,
                        modifier = Modifier.padding(8.dp)
                    )

                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(text = dev.name)
                        Text(text = dev.address)
                    }

                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTopBar(nc: NavController, model: MainViewModel = hiltViewModel(), onClick: () -> Unit) {

    val state by model.btState.collectAsState()
    val btIcon = when (state) {
        State.CONNECTING -> painterResource(id = R.drawable.bluetooth_searching_24)
        State.CONNECTED -> painterResource(id = R.drawable.bluetooth_connected_24)
        State.NONE -> painterResource(id = R.drawable.bluetooth_disabled_24)

    }
    TopAppBar(title = { Text(text = "Ignition") }, actions = {

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = state.name)
            IconButton(onClick = onClick) {
                Icon(painter = btIcon, contentDescription = "connect state")
            }
            IconButton(onClick = {
                nc.navigate(DestinationScreen.SettingsScreen.route) {
                    launchSingleTop = true
                }
            }) {
                Icon(imageVector = Icons.Default.Menu, contentDescription = "menu")
            }
        }

    })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTopBar(nc: NavHostController) {
    TopAppBar(navigationIcon = {
        IconButton(onClick = {
            nc.navigate(DestinationScreen.MainScreen.route) {
                launchSingleTop = true
            }
        }) {
            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "back")
        }
    }, title = { Text(text = "Settings") })
}
