package cl.alercelab.centrointegral.calendar

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cl.alercelab.centrointegral.R
import cl.alercelab.centrointegral.data.Repos
import cl.alercelab.centrointegral.domain.Cita
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class CalendarFragment : Fragment() {

    private lateinit var calendarView: CalendarView
    private lateinit var rvDay: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var btnCrearCita: FloatingActionButton
    private lateinit var spTipo: Spinner
    private lateinit var spRango: Spinner
    private lateinit var etBuscar: EditText
    private lateinit var progressBar: ProgressBar

    private val db = FirebaseFirestore.getInstance()
    private val repos = Repos()
    private val citas = mutableListOf<Cita>()
    private val formato = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_calendar, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Referencias UI
        calendarView = view.findViewById(R.id.calendarView)
        rvDay = view.findViewById(R.id.rvDay)
        tvEmpty = view.findViewById(R.id.tvEmpty)
        btnCrearCita = view.findViewById(R.id.btnCrearCita)
        spTipo = view.findViewById(R.id.spTipo)
        spRango = view.findViewById(R.id.spRango)
        etBuscar = view.findViewById(R.id.etBuscar)
        progressBar = view.findViewById(R.id.progressBar)

        rvDay.layoutManager = LinearLayoutManager(requireContext())

        verificarRolUsuario()
        cargarCitas()

        // Cambio de día en el calendario
        calendarView.setOnDateChangeListener { _, year, month, day ->
            val cal = Calendar.getInstance().apply {
                set(year, month, day, 0, 0, 0)
            }
            mostrarCitasDelDia(cal.timeInMillis)
        }

        // Botón para crear nueva cita
        btnCrearCita.setOnClickListener {
            try {
                findNavController().navigate(R.id.action_calendarFragment_to_citaFormFragment)
            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "Error al abrir formulario: ${e.localizedMessage}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    /** 🔹 Verifica el rol del usuario para mostrar o no el botón de crear cita */
    private fun verificarRolUsuario() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        db.collection("usuarios").document(uid).get()
            .addOnSuccessListener { doc ->
                val rol = doc.getString("rol")?.lowercase(Locale.getDefault()) ?: ""
                btnCrearCita.visibility =
                    if (rol == "admin" || rol == "gestor") View.VISIBLE else View.GONE
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error verificando rol del usuario", Toast.LENGTH_SHORT).show()
            }
    }

    /** 🔹 Carga las citas desde Firestore */
    private fun cargarCitas() {
        lifecycleScope.launch {
            try {
                progressBar.visibility = View.VISIBLE
                val result = db.collectionGroup("citas").get().await()
                citas.clear()
                citas.addAll(result.mapNotNull {
                    val cita = it.toObject(Cita::class.java)
                    cita?.id = it.id // ✅ necesario para navegar al detalle
                    cita
                })
                mostrarCitasDelDia(Calendar.getInstance().timeInMillis)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error al cargar citas: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    /** 🔹 Filtra y muestra las citas del día seleccionado */
    private fun mostrarCitasDelDia(diaMillis: Long) {
        val inicioDia = Calendar.getInstance().apply {
            timeInMillis = diaMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val finDia = inicioDia + 24 * 60 * 60 * 1000

        val citasFiltradas = citas.filter {
            it.fechaInicioMillis in inicioDia..finDia
        }.sortedBy { it.fechaInicioMillis }

        if (citasFiltradas.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
            rvDay.visibility = View.GONE
        } else {
            tvEmpty.visibility = View.GONE
            rvDay.visibility = View.VISIBLE
            rvDay.adapter = CitaAdapter(citasFiltradas, formato) { citaSeleccionada ->
                // ✅ Navegación al detalle con Bundle
                val bundle = Bundle().apply {
                    putString("citaId", citaSeleccionada.id)
                }
                findNavController().navigate(R.id.citaDetalleFragment, bundle)
            }
        }
    }
}

/** =======================================================
 *  🔹 Adaptador personalizado para mostrar citas en la lista
 *  ======================================================= */
class CitaAdapter(
    private val citas: List<Cita>,
    private val formato: SimpleDateFormat,
    private val onCitaClick: (Cita) -> Unit // ✅ callback de clic
) : RecyclerView.Adapter<CitaViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CitaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cita, parent, false)
        return CitaViewHolder(view)
    }

    override fun onBindViewHolder(holder: CitaViewHolder, position: Int) {
        val cita = citas[position]
        val inicio = cita.fechaInicioMillis.takeIf { it > 0 }?.let { formato.format(Date(it)) } ?: "?"
        val fin = cita.fechaFinMillis.takeIf { it > 0 }?.let { formato.format(Date(it)) } ?: "?"
        val lugar = cita.lugar ?: "Sin lugar"

        holder.bind(lugar, "$inicio - $fin")

        // ✅ Acción de clic
        holder.itemView.setOnClickListener {
            onCitaClick(cita)
        }
    }

    override fun getItemCount(): Int = citas.size
}

class CitaViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    private val titulo = view.findViewById<TextView>(R.id.tvTituloCita)
    private val detalle = view.findViewById<TextView>(R.id.tvDetalleCita)

    fun bind(tituloTxt: String, detalleTxt: String) {
        titulo.text = tituloTxt
        detalle.text = detalleTxt
    }
}
