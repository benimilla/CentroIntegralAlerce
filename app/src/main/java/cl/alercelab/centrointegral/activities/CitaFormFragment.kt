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
import cl.alercelab.centrointegral.data.Repos
import cl.alercelab.centrointegral.domain.Actividad
import cl.alercelab.centrointegral.domain.Cita
import kotlinx.coroutines.launch
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.ceil

class CitaFormFragment : Fragment() {

    private lateinit var spActividad: Spinner
    private lateinit var tvLugar: TextView
    private lateinit var tvAvisoPrevio: TextView
    private lateinit var tvDuracionMax: TextView
    private lateinit var etFecha: EditText
    private lateinit var etHoraInicio: EditText
    private lateinit var etHoraFin: EditText
    private lateinit var etObservaciones: EditText
    private lateinit var btnGuardar: Button
    private lateinit var progressBar: ProgressBar

    private val repos = Repos()
    private var actividades: List<Actividad> = emptyList()
    private var actividadSeleccionada: Actividad? = null

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
        btnGuardar = view.findViewById(R.id.btnGuardar)
        progressBar = view.findViewById(R.id.progressBar)

        configurarPickers()
        cargarActividades()
        btnGuardar.setOnClickListener { guardarCita() }
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

                // Adaptador con layout personalizado (texto oscuro, visible)
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

            } catch (e: Exception) {
                toast("Error al cargar actividades: ${e.message}")
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    /** 📋 Muestra datos de la actividad seleccionada */
    private fun actualizarInfoActividad() {
        actividadSeleccionada?.let {
            tvLugar.text = "Lugar: ${it.lugar}"
            tvAvisoPrevio.text = "Aviso previo: ${it.diasAvisoPrevio} día(s)"
            tvDuracionMax.text = "Duración máxima: ${it.duracionMin ?: 0} min"
        }
    }

    /** 💾 Guarda la cita luego de validar */
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

        lifecycleScope.launch {
            progressBar.visibility = View.VISIBLE
            try {
                val conflicto = repos.hayConflictoCita(actividad.lugar, inicio, fin)
                if (conflicto) {
                    toast("Conflicto: ya hay una cita en ese lugar y horario.")
                    return@launch
                }

                val cita = Cita(
                    actividadId = actividad.id,
                    fechaInicioMillis = inicio,
                    fechaFinMillis = fin,
                    lugar = actividad.lugar,
                    observaciones = etObservaciones.text?.toString(),
                    duracionMin = duracion
                )

                findNavController().previousBackStackEntry?.savedStateHandle?.set("nuevaCita", cita)
                toast("Cita agregada correctamente.")
                findNavController().navigateUp()

            } catch (e: Exception) {
                toast("Error al guardar cita: ${e.message}")
            } finally {
                progressBar.visibility = View.GONE
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
