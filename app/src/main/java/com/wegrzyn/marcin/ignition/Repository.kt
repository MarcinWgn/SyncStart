package com.wegrzyn.marcin.ignition

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

    private val _txt = MutableStateFlow("")
    val txt = _txt.asStateFlow()

    val btGranted = MutableStateFlow(true)

    var active = MutableStateFlow(false)

    val adrTab = listOf(
        Pair("3B3", "41 00 00 00 4С 00 00 00"),
        Pair("3B3", "00 00 00 00 07 00 e0 00"),
        Pair("048", "00 00 00 00 07 00 E0 00")
    )
    val frame = 0
    lateinit var configJob: Job
    lateinit var sendingJob: Job

    fun startStop() {
        if (active.value) {
            stop()
        } else {
            start()
        }
    }

    private fun stop() {
        configJob.cancel()
        sendingJob.cancel()
    }

    private fun start() {
        configJob = ioScope.launch {

            active.value = true

            bluetooth.write("ATZ")
            delay(1000)
            bluetooth.write("AT SP6")
            delay(500)
            bluetooth.write("AT E0")
            delay(500)
            bluetooth.write("AT CAF0")
            delay(500)
            bluetooth.write("AT AL")
            delay(500)
        }
        sendingJob = ioScope.launch {

            active.value = true

            bluetooth.write(adrTab[frame].first)
            delay(500)

            while (active.value) {
                bluetooth.write(adrTab[frame].second)
                delay(500)
            }
        }
        sendingJob.invokeOnCompletion {
            active.value = false
        }

        ioScope.launch {
            configJob.start()
            configJob.join()
            _txt.value += "finish config \n"
            sendingJob.join()
            sendingJob.start()
        }.start()
    }

    init {
        ioScope.launch {
            bluetooth.data.collect {
                _txt.value += it + "\n"
            }
        }.start()

        ioScope.launch {
            btState.collect{
                if (active.value && it==State.NONE){
                    stop()
                }
            }
        }.start()
    }
}