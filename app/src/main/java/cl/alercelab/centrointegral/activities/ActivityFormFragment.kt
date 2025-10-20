package cl.alercelab.centrointegral.activities

import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cl.alercelab.centrointegral.R
import cl.alercelab.centrointegral.data.Repos
import cl.alercelab.centrointegral.domain.Actividad
import cl.alercelab.centrointegral.domain.Cita
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObjects
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class ActivityFormFragment : Fragment() {

    private lateinit var etNombre: EditText
    private lateinit var spTipo: Spinner
    private lateinit var spPeriodicidad: Spinner
    private lateinit var etCupo: EditText
    private lateinit var etOferente: EditText
    private lateinit var etSocio: EditText
    private lateinit var etBeneficiarios: EditText
    private lateinit var etDiasAviso: EditText

    private lateinit var rvCitas: RecyclerView
    private lateinit var btnAddCita: Button
    private lateinit var btnGuardar: Button
    private lateinit var tvEmpty: TextView

    private val citas = mutableListOf<CitaUI>()
    private lateinit var adapter: CitasAdapter

    private val db = FirebaseFirestore.getInstance()
    private var actividadId: String? = null
    private var isEditing = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, s: Bundle?): View {
        val v = inflater.inflate(R.layout.fragment_activity_form, container, false)

        etNombre = v.findViewById(R.id.etNombre)
        spTipo = v.findViewById(R.id.spTipo)
        spPeriodicidad = v.findViewById(R.id.spPeriodicidad)
        etCupo = v.findViewById(R.id.etCupo)
        etOferente = v.findViewById(R.id.etOferente)
        etSocio = v.findViewById(R.id.etSocio)
        etBeneficiarios = v.findViewById(R.id.etBeneficiarios)
        etDiasAviso = v.findViewById(R.id.etDiasAviso)

        rvCitas = v.findViewById(R.id.rvCitas)
        btnAddCita = v.findViewById(R.id.btnAddCita)
        btnGuardar = v.findViewById(R.id.btnGuardar)
        tvEmpty = v.findViewById(R.id.tvEmpty)

        rvCitas.layoutManager = LinearLayoutManager(requireContext())
        adapter = CitasAdapter(
            onEdit = { idx -> editCita(idx) },
            onDelete = { idx -> removeCita(idx) }
        )
        rvCitas.adapter = adapter

        spTipo.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            listOf(
                "Capacitación","Taller","Charlas","Atenciones",
                "Operativo en oficina","Operativo rural","Operativo","Práctica profesional","Diagnostico"
            )
        )
        spPeriodicidad.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            listOf("Puntual","Periódica")
        )

        actividadId = arguments?.getString("actividadId")
        isEditing = actividadId != null

        if (isEditing) {
            loadActividad(actividadId!!)
            loadCitas(actividadId!!)
        } else {
            refreshCitasUI()
        }

        btnAddCita.setOnClickListener { openAddCitaBottomSheet(null, -1) }
        btnGuardar.setOnClickListener { onGuardar() }

        return v
    }

    private fun loadActividad(id: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            val doc = db.collection("actividades").document(id).get().await()
            val act = doc.toObject(Actividad::class.java) ?: return@launch
            etNombre.setText(act.nombre)
            setSpinnerValue(spTipo, act.tipo)
            setSpinnerValue(spPeriodicidad, act.periodicidad)
            etCupo.setText(act.cupo?.toString().orEmpty())
            etOferente.setText(act.oferente.orEmpty())
            etSocio.setText(act.socioComunitario.orEmpty())
            etBeneficiarios.setText(act.beneficiarios.joinToString(", "))
            etDiasAviso.setText(act.diasAvisoPrevio.toString())
        }
    }

    private fun loadCitas(actividadId: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            val snaps = db.collection("citas")
                .whereEqualTo("actividadId", actividadId)
                .get().await()
            val list = snaps.toObjects<Cita>()
            citas.clear()
            citas.addAll(list.map { c -> CitaUI.fromCita(c) })
            refreshCitasUI()
        }
    }

    private fun refreshCitasUI() {
        tvEmpty.visibility = if (citas.isEmpty()) View.VISIBLE else View.GONE
        adapter.setData(citas.toList())
    }

    private fun editCita(index: Int) {
        openAddCitaBottomSheet(citas[index], index)
    }

    private fun removeCita(index: Int) {
        citas.removeAt(index)
        refreshCitasUI()
    }

    private fun openAddCitaBottomSheet(existing: CitaUI?, index: Int) {
        val sheet = AddCitaBottomSheet.new(existing, role = "admin") // O "gestor" según el caso
        sheet.onSaved = { nueva ->
            if (index >= 0) citas[index] = nueva else citas.add(nueva)
            refreshCitasUI()
        }
        sheet.show(parentFragmentManager, "add_cita")
    }
    private fun onGuardar() {
        val nombre = etNombre.text.toString().trim()
        val tipo = spTipo.selectedItem?.toString() ?: ""
        val periodicidad = spPeriodicidad.selectedItem?.toString() ?: "Puntual"
        val cupo = etCupo.text.toString().trim().toIntOrNull()
        val oferente = etOferente.text.toString().trim().ifBlank { null }
        val socio = etSocio.text.toString().trim().ifBlank { null }
        val beneficiarios = etBeneficiarios.text.toString().split(",")
            .map { it.trim() }.filter { it.isNotBlank() }
        val diasAviso = etDiasAviso.text.toString().toIntOrNull() ?: 0

        if (nombre.isBlank()) {
            Snackbar.make(requireView(), "Debe ingresar el nombre de la actividad", Snackbar.LENGTH_LONG).show()
            return
        }
        if (citas.isEmpty()) {
            Snackbar.make(requireView(), "Debe agregar al menos una cita", Snackbar.LENGTH_LONG).show()
            return
        }

        val base = Actividad(
            id = actividadId ?: "",
            nombre = nombre,
            tipo = tipo,
            periodicidad = periodicidad,
            cupo = cupo,
            oferente = oferente,
            socioComunitario = socio,
            beneficiarios = beneficiarios,
            diasAvisoPrevio = diasAviso,
            estado = "vigente"
        )

        val slots = citas.map { Triple(it.fechaMillis, it.lugar, it.duracionMin) }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                if (!isEditing) {
                    Repos().crearActividadConCitas(base, slots, requireContext())
                    Snackbar.make(requireView(), "Actividad creada", Snackbar.LENGTH_LONG).show()
                } else {
                    Repos().modificarActividad(base.copy(id = actividadId!!))
                    Snackbar.make(requireView(), "Actividad actualizada", Snackbar.LENGTH_LONG).show()
                }
                findNavController().popBackStack()
            } catch (e: Exception) {
                Snackbar.make(requireView(), "Error: ${e.message}", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun setSpinnerValue(spinner: Spinner, value: String?) {
        val i = (0 until spinner.count).firstOrNull { idx ->
            spinner.getItemAtPosition(idx)?.toString().equals(value, ignoreCase = true)
        } ?: 0
        spinner.setSelection(i)
    }

    class CitasAdapter(
        private val onEdit: (Int) -> Unit,
        private val onDelete: (Int) -> Unit
    ) : RecyclerView.Adapter<CitasAdapter.VH>() {

        private val items = mutableListOf<CitaUI>()
        private val sdf = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())

        class VH(v: View) : RecyclerView.ViewHolder(v) {
            val tvLine: TextView = v.findViewById(R.id.tvLine)
            val btnEdit: ImageButton = v.findViewById(R.id.btnEdit)
            val btnDelete: ImageButton = v.findViewById(R.id.btnDelete)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_cita, parent, false)
            return VH(view)
        }

        override fun getItemCount(): Int = items.size

        override fun onBindViewHolder(h: VH, pos: Int) {
            val it = items[pos]
            val whenTxt = sdf.format(Date(it.fechaMillis))
            h.tvLine.text = "📌 $whenTxt — ${it.lugar} · ${it.duracionMin} min"
            h.btnEdit.setOnClickListener { onEdit(pos) }
            h.btnDelete.setOnClickListener { onDelete(pos) }
        }

        fun setData(data: List<CitaUI>) {
            items.clear()
            items.addAll(data)
            notifyDataSetChanged()
        }
    }

    data class CitaUI(
        val fechaMillis: Long,
        val lugar: String,
        val duracionMin: Int
    ) {
        companion object {
            fun fromCita(c: Cita) = CitaUI(
                fechaMillis = c.fechaMillis,
                lugar = c.lugar,
                duracionMin = c.duracionMin
            )
        }
    }
}
