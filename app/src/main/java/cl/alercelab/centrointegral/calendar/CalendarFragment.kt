package cl.alercelab.centrointegral.calendar

import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cl.alercelab.centrointegral.R
import cl.alercelab.centrointegral.adapters.SlotRow
import cl.alercelab.centrointegral.adapters.SlotsAdapter
import cl.alercelab.centrointegral.data.Repos
import cl.alercelab.centrointegral.domain.UserProfile
import cl.alercelab.centrointegral.ui.ActivityDetailBottomSheet
import cl.alercelab.centrointegral.utils.TimeUtils
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime

class CalendarFragment : Fragment() {

    private lateinit var adapter: SlotsAdapter
    private var perfil: UserProfile? = null

    private lateinit var rv: RecyclerView
    private lateinit var spTipo: Spinner
    private lateinit var spRango: Spinner
    private lateinit var etBuscar: EditText
    private lateinit var calendarView: CalendarView
    private lateinit var tvEmpty: TextView

    private var currentDate: LocalDate = LocalDate.now()
    private val horas = (8..20).map { String.format("%02d:00", it) }

    private var dragFromPos: Int = -1
    private var dragFromCitaId: String? = null
    private var dragFromLugar: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, s: Bundle?): View {
        val v = inflater.inflate(R.layout.fragment_calendar, container, false)

        rv = v.findViewById(R.id.rvDay)
        spTipo = v.findViewById(R.id.spTipo)
        spRango = v.findViewById(R.id.spRango)
        etBuscar = v.findViewById(R.id.etBuscar)
        calendarView = v.findViewById(R.id.calendarView)
        tvEmpty = v.findViewById(R.id.tvEmpty)

        rv.layoutManager = LinearLayoutManager(requireContext())
        adapter = SlotsAdapter { row ->
            if (row.citaId != null) {
                val bs = ActivityDetailBottomSheet.new(
                    perfil?.rol ?: "usuario",
                    row.titulo ?: "Actividad",
                    row.lugar ?: "-",
                    row.hora
                )
                bs.show(parentFragmentManager, "detail")
            }
        }
        rv.adapter = adapter

        spRango.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            listOf("Día", "Semana", "Mes", "Año")
        )

        spTipo.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            listOf(
                "Todos", "Capacitación", "Taller", "Charlas",
                "Atenciones", "Operativo", "Operativo rural",
                "Práctica profesional", "Diagnóstico"
            )
        )

        val ith = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
        ) {
            override fun onMove(
                rv: RecyclerView,
                vh: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val from = vh.bindingAdapterPosition
                val to = target.bindingAdapterPosition
                val fromItem = adapter.getItem(from)
                val toItem = adapter.getItem(to)

                if (dragFromPos == -1 && fromItem.citaId != null) {
                    dragFromPos = from
                    dragFromCitaId = fromItem.citaId
                    dragFromLugar = fromItem.lugar
                }

                if (fromItem.citaId != null && toItem.citaId == null) {
                    adapter.updateItem(to, fromItem.copy(hora = toItem.hora))
                    adapter.updateItem(from, toItem)
                }
                return true
            }

            override fun clearView(rv: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(rv, viewHolder)
                if (dragFromPos != -1) {
                    val targetPos = viewHolder.bindingAdapterPosition
                    val targetItem = adapter.getItem(targetPos)
                    val citaId = dragFromCitaId
                    val newHour = targetItem.hora
                    val lugar = targetItem.lugar ?: dragFromLugar ?: "Centro"
                    persistReschedule(citaId, newHour, lugar)
                    dragFromPos = -1
                    dragFromCitaId = null
                    dragFromLugar = null
                }
            }

            override fun onSwiped(vh: RecyclerView.ViewHolder, dir: Int) {}
        }
        ItemTouchHelper(ith).attachToRecyclerView(rv)

        calendarView.setOnDateChangeListener { _, y, m, d ->
            currentDate = LocalDate.of(y, m + 1, d)
            loadDay()
        }

        spTipo.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                loadDay()
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        viewLifecycleOwner.lifecycleScope.launch {
            perfil = Repos().currentUserProfile()
            loadDay()
        }

        return v
    }

    private fun persistReschedule(citaId: String?, newHour: String, lugar: String) {
        if (citaId == null) {
            loadDay()
            return
        }
        val ldt = LocalDateTime.parse("${currentDate}T${newHour}")
        val millisInicio = TimeUtils.localDateTimeToEpochMillis(ldt)
        val millisFin = millisInicio + (60 * 60 * 1000) // duración 1 hora

        viewLifecycleOwner.lifecycleScope.launch {
            // Ajustado: coincide con la firma updateCitaTiempoYLugar(citaId, inicio, fin, lugar)
            Repos().updateCitaTiempoYLugar(citaId, millisInicio, millisFin, lugar)
            loadDay()
        }
    }

    private fun loadDay() {
        val (start, end) = TimeUtils.dayBounds(currentDate)
        val tipoSel = spTipo.selectedItem?.toString()?.takeIf { it != "Todos" }
        val q = etBuscar.text?.toString()

        viewLifecycleOwner.lifecycleScope.launch {
            val p = perfil ?: return@launch

            val rows = horas.map { h -> SlotRow(h, null, null, null) }.toMutableList()

            // ✅ Ajustado: solo 2 parámetros
            val data = Repos().citasEnRango(start, end)

            data.forEach { cita ->
                val hour = TimeUtils.hourString(cita.fechaInicioMillis)
                val idx = horas.indexOf(hour)
                if (idx >= 0) {
                    rows[idx] = SlotRow(
                        hour,
                        "Actividad", // el nombre puede venir de la actividad si querés unir datos
                        cita.lugar,
                        cita.id
                    )
                }
            }

            tvEmpty.visibility = if (data.isEmpty()) View.VISIBLE else View.GONE
            adapter.setData(rows)
        }
    }
}
