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
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class ActivitiesListFragment : Fragment() {

    private lateinit var rv: RecyclerView
    private lateinit var adapter: ActivitiesAdapter
    private lateinit var emptyView: TextView
    private lateinit var btnAdd: Button

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val v = inflater.inflate(R.layout.fragment_activities_list, container, false)

        rv = v.findViewById(R.id.rvActividades)
        emptyView = v.findViewById(R.id.tvEmpty)
        btnAdd = v.findViewById(R.id.btnAddActividad)

        rv.layoutManager = LinearLayoutManager(requireContext())
        adapter = ActivitiesAdapter(
            onEdit = { id ->
                val b = Bundle().apply { putString("actividadId", id) }
                findNavController().navigate(R.id.action_activities_to_activity_form, b)
            },
            onDelete = { id ->
                lifecycleScope.launch {
                    Repos().deleteActividad(id)
                    Snackbar.make(requireView(), "Actividad eliminada correctamente", Snackbar.LENGTH_LONG).show()
                    loadActividades()
                }
            }
        )
        rv.adapter = adapter

        btnAdd.setOnClickListener {
            findNavController().navigate(R.id.action_activities_to_activity_form)
        }

        loadActividades()
        return v
    }

    private fun loadActividades() {
        lifecycleScope.launch {
            val perfil = Repos().currentUserProfile()
            val acts = if (perfil?.rol == "admin" || perfil?.rol == "gestor") {
                Repos().listAllActividades()
            } else {
                Repos().listUserActividades(perfil?.uid ?: "")
            }

            if (acts.isEmpty()) {
                rv.visibility = View.GONE
                emptyView.visibility = View.VISIBLE
            } else {
                rv.visibility = View.VISIBLE
                emptyView.visibility = View.GONE
                adapter.setData(acts)
            }
        }
    }

    class ActivitiesAdapter(
        private val onEdit: (String) -> Unit,
        private val onDelete: (String) -> Unit
    ) : RecyclerView.Adapter<ActivitiesAdapter.VH>() {

        private val items = mutableListOf<Actividad>()

        class VH(v: View) : RecyclerView.ViewHolder(v) {
            val tvTitle: TextView = v.findViewById(R.id.tvTitle)
            val tvSubtitle: TextView = v.findViewById(R.id.tvSubtitle)
            val btnEdit: ImageButton = v.findViewById(R.id.btnEdit)
            val btnDelete: ImageButton = v.findViewById(R.id.btnDelete)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_activity, parent, false)
            return VH(v)
        }

        override fun getItemCount(): Int = items.size

        override fun onBindViewHolder(h: VH, pos: Int) {
            val a = items[pos]
            h.tvTitle.text = a.nombre
            h.tvSubtitle.text = "Tipo: ${a.tipo} | Estado: ${a.estado}"
            h.btnEdit.setOnClickListener { onEdit(a.id) }
            h.btnDelete.setOnClickListener { onDelete(a.id) }
        }

        fun setData(list: List<Actividad>) {
            items.clear()
            items.addAll(list)
            notifyDataSetChanged()
        }
    }
}
