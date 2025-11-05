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
import cl.alercelab.centrointegral.adapters.SocioComunitarioAdapter
import cl.alercelab.centrointegral.data.Repos
import cl.alercelab.centrointegral.domain.SocioComunitario
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch

class MantenedorSocioFragment : BaseMantenedorFragment() {

    private val repo = Repos() // Repositorio para acceder a la base de datos
    private lateinit var recycler: RecyclerView // Lista de socios comunitarios
    private lateinit var fabAdd: FloatingActionButton // Botón flotante para agregar un nuevo socio
    private val items = mutableListOf<SocioComunitario>() // Lista local de socios
    private lateinit var adapter: SocioComunitarioAdapter // Adaptador para mostrar los socios

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val v = inflater.inflate(R.layout.fragment_mantenedor_list, container, false)

        //  Configura RecyclerView y su adaptador
        recycler = v.findViewById(R.id.recyclerMantenedor)
        fabAdd = v.findViewById(R.id.fabAdd)
        recycler.layoutManager = LinearLayoutManager(requireContext())
        adapter = SocioComunitarioAdapter(items, ::editarItem, ::eliminarItem)
        recycler.adapter = adapter

        //  Botón flotante para crear un nuevo socio
        fabAdd.setOnClickListener { mostrarDialogo(null) }

        //  Cargar los socios al iniciar
        cargarDatos()
        return v
    }

    //  Obtiene los socios desde la base de datos y los muestra
    private fun cargarDatos() {
        lifecycleScope.launch {
            try {
                items.clear()
                items.addAll(repo.obtenerSociosComunitarios())
                adapter.notifyDataSetChanged()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error al cargar socios: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    //  Muestra un diálogo para crear o editar un socio comunitario
    private fun mostrarDialogo(item: SocioComunitario?) {
        val view = layoutInflater.inflate(R.layout.dialog_socio, null)
        val txtNombre = view.findViewById<EditText>(R.id.txtNombre)
        txtNombre.setText(item?.nombre ?: "")

        AlertDialog.Builder(requireContext())
            .setTitle(if (item == null) "Nuevo Socio Comunitario" else "Editar Socio Comunitario")
            .setView(view)
            .setPositiveButton("Guardar") { _, _ ->
                val nombre = txtNombre.text.toString().trim()

                // Validación de campo vacío
                if (nombre.isEmpty()) {
                    Toast.makeText(requireContext(), "El nombre es obligatorio", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                lifecycleScope.launch {
                    try {
                        // Verifica si ya existe un socio con el mismo nombre
                        val socios = repo.obtenerSociosComunitarios()
                        val existeDuplicado = socios.any {
                            it.nombre.equals(nombre, ignoreCase = true) && it.id != item?.id
                        }

                        if (existeDuplicado) {
                            Toast.makeText(requireContext(), "Ya existe un socio con ese nombre", Toast.LENGTH_LONG).show()
                            return@launch
                        }

                        // Crea o actualiza el socio
                        val nuevo = item?.copy(nombre = nombre) ?: SocioComunitario(nombre = nombre)
                        if (item == null)
                            repo.crearSocioComunitario(nuevo)
                        else
                            repo.actualizarSocioComunitario(nuevo)

                        cargarDatos()
                        Toast.makeText(requireContext(), "Socio guardado correctamente", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "Error al guardar: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    //  Abre el diálogo con los datos del socio seleccionado
    private fun editarItem(item: SocioComunitario) = mostrarDialogo(item)

    //  Muestra un diálogo de confirmación antes de eliminar un socio
    private fun eliminarItem(item: SocioComunitario) {
        AlertDialog.Builder(requireContext())
            .setMessage("¿Seguro que deseas eliminar '${item.nombre}'?")
            .setPositiveButton("Sí") { _, _ ->
                lifecycleScope.launch {
                    try {
                        repo.eliminarSocioComunitario(item.id)
                        cargarDatos()
                        Toast.makeText(requireContext(), "Socio eliminado", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "Error al eliminar: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
            .setNegativeButton("No", null)
            .show()
    }
}