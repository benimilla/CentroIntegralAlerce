package cl.alercelab.centrointegral.activities

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import cl.alercelab.centrointegral.R
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class CitaFormFragment : Fragment() {

    private lateinit var spActividad: Spinner
    private lateinit var tvAvisoPrevioInfo: TextView
    private lateinit var etLugar: EditText
    private lateinit var etMotivo: EditText
    private lateinit var etFecha: EditText
    private lateinit var etHoraInicio: EditText
    private lateinit var etHoraFin: EditText
    private lateinit var btnGuardar: Button
    private lateinit var progress: ProgressBar
    private lateinit var tvDuracionMaxInfo: TextView // 🟢 NUEVO

    private val db = FirebaseFirestore.getInstance()
    private val actividades = mutableListOf<Pair<String, String>>() // (id, nombre)
    private var actividadSeleccionadaId: String? = null

    private val formatoFecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val formatoHora = SimpleDateFormat("HH:mm", Locale.getDefault())

    private var fechaInicioMillis: Long = 0
    private var fechaFinMillis: Long = 0

    private var duracionMaxMinutos: Int? = null
    private var diasAvisoPrevio: Int? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val v = inflater.inflate(R.layout.fragment_cita_form, container, false)

        spActividad = v.findViewById(R.id.spActividad)
        tvAvisoPrevioInfo = v.findViewById(R.id.tvAvisoPrevioInfo)
        etLugar = v.findViewById(R.id.etLugar)
        etMotivo = v.findViewById(R.id.etMotivo)
        etFecha = v.findViewById(R.id.etFecha)
        etHoraInicio = v.findViewById(R.id.etHoraInicio)
        etHoraFin = v.findViewById(R.id.etHoraFin)
        btnGuardar = v.findViewById(R.id.btnGuardar)
        progress = v.findViewById(R.id.progressBar)
        tvDuracionMaxInfo = v.findViewById(R.id.tvDuracionMaxInfo) // 🟢 NUEVO

        etLugar.isEnabled = false
        etFecha.isFocusable = false
        etHoraInicio.isFocusable = false
        etHoraFin.isFocusable = false
        tvAvisoPrevioInfo.visibility = View.GONE
        tvDuracionMaxInfo.visibility = View.GONE // 🟢 NUEVO

        configurarPickers()
        cargarActividades()
        configurarBotonGuardar()

        return v
    }

    /** 🔹 Carga actividades */
    private fun cargarActividades() {
        lifecycleScope.launch {
            try {
                progress.visibility = View.VISIBLE
                val snap = db.collection("actividades").get().await()
                actividades.clear()
                actividades.addAll(
                    snap.documents.mapNotNull {
                        val nombre = it.getString("nombre")
                        val id = it.id
                        if (nombre != null) id to nombre else null
                    }
                )

                val adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_dropdown_item,
                    actividades.map { it.second }
                )
                spActividad.adapter = adapter

                spActividad.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>?,
                        view: View?,
                        position: Int,
                        id: Long
                    ) {
                        actividadSeleccionadaId = actividades[position].first
                        cargarDatosDeActividad(actividadSeleccionadaId!!)
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {
                        actividadSeleccionadaId = null
                        etLugar.setText("")
                        tvAvisoPrevioInfo.visibility = View.GONE
                        tvDuracionMaxInfo.visibility = View.GONE // 🟢 NUEVO
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error cargando actividades: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                progress.visibility = View.GONE
            }
        }
    }

    /** 🔹 Carga datos de la actividad seleccionada */
    private fun cargarDatosDeActividad(actividadId: String) {
        lifecycleScope.launch {
            try {
                val doc = db.collection("actividades").document(actividadId).get().await()
                val lugar = doc.getString("lugar") ?: "Sin lugar"
                duracionMaxMinutos = (doc.getLong("duracionMin") ?: 60L).toInt()
                diasAvisoPrevio = (doc.getLong("diasAvisoPrevio") ?: 0L).toInt()
                etLugar.setText(lugar)

                // 🔹 Mostrar aviso previo
                if (diasAvisoPrevio!! > 0) {
                    tvAvisoPrevioInfo.text = "🕓 Requiere programarse con $diasAvisoPrevio día(s) de anticipación."
                } else {
                    tvAvisoPrevioInfo.text = "🕓 No requiere anticipación especial."
                }
                tvAvisoPrevioInfo.visibility = View.VISIBLE

                // 🟢 NUEVO: mostrar duración máxima
                tvDuracionMaxInfo.text = "⏱️ Duración máxima permitida: ${duracionMaxMinutos} min"
                tvDuracionMaxInfo.visibility = View.VISIBLE

            } catch (e: Exception) {
                etLugar.setText("Error al cargar datos")
                tvAvisoPrevioInfo.visibility = View.GONE
                tvDuracionMaxInfo.visibility = View.GONE // 🟢 NUEVO
                duracionMaxMinutos = null
                diasAvisoPrevio = null
            }
        }
    }

    /** 🔹 Configura los selectores */
    private fun configurarPickers() {
        val calendar = Calendar.getInstance()

        etFecha.setOnClickListener {
            DatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    calendar.set(year, month, day)
                    etFecha.setText(formatoFecha.format(calendar.time))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        etHoraInicio.setOnClickListener {
            TimePickerDialog(
                requireContext(),
                { _, hour, minute ->
                    calendar.set(Calendar.HOUR_OF_DAY, hour)
                    calendar.set(Calendar.MINUTE, minute)
                    fechaInicioMillis = calendar.timeInMillis
                    etHoraInicio.setText(formatoHora.format(calendar.time))
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true
            ).show()
        }

        etHoraFin.setOnClickListener {
            TimePickerDialog(
                requireContext(),
                { _, hour, minute ->
                    calendar.set(Calendar.HOUR_OF_DAY, hour)
                    calendar.set(Calendar.MINUTE, minute)
                    fechaFinMillis = calendar.timeInMillis
                    etHoraFin.setText(formatoHora.format(calendar.time))

                    // 🟢 NUEVO: mostrar duración real si ambas horas están elegidas
                    if (fechaInicioMillis > 0) {
                        val duracion = ((fechaFinMillis - fechaInicioMillis) / 60000).toInt()
                        if (duracion > 0) {
                            tvDuracionMaxInfo.text = "⏱️ Duración seleccionada: $duracion min (máx ${duracionMaxMinutos ?: "?"} min)"
                        }
                    }
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true
            ).show()
        }
    }

    /** 🔹 Botón Guardar */
    private fun configurarBotonGuardar() {
        btnGuardar.setOnClickListener {
            val motivo = etMotivo.text.toString().trim()
            val actividadId = actividadSeleccionadaId
            val lugar = etLugar.text.toString().trim()

            if (actividadId == null) {
                Toast.makeText(requireContext(), "Seleccione una actividad", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (etFecha.text.isEmpty() || etHoraInicio.text.isEmpty() || etHoraFin.text.isEmpty()) {
                Toast.makeText(requireContext(), "Seleccione fecha y horas válidas", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (fechaFinMillis <= fechaInicioMillis) {
                Toast.makeText(requireContext(), "La hora de fin debe ser posterior al inicio", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 🔹 Duración real
            val duracionReal = ((fechaFinMillis - fechaInicioMillis) / 60000).toInt()
            duracionMaxMinutos?.let {
                if (duracionReal > it) {
                    Toast.makeText(
                        requireContext(),
                        "La duración excede el máximo permitido (${it} min).",
                        Toast.LENGTH_LONG
                    ).show()
                    return@setOnClickListener
                }
            }

            // 🔹 Validación de anticipación mínima
            val hoy = Calendar.getInstance()
            val fechaSeleccionada = Calendar.getInstance().apply { timeInMillis = fechaInicioMillis }

            hoy.set(Calendar.HOUR_OF_DAY, 0)
            hoy.set(Calendar.MINUTE, 0)
            hoy.set(Calendar.SECOND, 0)
            hoy.set(Calendar.MILLISECOND, 0)
            fechaSeleccionada.set(Calendar.HOUR_OF_DAY, 0)
            fechaSeleccionada.set(Calendar.MINUTE, 0)
            fechaSeleccionada.set(Calendar.SECOND, 0)
            fechaSeleccionada.set(Calendar.MILLISECOND, 0)

            val diffMillis = fechaSeleccionada.timeInMillis - hoy.timeInMillis
            val diferenciaDias = (diffMillis / (1000 * 60 * 60 * 24)).toInt()

            if (diferenciaDias < 1) {
                Toast.makeText(requireContext(), "No se pueden crear citas para hoy o días pasados.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            diasAvisoPrevio?.let {
                if (diferenciaDias < it) {
                    Toast.makeText(requireContext(), "Debe programarse con al menos $it día(s) de anticipación.", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
            }

            lifecycleScope.launch {
                progress.visibility = View.VISIBLE
                try {
                    val conflicto = hayConflicto(lugar, fechaInicioMillis, fechaFinMillis)
                    if (conflicto) {
                        Toast.makeText(requireContext(), "⚠️ Ya existe una cita en este lugar y horario.", Toast.LENGTH_LONG).show()
                    } else {
                        val cita = hashMapOf(
                            "actividadId" to actividadId,
                            "fechaInicioMillis" to fechaInicioMillis,
                            "fechaFinMillis" to fechaFinMillis,
                            "lugar" to lugar,
                            "observaciones" to motivo,
                            "estado" to "programada"
                        )

                        db.collection("citas").add(cita).await()
                        Toast.makeText(requireContext(), "✅ Cita creada correctamente", Toast.LENGTH_SHORT).show()
                        findNavController().popBackStack()
                    }
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Error al crear cita: ${e.message}", Toast.LENGTH_LONG).show()
                } finally {
                    progress.visibility = View.GONE
                }
            }
        }
    }

    /** 🔹 Comprueba conflictos */
    private suspend fun hayConflicto(lugar: String, inicio: Long, fin: Long): Boolean {
        val snap = db.collection("citas")
            .whereEqualTo("lugar", lugar)
            .whereEqualTo("estado", "programada")
            .get().await()

        return snap.documents.any { doc ->
            val i = doc.getLong("fechaInicioMillis") ?: return@any false
            val f = doc.getLong("fechaFinMillis") ?: return@any false
            (inicio in i..f) || (fin in i..f) || (inicio <= i && fin >= f)
        }
    }
}
