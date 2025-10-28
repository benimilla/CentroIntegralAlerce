package cl.alercelab.centrointegral.admin.mantenedores

import android.app.AlertDialog
import android.os.Bundle
import android.view.*
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cl.alercelab.centrointegral.R
import cl.alercelab.centrointegral.adapters.LugarAdapter
import cl.alercelab.centrointegral.data.Repos
import cl.alercelab.centrointegral.domain.Lugar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch

class MantenedorLugarFragment : Fragment() {

    private val repo = Repos()
    private lateinit var recycler: RecyclerView
    private lateinit var fabAdd: FloatingActionButton
    private val items = mutableListOf<Lugar>()
    private lateinit var adapter: LugarAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val v = inflater.inflate(R.layout.fragment_mantenedor_list, container, false)
        recycler = v.findViewById(R.id.recyclerMantenedor)
        fabAdd = v.findViewById(R.id.fabAdd)
        recycler.layoutManager = LinearLayoutManager(requireContext())
        adapter = LugarAdapter(items, ::editarItem, ::eliminarItem)
        recycler.adapter = adapter
        fabAdd.setOnClickListener { mostrarDialogo(null) }
        cargarDatos()
        return v
    }

    private fun cargarDatos() {
        lifecycleScope.launch {
            items.clear()
            items.addAll(repo.obtenerLugares())
            adapter.notifyDataSetChanged()
        }
    }

    private fun mostrarDialogo(item: Lugar?) {
        val view = layoutInflater.inflate(R.layout.dialog_lugar, null)
        val txtNombre = view.findViewById<EditText>(R.id.txtNombre)
        val txtCupo = view.findViewById<EditText>(R.id.txtCupo)

        txtNombre.setText(item?.nombre ?: "")
        txtCupo.setText(item?.cupo?.toString() ?: "")

        AlertDialog.Builder(requireContext())
            .setTitle(if (item == null) "Nuevo Lugar" else "Editar Lugar")
            .setView(view)
            .setPositiveButton("Guardar") { _, _ ->
                val cupoParsed = txtCupo.text.toString().trim().ifEmpty { null }?.toIntOrNull()
                val nuevo = item?.copy(
                    nombre = txtNombre.text.toString().trim(),
                    cupo = cupoParsed
                ) ?: Lugar(
                    nombre = txtNombre.text.toString().trim(),
                    cupo = cupoParsed
                )
                lifecycleScope.launch {
                    try {
                        if (item == null) repo.crearLugar(nuevo) else repo.actualizarLugar(nuevo)
                        cargarDatos()
                        Toast.makeText(requireContext(), "Guardado", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun editarItem(item: Lugar) = mostrarDialogo(item)

    private fun eliminarItem(item: Lugar) {
        AlertDialog.Builder(requireContext())
            .setMessage("¿Eliminar ${item.nombre}?")
            .setPositiveButton("Sí") { _, _ ->
                lifecycleScope.launch {
                    repo.eliminarLugar(item.id)
                    cargarDatos()
                }
            }
            .setNegativeButton("No", null)
            .show()
    }
}
