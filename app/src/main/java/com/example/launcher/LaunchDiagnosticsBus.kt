package com.example.launcher

import com.example.model.InProcessLaunchFailure
import com.example.model.ProbeReport
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object LaunchDiagnosticsBus {
    private val _lastFailure = MutableStateFlow<InProcessLaunchFailure?>(null)
    val lastFailure: StateFlow<InProcessLaunchFailure?> = _lastFailure.asStateFlow()

    private val _latestProbe = MutableStateFlow<ProbeReport?>(null)
    val latestProbe: StateFlow<ProbeReport?> = _latestProbe.asStateFlow()

    fun publishFailure(failure: InProcessLaunchFailure?) {
        _lastFailure.value = failure
    }

    fun publishProbe(report: ProbeReport?) {
        _latestProbe.value = report
    }
}
