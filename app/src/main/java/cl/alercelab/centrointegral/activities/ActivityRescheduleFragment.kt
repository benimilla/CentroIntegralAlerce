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
import cl.alercelab.centrointegral.domain.Cita
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class ActivityRescheduleFragment : Fragment() {

    private lateinit var tvActividad: TextView
    private lateinit var spCitas: Spinner
    private lateinit var tvFecha: TextView
    private lateinit var tvHoraInicio: TextView
    private lateinit var tvHoraFin: TextView
    private lateinit var etLugar: EditText
    private lateinit var btnGuardar: Button

    private val db = FirebaseFirestore.getInstance()
    private var actividadId: String? = null
    private var citaId: String? = null
    private var fechaInicio: Long? = null
    private var fechaFin: Long? = null

    private val sdfFecha = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
    private val sdfHora = SimpleDateFormat("HH:mm", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val v = inflater.inflate(R.layout.fragment_activity_reschedule, container, false)

        tvActividad = v.findViewById(R.id.tvActividad)
        spCitas = v.findViewById(R.id.spCitas)
        tvFecha = v.findViewById(R.id.tvFecha)
        tvHoraInicio = v.findViewById(R.id.tvHoraInicio)
        tvHoraFin = v.findViewById(R.id.tvHoraFin)
        etLugar = v.findViewById(R.id.etLugar)
        btnGuardar = v.findViewById(R.id.btnGuardar)

        actividadId = arguments?.getString("actividadId")

        // Muestra los selectores de fecha y hora cuando se hace clic
        tvFecha.setOnClickListener { pickDate() }
        tvHoraInicio.setOnClickListener { pickTime(isStart = true) }
        tvHoraFin.setOnClickListener { pickTime(isStart = false) }

        // Guarda los cambios al presionar el botón
        btnGuardar.setOnClickListener { onSave() }

        loadCitas()
        return v
    }

    private fun loadCitas() {
        val id = actividadId ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val actDoc = db.collection("actividades").document(id).get().await()
                tvActividad.text = actDoc.getString("nombre") ?: "Actividad"

                val snaps = db.collection("citas")
                    .whereEqualTo("actividadId", id)
                    .get()
                    .await()

                val citas = snaps.toObjects(Cita::class.java)
                if (citas.isEmpty()) {
                    // Si no hay citas, se muestra mensaje y se cierra el fragmento
                    Toast.makeText(requireContext(), "Esta actividad no tiene citas", Toast.LENGTH_LONG).show()
                    findNavController().popBackStack()
                    return@launch
                }

                val items = citas.map { it.id to it }
                spCitas.adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_dropdown_item,
                    items.map { "Cita ${it.first.take(6)}..." }
                )

                // Al seleccionar una cita, se actualizan los campos del formulario
                spCitas.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                        val sel = items[position].second
                        citaId = sel.id
                        etLugar.setText(sel.lugar)
                        fechaInicio = sel.fechaInicioMillis
                        fechaFin = sel.fechaFinMillis
                        tvFecha.text = sdfFecha.format(Date(sel.fechaInicioMillis))
                        tvHoraInicio.text = sdfHora.format(Date(sel.fechaInicioMillis))
                        tvHoraFin.text = sdfHora.format(Date(sel.fechaFinMillis))
                    }

                    override fun onNothingSelected(parent: AdapterView<*>) {}
                }

            } catch (e: Exception) {
                // Muestra un error si no se pueden cargar las citas
                Toast.makeText(requireContext(), "Error al cargar citas: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun pickDate() {
        // Abre un selector de fecha y guarda el valor seleccionado
        val cal = Calendar.getInstance()
        DatePickerDialog(requireContext(), { _, y, m, d ->
            cal.set(y, m, d)
            fechaInicio = cal.timeInMillis
            tvFecha.text = "%02d-%02d-%04d".format(d, m + 1, y)
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun pickTime(isStart: Boolean) {
        // Abre un selector de hora y guarda la hora de inicio o fin según el parámetro
        val cal = Calendar.getInstance()
        TimePickerDialog(requireContext(), { _, h, m ->
            cal.set(Calendar.HOUR_OF_DAY, h)
            cal.set(Calendar.MINUTE, m)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            if (isStart) {
                fechaInicio = cal.timeInMillis
                tvHoraInicio.text = "%02d:%02d".format(h, m)
            } else {
                fechaFin = cal.timeInMillis
                tvHoraFin.text = "%02d:%02d".format(h, m)
            }
        }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
    }

    private fun onSave() {
        val cId = citaId ?: run {
            Toast.makeText(requireContext(), "Seleccione una cita", Toast.LENGTH_SHORT).show()
            return
        }

        val lugar = etLugar.text.toString().ifBlank { "Centro" }
        val inicio = fechaInicio ?: run {
            Toast.makeText(requireContext(), "Seleccione fecha/hora de inicio", Toast.LENGTH_SHORT).show()
            return
        }
        val fin = fechaFin ?: run {
            Toast.makeText(requireContext(), "Seleccione hora de fin", Toast.LENGTH_SHORT).show()
            return
        }

        if (fin <= inicio) {
            // Verifica que la hora final sea posterior a la inicial
            Toast.makeText(requireContext(), "La hora de fin debe ser posterior a la de inicio", Toast.LENGTH_SHORT).show()
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Llama al repositorio para actualizar los datos de la cita en la base de datos
                Repos().reagendarCita(cId, inicio, fin, lugar)

                // Muestra un mensaje de éxito y vuelve atrás
                Toast.makeText(requireContext(), "Cita reagendada correctamente", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()

            } catch (e: Exception) {
                // Muestra un mensaje de error si falla el guardado
                Toast.makeText(requireContext(), "Error al guardar: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
