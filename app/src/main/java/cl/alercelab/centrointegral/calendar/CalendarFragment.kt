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

    //  Declaración de componentes de la interfaz
    private lateinit var calendarView: CalendarView
    private lateinit var rvDay: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var btnCrearCita: FloatingActionButton
    private lateinit var spTipo: Spinner
    private lateinit var spRango: Spinner
    private lateinit var etBuscar: EditText
    private lateinit var progressBar: ProgressBar

    //  Variables de apoyo
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

        //  Vinculación de vistas
        calendarView = view.findViewById(R.id.calendarView)
        rvDay = view.findViewById(R.id.rvDay)
        tvEmpty = view.findViewById(R.id.tvEmpty)
        btnCrearCita = view.findViewById(R.id.btnCrearCita)
        spTipo = view.findViewById(R.id.spTipo)
        spRango = view.findViewById(R.id.spRango)
        etBuscar = view.findViewById(R.id.etBuscar)
        progressBar = view.findViewById(R.id.progressBar)

        rvDay.layoutManager = LinearLayoutManager(requireContext())

        verificarRolUsuario() //  Determina si el usuario puede crear citas
        cargarCitas() //  Descarga citas desde Firestore

        //  Observa si se creó o modificó una cita para actualizar el listado
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            findNavController().currentBackStackEntry
                ?.savedStateHandle
                ?.getLiveData<Boolean>("citaGuardada")
                ?.observe(viewLifecycleOwner) { guardada ->
                    if (guardada == true) {
                        cargarCitas()
                        //  Se marca como false para evitar recargas continuas
                        findNavController().currentBackStackEntry
                            ?.savedStateHandle
                            ?.set("citaGuardada", false)
                    }
                }
        }

        //  Cuando el usuario cambia de día en el calendario
        calendarView.setOnDateChangeListener { _, year, month, day ->
            val cal = Calendar.getInstance().apply {
                set(year, month, day, 0, 0, 0)
            }
            mostrarCitasDelDia(cal.timeInMillis)
        }

        //  Botón flotante para crear una nueva cita
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

    /**  Verifica el rol del usuario logueado (admin/gestor/usuario) */
    private fun verificarRolUsuario() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        db.collection("usuarios").document(uid).get()
            .addOnSuccessListener { doc ->
                val rol = doc.getString("rol")?.lowercase(Locale.getDefault()) ?: ""
                //  Solo admin o gestor pueden crear citas
                btnCrearCita.visibility =
                    if (rol == "admin" || rol == "gestor") View.VISIBLE else View.GONE
            }
            .addOnFailureListener {
                Toast.makeText(
                    requireContext(),
                    "Error verificando rol del usuario",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    /**  Descarga todas las citas desde Firestore */
    private fun cargarCitas() {
        lifecycleScope.launch {
            try {
                progressBar.visibility = View.VISIBLE
                val result = db.collectionGroup("citas").get().await()
                citas.clear()
                citas.addAll(result.mapNotNull {
                    val cita = it.toObject(Cita::class.java)
                    cita?.id = it.id //  Se guarda el ID del documento
                    cita
                })
                //  Muestra citas del día actual por defecto
                mostrarCitasDelDia(Calendar.getInstance().timeInMillis)
            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "Error al cargar citas: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    /**  Filtra y muestra las citas correspondientes a un día específico */
    private fun mostrarCitasDelDia(diaMillis: Long) {
        val inicioDia = Calendar.getInstance().apply {
            timeInMillis = diaMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val finDia = inicioDia + 24 * 60 * 60 * 1000 //  +24h

        val citasFiltradas = citas.filter {
            it.fechaInicioMillis in inicioDia..finDia
        }.sortedBy { it.fechaInicioMillis }

        //  Si no hay citas, muestra mensaje vacío
        if (citasFiltradas.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
            rvDay.visibility = View.GONE
        } else {
            tvEmpty.visibility = View.GONE
            rvDay.visibility = View.VISIBLE
            //  Carga el adaptador con las citas del día
            rvDay.adapter = CitaAdapter(citasFiltradas, formato) { citaSeleccionada ->
                val bundle = Bundle().apply {
                    putString("citaId", citaSeleccionada.id)
                }
                //  Navega al detalle de la cita seleccionada
                findNavController().navigate(R.id.citaDetalleFragment, bundle)
            }
        }
    }
}

/** =======================================================
 *   Adaptador del RecyclerView para mostrar citas diarias
 *  ======================================================= */
class CitaAdapter(
    private val citas: List<Cita>,
    private val formato: SimpleDateFormat,
    private val onCitaClick: (Cita) -> Unit
) : RecyclerView.Adapter<CitaViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CitaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cita, parent, false)
        return CitaViewHolder(view)
    }

    override fun onBindViewHolder(holder: CitaViewHolder, position: Int) {
        val cita = citas[position]
        //  Se formatean las fechas para mostrar inicio y fin
        val inicio =
            cita.fechaInicioMillis.takeIf { it > 0 }?.let { formato.format(Date(it)) } ?: "?"
        val fin =
            cita.fechaFinMillis.takeIf { it > 0 }?.let { formato.format(Date(it)) } ?: "?"
        val lugar = cita.lugar ?: "Sin lugar"

        holder.bind(lugar, "$inicio - $fin")

        // 🔹 Acción al hacer clic en una cita
        holder.itemView.setOnClickListener {
            onCitaClick(cita)
        }
    }

    override fun getItemCount(): Int = citas.size
}

/**  ViewHolder que gestiona las vistas individuales de cada cita */
class CitaViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    private val titulo = view.findViewById<TextView>(R.id.tvTituloCita)
    private val detalle = view.findViewById<TextView>(R.id.tvDetalleCita)

    fun bind(tituloTxt: String, detalleTxt: String) {
        titulo.text = tituloTxt
        detalle.text = detalleTxt
    }
}

/*  En resumen:
   Este fragmento muestra un calendario con citas descargadas desde Firestore.
   Permite seleccionar un día, visualizar las citas agendadas y navegar al detalle.
   Los usuarios con rol 'admin' o 'gestor' pueden crear nuevas citas. */
