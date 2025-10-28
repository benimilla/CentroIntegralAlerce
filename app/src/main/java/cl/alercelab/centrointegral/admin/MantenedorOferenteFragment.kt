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
import cl.alercelab.centrointegral.adapters.OferenteAdapter
import cl.alercelab.centrointegral.data.Repos
import cl.alercelab.centrointegral.domain.Oferente
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch

class MantenedorOferenteFragment : Fragment() {

    private val repo = Repos()
    private lateinit var recycler: RecyclerView
    private lateinit var fabAdd: FloatingActionButton
    private val items = mutableListOf<Oferente>()
    private lateinit var adapter: OferenteAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val v = inflater.inflate(R.layout.fragment_mantenedor_list, container, false)
        recycler = v.findViewById(R.id.recyclerMantenedor)
        fabAdd = v.findViewById(R.id.fabAdd)
        recycler.layoutManager = LinearLayoutManager(requireContext())
        adapter = OferenteAdapter(items, ::editarItem, ::eliminarItem)
        recycler.adapter = adapter
        fabAdd.setOnClickListener { mostrarDialogo(null) }
        cargarDatos()
        return v
    }

    private fun cargarDatos() {
        lifecycleScope.launch {
            items.clear()
            items.addAll(repo.obtenerOferentes())
            adapter.notifyDataSetChanged()
        }
    }

    private fun mostrarDialogo(item: Oferente?) {
        val view = layoutInflater.inflate(R.layout.dialog_oferente, null)
        val txtNombre = view.findViewById<EditText>(R.id.txtNombre)
        val txtDocente = view.findViewById<EditText>(R.id.txtDocente)

        txtNombre.setText(item?.nombre ?: "")
        txtDocente.setText(item?.docenteResponsable ?: "")

        AlertDialog.Builder(requireContext())
            .setTitle(if (item == null) "Nuevo Oferente" else "Editar Oferente")
            .setView(view)
            .setPositiveButton("Guardar") { _, _ ->
                val nuevo = item?.copy(
                    nombre = txtNombre.text.toString().trim(),
                    docenteResponsable = txtDocente.text.toString().trim()
                ) ?: Oferente(
                    nombre = txtNombre.text.toString().trim(),
                    docenteResponsable = txtDocente.text.toString().trim()
                )
                lifecycleScope.launch {
                    try {
                        if (item == null) repo.crearOferente(nuevo) else repo.actualizarOferente(nuevo)
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

    private fun editarItem(item: Oferente) = mostrarDialogo(item)
    private fun eliminarItem(item: Oferente) {
        AlertDialog.Builder(requireContext())
            .setMessage("¿Eliminar ${item.nombre}?")
            .setPositiveButton("Sí") { _, _ ->
                lifecycleScope.launch {
                    repo.eliminarOferente(item.id)
                    cargarDatos()
                }
            }
            .setNegativeButton("No", null)
            .show()
    }
}
