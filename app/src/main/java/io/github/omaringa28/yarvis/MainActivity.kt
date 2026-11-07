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

class MainActivity : Activity() {

    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.gravity = Gravity.CENTER
        layout.setPadding(30, 30, 30, 30)
        layout.setBackgroundColor(Color.rgb(2, 8, 23))

        val title = TextView(this)
        title.text = "YARVIS"
        title.textSize = 36f
        title.setTextColor(Color.CYAN)
        title.gravity = Gravity.CENTER

        status = TextView(this)
        status.text = "ASISTENTE PERSONAL"
        status.textSize = 17f
        status.setTextColor(Color.WHITE)
        status.gravity = Gravity.CENTER

        val start = Button(this)
        start.text = "ACTIVAR ESCUCHA"

        val stop = Button(this)
        stop.text = "DETENER YARVIS"

        start.setOnClickListener {
            pedirPermisos()
        }

        stop.setOnClickListener {
            stopService(Intent(this, YarvisService::class.java))
            status.text = "YARVIS EN ESPERA"
        }

        layout.addView(title)
        layout.addView(status)
        layout.addView(start)
        layout.addView(stop)

        setContentView(layout)
    }

    private fun pedirPermisos() {
        val permissions = mutableListOf(
            Manifest.permission.RECORD_AUDIO
        )

        if (Build.VERSION.SDK_INT >= 33) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val missing = permissions.filter {
            checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty()) {
            requestPermissions(missing.toTypedArray(), 100)
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

        if (requestCode == 100 &&
            grantResults.isNotEmpty() &&
            grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            activarServicio()
        } else {
            status.text = "SE NECESITA PERMISO DEL MICRÓFONO"
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
}
