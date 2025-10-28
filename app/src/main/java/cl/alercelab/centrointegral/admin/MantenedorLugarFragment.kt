package cl.alercelab.centrointegral.admin.mantenedores

import android.app.AlertDialog
import android.os.Bundle
import android.view.*
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cl.alercelab.centrointegral.R
import cl.alercelab.centrointegral.data.Repos
import cl.alercelab.centrointegral.domain.Lugar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch
import cl.alercelab.centrointegral.adapters.LugarAdapter
import android.widget.Toast


class MantenedorLugarFragment : Fragment() {

    private val repo = Repos()
    private val items = mutableListOf<Lugar>()
    private lateinit var adapter: LugarAdapter


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


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val v = inflater.inflate(R.layout.fragment_mantenedor_list, container, false)
        val recycler = v.findViewById<RecyclerView>(R.id.recyclerMantenedor)
        val fabAdd = v.findViewById<FloatingActionButton>(R.id.fabAdd)
        recycler.layoutManager = LinearLayoutManager(requireContext())
        adapter = LugarAdapter(items, ::editar, ::eliminar)
        recycler.adapter = adapter
        fabAdd.setOnClickListener { mostrarDialogo(null) }
        cargar()
        return v
    }

    private fun cargar() {
        lifecycleScope.launch {
            items.clear()
            items.addAll(repo.obtenerLugares())
            adapter.notifyDataSetChanged()
        }
    }

    private fun mostrarDialogo(item: Lugar?) {
        val view = layoutInflater.inflate(R.layout.dialog_mantenedor, null)
        val txtNombre = view.findViewById<EditText>(R.id.txtNombre)
        val txtDescripcion = view.findViewById<EditText>(R.id.txtDescripcion)

        txtNombre.setText(item?.nombre ?: "")
        txtDescripcion.hint = "Cupo (número)"
        txtDescripcion.setText(item?.cupo?.toString() ?: "")

        AlertDialog.Builder(requireContext())
            .setTitle(if (item == null) "Nuevo Lugar" else "Editar Lugar")
            .setView(view)
            .setPositiveButton("Guardar") { _, _ ->
                val cupo = txtDescripcion.text.toString().toIntOrNull()
                val nuevo = item?.copy(nombre = txtNombre.text.toString(), cupo = cupo)
                    ?: Lugar(nombre = txtNombre.text.toString(), cupo = cupo)
                lifecycleScope.launch {
                    if (item == null) repo.crearLugar(nuevo)
                    else repo.actualizarLugar(nuevo)
                    cargar()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun editar(item: Lugar) = mostrarDialogo(item)

    private fun eliminar(item: Lugar) {
        AlertDialog.Builder(requireContext())
            .setMessage("¿Eliminar ${item.nombre}?")
            .setPositiveButton("Sí") { _, _ ->
                lifecycleScope.launch {
                    repo.eliminarLugar(item.id!!)
                    cargar()
                }
            }
            .setNegativeButton("No", null)
            .show()
    }
}
