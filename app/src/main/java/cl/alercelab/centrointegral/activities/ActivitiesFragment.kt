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
    private lateinit var tvEmpty: TextView
    private lateinit var etSearch: EditText
    private lateinit var adapter: ActivitiesAdapter

    private val db = FirebaseFirestore.getInstance()
    private val repos = Repos()
    private val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val v = inflater.inflate(R.layout.fragment_activities, container, false)

        rv = v.findViewById(R.id.rvActivities)
        btnNew = v.findViewById(R.id.btnNewActivity)
        etSearch = v.findViewById(R.id.etSearch)
        tvEmpty = v.findViewById(R.id.tvEmpty)

        rv.layoutManager = LinearLayoutManager(requireContext())

        // Configura el adaptador con las acciones para cada botón de la lista
        adapter = ActivitiesAdapter(
            onEdit = { act -> goToEdit(act.id) },
            onReschedule = { act -> goToReschedule(act.id) },
            onDelete = { act -> confirmDelete(act) },
            onDetail = { act -> showActivityDetailDialog(act) }
        )
        rv.adapter = adapter

        // Acción para crear una nueva actividad
        btnNew.setOnClickListener { goToCreate() }

        // Realiza la búsqueda cuando se presiona Enter en el campo de texto
        etSearch.setOnEditorActionListener { _, _, _ ->
            loadList()
            true
        }

        loadList()
        return v
    }

    private fun loadList() {
        val query = etSearch.text.toString().trim()
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Carga todas las actividades desde Firestore
                val snap = db.collection("actividades").get().await()
                val list = snap.toObjects<Actividad>().mapIndexed { i, a ->
                    a.copy(id = snap.documents[i].id)
                }.sortedBy { it.nombre.lowercase() }

                // Filtra las actividades según la búsqueda del usuario
                val filtered = if (query.isBlank()) list else list.filter {
                    it.nombre.contains(query, ignoreCase = true)
                }

                adapter.submitList(filtered)
                tvEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
            } catch (e: Exception) {
                // Muestra error si ocurre un problema al cargar los datos
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun goToCreate() {
        // Navega al formulario para crear una nueva actividad
        findNavController().navigate(R.id.action_activities_to_activity_form)
    }

    private fun goToEdit(actividadId: String) {
        // Navega al formulario de edición de una actividad existente
        val b = Bundle().apply { putString("actividadId", actividadId) }
        findNavController().navigate(R.id.action_activities_to_activity_form, b)
    }

    private fun goToReschedule(actividadId: String) {
        // Navega a la pantalla de reprogramación de actividad
        val b = Bundle().apply { putString("actividadId", actividadId) }
        findNavController().navigate(R.id.action_activities_to_reschedule, b)
    }

    private fun confirmDelete(act: Actividad) {
        // Muestra un cuadro de confirmación antes de eliminar una actividad
        AlertDialog.Builder(requireContext())
            .setTitle("Eliminar actividad")
            .setMessage("¿Deseas eliminar '${act.nombre}'? Esta acción no se puede deshacer.")
            .setPositiveButton("Eliminar") { _, _ -> deleteActivity(act.id) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deleteActivity(actividadId: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Elimina la actividad y sus citas asociadas
                repos.deleteActividad(actividadId)
                Toast.makeText(requireContext(), "Actividad eliminada correctamente", Toast.LENGTH_SHORT).show()
                loadList()
            } catch (e: Exception) {
                // Muestra mensaje de error si falla la eliminación
                Toast.makeText(requireContext(), "Error al eliminar: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showActivityDetailDialog(a: Actividad) {
        // Construye un mensaje con la información detallada de la actividad
        val msg = buildString {
            appendLine("Nombre: ${a.nombre}")
            appendLine("Tipo: ${a.tipo}")
            appendLine("Periodicidad: ${a.periodicidad}")
            appendLine("Lugar: ${a.lugar}")
            appendLine("Oferente: ${a.oferente ?: "-"}")
            appendLine("Socio Comunitario: ${a.socioComunitario ?: "-"}")
            appendLine("Cupo: ${a.cupo ?: "-"}")
            appendLine("Beneficiarios: ${if (a.beneficiarios.isEmpty()) "-" else a.beneficiarios.joinToString()}")
            appendLine("Días aviso previo: ${a.diasAvisoPrevio}")
            appendLine("Estado: ${a.estado}")
        }

        // Muestra el detalle en un cuadro de diálogo
        AlertDialog.Builder(requireContext())
            .setTitle("Detalle de actividad")
            .setMessage(msg)
            .setPositiveButton("Cerrar", null)
            .show()
    }
}
