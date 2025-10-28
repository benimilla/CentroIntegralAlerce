package cl.alercelab.centrointegral.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import cl.alercelab.centrointegral.R
import cl.alercelab.centrointegral.domain.Proyecto

class ProyectoAdapter(
    private val items: List<Proyecto>,
    private val onEdit: (Proyecto) -> Unit,
    private val onDelete: (Proyecto) -> Unit
) : RecyclerView.Adapter<ProyectoAdapter.ViewHolder>() {

    inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val nombre: TextView = v.findViewById(R.id.itemNombre)
        val descripcion: TextView = v.findViewById(R.id.itemDescripcion)

        init {
            v.setOnClickListener { onEdit(items[adapterPosition]) }
            v.setOnLongClickListener {
                onDelete(items[adapterPosition])
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_mantenedor, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.nombre.text = item.nombre
        holder.descripcion.text = "Proyecto registrado"
    }

    override fun getItemCount() = items.size
}
