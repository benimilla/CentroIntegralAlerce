package cl.alercelab.centrointegral.admin

import android.app.AlertDialog
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cl.alercelab.centrointegral.R
import cl.alercelab.centrointegral.data.Repos
import cl.alercelab.centrointegral.domain.UserProfile
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AdminUsersFragment : Fragment() {

    private lateinit var adapter: UsersAdapter
    private lateinit var emptyView: TextView
    private val repos = Repos()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val v = inflater.inflate(R.layout.fragment_admin_users, container, false)

        val rv = v.findViewById<RecyclerView>(R.id.rvUsers)
        val btnNuevoUsuario = v.findViewById<Button>(R.id.btnNuevoUsuario)
        emptyView = TextView(requireContext()).apply {
            text = "No hay usuarios registrados."
            visibility = View.GONE
            textAlignment = View.TEXT_ALIGNMENT_CENTER
        }

        rv.layoutManager = LinearLayoutManager(requireContext())
        adapter = UsersAdapter(
            onDelete = { uid -> deleteUser(v, uid) },
            onEditRole = { uid, newRole -> updateRole(v, uid, newRole) },
            onEditUser = { user -> showEditDialog(v, user) }
        )
        rv.adapter = adapter

        btnNuevoUsuario.setOnClickListener {
            Toast.makeText(requireContext(), "Creando nuevo usuario...", Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.action_adminUsers_to_registerFragment)
        }

        loadUsers(v)
        return v
    }

    /** 🔹 Carga la lista de usuarios */
    private fun loadUsers(view: View) {
        lifecycleScope.launch {
            val users = repos.listAllUsers()
            adapter.setData(users)
            emptyView.visibility = if (users.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    /** 🗑 Elimina un usuario y registra auditoría */
    private fun deleteUser(view: View, uid: String) {
        lifecycleScope.launch {
            try {
                repos.deleteUser(uid)
                registrarAuditoria(
                    usuarioId = "admin_local",
                    usuarioNombre = "Administrador",
                    modulo = "Usuarios",
                    accion = "Eliminación",
                    entidadId = uid,
                    descripcion = "Se eliminó el usuario con ID $uid"
                )
                Toast.makeText(requireContext(), "Usuario eliminado", Toast.LENGTH_SHORT).show()
                loadUsers(view)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    /** 🔁 Actualiza el rol del usuario */
    private fun updateRole(view: View, uid: String, newRole: String) {
        lifecycleScope.launch {
            try {
                repos.updateUserRole(uid, newRole)
                registrarAuditoria(
                    usuarioId = "admin_local",
                    usuarioNombre = "Administrador",
                    modulo = "Usuarios",
                    accion = "Cambio de rol",
                    entidadId = uid,
                    descripcion = "Rol cambiado a '$newRole'",
                    cambios = mapOf("rol" to newRole)
                )
                Toast.makeText(requireContext(), "Rol actualizado", Toast.LENGTH_SHORT).show()
                loadUsers(view)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    /** ✏️ Muestra diálogo para editar usuario */
    private fun showEditDialog(view: View, user: UserProfile) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_edit_user, null)
        val etNombre = dialogView.findViewById<EditText>(R.id.etNombre)
        val etEmail = dialogView.findViewById<EditText>(R.id.etEmail)

        etNombre.setText(user.nombre)
        etEmail.setText(user.email)

        AlertDialog.Builder(requireContext())
            .setTitle("Editar usuario")
            .setView(dialogView)
            .setPositiveButton("Guardar") { _, _ ->
                val nuevoNombre = etNombre.text.toString().trim()
                val nuevoEmail = etEmail.text.toString().trim()

                if (nuevoNombre.isEmpty() || nuevoEmail.isEmpty()) {
                    Toast.makeText(requireContext(), "Campos vacíos", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                lifecycleScope.launch {
                    try {
                        repos.updateUserData(user.uid, nuevoNombre, nuevoEmail)
                        registrarAuditoria(
                            usuarioId = "admin_local",
                            usuarioNombre = "Administrador",
                            modulo = "Usuarios",
                            accion = "Edición",
                            entidadId = user.uid,
                            descripcion = "Se actualizó el usuario '${user.nombre}'",
                            cambios = mapOf(
                                "nombre" to nuevoNombre,
                                "email" to nuevoEmail
                            )
                        )
                        Toast.makeText(requireContext(), "Usuario actualizado", Toast.LENGTH_SHORT).show()
                        loadUsers(view)
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    /** 🧾 Guarda un registro en la colección "auditoria" de Firestore. */
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

    // -------------------------------------------------------------
    // Adaptador
    // -------------------------------------------------------------
    class UsersAdapter(
        private val onDelete: (String) -> Unit,
        private val onEditRole: (String, String) -> Unit,
        private val onEditUser: (UserProfile) -> Unit
    ) : RecyclerView.Adapter<UsersAdapter.VH>() {

        private val items = mutableListOf<UserProfile>()

        class VH(v: View) : RecyclerView.ViewHolder(v) {
            val name: TextView = v.findViewById(R.id.tvName)
            val email: TextView = v.findViewById(R.id.tvEmail)
            val role: Spinner = v.findViewById(R.id.spRole)
            val btnDelete: Button = v.findViewById(R.id.btnDelete)
            val btnEdit: Button = v.findViewById(R.id.btnEdit)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_user_admin, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val u = items[position]
            holder.name.text = u.nombre
            holder.email.text = u.email

            // 🔹 Asegura que el texto del rol sea visible
            val roles = listOf("usuario", "gestor", "admin")
            val adapterSpinner = ArrayAdapter(
                holder.itemView.context,
                android.R.layout.simple_spinner_item,
                roles
            )
            adapterSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            holder.role.adapter = adapterSpinner
            holder.role.setSelection(roles.indexOf(u.rol))
            holder.role.setPopupBackgroundResource(android.R.color.white)

            holder.role.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>, view: View?, pos: Int, id: Long
                ) {
                    val selected = roles[pos]
                    if (selected != u.rol) onEditRole(u.uid, selected)
                }
                override fun onNothingSelected(parent: AdapterView<*>) {}
            }

            holder.btnEdit.setOnClickListener { onEditUser(u) }
            holder.btnDelete.setOnClickListener { onDelete(u.uid) }
        }

        override fun getItemCount(): Int = items.size

        fun setData(list: List<UserProfile>) {
            items.clear()
            items.addAll(list)
            notifyDataSetChanged()
        }
    }
}
