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
import cl.alercelab.centrointegral.adapters.TipoActividadAdapter
import cl.alercelab.centrointegral.data.Repos
import cl.alercelab.centrointegral.domain.TipoActividad
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch

class MantenedorTipoActividadFragment : BaseMantenedorFragment() {

    private val repo = Repos() // Repositorio para acceder a la base de datos
    private lateinit var recycler: RecyclerView // Lista para mostrar los tipos de actividad
    private lateinit var fabAdd: FloatingActionButton // Botón flotante para agregar un nuevo tipo
    private val items = mutableListOf<TipoActividad>() // Lista de elementos mostrados
    private lateinit var adapter: TipoActividadAdapter // Adaptador para el RecyclerView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val v = inflater.inflate(R.layout.fragment_mantenedor_list, container, false)

        //  Inicializa vistas y configura el adaptador
        recycler = v.findViewById(R.id.recyclerMantenedor)
        fabAdd = v.findViewById(R.id.fabAdd)
        recycler.layoutManager = LinearLayoutManager(requireContext())
        adapter = TipoActividadAdapter(items, ::editarItem, ::eliminarItem)
        recycler.adapter = adapter

        //  Botón para agregar un nuevo tipo de actividad
        fabAdd.setOnClickListener { mostrarDialogo(null) }

        //  Cargar los datos iniciales desde la base de datos
        cargarDatos()
        return v
    }

    //  Carga los tipos de actividad desde el repositorio
    private fun cargarDatos() {
        lifecycleScope.launch {
            try {
                items.clear()
                items.addAll(repo.obtenerTiposActividad()) // obtiene todos los tipos desde Firebase
                adapter.notifyDataSetChanged()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error al cargar tipos: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    //  Muestra un diálogo para crear o editar un tipo de actividad
    private fun mostrarDialogo(item: TipoActividad?) {
        val view = layoutInflater.inflate(R.layout.dialog_tipo_actividad, null)
        val txtNombre = view.findViewById<EditText>(R.id.txtNombre)
        val txtDescripcion = view.findViewById<EditText>(R.id.txtDescripcion)

        // Si se edita, se rellenan los campos con los valores actuales
        txtNombre.setText(item?.nombre ?: "")
        txtDescripcion.setText(item?.descripcion ?: "")

        AlertDialog.Builder(requireContext())
            .setTitle(if (item == null) "Nuevo Tipo de Actividad" else "Editar Tipo de Actividad")
            .setView(view)
            .setPositiveButton("Guardar") { _, _ ->
                val nombre = txtNombre.text.toString().trim()
                val descripcion = txtDescripcion.text.toString().trim()

                // Validaciones básicas
                if (nombre.isEmpty()) {
                    Toast.makeText(requireContext(), "El nombre es obligatorio", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (descripcion.isEmpty()) {
                    Toast.makeText(requireContext(), "Debe ingresar una descripción", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                lifecycleScope.launch {
                    try {
                        //  Verifica si ya existe un tipo con el mismo nombre
                        val tipos = repo.obtenerTiposActividad()
                        val existeDuplicado = tipos.any { it.nombre.equals(nombre, ignoreCase = true) && it.id != item?.id }

                        if (existeDuplicado) {
                            Toast.makeText(requireContext(), "Ya existe un tipo con ese nombre", Toast.LENGTH_LONG).show()
                            return@launch
                        }

                        // Crea o actualiza el tipo según corresponda
                        val nuevo = item?.copy(nombre = nombre, descripcion = descripcion)
                            ?: TipoActividad(nombre = nombre, descripcion = descripcion)

                        if (item == null)
                            repo.crearTipoActividad(nuevo)
                        else
                            repo.actualizarTipoActividad(nuevo)

                        //  Refresca la lista
                        cargarDatos()
                        Toast.makeText(requireContext(), "Tipo guardado correctamente", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "Error al guardar: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    //  Abre el diálogo con los datos de un tipo para editarlo
    private fun editarItem(item: TipoActividad) = mostrarDialogo(item)

    //  Muestra un diálogo de confirmación antes de eliminar
    private fun eliminarItem(item: TipoActividad) {
        AlertDialog.Builder(requireContext())
            .setMessage("¿Seguro que deseas eliminar '${item.nombre}'?")
            .setPositiveButton("Sí") { _, _ ->
                lifecycleScope.launch {
                    try {
                        repo.eliminarTipoActividad(item.id)
                        cargarDatos()
                        Toast.makeText(requireContext(), "Tipo eliminado", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "Error al eliminar: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
            .setNegativeButton("No", null)
            .show()
    }
}