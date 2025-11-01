package cl.alercelab.centrointegral.admin

import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cl.alercelab.centrointegral.R
import cl.alercelab.centrointegral.data.Repos
import cl.alercelab.centrointegral.domain.Auditoria
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AuditoriaFragment : Fragment() {

    private lateinit var recyclerAuditoria: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var spModulo: Spinner
    private lateinit var spAccion: Spinner
    private lateinit var btnFiltrar: Button
    private lateinit var btnLimpiar: Button
    private lateinit var tvVacio: TextView
    private lateinit var adapter: AuditoriaAdapter

    private val repos = Repos()
    private var registros: List<Auditoria> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_auditoria, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerAuditoria = view.findViewById(R.id.recyclerAuditoria)
        progressBar = view.findViewById(R.id.progressBar)
        spModulo = view.findViewById(R.id.spModulo)
        spAccion = view.findViewById(R.id.spAccion)
        btnFiltrar = view.findViewById(R.id.btnFiltrar)
        btnLimpiar = view.findViewById(R.id.btnLimpiar)
        tvVacio = view.findViewById(R.id.tvVacio)

        recyclerAuditoria.layoutManager = LinearLayoutManager(requireContext())
        adapter = AuditoriaAdapter()
        recyclerAuditoria.adapter = adapter

        configurarSpinners()
        cargarAuditoria()

        btnFiltrar.setOnClickListener { aplicarFiltros() }
        btnLimpiar.setOnClickListener { limpiarFiltros() }
    }

    /** 🔹 Configura los filtros de módulo y acción */
    private fun configurarSpinners() {
        val modulos = listOf("Todos", "Actividades", "Citas", "Usuarios", "Gestor de Usuarios", "Mantenedores")
        val acciones = listOf("Todas", "Creación", "Edición", "Eliminación", "Aprobación", "Rechazo", "Actualización")

        val adapterModulo = ArrayAdapter(requireContext(), R.layout.spinner_item_custom, modulos)
        adapterModulo.setDropDownViewResource(R.layout.spinner_item_custom)
        spModulo.adapter = adapterModulo

        val adapterAccion = ArrayAdapter(requireContext(), R.layout.spinner_item_custom, acciones)
        adapterAccion.setDropDownViewResource(R.layout.spinner_item_custom)
        spAccion.adapter = adapterAccion
    }

    /** 🚀 Carga todos los registros desde Firestore */
    private fun cargarAuditoria() {
        lifecycleScope.launch {
            progressBar.visibility = View.VISIBLE
            recyclerAuditoria.visibility = View.GONE
            tvVacio.visibility = View.GONE
            try {
                registros = repos.obtenerAuditoria()
                if (registros.isEmpty()) {
                    tvVacio.visibility = View.VISIBLE
                    tvVacio.text = "No hay registros de auditoría disponibles."
                } else {
                    adapter.actualizar(registros)
                    recyclerAuditoria.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                tvVacio.visibility = View.VISIBLE
                tvVacio.text = "Error al cargar auditoría: ${e.message}"
                Toast.makeText(requireContext(), "Error al cargar auditoría", Toast.LENGTH_LONG).show()
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    /** 🔍 Filtra por módulo y acción */
    private fun aplicarFiltros() {
        val modulo = spModulo.selectedItem.toString()
        val accion = spAccion.selectedItem.toString()

        var filtrados = registros
        if (modulo != "Todos") filtrados = filtrados.filter { it.modulo == modulo }
        if (accion != "Todas") filtrados = filtrados.filter { it.accion == accion }

        if (filtrados.isEmpty()) {
            tvVacio.visibility = View.VISIBLE
            tvVacio.text = "No se encontraron registros con esos filtros."
            recyclerAuditoria.visibility = View.GONE
        } else {
            tvVacio.visibility = View.GONE
            recyclerAuditoria.visibility = View.VISIBLE
        }

        adapter.actualizar(filtrados)
    }

    /** ♻️ Limpia los filtros */
    private fun limpiarFiltros() {
        spModulo.setSelection(0)
        spAccion.setSelection(0)
        adapter.actualizar(registros)
        recyclerAuditoria.visibility = if (registros.isEmpty()) View.GONE else View.VISIBLE
        tvVacio.visibility = if (registros.isEmpty()) View.VISIBLE else View.GONE
        if (registros.isEmpty()) tvVacio.text = "No hay registros de auditoría disponibles."
    }
}

/** 🧾 Adaptador del RecyclerView */
class AuditoriaAdapter : RecyclerView.Adapter<AuditoriaAdapter.ViewHolder>() {

    private var registros: List<Auditoria> = emptyList()
    private val formato = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvUsuario: TextView = view.findViewById(R.id.tvUsuario)
        val tvModulo: TextView = view.findViewById(R.id.tvModulo)
        val tvAccion: TextView = view.findViewById(R.id.tvAccion)
        val tvDescripcion: TextView = view.findViewById(R.id.tvDescripcion)
        val tvFecha: TextView = view.findViewById(R.id.tvFecha)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_auditoria, parent, false)
        return ViewHolder(v)
    }

    override fun getItemCount(): Int = registros.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val registro = registros[position]
        holder.tvUsuario.text = "👤 ${registro.usuarioNombre}"
        holder.tvModulo.text = "📦 Módulo: ${registro.modulo}"
        holder.tvAccion.text = "⚙️ Acción: ${registro.accion}"
        holder.tvDescripcion.text = registro.descripcion
        holder.tvFecha.text = "🕓 ${formato.format(Date(registro.fecha))}"
    }

    /** 🔄 Actualiza el listado */
    fun actualizar(lista: List<Auditoria>) {
        registros = lista.sortedByDescending { it.fecha }
        notifyDataSetChanged()
    }
}
