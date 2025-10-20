package cl.alercelab.centrointegral.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

class AdminFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val v = inflater.inflate(R.layout.fragment_admin, container, false)

        val rv = v.findViewById<RecyclerView>(R.id.rvRequests)
        rv.layoutManager = LinearLayoutManager(requireContext())

        // 1) Declarar adapter antes
        lateinit var adapter: RequestsAdapter

        // 2) Inicializar adapter
        adapter = RequestsAdapter(
            onApprove = { uid ->
                viewLifecycleOwner.lifecycleScope.launch {
                    Repos().approveUser(uid)
                    Toast.makeText(requireContext(), "Aprobado", Toast.LENGTH_SHORT).show()
                    load(adapter)
                }
            },
            onReject = { uid ->
                viewLifecycleOwner.lifecycleScope.launch {
                    Repos().rejectUser(uid)
                    Toast.makeText(requireContext(), "Rechazado", Toast.LENGTH_SHORT).show()
                    load(adapter)
                }
            }
        )

        // 3) Asignar
        rv.adapter = adapter

        // 4) Cargar contenido
        load(adapter)

        return v
    }

    private fun load(adapter: RequestsAdapter) {
        viewLifecycleOwner.lifecycleScope.launch {
            val result = Repos().listPendingUsers()
            adapter.setData(result)
        }
    }

    class RequestsAdapter(
        private val onApprove: (String) -> Unit,
        private val onReject: (String) -> Unit
    ) : RecyclerView.Adapter<RequestsAdapter.VH>() {

        private val items = mutableListOf<UserProfile>()

        class VH(v: View) : RecyclerView.ViewHolder(v) {
            val tvName: TextView = v.findViewById(R.id.tvName)
            val tvEmail: TextView = v.findViewById(R.id.tvEmail)
            val tvRole: TextView = v.findViewById(R.id.tvRole)
            val btnApprove: Button = v.findViewById(R.id.btnApprove)
            val btnReject: Button = v.findViewById(R.id.btnReject)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_request, parent, false)
            return VH(view)
        }

        override fun getItemCount(): Int = items.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val u = items[position]
            holder.tvName.text = u.nombre
            holder.tvEmail.text = u.email
            holder.tvRole.text = "Rol solicitado: ${u.rol}"

            holder.btnApprove.setOnClickListener { onApprove(u.uid) }
            holder.btnReject.setOnClickListener { onReject(u.uid) }
        }

        fun setData(data: List<UserProfile>) {
            items.clear()
            items.addAll(data)
            notifyDataSetChanged()
        }
    }
}
