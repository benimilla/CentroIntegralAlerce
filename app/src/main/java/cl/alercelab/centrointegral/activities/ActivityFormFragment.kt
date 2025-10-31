package cl.alercelab.centrointegral.activities

import android.app.AlertDialog
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
    private lateinit var btnAgregarCita: Button
    private lateinit var btnGuardar: Button
    private lateinit var tvTituloActividad: TextView

    private var citas: MutableList<Cita> = mutableListOf()
    private var actividadExistente: Actividad? = null

    private val repos = Repos()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_activity_form, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Asignación de vistas
        etNombre = view.findViewById(R.id.etNombre)
        spTipo = view.findViewById(R.id.spTipo)
        spPeriodicidad = view.findViewById(R.id.spPeriodicidad)
        etCupo = view.findViewById(R.id.etCupo)
        spOferente = view.findViewById(R.id.spOferente)
        spSocioComunitario = view.findViewById(R.id.spSocioComunitario)
        etBeneficiarios = view.findViewById(R.id.etBeneficiarios)
        etDiasAvisoPrevio = view.findViewById(R.id.etDiasAvisoPrevio)
        spLugar = view.findViewById(R.id.spLugar)
        btnAgregarCita = view.findViewById(R.id.btnAgregarCita)
        btnGuardar = view.findViewById(R.id.btnGuardar)
        tvTituloActividad = view.findViewById(R.id.tvTituloActividad)

        // Verificar si estamos editando
        val actividadId = arguments?.getString("actividadId")
        if (actividadId != null) {
            cargarActividadExistente(actividadId)
        }

        btnAgregarCita.setOnClickListener {
            val bundle = Bundle().apply {
                putBoolean("desdeActividad", true)
            }
            findNavController().navigate(R.id.action_activityFormFragment_to_citaFormFragment, bundle)
        }

        btnGuardar.setOnClickListener {
            guardarActividad()
        }

        // Observa las citas agregadas desde CitaFormFragment
        findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<Cita>("nuevaCita")
            ?.observe(viewLifecycleOwner) { cita ->
                citas.add(cita)
                Toast.makeText(requireContext(), "Cita agregada correctamente", Toast.LENGTH_SHORT).show()
            }

        cargarListasDesplegables()
    }

    private fun cargarListasDesplegables() {
        lifecycleScope.launch {
            val tipos = repos.obtenerTiposActividad().map { it.nombre }
            val oferentes = repos.obtenerOferentes().map { it.nombre }
            val socios = repos.obtenerSociosComunitarios().map { it.nombre }
            val lugares = repos.obtenerLugares().map { it.nombre }

            spTipo.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, tipos)
            spPeriodicidad.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, listOf("Única", "Semanal", "Mensual"))
            spOferente.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, oferentes)
            spSocioComunitario.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, socios)
            spLugar.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, lugares)
        }
    }

    private fun cargarActividadExistente(id: String) {
        lifecycleScope.launch {
            val act = repos.obtenerActividadPorId(id)
            if (act != null) {
                actividadExistente = act
                tvTituloActividad.text = "Editar Actividad"
                etNombre.setText(act.nombre)
                etCupo.setText(act.cupo?.toString() ?: "")
                etBeneficiarios.setText(act.beneficiarios.joinToString(", "))
                etDiasAvisoPrevio.setText(act.diasAvisoPrevio.toString())
            }
        }
    }

    private fun guardarActividad() {
        val nombre = etNombre.text.toString().trim()
        val tipo = spTipo.selectedItem?.toString() ?: ""
        val periodicidad = spPeriodicidad.selectedItem?.toString() ?: ""
        val cupo = etCupo.text.toString().toIntOrNull()
        val oferente = spOferente.selectedItem?.toString()
        val socio = spSocioComunitario.selectedItem?.toString()
        val beneficiarios = etBeneficiarios.text.toString().split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val diasAviso = etDiasAvisoPrevio.text.toString().toIntOrNull() ?: 0
        val lugar = spLugar.selectedItem?.toString() ?: ""

        if (nombre.isEmpty() || tipo.isEmpty() || lugar.isEmpty()) {
            Toast.makeText(requireContext(), "Completa los campos obligatorios", Toast.LENGTH_SHORT).show()
            return
        }

        val nuevaActividad = Actividad(
            id = actividadExistente?.id ?: "",
            nombre = nombre,
            tipo = tipo,
            periodicidad = periodicidad,
            cupo = cupo,
            oferente = oferente,
            socioComunitario = socio,
            beneficiarios = beneficiarios,
            diasAvisoPrevio = diasAviso,
            lugar = lugar,
            fechaInicio = System.currentTimeMillis(),
            citas = citas
        )

        lifecycleScope.launch {
            try {
                if (actividadExistente == null) {
                    repos.crearActividadConCitas(nuevaActividad, citas)
                } else {
                    repos.actualizarActividad(nuevaActividad.id, nuevaActividad)
                }
                Toast.makeText(requireContext(), "Actividad guardada correctamente", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error al guardar: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
