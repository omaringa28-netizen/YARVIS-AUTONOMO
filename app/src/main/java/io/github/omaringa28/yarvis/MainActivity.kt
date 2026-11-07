
package io.github.omaringa28.yarvis

import android.media.AudioManager
import android.view.KeyEvent
import java.text.Normalizer
import android.app.Activity
import android.os.Bundle
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.*
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.math.*

class MainActivity : Activity() {

    private lateinit var status: TextView
    private lateinit var answer: TextView
    private lateinit var message: EditText
    private lateinit var orb: OrbView
    private var tts: TextToSpeech? = null

    private val workerUrl =
        "https://yarvis-cerebro.omaringa28.workers.dev/"

    private val voiceRequest = 200
    private val permissionRequest = 100

    private val backgroundColor = Color.rgb(3, 8, 23)
    private val cyan = Color.rgb(0, 220, 255)
    private val blue = Color.rgb(0, 115, 255)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = backgroundColor
        window.navigationBarColor = backgroundColor

        tts = TextToSpeech(this) { result ->
            if (result == TextToSpeech.SUCCESS) {
                tts?.language = Locale("es", "ES")
                tts?.setSpeechRate(0.88f)
                tts?.setPitch(0.85f)
            }
        }

        construirInterfaz()

        tts?.setOnUtteranceProgressListener(
            object : UtteranceProgressListener() {
                override fun onStart(id: String?) {
                    runOnUiThread {
                        orb.setMode("speaking")
                        status.text = "YARVIS ESTÁ RESPONDIENDO"
                    }
                }

                override fun onDone(id: String?) {
                    runOnUiThread {
                        orb.setMode("idle")
                        status.text = "SISTEMA EN LÍNEA"
                    }
                }

                override fun onError(id: String?) {
                    runOnUiThread { orb.setMode("idle") }
                }
            }
        )
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun fondo(
        color1: Int,
        color2: Int,
        radius: Int = 22,
        strokeColor: Int = Color.TRANSPARENT
    ): GradientDrawable {
        return GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(color1, color2)
        ).apply {
            cornerRadius = dp(radius).toFloat()
            if (strokeColor != Color.TRANSPARENT) {
                setStroke(dp(1), strokeColor)
            }
        }
    }

    private fun texto(
        contenido: String,
        size: Float,
        color: Int,
        bold: Boolean = false
    ): TextView {
        return TextView(this).apply {
            text = contenido
            textSize = size
            setTextColor(color)
            gravity = Gravity.CENTER
            if (bold) {
                typeface = Typeface.create(
                    "sans-serif-medium", Typeface.BOLD
                )
            }
        }
    }

    private fun boton(
        contenido: String,
        color1: Int,
        color2: Int,
        borde: Int = Color.TRANSPARENT
    ): Button {
        return Button(this).apply {
            text = contenido
            textSize = 13f
            isAllCaps = false
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            background = fondo(color1, color2, 18, borde)
            backgroundTintList = null
            elevation = dp(5).toFloat()
            minHeight = dp(52)
            setPadding(dp(10), dp(8), dp(10), dp(8))
        }
    }

    private fun construirInterfaz() {

        val scroll = ScrollView(this).apply {
            setBackgroundColor(backgroundColor)
            isFillViewport = true
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(20), dp(25), dp(20), dp(30))
        }

        // ENCABEZADO

        val marca = texto("Y A R V I S", 29f, cyan, true)
        marca.letterSpacing = 0.22f

        layout.addView(
            marca,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(48)
            )
        )

        val subtitulo = texto(
            "ASISTENTE PERSONAL INTELIGENTE",
            10f,
            Color.LTGRAY
        )
        subtitulo.letterSpacing = 0.16f

        layout.addView(subtitulo)

        val linea = View(this).apply {
            setBackgroundColor(Color.rgb(0, 115, 180))
        }

        val lineaParams = LinearLayout.LayoutParams(
            dp(145), dp(1)
        ).apply {
            topMargin = dp(17)
            bottomMargin = dp(18)
        }
        layout.addView(linea, lineaParams)

        // ESTADO DEL SISTEMA

        val estadoPanel = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            background = fondo(
                Color.rgb(8, 30, 53),
                Color.rgb(5, 17, 35),
                30,
                Color.rgb(0, 110, 170)
            )
            setPadding(dp(18), dp(10), dp(18), dp(10))
        }

        val punto = texto("●", 12f, Color.GREEN)
        val estadoTexto = texto(
            "GEMINI  •  SISTEMA DISPONIBLE",
            11f,
            Color.WHITE,
            true
        )

        estadoPanel.addView(punto)
        estadoPanel.addView(
            estadoTexto,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { leftMargin = dp(8) }
        )

        layout.addView(estadoPanel)

        // ESFERA HOLOGRÁFICA

        orb = OrbView(this)

        layout.addView(
            orb,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(285)
            ).apply {
                topMargin = dp(12)
                bottomMargin = dp(5)
            }
        )

        status = texto(
            "SISTEMA EN ESPERA",
            12f,
            cyan,
            true
        )
        status.letterSpacing = 0.12f

        layout.addView(
            status,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(35)
            )
        )

        // PANEL DE RESPUESTA

        val answerPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = fondo(
                Color.rgb(9, 24, 45),
                Color.rgb(5, 13, 29),
                20,
                Color.rgb(0, 90, 145)
            )
            setPadding(dp(17), dp(15), dp(17), dp(15))
        }

        val answerTitle = texto(
            "NÚCLEO DE RESPUESTA",
            10f,
            cyan,
            true
        )
        answerTitle.gravity = Gravity.START

        answer = texto(
            "Hola. Soy YARVIS. Estoy preparado para ayudarte.",
            15f,
            Color.WHITE
        )
        answer.gravity = Gravity.START
        answer.setLineSpacing(dp(4).toFloat(), 1f)

        answerPanel.addView(answerTitle)

        answerPanel.addView(
            answer,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(10) }
        )

        layout.addView(
            answerPanel,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(12)
                bottomMargin = dp(20)
            }
        )

        // BARRA PARA ESCRIBIR

        val inputTitle = texto(
            "ESCRIBE TU MENSAJE",
            10f,
            Color.LTGRAY,
            true
        )
        inputTitle.gravity = Gravity.START

        layout.addView(
            inputTitle,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(25)
            )
        )

        val inputPanel = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = fondo(
                Color.rgb(12, 28, 49),
                Color.rgb(7, 18, 34),
                18,
                Color.rgb(0, 135, 190)
            )
            setPadding(dp(12), dp(4), dp(5), dp(4))
        }

        message = EditText(this).apply {
            hint = "Pregunta lo que quieras..."
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            textSize = 14f
            setPadding(dp(8), dp(8), dp(8), dp(8))
            background = null
            minHeight = dp(52)
        }

        val send = boton(
            "➤",
            Color.rgb(0, 125, 230),
            Color.rgb(0, 210, 255)
        )

        inputPanel.addView(
            message,
            LinearLayout.LayoutParams(0, dp(55), 1f)
        )

        inputPanel.addView(
            send,
            LinearLayout.LayoutParams(dp(58), dp(48))
        )

        send.setOnClickListener {
            val question = message.text.toString().trim()
            if (question.isNotEmpty()) {
                consultarGemini(question)
            }
        }

        layout.addView(
            inputPanel,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(16) }
        )

        // BOTONES DE VOZ Y SERVICIO

        val voice = boton(
            "🎙  HABLAR CON YARVIS",
            Color.rgb(0, 110, 205),
            Color.rgb(0, 190, 230)
        )

        val start = boton(
            "◉  ACTIVAR SERVICIO",
            Color.rgb(10, 48, 78),
            Color.rgb(5, 25, 48),
            Color.rgb(0, 170, 210)
        )

        val stop = boton(
            "■  DETENER SERVICIO",
            Color.rgb(55, 23, 40),
            Color.rgb(35, 12, 27),
            Color.rgb(160, 55, 90)
        )

        voice.setOnClickListener {
            comprobarMicrofono()
        }

        start.setOnClickListener {
            activarServicio()
        }

        stop.setOnClickListener {
            stopService(Intent(this, YarvisService::class.java))
            status.text = "YARVIS EN ESPERA"
            orb.setMode("idle")
        }

        layout.addView(
            voice,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(55)
            ).apply { bottomMargin = dp(10) }
        )

        val controles = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        controles.addView(
            start,
            LinearLayout.LayoutParams(0, dp(53), 1f).apply {
                rightMargin = dp(5)
            }
        )

        controles.addView(
            stop,
            LinearLayout.LayoutParams(0, dp(53), 1f).apply {
                leftMargin = dp(5)
            }
        )

        layout.addView(controles)

        val pie = texto(
            "YARVIS AUTÓNOMO  •  INTELIGENCIA ARTIFICIAL",
            9f,
            Color.rgb(90, 130, 165)
        )

        layout.addView(
            pie,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(48)
            )
        )

        scroll.addView(layout)
        setContentView(scroll)
    }

    private fun consultarGemini(texto: String) {
        if (ejecutarComandoLocal(texto)) return
        status.text = "YARVIS ESTÁ PENSANDO..."
        answer.text = "Analizando tu mensaje..."
        orb.setMode("thinking")

        Thread {
            var connection: HttpURLConnection? = null

            try {
                connection = URL(workerUrl)
                    .openConnection() as HttpURLConnection

                connection.requestMethod = "POST"
                connection.connectTimeout = 20000
                connection.readTimeout = 60000
                connection.doOutput = true
                connection.setRequestProperty(
                    "Content-Type",
                    "application/json; charset=UTF-8"
                )

                val body = JSONObject()
                    .put("message", texto)
                    .toString()

                connection.outputStream.use {
                    it.write(body.toByteArray(Charsets.UTF_8))
                }

                val code = connection.responseCode
                val stream = if (code in 200..299) {
                    connection.inputStream
                } else {
                    connection.errorStream
                }

                val responseText = stream.bufferedReader().use {
                    it.readText()
                }

                val json = JSONObject(responseText)

                if (code !in 200..299) {
                    throw Exception(
                        json.optString("error", "Error HTTP $code")
                    )
                }

                val respuesta = json.optString("reply", "")

                if (respuesta.isBlank()) {
                    throw Exception("No recibí una respuesta.")
                }

                runOnUiThread {
                    answer.text = respuesta
                    status.text = "YARVIS EN LÍNEA"

                    tts?.speak(
                        respuesta,
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        "respuesta_gemini"
                    )

                    message.text.clear()
                }

            } catch (e: Exception) {
                runOnUiThread {
                    status.text = "ERROR DE CONEXIÓN"
                    answer.text = "No pude conectar: ${e.message}"
                    orb.setMode("idle")
                }
            } finally {
                connection?.disconnect()
            }
        }.start()
    }

    
