package cl.alercelab.centrointegral.activities

import android.app.AlertDialog
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
import cl.alercelab.centrointegral.data.Repos
import cl.alercelab.centrointegral.domain.Actividad
import cl.alercelab.centrointegral.domain.Cita
import kotlinx.coroutines.launch
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.ceil
import android.util.Log

class CitaFormFragment : Fragment() {

    private lateinit var spActividad: Spinner
    private lateinit var tvLugar: TextView
    private lateinit var tvAvisoPrevio: TextView
    private lateinit var tvDuracionMax: TextView
    private lateinit var etFecha: EditText
    private lateinit var etHoraInicio: EditText
    private lateinit var etHoraFin: EditText
    private lateinit var etObservaciones: EditText
    private lateinit var spEstado: Spinner
    private lateinit var btnGuardar: Button
    private lateinit var btnEliminar: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvTituloCita: TextView

    private lateinit var tvPeriodicidad: TextView

    private val repos = Repos()
    private var actividades: List<Actividad> = emptyList()
    private var actividadSeleccionada: Actividad? = null
    private var citaExistente: Cita? = null

    private val formato = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_cita_form, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicializar vistas
        spActividad = view.findViewById(R.id.spActividad)
        tvLugar = view.findViewById(R.id.tvLugar)
        tvAvisoPrevio = view.findViewById(R.id.tvAvisoPrevio)
        tvDuracionMax = view.findViewById(R.id.tvDuracionMax)
        etFecha = view.findViewById(R.id.etFecha)
        etHoraInicio = view.findViewById(R.id.etHoraInicio)
        etHoraFin = view.findViewById(R.id.etHoraFin)
        etObservaciones = view.findViewById(R.id.etObservaciones)
        spEstado = view.findViewById(R.id.spEstado)
        btnGuardar = view.findViewById(R.id.btnGuardar)
        btnEliminar = view.findViewById(R.id.btnEliminar)
        progressBar = view.findViewById(R.id.progressBar)
        tvTituloCita = view.findViewById(R.id.tvTituloCita)
        tvPeriodicidad = view.findViewById(R.id.tvPeriodicidad)
        configurarPickers()
        configurarSpinnerEstado()

        // Guardar cita si viene desde detalle
        citaExistente = arguments?.getSerializable("cita") as? Cita

        // Cambiar título según contexto
        if (citaExistente != null) {
            tvTituloCita.text = "Edición de Cita"
            btnGuardar.text = "Actualizar"
        } else {
            tvTituloCita.text = "Nueva Cita"
        }

        cargarActividades()

