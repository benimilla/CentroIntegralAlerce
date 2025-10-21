package cl.alercelab.centrointegral.calendar

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import cl.alercelab.centrointegral.R

class WeekGridAdapter(
    private val canDrag: Boolean,
    private val onClick: (WeekCell) -> Unit
) : RecyclerView.Adapter<WeekGridAdapter.VH>() {

    private val items = mutableListOf<WeekCell>()

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvHour: TextView = v.findViewById(R.id.tvHour)
        val tvTitle: TextView = v.findViewById(R.id.tvTitle)
        val tvPlace: TextView = v.findViewById(R.id.tvPlace)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_slot_week, parent, false)
        return VH(v)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(h: VH, pos: Int) {
        val cell = items[pos]
        h.tvHour.text = cell.hour
        h.tvTitle.text = cell.titulo ?: "(libre)"
        h.tvPlace.text = cell.lugar ?: ""

        // si hay cita → fondo suave verdoso; si no → transparente (borde visible viene del Card)
        h.itemView.alpha = if (cell.citaId == null) 0.9f else 1f
        h.itemView.setBackgroundColor(
            if (cell.citaId == null) Color.TRANSPARENT else Color.argb(26, 46, 125, 50) // verde muy suave
        )

        h.itemView.setOnClickListener { onClick(cell) }
    }

    fun setData(data: List<WeekCell>) {
        items.clear()
        items.addAll(data)
        notifyDataSetChanged()
    }

    fun getItem(position: Int) = items[position]

    fun updateItem(position: Int, newItem: WeekCell) {
        items[position] = newItem
        notifyItemChanged(position)
    }
}
