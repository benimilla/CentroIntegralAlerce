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
            try {
                items.clear()
                items.addAll(repo.obtenerLugares())
                adapter.notifyDataSetChanged()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error al cargar lugares: ${e.message}", Toast.LENGTH_LONG).show()
            }
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
                val nombre = txtNombre.text.toString().trim()
                val cupoStr = txtCupo.text.toString().trim()

                if (nombre.isEmpty()) {
                    Toast.makeText(requireContext(), "El nombre es obligatorio", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val cupo = if (cupoStr.isEmpty()) null else cupoStr.toIntOrNull()
                if (cupoStr.isNotEmpty() && cupo == null) {
                    Toast.makeText(requireContext(), "El cupo debe ser un número válido", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                lifecycleScope.launch {
                    try {
                        val lugaresExistentes = repo.obtenerLugares()
                        val existeDuplicado = lugaresExistentes.any {
                            it.nombre.equals(nombre, ignoreCase = true) && it.id != item?.id
                        }

                        if (existeDuplicado) {
                            Toast.makeText(requireContext(), "Ya existe un lugar con ese nombre", Toast.LENGTH_LONG).show()
                            return@launch
                        }

                        val nuevo = item?.copy(nombre = nombre, cupo = cupo)
                            ?: Lugar(nombre = nombre, cupo = cupo)

                        if (item == null) repo.crearLugar(nuevo) else repo.actualizarLugar(nuevo)
                        cargarDatos()
                        Toast.makeText(requireContext(), "Lugar guardado correctamente", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "Error al guardar: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun editarItem(item: Lugar) = mostrarDialogo(item)

    private fun eliminarItem(item: Lugar) {
        AlertDialog.Builder(requireContext())
            .setMessage("¿Seguro que deseas eliminar '${item.nombre}'?")
            .setPositiveButton("Sí") { _, _ ->
                lifecycleScope.launch {
                    try {
                        repo.eliminarLugar(item.id)
                        cargarDatos()
                        Toast.makeText(requireContext(), "Lugar eliminado", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "Error al eliminar: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
            .setNegativeButton("No", null)
            .show()
    }
}