        btnGuardar.setOnClickListener { guardarCita() }
        btnEliminar.setOnClickListener { confirmarEliminacion() }
    }

    /** 📅 Configura los pickers de fecha y hora */
    private fun configurarPickers() {
        etFecha.setOnClickListener {
            val c = Calendar.getInstance()
            DatePickerDialog(
                requireContext(),
                { _, y, m, d -> etFecha.setText("%02d/%02d/%04d".format(d, m + 1, y)) },
                c.get(Calendar.YEAR),
                c.get(Calendar.MONTH),
                c.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        val timePicker = { target: EditText ->
            val c = Calendar.getInstance()
            TimePickerDialog(
                requireContext(),
                { _, h, m -> target.setText("%02d:%02d".format(h, m)) },
                c.get(Calendar.HOUR_OF_DAY),
                c.get(Calendar.MINUTE),
                true
            ).show()
        }

        etHoraInicio.setOnClickListener { timePicker(etHoraInicio) }
        etHoraFin.setOnClickListener { timePicker(etHoraFin) }
    }

    /** 🔹 Configura el spinner de estados */
    private fun configurarSpinnerEstado() {
        val adapterEstado = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.estados_cita,
            R.layout.item_spinner_estado
        )
        adapterEstado.setDropDownViewResource(R.layout.item_spinner_estado)
        spEstado.adapter = adapterEstado

        // Estado por defecto "Pendiente" si es nueva cita
        if (citaExistente == null) {
            val index = adapterEstado.getPosition("Pendiente")
            if (index >= 0) spEstado.setSelection(index)
        }
    }

    /** 🔹 Carga todas las actividades disponibles */
    private fun cargarActividades() {
        lifecycleScope.launch {
            progressBar.visibility = View.VISIBLE
            try {
                actividades = repos.obtenerActividades()
                if (actividades.isEmpty()) {
                    toast("No hay actividades disponibles.")
                    return@launch
                }

                val nombres = actividades.map { it.nombre }

                val adapter = ArrayAdapter(
                    requireContext(),
                    R.layout.item_spinner_actividad,
                    nombres
                )
                adapter.setDropDownViewResource(R.layout.item_spinner_actividad)
                spActividad.adapter = adapter

                spActividad.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>?,
                        view: View?,
                        position: Int,
                        id: Long
                    ) {
                        actividadSeleccionada = actividades[position]
                        actualizarInfoActividad()
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }

                // Si venimos desde "Editar cita"
                citaExistente?.let { cita ->
                    val index = actividades.indexOfFirst { it.id == cita.actividadId }
                    if (index >= 0) {
                        spActividad.setSelection(index)
                        actividadSeleccionada = actividades[index]
                        actualizarInfoActividad()
                        cargarDatosCita(cita)
                    }
                }

            } catch (e: Exception) {
                toast("Error al cargar actividades: ${e.message}")
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    /**  Muestra datos de la actividad seleccionada */
    private fun actualizarInfoActividad() {
        actividadSeleccionada?.let {
            tvLugar.text = "Lugar: ${it.lugar}"
            tvAvisoPrevio.text = "Aviso previo: ${it.diasAvisoPrevio} día(s)"
            tvDuracionMax.text = "Duración máxima: ${it.duracionMin ?: 0} min"
            tvPeriodicidad.text = "Periodicidad: ${it.periodicidad ?: "-"}"
        }
    }


    /** 🧾 Carga datos al editar una cita existente */
    private fun cargarDatosCita(cita: Cita) {
        val fecha = formato.format(Date(cita.fechaInicioMillis)).split(" ")[0]
        val horaInicio = formato.format(Date(cita.fechaInicioMillis)).split(" ")[1]
        val horaFin = formato.format(Date(cita.fechaFinMillis)).split(" ")[1]

        etFecha.setText(fecha)
        etHoraInicio.setText(horaInicio)
        etHoraFin.setText(horaFin)
        etObservaciones.setText(cita.observaciones ?: "")

        val adapter = spEstado.adapter as ArrayAdapter<String>
        val index = adapter.getPosition(cita.estado)
        if (index >= 0) spEstado.setSelection(index)

        btnEliminar.visibility = View.VISIBLE
    }

    /** 💾 Guarda o actualiza la cita */
    private fun guardarCita() {
        val actividad = actividadSeleccionada
        if (actividad == null) {
            toast("Selecciona una actividad antes de continuar.")
            return
        }

        val fecha = etFecha.text.toString().trim()
        val horaInicio = etHoraInicio.text.toString().trim()
        val horaFin = etHoraFin.text.toString().trim()

        if (fecha.isEmpty() || horaInicio.isEmpty() || horaFin.isEmpty()) {
            toast("Completa la fecha y las horas de inicio y fin.")
            return
        }

        val inicio: Long
        val fin: Long
        try {
            inicio = formato.parse("$fecha $horaInicio")?.time
                ?: run { toast("Fecha u hora inválida."); return }
            fin = formato.parse("$fecha $horaFin")?.time
                ?: run { toast("Fecha u hora inválida."); return }
        } catch (e: ParseException) {
            toast("Formato de fecha/hora inválido.")
            return
        }

        if (fin <= inicio) {
            toast("La hora de fin debe ser posterior al inicio.")
            return
        }

        val minDias = maxOf(1, actividad.diasAvisoPrevio)
        val diasDiff = diasEntreAhoraY(inicio)
        if (diasDiff < minDias) {
            toast("Debe programarse con al menos $minDias día(s) de anticipación.")
            return
        }

        val duracion = ((fin - inicio) / (1000 * 60)).toInt()
        val duracionMax = actividad.duracionMin
        if (duracionMax != null && duracion > duracionMax) {
            toast("La duración supera el máximo permitido (${duracionMax} min).")
            return
        }

        val estadoSeleccionado = spEstado.selectedItem.toString()

        lifecycleScope.launch {
            progressBar.visibility = View.VISIBLE
            try {
                val conflicto = repos.hayConflictoCita(actividad.lugar, inicio, fin)
                if (conflicto && citaExistente == null) {
                    toast("Conflicto: ya hay una cita en ese lugar y horario.")
                    return@launch
                }

                if (citaExistente == null) {
                    val nuevaCita = Cita(
                        actividadId = actividad.id,
                        fechaInicioMillis = inicio,
                        fechaFinMillis = fin,
                        lugar = actividad.lugar,
                        observaciones = etObservaciones.text?.toString(),
                        duracionMin = duracion,
                        estado = estadoSeleccionado
                    )
                    repos.crearCita(nuevaCita)
                    toast("✅ Cita creada correctamente.")

                    // Generar repeticiones si aplica
                    if (actividad.periodicidad != null && actividad.periodicidad != "Única") {
                        generarRepeticiones(actividad, inicio, fin)
                    }

                } else {
                    val citaEditada = citaExistente!!.copy(
                        actividadId = actividad.id,
                        fechaInicioMillis = inicio,
                        fechaFinMillis = fin,
                        lugar = actividad.lugar,
                        observaciones = etObservaciones.text?.toString(),
                        duracionMin = duracion,
                        estado = estadoSeleccionado,
                        ultimaActualizacion = System.currentTimeMillis()
                    )
                    repos.actualizarCita(citaEditada.id, citaEditada)
                    toast("✅ Cita actualizada correctamente.")
                }

                findNavController().previousBackStackEntry
                    ?.savedStateHandle
                    ?.set("citaGuardada", true)

                findNavController().navigateUp()

            } catch (e: Exception) {
                Log.e("CITA_FORM", "Error al guardar cita", e)
                toast("Error al guardar cita: ${e.message}")
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    /** 🔁 Genera automáticamente repeticiones semanales o mensuales */
    private suspend fun generarRepeticiones(actividad: Actividad, inicioBase: Long, finBase: Long) {
        val calendarioInicio = Calendar.getInstance()
        val calendarioFin = Calendar.getInstance()
        calendarioInicio.timeInMillis = inicioBase
        calendarioFin.timeInMillis = finBase

        val repeticiones = mutableListOf<Cita>()
        val cantidadRepeticiones = when (actividad.periodicidad) {
            "Semanal" -> 4  // 4 semanas
            "Mensual" -> 3  // 3 meses
            else -> 0
        }

        repeat(cantidadRepeticiones) {
            when (actividad.periodicidad) {
                "Semanal" -> {
                    calendarioInicio.add(Calendar.WEEK_OF_YEAR, 1)
                    calendarioFin.add(Calendar.WEEK_OF_YEAR, 1)
                }
                "Mensual" -> {
                    calendarioInicio.add(Calendar.MONTH, 1)
                    calendarioFin.add(Calendar.MONTH, 1)
                }
            }

            val citaRepetida = Cita(
                actividadId = actividad.id,
                fechaInicioMillis = calendarioInicio.timeInMillis,
                fechaFinMillis = calendarioFin.timeInMillis,
                lugar = actividad.lugar,
                observaciones = "Repetición automática (${actividad.periodicidad?.lowercase()})",
                duracionMin = actividad.duracionMin,
                estado = "Pendiente"
            )

            repeticiones.add(citaRepetida)
        }

        if (repeticiones.isNotEmpty()) {
            for (cita in repeticiones) {
                repos.crearCita(cita)
            }
            toast("🔁 ${repeticiones.size} citas recurrentes creadas automáticamente.")
        }
    }

    /** 🗑️ Confirmar eliminación */
    private fun confirmarEliminacion() {
        AlertDialog.Builder(requireContext())
            .setTitle("Eliminar cita")
            .setMessage("¿Seguro que deseas eliminar esta cita?")
            .setPositiveButton("Eliminar") { _, _ -> eliminarCita() }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    /** ❌ Elimina la cita */
    private fun eliminarCita() {
        citaExistente?.let { cita ->
            lifecycleScope.launch {
                progressBar.visibility = View.VISIBLE
                try {
                    repos.eliminarCita(cita.id)
                    toast("🗑️ Cita eliminada correctamente.")
                    findNavController().navigateUp()
                } catch (e: Exception) {
                    toast("Error al eliminar cita: ${e.message}")
                } finally {
                    progressBar.visibility = View.GONE
                }
            }
        }
    }

    /** ⏳ Calcula diferencia de días */
    private fun diasEntreAhoraY(futuroMillis: Long): Int {
        val ahora = System.currentTimeMillis()
        val diffMs = futuroMillis - ahora
        val unDiaMs = 24 * 60 * 60 * 1000.0
        return ceil(diffMs / unDiaMs).toInt()
    }

    private fun toast(msg: String) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
    }
}
