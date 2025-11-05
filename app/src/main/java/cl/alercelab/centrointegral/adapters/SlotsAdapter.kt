package cl.alercelab.centrointegral.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import cl.alercelab.centrointegral.R

class SlotsAdapter(
    private val onClick: (SlotRow) -> Unit
) : RecyclerView.Adapter<SlotsAdapter.VH>() {

    private val items = mutableListOf<SlotRow>()

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvHour: TextView = v.findViewById(R.id.tvHour)
        val tvTitle: TextView = v.findViewById(R.id.tvTitle)
        val tvPlace: TextView = v.findViewById(R.id.tvPlace)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_slot_activity, parent, false))

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(h: VH, pos: Int) {
        val row = items[pos]

        h.tvHour.text = row.hora
        h.tvTitle.text = row.titulo ?: "(libre)"
        h.tvPlace.text = row.lugar ?: ""

        // Cambia la opacidad según si el slot tiene cita o está libre
        h.itemView.alpha = if (row.citaId == null) 0.7f else 1f
        // Define color de fondo: transparente si está libre, verde suave si tiene cita
        h.itemView.setBackgroundColor(
            if (row.citaId == null) Color.TRANSPARENT
            else Color.argb(24, 46, 125, 50)
        )

        // Evento click sobre cada slot: pasa el SlotRow correspondiente al callback onClick
        h.itemView.setOnClickListener {
            val adapterPos = h.bindingAdapterPosition
            if (adapterPos != RecyclerView.NO_POSITION) {
                onClick(items[adapterPos])
            }
        }
    }

    fun setData(data: List<SlotRow>) {
        items.clear()
        items.addAll(data)
        notifyDataSetChanged()
    }

    fun getItem(position: Int) = items[position]

    fun updateItem(position: Int, newItem: SlotRow) {
        items[position] = newItem
        notifyItemChanged(position)
    }
}
