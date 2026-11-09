
package io.github.omaringa28.yarvis

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class YarvisAccessibilityService : AccessibilityService() {

    companion object {
        @Volatile
        var instancia: YarvisAccessibilityService? = null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instancia = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}

    
fun ejecutarAccion(orden: String): Boolean {
    val comando = orden.lowercase()

    return when {
        // REGRESAR A LA PANTALLA DE INICIO
        comando.contains("regresar al inicio") ||
        comando.contains("regresa a inicio") ||
        comando.contains("volver al inicio") ||
        comando.contains("vuelve al inicio") ||
        comando.contains("ir al inicio") ||
        comando.contains("pantalla principal") ||
        comando.contains("pantalla de inicio") ||
        comando == "inicio" ||
        comando == "home" -> {
            performGlobalAction(GLOBAL_ACTION_HOME)
        }

        // TOMAR FOTOGRAFÍAS
        comando.contains("toma foto") ||
        comando.contains("tomar foto") ||
        comando.contains("captura") ||
        comando.contains("saca foto") -> {
            pulsarElemento(
                listOf(
                    "tomar foto",
                    "take picture",
                    "capturar",
                    "obturador",
                    "shutter"
                )
            )
        }

        // SALIR DE LA CÁMARA
        comando.contains("salir de camara") ||
        comando.contains("salir de cámara") ||
        comando.contains("cierra camara") ||
        comando.contains("cierra cámara") -> {
            performGlobalAction(GLOBAL_ACTION_HOME)
        }

        // REGRESAR A LA PANTALLA ANTERIOR
        comando.contains("volver") ||
        comando.contains("regresar") -> {
            performGlobalAction(GLOBAL_ACTION_BACK)
        }

        // DESLIZAMIENTOS
        comando.contains("desliza hacia arriba") ->
            deslizarPantalla("arriba")

        comando.contains("desliza hacia abajo") ->
            deslizarPantalla("abajo")

        comando.contains("desliza hacia la izquierda") ->
            deslizarPantalla("izquierda")

        comando.contains("desliza hacia la derecha") ->
            deslizarPantalla("derecha")

        else -> false
    }
}


    fun pulsarPorNombre(nombre: String): Boolean {
    val buscado = nombre.lowercase()

    val alias = when {
        buscado.contains("galeria") ||
        buscado.contains("fotos") ||
        buscado.contains("imagenes") ->
            listOf("galería", "galeria", "fotos", "imágenes", "imagenes")

        buscado.contains("camara") ->
            listOf("cámara", "camara", "camera")

        buscado.contains("ubicacion") ->
            listOf("ubicación", "ubicacion", "location")

        buscado.contains("contactos") ->
            listOf("contactos", "contacts")

        buscado.contains("documento") ->
            listOf("documento", "documentos", "documents")

        else -> listOf(buscado)
    }

    return pulsarElemento(alias)
    }

    private fun pulsarElemento(nombres: List<String>): Boolean {
        val raiz = rootInActiveWindow ?: return false
        return buscarYpulsar(raiz, nombres)
    }

    private fun buscarYpulsar(
        nodo: AccessibilityNodeInfo,
        nombres: List<String>
    ): Boolean {
        val texto = (
            nodo.text?.toString() + " " +
            nodo.contentDescription?.toString()
        ).lowercase()

        if (nombres.any { texto.contains(it) } && nodo.isEnabled) {
            if (nodo.isClickable) {
                return nodo.performAction(
                    AccessibilityNodeInfo.ACTION_CLICK
                )
            }

            val padre = nodo.parent
            if (padre != null && padre.isClickable && padre.isEnabled) {
                return padre.performAction(
                    AccessibilityNodeInfo.ACTION_CLICK
                )
            }
        }

        for (i in 0 until nodo.childCount) {
            val hijo = nodo.getChild(i) ?: continue
            if (buscarYpulsar(hijo, nombres)) return true
        }

        return false
    }

    fun escribirTexto(texto: String): Boolean {
        val raiz = rootInActiveWindow ?: return false

        val enfocado = raiz.findFocus(
            AccessibilityNodeInfo.FOCUS_INPUT
        )

        val campo = if (enfocado != null && enfocado.isEditable) {
            enfocado
        } else {
            buscarCampoEditable(raiz)
        } ?: return false

        val argumentos = Bundle().apply {
            putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                texto
            )
        }

        return campo.performAction(
            AccessibilityNodeInfo.ACTION_SET_TEXT,
            argumentos
        )
    }

    private fun buscarCampoEditable(
        nodo: AccessibilityNodeInfo
    ): AccessibilityNodeInfo? {
        if (nodo.isEditable && nodo.isEnabled) {
            return nodo
        }

        for (i in 0 until nodo.childCount) {
            val hijo = nodo.getChild(i) ?: continue
            val encontrado = buscarCampoEditable(hijo)
            if (encontrado != null) return encontrado
        }

                return null
    }

    // LEER ELEMENTOS VISIBLES DE CUALQUIER APLICACIÓN
    fun leerPantalla(): String {
        val raiz = rootInActiveWindow
            ?: return "No puedo leer la pantalla actual."

        val elementos = linkedSetOf<String>()

        fun recorrer(nodo: AccessibilityNodeInfo) {
            if (elementos.size >= 100) return

            val texto = nodo.text?.toString()?.trim()
            val descripcion = nodo.contentDescription?.toString()?.trim()

            if (!texto.isNullOrBlank()) {
                elementos.add(texto)
            }

            if (!descripcion.isNullOrBlank()) {
                elementos.add(descripcion)
            }

            for (i in 0 until nodo.childCount) {
                if (elementos.size >= 100) break

                val hijo = nodo.getChild(i) ?: continue
                recorrer(hijo)
            }
        }

        recorrer(raiz)

        return if (elementos.isEmpty()) {
            "No encuentro textos ni botones visibles."
        } else {
            elementos.joinToString("\n").take(5000)
        }
    }

    fun deslizarPantalla(direccion: String): Boolean {
        val ancho = resources.displayMetrics.widthPixels.toFloat()
        val alto = resources.displayMetrics.heightPixels.toFloat()

        val centroX = ancho / 2f
        val centroY = alto / 2f

        val ruta = Path()

        when (direccion.lowercase()) {
            "arriba" -> {
                ruta.moveTo(centroX, alto * 0.80f)
                ruta.lineTo(centroX, alto * 0.25f)
            }

            "abajo" -> {
                ruta.moveTo(centroX, alto * 0.25f)
                ruta.lineTo(centroX, alto * 0.80f)
            }

            "izquierda" -> {
                ruta.moveTo(ancho * 0.85f, centroY)
                ruta.lineTo(ancho * 0.15f, centroY)
            }

            "derecha" -> {
                ruta.moveTo(ancho * 0.15f, centroY)
                ruta.lineTo(ancho * 0.85f, centroY)
            }

            else -> return false
        }

        val gesto = GestureDescription.Builder()
            .addStroke(
                GestureDescription.StrokeDescription(
                    ruta,
                    0,
                    350
                )
            )
            .build()

        return dispatchGesture(gesto, null, null)
    }

    override fun onDestroy() {
        if (instancia === this) instancia = null
        super.onDestroy()
    }
}
