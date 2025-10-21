package cl.alercelab.centrointegral.calendar

import android.os.Bundle
import android.view.*
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import cl.alercelab.centrointegral.R
import cl.alercelab.centrointegral.data.Repos
import cl.alercelab.centrointegral.domain.UserProfile
import cl.alercelab.centrointegral.ui.ActivityDetailBottomSheet
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.launch
import java.time.*

class WeekCalendarFragment : Fragment() {

    private lateinit var rv: RecyclerView
    private lateinit var tvWeekRange: TextView
    private lateinit var chipGroup: ChipGroup
    private lateinit var adapter: WeekGridAdapter
    private var perfil: UserProfile? = null

    private var weekStart: LocalDate = LocalDate.now().with(DayOfWeek.MONDAY)
    private val hours: List<String> = (8..20).map { String.format("%02d:00", it) } // 13 slots

    // Filtro por tipo (null = Todos)
    private var selectedTipo: String? = null

    // Tipos de actividad que definiste
    private val tipos = listOf(
        "Todos",
        "Capacitación",
        "Taller",
        "Charlas",
        "Atenciones",
        "Operativo en oficina",
        "Operativo rural",
        "Operativo",
        "Práctica profesional",
        "Diagnostico"
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val v = inflater.inflate(R.layout.fragment_calendar_week, container, false)
        rv = v.findViewById(R.id.rvWeek)
        tvWeekRange = v.findViewById(R.id.tvWeekRange)
        chipGroup = v.findViewById(R.id.chipGroupTipos)

        // Header días (L..D)
        val dayLabels = listOf<TextView>(
            v.findViewById(R.id.day0), v.findViewById(R.id.day1), v.findViewById(R.id.day2),
            v.findViewById(R.id.day3), v.findViewById(R.id.day4), v.findViewById(R.id.day5), v.findViewById(R.id.day6)
        )
        updateHeaderAndRange(dayLabels)

        v.findViewById<ImageButton>(R.id.btnPrev).setOnClickListener {
            weekStart = weekStart.minusWeeks(1)
            updateHeaderAndRange(dayLabels)
            loadWeek()
        }
        v.findViewById<ImageButton>(R.id.btnNext).setOnClickListener {
            weekStart = weekStart.plusWeeks(1)
            updateHeaderAndRange(dayLabels)
            loadWeek()
        }

        rv.layoutManager = GridLayoutManager(requireContext(), 7)
        adapter = WeekGridAdapter(
            canDrag = false,
            onClick = { cell ->
                if (cell.citaId != null) {
                    val rol = perfil?.rol ?: "usuario"
                    ActivityDetailBottomSheet.new(
                        rol,
                        cell.titulo ?: "Actividad",
                        cell.lugar ?: "-",
                        cell.hour
                    ).show(parentFragmentManager, "detail")
                }
            }
        )
        rv.adapter = adapter

        // Chips de filtro (verdes)
        setupChips()

        // Cargar perfil y semana
        viewLifecycleOwner.lifecycleScope.launch {
            perfil = Repos().currentUserProfile()
            setupDragIfAllowed()
            loadWeek()
        }

        return v
    }

