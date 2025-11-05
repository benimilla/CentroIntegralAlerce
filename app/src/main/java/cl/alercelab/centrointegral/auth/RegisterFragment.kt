package cl.alercelab.centrointegral.auth

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import cl.alercelab.centrointegral.R
import cl.alercelab.centrointegral.data.Repos
import kotlinx.coroutines.launch

class RegisterFragment : Fragment() {

    //  Lista de roles disponibles para el registro con su identificador interno
    private val roles = listOf(
        "Administrador" to "admin",
        "Gestor de actividades" to "gestor",
        "Usuario/Consultor" to "usuario"
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val v = inflater.inflate(R.layout.fragment_register, container, false)

        //  Referencias a los elementos de la vista
        val etNombre = v.findViewById<EditText>(R.id.etNombre)
        val etEmail = v.findViewById<EditText>(R.id.etEmail)
        val etPassword = v.findViewById<EditText>(R.id.etPassword)
        val spRole = v.findViewById<Spinner>(R.id.spRole)
        val tvError = v.findViewById<TextView>(R.id.tvError)
        val btnRegister = v.findViewById<Button>(R.id.btnRegister)

        //  Configurar el Spinner con los nombres de los roles
        spRole.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            roles.map { it.first } // solo los nombres visibles (Administrador, Gestor, etc.)
        )

        //  Acción al presionar el botón de registro
        btnRegister.setOnClickListener {
            val nombre = etNombre.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val pass = etPassword.text.toString().trim()
            val rol = roles[spRole.selectedItemPosition].second // obtiene el valor interno del rol

            tvError.text = "" // limpia errores previos

            //  Validaciones básicas de los campos
            when {
                nombre.isEmpty() -> tvError.text = "Debe ingresar un nombre"
                nombre.length < 3 -> tvError.text = "El nombre es demasiado corto"
                email.isEmpty() -> tvError.text = "Debe ingresar un correo"
                !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> tvError.text = "El formato del correo no es válido"
                pass.isEmpty() -> tvError.text = "Debe ingresar una contraseña"
                pass.length < 6 -> tvError.text = "La contraseña debe tener al menos 6 caracteres"
                else -> registrarUsuario(nombre, email, pass, rol, tvError)
            }
        }

        //  Maneja el botón físico de “atrás” para volver correctamente al fragment anterior
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            parentFragmentManager.popBackStack()
        }

        return v
    }

    //  Función que realiza el registro del usuario en Firebase a través del repositorio
    private fun registrarUsuario(
        nombre: String,
        email: String,
        pass: String,
        rol: String,
        tvError: TextView
    ) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                //  Llamada suspendida al repositorio que maneja Firebase
                Repos().register(nombre, email, pass, rol)

                //  Registro exitoso → se notifica al usuario
                Toast.makeText(
                    requireContext(),
                    "Registro enviado. Pendiente de autorización.",
                    Toast.LENGTH_LONG
                ).show()

                //  Regresa al fragmento anterior (Login)
                parentFragmentManager.popBackStack()

            } catch (e: Exception) {
                // ️ Manejo de errores específicos para mejorar la experiencia del usuario
                tvError.text = when {
                    e.message?.contains("already in use", ignoreCase = true) == true ->
                        "Este correo ya está registrado"
                    e.message?.contains("invalid", ignoreCase = true) == true ->
                        "El correo o la contraseña no son válidos"
                    else -> e.message ?: "Ocurrió un error al registrar"
                }
            }
        }
    }
}