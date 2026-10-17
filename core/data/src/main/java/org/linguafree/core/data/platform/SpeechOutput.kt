package org.linguafree.core.data.platform

import android.content.Context
import android.net.Uri
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * English voice: Android TextToSpeech (en-US, best installed offline voice) for generated text,
 * Media3 ExoPlayer for recorded files referenced as `asset:path/file.ogg`.
 */
class SpeechOutput(private val context: Context) : TextToSpeech.OnInitListener {

    enum class Status { Initializing, Ready, NoEnglishVoice, Unavailable }

    private val _status = MutableStateFlow(Status.Initializing)
    val status: StateFlow<Status> = _status.asStateFlow()

    private var tts: TextToSpeech? = null
    private var player: ExoPlayer? = null
    private var pending: Pair<String, Boolean>? = null
    var speed: Float = 1.0f

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        val engine = tts
        if (status != TextToSpeech.SUCCESS || engine == null) {
            _status.value = Status.Unavailable
            return
        }
        val result = engine.setLanguage(Locale.US)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            _status.value = Status.NoEnglishVoice
            return
        }
        bestVoice(engine.voices.orEmpty())?.let { engine.voice = it }
        _status.value = Status.Ready
        pending?.let { (t, slow) -> speak(t, slow) }
        pending = null
    }

    /** Prefers offline US English voices of the highest quality. */
    private fun bestVoice(voices: Set<Voice>): Voice? {
        val english = voices.filter { it.locale.language == "en" && !it.isNetworkConnectionRequired && "notInstalled" !in it.features }
        return english.filter { it.locale.country == "US" }.maxByOrNull { it.quality } ?: english.maxByOrNull { it.quality }
    }

    val voiceName: String? get() = tts?.voice?.name

    fun speak(text: String, slow: Boolean = false) {
        if (text.startsWith("asset:")) {
            playAsset(text.removePrefix("asset:"), slow)
            return
        }
        val engine = tts
        if (_status.value != Status.Ready || engine == null) {
            pending = text to slow
            return
        }
        engine.setSpeechRate((if (slow) 0.55f else 0.95f) * speed)
        engine.setPitch(1.0f)
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "lf-${System.nanoTime()}")
    }

    private fun playAsset(path: String, slow: Boolean) {
        val p = player ?: ExoPlayer.Builder(context).build().also { player = it }
        p.setMediaItem(MediaItem.fromUri(Uri.parse("asset:///$path")))
        p.playbackParameters = PlaybackParameters(if (slow) 0.7f else 1f)
        p.prepare()
        p.play()
    }

    fun stop() {
        tts?.stop()
        player?.stop()
    }

    fun release() {
        tts?.shutdown()
        player?.release()
        tts = null
        player = null
    }
}