private fun ejecutarComandoLocal(texto: String): Boolean {

    val orden = Normalizer.normalize(
        texto.lowercase(Locale.ROOT),
        Normalizer.Form.NFD
    ).replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")

    when {
        orden.contains("abre whatsapp") ->
            abrirAplicacion("com.whatsapp", "WhatsApp")

        orden.contains("abre youtube") ->
            abrirAplicacion("com.google.android.youtube", "YouTube")

        orden.contains("abre spotify") ->
            abrirAplicacion("com.spotify.music", "Spotify")

        orden.contains("abre chrome") ->
            abrirAplicacion("com.android.chrome", "Chrome")

        orden.contains("sube el volumen") ||
        orden.contains("aumenta el volumen") -> {
            cambiarVolumen(AudioManager.ADJUST_RAISE)
            responderLocal("Subiendo el volumen.")
        }

        orden.contains("baja el volumen") ||
        orden.contains("disminuye el volumen") -> {
            cambiarVolumen(AudioManager.ADJUST_LOWER)
            responderLocal("Bajando el volumen.")
        }

        orden.contains("pausa la musica") ||
        orden.contains("reanuda la musica") ||
        orden.contains("continua la musica") ||
        orden.contains("reproduce musica") -> {
            controlarMusica()
            responderLocal("Enviando la orden al reproductor.")
        }

        else -> return false
    }

    return true
}

