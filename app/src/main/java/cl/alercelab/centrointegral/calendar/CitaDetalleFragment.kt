package cl.alercelab.centrointegral.calendar

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import cl.alercelab.centrointegral.R
import cl.alercelab.centrointegral.data.Repos
import cl.alercelab.centrointegral.domain.Cita
import cl.alercelab.centrointegral.domain.Actividad
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class CitaDetalleFragment : Fragment() {

    private val repo = Repos()
    private val db = FirebaseFirestore.getInstance()

    private var citaId: String? = null
    private var citaActual: Cita? = null
    private var actividadActual: Actividad? = null

    private lateinit var tvTitulo: TextView
    private lateinit var tvFecha: TextView
    private lateinit var tvHora: TextView
    private lateinit var tvLugar: TextView
    private lateinit var tvOferente: TextView
    private lateinit var tvSocio: TextView
    private lateinit var tvObservaciones: TextView
    private lateinit var btnEditar: Button

    private val formatoFecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val formatoHora = SimpleDateFormat("HH:mm", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_cita_detalle, container, false)

        tvTitulo = view.findViewById(R.id.tvTitulo)
        tvFecha = view.findViewById(R.id.tvFecha)
        tvHora = view.findViewById(R.id.tvHora)
        tvLugar = view.findViewById(R.id.tvLugar)
        tvOferente = view.findViewById(R.id.tvOferente)
        tvSocio = view.findViewById(R.id.tvSocio)
        tvObservaciones = view.findViewById(R.id.tvObservaciones)
        btnEditar = view.findViewById(R.id.btnEditar)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        citaId = arguments?.getString("citaId")

        if (citaId == null) {
            Toast.makeText(requireContext(), "Error: cita no encontrada", Toast.LENGTH_LONG).show()
            requireActivity().onBackPressedDispatcher.onBackPressed()
            return
        }

        cargarDatosCita()

        btnEditar.setOnClickListener {
            citaActual?.let { mostrarDialogoReagendar(it) }
                ?: Toast.makeText(requireContext(), "No hay cita cargada", Toast.LENGTH_SHORT).show()
        }
    }

    private fun cargarDatosCita() {
        lifecycleScope.launch {
            try {
                // 🔹 Cargar cita desde Firestore
                val doc = db.collection("citas").document(citaId!!).get().await()
                citaActual = doc.toObject(Cita::class.java)?.apply { id = doc.id }

                if (citaActual == null) {
                    Toast.makeText(requireContext(), "No se encontró la cita", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                // 🔹 Cargar la actividad asociada
                if (citaActual!!.actividadId.isNotEmpty()) {
                    actividadActual = repo.obtenerActividadPorId(citaActual!!.actividadId)
                }

                mostrarDatosCompletos()

            } catch (e: Exception) {
                Log.e("CITA_DETALLE", "Error cargando datos", e)
                Toast.makeText(requireContext(), "Error al cargar datos: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun mostrarDatosCompletos() {
        val cita = citaActual ?: return
        val actividad = actividadActual

        tvTitulo.text = actividad?.nombre ?: "(Actividad sin nombre)"
        tvFecha.text = "📅 Fecha: ${formatoFecha.format(Date(cita.fechaInicioMillis))}"
        tvHora.text = "⏰ Hora: ${formatoHora.format(Date(cita.fechaInicioMillis))}"
        tvLugar.text = "📍 Lugar: ${cita.lugar.ifEmpty { actividad?.lugar ?: "Sin lugar" }}"
        tvOferente.text = "👤 Oferente: ${actividad?.oferente ?: "No especificado"}"
        tvSocio.text = "🤝 Socio Comunitario: ${actividad?.socioComunitario ?: "No aplica"}"
        tvObservaciones.text = "📝 Observaciones: ${cita.observaciones ?: "-"}"
    }

    private fun mostrarDialogoReagendar(cita: Cita) {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = cita.fechaInicioMillis

        DatePickerDialog(requireContext(),
            { _, year, month, day ->
                calendar.set(year, month, day)

                TimePickerDialog(requireContext(),
                    { _, hour, minute ->
                        calendar.set(Calendar.HOUR_OF_DAY, hour)
                        calendar.set(Calendar.MINUTE, minute)

                        val nuevoInicio = calendar.timeInMillis
                        val duracion = cita.fechaFinMillis - cita.fechaInicioMillis
                        val nuevoFin = nuevoInicio + duracion

                        lifecycleScope.launch {
                            if (hayConflicto(nuevoInicio, nuevoFin, cita.id)) {
                                Toast.makeText(requireContext(), "⚠️ Ya existe una cita en ese horario", Toast.LENGTH_LONG).show()
                            } else {
                                repo.reagendarCita(cita.id, nuevoInicio, nuevoFin, cita.lugar)
                                Toast.makeText(requireContext(), "✅ Cita reagendada correctamente", Toast.LENGTH_SHORT).show()
                                requireActivity().onBackPressedDispatcher.onBackPressed()
                            }
                        }
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    true
                ).show()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private suspend fun hayConflicto(inicio: Long, fin: Long, citaId: String): Boolean {
        val citas = repo.citasEnRango(inicio - 3600000, fin + 3600000)
        return citas.any {
            it.id != citaId && (
                    (inicio in it.fechaInicioMillis..it.fechaFinMillis) ||
                            (fin in it.fechaInicioMillis..it.fechaFinMillis) ||
                            (inicio <= it.fechaInicioMillis && fin >= it.fechaFinMillis)
                    )
        }
    }
}
