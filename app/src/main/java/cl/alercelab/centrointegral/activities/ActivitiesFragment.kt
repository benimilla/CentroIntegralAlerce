package cl.alercelab.centrointegral.activities

import android.app.AlertDialog
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
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObjects
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class ActivitiesFragment : Fragment() {

    private lateinit var rv: RecyclerView
    private lateinit var btnNew: Button
    private lateinit var etSearch: EditText
    private lateinit var tvEmpty: TextView
    private lateinit var adapter: ActivitiesAdapter

    private val db = FirebaseFirestore.getInstance()
    private val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val v = inflater.inflate(R.layout.fragment_activities, container, false)
        rv = v.findViewById(R.id.rvActivities)
        btnNew = v.findViewById(R.id.btnNewActivity)
        etSearch = v.findViewById(R.id.etSearch)
        tvEmpty = v.findViewById(R.id.tvEmpty)

        rv.layoutManager = LinearLayoutManager(requireContext())
        adapter = ActivitiesAdapter(
            onEdit = { act -> goToEdit(act.id) },
            onReschedule = { act -> goToReschedule(act.id) },
            onDelete = { act -> cancelActivity(act.id) },
            onDetails = { act -> showActivityDetailDialog(act) } // <- detalle sin navegar
        )
        rv.adapter = adapter

        btnNew.setOnClickListener { goToCreate() }

        // Buscar por texto (ENTER)
        etSearch.setOnEditorActionListener { _, _, _ ->
            loadList()
            true
        }

        loadList()
        return v
    }

    private fun goToCreate() {
        // ya existe en tu nav_graph
        findNavController().navigate(R.id.action_activities_to_activity_form)
    }

    private fun goToEdit(actividadId: String) {
        val b = Bundle().apply { putString("actividadId", actividadId) }
        findNavController().navigate(R.id.action_activities_to_activity_form, b)
    }

    private fun goToReschedule(actividadId: String) {
        val b = Bundle().apply { putString("actividadId", actividadId) }
        findNavController().navigate(R.id.action_activities_to_reschedule, b)
    }

    private fun cancelActivity(actividadId: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                Repos().cancelarActividad(actividadId, "Cancelada por administrador")
                Toast.makeText(requireContext(), "Actividad cancelada", Toast.LENGTH_SHORT).show()
                loadList()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error al cancelar: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun loadList() {
        val q = etSearch.text?.toString()?.trim().orEmpty()
        viewLifecycleOwner.lifecycleScope.launch {
            val snap = db.collection("actividades").get().await()
            val list = snap.toObjects<Actividad>()
                .mapIndexed { idx, a -> a.copy(id = snap.documents[idx].id) }
                .sortedBy { it.nombre.lowercase() }

            val filtered = if (q.isBlank())
                list
            else
                list.filter { it.nombre.contains(q, ignoreCase = true) }

            adapter.setData(filtered)
            tvEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun showActivityDetailDialog(a: Actividad) {
        val msg = buildString {
            appendLine("Nombre: ${a.nombre}")
            appendLine("Tipo: ${a.tipo}")
            appendLine("Periodicidad: ${a.periodicidad}")
            appendLine("Lugar: ${a.lugar}")
            appendLine("Oferente: ${a.oferente ?: "-"}")
            appendLine("Socio Comunitario: ${a.socioComunitario ?: "-"}")
            appendLine("Cupo: ${a.cupo ?: "-"}")
            appendLine("Beneficiarios: ${if (a.beneficiarios.isEmpty()) "-" else a.beneficiarios.joinToString()}")
            if (a.fechaInicio > 0L && a.fechaFin > 0L) {
                appendLine("Desde: ${sdf.format(Date(a.fechaInicio))}")
                appendLine("Hasta: ${sdf.format(Date(a.fechaFin))}")
            }
            appendLine("Estado: ${a.estado}")
            a.motivoCancelacion?.let { appendLine("Motivo cancelación: $it") }
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Detalle de actividad")
            .setMessage(msg)
            .setPositiveButton("Cerrar", null)
            .show()
    }

    // ---- Adapter ----
    class ActivitiesAdapter(
        private val onEdit: (Actividad) -> Unit,
        private val onReschedule: (Actividad) -> Unit,
        private val onDelete: (Actividad) -> Unit,
        private val onDetails: (Actividad) -> Unit
    ) : RecyclerView.Adapter<ActivitiesAdapter.VH>() {

        private val items = mutableListOf<Actividad>()

        class VH(v: View) : RecyclerView.ViewHolder(v) {
            val tvTitle: TextView = v.findViewById(R.id.tvTitle)
            val tvSubtitle: TextView = v.findViewById(R.id.tvSubtitle)
            val btnEdit: ImageButton = v.findViewById(R.id.btnEdit)
            val btnReschedule: ImageButton = v.findViewById(R.id.btnReschedule)
            val btnDelete: ImageButton = v.findViewById(R.id.btnDelete)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_activity, parent, false)
            return VH(view)
        }

        override fun getItemCount(): Int = items.size

        override fun onBindViewHolder(h: VH, pos: Int) {
            val it = items[pos]
            h.tvTitle.text = it.nombre
            val estado = it.estado
            val tipo = it.tipo
            val per = it.periodicidad
            h.tvSubtitle.text = "Tipo: $tipo · Periodicidad: $per · Estado: $estado"

            h.itemView.setOnClickListener { onDetails(items[pos]) }
            h.btnEdit.setOnClickListener { onEdit(items[pos]) }
            h.btnReschedule.setOnClickListener { onReschedule(items[pos]) }
            h.btnDelete.setOnClickListener { onDelete(items[pos]) }
        }

        fun setData(data: List<Actividad>) {
            items.clear()
            items.addAll(data)
            notifyDataSetChanged()
        }
    }
}
