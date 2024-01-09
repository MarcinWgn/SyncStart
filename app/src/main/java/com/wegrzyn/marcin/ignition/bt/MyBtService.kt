package com.wegrzyn.marcin.ignition.bt

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import androidx.core.content.ContextCompat.getSystemService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.UUID

@SuppressLint("MissingPermission")
class MyBtService(private val context: Context) {
    private var dev = "OBD"

    private val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    private val _state: MutableStateFlow<State> = MutableStateFlow(State.NONE)
    val state = _state.asStateFlow()

    private val _data: MutableSharedFlow<String> = MutableSharedFlow()
    val data = _data.asSharedFlow()

    private var mmSocket: BluetoothSocket? = null
    private var device: BluetoothDevice? = null

    fun start(devAdr: String) {
        Log.d(TAG, "start: $dev")
        if (mmSocket != null) {
            close()
        }
        try {
            device = getBtDev(devAdr)
            mmSocket = device?.createRfcommSocketToServiceRecord(uuid)
            Log.d(TAG, "start create socket")
        } catch (e: IOException) {
            Log.d(TAG, "create socket failed: $e")
        } catch (e: NullPointerException) {
            Log.d(TAG, "no device $e")
            close()
        }
        connectJobs().start()
    }

    fun getPairedDevices(): MutableSet<BluetoothDevice>? = getBtAdapter(context)?.bondedDevices

    private fun getBtAdapter(context: Context): BluetoothAdapter? {
        val bluetoothManager: BluetoothManager? =
            getSystemService(context, BluetoothManager::class.java)
        return bluetoothManager?.adapter
    }

    private fun getBtDev(devAdr: String): BluetoothDevice? {
        getPairedDevices()?.forEach { device ->
            if (device.address == devAdr) {
                return device
            }
        }
        getPairedDevices()?.forEach { device ->
            if (device.name.contains(dev)) {
                return device
            }
        }
        return null
    }


    private fun close() {
        try {
            if (mmSocket != null) {
                mmSocket?.close()
                mmSocket = null
            }
            connectJobs().cancel()
        } catch (e: IOException) {
            Log.d(TAG, "close socket exception: $e")
        } finally {
            _state.value = State.NONE
        }
    }

    private fun connectJobs() = CoroutineScope(Dispatchers.IO + Job()).launch {
        var job2: Job? = null

        val job = launch(Dispatchers.IO) {
            // TODO: cancel discovery
            _state.value = State.CONNECTING
            Log.d(TAG, "socket: ${mmSocket.toString()}")
            try {
                mmSocket?.connect()
            } catch (e: IOException) {
                Log.d(TAG, "socket exception: $e")
                close()
                job2?.cancel()
            } catch (e: CancellationException) {
                Log.d(TAG, "socket cancel exception: $e")
            }
        }

        job2 = launch(Dispatchers.IO) {

            job.join()

            _state.value = State.CONNECTED

            val buffer = ByteArray(1024)
            var run = true
            var numBytes = 0

            while (run && isActive) {
                try {
                    numBytes = mmSocket?.inputStream?.read(buffer)!!
                } catch (e: IOException) {
                    run = false
                    close()
                    Log.e(TAG, "blue exception: $e")
                } catch (e: CancellationException) {
                    run = false
                    close()
                    Log.e(TAG, "job2 cancel ex: $e")
                } catch (e: NullPointerException) {
                    run = false
                    close()
                    Log.e(TAG, "null point cancel: ex: $e")
                }
                _data.emit(buffer.decodeToString(0, numBytes))
            }
        }
    }

    fun write(string: String) {
        try {
            mmSocket?.outputStream?.write("$string\r".toByteArray())
        } catch (e: IOException) {
            Log.d(TAG, "Error occurred when sending data")
        }

    }
}

enum class State {
    NONE, CONNECTING, CONNECTED;
}
const val TAG = "MY_TEST"
