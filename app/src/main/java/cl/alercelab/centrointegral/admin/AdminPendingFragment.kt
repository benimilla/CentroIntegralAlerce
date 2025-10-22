package cl.alercelab.centrointegral.admin

import android.os.Bundle
import android.view.*
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cl.alercelab.centrointegral.R
import cl.alercelab.centrointegral.data.Repos
import cl.alercelab.centrointegral.domain.UserProfile
import kotlinx.coroutines.launch

class AdminPendingFragment : Fragment() {

    private lateinit var adapter: PendingAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val v = inflater.inflate(R.layout.fragment_admin_pending, container, false)

        val rv = v.findViewById<RecyclerView>(R.id.rvPending)
        rv.layoutManager = LinearLayoutManager(requireContext())
        adapter = PendingAdapter(
            onApprove = { uid -> approveUser(uid) },
            onReject = { uid -> rejectUser(uid) }
        )
        rv.adapter = adapter
        load()
        return v
    }

    private fun load() {
        viewLifecycleOwner.lifecycleScope.launch {
            val result = Repos().listPendingUsers()
            adapter.setData(result)
        }
    }

    private fun approveUser(uid: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            Repos().approveUser(uid)
            Toast.makeText(requireContext(), "Usuario aprobado", Toast.LENGTH_SHORT).show()
            load()
        }
    }

    private fun rejectUser(uid: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            Repos().rejectUser(uid)
            Toast.makeText(requireContext(), "Usuario rechazado", Toast.LENGTH_SHORT).show()
            load()
        }
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
