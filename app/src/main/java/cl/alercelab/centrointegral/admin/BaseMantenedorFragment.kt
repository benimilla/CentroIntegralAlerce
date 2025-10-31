package cl.alercelab.centrointegral.admin

import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.*

/**
 * Clase base para todos los mantenedores del módulo Admin.
 * Proporciona manejo centralizado de corrutinas, errores y notificaciones.
 */
abstract class BaseMantenedorFragment : Fragment() {

    // Job supervisor para evitar que un fallo en una corrutina cancele las demás
    private val supervisorJob = SupervisorJob()
    private val uiScope = CoroutineScope(Dispatchers.Main + supervisorJob)

    /**
     * Muestra un mensaje breve en la parte inferior.
     */
    protected fun showSnackbar(view: View, message: String, isError: Boolean = false) {
        val snackbar = Snackbar.make(view, message, Snackbar.LENGTH_SHORT)
        if (isError) snackbar.setBackgroundTint(0xFFD32F2F.toInt()) // rojo error
        snackbar.show()
    }

    /**
     * Muestra un mensaje de éxito estandarizado.
     */
    protected fun showSuccess(view: View, message: String) {
        showSnackbar(view, message, isError = false)
    }

    /**
     * Muestra un mensaje de error estandarizado.
     */
    protected fun showError(view: View, error: Throwable) {
        showSnackbar(view, "Error: ${error.message ?: "Desconocido"}", isError = true)
        error.printStackTrace()
    }

    /**
     * Ejecuta de forma segura un bloque en corrutina, capturando excepciones.
     * Permite operaciones con UI y backend sin bloquear el hilo principal.
     */
    protected fun launchSafely(
        block: suspend CoroutineScope.() -> Unit,
        onError: (Throwable) -> Unit = { e -> e.printStackTrace() }
    ) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                // No hacemos nada: corrutina cancelada normalmente
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onError(e) }
            }
        }
    }

    /**
     * Ejecuta un bloque intensivo en segundo plano (Dispatchers.IO).
     */
    protected fun <T> launchIO(
        block: suspend () -> T,
        onResult: (T) -> Unit,
        onError: (Throwable) -> Unit = { e -> e.printStackTrace() }
    ) {
        uiScope.launch {
            try {
                val result = withContext(Dispatchers.IO) { block() }
                onResult(result)
            } catch (e: Exception) {
                onError(e)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        supervisorJob.cancel() // Evita fugas de corrutinas
    }
}
