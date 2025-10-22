package cl.alercelab.centrointegral.calendar

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import cl.alercelab.centrointegral.databinding.FragmentCalendarWeekBinding
import cl.alercelab.centrointegral.data.Repos
import cl.alercelab.centrointegral.domain.Actividad
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class WeekCalendarFragment : Fragment() {

    private var _binding: FragmentCalendarWeekBinding? = null
    private val binding get() = _binding!!
    private val repos = Repos()
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCalendarWeekBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadWeekActivities()
    }

    private fun loadWeekActivities() {
        lifecycleScope.launch {
            try {
                val desde = System.currentTimeMillis()
                val hasta = desde + (7 * 24 * 60 * 60 * 1000L) // +7 días

                val actividades = repos.actividadesEnRango(desde = desde, hasta = hasta)

                if (actividades.isEmpty()) {
                    binding.containerWeek.removeAllViews()
                    val emptyText = TextView(requireContext()).apply {
                        text = "No hay actividades programadas esta semana."
                        textAlignment = View.TEXT_ALIGNMENT_CENTER
                        textSize = 16f
                        setPadding(20, 40, 20, 40)
                    }
                    binding.containerWeek.addView(emptyText)
                } else {
                    showActivities(actividades)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                binding.containerWeek.removeAllViews()
                val errorText = TextView(requireContext()).apply {
                    text = "Error al cargar actividades."
                    textAlignment = View.TEXT_ALIGNMENT_CENTER
                    textSize = 16f
                    setPadding(20, 40, 20, 40)
                }
                binding.containerWeek.addView(errorText)
            }
        }
    }

    private fun showActivities(actividades: List<Actividad>) {
        binding.containerWeek.removeAllViews()

        actividades.sortedBy { it.fechaInicio }.forEach { act ->
            val view = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(20, 20, 20, 30)

                val title = TextView(requireContext()).apply {
                    text = "• ${act.nombre}"
                    textSize = 18f
                    setPadding(0, 10, 0, 5)
                }

                val date = TextView(requireContext()).apply {
                    text = "Inicio: ${dateFormat.format(Date(act.fechaInicio))}"
                    textSize = 14f
                }

                val location = TextView(requireContext()).apply {
                    text = "Lugar: ${act.lugar ?: "Centro"}"
                    textSize = 14f
                }

                addView(title)
                addView(date)
                addView(location)
            }

            binding.containerWeek.addView(view)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
