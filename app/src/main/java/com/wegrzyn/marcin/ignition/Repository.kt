package com.wegrzyn.marcin.ignition

import com.wegrzyn.marcin.ignition.bt.MyBtService
import com.wegrzyn.marcin.ignition.di.IoScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Repository @Inject constructor(
    val bluetooth: MyBtService,
    @IoScope val ioScope: CoroutineScope,
) {
    val btState = bluetooth.state
    val btGranted = MutableStateFlow(true)
}