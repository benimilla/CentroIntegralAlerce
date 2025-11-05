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

class MantenedorLugarFragment : BaseMantenedorFragment() {

    private val repo = Repos() // Repositorio que maneja la comunicación con la base de datos
    private lateinit var recycler: RecyclerView
    private lateinit var fabAdd: FloatingActionButton
    private val items = mutableListOf<Lugar>() // Lista local para los lugares cargados
    private lateinit var adapter: LugarAdapter // Adaptador del RecyclerView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val v = inflater.inflate(R.layout.fragment_mantenedor_list, container, false)
        recycler = v.findViewById(R.id.recyclerMantenedor)
        fabAdd = v.findViewById(R.id.fabAdd)
        recycler.layoutManager = LinearLayoutManager(requireContext())
        adapter = LugarAdapter(items, ::editarItem, ::eliminarItem)
        recycler.adapter = adapter
        fabAdd.setOnClickListener { mostrarDialogo(null) } // Botón para agregar un nuevo lugar
        cargarDatos() // Carga inicial de los lugares
        return v
    }

    // Carga los lugares desde la base de datos y actualiza la vista
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

    // Muestra un cuadro de diálogo para crear o editar un lugar
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

                // Validación: el nombre no puede estar vacío
                if (nombre.isEmpty()) {
                    Toast.makeText(requireContext(), "El nombre es obligatorio", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                // Validación: cupo debe ser un número válido si no está vacío
                val cupo = if (cupoStr.isEmpty()) null else cupoStr.toIntOrNull()
                if (cupoStr.isNotEmpty() && cupo == null) {
                    Toast.makeText(requireContext(), "El cupo debe ser un número válido", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                lifecycleScope.launch {
                    try {
                        val lugaresExistentes = repo.obtenerLugares()
                        // Evita duplicados por nombre
                        val existeDuplicado = lugaresExistentes.any {
                            it.nombre.equals(nombre, ignoreCase = true) && it.id != item?.id
                        }

                        if (existeDuplicado) {
                            Toast.makeText(requireContext(), "Ya existe un lugar con ese nombre", Toast.LENGTH_LONG).show()
                            return@launch
                        }

                        // Crea o actualiza el objeto Lugar según el caso
                        val nuevo = item?.copy(nombre = nombre, cupo = cupo)
                            ?: Lugar(nombre = nombre, cupo = cupo)

                        // Llama al repositorio para guardar los datos
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

    // Llama al diálogo en modo edición
    private fun editarItem(item: Lugar) = mostrarDialogo(item)

    // Elimina un lugar tras confirmación del usuario
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