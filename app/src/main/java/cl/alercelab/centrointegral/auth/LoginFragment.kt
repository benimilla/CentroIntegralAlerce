package cl.alercelab.centrointegral.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import cl.alercelab.centrointegral.R
import cl.alercelab.centrointegral.databinding.FragmentLoginBinding
import cl.alercelab.centrointegral.data.Repos
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val repos = Repos()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val ok = repos.login(email, password)
                if (!ok) {
                    Toast.makeText(requireContext(), "Credenciales incorrectas", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val auth = FirebaseAuth.getInstance()
                val user = auth.currentUser
                if (user == null) {
                    Toast.makeText(requireContext(), "Error al iniciar sesión.", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                // 🔄 Recargar usuario desde Firebase para obtener estado más reciente
                user.reload().addOnCompleteListener { reloadTask ->
                    if (reloadTask.isSuccessful) {
                        lifecycleScope.launch {
                            val db = FirebaseFirestore.getInstance()

                            // 🔹 Sincronizar verificación si corresponde
                            if (user.isEmailVerified) {
                                try {
                                    db.collection("usuarios").document(user.uid)
                                        .update("emailVerificado", true)
                                        .await() //  Esperar confirmación antes de seguir
                                } catch (e: Exception) {
                                    Toast.makeText(
                                        requireContext(),
                                        "Error al actualizar verificación: ${e.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }

                            // 🔹 Cargar perfil actualizado desde Firestore
                            val profile = repos.currentUserProfile()
                            if (profile == null) {
                                Toast.makeText(requireContext(), "Error al cargar usuario", Toast.LENGTH_SHORT).show()
                                FirebaseAuth.getInstance().signOut()
                                return@launch
                            }

                            // 🔹 Validar aprobación y estado
                            if (!profile.aprobado || profile.estado.lowercase() != "activo") {
                                FirebaseAuth.getInstance().signOut()
                                binding.tvPending.visibility = View.VISIBLE
                                binding.tvPending.text = if (!profile.aprobado)
                                    "Tu cuenta está pendiente de aprobación por el administrador."
                                else
                                    "Tu cuenta fue desactivada por el administrador."
                                Toast.makeText(
                                    requireContext(),
                                    "Tu cuenta aún no ha sido aprobada o fue desactivada.",
                                    Toast.LENGTH_LONG
                                ).show()
                                return@launch
                            } else {
                                binding.tvPending.visibility = View.GONE
                            }

                            // 🔹 Usuario validado → navegar al calendario principal
                            findNavController().navigate(R.id.action_login_to_calendar)
                        }
                    } else {
                        Toast.makeText(requireContext(), "Error al actualizar usuario.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        // Recuperar contraseña
        binding.tvForgotPassword.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_forgot)
        }

        // Registrar nuevo usuario
        binding.tvRegister.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_register)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
