package cl.alercelab.centrointegral.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import cl.alercelab.centrointegral.R
import cl.alercelab.centrointegral.domain.SocioComunitario

class SocioComunitarioAdapter(
    private val items: MutableList<SocioComunitario>,
    private val onEdit: (SocioComunitario) -> Unit,
    private val onDelete: (SocioComunitario) -> Unit
) : RecyclerView.Adapter<SocioComunitarioAdapter.VH>() {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val nombre: TextView = v.findViewById(R.id.itemNombre)
        val descripcion: TextView = v.findViewById(R.id.itemDescripcion)
        init {
            // Al hacer clic sobre un elemento se ejecuta la acción de edición
            v.setOnClickListener { onEdit(items[bindingAdapterPosition]) }
            // Al mantener presionado un elemento se ejecuta la acción de eliminación
            v.setOnLongClickListener { onDelete(items[bindingAdapterPosition]); true }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_mantenedor, parent, false)
        return VH(v)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val it = items[position]
        holder.nombre.text = it.nombre
        holder.descripcion.visibility = View.GONE
        holder.descripcion.text = ""
    }
}
