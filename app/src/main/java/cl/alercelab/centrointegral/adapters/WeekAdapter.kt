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

class WeekAdapter(
    private var actividades: List<Actividad>
) : RecyclerView.Adapter<WeekAdapter.ViewHolder>() {

    private val sdfFecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val sdfHora = SimpleDateFormat("HH:mm", Locale.getDefault())

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNombre: TextView = itemView.findViewById(R.id.tvNombre)
        val tvTipo: TextView = itemView.findViewById(R.id.tvTipo)
        val tvLugar: TextView = itemView.findViewById(R.id.tvLugar)
        val tvFecha: TextView = itemView.findViewById(R.id.tvFecha)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_week_activity, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val actividad = actividades[position]
        holder.tvNombre.text = actividad.nombre
        holder.tvTipo.text = actividad.tipo
        holder.tvLugar.text = "Lugar: ${actividad.lugar}"
        holder.tvFecha.text = "Inicio: ${sdfFecha.format(Date(actividad.fechaInicio))}"
    }

    override fun getItemCount(): Int = actividades.size
}