    /** Construye chips verdes con selección única */
    private fun setupChips() {
        chipGroup.removeAllViews()
        tipos.forEach { nombre ->
            val chip = Chip(requireContext(), null, com.google.android.material.R.style.Widget_Material3_Chip_Filter)
            chip.text = nombre
            chip.isCheckable = true
            chip.isCheckedIconVisible = false
            chip.isClickable = true
            chip.isFocusable = true

            // Colores verdes
            chip.chipStrokeWidth = resources.getDimension(R.dimen.chip_stroke)
            chip.chipStrokeColor = resources.getColorStateList(R.color.chip_stroke_selector, null)
            chip.setChipBackgroundColorResource(R.color.chip_bg_selector)
            chip.setTextColor(resources.getColorStateList(R.color.chip_text_selector, null))

            // Selección por defecto "Todos"
            if (nombre == "Todos") chip.isChecked = true

            chip.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    selectedTipo = if (nombre == "Todos") null else nombre
                    loadWeek()
                }
            }
            chipGroup.addView(chip)
        }
    }

    /** Encabezado y rango semana */
    private fun updateHeaderAndRange(dayLabels: List<TextView>) {
        val days = (0..6).map { weekStart.plusDays(it.toLong()) }
        val fmtDay = java.time.format.DateTimeFormatter.ofPattern("EEE d")
        days.forEachIndexed { i, d -> dayLabels[i].text = d.format(fmtDay) }

        val fmtRange = java.time.format.DateTimeFormatter.ofPattern("d MMM")
        val end = weekStart.plusDays(6)
        tvWeekRange.text = "${weekStart.format(fmtRange)} – ${end.format(fmtRange)}"
    }

    private fun ldtToMillis(ldt: LocalDateTime): Long =
        ldt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private fun weekBoundsMillis(): Pair<Long, Long> {
        val start = ldtToMillis(weekStart.atStartOfDay())
        val end = ldtToMillis(weekStart.plusDays(7).atStartOfDay())
        return start to end
    }

    private fun posOf(dayIndex: Int, hourIndex: Int): Int = hourIndex * 7 + dayIndex

    /** Drag & drop solo admin/gestor */
    private fun setupDragIfAllowed() {
        val canDrag = perfil?.rol in listOf("admin", "gestor")
        if (!canDrag) return

        val ith = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN or ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT, 0
        ) {
            private var dragFromCitaId: String? = null
            private var dragFromPos: Int = -1
            private var dragFromLugar: String? = null

            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                val from = viewHolder.bindingAdapterPosition
                val to = target.bindingAdapterPosition
                val fromItem = adapter.getItem(from)
                val toItem = adapter.getItem(to)

                if (dragFromPos == -1 && fromItem.citaId != null) {
                    dragFromPos = from
                    dragFromCitaId = fromItem.citaId
                    dragFromLugar = fromItem.lugar
                }

                if (fromItem.citaId != null && toItem.citaId == null) {
                    adapter.updateItem(to, fromItem.copy(dayIndex = toItem.dayIndex, hour = toItem.hour))
                    adapter.updateItem(from, toItem.copy(dayIndex = fromItem.dayIndex, hour = fromItem.hour))
                }
                return true
            }

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)
                if (dragFromPos != -1) {
                    val targetPos = viewHolder.bindingAdapterPosition
                    val targetItem = adapter.getItem(targetPos)
                    val citaId = dragFromCitaId ?: return
                    val newHour = targetItem.hour
                    val lugar = targetItem.lugar ?: dragFromLugar ?: "Centro"

                    viewLifecycleOwner.lifecycleScope.launch {
                        val day = weekStart.plusDays(targetItem.dayIndex.toLong())
                        val ldt = LocalDateTime.of(day, LocalTime.parse(newHour))
                        val millis = ldtToMillis(ldt)
                        val ok = Repos().reagendarCita(citaId, millis, lugar, motivo = "Drag & drop")
                        if (!ok) {
                            Toast.makeText(requireContext(), "Conflicto: horario/lugar ocupado", Toast.LENGTH_SHORT).show()
                        }
                        loadWeek()
                    }

                    dragFromPos = -1
                    dragFromCitaId = null
                    dragFromLugar = null
                }
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
        }
        ItemTouchHelper(ith).attachToRecyclerView(rv)
    }

    /** Carga de datos con filtro por tipo + visibilidad por rol (Repos) */
    private fun loadWeek() {
        val (start, end) = weekBoundsMillis()
        val p = perfil ?: return
        val hoursCount = hours.size

        val cells = MutableList(hoursCount * 7) { idx ->
            val dayIndex = idx % 7
            val hourIndex = idx / 7
            WeekCell(dayIndex = dayIndex, hour = hours[hourIndex], titulo = null, lugar = null, citaId = null)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            var data = Repos().actividadesEnRango(
                start = start,
                end = end,
                tipo = null,              // Repos ya filtra por rol, no por tipo aquí
                query = null,
                onlyMine = (p.rol == "usuario"),
                rol = p.rol,
                uid = p.uid
            )

            // Filtro local por tipo (chip seleccionado)
            selectedTipo?.let { tipoFiltro ->
                data = data.filter { it.second?.tipo.equals(tipoFiltro, ignoreCase = true) }
            }

            data.forEach { (cita, actividad) ->
                val z = Instant.ofEpochMilli(cita.fechaMillis).atZone(ZoneId.systemDefault())
                val dayIndex = ((z.dayOfWeek.value + 6) % 7)
                val hourStr = z.toLocalTime().withMinute(0).withSecond(0).toString().substring(0, 5)
                val hourIndex = hours.indexOf(hourStr)
                if (dayIndex in 0..6 && hourIndex >= 0) {
                    val pos = posOf(dayIndex, hourIndex)
                    cells[pos] = WeekCell(
                        dayIndex = dayIndex,
                        hour = hourStr,
                        titulo = actividad?.nombre ?: "Actividad",
                        lugar = cita.lugar,
                        citaId = cita.id
                    )
                }
            }

            adapter.setData(cells)
            view?.findViewById<TextView>(R.id.tvEmpty)?.visibility =
                if (data.isEmpty()) View.VISIBLE else View.GONE

            setupDragIfAllowed()
        }
    }
}
