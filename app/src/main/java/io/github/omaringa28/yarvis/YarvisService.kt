
package io.github.omaringa28.yarvis

import android.app.Service
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

class YarvisService : Service() {

    private val channelId = "yarvis_service"
    private val handler = Handler(Looper.getMainLooper())
    private val escuchaRunnable = Runnable { escuchar() }

    private var recognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var listening = false
    private var serviceActive = false
    private var ttsReady = false

    override fun onCreate() {
        super.onCreate()
        serviceActive = true
        crearNotificacion()
        mantenerProcesadorActivo()
        iniciarVoz()
        crearReconocedor()
        programarEscucha(1200)
    }

    private fun crearNotificacion() {
        val manager = getSystemService(NotificationManager::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Servicio YARVIS",
                NotificationManager.IMPORTANCE_LOW
            )
            manager.createNotificationChannel(channel)
        }

        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, channelId)
                .setContentTitle("YARVIS está activo")
                .setContentText("Preparado para escuchar tu voz")
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setOngoing(true)
                .build()
        } else {
            Notification.Builder(this)
                .setContentTitle("YARVIS está activo")
                .setContentText("Preparado para escuchar tu voz")
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setOngoing(true)
                .build()
        }

        startForeground(1, notification)
    }

    private fun mantenerProcesadorActivo() {
        try {
            val powerManager = getSystemService(POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "YARVIS::ReconocimientoVoz"
            ).apply {
                setReferenceCounted(false)
                acquire()
            }
        } catch (_: Exception) {
            wakeLock = null
        }
    }

    private fun iniciarVoz() {
        tts = TextToSpeech(this) { result ->
            if (result == TextToSpeech.SUCCESS) {
                val motor = tts ?: return@TextToSpeech
                motor.language = Locale("es", "PE")
                motor.setSpeechRate(0.88f)
                motor.setPitch(0.75f)

                motor.setOnUtteranceProgressListener(
                    object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {}

                        override fun onDone(utteranceId: String?) {
                            handler.post { programarEscucha(700) }
                        }

                        @Deprecated("Deprecated in Java")
                        override fun onError(utteranceId: String?) {
                            handler.post { programarEscucha(1000) }
                        }

                        override fun onError(
                            utteranceId: String?,
                            errorCode: Int
                        ) {
                            handler.post { programarEscucha(1000) }
                        }
                    }
                )
                ttsReady = true
            }
        }
    }

    private fun crearReconocedor() {
        if (!serviceActive) return

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            recognizer = null
            programarEscucha(5000)
            return
        }

        try {
            recognizer = SpeechRecognizer.createSpeechRecognizer(this)

            recognizer?.setRecognitionListener(
                object : RecognitionListener {

                    override fun onReadyForSpeech(params: Bundle?) {
                        listening = true
                    }

                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        listening = false
                    }

                    override fun onError(error: Int) {
                        listening = false
                        reiniciarReconocedor()
                        programarEscucha(1600)
                    }

                    override fun onResults(results: Bundle?) {
                        listening = false

                        val frases = results?.getStringArrayList(
                            SpeechRecognizer.RESULTS_RECOGNITION
                        )

                        val frase = frases?.firstOrNull()
                            ?.lowercase(Locale.ROOT) ?: ""

                        if (
                            frase.contains("hola yarvis") ||
                            frase.contains("hola jarvis") ||
                            frase.contains("hola yervis")
                        ) {
                            hablar(
                                "Hola. Estoy aquí. ¿En qué puedo ayudarte?"
                            )
                        } else {
                            programarEscucha(600)
                        }
                    }

                    override fun onPartialResults(
                        partialResults: Bundle?
                    ) {}

                    override fun onEvent(
                        eventType: Int,
                        params: Bundle?
                    ) {}
                }
            )
        } catch (_: Exception) {
            recognizer = null
            listening = false
            programarEscucha(2500)
        }
    }

    private fun reiniciarReconocedor() {
        try {
            recognizer?.cancel()
            recognizer?.destroy()
        } catch (_: Exception) {
        }

        recognizer = null
        listening = false
        crearReconocedor()
    }

    private fun programarEscucha(retardo: Long) {
        if (!serviceActive) return

        handler.removeCallbacks(escuchaRunnable)
        handler.postDelayed(escuchaRunnable, retardo)
    }

    private fun escuchar() {
        if (!serviceActive || listening) return

        if (recognizer == null) {
            crearReconocedor()

            if (recognizer == null) {
                programarEscucha(5000)
                return
            }
        }

        val intent = Intent(
            RecognizerIntent.ACTION_RECOGNIZE_SPEECH
        ).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-PE")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(
                RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,
                1200L
            )
            putExtra(
                RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS,
                900L
            )
        }

        try {
            listening = true
            recognizer?.startListening(intent)
        } catch (_: Exception) {
            listening = false
            reiniciarReconocedor()
            programarEscucha(2000)
        }
    }

    private fun hablar(mensaje: String) {
        val motor = tts

        if (!ttsReady || motor == null) {
            programarEscucha(1200)
            return
        }

        motor.speak(
            mensaje,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "yarvis_respuesta"
        )
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        serviceActive = false
        handler.removeCallbacksAndMessages(null)

        try {
            recognizer?.cancel()
            recognizer?.destroy()
        } catch (_: Exception) {
        }

        recognizer = null

        tts?.stop()
        tts?.shutdown()
        tts = null
        ttsReady = false

        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (_: Exception) {
        }

        wakeLock = null

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
