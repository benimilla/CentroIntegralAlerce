package cl.alercelab.centrointegral.admin.mantenedores

import android.app.AlertDialog
import android.os.Bundle
import android.view.*
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cl.alercelab.centrointegral.R
import cl.alercelab.centrointegral.data.Repos
import cl.alercelab.centrointegral.domain.Proyecto
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch
import cl.alercelab.centrointegral.adapters.ProyectoAdapter
import android.widget.Toast

class MantenedorProyectoFragment : Fragment() {

    private val repo = Repos()
    private val items = mutableListOf<Proyecto>()
    private lateinit var adapter: ProyectoAdapter


    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            val user = repo.currentUserProfile()
            if (user?.rol != "admin") {
                Toast.makeText(requireContext(), "Acceso restringido", Toast.LENGTH_SHORT).show()
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val v = inflater.inflate(R.layout.fragment_mantenedor_list, container, false)
        val recycler = v.findViewById<RecyclerView>(R.id.recyclerMantenedor)
        val fabAdd = v.findViewById<FloatingActionButton>(R.id.fabAdd)
        recycler.layoutManager = LinearLayoutManager(requireContext())
        adapter = ProyectoAdapter(items, ::editar, ::eliminar)
        recycler.adapter = adapter
        fabAdd.setOnClickListener { mostrarDialogo(null) }
        cargar()
        return v
    }

    private fun cargar() {
        lifecycleScope.launch {
            items.clear()
            items.addAll(repo.obtenerProyectos())
            adapter.notifyDataSetChanged()
        }
    }

    private fun mostrarDialogo(item: Proyecto?) {
        val view = layoutInflater.inflate(R.layout.dialog_mantenedor, null)
        val txtNombre = view.findViewById<EditText>(R.id.txtNombre)
        txtNombre.setText(item?.nombre ?: "")

        AlertDialog.Builder(requireContext())
            .setTitle(if (item == null) "Nuevo Proyecto" else "Editar Proyecto")
            .setView(view)
            .setPositiveButton("Guardar") { _, _ ->
                val nuevo = item?.copy(nombre = txtNombre.text.toString())
                    ?: Proyecto(nombre = txtNombre.text.toString())
                lifecycleScope.launch {
                    if (item == null) repo.crearProyecto(nuevo)
                    else repo.actualizarProyecto(nuevo)
                    cargar()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun editar(item: Proyecto) = mostrarDialogo(item)

    private fun eliminar(item: Proyecto) {
        AlertDialog.Builder(requireContext())
            .setMessage("¿Eliminar ${item.nombre}?")
            .setPositiveButton("Sí") { _, _ ->
                lifecycleScope.launch {
                    repo.eliminarProyecto(item.id!!)
                    cargar()
                }
            }
            .setNegativeButton("No", null)
            .show()
    }
}
