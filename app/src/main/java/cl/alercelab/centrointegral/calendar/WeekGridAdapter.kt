package cl.alercelab.centrointegral.calendar

import android.content.Context
import android.view.*
import android.widget.*
import java.util.*

class WeekGridAdapter(
    private val context: Context,
    private val onRangeSelected: (startMillis: Long, endMillis: Long) -> Unit
) : BaseAdapter() {

    private val days = mutableListOf<Calendar>()

    init {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
        repeat(7) {
            days.add(cal.clone() as Calendar)
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }
    }

    override fun getCount(): Int = days.size
    override fun getItem(position: Int): Any = days[position]
    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)
        val day = days[position]
        val tv = view.findViewById<TextView>(android.R.id.text1)
        tv.text = day.get(Calendar.DAY_OF_MONTH).toString()
        tv.gravity = Gravity.CENTER
        tv.setPadding(0, 16, 0, 16)

        view.setOnClickListener {
            val start = day.timeInMillis
            val end = day.apply { set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59) }.timeInMillis
            onRangeSelected(start, end)
        }

        return view
    }
}
