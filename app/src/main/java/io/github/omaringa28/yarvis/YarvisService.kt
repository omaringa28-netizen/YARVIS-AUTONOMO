
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
import java.text.Normalizer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

class YarvisService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private var activo = false
    private var escuchando = false
    private var vozLista = false
    private var esperandoOrden = false

    private val escuchaRunnable = Runnable { escuchar() }
    private val finSesionRunnable = Runnable {
    if (esperandoOrden) {
        esperandoOrden = false
        hablar("Vuelvo a quedar en espera. Di hola Yarvis para llamarme.")
    }
    }

    override fun onCreate() {
        super.onCreate()

        activo = true
        crearNotificacion()
        mantenerProcesadorActivo()
        iniciarVoz()
        crearReconocedor()
        programarEscucha(1500)
    }

    private fun crearNotificacion() {
        val manager = getSystemService(NotificationManager::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    "yarvis_service",
                    "Servicio YARVIS",
                    NotificationManager.IMPORTANCE_LOW
                )
            )
        }

        val notificacion =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Notification.Builder(this, "yarvis_service")
            } else {
                Notification.Builder(this)
            }
                .setContentTitle("YARVIS está activo")
                .setContentText("Preparado para escuchar tu voz")
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setOngoing(true)
                .build()

        startForeground(1, notificacion)
    }

    private fun mantenerProcesadorActivo() {
        try {
            val pm = getSystemService(POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(
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

    private fun normalizar(texto: String): String {
        return Normalizer.normalize(
            texto.lowercase(Locale.ROOT),
            Normalizer.Form.NFD
        )
            .replace(Regex("\\p{Mn}+"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
            .replace("avre", "abre")
            .replace("habre", "abre")
            .replace("jarvis", "yarvis")
            .replace("guasap", "whatsapp")
            .replace("wasap", "whatsapp")
            .replace("toma una foto", "toma foto")
.replace("saca una foto", "saca foto")
            .replace("simpmusic", "simpmusic")
    }

    private fun iniciarVoz() {
        tts = TextToSpeech(this) { resultado ->
            if (resultado == TextToSpeech.SUCCESS) {
                tts?.language = Locale("es", "PE")
                tts?.setSpeechRate(0.88f)
                tts?.setPitch(0.75f)

                tts?.setOnUtteranceProgressListener(
                    object : UtteranceProgressListener() {
                        override fun onStart(id: String?) {}

                        override fun onDone(id: String?) {
                            handler.post {
                                programarEscucha(700)
                            }
                        }

                        @Deprecated("Deprecated in Java")
                        override fun onError(id: String?) {
                            handler.post {
                                programarEscucha(1000)
                            }
                        }

                        override fun onError(id: String?, errorCode: Int) {
                            handler.post {
                                programarEscucha(1000)
                            }
                        }
                    }
                )

                vozLista = true
            }
        }
    }

    private fun crearReconocedor() {
        if (!activo || !SpeechRecognizer.isRecognitionAvailable(this)) {
            programarEscucha(4000)
            return
        }

        try {
            recognizer?.destroy()
            recognizer = SpeechRecognizer.createSpeechRecognizer(this)

            recognizer?.setRecognitionListener(
                object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        escuchando = true
                    }

                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        escuchando = false
                    }

                    override fun onError(error: Int) {
                        escuchando = false
                        programarEscucha(1500)
                    }

                    override fun onResults(results: Bundle?) {
                        escuchando = false

                        val texto = results?.getStringArrayList(
                            SpeechRecognizer.RESULTS_RECOGNITION
                        )?.firstOrNull().orEmpty()

                        procesarVoz(normalizar(texto))
                    }

                    override fun onPartialResults(results: Bundle?) {}
                    override fun onEvent(type: Int, params: Bundle?) {}
                }
            )
        } catch (_: Exception) {
            recognizer = null
            programarEscucha(3000)
        }
    }

    
private fun procesarVoz(frase: String) {
    if (frase.isBlank()) {
        programarEscucha(500)
        return
    }

    val activadores = listOf(
        "hola yarvis",
        "ola yarvis",
        "yarvis"
    )

    val activador = activadores.firstOrNull {
        frase.contains(it)
    }

    if (activador != null) {
        esperandoOrden = true
        renovarSesion()

        val orden = frase.substringAfter(activador).trim()

        if (orden.isNotBlank()) {
            procesarOrden(orden)
        } else {
            hablar("Hola. Estoy aquí. ¿Qué necesitas?")
        }
        return
    }

    if (esperandoOrden) {
        renovarSesion()
        procesarOrden(frase)
    } else {
        programarEscucha(500)
    }
}

private fun renovarSesion() {
    handler.removeCallbacks(finSesionRunnable)
    handler.postDelayed(finSesionRunnable, 45000)
}

    private fun procesarOrden(texto: String) {
        val orden = normalizar(texto)

        if (
            orden.contains("gracias") ||
            orden.contains("eso es todo") ||
            orden.contains("terminar conversacion")
        ) {
            esperandoOrden = false
handler.removeCallbacks(finSesionRunnable)
hablar("De acuerdo. Aquí estaré.")
            return
        }

        if (
            orden.contains("que hora es") ||
            orden.contains("dime la hora") ||
            orden == "hora"
        ) {
            val hora = SimpleDateFormat(
                "h:mm a",
                Locale("es", "PE")
            ).format(Date())

            hablar("Son las $hora")
            return
        }

        val servicio = YarvisAccessibilityService.instancia

        if (orden.contains("toma foto") ||
            orden.contains("tomar foto") ||
            orden.contains("saca foto") ||
            orden.contains("captura")) {

            if (servicio?.ejecutarAccion(orden) == true) {
                hablar("Tomando la foto.")
            } else {
                hablar("No pude encontrar el botón de captura.")
            }
            return
        }

        if (orden.contains("sal de camara") ||
            orden.contains("salir de camara") ||
            orden.contains("cierra camara") ||
            orden == "volver" ||
            orden == "regresar") {

            if (servicio?.ejecutarAccion(orden) == true) {
                hablar("De acuerdo.")
            } else {
                hablar("No pude realizar esa acción.")
            }
            return
        }

        val prefijos = listOf(
            "abre ",
            "abrir ",
            "inicia ",
            "iniciar ",
            "ejecuta ",
            "ejecutar "
        )

        val prefijo = prefijos.firstOrNull {
            orden.startsWith(it)
        }

        if (prefijo != null) {
            val nombre = orden.removePrefix(prefijo)
                .removePrefix("la aplicacion ")
                .removePrefix("la app ")
                .trim()

            abrirAplicacion(nombre)
            return
        }

        consultarCerebro(orden)
    }

    private fun consultarCerebro(mensaje: String) {
    Thread {
        try {
            val url = URL("https://yarvis-cerebro.omaringa28.workers.dev/")
            val conexion = url.openConnection() as HttpURLConnection

            conexion.requestMethod = "POST"
            conexion.connectTimeout = 20000
            conexion.readTimeout = 60000
            conexion.doOutput = true
            conexion.setRequestProperty("Content-Type", "application/json")

            val datos = JSONObject()
            datos.put("message", mensaje)

            conexion.outputStream.use {
                it.write(datos.toString().toByteArray(Charsets.UTF_8))
            }

            val respuesta = conexion.inputStream.bufferedReader().use {
                it.readText()
            }

            val texto = JSONObject(respuesta).optString(
                "reply",
                "No recibí una respuesta."
            )

            handler.post {
                hablar(texto)
            }

            conexion.disconnect()

        } catch (e: Exception) {
            handler.post {
                hablar("No pude conectar con mi cerebro. Comprueba tu conexión a internet.")
            }
        }
    }.start()
    }
    private fun abrirAplicacion(nombre: String) {
        try {
            val buscada = normalizar(nombre)

            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }

            val aplicaciones = packageManager.queryIntentActivities(
                intent,
                0
            )

            val aplicacion = aplicaciones.firstOrNull {
                val etiqueta = normalizar(
                    it.loadLabel(packageManager).toString()
                )
                etiqueta == buscada ||
                    etiqueta.contains(buscada) ||
                    buscada.contains(etiqueta)
            }

            if (aplicacion == null) {
                hablar("No encuentro la aplicación $nombre.")
                return
            }

            val lanzamiento = packageManager.getLaunchIntentForPackage(
                aplicacion.activityInfo.packageName
            )

            if (lanzamiento == null) {
                hablar("No puedo abrir esa aplicación.")
                return
            }

            lanzamiento.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(lanzamiento)

            esperandoOrden = true
            hablar("Abriendo ${aplicacion.loadLabel(packageManager)}.")

        } catch (_: Exception) {
            hablar("Android no permitió abrir esa aplicación.")
        }
    }

    private fun hablar(mensaje: String) {
        if (!vozLista || tts == null) {
            programarEscucha(1000)
            return
        }

        tts?.speak(
            mensaje,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "yarvis_respuesta"
        )
    }

    private fun programarEscucha(retardo: Long) {
        if (!activo) return
        handler.removeCallbacks(escuchaRunnable)
        handler.postDelayed(escuchaRunnable, retardo)
    }

    private fun escuchar() {
        if (!activo || escuchando) return

        if (recognizer == null) {
            crearReconocedor()
            if (recognizer == null) return
        }

        val intent = Intent(
            RecognizerIntent.ACTION_RECOGNIZE_SPEECH
        ).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                "es-PE"
            )
            putExtra(
                RecognizerIntent.EXTRA_MAX_RESULTS,
                3
            )
            putExtra(
                RecognizerIntent.EXTRA_PARTIAL_RESULTS,
                true
            )
        }

        try {
            escuchando = true
            recognizer?.startListening(intent)
        } catch (_: Exception) {
            escuchando = false
            recognizer = null
            programarEscucha(2000)
        }
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        activo = false
        handler.removeCallbacksAndMessages(null)

        try {
            recognizer?.cancel()
            recognizer?.destroy()
        } catch (_: Exception) {}

        recognizer = null
        tts?.stop()
        tts?.shutdown()
        tts = null

        try {
            if (wakeLock?.isHeld == true) wakeLock?.release()
        } catch (_: Exception) {}

        wakeLock = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
