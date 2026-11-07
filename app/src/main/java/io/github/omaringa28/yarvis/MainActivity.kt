
package io.github.omaringa28.yarvis

import android.app.Activity
import android.os.Bundle
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.view.View
import android.view.Gravity
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tts = TextToSpeech(this) { result ->
            if (result == TextToSpeech.SUCCESS) {
                tts?.language = Locale("es", "ES")
                tts?.setSpeechRate(0.88f)
                tts?.setPitch(0.85f)
            }
        }

        val fondo = Color.rgb(2, 8, 23)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(24, 25, 24, 25)
            setBackgroundColor(fondo)
        }

        val title = TextView(this).apply {
            text = "Y A R V I S"
            textSize = 31f
            setTextColor(Color.CYAN)
            gravity = Gravity.CENTER
            letterSpacing = 0.18f
        }

        val subtitle = TextView(this).apply {
            text = "ASISTENTE PERSONAL INTELIGENTE"
            textSize = 11f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
        }

        orb = OrbView(this)

        status = TextView(this).apply {
            text = "SISTEMA EN ESPERA"
            textSize = 14f
            setTextColor(Color.CYAN)
            gravity = Gravity.CENTER
        }

        answer = TextView(this).apply {
            text = "Hola. Soy YARVIS. Estoy preparado."
            textSize = 16f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(8, 15, 8, 15)
        }

        message = EditText(this).apply {
            hint = "Escribe tu pregunta..."
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            setSingleLine(false)
        }

        val send = Button(this).apply {
            text = "ENVIAR A YARVIS"
            setOnClickListener {
                val text = message.text.toString().trim()
                if (text.isNotEmpty()) consultarGemini(text)
            }
        }

        val voice = Button(this).apply {
            text = "🎙 HABLAR CON YARVIS"
            setOnClickListener { comprobarMicrofono() }
        }

        val start = Button(this).apply {
            text = "ACTIVAR SERVICIO"
            setOnClickListener { activarServicio() }
        }

        val stop = Button(this).apply {
            text = "DETENER SERVICIO"
            setOnClickListener {
                stopService(Intent(this@MainActivity, YarvisService::class.java))
                status.text = "YARVIS EN ESPERA"
                orb.setMode("idle")
            }
        }

        layout.addView(title)
        layout.addView(subtitle)

        layout.addView(
            orb,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(260)
            )
        )

        layout.addView(status)
        layout.addView(answer)
        layout.addView(message)
        layout.addView(send)
        layout.addView(voice)
        layout.addView(start)
        layout.addView(stop)

        setContentView(layout)

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
                        status.text = "YARVIS EN LÍNEA"
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

    private fun consultarGemini(texto: String) {
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
                    throw Exception("El servidor no devolvió una respuesta.")
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
        val intent = Intent(this, YarvisService::class.java)

        try {
            startForegroundService(intent)
            status.text = "SERVICIO YARVIS INICIADO"
            orb.setMode("listening")
        } catch (e: Exception) {
            status.text = "NO SE PUDO ACTIVAR EL SERVICIO"
        }
    }

    override fun onDestroy() {
        orb.setMode("idle")
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}

class OrbView(context: android.content.Context) : View(context) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var progress = 0f
    private var mode = "idle"

    private val animator = android.animation.ValueAnimator
        .ofFloat(0f, 1f).apply {
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
        val base = min(width, height) * 0.28f

        val pulse = (sin(progress * Math.PI * 2) + 1) / 2
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

        // Resplandor exterior
        paint.shader = RadialGradient(
            cx, cy, base * 2.1f,
            intArrayOf(
                Color.argb(100, 0, 160, 255),
                Color.argb(25, 0, 100, 255),
                Color.TRANSPARENT
            ),
            null,
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, base * 2.1f, paint)
        paint.shader = null

        // Ondas expansivas
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

        // Anillos
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

        // Esfera central
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

        // Texto central
        paint.color = Color.WHITE
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = base * 0.23f
        paint.typeface = Typeface.create(
            "sans-serif-medium", Typeface.BOLD
        )

        canvas.drawText("YARVIS", cx, cy + base * 0.08f, paint)

        paint.style = Paint.Style.FILL
    }

    override fun onDetachedFromWindow() {
        animator.cancel()
        super.onDetachedFromWindow()
    }
}
