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
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import java.util.Locale

class YarvisService : Service() {

    private val channelId = "yarvis_service"
    private val handler = Handler(Looper.getMainLooper())

    private var recognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var listening = false

    override fun onCreate() {
        super.onCreate()
        crearNotificacion()
        iniciarVoz()
        iniciarReconocimiento()
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

    private fun iniciarVoz() {
        tts = TextToSpeech(this) { result ->
            if (result == TextToSpeech.SUCCESS) {
                tts?.language = Locale("es", "PE")
                tts?.setSpeechRate(0.88f)
                tts?.setPitch(0.75f)
            }
        }
    }

    private fun iniciarReconocimiento() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            hablar("No encuentro un servicio de reconocimiento de voz.")
            return
        }

        recognizer = SpeechRecognizer.createSpeechRecognizer(this)

        recognizer?.setRecognitionListener(object : RecognitionListener {

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
                programarEscucha(1500)
            }

            override fun onResults(results: Bundle?) {
                listening = false

                val frases = results?.getStringArrayList(
                    SpeechRecognizer.RESULTS_RECOGNITION
                )

                val frase = frases?.firstOrNull()?.lowercase(Locale.ROOT) ?: ""

                if (frase.contains("hola yarvis") ||
                    frase.contains("hola jarvis")) {
                    hablar("Hola. Estoy aquí. ¿En qué puedo ayudarte?")
                    programarEscucha(2500)
                } else {
                    programarEscucha(500)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        programarEscucha(1000)
    }

    private fun programarEscucha(retardo: Long) {
        handler.postDelayed({
            escuchar()
        }, retardo)
    }

    private fun escuchar() {
        if (listening || recognizer == null) return

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-PE")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }

        try {
            listening = true
            recognizer?.startListening(intent)
        } catch (e: Exception) {
            listening = false
            programarEscucha(2000)
        }
    }

    private fun hablar(mensaje: String) {
        tts?.speak(
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
        handler.removeCallbacksAndMessages(null)
        recognizer?.cancel()
        recognizer?.destroy()
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
