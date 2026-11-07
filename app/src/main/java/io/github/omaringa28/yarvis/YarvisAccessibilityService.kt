
package io.github.omaringa28.yarvis

import android.accessibilityservice.AccessibilityService
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
        return when {
            orden.contains("toma foto") ||
            orden.contains("tomar foto") ||
            orden.contains("captura") ||
            orden.contains("saca foto") -> {
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

            orden.contains("sal de camara") ||
            orden.contains("salir de camara") ||
            orden.contains("cierra camara") -> {
                performGlobalAction(GLOBAL_ACTION_HOME)
            }

            orden.contains("volver") ||
            orden.contains("regresar") -> {
                performGlobalAction(GLOBAL_ACTION_BACK)
            }

            else -> false
        }
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

        if (
            nombres.any { texto.contains(it) } &&
            nodo.isEnabled
        ) {
            if (nodo.isClickable) {
                return nodo.performAction(
                    AccessibilityNodeInfo.ACTION_CLICK
                )
            }

            val padre = nodo.parent
            if (padre != null && padre.isClickable) {
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

    override fun onDestroy() {
        if (instancia === this) instancia = null
        super.onDestroy()
    }
}
