package com.wegrzyn.marcin.ignition

import android.util.Log
import com.wegrzyn.marcin.ignition.bt.MyBtService
import com.wegrzyn.marcin.ignition.bt.State
import com.wegrzyn.marcin.ignition.di.IoScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Repository @Inject constructor(
    val bluetooth: MyBtService,
    @IoScope val ioScope: CoroutineScope,
) {
    val btState = bluetooth.state

    val btGranted = MutableStateFlow(true)

    private val data = bluetooth.data

    val rev = MutableStateFlow("00")

    var active = MutableStateFlow(false)


    private val adrTab = listOf(
        Pair("3b3", "40 80 00 12 71 03 81 00"),
        Pair("3b2", "40 88 C0 12 10 03 80 02"),
        Pair("3b1", "08 00 00 00 00 00 00 00")
    )
    private var frame = 0
    private var configJob: Job? = null


    init {
        ioScope.launch {
            data.collect{
                Log.d(TAG, "data: $it ")
            }
        }
    }

    fun startStop(selected: Int) {
        frame = selected
        if (active.value) {
            stop()
        } else {
            start()
            rev.value="00"
        }
    }

    fun setReverse(){
        if (rev.value=="00"){
            rev.value ="01"
        }else{
            rev.value = "00"
        }
    }

    fun stop() {
        rev.value="00"
        configJob?.cancel()
    }

    private fun start() {
        configJob = ioScope.launch {
            active.value = true

            bluetooth.write("ATZ")
            delay(800)
            bluetooth.write("AT SP6")
            delay(300)
            bluetooth.write("AT E0")
            delay(300)
            bluetooth.write("AT CAF0")
            delay(300)
            bluetooth.write("AT AL")
            delay(300)

            while (active.value) {

                bluetooth.write("AT SH" + adrTab[frame].first)
                Log.d(TAG, "write:  ${adrTab[frame].first}")
                delay(200)

                bluetooth.write(adrTab[frame].second)
                Log.d(TAG, "write:  ${adrTab[frame].second}")
                delay(200)

                bluetooth.write("AT SH 109")
                Log.d(TAG, "write:  AT SH 109")

                delay(200)
                bluetooth.write("00 03 F1 00 00 00 ${rev.value} 00")
                Log.d(TAG, "write: 00 03 F1 00 00 00 ${rev.value} 00")
                delay(200)
            }
        }
        configJob?.invokeOnCompletion {
            active.value = false
        }
    }


    init {
        stopSendingDetect()
    }

    private fun stopSendingDetect() {
        ioScope.launch {
            btState.collect {
                if (active.value && it == State.NONE) {
                    stop()
                }
            }
        }.start()
    }
}