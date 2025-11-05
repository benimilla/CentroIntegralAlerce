package cl.alercelab.centrointegral.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import cl.alercelab.centrointegral.R
import cl.alercelab.centrointegral.domain.Oferente

class OferenteAdapter(
    private val items: MutableList<Oferente>,
    private val onEdit: (Oferente) -> Unit,
    private val onDelete: (Oferente) -> Unit
) : RecyclerView.Adapter<OferenteAdapter.VH>() {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val nombre: TextView = v.findViewById(R.id.itemNombre)
        val descripcion: TextView = v.findViewById(R.id.itemDescripcion)
        init {
            // Al hacer clic sobre un elemento se ejecuta la acción para editar el oferente
            v.setOnClickListener { onEdit(items[bindingAdapterPosition]) }
            // Al mantener presionado un elemento se ejecuta la acción para eliminar el oferente
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
        holder.descripcion.visibility = View.VISIBLE
        holder.descripcion.text = "Docente responsable: ${it.docenteResponsable}"
    }
}
