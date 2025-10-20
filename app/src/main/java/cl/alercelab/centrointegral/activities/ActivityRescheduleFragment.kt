package cl.alercelab.centrointegral.activities

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import cl.alercelab.centrointegral.R
import cl.alercelab.centrointegral.data.Repos
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObjects
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*

class ActivityRescheduleFragment : Fragment() {

    private lateinit var tvActividad: TextView
    private lateinit var spCitas: Spinner
    private lateinit var tvFecha: TextView
    private lateinit var tvHora: TextView
    private lateinit var etLugar: EditText
    private lateinit var btnGuardar: Button

    private val db = FirebaseFirestore.getInstance()
    private var actividadId: String? = null
    private var citaId: String? = null
    private var fechaMillis: Long? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val v = inflater.inflate(R.layout.fragment_activity_reschedule, container, false)

        tvActividad = v.findViewById(R.id.tvActividad)
        spCitas = v.findViewById(R.id.spCitas)
        tvFecha = v.findViewById(R.id.tvFecha)
        tvHora = v.findViewById(R.id.tvHora)
        etLugar = v.findViewById(R.id.etLugar)
        btnGuardar = v.findViewById(R.id.btnGuardar)

        actividadId = arguments?.getString("actividadId")

        tvFecha.setOnClickListener { pickDate() }
        tvHora.setOnClickListener { pickTime() }
        btnGuardar.setOnClickListener { onSave() }

        loadCitas()
        return v
    }

    private fun loadCitas() {
        val id = actividadId ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            val actDoc = db.collection("actividades").document(id).get().await()
            tvActividad.text = actDoc.getString("nombre") ?: "Actividad"

            val snaps = db.collection("citas").whereEqualTo("actividadId", id).get().await()
            val citas = snaps.toObjects<cl.alercelab.centrointegral.domain.Cita>()
            if (citas.isEmpty()) {
                Toast.makeText(requireContext(),"Esta actividad no tiene citas", Toast.LENGTH_LONG).show()
                findNavController().popBackStack(); return@launch
            }
            val items = citas.map { it.id to it }
            spCitas.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, items.map { it.first })
            spCitas.onItemSelectedListener = object: AdapterView.OnItemSelectedListener{
                override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                    val sel = items[position].second
                    citaId = sel.id
                    etLugar.setText(sel.lugar ?: "Centro")
                    fechaMillis = sel.fechaMillis
                }
                override fun onNothingSelected(parent: AdapterView<*>) {}
            }
        }
    }

    private fun pickDate() {
        val cal = Calendar.getInstance()
        DatePickerDialog(requireContext(),
            { _, y, m, d ->
                val c = Calendar.getInstance().apply { set(y, m, d, 0, 0, 0) }
                val hhmm = tvHora.text?.toString()?.takeIf { it.contains(":") } ?: "08:00"
                val parts = hhmm.split(":")
                c.set(Calendar.HOUR_OF_DAY, parts[0].toInt())
                c.set(Calendar.MINUTE, parts[1].toInt())
                tvFecha.text = "%02d-%02d-%04d".format(d, m+1, y)
                fechaMillis = c.timeInMillis
            },
            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun pickTime() {
        val cal = Calendar.getInstance()
        TimePickerDialog(requireContext(),
            { _, h, m ->
                tvHora.text = "%02d:%02d".format(h, m)
                val c = Calendar.getInstance()
                if (fechaMillis != null) c.timeInMillis = fechaMillis!!
                c.set(Calendar.HOUR_OF_DAY, h)
                c.set(Calendar.MINUTE, m)
                fechaMillis = c.timeInMillis
            },
            cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true
        ).show()
    }

    private fun onSave() {
        val cId = citaId ?: run { Toast.makeText(requireContext(),"Seleccione cita", Toast.LENGTH_SHORT).show(); return }
        val lugar = etLugar.text.toString().ifBlank { "Centro" }
        val whenMillis = fechaMillis ?: run { Toast.makeText(requireContext(),"Seleccione fecha/hora", Toast.LENGTH_SHORT).show(); return }

        viewLifecycleOwner.lifecycleScope.launch {
            val ok = Repos().reagendarCita(cId, whenMillis, lugar, "Reagendado manualmente")
            if (ok) {
                Toast.makeText(requireContext(),"Cita reagendada", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            } else {
                Toast.makeText(requireContext(),"Conflicto: horario/lugar ocupado", Toast.LENGTH_LONG).show()
            }
        }
    }
}
