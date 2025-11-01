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
import cl.alercelab.centrointegral.domain.UserProfile
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AdminPendingFragment : Fragment() {

    private lateinit var adapter: PendingAdapter
    private val repos = Repos()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val v = inflater.inflate(R.layout.fragment_admin_pending, container, false)

        val rv = v.findViewById<RecyclerView>(R.id.rvPending)
        rv.layoutManager = LinearLayoutManager(requireContext())
        adapter = PendingAdapter(
            onApprove = { uid -> approveUser(v, uid) },
            onReject = { uid -> rejectUser(v, uid) }
        )
        rv.adapter = adapter
        load(v)
        return v
    }

    private fun load(view: View) {
        lifecycleScope.launch {
            val result = repos.listPendingUsers()
            adapter.setData(result)
        }
    }

    private fun approveUser(view: View, uid: String) {
        lifecycleScope.launch {
            try {
                repos.approveUser(uid)
                registrarAuditoria(
                    usuarioId = "admin_local",
                    usuarioNombre = "Administrador",
                    modulo = "Usuarios pendientes",
                    accion = "Aprobación",
                    entidadId = uid,
                    descripcion = "Usuario aprobado"
                )
                Toast.makeText(requireContext(), "Usuario aprobado", Toast.LENGTH_SHORT).show()
                load(view)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun rejectUser(view: View, uid: String) {
        lifecycleScope.launch {
            try {
                repos.rejectUser(uid)
                registrarAuditoria(
                    usuarioId = "admin_local",
                    usuarioNombre = "Administrador",
                    modulo = "Usuarios pendientes",
                    accion = "Rechazo",
                    entidadId = uid,
                    descripcion = "Usuario rechazado"
                )
                Toast.makeText(requireContext(), "Usuario rechazado", Toast.LENGTH_SHORT).show()
                load(view)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * 🧾 Guarda registro de auditoría en Firestore.
     */
    private suspend fun registrarAuditoria(
        usuarioId: String,
        usuarioNombre: String,
        modulo: String,
        accion: String,
        entidadId: String,
        descripcion: String,
        cambios: Map<String, Any>? = null
    ) {
        val db = FirebaseFirestore.getInstance()
        val registro = mapOf(
            "usuarioId" to usuarioId,
            "usuarioNombre" to usuarioNombre,
            "modulo" to modulo,
            "accion" to accion,
            "entidadId" to entidadId,
            "descripcion" to descripcion,
            "cambios" to (cambios ?: emptyMap<String, Any>()),
            "fecha" to System.currentTimeMillis()
        )
        db.collection("auditoria").add(registro).await()
    }

    class PendingAdapter(
        private val onApprove: (String) -> Unit,
        private val onReject: (String) -> Unit
    ) : RecyclerView.Adapter<PendingAdapter.VH>() {

        private val items = mutableListOf<UserProfile>()

        class VH(v: View) : RecyclerView.ViewHolder(v) {
            val name: TextView = v.findViewById(R.id.tvName)
            val email: TextView = v.findViewById(R.id.tvEmail)
            val role: TextView = v.findViewById(R.id.tvRole)
            val btnApprove: Button = v.findViewById(R.id.btnApprove)
            val btnReject: Button = v.findViewById(R.id.btnReject)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_request, parent, false)
            return VH(view)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val user = items[position]
            holder.name.text = user.nombre
            holder.email.text = user.email
            holder.role.text = "Rol solicitado: ${user.rol}"

            holder.btnApprove.setOnClickListener { onApprove(user.uid) }
            holder.btnReject.setOnClickListener { onReject(user.uid) }
        }

        override fun getItemCount() = items.size

        fun setData(data: List<UserProfile>) {
            items.clear()
            items.addAll(data)
            notifyDataSetChanged()
        }
    }
}
