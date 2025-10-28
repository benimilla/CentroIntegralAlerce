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

        // 🔹 Llenar spinner de periodicidad
        val periodicidades = listOf("Única", "Semanal", "Mensual")
        spPeriodicidad.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            periodicidades
        )

        // 🔹 Cargar datos Firestore
        lifecycleScope.launch {
            try {
                val lugares = repos.obtenerLugares()
                val tipos = repos.obtenerTiposActividad()
                val oferentes = repos.obtenerOferentes()
                val socios = repos.obtenerSociosComunitarios()

                // Tipos
                spTipo.adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_dropdown_item,
                    tipos.map { it.nombre }
                )

                // Oferentes
                spOferente.adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_dropdown_item,
                    oferentes.map { it.nombre }
                )

                // Lugares
                spLugar.adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_dropdown_item,
                    lugares.map { it.nombre }
                )

                // Socios comunitarios (opcional)
                val sociosOpciones = mutableListOf("Sin socio comunitario")
                sociosOpciones.addAll(socios.map { it.nombre })
                spSocioComunitario.adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_dropdown_item,
                    sociosOpciones
                )

            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error cargando opciones: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        // 🔹 Acción del botón agregar cita (placeholder)
        btnAgregarCita.setOnClickListener {
            Toast.makeText(requireContext(), "Función de agregar cita próximamente", Toast.LENGTH_SHORT).show()
        }

        btnGuardar.setOnClickListener {
            guardarActividad()
        }
    }

    private fun guardarActividad() {
        val nombre = etNombre.text.toString().trim()
        val tipo = spTipo.selectedItem?.toString() ?: ""
        val periodicidad = spPeriodicidad.selectedItem?.toString() ?: ""
        val cupo = etCupo.text.toString().toIntOrNull()
        val oferente = spOferente.selectedItem?.toString()
        val lugar = spLugar.selectedItem?.toString() ?: ""
        val beneficiariosTexto = etBeneficiarios.text.toString()
        val beneficiarios = beneficiariosTexto.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val diasAviso = etDiasAvisoPrevio.text.toString().toIntOrNull() ?: 0

        val socioSeleccionado = spSocioComunitario.selectedItem?.toString() ?: ""
        val socioComunitario = if (socioSeleccionado == "Sin socio comunitario") null else socioSeleccionado

        if (nombre.isEmpty() || tipo.isEmpty() || oferente.isNullOrEmpty() || lugar.isEmpty()) {
            Toast.makeText(requireContext(), "Completa todos los campos obligatorios", Toast.LENGTH_SHORT).show()
            return
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
                repos.crearActividadConCitas(actividad, listaCitas)
                Toast.makeText(requireContext(), "Actividad creada con éxito", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error al guardar: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
