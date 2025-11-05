package cl.alercelab.centrointegral.activities

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import cl.alercelab.centrointegral.R
import cl.alercelab.centrointegral.domain.Actividad

class ActivitiesAdapter(
    private val onEdit: (Actividad) -> Unit,
    private val onReschedule: (Actividad) -> Unit,
    private val onDelete: (Actividad) -> Unit,
    private val onDetail: (Actividad) -> Unit
) : RecyclerView.Adapter<ActivitiesAdapter.VH>() {

    private val items = mutableListOf<Actividad>()

    fun submitList(list: List<Actividad>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_activity, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val act = items[position]
        holder.bind(act)
    }

    override fun getItemCount(): Int = items.size

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        private val tvTitle = v.findViewById<TextView>(R.id.tvTitle)
        private val tvSubtitle = v.findViewById<TextView>(R.id.tvSubtitle)
        private val btnEdit = v.findViewById<ImageButton>(R.id.btnEdit)
        private val btnReschedule = v.findViewById<ImageButton>(R.id.btnReschedule)
        private val btnDelete = v.findViewById<ImageButton>(R.id.btnDelete)

        fun bind(a: Actividad) {
            tvTitle.text = a.nombre
            tvSubtitle.text = "Tipo: ${a.tipo} | Estado: ${a.estado}"

            // Abre el detalle de la actividad cuando se toca el elemento
            itemView.setOnClickListener { onDetail(a) }

            // Botón para editar la actividad seleccionada
            btnEdit.setOnClickListener { onEdit(a) }

            // Botón para reprogramar la actividad
            btnReschedule.setOnClickListener { onReschedule(a) }

            // Botón para eliminar la actividad
            btnDelete.setOnClickListener { onDelete(a) }
        }
    }
}
