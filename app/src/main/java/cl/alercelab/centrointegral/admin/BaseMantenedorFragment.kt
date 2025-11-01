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

    /**
     * 🔍 Registra una acción de auditoría en Firestore.
     *
     * Este método puede ser invocado por cualquier mantenedor que herede de esta clase.
     *
     * @param usuarioId ID del usuario que realiza la acción.
     * @param usuarioNombre Nombre del usuario.
     * @param modulo Nombre del módulo (Ej: "Citas", "Actividades", "Usuarios").
     * @param accion Tipo de acción ("Creación", "Edición", "Eliminación", etc.).
     * @param entidadId ID del objeto afectado.
     * @param descripcion Texto descriptivo del cambio realizado.
     * @param cambios Mapa opcional con los campos modificados (clave = campo, valor = nuevo valor).
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

    override fun onDestroyView() {
        super.onDestroyView()
        supervisorJob.cancel() // Evita fugas de corrutinas
    }
}
