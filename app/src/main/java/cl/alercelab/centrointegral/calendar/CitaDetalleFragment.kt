package cl.alercelab.centrointegral.calendar

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import cl.alercelab.centrointegral.R
import cl.alercelab.centrointegral.data.Repos
import cl.alercelab.centrointegral.domain.Actividad
import cl.alercelab.centrointegral.domain.Cita
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*
import cl.alercelab.centrointegral.notifications.NotificationHelper
import androidx.work.WorkManager
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit
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
    private lateinit var tvEstado: TextView
    private lateinit var tvObservaciones: TextView
    private lateinit var btnReagendar: Button
    private lateinit var btnEditarCita: Button
    private lateinit var btnEliminarCita: Button

    private val formatoFecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val formatoHora = SimpleDateFormat("HH:mm", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_cita_detalle, container, false)

        tvTitulo = view.findViewById(R.id.tvTitulo)
        tvFecha = view.findViewById(R.id.tvFecha)
        tvHora = view.findViewById(R.id.tvHora)
        tvLugar = view.findViewById(R.id.tvLugar)
        tvOferente = view.findViewById(R.id.tvOferente)
        tvSocio = view.findViewById(R.id.tvSocio)
        tvEstado = view.findViewById(R.id.tvEstado)
        tvObservaciones = view.findViewById(R.id.tvObservaciones)
        btnReagendar = view.findViewById(R.id.btnEditar)
        btnEditarCita = view.findViewById(R.id.btnEditarCita)
        btnEliminarCita = view.findViewById(R.id.btnEliminarCita)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        citaId = arguments?.getString("citaId")

        if (citaId == null) {
            toast("Error: cita no encontrada")
            requireActivity().onBackPressedDispatcher.onBackPressed()
            return
        }

        cargarDatosCita()

        btnReagendar.setOnClickListener {
            citaActual?.let { mostrarDialogoReagendar(it) }
        }

        btnEditarCita.setOnClickListener {
            citaActual?.let {
                val bundle = Bundle().apply {
                    putString("citaId", it.id)
                    putString("modo", "editar")
                }
                findNavController().navigate(R.id.action_citaDetalleFragment_to_citaFormFragment, bundle)
            }
        }

        btnEliminarCita.setOnClickListener {
            citaActual?.let { confirmarEliminacion(it) }
        }
    }

    private fun cargarDatosCita() {
        lifecycleScope.launch {
            try {
                val doc = db.collection("citas").document(citaId!!).get().await()
                citaActual = doc.toObject(Cita::class.java)?.apply { id = doc.id }

                if (citaActual == null) {
                    toast("No se encontró la cita")
                    return@launch
                }

                if (citaActual!!.actividadId.isNotEmpty()) {
                    actividadActual = repo.obtenerActividadPorId(citaActual!!.actividadId)
                }

                mostrarDatosCompletos()
            } catch (e: Exception) {
                Log.e("CITA_DETALLE", "Error cargando datos", e)
                toast("Error al cargar datos: ${e.message}")
            }
        }
    }

    private fun mostrarDatosCompletos() {
        val cita = citaActual ?: return
        val actividad = actividadActual

        tvTitulo.text = actividad?.nombre ?: "(Actividad sin nombre)"
        tvFecha.text = " Fecha: ${formatoFecha.format(Date(cita.fechaInicioMillis))}"
        tvHora.text = " Hora: ${formatoHora.format(Date(cita.fechaInicioMillis))}"
        tvLugar.text = " Lugar: ${cita.lugar.ifEmpty { actividad?.lugar ?: "Sin lugar" }}"
        tvOferente.text = " Oferente: ${actividad?.oferente ?: "No especificado"}"
        tvSocio.text = " Socio: ${actividad?.socioComunitario ?: "No aplica"}"
        tvEstado.text = " Estado: ${cita.estado ?: "Pendiente"}"
        tvObservaciones.text = " Observaciones: ${cita.observaciones ?: "-"}"
    }

    private fun mostrarDialogoReagendar(cita: Cita) {
        val calendar = Calendar.getInstance().apply { timeInMillis = cita.fechaInicioMillis }
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
                                toast("⚠️ Ya existe una cita en ese horario.")
                            } else {
                                repo.reagendarCita(cita.id, nuevoInicio, nuevoFin, cita.lugar)
                                toast("✅ Cita reagendada correctamente.")

                                //  Nueva notificación
                                NotificationHelper.showSimpleNotification(
                                    requireContext(),
                                    "Cita reagendada",
                                    "La cita de '${actividadActual?.nombre ?: "Actividad"}' se ha reagendado para el ${
                                        formatoFecha.format(Date(nuevoInicio))
                                    } a las ${formatoHora.format(Date(nuevoInicio))}."
                                )

// 👉 Programar alerta 30 min antes
                                val delay = (nuevoInicio - System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(30))
                                    .coerceAtLeast(0)
                                val work = OneTimeWorkRequestBuilder<cl.alercelab.centrointegral.notifications.AlertWorker>()
                                    .setInputData(workDataOf("actividadId" to (actividadActual?.id ?: ""), "citaId" to cita.id))
                                    .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                                    .build()
                                WorkManager.getInstance(requireContext()).enqueue(work)

                                findNavController().navigateUp()
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
                            (fin in it.fechaInicioMillis..it.fechaFinMillis)
                    )
        }
    }

    private fun confirmarEliminacion(cita: Cita) {
        AlertDialog.Builder(requireContext())
            .setTitle("Eliminar Cita")
            .setMessage("¿Estás seguro de eliminar esta cita? Esta acción no se puede deshacer.")
            .setPositiveButton("Eliminar") { _, _ ->
                lifecycleScope.launch {
                    try {
                        db.collection("citas").document(cita.id).delete().await()
                        toast("🗑️ Cita eliminada correctamente.")

                        //  Nueva notificación
                        NotificationHelper.showSimpleNotification(
                            requireContext(),
                            "Cita cancelada",
                            "Se ha cancelado la cita de la actividad '${actividadActual?.nombre ?: "sin nombre"}'."
                        )

                        findNavController().navigateUp()
                    } catch (e: Exception) {
                        toast("Error al eliminar cita: ${e.message}")
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun toast(msg: String) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
    }
}
