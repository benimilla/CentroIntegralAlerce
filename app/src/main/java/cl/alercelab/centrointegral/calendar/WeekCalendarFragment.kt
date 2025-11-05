package cl.alercelab.centrointegral.calendar

import android.app.AlertDialog
import android.os.Bundle
import android.view.*
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import cl.alercelab.centrointegral.R
import cl.alercelab.centrointegral.data.Repos
import cl.alercelab.centrointegral.domain.Actividad
import cl.alercelab.centrointegral.domain.Cita
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.time.ZoneId
import java.util.*

class WeekCalendarFragment : Fragment() {

    private lateinit var containerWeek: LinearLayout
    private val repos = Repos()
    private val db = FirebaseFirestore.getInstance()

    private val sdfDay = SimpleDateFormat("EEEE dd/MM", Locale.getDefault())
    private val sdfHour = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val sdfFull = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val v = inflater.inflate(R.layout.fragment_calendar_week, container, false)
        containerWeek = v.findViewById(R.id.containerWeek)
        loadCurrentWeek()
        return v
    }

    private fun loadCurrentWeek() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val (startMs, endMs) = weekBounds(System.currentTimeMillis())

                val actividades = repos.actividadesEnRango(startMs, endMs)
                val actsById = actividades.associateBy { it.id }

                // Consulta a Firestore todas las citas que se encuentren dentro del rango semanal actual
                val citasSnap = db.collection("citas")
                    .whereGreaterThanOrEqualTo("fechaInicioMillis", startMs)
                    .whereLessThanOrEqualTo("fechaInicioMillis", endMs)
                    .get()
                    .await()

                // Convierte los documentos en objetos Cita y los ordena por fecha
                val citas = citasSnap.documents.mapNotNull { d ->
                    val c = d.toObject(Cita::class.java)
                    c?.copy(id = d.id)
                }.sortedBy { it.fechaInicioMillis }

                containerWeek.removeAllViews()

                val days = daysOfWeek(startMs)
                days.forEach { dayStart ->
                    val dayEnd = dayStart + 24 * 60 * 60 * 1000 - 1
                    val dayCitas = citas.filter { it.fechaInicioMillis in dayStart..dayEnd }

                    val header = TextView(requireContext()).apply {
                        text = sdfDay.format(Date(dayStart)).replaceFirstChar { it.uppercase() }
                        textSize = 16f
                        setPadding(12, 24, 12, 8)
                    }
                    containerWeek.addView(header)

                    if (dayCitas.isEmpty()) {
                        // Muestra texto cuando no hay actividades para el día
                        val empty = TextView(requireContext()).apply {
                            text = "— Sin actividades —"
                            setPadding(24, 0, 24, 8)
                        }
                        containerWeek.addView(empty)
                    } else {
                        dayCitas.forEach { c ->
                            val a = actsById[c.actividadId]
                            val row = layoutInflater.inflate(R.layout.item_week_activity, containerWeek, false)

                            val tvNombre = row.findViewById<TextView>(R.id.tvNombre)
                            val tvTipo = row.findViewById<TextView>(R.id.tvTipo)
                            val tvLugar = row.findViewById<TextView>(R.id.tvLugar)
                            val tvFecha = row.findViewById<TextView>(R.id.tvFecha)

                            tvNombre.text = a?.nombre ?: "(Actividad)"
                            tvTipo.text = a?.tipo ?: "-"
                            tvLugar.text = "Lugar: ${c.lugar}"
                            tvFecha.text =
                                "${sdfHour.format(Date(c.fechaInicioMillis))} - ${sdfHour.format(Date(c.fechaFinMillis))}"

                            // Abre el fragmento de detalle de la cita al tocar la actividad
                            row.setOnClickListener {
                                val frag = CitaDetalleFragment().apply {
                                    arguments = Bundle().apply {
                                        putString("citaId", c.id)
                                        putString("actividadId", c.actividadId)
                                    }
                                }
                                parentFragmentManager.beginTransaction()
                                    .replace(R.id.nav_host_fragment, frag)
                                    .addToBackStack(null)
                                    .commit()
                            }

                            containerWeek.addView(row)
                        }
                    }
                }
            } catch (e: Exception) {
                // Muestra un mensaje de error si ocurre un problema al cargar las citas
                containerWeek.removeAllViews()
                val tv = TextView(requireContext()).apply {
                    text = "Error al cargar: ${e.message}"
                    textAlignment = View.TEXT_ALIGNMENT_CENTER
                    textSize = 16f
                    setPadding(12, 20, 12, 20)
                }
                containerWeek.addView(tv)
            }
        }
    }

    private fun weekBounds(nowMs: Long): Pair<Long, Long> {
        val zone = ZoneId.systemDefault()
        val today = Date(nowMs).toInstant().atZone(zone).toLocalDate()
        val monday = today.minusDays(((today.dayOfWeek.value + 6) % 7).toLong())
        val start = monday.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = monday.plusDays(7).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        return start to end
    }

    private fun daysOfWeek(weekStartMs: Long): List<Long> {
        val out = mutableListOf<Long>()
        var d = weekStartMs
        repeat(7) {
            out.add(d)
            d += 24 * 60 * 60 * 1000
        }
        return out
    }
}

// Fragmento encargado de mostrar el calendario semanal, obteniendo las citas desde Firestore,
// agrupándolas por día y permitiendo acceder al detalle de cada cita al seleccionarla.