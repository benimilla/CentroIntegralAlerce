package cl.alercelab.centrointegral.auth

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import cl.alercelab.centrointegral.R
import cl.alercelab.centrointegral.data.Repos
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val v = inflater.inflate(R.layout.fragment_login, container, false)

        val etEmail = v.findViewById<EditText>(R.id.etEmail)
        val etPassword = v.findViewById<EditText>(R.id.etPassword)
        val tvError = v.findViewById<TextView>(R.id.tvError)

        // ✅ Si ya hay sesión activa, saltar login
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            viewLifecycleOwner.lifecycleScope.launch {
                val perfil = Repos().currentUserProfile()
                if (perfil != null && perfil.estado == "activo") {
                    navegarSegunRol(perfil.rol)
                }
            }
        }

        v.findViewById<Button>(R.id.btnLogin).setOnClickListener {
            val email = etEmail.text.toString().trim()
            val pass = etPassword.text.toString().trim()
            tvError.text = ""

            // ✅ Validaciones
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                tvError.text = "El correo ingresado no es válido"
                return@setOnClickListener
            }
            if (pass.length < 6) {
                tvError.text = "La contraseña debe tener al menos 6 caracteres"
                return@setOnClickListener
            }

            // ✅ Login Firebase
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val repo = Repos()
                    val perfil = repo.login(email, pass)

                    if (perfil == null) {
                        tvError.text = "No se pudo cargar su perfil (UID sin documento Firestore)"
                        repo.logout()
                        return@launch
                    }

                    when (perfil.estado) {
                        "pendiente" -> {
                            tvError.text = "Su cuenta está pendiente de aprobación"
                            repo.logout()
                        }
                        "rechazado" -> {
                            tvError.text = "Su solicitud fue rechazada"
                            repo.logout()
                        }
                        "activo" -> navegarSegunRol(perfil.rol)
                        else -> {
                            tvError.text = "Estado de cuenta desconocido"
                            repo.logout()
                        }
                    }

                } catch (e: Exception) {
                    tvError.text = "Error al iniciar sesión: ${e.message}"
                }
            }
        }

        // ✅ Ir a registro
        v.findViewById<Button>(R.id.btnGoRegister).setOnClickListener {
            findNavController().navigate(R.id.action_login_to_register)
        }

        // ✅ Ir a recuperar contraseña
        v.findViewById<Button>(R.id.btnForgot).setOnClickListener {
            findNavController().navigate(R.id.action_login_to_forgot)
        }

        return v
    }

    // ✅ Navegación según rol
    private fun navegarSegunRol(rol: String) {
        when (rol) {
            "admin", "gestor", "usuario" -> {
                // Va al calendario y limpia el backstack
                findNavController().navigate(
                    R.id.action_login_to_calendar,
                    null,
                    androidx.navigation.NavOptions.Builder()
                        .setPopUpTo(R.id.loginFragment, true)
                        .build()
                )
            }
            else -> Toast.makeText(requireContext(), "Rol desconocido", Toast.LENGTH_SHORT).show()
        }
    }
}
