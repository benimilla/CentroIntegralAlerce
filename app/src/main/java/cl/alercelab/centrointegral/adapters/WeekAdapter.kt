package cl.alercelab.centrointegral.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import cl.alercelab.centrointegral.R
import cl.alercelab.centrointegral.domain.Actividad
import java.text.SimpleDateFormat
import java.util.*

class WeekAdapter : RecyclerView.Adapter<WeekAdapter.ViewHolder>() {

    private val data = mutableListOf<Actividad>()
    private val sdf = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault())

    fun setData(list: List<Actividad>) {
        data.clear()
        data.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_week_activity, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = data[position]
        holder.tvNombre.text = item.nombre
        holder.tvTipo.text = item.tipo
        holder.tvLugar.text = "Lugar: ${item.lugar}"
        holder.tvFecha.text =
            "${sdf.format(Date(item.fechaInicio))} - ${sdf.format(Date(item.fechaFin))}"
    }

    override fun getItemCount() = data.size

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val tvNombre: TextView = v.findViewById(R.id.tvNombre)
        val tvTipo: TextView = v.findViewById(R.id.tvTipo)
        val tvLugar: TextView = v.findViewById(R.id.tvLugar)
        val tvFecha: TextView = v.findViewById(R.id.tvFecha)
    }
}
