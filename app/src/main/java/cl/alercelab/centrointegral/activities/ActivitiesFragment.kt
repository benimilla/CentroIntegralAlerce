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
import cl.alercelab.centrointegral.domain.UserProfile
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObjects
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ActivitiesFragment : Fragment() {

    private lateinit var rv: RecyclerView
    private lateinit var btnNew: Button
    private lateinit var etSearch: EditText
    private lateinit var tvEmpty: TextView
    private lateinit var adapter: ActivitiesAdapter

    private var perfil: UserProfile? = null
    private val db = FirebaseFirestore.getInstance()

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
            onDelete = { act -> cancelActivity(act.id) }
        )
        rv.adapter = adapter

        btnNew.setOnClickListener { goToCreate() }

        // Cargar perfil y validar rol
        viewLifecycleOwner.lifecycleScope.launch {
            perfil = Repos().currentUserProfile()
            val p = perfil
            if (p == null || (p.rol != "admin" && p.rol != "gestor")) {
                Toast.makeText(requireContext(), "Sin permisos para acceder a Actividades", Toast.LENGTH_LONG).show()
                findNavController().popBackStack()
                return@launch
            }
            loadList()
        }

        // Buscar por texto (ENTER)
        etSearch.setOnEditorActionListener { _, _, _ ->
            loadList()
            true
        }

        return v
    }

    private fun goToCreate() {
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
            val list = snap.toObjects<Actividad>().sortedBy { it.nombre?.lowercase() ?: "" }
            val filtered = if (q.isBlank()) list else list.filter { it.nombre?.contains(q, ignoreCase = true) == true }
            adapter.setData(filtered)
            tvEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    // ---- Adapter ----
    class ActivitiesAdapter(
        private val onEdit: (Actividad) -> Unit,
        private val onReschedule: (Actividad) -> Unit,
        private val onDelete: (Actividad) -> Unit
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
            h.tvTitle.text = it.nombre ?: "(sin nombre)"
            val estado = it.estado ?: "vigente"
            val tipo = it.tipo ?: "-"
            val per = it.periodicidad ?: "-"
            h.tvSubtitle.text = "Tipo: $tipo · Periodicidad: $per · Estado: $estado"

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
