package cl.alercelab.centrointegral.activities

import android.app.AlertDialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import cl.alercelab.centrointegral.domain.Actividad
import cl.alercelab.centrointegral.domain.Cita
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class ActivityDetailFragment : DialogFragment() {

    private val db = FirebaseFirestore.getInstance()
    private val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    override fun onCreateDialog(savedInstanceState: Bundle?) = activity?.let { act ->
        val actividadId = arguments?.getString("actividadId").orEmpty()

        // Crea el cuadro de diálogo que mostrará los detalles de la actividad
        val builder = AlertDialog.Builder(act)
            .setTitle("Detalle de actividad")
            .setMessage("Cargando...")
            .setPositiveButton("Cerrar", null)

        val dlg = builder.create()

        // Carga la información de la actividad y sus citas desde Firestore
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Obtiene el documento de la actividad por su ID
                val doc = db.collection("actividades").document(actividadId).get().await()
                val actividad = doc.toObject<Actividad>()?.copy(id = doc.id)

                // Obtiene las citas asociadas a la actividad
                val citasSnap = db.collection("citas")
                    .whereEqualTo("actividadId", actividadId)
                    .get()
                    .await()
                val citas = citasSnap.documents.mapNotNull { it.toObject(Cita::class.java)?.copy(id = it.id) }
                    .sortedBy { it.fechaInicioMillis }

                // Muestra la información formateada dentro del cuadro de diálogo
                val msg = buildMessage(actividad, citas)
                dlg.setMessage(msg)
            } catch (e: Exception) {
                // Muestra un mensaje de error si ocurre un problema al cargar los datos
                dlg.setMessage("Error: ${e.message}")
            }
        }

        dlg
    } ?: super.onCreateDialog(savedInstanceState)

    private fun buildMessage(a: Actividad?, citas: List<Cita>): String {
        if (a == null) return "Actividad no encontrada"

        // Construye el texto que se mostrará en el diálogo con los detalles de la actividad y sus citas
        return buildString {
            appendLine("Nombre: ${a.nombre}")
            appendLine("Tipo: ${a.tipo}")
            appendLine("Periodicidad: ${a.periodicidad}")
            appendLine("Lugar: ${a.lugar}")
            appendLine("Oferente: ${a.oferente ?: "-"}")
            appendLine("Socio Comunitario: ${a.socioComunitario ?: "-"}")
            appendLine("Cupo: ${a.cupo ?: "-"}")
            appendLine("Beneficiarios: ${if (a.beneficiarios.isEmpty()) "-" else a.beneficiarios.joinToString()}")
            appendLine("Días aviso previo: ${a.diasAvisoPrevio}")
            appendLine("Estado: ${a.estado}")
            a.motivoCancelacion?.let { appendLine("Motivo cancelación: $it") }
            appendLine()
            appendLine("Citas:")
            if (citas.isEmpty()) {
                appendLine("- (sin citas)")
            } else {
                citas.forEach { c ->
                    // Muestra cada cita con fecha, hora, duración y lugar
                    val duracion = ((c.fechaFinMillis - c.fechaInicioMillis) / (1000 * 60)).toInt()
                    appendLine("• ${sdf.format(Date(c.fechaInicioMillis))} - ${sdf.format(Date(c.fechaFinMillis))} (${duracion} min) · ${c.lugar}")
                }
            }
        }
    }
}
