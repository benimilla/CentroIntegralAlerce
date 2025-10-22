package cl.alercelab.centrointegral.activities

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import cl.alercelab.centrointegral.data.Repos
import cl.alercelab.centrointegral.databinding.FragmentActivityFormBinding
import cl.alercelab.centrointegral.domain.Actividad
import cl.alercelab.centrointegral.domain.Cita
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ActivityFormFragment : Fragment() {

    private var _binding: FragmentActivityFormBinding? = null
    private val binding get() = _binding!!
    private val repos = Repos()
    private val citas = mutableListOf<Cita>()
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentActivityFormBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 🔹 Configurar selectores
        val tipos = listOf("Taller", "Reunión", "Asesoría", "Capacitación", "Otro")
        val periodicidades = listOf("Puntual", "Periódica")
        val lugares = listOf("Centro Integral Alerce", "Sede Comunitaria", "Escuela", "Otro")

        binding.spTipo.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, tipos)
        binding.spPeriodicidad.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, periodicidades)
        binding.spLugar.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, lugares)

        binding.btnAgregarCita.setOnClickListener { mostrarDialogoCita() }
        binding.btnGuardar.setOnClickListener { guardarActividad() }
    }

    private fun mostrarDialogoCita() {
        val cal = Calendar.getInstance()

        // Paso 1: Fecha del día
        DatePickerDialog(requireContext(), { _, y, m, d ->
            cal.set(Calendar.YEAR, y)
            cal.set(Calendar.MONTH, m)
            cal.set(Calendar.DAY_OF_MONTH, d)

            // Paso 2: Hora de inicio
            TimePickerDialog(requireContext(), { _, hIni, minIni ->
                val inicioCal = Calendar.getInstance().apply {
                    set(y, m, d, hIni, minIni)
                }

                // Paso 3: Hora de fin
                TimePickerDialog(requireContext(), { _, hFin, minFin ->
                    val finCal = Calendar.getInstance().apply {
                        set(y, m, d, hFin, minFin)
                    }

                    if (finCal.timeInMillis <= inicioCal.timeInMillis) {
                        Toast.makeText(requireContext(), "La hora de fin debe ser posterior a la de inicio", Toast.LENGTH_SHORT).show()
                        return@TimePickerDialog
                    }

                    val lugar = binding.spLugar.selectedItem.toString()

                    val cita = Cita(
                        fechaInicioMillis = inicioCal.timeInMillis,
                        fechaFinMillis = finCal.timeInMillis,
                        lugar = lugar
                    )

                    citas.add(cita)
                    Toast.makeText(
                        requireContext(),
                        "Cita agregada: ${dateFormat.format(inicioCal.time)} - ${dateFormat.format(finCal.time)}",
                        Toast.LENGTH_SHORT
                    ).show()

                }, hIni + 1, minIni, true).show() // default fin = 1h después
            },
                cal.get(Calendar.HOUR_OF_DAY),
                cal.get(Calendar.MINUTE),
                true
            ).show()
        },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun guardarActividad() {
        val nombre = binding.etNombre.text.toString().trim()
        val tipo = binding.spTipo.selectedItem.toString()
        val periodicidad = binding.spPeriodicidad.selectedItem.toString()
        val cupo = binding.etCupo.text.toString().toIntOrNull() ?: 0
        val oferente = binding.etOferente.text.toString().trim()
        val socio = binding.etSocioComunitario.text.toString().trim()
        val beneficiarios = binding.etBeneficiarios.text.toString()
            .split(",").map { it.trim() }.filter { it.isNotBlank() }
        val diasAviso = binding.etDiasAvisoPrevio.text.toString().toIntOrNull() ?: 0

        if (nombre.isEmpty() || citas.isEmpty()) {
            Toast.makeText(requireContext(), "Debe ingresar todos los datos y al menos una cita", Toast.LENGTH_SHORT).show()
            return
        }

        if (periodicidad.equals("Puntual", true) && citas.size > 1) {
            Toast.makeText(requireContext(), "Las actividades puntuales solo deben tener una cita", Toast.LENGTH_SHORT).show()
            return
        }

        val fechaInicio = citas.minOf { it.fechaInicioMillis }
        val fechaFin = citas.maxOf { it.fechaFinMillis }

        val actividad = Actividad(
            nombre = nombre,
            tipo = tipo,
            periodicidad = periodicidad,
            cupo = cupo,
            oferente = oferente,
            socioComunitario = socio,
            beneficiarios = beneficiarios,
            diasAvisoPrevio = diasAviso,
            fechaInicio = fechaInicio,
            fechaFin = fechaFin,
            estado = "vigente"
        )

        lifecycleScope.launch {
            try {
                repos.crearActividadConCitas(actividad, citas)
                Toast.makeText(requireContext(), "Actividad creada correctamente", Toast.LENGTH_LONG).show()
                requireActivity().onBackPressedDispatcher.onBackPressed()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error al crear: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
