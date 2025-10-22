package cl.alercelab.centrointegral.admin

import android.app.AlertDialog
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
import kotlinx.coroutines.launch

class AdminUsersFragment : Fragment() {

    private lateinit var adapter: UsersAdapter
    private lateinit var emptyView: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val v = inflater.inflate(R.layout.fragment_admin_users, container, false)

        val rv = v.findViewById<RecyclerView>(R.id.rvUsers)
        emptyView = TextView(requireContext()).apply {
            text = "No hay usuarios registrados."
            visibility = View.GONE
            textAlignment = View.TEXT_ALIGNMENT_CENTER
        }

        rv.layoutManager = LinearLayoutManager(requireContext())
        adapter = UsersAdapter(
            onDelete = { uid -> deleteUser(uid) },
            onEditRole = { uid, newRole -> updateRole(uid, newRole) },
            onEditUser = { user -> showEditDialog(user) } // 👈 nuevo
        )
        rv.adapter = adapter

        loadUsers()
        return v
    }

    private fun loadUsers() {
        lifecycleScope.launch {
            val users = Repos().listAllUsers()
            adapter.setData(users)
            emptyView.visibility = if (users.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun deleteUser(uid: String) {
        lifecycleScope.launch {
            Repos().deleteUser(uid)
            Toast.makeText(requireContext(), "Usuario eliminado", Toast.LENGTH_SHORT).show()
            loadUsers()
        }
    }

    private fun updateRole(uid: String, newRole: String) {
        lifecycleScope.launch {
            Repos().updateUserRole(uid, newRole)
            Toast.makeText(requireContext(), "Rol actualizado", Toast.LENGTH_SHORT).show()
            loadUsers()
        }
    }

    // 🔹 NUEVO: Mostrar diálogo para editar nombre/correo
    private fun showEditDialog(user: UserProfile) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_user, null)
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
                        // Actualizar en Firestore
                        val db = Repos()
                        db.updateUserData(user.uid, nuevoNombre, nuevoEmail)
                        Toast.makeText(requireContext(), "Usuario actualizado", Toast.LENGTH_SHORT).show()
                        loadUsers()
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // -------------------------------------------------------------
    // ADAPTADOR DE USUARIOS
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
            val btnEdit: Button = v.findViewById(R.id.btnEdit) // 👈 nuevo
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

            val roles = listOf("usuario", "gestor", "admin")
            holder.role.adapter = ArrayAdapter(
                holder.itemView.context,
                android.R.layout.simple_spinner_item,
                roles
            )
            holder.role.setSelection(roles.indexOf(u.rol))
            holder.role.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
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
