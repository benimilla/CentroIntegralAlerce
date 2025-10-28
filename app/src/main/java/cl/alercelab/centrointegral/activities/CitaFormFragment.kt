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
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class CitaFormFragment : Fragment() {

    private lateinit var spActividad: Spinner
    private lateinit var etLugar: EditText
    private lateinit var etMotivo: EditText
    private lateinit var btnGuardar: Button
    private lateinit var progress: ProgressBar

    private val db = FirebaseFirestore.getInstance()
    private val actividades = mutableListOf<Pair<String, String>>() // (id, nombre)
    private var actividadSeleccionadaId: String? = null
    private val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val v = inflater.inflate(R.layout.fragment_cita_form, container, false)

        spActividad = v.findViewById(R.id.spActividad)
        etLugar = v.findViewById(R.id.etLugar)
        etMotivo = v.findViewById(R.id.etMotivo)
        btnGuardar = v.findViewById(R.id.btnGuardar)
        progress = v.findViewById(R.id.progressBar)

        etLugar.isEnabled = false // ✅ el lugar ahora no se puede editar manualmente

        cargarActividades()
        configurarBotonGuardar()

        return v
    }

    /** 🔹 Carga actividades desde Firestore y llena el Spinner */
    private fun cargarActividades() {
        lifecycleScope.launch {
            try {
                progress.visibility = View.VISIBLE
                val snap = db.collection("actividades").get().await()
                actividades.clear()
                actividades.addAll(snap.documents.mapNotNull {
                    val nombre = it.getString("nombre")
                    val id = it.id
                    if (nombre != null) id to nombre else null
                })

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
                        cargarLugarDeActividad(actividadSeleccionadaId!!)
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {
                        actividadSeleccionadaId = null
                        etLugar.setText("")
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "Error cargando actividades: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                progress.visibility = View.GONE
            }
        }
    }

    /** 🔹 Carga el lugar de la actividad seleccionada y lo muestra en el campo */
    private fun cargarLugarDeActividad(actividadId: String) {
        lifecycleScope.launch {
            try {
                val doc = db.collection("actividades").document(actividadId).get().await()
                val lugar = doc.getString("lugar") ?: "Sin lugar"
                etLugar.setText(lugar)
            } catch (e: Exception) {
                etLugar.setText("Error al cargar lugar")
                Toast.makeText(requireContext(), "Error obteniendo lugar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /** 🔹 Configura el botón de guardar cita */
    private fun configurarBotonGuardar() {
        btnGuardar.setOnClickListener {
            val motivo = etMotivo.text.toString().trim()
            val actividadId = actividadSeleccionadaId
            val lugar = etLugar.text.toString().trim()

            if (actividadId == null) {
                Toast.makeText(requireContext(), "Debe seleccionar una actividad", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (lugar.isEmpty() || lugar == "Sin lugar") {
                Toast.makeText(requireContext(), "La actividad no tiene lugar definido", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val now = System.currentTimeMillis()
            val fechaInicio = now
            val fechaFin = now + 60 * 60 * 1000 // Por defecto, 1 hora

            val cita = hashMapOf(
                "actividadId" to actividadId,
                "fechaInicioMillis" to fechaInicio,
                "fechaFinMillis" to fechaFin,
                "lugar" to lugar,
                "motivo" to motivo
            )

            lifecycleScope.launch {
                try {
                    progress.visibility = View.VISIBLE
                    db.collection("citas").add(cita).await()
                    Toast.makeText(requireContext(), "Cita creada correctamente", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Error al crear cita: ${e.message}", Toast.LENGTH_LONG).show()
                } finally {
                    progress.visibility = View.GONE
                }
            }
        }
    }
}
