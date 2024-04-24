package com.example.myapplication

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import be.tarsos.dsp.AudioDispatcher
import be.tarsos.dsp.io.TarsosDSPAudioFormat
import be.tarsos.dsp.io.android.AudioDispatcherFactory
import be.tarsos.dsp.pitch.PitchDetectionHandler
import be.tarsos.dsp.pitch.PitchProcessor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.nio.ByteOrder
import javax.inject.Inject

@HiltViewModel
class PitchTestViewModel @Inject constructor(
) : ViewModel() {
    private val _note = mutableStateOf("")
    val note: State<String> = _note

    private var dispatcher: AudioDispatcher? = null
    var tarsosDSPAudioFormat: TarsosDSPAudioFormat? = null

    fun hasMicrophonePermission(context: Context) =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

    fun startAudioListener(context: Context) = viewModelScope.launch(Dispatchers.Default) {
        if (!hasMicrophonePermission(context)) return@launch
        tarsosDSPAudioFormat = TarsosDSPAudioFormat(
            TarsosDSPAudioFormat.Encoding.PCM_SIGNED,
            22050f,
            2 * 8,
            1,
            2 * 1,
            22050f, ByteOrder.BIG_ENDIAN == ByteOrder.nativeOrder()
        )

        dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(
            SAMPLE_RATE.toInt(),
            BUFFER_SIZE,
            BUFFER_OVERLAP
        )

        val processor = PitchProcessor(PitchProcessor.PitchEstimationAlgorithm.FFT_YIN, SAMPLE_RATE, BUFFER_SIZE, pitchDetectionHandler())

        dispatcher?.addAudioProcessor(processor)
        dispatcher?.run()
    }

    fun stopAudioListener() = dispatcher?.stop()

    private fun pitchDetectionHandler() = PitchDetectionHandler { result, audioEvent ->
        viewModelScope.launch(Dispatchers.Default) {
            val pitchInHz = result.pitch.toDouble()
            _note.value = ProcessPitch.processPitch(result.pitch)
        }
    }

    companion object {
        const val SAMPLE_RATE = 22050F
        const val BUFFER_SIZE = 1024
        const val BUFFER_OVERLAP = 0
    }
}
