package cl.alercelab.centrointegral.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import cl.alercelab.centrointegral.R
import cl.alercelab.centrointegral.domain.Lugar

class LugarAdapter(
    private val items: MutableList<Lugar>,
    private val onEdit: (Lugar) -> Unit,
    private val onDelete: (Lugar) -> Unit
) : RecyclerView.Adapter<LugarAdapter.VH>() {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val nombre: TextView = v.findViewById(R.id.itemNombre)
        val descripcion: TextView = v.findViewById(R.id.itemDescripcion)
        init {
            // Al hacer clic en el ítem se ejecuta la acción de edición del lugar
            v.setOnClickListener { onEdit(items[bindingAdapterPosition]) }
            // Al mantener presionado el ítem se ejecuta la acción de eliminación del lugar
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
        val cupoTxt = it.cupo?.toString() ?: "—"
        holder.descripcion.visibility = View.VISIBLE
        holder.descripcion.text = "Cupo: $cupoTxt"
    }
}
