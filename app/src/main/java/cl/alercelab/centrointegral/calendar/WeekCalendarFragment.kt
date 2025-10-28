package cl.alercelab.centrointegral.calendar

import android.app.AlertDialog
import android.os.Bundle
import android.view.*
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import cl.alercelab.centrointegral.R
import cl.alercelab.centrointegral.data.Repos
import cl.alercelab.centrointegral.domain.Actividad
import cl.alercelab.centrointegral.domain.Cita
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.time.LocalDate
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

                // 1) Actividades cuyo rango (fechaInicio/fechaFin) intersecta la semana
                val actividades = repos.actividadesEnRango(startMs, endMs)
                val actsById = actividades.associateBy { it.id }

                // 2) Citas de la semana (un solo campo rango: fechaInicioMillis)
                val citasSnap = db.collection("citas")
                    .whereGreaterThanOrEqualTo("fechaInicioMillis", startMs)
                    .whereLessThanOrEqualTo("fechaInicioMillis", endMs)
                    .get()
                    .await()

                val citas = citasSnap.documents.mapNotNull { d ->
                    val c = d.toObject(Cita::class.java)
                    c?.copy(id = d.id)
                }.sortedBy { it.fechaInicioMillis }

                // 3) Agrupar por día y dibujar
                containerWeek.removeAllViews()

                val days = daysOfWeek(startMs)
                days.forEach { dayStart ->
                    val dayEnd = dayStart + 24 * 60 * 60 * 1000 - 1

                    val dayCitas = citas.filter { it.fechaInicioMillis in dayStart..dayEnd }
                    if (dayCitas.isEmpty()) {
                        // Día sin citas: igual mostramos el encabezado
                        val header = TextView(requireContext()).apply {
                            text = sdfDay.format(Date(dayStart)).replaceFirstChar { it.uppercase() }
                            textSize = 16f
                            setPadding(12, 24, 12, 8)
                        }
                        val empty = TextView(requireContext()).apply {
                            text = "— Sin actividades —"
                            setPadding(24, 0, 24, 8)
                        }
                        containerWeek.addView(header)
                        containerWeek.addView(empty)
                    } else {
                        val header = TextView(requireContext()).apply {
                            text = sdfDay.format(Date(dayStart)).replaceFirstChar { it.uppercase() }
                            textSize = 16f
                            setPadding(12, 24, 12, 8)
                        }
                        containerWeek.addView(header)

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
                            tvFecha.text = "${sdfHour.format(Date(c.fechaInicioMillis))} - ${sdfHour.format(Date(c.fechaFinMillis))}"

                            // Detalle de actividad/cita en diálogo
                            row.setOnClickListener {
                                showDetailDialog(a, c)
                            }

                            containerWeek.addView(row)
                        }
                    }
                }
            } catch (e: Exception) {
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

    private fun showDetailDialog(a: Actividad?, c: Cita) {
        val msg = buildString {
            appendLine("Actividad: ${a?.nombre ?: "(desconocida)"}")
            appendLine("Tipo: ${a?.tipo ?: "-"}")
            appendLine("Periodicidad: ${a?.periodicidad ?: "-"}")
            appendLine("Lugar: ${c.lugar}")
            appendLine("Inicio: ${sdfFull.format(Date(c.fechaInicioMillis))}")
            appendLine("Fin: ${sdfFull.format(Date(c.fechaFinMillis))}")
            appendLine("Duración: ${c.duracionMin} minutos")
            a?.let {
                appendLine()
                appendLine("Oferente: ${it.oferente ?: "-"}")
                appendLine("Socio Comunitario: ${it.socioComunitario ?: "-"}")
                appendLine("Cupo: ${it.cupo ?: "-"}")
            }
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Detalle")
            .setMessage(msg)
            .setPositiveButton("Cerrar", null)
            .show()
    }

    /** Retorna (inicioSemanaLunes00:00, finSemanaDomingo23:59:59.999) en millis */
    private fun weekBounds(nowMs: Long): Pair<Long, Long> {
        val zone = ZoneId.systemDefault()
        val today = Date(nowMs).toInstant().atZone(zone).toLocalDate()
        val monday = today.minusDays(((today.dayOfWeek.value + 6) % 7).toLong()) // Lunes = inicio
        val start = monday.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = monday.plusDays(7).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        return start to end
    }

    /** Retorna lista de inicios de día (0:00) de Lunes..Domingo en ms */
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
