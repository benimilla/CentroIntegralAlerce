package cl.alercelab.centrointegral.calendar

import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import cl.alercelab.centrointegral.R
import cl.alercelab.centrointegral.data.Repos
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class CitaDetalleFragment : Fragment() {

    private val repos = Repos()
    private val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    private lateinit var tvActividad: TextView
    private lateinit var tvLugar: TextView
    private lateinit var tvDuracion: TextView
    private lateinit var tvObservaciones: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(R.layout.fragment_cita_detalle, container, false)
        tvActividad = v.findViewById(R.id.tvActividad)
        tvLugar = v.findViewById(R.id.tvLugar)
        tvDuracion = v.findViewById(R.id.tvDuracion)
        tvObservaciones = v.findViewById(R.id.tvObservaciones)
        return v
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val citaId = arguments?.getString("citaId") ?: return

        lifecycleScope.launch {
            val cita = repos.obtenerCitasPorActividad(citaId).firstOrNull() ?: return@launch
            val duracion = ((cita.fechaFinMillis - cita.fechaInicioMillis) / (1000 * 60)).toInt()

            tvActividad.text = "Cita de actividad ID: ${cita.actividadId}"
            tvLugar.text = "Lugar: ${cita.lugar}"
            tvDuracion.text = "Duración: $duracion min"
            tvObservaciones.text = "Observaciones: ${cita.observaciones ?: "-"}"
        }
    }
}