private fun abrirAplicacion(paquete: String, nombre: String) {
    val intent = packageManager.getLaunchIntentForPackage(paquete)

    if (intent != null) {
        startActivity(intent)
        responderLocal("Abriendo $nombre.")
    } else {
        responderLocal("No encuentro $nombre instalado.")
    }
}

private fun cambiarVolumen(direccion: Int) {
    val audio = getSystemService(AUDIO_SERVICE) as AudioManager

    audio.adjustStreamVolume(
        AudioManager.STREAM_MUSIC,
        direccion,
        AudioManager.FLAG_SHOW_UI
    )
}

private fun controlarMusica() {
    val audio = getSystemService(AUDIO_SERVICE) as AudioManager

    audio.dispatchMediaKeyEvent(
        KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
    )

    audio.dispatchMediaKeyEvent(
        KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
    )
}

private fun responderLocal(mensaje: String) {
    answer.text = mensaje
    status.text = "ORDEN EJECUTADA"
    orb.setMode("speaking")

    tts?.speak(
        mensaje,
        TextToSpeech.QUEUE_FLUSH,
        null,
        "orden_local"
    )
}

    private fun comprobarMicrofono() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                arrayOf(Manifest.permission.RECORD_AUDIO),
                permissionRequest
            )
        } else {
            abrirReconocimiento()
        }
    }

    private fun abrirReconocimiento() {
        try {
            val intent = Intent(
                RecognizerIntent.ACTION_RECOGNIZE_SPEECH
            ).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-PE")
                putExtra(
                    RecognizerIntent.EXTRA_PROMPT,
                    "Habla con YARVIS"
                )
            }

            status.text = "TE ESTOY ESCUCHANDO..."
            orb.setMode("listening")
            startActivityForResult(intent, voiceRequest)

        } catch (e: Exception) {
            status.text = "NO SE PUDO ABRIR EL MICRÓFONO"
            orb.setMode("idle")
        }
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == voiceRequest) {
            if (resultCode == RESULT_OK) {
                val phrases = data?.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS
                )

                val phrase = phrases?.firstOrNull()

                if (!phrase.isNullOrBlank()) {
                    message.setText(phrase)
                    consultarGemini(phrase)
                } else {
                    orb.setMode("idle")
                }
            } else {
                orb.setMode("idle")
                status.text = "YARVIS EN ESPERA"
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(
            requestCode, permissions, grantResults
        )

        if (requestCode == permissionRequest &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            abrirReconocimiento()
        }
    }

    private fun activarServicio() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            comprobarMicrofono()
            return
        }

        try {
            startForegroundService(
                Intent(this, YarvisService::class.java)
            )
            status.text = "SERVICIO YARVIS INICIADO"
            orb.setMode("listening")
        } catch (e: Exception) {
            status.text = "NO SE PUDO ACTIVAR EL SERVICIO"
        }
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}

