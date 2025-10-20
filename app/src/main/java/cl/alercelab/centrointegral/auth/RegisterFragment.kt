package cl.alercelab.centrointegral.auth

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import cl.alercelab.centrointegral.R
import cl.alercelab.centrointegral.data.Repos
import kotlinx.coroutines.launch

class RegisterFragment : Fragment() {

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

        val etNombre = v.findViewById<EditText>(R.id.etNombre)
        val etEmail = v.findViewById<EditText>(R.id.etEmail)
        val etPassword = v.findViewById<EditText>(R.id.etPassword)
        val spRole = v.findViewById<Spinner>(R.id.spRole)
        val tvError = v.findViewById<TextView>(R.id.tvError)

        spRole.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            roles.map { it.first }
        )

        v.findViewById<Button>(R.id.btnRegister).setOnClickListener {
            val nombre = etNombre.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val pass = etPassword.text.toString().trim()
            val rol = roles[spRole.selectedItemPosition].second

            tvError.text = ""

            when {
                nombre.isEmpty() ->
                    tvError.text = "Debe ingresar un nombre"
                nombre.length < 3 ->
                    tvError.text = "El nombre es demasiado corto"
                email.isEmpty() ->
                    tvError.text = "Debe ingresar un correo"
                !Patterns.EMAIL_ADDRESS.matcher(email).matches() ->
                    tvError.text = "El formato del correo no es válido"
                pass.isEmpty() ->
                    tvError.text = "Debe ingresar una contraseña"
                pass.length < 6 ->
                    tvError.text = "La contraseña debe tener al menos 6 caracteres"
                else -> {
                    viewLifecycleOwner.lifecycleScope.launch {
                        try {
                            Repos().register(nombre, email, pass, rol)
                            Toast.makeText(
                                requireContext(),
                                "Registro enviado. Pendiente de autorización.",
                                Toast.LENGTH_LONG
                            ).show()
                            requireActivity().onBackPressedDispatcher.onBackPressed()
                        } catch (e: Exception) {
                            tvError.text = when {
                                e.message?.contains("already in use", ignoreCase = true) == true ->
                                    "Este correo ya está registrado"
                                else ->
                                    e.message ?: "Ocurrió un error al registrar"
                            }
                        }
                    }
                }
            }
        }

        return v
    }
}
