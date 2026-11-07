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
import android.widget.LinearLayout
import android.widget.TextView
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import java.util.Locale

class MainActivity : Activity() {

    private lateinit var status: TextView
    private var tts: TextToSpeech? = null
    private val permisoMicrofono = 100
    private val pruebaVoz = 200

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tts = TextToSpeech(this) { result ->
            if (result == TextToSpeech.SUCCESS) {
                tts?.language = Locale("es", "PE")
                tts?.setSpeechRate(0.88f)
                tts?.setPitch(0.75f)
            }
        }

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.gravity = Gravity.CENTER
        layout.setPadding(30, 30, 30, 30)
        layout.setBackgroundColor(Color.rgb(2, 8, 23))

        val title = TextView(this)
        title.text = "YARVIS"
        title.textSize = 38f
        title.setTextColor(Color.CYAN)
        title.gravity = Gravity.CENTER

        status = TextView(this)
        status.text = "ASISTENTE PERSONAL"
        status.textSize = 17f
        status.setTextColor(Color.WHITE)
        status.gravity = Gravity.CENTER

        val probar = Button(this)
        probar.text = "PROBAR RECONOCIMIENTO DE VOZ"

        val start = Button(this)
        start.text = "ACTIVAR ESCUCHA"

        val stop = Button(this)
        stop.text = "DETENER YARVIS"

        probar.setOnClickListener {
            comprobarPermiso()
        }

        start.setOnClickListener {
            pedirPermisosServicio()
        }

        stop.setOnClickListener {
            stopService(Intent(this, YarvisService::class.java))
            status.text = "YARVIS EN ESPERA"
        }

        layout.addView(title)
        layout.addView(status)
        layout.addView(probar)
        layout.addView(start)
        layout.addView(stop)

        setContentView(layout)
    }

    private fun comprobarPermiso() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                arrayOf(Manifest.permission.RECORD_AUDIO),
                permisoMicrofono
            )
        } else {
            abrirReconocimiento()
        }
    }

    private fun abrirReconocimiento() {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-PE")
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Habla con YARVIS")

            status.text = "ESCUCHANDO..."
            startActivityForResult(intent, pruebaVoz)

        } catch (e: Exception) {
            status.text = "NO SE ENCONTRO RECONOCIMIENTO DE VOZ"
        }
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == pruebaVoz) {
            if (resultCode == RESULT_OK) {
                val resultados = data?.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS
                )

                val frase = resultados?.firstOrNull() ?: ""

                status.text = "TE ESCUCHE: $frase"

                if (frase.lowercase(Locale.ROOT).contains("hola yarvis") ||
                    frase.lowercase(Locale.ROOT).contains("hola jarvis")) {
                    hablar("Hola. Estoy aquí. ¿En qué puedo ayudarte?")
                } else {
                    hablar("Te he escuchado. Dijiste: $frase")
                }
            } else {
                status.text = "NO SE RECIBIO NINGUNA FRASE"
            }
        }
    }

    private fun hablar(mensaje: String) {
        tts?.speak(
            mensaje,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "respuesta_yarvis"
        )
    }

    private fun pedirPermisosServicio() {
        val permisos = mutableListOf(Manifest.permission.RECORD_AUDIO)

        if (Build.VERSION.SDK_INT >= 33) {
            permisos.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val faltantes = permisos.filter {
            checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
        }

        if (faltantes.isNotEmpty()) {
            requestPermissions(faltantes.toTypedArray(), permisoMicrofono)
        } else {
            activarServicio()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == permisoMicrofono &&
            grantResults.isNotEmpty() &&
            grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            if (permissions.contains(Manifest.permission.POST_NOTIFICATIONS) ||
                Build.VERSION.SDK_INT < 33) {
                activarServicio()
            } else {
                abrirReconocimiento()
            }
        } else {
            status.text = "SE NECESITAN LOS PERMISOS"
        }
    }

    private fun activarServicio() {
        val intent = Intent(this, YarvisService::class.java)
        if (Build.VERSION.SDK_INT >= 26) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        status.text = "INICIANDO YARVIS..."
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}
