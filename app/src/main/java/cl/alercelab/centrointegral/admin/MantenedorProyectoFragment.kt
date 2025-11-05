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
import cl.alercelab.centrointegral.adapters.ProyectoAdapter
import cl.alercelab.centrointegral.data.Repos
import cl.alercelab.centrointegral.domain.Proyecto
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch

class MantenedorProyectoFragment : BaseMantenedorFragment() {

    private val repo = Repos() // Repositorio que maneja las operaciones de datos
    private lateinit var recycler: RecyclerView // Lista visual de proyectos
    private lateinit var fabAdd: FloatingActionButton // Botón flotante para agregar nuevo proyecto
    private val items = mutableListOf<Proyecto>() // Lista de proyectos cargados
    private lateinit var adapter: ProyectoAdapter // Adaptador para mostrar los proyectos en el RecyclerView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val v = inflater.inflate(R.layout.fragment_mantenedor_list, container, false)
        recycler = v.findViewById(R.id.recyclerMantenedor)
        fabAdd = v.findViewById(R.id.fabAdd)

        // Configura el RecyclerView con un layout lineal y el adaptador
        recycler.layoutManager = LinearLayoutManager(requireContext())
        adapter = ProyectoAdapter(items, ::editarItem, ::eliminarItem)
        recycler.adapter = adapter

        // Acción del botón flotante para crear un nuevo proyecto
        fabAdd.setOnClickListener { mostrarDialogo(null) }

        cargarDatos() // Carga inicial de proyectos
        return v
    }

    // Obtiene los proyectos desde el repositorio y actualiza la lista
    private fun cargarDatos() {
        lifecycleScope.launch {
            try {
                items.clear()
                items.addAll(repo.obtenerProyectos())
                adapter.notifyDataSetChanged()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error al cargar proyectos: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Muestra un cuadro de diálogo para agregar o editar un proyecto
    private fun mostrarDialogo(item: Proyecto?) {
        val view = layoutInflater.inflate(R.layout.dialog_proyecto, null)
        val txtNombre = view.findViewById<EditText>(R.id.txtNombre)
        txtNombre.setText(item?.nombre ?: "")

        AlertDialog.Builder(requireContext())
            .setTitle(if (item == null) "Nuevo Proyecto" else "Editar Proyecto")
            .setView(view)
            .setPositiveButton("Guardar") { _, _ ->
                val nombre = txtNombre.text.toString().trim()
                if (nombre.isEmpty()) {
                    Toast.makeText(requireContext(), "El nombre es obligatorio", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                lifecycleScope.launch {
                    try {
                        // Verifica si ya existe un proyecto con el mismo nombre
                        val proyectosExistentes = repo.obtenerProyectos()
                        val existeDuplicado = proyectosExistentes.any {
                            it.nombre.equals(nombre, ignoreCase = true) && it.id != item?.id
                        }

                        if (existeDuplicado) {
                            Toast.makeText(requireContext(), "Ya existe un proyecto con ese nombre", Toast.LENGTH_LONG).show()
                            return@launch
                        }

                        // Crea o actualiza el proyecto según corresponda
                        val nuevo = item?.copy(nombre = nombre) ?: Proyecto(nombre = nombre)
                        if (item == null) repo.crearProyecto(nuevo) else repo.actualizarProyecto(nuevo)
                        cargarDatos()
                        Toast.makeText(requireContext(), "Proyecto guardado correctamente", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "Error al guardar: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // Llama a la edición del proyecto seleccionado
    private fun editarItem(item: Proyecto) = mostrarDialogo(item)

    // Elimina el proyecto seleccionado tras confirmación
    private fun eliminarItem(item: Proyecto) {
        AlertDialog.Builder(requireContext())
            .setMessage("¿Seguro que deseas eliminar '${item.nombre}'?")
            .setPositiveButton("Sí") { _, _ ->
                lifecycleScope.launch {
                    try {
                        repo.eliminarProyecto(item.id)
                        cargarDatos()
                        Toast.makeText(requireContext(), "Proyecto eliminado", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "Error al eliminar: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
            .setNegativeButton("No", null)
            .show()
    }
}

// Comentario resumen:
// Este fragmento implementa un mantenedor de proyectos que permite listar, crear, editar y eliminar proyectos.
// Utiliza un RecyclerView para mostrar los datos, un FloatingActionButton para agregar nuevos proyectos,
// y diálogos (AlertDialog) para las operaciones CRUD.