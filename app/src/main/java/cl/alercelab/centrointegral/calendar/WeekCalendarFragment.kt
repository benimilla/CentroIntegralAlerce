package cl.alercelab.centrointegral.calendar

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import cl.alercelab.centrointegral.R
import cl.alercelab.centrointegral.data.Repos
import cl.alercelab.centrointegral.utils.TimeUtils
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.*

class WeekCalendarFragment : Fragment() {

    private lateinit var containerWeek: LinearLayout
    private val sdf = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val v = inflater.inflate(R.layout.fragment_calendar_week, container, false)
        containerWeek = v.findViewById(R.id.containerWeek)

        loadWeek()
        return v
    }

    private fun loadWeek() {
        viewLifecycleOwner.lifecycleScope.launch {
            val (inicio, fin) = TimeUtils.weekBounds(LocalDate.now())
            val actividades = Repos().actividadesEnRango(inicio, fin)

            containerWeek.removeAllViews()
            if (actividades.isEmpty()) {
                val tv = TextView(requireContext()).apply {
                    text = "No hay actividades esta semana"
                    textAlignment = View.TEXT_ALIGNMENT_CENTER
                    textSize = 16f
                    setPadding(12, 20, 12, 20)
                }
                containerWeek.addView(tv)
                return@launch
            }

            actividades.forEach { act ->
                val itemView = layoutInflater.inflate(R.layout.item_week_activity, containerWeek, false)
                val tvNombre = itemView.findViewById<TextView>(R.id.tvNombre)
                val tvTipo = itemView.findViewById<TextView>(R.id.tvTipo)
                val tvLugar = itemView.findViewById<TextView>(R.id.tvLugar)
                val tvFecha = itemView.findViewById<TextView>(R.id.tvFecha)

                tvNombre.text = act.nombre
                tvTipo.text = act.tipo
                tvLugar.text = "Lugar: ${act.lugar}"
                tvFecha.text =
                    "${sdf.format(Date(act.fechaInicio))} - ${sdf.format(Date(act.fechaFin))}"

                containerWeek.addView(itemView)
            }
        }
    }
}
