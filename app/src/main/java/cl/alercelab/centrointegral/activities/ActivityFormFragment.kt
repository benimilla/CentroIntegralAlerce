package cl.alercelab.centrointegral.activities

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
import cl.alercelab.centrointegral.domain.*
import kotlinx.coroutines.launch

class ActivityFormFragment : Fragment() {

    private lateinit var etNombre: EditText
    private lateinit var spTipo: Spinner
    private lateinit var spPeriodicidad: Spinner
    private lateinit var etCupo: EditText
    private lateinit var spOferente: Spinner
    private lateinit var spSocioComunitario: Spinner
    private lateinit var etBeneficiarios: EditText
    private lateinit var etDiasAvisoPrevio: EditText
    private lateinit var spLugar: Spinner
    private lateinit var etDuracion: EditText
    private lateinit var btnAgregarCita: Button
    private lateinit var btnGuardar: Button
    private lateinit var tvResumenCitas: TextView

    private val repos = Repos()
    private var listaCitas: MutableList<Cita> = mutableListOf()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_activity_form, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Referencias
        etNombre = view.findViewById(R.id.etNombre)
        spTipo = view.findViewById(R.id.spTipo)
        spPeriodicidad = view.findViewById(R.id.spPeriodicidad)
        etCupo = view.findViewById(R.id.etCupo)
        spOferente = view.findViewById(R.id.spOferente)
        spSocioComunitario = view.findViewById(R.id.spSocioComunitario)
        etBeneficiarios = view.findViewById(R.id.etBeneficiarios)
        etDiasAvisoPrevio = view.findViewById(R.id.etDiasAvisoPrevio)
        spLugar = view.findViewById(R.id.spLugar)
        etDuracion = view.findViewById(R.id.etDuracion)
        btnAgregarCita = view.findViewById(R.id.btnAgregarCita)
        btnGuardar = view.findViewById(R.id.btnGuardar)

        // 🔹 Buscamos el contenedor interior (LinearLayout)
        val linearContainer = (view as? ScrollView)?.getChildAt(0) as? LinearLayout
            ?: view.findViewById(R.id.linearContainer) // si le pones un id al LinearLayout principal

        // 🔹 Creamos un texto dinámico dentro del contenedor
        tvResumenCitas = TextView(requireContext()).apply {
            text = "Citas agregadas: 0"
            textSize = 14f
            setPadding(0, 8, 0, 8)
        }

        // ✅ Agregar dentro del LinearLayout (no al ScrollView)
        linearContainer.addView(tvResumenCitas, linearContainer.childCount - 1)

        // 🔹 Llenar spinner de periodicidad
        val periodicidades = listOf("Única", "Semanal", "Mensual")
        spPeriodicidad.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            periodicidades
        )

        // 🔹 Cargar datos desde Firestore
        lifecycleScope.launch {
            try {
                val lugares = repos.obtenerLugares()
                val tipos = repos.obtenerTiposActividad()
                val oferentes = repos.obtenerOferentes()
                val socios = repos.obtenerSociosComunitarios()

                spTipo.adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_dropdown_item,
                    tipos.map { it.nombre }
                )

                spOferente.adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_dropdown_item,
                    oferentes.map { it.nombre }
                )

                spLugar.adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_dropdown_item,
                    lugares.map { it.nombre }
                )

                val sociosOpciones = mutableListOf("Sin socio comunitario")
                sociosOpciones.addAll(socios.map { it.nombre })
                spSocioComunitario.adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_dropdown_item,
                    sociosOpciones
                )

            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "Error cargando opciones: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        // 🔹 Agregar cita
        btnAgregarCita.setOnClickListener {
            findNavController().navigate(R.id.action_activityFormFragment_to_citaFormFragment)
        }

        // 🔹 Escuchar citas nuevas
        findNavController().currentBackStackEntry?.savedStateHandle
            ?.getLiveData<Cita>("nuevaCita")
            ?.observe(viewLifecycleOwner) { cita ->
                listaCitas.add(cita)
                tvResumenCitas.text = "Citas agregadas: ${listaCitas.size}"
                Toast.makeText(
                    requireContext(),
                    "Cita agregada (${listaCitas.size})",
                    Toast.LENGTH_SHORT
                ).show()
            }

        // 🔹 Guardar
        btnGuardar.setOnClickListener { guardarActividad() }
    }

    private fun guardarActividad() {
        val nombre = etNombre.text.toString().trim()
        val tipo = spTipo.selectedItem?.toString() ?: ""
        val periodicidad = spPeriodicidad.selectedItem?.toString() ?: ""
        val cupo = etCupo.text.toString().toIntOrNull()
        val oferente = spOferente.selectedItem?.toString()
        val lugar = spLugar.selectedItem?.toString() ?: ""
        val beneficiariosTexto = etBeneficiarios.text.toString()
        val beneficiarios =
            beneficiariosTexto.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val diasAviso = etDiasAvisoPrevio.text.toString().toIntOrNull() ?: 0

        val socioSeleccionado = spSocioComunitario.selectedItem?.toString() ?: ""
        val socioComunitario =
            if (socioSeleccionado == "Sin socio comunitario") null else socioSeleccionado

        if (nombre.isEmpty() || tipo.isEmpty() || oferente.isNullOrEmpty() || lugar.isEmpty()) {
            Toast.makeText(
                requireContext(),
                "Completa todos los campos obligatorios",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val citasFinales = mutableListOf<Cita>().apply { addAll(listaCitas) }

        if (periodicidad != "Única" && listaCitas.isNotEmpty()) {
            val baseCita = listaCitas.first()
            val intervalo = when (periodicidad) {
                "Semanal" -> 7 * 24 * 60 * 60 * 1000L
                "Mensual" -> 30 * 24 * 60 * 60 * 1000L
                else -> 0L
            }
            if (intervalo > 0) {
                for (i in 1..3) {
                    citasFinales.add(
                        baseCita.copy(
                            fechaInicioMillis = baseCita.fechaInicioMillis + intervalo * i,
                            fechaFinMillis = baseCita.fechaFinMillis + intervalo * i
                        )
                    )
                }
            }
        }

        val actividad = Actividad(
            id = "",
            nombre = nombre,
            tipo = tipo,
            periodicidad = periodicidad,
            cupo = cupo,
            oferente = oferente,
            socioComunitario = socioComunitario,
            beneficiarios = beneficiarios,
            diasAvisoPrevio = diasAviso,
            lugar = lugar,
            estado = "activa"
        )

        lifecycleScope.launch {
            try {
                repos.crearActividadConCitas(actividad, citasFinales)
                Toast.makeText(
                    requireContext(),
                    "Actividad y citas creadas con éxito",
                    Toast.LENGTH_LONG
                ).show()
                findNavController().popBackStack()
            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "Error al guardar: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
