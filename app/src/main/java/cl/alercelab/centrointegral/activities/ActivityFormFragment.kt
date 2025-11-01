package cl.alercelab.centrointegral.activities

import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import cl.alercelab.centrointegral.R
import cl.alercelab.centrointegral.data.Repos
import cl.alercelab.centrointegral.domain.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ActivityFormFragment : Fragment() {

    private lateinit var etNombre: EditText
    private lateinit var spTipo: Spinner
    private lateinit var spPeriodicidad: Spinner
    private lateinit var layoutFrecuencia: LinearLayout
    private lateinit var spFrecuencia: Spinner
    private lateinit var etCupo: EditText
    private lateinit var spOferente: Spinner
    private lateinit var spSocioComunitario: Spinner
    private lateinit var etBeneficiarios: EditText
    private lateinit var etDiasAvisoPrevio: EditText
    private lateinit var spLugar: Spinner
    private lateinit var etDuracion: EditText
    private lateinit var spEstado: Spinner
    private lateinit var etMotivoCancelacion: EditText
    private lateinit var btnAgregarCita: Button
    private lateinit var btnGuardar: Button
    private lateinit var tvTituloActividad: TextView
    private lateinit var tvResumenCitas: TextView
    private lateinit var progressBar: ProgressBar

    private var citas: MutableList<Cita> = mutableListOf()
    private var actividadExistente: Actividad? = null
    private val repos = Repos()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_activity_form, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        asignarVistas(view)
        inicializarSpinners()
        configurarEventos()
        cargarListasDesplegables()

        val actividadId = arguments?.getString("actividadId")
        if (actividadId != null) cargarActividadExistente(actividadId)
    }

    /** 🔹 Vincula vistas del layout */
    private fun asignarVistas(v: View) {
        etNombre = v.findViewById(R.id.etNombre)
        spTipo = v.findViewById(R.id.spTipo)
        spPeriodicidad = v.findViewById(R.id.spPeriodicidad)
        layoutFrecuencia = v.findViewById(R.id.layoutFrecuencia)
        spFrecuencia = v.findViewById(R.id.spFrecuencia)
        etCupo = v.findViewById(R.id.etCupo)
        spOferente = v.findViewById(R.id.spOferente)
        spSocioComunitario = v.findViewById(R.id.spSocioComunitario)
        etBeneficiarios = v.findViewById(R.id.etBeneficiarios)
        etDiasAvisoPrevio = v.findViewById(R.id.etDiasAvisoPrevio)
        spLugar = v.findViewById(R.id.spLugar)
        etDuracion = v.findViewById(R.id.etDuracion)
        spEstado = v.findViewById(R.id.spEstado)
        etMotivoCancelacion = v.findViewById(R.id.etMotivoCancelacion)
        btnAgregarCita = v.findViewById(R.id.btnAgregarCita)
        btnGuardar = v.findViewById(R.id.btnGuardar)
        tvTituloActividad = v.findViewById(R.id.tvTituloActividad)
        tvResumenCitas = v.findViewById(R.id.tvResumenCitas)
        progressBar = v.findViewById(R.id.progressBar)
    }

    /** 🌀 Inicializa los spinners de periodicidad, frecuencia y estado */
    private fun inicializarSpinners() {
        val periodicidades = listOf("Única", "Semanal", "Mensual")
        val adapterPeriodicidad = ArrayAdapter(requireContext(), R.layout.spinner_item_custom, periodicidades)
        adapterPeriodicidad.setDropDownViewResource(R.layout.spinner_item_custom)
        spPeriodicidad.adapter = adapterPeriodicidad

        // Maneja el cambio de periodicidad
        spPeriodicidad.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                when (periodicidades[pos]) {
                    "Semanal" -> mostrarOpcionesFrecuencia(listOf("Cada semana", "1 semana sí / 1 no", "Cada 3 semanas"))
                    "Mensual" -> mostrarOpcionesFrecuencia(listOf("Cada mes", "Cada 2 meses", "Cada 3 meses"))
                    else -> layoutFrecuencia.visibility = View.GONE
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        val estados = listOf("activa", "inactiva", "cancelada")
        val adapterEstado = ArrayAdapter(requireContext(), R.layout.spinner_item_custom, estados)
        adapterEstado.setDropDownViewResource(R.layout.spinner_item_custom)
        spEstado.adapter = adapterEstado

        spEstado.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                etMotivoCancelacion.visibility = if (estados[pos] == "cancelada") View.VISIBLE else View.GONE
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    /** 🔄 Muestra el bloque de frecuencia solo si aplica */
    private fun mostrarOpcionesFrecuencia(opciones: List<String>) {
        layoutFrecuencia.visibility = View.VISIBLE
        val adapter = ArrayAdapter(requireContext(), R.layout.spinner_item_custom, opciones)
        adapter.setDropDownViewResource(R.layout.spinner_item_custom)
        spFrecuencia.adapter = adapter
    }

    /** ⚙️ Configura botones y observadores */
    private fun configurarEventos() {
        btnAgregarCita.setOnClickListener {
            val bundle = Bundle().apply { putBoolean("desdeActividad", true) }
            findNavController().navigate(R.id.action_activityFormFragment_to_citaFormFragment, bundle)
        }

        btnGuardar.setOnClickListener { guardarActividad() }

        // Recibe cita nueva desde CitaFormFragment
        findNavController().currentBackStackEntry?.savedStateHandle
            ?.getLiveData<Cita>("nuevaCita")
            ?.observe(viewLifecycleOwner) { cita ->
                val normalizada = cita.copy(
                    observaciones = cita.observaciones?.trim().takeUnless { it.isNullOrEmpty() } ?: ""
                )
                citas.add(normalizada)

                val ultima = citas.lastOrNull()
                val fecha = ultima?.let {
                    SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(it.fechaInicioMillis))
                }
                tvResumenCitas.text = if (fecha != null)
                    "📅 ${citas.size} citas (última: $fecha en ${ultima.lugar})"
                else
                    "Citas agregadas: ${citas.size}"
                Toast.makeText(requireContext(), "Cita agregada correctamente", Toast.LENGTH_SHORT).show()
            }
    }

    /** 📦 Carga listas de oferentes, lugares, etc. */
    private fun cargarListasDesplegables() {
        lifecycleScope.launch {
            try {
                val tipos = repos.obtenerTiposActividad().map { it.nombre }
                val oferentes = repos.obtenerOferentes().map { it.nombre }
                val socios = repos.obtenerSociosComunitarios().map { it.nombre }
                val lugares = repos.obtenerLugares().map { it.nombre }

                fun adaptador(lista: List<String>) = ArrayAdapter(requireContext(), R.layout.spinner_item_custom, lista)
                    .apply { setDropDownViewResource(R.layout.spinner_item_custom) }

                spTipo.adapter = adaptador(tipos)
                spOferente.adapter = adaptador(oferentes)
                spSocioComunitario.adapter = adaptador(socios)
                spLugar.adapter = adaptador(lugares)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error cargando listas: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    /** 🧭 Carga datos de una actividad existente para editarla */
    private fun cargarActividadExistente(id: String) {
        lifecycleScope.launch {
            progressBar.visibility = View.VISIBLE
            try {
                val act = repos.obtenerActividadPorId(id)
                if (act != null) {
                    actividadExistente = act
                    tvTituloActividad.text = "Editar Actividad"
                    etNombre.setText(act.nombre)
                    etCupo.setText(act.cupo?.toString() ?: "")
                    etBeneficiarios.setText(act.beneficiarios.joinToString(", "))
                    etDiasAvisoPrevio.setText(act.diasAvisoPrevio.toString())
                    etDuracion.setText(act.duracionMin?.toString() ?: "")
                    val posEstado = listOf("activa", "inactiva", "cancelada").indexOf(act.estado)
                    if (posEstado >= 0) spEstado.setSelection(posEstado)
                    etMotivoCancelacion.setText(act.motivoCancelacion ?: "")

                    // ✅ Actualizar periodicidad y frecuencia
                    val periodicidades = listOf("Única", "Semanal", "Mensual")
                    val posPeriodicidad = periodicidades.indexOf(act.periodicidad)
                    if (posPeriodicidad >= 0) spPeriodicidad.setSelection(posPeriodicidad)

                    // Mostrar opciones de frecuencia según periodicidad
                    when (act.periodicidad) {
                        "Semanal" -> mostrarOpcionesFrecuencia(listOf("Cada semana", "1 semana sí / 1 no", "Cada 3 semanas"))
                        "Mensual" -> mostrarOpcionesFrecuencia(listOf("Cada mes", "Cada 2 meses", "Cada 3 meses"))
                        else -> layoutFrecuencia.visibility = View.GONE
                    }

                    // Seleccionar frecuencia si existe
                    act.frecuencia?.let { freq ->
                        val adapter = spFrecuencia.adapter as? ArrayAdapter<String>
                        val posFreq = adapter?.getPosition(freq) ?: -1
                        if (posFreq >= 0) spFrecuencia.setSelection(posFreq)
                    }

                    // Cargar citas asociadas desde Firestore
                    val citasAsociadas = repos.obtenerCitasPorActividad(act.id)
                    citas.clear()
                    citas.addAll(citasAsociadas)
                    tvResumenCitas.text = "📅 ${citas.size} citas cargadas"
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error al cargar la actividad: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    /** 💾 Validación y guardado final de la actividad */
    private fun guardarActividad() {
        val nombre = etNombre.text.toString().trim()
        val tipo = spTipo.selectedItem?.toString()?.trim() ?: ""
        val periodicidad = spPeriodicidad.selectedItem?.toString()?.trim() ?: ""
        val frecuencia = if (layoutFrecuencia.visibility == View.VISIBLE) spFrecuencia.selectedItem?.toString() else null
        val cupo = etCupo.text.toString().toIntOrNull()
        val oferente = spOferente.selectedItem?.toString()?.trim()
        val socio = spSocioComunitario.selectedItem?.toString()?.trim()
        val beneficiarios = etBeneficiarios.text.toString().split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val diasAviso = etDiasAvisoPrevio.text.toString().toIntOrNull() ?: 0
        val lugar = spLugar.selectedItem?.toString()?.trim() ?: ""
        val duracion = etDuracion.text.toString().toIntOrNull()
        val estado = spEstado.selectedItem?.toString() ?: "activa"
        val motivoCancelacion = etMotivoCancelacion.text.toString().takeIf { it.isNotBlank() }

        when {
            nombre.isEmpty() -> { etNombre.error = "Campo obligatorio"; return }
            tipo.isEmpty() -> { mostrarError("Selecciona un tipo"); return }
            cupo == null || cupo <= 0 -> { etCupo.error = "Debe ser mayor a 0"; return }
            duracion == null || duracion <= 0 -> { etDuracion.error = "Debe ser mayor a 0"; return }
            lugar.isEmpty() -> { mostrarError("Selecciona un lugar"); return }
            beneficiarios.isEmpty() -> { etBeneficiarios.error = "Agrega beneficiarios"; return }
            estado == "cancelada" && motivoCancelacion.isNullOrBlank() -> { mostrarError("Debes indicar el motivo de cancelación"); return }
        }

        if (citas.isEmpty()) {
            Toast.makeText(requireContext(), "Nota: no se agregaron citas a esta actividad aún.", Toast.LENGTH_SHORT).show()
        }

        guardarEnFirestore(
            nombre, tipo, periodicidad, frecuencia, cupo, oferente, socio,
            beneficiarios, diasAviso, lugar, duracion, estado, motivoCancelacion
        )
    }

    private fun mostrarError(msg: String) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
    }

    /** 🚀 Guarda o actualiza la actividad en Firestore */
    private fun guardarEnFirestore(
        nombre: String, tipo: String, periodicidad: String, frecuencia: String?, cupo: Int?, oferente: String?,
        socio: String?, beneficiarios: List<String>, diasAviso: Int, lugar: String, duracion: Int?,
        estado: String, motivo: String?
    ) {
        val actividad = Actividad(
            id = actividadExistente?.id ?: "",
            nombre = nombre,
            tipo = tipo,
            periodicidad = periodicidad,
            frecuencia = frecuencia,
            cupo = cupo,
            oferente = oferente,
            socioComunitario = socio,
            beneficiarios = beneficiarios,
            diasAvisoPrevio = diasAviso,
            lugar = lugar,
            duracionMin = duracion,
            estado = estado,
            motivoCancelacion = motivo,
            fechaInicio = System.currentTimeMillis(),
            citas = emptyList()
        )

        lifecycleScope.launch {
            try {
                progressBar.visibility = View.VISIBLE
                btnGuardar.isEnabled = false
                btnAgregarCita.isEnabled = false

                if (actividadExistente == null) {
                    repos.crearActividadConCitas(actividad, citas)
                } else {
                    repos.actualizarActividad(actividad.id, actividad.copy(citas = emptyList()))
                    if (citas.isNotEmpty()) {
                        for (c in citas) {
                            if (c.id.isBlank()) {
                                repos.crearCita(c.copy(actividadId = actividad.id))
                            }
                        }
                    }
                }

                Toast.makeText(requireContext(), "Actividad guardada correctamente", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            } catch (e: Exception) {
                mostrarError("Error al guardar: ${e.message}")
            } finally {
                progressBar.visibility = View.GONE
                btnGuardar.isEnabled = true
                btnAgregarCita.isEnabled = true
            }
        }
    }
}
