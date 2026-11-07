package io.github.omaringa28.yarvis

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.content.pm.PackageManager
import android.Manifest
import android.os.Build
import android.graphics.Color
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

class MainActivity : Activity() {

    private lateinit var status: TextView
    private lateinit var answer: TextView
    private lateinit var message: EditText
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

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.gravity = Gravity.CENTER
        layout.setPadding(25, 35, 25, 35)
        layout.setBackgroundColor(Color.rgb(2, 8, 23))

        val title = TextView(this)
        title.text = "YARVIS"
        title.textSize = 38f
        title.setTextColor(Color.CYAN)
        title.gravity = Gravity.CENTER

        status = TextView(this)
        status.text = "GEMINI: CONEXIÓN PENDIENTE"
        status.textSize = 15f
        status.setTextColor(Color.WHITE)
        status.gravity = Gravity.CENTER

        message = EditText(this)
        message.hint = "Escribe tu pregunta..."
        message.setTextColor(Color.WHITE)
        message.setHintTextColor(Color.GRAY)
        message.setSingleLine(false)

        val send = Button(this)
        send.text = "ENVIAR A YARVIS"

        val voice = Button(this)
        voice.text = "HABLAR CON YARVIS"

        val start = Button(this)
        start.text = "ACTIVAR SERVICIO"

        val stop = Button(this)
        stop.text = "DETENER YARVIS"

        answer = TextView(this)
        answer.text = "Hola. Soy YARVIS. Estoy preparado."
        answer.textSize = 17f
        answer.setTextColor(Color.CYAN)
        answer.setPadding(10, 25, 10, 25)

        send.setOnClickListener {
            val text = message.text.toString().trim()
            if (text.isNotEmpty()) {
                consultarGemini(text)
            }
        }

        voice.setOnClickListener {
            comprobarMicrofono()
        }

        start.setOnClickListener {
            activarServicio()
        }

        stop.setOnClickListener {
            stopService(Intent(this, YarvisService::class.java))
            status.text = "YARVIS EN ESPERA"
        }

        layout.addView(title)
        layout.addView(status)
        layout.addView(answer)
        layout.addView(message)
        layout.addView(send)
        layout.addView(voice)
        layout.addView(start)
        layout.addView(stop)

        setContentView(layout)
    }

    private fun consultarGemini(texto: String) {
        status.text = "YARVIS ESTÁ PENSANDO..."
        answer.text = "Conectando con mi cerebro..."

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
                    answer.text = "No pude conectar con Gemini: ${e.message}"
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
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE,
                    "es-PE"
                )
                putExtra(
                    RecognizerIntent.EXTRA_PROMPT,
                    "Habla con YARVIS"
                )
            }

            status.text = "TE ESTOY ESCUCHANDO..."
            startActivityForResult(intent, voiceRequest)

        } catch (e: Exception) {
            status.text = "NO SE PUDO ABRIR EL MICROFONO"
        }
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == voiceRequest && resultCode == RESULT_OK) {
            val phrases = data?.getStringArrayListExtra(
                RecognizerIntent.EXTRA_RESULTS
            )

            val phrase = phrases?.firstOrNull()

            if (!phrase.isNullOrBlank()) {
                message.setText(phrase)
                consultarGemini(phrase)
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

        if (Build.VERSION.SDK_INT >= 26) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }

        status.text = "SERVICIO YARVIS INICIADO"
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}
