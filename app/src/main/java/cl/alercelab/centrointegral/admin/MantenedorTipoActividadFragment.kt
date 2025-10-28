package cl.alercelab.centrointegral.admin.mantenedores

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cl.alercelab.centrointegral.R
import cl.alercelab.centrointegral.data.Repos
import cl.alercelab.centrointegral.domain.TipoActividad
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch
import cl.alercelab.centrointegral.adapters.TipoActividadAdapter


class MantenedorTipoActividadFragment : Fragment() {

    private val repo = Repos()
    private lateinit var recycler: RecyclerView
    private lateinit var fabAdd: FloatingActionButton
    private val items = mutableListOf<TipoActividad>()
    private lateinit var adapter: TipoActividadAdapter


    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            val user = repo.currentUserProfile()
            if (user?.rol != "admin") {
                Toast.makeText(requireContext(), "Acceso restringido", Toast.LENGTH_SHORT).show()
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val v = inflater.inflate(R.layout.fragment_mantenedor_list, container, false)
        recycler = v.findViewById(R.id.recyclerMantenedor)
        fabAdd = v.findViewById(R.id.fabAdd)
        recycler.layoutManager = LinearLayoutManager(requireContext())
        adapter = TipoActividadAdapter(items, ::editarItem, ::eliminarItem)
        recycler.adapter = adapter

        fabAdd.setOnClickListener { mostrarDialogo(null) }

        cargarDatos()
        return v
    }

    private fun cargarDatos() {
        lifecycleScope.launch {
            items.clear()
            items.addAll(repo.obtenerTiposActividad())
            adapter.notifyDataSetChanged()
        }
    }

    private fun mostrarDialogo(item: TipoActividad?) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_mantenedor, null)
        val txtNombre = dialogView.findViewById<EditText>(R.id.txtNombre)
        val txtDescripcion = dialogView.findViewById<EditText>(R.id.txtDescripcion)

        txtNombre.setText(item?.nombre ?: "")
        txtDescripcion.setText(item?.descripcion ?: "")

        AlertDialog.Builder(requireContext())
            .setTitle(if (item == null) "Nuevo tipo de actividad" else "Editar tipo de actividad")
            .setView(dialogView)
            .setPositiveButton("Guardar") { _, _ ->
                val nuevo = item?.copy(
                    nombre = txtNombre.text.toString(),
                    descripcion = txtDescripcion.text.toString()
                ) ?: TipoActividad(
                    nombre = txtNombre.text.toString(),
                    descripcion = txtDescripcion.text.toString()
                )

                lifecycleScope.launch {
                    if (item == null) repo.crearTipoActividad(nuevo)
                    else repo.actualizarTipoActividad(nuevo)
                    cargarDatos()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun editarItem(item: TipoActividad) = mostrarDialogo(item)

    private fun eliminarItem(item: TipoActividad) {
        AlertDialog.Builder(requireContext())
            .setMessage("¿Eliminar ${item.nombre}?")
            .setPositiveButton("Sí") { _, _ ->
                lifecycleScope.launch {
                    repo.eliminarTipoActividad(item.id!!)
                    cargarDatos()
                }
            }
            .setNegativeButton("No", null)
            .show()
    }
}
