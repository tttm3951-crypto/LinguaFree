package org.linguafree.core.data.platform

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Texts shown when recognition fails. */
object SpeechHelp {
    /** Shown on any recognizer/language error (requested wording). */
    const val OFFLINE_PACK_HINT =
        "Скачайте офлайн-пакет английского в настройках голосового ввода Google: «Настройки → Google → Голосовой ввод → Офлайн-распознавание → English (US)»."
    const val NOT_HEARD = "Не удалось расслышать. Говори чуть громче и ближе к телефону."
    const val NO_RECOGNIZER = "На телефоне нет службы распознавания речи. " + OFFLINE_PACK_HINT
    const val NO_PERMISSION = "Нужен доступ к микрофону, чтобы проверить произношение."

    /** Opens voice input settings (where the offline English pack is downloaded). */
    fun settingsIntent(): Intent = Intent(Settings.ACTION_VOICE_INPUT_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    fun fallbackSettingsIntent(): Intent = Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}

/**
 * In-app speech recognition without the Google dialog. Prefers the on-device recognizer
 * (Android 12+), otherwise the default service with EXTRA_PREFER_OFFLINE. Never needs
 * the app's own internet access. Must be used from the main thread.
 */
class SpeechInput(private val context: Context) {

    sealed interface State {
        data object Idle : State
        data object Listening : State
        /** Microphone level 0..1 for the animation. */
        data class Level(val value: Float) : State
        data class Partial(val text: String) : State
        data class Result(val alternatives: List<String>) : State
        data class Error(val message: String, val showPackHint: Boolean) : State
    }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()
    private var recognizer: SpeechRecognizer? = null
    private var triedFallback = false

    val isAvailable: Boolean get() = SpeechRecognizer.isRecognitionAvailable(context) || onDeviceAvailable()

    private fun onDeviceAvailable(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && runCatching { SpeechRecognizer.isOnDeviceRecognitionAvailable(context) }.getOrDefault(false)

    fun start() {
        cancel()
        triedFallback = false
        if (!isAvailable) {
            _state.value = State.Error(SpeechHelp.NO_RECOGNIZER, showPackHint = true)
            return
        }
        launch(onDevice = onDeviceAvailable())
    }

    private fun launch(onDevice: Boolean) {
        val r = runCatching {
            if (onDevice && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
            else SpeechRecognizer.createSpeechRecognizer(context)
        }.getOrNull()
        if (r == null) {
            _state.value = State.Error(SpeechHelp.NO_RECOGNIZER, showPackHint = true)
            return
        }
        recognizer = r
        r.setRecognitionListener(listener(onDevice))
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-US")
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        }
        _state.value = State.Listening
        runCatching { r.startListening(intent) }.onFailure {
            _state.value = State.Error(SpeechHelp.OFFLINE_PACK_HINT, showPackHint = true)
        }
    }

    private fun listener(onDevice: Boolean) = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) { _state.value = State.Listening }
        override fun onBeginningOfSpeech() = Unit
        override fun onRmsChanged(rmsdB: Float) { _state.value = State.Level(((rmsdB + 2f) / 12f).coerceIn(0f, 1f)) }
        override fun onBufferReceived(buffer: ByteArray?) = Unit
        override fun onEndOfSpeech() = Unit
        override fun onEvent(eventType: Int, params: Bundle?) = Unit

        override fun onPartialResults(partialResults: Bundle?) {
            partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let {
                if (it.isNotBlank()) _state.value = State.Partial(it)
            }
        }

        override fun onResults(results: Bundle?) {
            val list = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty().filter { it.isNotBlank() }
            _state.value = if (list.isEmpty()) State.Error(SpeechHelp.NOT_HEARD, showPackHint = false) else State.Result(list)
            destroy()
        }

        override fun onError(error: Int) {
            destroy()
            // The on-device model may be missing for English: retry once with the default service.
            if (onDevice && !triedFallback && error != SpeechRecognizer.ERROR_NO_MATCH && error != SpeechRecognizer.ERROR_SPEECH_TIMEOUT &&
                SpeechRecognizer.isRecognitionAvailable(context)
            ) {
                triedFallback = true
                launch(onDevice = false)
                return
            }
            _state.value = when (error) {
                SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> State.Error(SpeechHelp.NOT_HEARD, showPackHint = false)
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> State.Error(SpeechHelp.NO_PERMISSION, showPackHint = false)
                else -> State.Error(SpeechHelp.OFFLINE_PACK_HINT, showPackHint = true)
            }
        }
    }

    fun stop() { runCatching { recognizer?.stopListening() } }

    fun cancel() {
        runCatching { recognizer?.cancel() }
        destroy()
        _state.value = State.Idle
    }

    fun reset() { _state.value = State.Idle }

    private fun destroy() {
        runCatching { recognizer?.destroy() }
        recognizer = null
    }
}
