package cl.alercelab.centrointegral.admin

import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await

/**
 * Clase base para todos los mantenedores del módulo Admin.
 * Proporciona manejo centralizado de corrutinas, errores, notificaciones y registro de auditoría.
 */
abstract class BaseMantenedorFragment : Fragment() {

    // SupervisorJob evita que un fallo en una corrutina cancele las demás dentro del mismo scope
    private val supervisorJob = SupervisorJob()
    private val uiScope = CoroutineScope(Dispatchers.Main + supervisorJob) // Scope principal para UI

    /**
     * Muestra un mensaje breve en la parte inferior (Snackbar).
     * Si isError = true, el fondo se pinta rojo para destacar errores.
     */
    protected fun showSnackbar(view: View, message: String, isError: Boolean = false) {
        val snackbar = Snackbar.make(view, message, Snackbar.LENGTH_SHORT)
        if (isError) snackbar.setBackgroundTint(0xFFD32F2F.toInt()) // Rojo error (Color Material)
        snackbar.show()
    }

    /**
     * Muestra un mensaje de éxito estandarizado en un Snackbar.
     */
    protected fun showSuccess(view: View, message: String) {
        showSnackbar(view, message, isError = false)
    }

    /**
     * Muestra un mensaje de error estandarizado en un Snackbar.
     * También imprime el stacktrace para depuración.
     */
    protected fun showError(view: View, error: Throwable) {
        showSnackbar(view, "Error: ${error.message ?: "Desconocido"}", isError = true)
        error.printStackTrace()
    }

    /**
     * Ejecuta un bloque suspend de manera segura dentro del ciclo de vida del fragmento.
     * Captura excepciones y evita bloqueos en el hilo principal.
     */
    protected fun launchSafely(
        block: suspend CoroutineScope.() -> Unit,
        onError: (Throwable) -> Unit = { e -> e.printStackTrace() }
    ) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                // No se considera error: la corrutina fue cancelada normalmente
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onError(e) }
            }
        }
    }

    /**
     * Ejecuta un bloque intensivo o de acceso a red en el hilo de IO,
     * y devuelve el resultado en el hilo principal para actualizar la UI.
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

    /**
     *  Registra una acción de auditoría en Firestore.
     * Cualquier mantenedor que herede esta clase puede usarlo para registrar eventos importantes.
     *
     * @param usuarioId ID del usuario que realiza la acción.
     * @param usuarioNombre Nombre del usuario.
     * @param modulo Módulo del sistema donde ocurre la acción (Ej: "Citas", "Actividades").
     * @param accion Tipo de acción ("Creación", "Edición", "Eliminación", etc.).
     * @param entidadId ID del objeto afectado.
     * @param descripcion Texto descriptivo del cambio.
     * @param cambios Campos modificados y sus nuevos valores (opcional).
     */
    protected fun registrarAuditoria(
        usuarioId: String,
        usuarioNombre: String,
        modulo: String,
        accion: String,
        entidadId: String,
        descripcion: String,
        cambios: Map<String, Any>? = null
    ) {
        launchIO(
            block = {
                // Construye el objeto auditoría
                val auditoria = mapOf(
                    "usuarioId" to usuarioId,
                    "usuarioNombre" to usuarioNombre,
                    "modulo" to modulo,
                    "accion" to accion,
                    "entidadId" to entidadId,
                    "descripcion" to descripcion,
                    "cambios" to (cambios ?: emptyMap<String, Any>()),
                    "fecha" to System.currentTimeMillis()
                )
                // Inserta en Firestore
                val db = FirebaseFirestore.getInstance()
                db.collection("auditoria").add(auditoria).await()
                true
            },
            onResult = {
                view?.let { showSuccess(it, "Registro de auditoría guardado") }
            },
            onError = { e ->
                view?.let { showError(it, e) }
            }
        )
    }

    /**
     * Cancela todas las corrutinas al destruir la vista del fragmento
     * para evitar fugas de memoria o ejecuciones fuera del ciclo de vida.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        supervisorJob.cancel()
    }
}