class OrbView(context: android.content.Context) : View(context) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var progress = 0f
    private var mode = "idle"

    private val animator =
        android.animation.ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 2200
            repeatCount = android.animation.ValueAnimator.INFINITE
            addUpdateListener {
                progress = it.animatedValue as Float
                invalidate()
            }
        }

    init {
        animator.start()
    }

    fun setMode(newMode: String) {
        mode = newMode
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val cx = width / 2f
        val cy = height / 2f
        val base = min(width, height) * 0.27f

        val pulse =
            ((sin(progress * Math.PI * 2) + 1) / 2).toFloat()

        val scale = when (mode) {
            "listening" -> 1.08f + pulse * 0.08f
            "thinking" -> 0.95f + pulse * 0.12f
            "speaking" -> 1.02f + pulse * 0.10f
            else -> 1f + pulse * 0.025f
        }

        val color = when (mode) {
            "listening" -> Color.rgb(0, 220, 255)
            "thinking" -> Color.rgb(80, 120, 255)
            "speaking" -> Color.rgb(0, 255, 210)
            else -> Color.rgb(0, 150, 255)
        }

        // RESPLANDOR

        paint.shader = RadialGradient(
            cx, cy, base * 2.2f,
            intArrayOf(
                Color.argb(100, 0, 160, 255),
                Color.argb(25, 0, 100, 255),
                Color.TRANSPARENT
            ),
            null,
            Shader.TileMode.CLAMP
        )

        canvas.drawCircle(cx, cy, base * 2.2f, paint)
        paint.shader = null

        // ONDAS LUMINOSAS

        for (i in 0..2) {
            val wave = (progress + i / 3f) % 1f
            val radius = base * (1.15f + wave * 0.9f)
            val alpha = (150 * (1f - wave)).toInt()

            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2.5f
            paint.color = Color.argb(
                alpha.coerceIn(0, 255), 0, 190, 255
            )

            canvas.drawCircle(cx, cy, radius, paint)
        }

        // ANILLOS

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        paint.color = color

        canvas.drawCircle(cx, cy, base * 1.08f * scale, paint)

        paint.strokeWidth = 4f

        val rect = RectF(
            cx - base * 1.35f,
            cy - base * 1.35f,
            cx + base * 1.35f,
            cy + base * 1.35f
        )

        canvas.drawArc(
            rect,
            progress * 360,
            95f,
            false,
            paint
        )

        // ESFERA CENTRAL

        paint.style = Paint.Style.FILL

        paint.shader = RadialGradient(
            cx - base * 0.25f,
            cy - base * 0.3f,
            base * 1.25f,
            intArrayOf(
                Color.WHITE,
                color,
                Color.rgb(0, 35, 100),
                Color.rgb(0, 5, 25)
            ),
            floatArrayOf(0f, 0.18f, 0.65f, 1f),
            Shader.TileMode.CLAMP
        )

        canvas.drawCircle(cx, cy, base * scale, paint)
        paint.shader = null

        // NOMBRE CENTRAL

        paint.color = Color.WHITE
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = base * 0.23f
        paint.typeface = Typeface.create(
            "sans-serif-medium", Typeface.BOLD
        )

        canvas.drawText(
            "YARVIS",
            cx,
            cy + base * 0.08f,
            paint
        )

        paint.style = Paint.Style.FILL
    }

    override fun onDetachedFromWindow() {
        animator.cancel()
        super.onDetachedFromWindow()
    }
}
