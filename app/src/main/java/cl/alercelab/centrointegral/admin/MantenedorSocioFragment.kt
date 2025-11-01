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

    private val repo = Repos()
    private lateinit var recycler: RecyclerView
    private lateinit var fabAdd: FloatingActionButton
    private val items = mutableListOf<SocioComunitario>()
    private lateinit var adapter: SocioComunitarioAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val v = inflater.inflate(R.layout.fragment_mantenedor_list, container, false)
        recycler = v.findViewById(R.id.recyclerMantenedor)
        fabAdd = v.findViewById(R.id.fabAdd)
        recycler.layoutManager = LinearLayoutManager(requireContext())
        adapter = SocioComunitarioAdapter(items, ::editarItem, ::eliminarItem)
        recycler.adapter = adapter
        fabAdd.setOnClickListener { mostrarDialogo(null) }
        cargarDatos()
        return v
    }

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

    private fun mostrarDialogo(item: SocioComunitario?) {
        val view = layoutInflater.inflate(R.layout.dialog_socio, null)
        val txtNombre = view.findViewById<EditText>(R.id.txtNombre)
        txtNombre.setText(item?.nombre ?: "")

        AlertDialog.Builder(requireContext())
            .setTitle(if (item == null) "Nuevo Socio Comunitario" else "Editar Socio Comunitario")
            .setView(view)
            .setPositiveButton("Guardar") { _, _ ->
                val nombre = txtNombre.text.toString().trim()
                if (nombre.isEmpty()) {
                    Toast.makeText(requireContext(), "El nombre es obligatorio", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                lifecycleScope.launch {
                    try {
                        val socios = repo.obtenerSociosComunitarios()
                        val existeDuplicado = socios.any { it.nombre.equals(nombre, ignoreCase = true) && it.id != item?.id }

                        if (existeDuplicado) {
                            Toast.makeText(requireContext(), "Ya existe un socio con ese nombre", Toast.LENGTH_LONG).show()
                            return@launch
                        }

                        val nuevo = item?.copy(nombre = nombre) ?: SocioComunitario(nombre = nombre)
                        if (item == null) repo.crearSocioComunitario(nuevo) else repo.actualizarSocioComunitario(nuevo)
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

    private fun editarItem(item: SocioComunitario) = mostrarDialogo(item)

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
