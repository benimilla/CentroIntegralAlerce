package cl.alercelab.centrointegral.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import cl.alercelab.centrointegral.R

class AdminMainFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val v = inflater.inflate(R.layout.fragment_admin_main, container, false)

        // =========================================================
        //  BOTONES PRINCIPALES
        // =========================================================

        // Botón para acceder a la gestión general de usuarios
        val btnGestion = v.findViewById<Button>(R.id.btnGestionUsuarios)

        // Botón para aprobar o rechazar solicitudes de nuevos usuarios
        val btnPermitir = v.findViewById<Button>(R.id.btnPermitirUsuarios)

        // Botón para visualizar el registro de auditoría del sistema
        val btnAuditoria = v.findViewById<Button>(R.id.btnAuditoria)

        // Navega a la vista de gestión de usuarios
        btnGestion.setOnClickListener {
            Toast.makeText(requireContext(), "Abriendo gestión de usuarios...", Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.action_adminMain_to_adminUsers)
        }

        // Navega a la vista donde se aprueban usuarios pendientes
        btnPermitir.setOnClickListener {
            Toast.makeText(requireContext(), "Abriendo usuarios pendientes...", Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.action_adminMain_to_adminPending)
        }

        // Navega a la vista del registro de auditoría
        btnAuditoria.setOnClickListener {
            Toast.makeText(requireContext(), "Abriendo auditoría del sistema...", Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.action_adminMain_to_auditoria)
        }

        // =========================================================
        //  MANTENEDORES (acceso a módulos administrativos específicos)
        // =========================================================

        val btnTipoActividad = v.findViewById<Button>(R.id.btnTipoActividad)
        val btnLugares = v.findViewById<Button>(R.id.btnLugares)
        val btnOferentes = v.findViewById<Button>(R.id.btnOferentes)
        val btnSocios = v.findViewById<Button>(R.id.btnSocios)
        val btnProyectos = v.findViewById<Button>(R.id.btnProyectos)

        // Navega al mantenedor de tipos de actividad
        btnTipoActividad.setOnClickListener {
            Toast.makeText(requireContext(), "Abriendo Mantenedor de Tipos de Actividad...", Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.action_adminMain_to_mantenedorTipoActividad)
        }

        // Navega al mantenedor de lugares
        btnLugares.setOnClickListener {
            Toast.makeText(requireContext(), "Abriendo Mantenedor de Lugares...", Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.action_adminMain_to_mantenedorLugar)
        }

        // Navega al mantenedor de oferentes
        btnOferentes.setOnClickListener {
            Toast.makeText(requireContext(), "Abriendo Mantenedor de Oferentes...", Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.action_adminMain_to_mantenedorOferente)
        }

        // Navega al mantenedor de socios comunitarios
        btnSocios.setOnClickListener {
            Toast.makeText(requireContext(), "Abriendo Mantenedor de Socios Comunitarios...", Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.action_adminMain_to_mantenedorSocio)
        }

        // Navega al mantenedor de proyectos
        btnProyectos.setOnClickListener {
            Toast.makeText(requireContext(), "Abriendo Mantenedor de Proyectos...", Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.action_adminMain_to_mantenedorProyecto)
        }

        return v
    }
}

//  Este fragmento representa el panel principal del módulo administrativo.
// Desde aquí, el administrador puede acceder a la gestión de usuarios, auditoría
// y a los distintos mantenedores del sistema (actividades, lugares, oferentes, etc.)