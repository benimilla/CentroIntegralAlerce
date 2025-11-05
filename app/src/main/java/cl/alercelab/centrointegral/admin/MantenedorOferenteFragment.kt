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

class MantenedorOferenteFragment : BaseMantenedorFragment() {

    private val repo = Repos() // Repositorio de datos para manejar oferentes
    private lateinit var recycler: RecyclerView // Vista para mostrar la lista de oferentes
    private lateinit var fabAdd: FloatingActionButton // Botón flotante para agregar oferentes
    private val items = mutableListOf<Oferente>() // Lista local de oferentes
    private lateinit var adapter: OferenteAdapter // Adaptador para manejar la vista de oferentes

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val v = inflater.inflate(R.layout.fragment_mantenedor_list, container, false)
        recycler = v.findViewById(R.id.recyclerMantenedor)
        fabAdd = v.findViewById(R.id.fabAdd)

        // Configura el RecyclerView con un layout vertical
        recycler.layoutManager = LinearLayoutManager(requireContext())

        // Crea el adaptador con funciones para editar y eliminar
        adapter = OferenteAdapter(items, ::editarItem, ::eliminarItem)
        recycler.adapter = adapter

        // Al presionar el botón flotante se abre el diálogo para agregar un nuevo oferente
        fabAdd.setOnClickListener { mostrarDialogo(null) }

        cargarDatos() // Carga inicial de oferentes
        return v
    }

    // Carga los oferentes desde el repositorio
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

    // Muestra un diálogo para crear o editar un oferente
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

                // Validaciones de campos obligatorios
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
                        // Verifica que no exista un oferente con el mismo nombre
                        val oferentesExistentes = repo.obtenerOferentes()
                        val existeDuplicado = oferentesExistentes.any {
                            it.nombre.equals(nombre, ignoreCase = true) && it.id != item?.id
                        }

                        if (existeDuplicado) {
                            Toast.makeText(requireContext(), "Ya existe un oferente con ese nombre", Toast.LENGTH_LONG).show()
                            return@launch
                        }

                        // Crea un nuevo oferente o actualiza uno existente
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

    // Permite editar un oferente existente
    private fun editarItem(item: Oferente) = mostrarDialogo(item)

    // Muestra confirmación antes de eliminar un oferente
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

// Comentario resumen:
// Este fragmento implementa un mantenedor de oferentes que permite listar, crear, editar y eliminar oferentes.
// Se usa un RecyclerView para mostrar los datos, un FloatingActionButton para agregar nuevos oferentes,
// y cuadros de diálogo (AlertDialog) para realizar las operaciones CRUD con validaciones y control de duplicados.