package cl.alercelab.centrointegral.calendar

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import cl.alercelab.centrointegral.R
import cl.alercelab.centrointegral.domain.Actividad
import cl.alercelab.centrointegral.domain.Cita
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class CitaDetalleFragment : Fragment() {

    private val db = FirebaseFirestore.getInstance()
    private val formato = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    private lateinit var tvTitulo: TextView
    private lateinit var tvInfo: TextView
    private lateinit var progress: ProgressBar

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val v = inflater.inflate(R.layout.fragment_cita_detalle, container, false)
        tvTitulo = v.findViewById(R.id.tvTitulo)
        tvInfo = v.findViewById(R.id.tvInfo)
        progress = v.findViewById(R.id.progressBar)
        return v
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val citaId = arguments?.getString("citaId")

        if (citaId.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "ID de cita no válido", Toast.LENGTH_SHORT).show()
            return
        }

        cargarDetalleCita(citaId)
    }

    private fun cargarDetalleCita(citaId: String) {
        lifecycleScope.launch {
            try {
                progress.visibility = View.VISIBLE

                val citaSnap = db.collection("citas").document(citaId).get().await()
                val cita = citaSnap.toObject(Cita::class.java)
                if (cita == null) {
                    Toast.makeText(requireContext(), "Cita no encontrada", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val actividadSnap = db.collection("actividades").document(cita.actividadId).get().await()
                val actividad = actividadSnap.toObject(Actividad::class.java)

                mostrarDetalle(cita, actividad)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error al cargar detalle: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                progress.visibility = View.GONE
            }
        }
    }

    private fun mostrarDetalle(cita: Cita, actividad: Actividad?) {
        tvTitulo.text = actividad?.nombre ?: "Cita sin actividad vinculada"

        val info = buildString {
            appendLine("📅 Fecha: ${formato.format(Date(cita.fechaInicioMillis))}")
            appendLine("🕓 Fin: ${formato.format(Date(cita.fechaFinMillis))}")
            appendLine("📍 Lugar: ${cita.lugar}")
            appendLine("⏱ Duración: ${cita.duracionMin} minutos")
            if (cita.motivo != null) appendLine("📝 Motivo: ${cita.motivo}")

            if (actividad != null) {
                appendLine()
                appendLine("🏷 Tipo: ${actividad.tipo}")
                appendLine("🔁 Periodicidad: ${actividad.periodicidad}")
                appendLine("👥 Cupo: ${actividad.cupo ?: "No especificado"}")
                appendLine("💼 Oferente: ${actividad.oferente ?: "No indicado"}")
                if (!actividad.socioComunitario.isNullOrEmpty())
                    appendLine("🤝 Socio comunitario: ${actividad.socioComunitario}")
                if (actividad.beneficiarios.isNotEmpty())
                    appendLine("🎯 Beneficiarios: ${actividad.beneficiarios.joinToString(", ")}")
                appendLine("📆 Días de aviso previo: ${actividad.diasAvisoPrevio}")
                appendLine("📍 Lugar actividad: ${actividad.lugar}")
            }
        }

        tvInfo.text = info
    }
}
