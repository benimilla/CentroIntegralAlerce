package cl.alercelab.centrointegral.activities

import android.app.AlertDialog
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
import cl.alercelab.centrointegral.notifications.NotificationHelper
import androidx.work.*
import java.util.concurrent.TimeUnit

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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_cita_form, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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

        citaExistente = arguments?.getSerializable("cita") as? Cita
        if (citaExistente != null) {
            tvTituloCita.text = "Edición de Cita"
            btnGuardar.text = "Actualizar"
        } else {
            tvTituloCita.text = "Nueva Cita"
        }

        cargarActividades()
        // Botón que guarda o actualiza una cita según corresponda
        btnGuardar.setOnClickListener { guardarCita() }
        // Botón que abre un diálogo de confirmación antes de eliminar la cita
        btnEliminar.setOnClickListener { confirmarEliminacion() }
    }

    private fun configurarPickers() {
        // Muestra un selector de fecha al tocar el campo correspondiente
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

        // Muestra un selector de hora al tocar los campos de hora
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

    private fun configurarSpinnerEstado() {
        val adapterEstado = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.estados_cita,
            R.layout.item_spinner_estado
        )
        adapterEstado.setDropDownViewResource(R.layout.item_spinner_estado)
        spEstado.adapter = adapterEstado

        // Si es una nueva cita, se selecciona "Pendiente" por defecto
        if (citaExistente == null) {
            val index = adapterEstado.getPosition("Pendiente")
            if (index >= 0) spEstado.setSelection(index)
        }
    }

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
                val adapter = ArrayAdapter(requireContext(), R.layout.item_spinner_actividad, nombres)
                adapter.setDropDownViewResource(R.layout.item_spinner_actividad)
                spActividad.adapter = adapter

                // Listener para detectar cambio de actividad seleccionada
                spActividad.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                        actividadSeleccionada = actividades[pos]
                        actualizarInfoActividad()
                    }
                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }

                // Si se está editando una cita, carga los datos en los campos
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

    private fun actualizarInfoActividad() {
        // Actualiza la información visible de la actividad seleccionada
        actividadSeleccionada?.let {
            tvLugar.text = "Lugar: ${it.lugar}"
            tvAvisoPrevio.text = "Aviso previo: ${it.diasAvisoPrevio} día(s)"
            tvDuracionMax.text = "Duración máxima: ${it.duracionMin ?: 0} min"
            tvPeriodicidad.text = "Periodicidad: ${it.periodicidad ?: "-"}"
        }
    }

    private fun cargarDatosCita(cita: Cita) {
        // Carga los datos de una cita existente en los campos del formulario
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

    private fun guardarCita() {
        // Valida los datos del formulario antes de guardar o actualizar una cita
        val actividad = actividadSeleccionada ?: run {
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
            inicio = formato.parse("$fecha $horaInicio")?.time ?: return toast("Fecha inválida").run {}
            fin = formato.parse("$fecha $horaFin")?.time ?: return toast("Hora inválida").run {}
        } catch (e: ParseException) {
            toast("Formato de fecha u hora inválido.")
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
                // Verifica si existe una cita que se cruza con el horario
                val conflicto = repos.hayConflictoCita(actividad.lugar, inicio, fin)
                if (conflicto && citaExistente == null) {
                    toast("Conflicto: ya hay una cita en ese lugar y horario.")
                    return@launch
                }

                val accion: String
                val citaId: String
                if (citaExistente == null) {
                    // Crea una nueva cita
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
                    citaId = nuevaCita.id
                    accion = "Creación"
                    toast("Cita creada correctamente.")

                    // Muestra una notificación local al crear la cita
                    NotificationHelper.showSimpleNotification(
                        requireContext(),
                        "Nueva cita creada",
                        "Se agendó la actividad '${actividad.nombre}' para el $fecha a las $horaInicio."
                    )

                    // Programa recordatorios automáticos antes del evento
                    programarAlerta(actividad.id, nuevaCita.id, inicio)

                    // Si la actividad es recurrente, genera repeticiones
                    if (actividad.periodicidad != null && actividad.periodicidad != "Única") {
                        generarRepeticiones(actividad, inicio, fin)
                    }
                } else {
                    // Actualiza una cita existente
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
                    citaId = citaEditada.id
                    accion = "Edición"
                    toast("Cita actualizada correctamente.")

                    // Notificación de cita modificada
                    NotificationHelper.showSimpleNotification(
                        requireContext(),
                        "Cita reagendada",
                        "La actividad '${actividad.nombre}' se ha reagendado para el $fecha a las $horaInicio."
                    )

                    // Reprograma las alertas
                    programarAlerta(actividad.id, citaEditada.id, inicio)
                }

                // Registra el cambio en la auditoría del sistema
                repos.registrarAuditoria(
                    usuarioId = "admin123",
                    usuarioNombre = "Administrador",
                    modulo = "Citas",
                    accion = accion,
                    entidadId = citaId,
                    descripcion = "Se realizó una $accion de la cita de la actividad '${actividad.nombre}' (${actividad.lugar})",
                    cambios = mapOf(
                        "Estado" to estadoSeleccionado,
                        "Fecha" to fecha,
                        "Hora Inicio" to horaInicio,
                        "Hora Fin" to horaFin
                    )
                )

                // Vuelve al fragmento anterior informando que se guardó la cita
                findNavController().previousBackStackEntry?.savedStateHandle?.set("citaGuardada", true)
                findNavController().navigateUp()
            } catch (e: Exception) {
                Log.e("CITA_FORM", "Error al guardar cita", e)
                toast("Error al guardar cita: ${e.message}")
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    private suspend fun generarRepeticiones(actividad: Actividad, inicioBase: Long, finBase: Long) {
        // Genera citas repetidas según la periodicidad de la actividad
        val calInicio = Calendar.getInstance()
        val calFin = Calendar.getInstance()
        calInicio.timeInMillis = inicioBase
        calFin.timeInMillis = finBase

        val repeticiones = mutableListOf<Cita>()
        val cantidad = when (actividad.periodicidad) {
            "Semanal" -> 4
            "Mensual" -> 3
            else -> 0
        }

        repeat(cantidad) {
            when (actividad.periodicidad) {
                "Semanal" -> {
                    calInicio.add(Calendar.WEEK_OF_YEAR, 1)
                    calFin.add(Calendar.WEEK_OF_YEAR, 1)
                }
                "Mensual" -> {
                    calInicio.add(Calendar.MONTH, 1)
                    calFin.add(Calendar.MONTH, 1)
                }
            }

            val repetida = Cita(
                actividadId = actividad.id,
                fechaInicioMillis = calInicio.timeInMillis,
                fechaFinMillis = calFin.timeInMillis,
                lugar = actividad.lugar,
                observaciones = "Repetición automática (${actividad.periodicidad?.lowercase()})",
                duracionMin = actividad.duracionMin,
                estado = "Pendiente"
            )
            repeticiones.add(repetida)
        }

        if (repeticiones.isNotEmpty()) {
            for (cita in repeticiones) repos.crearCita(cita)
            toast("${repeticiones.size} citas recurrentes creadas automáticamente.")
        }
    }

    private fun confirmarEliminacion() {
        // Muestra un cuadro de diálogo antes de eliminar la cita
        AlertDialog.Builder(requireContext())
            .setTitle("Eliminar cita")
            .setMessage("¿Seguro que deseas eliminar esta cita?")
            .setPositiveButton("Eliminar") { _, _ -> eliminarCita() }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun eliminarCita() {
        // Elimina la cita actual y registra la acción en la auditoría
        citaExistente?.let { cita ->
            lifecycleScope.launch {
                progressBar.visibility = View.VISIBLE
                try {
                    repos.eliminarCita(cita.id)
                    toast("Cita eliminada correctamente.")

                    NotificationHelper.showSimpleNotification(
                        requireContext(),
                        "Cita cancelada",
                        "Se ha cancelado la cita de la actividad '${actividadSeleccionada?.nombre}'."
                    )

                    repos.registrarAuditoria(
                        usuarioId = "admin123",
                        usuarioNombre = "Administrador",
                        modulo = "Citas",
                        accion = "Eliminación",
                        entidadId = cita.id,
                        descripcion = "Se eliminó la cita de la actividad '${actividadSeleccionada?.nombre}' en ${cita.lugar}"
                    )

                    findNavController().navigateUp()
                } catch (e: Exception) {
                    toast("Error al eliminar cita: ${e.message}")
                } finally {
                    progressBar.visibility = View.GONE
                }
            }
        }
    }

    private fun diasEntreAhoraY(futuroMillis: Long): Int {
        val ahora = System.currentTimeMillis()
        val diffMs = futuroMillis - ahora
        val unDiaMs = 24 * 60 * 60 * 1000.0
        return ceil(diffMs / unDiaMs).toInt()
    }

    private fun toast(msg: String) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
    }

    private fun programarAlerta(actividadId: String, citaId: String, fechaHora: Long) {
        // Programa alertas locales: una 30 minutos antes y otra 1 día antes de la cita
        val delay30Min = (fechaHora - System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(30)).coerceAtLeast(0)
        val alerta30Min = OneTimeWorkRequestBuilder<cl.alercelab.centrointegral.notifications.AlertWorker>()
            .setInputData(workDataOf("actividadId" to actividadId, "citaId" to citaId, "tipo" to "30min"))
            .setInitialDelay(delay30Min, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(requireContext()).enqueue(alerta30Min)

        val delay1Dia = (fechaHora - System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1)).coerceAtLeast(0)
        val alerta1Dia = OneTimeWorkRequestBuilder<cl.alercelab.centrointegral.notifications.AlertWorker>()
            .setInputData(workDataOf("actividadId" to actividadId, "citaId" to citaId, "tipo" to "1dia"))
            .setInitialDelay(delay1Dia, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(requireContext()).enqueue(alerta1Dia)
    }
}