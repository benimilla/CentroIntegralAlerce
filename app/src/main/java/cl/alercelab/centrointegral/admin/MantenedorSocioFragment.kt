package cl.alercelab.centrointegral.admin.mantenedores

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cl.alercelab.centrointegral.R
import cl.alercelab.centrointegral.data.Repos
import cl.alercelab.centrointegral.domain.SocioComunitario
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch
import cl.alercelab.centrointegral.adapters.SocioAdapter
import android.widget.Toast


class MantenedorSocioFragment : Fragment() {

    private val repo = Repos()
    private val items = mutableListOf<SocioComunitario>()
    private lateinit var adapter: SocioAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val v = inflater.inflate(R.layout.fragment_mantenedor_list, container, false)
        val recycler = v.findViewById<RecyclerView>(R.id.recyclerMantenedor)
        val fabAdd = v.findViewById<FloatingActionButton>(R.id.fabAdd)
        recycler.layoutManager = LinearLayoutManager(requireContext())
        adapter = SocioAdapter(items, ::editar, ::eliminar)
        recycler.adapter = adapter
        fabAdd.setOnClickListener { mostrarDialogo(null) }
        cargar()
        return v
    }

    private fun cargar() {
        lifecycleScope.launch {
            items.clear()
            items.addAll(repo.obtenerSociosComunitarios())
            adapter.notifyDataSetChanged()
        }
    }

    private fun mostrarDialogo(item: SocioComunitario?) {
        val view = layoutInflater.inflate(R.layout.dialog_mantenedor, null)
        val txtNombre = view.findViewById<EditText>(R.id.txtNombre)
        txtNombre.setText(item?.nombre ?: "")

        AlertDialog.Builder(requireContext())
            .setTitle(if (item == null) "Nuevo Socio Comunitario" else "Editar Socio Comunitario")
            .setView(view)
            .setPositiveButton("Guardar") { _, _ ->
                val nuevo = item?.copy(nombre = txtNombre.text.toString())
                    ?: SocioComunitario(nombre = txtNombre.text.toString())
                lifecycleScope.launch {
                    if (item == null) repo.crearSocioComunitario(nuevo)
                    else repo.actualizarSocioComunitario(nuevo)
                    cargar()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun editar(item: SocioComunitario) = mostrarDialogo(item)

    private fun eliminar(item: SocioComunitario) {
        AlertDialog.Builder(requireContext())
            .setMessage("¿Eliminar ${item.nombre}?")
            .setPositiveButton("Sí") { _, _ ->
                lifecycleScope.launch {
                    repo.eliminarSocioComunitario(item.id!!)
                    cargar()
                }
            }
            .setNegativeButton("No", null)
            .show()
    }
}
