package cl.alercelab.centrointegral.admin

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
            try {
                items.clear()
                items.addAll(repo.obtenerOferentes())
                adapter.notifyDataSetChanged()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error al cargar oferentes: ${e.message}", Toast.LENGTH_LONG).show()
            }
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
                val nombre = txtNombre.text.toString().trim()
                val docente = txtDocente.text.toString().trim()

                if (nombre.isEmpty()) {
                    Toast.makeText(requireContext(), "El nombre es obligatorio", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (docente.isEmpty()) {
                    Toast.makeText(requireContext(), "Debe ingresar un docente responsable", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                lifecycleScope.launch {
                    try {
                        val oferentesExistentes = repo.obtenerOferentes()
                        val existeDuplicado = oferentesExistentes.any {
                            it.nombre.equals(nombre, ignoreCase = true) && it.id != item?.id
                        }

                        if (existeDuplicado) {
                            Toast.makeText(requireContext(), "Ya existe un oferente con ese nombre", Toast.LENGTH_LONG).show()
                            return@launch
                        }

                        val nuevo = item?.copy(nombre = nombre, docenteResponsable = docente)
                            ?: Oferente(nombre = nombre, docenteResponsable = docente)

                        if (item == null) repo.crearOferente(nuevo) else repo.actualizarOferente(nuevo)
                        cargarDatos()
                        Toast.makeText(requireContext(), "Oferente guardado correctamente", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "Error al guardar: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun editarItem(item: Oferente) = mostrarDialogo(item)

    private fun eliminarItem(item: Oferente) {
        AlertDialog.Builder(requireContext())
            .setMessage("¿Seguro que deseas eliminar '${item.nombre}'?")
            .setPositiveButton("Sí") { _, _ ->
                lifecycleScope.launch {
                    try {
                        repo.eliminarOferente(item.id)
                        cargarDatos()
                        Toast.makeText(requireContext(), "Oferente eliminado", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "Error al eliminar: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
            .setNegativeButton("No", null)
            .show()
    }
}
