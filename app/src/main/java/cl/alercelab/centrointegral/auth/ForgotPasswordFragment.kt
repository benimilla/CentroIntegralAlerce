package cl.alercelab.centrointegral.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import cl.alercelab.centrointegral.R
import cl.alercelab.centrointegral.data.Repos
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ForgotPasswordFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val v = inflater.inflate(R.layout.fragment_forgot, container, false)
        val et = v.findViewById<EditText>(R.id.etEmail)

        v.findViewById<Button>(R.id.btnSend).setOnClickListener {
            val email = et.text.toString().trim()

            if (email.isEmpty()) {
                Toast.makeText(requireContext(), "Ingrese un correo", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(requireContext(), "Formato de correo inválido", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewLifecycleOwner.lifecycleScope.launch {
                val success = try {
                    Repos().resetPassword(email).await() // espera la respuesta real de Firebase
                    true
                } catch (e: Exception) {
                    false
                }

                if (success) {
                    Toast.makeText(
                        requireContext(),
                        "Correo enviado si la cuenta existe",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Error al enviar correo",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        return v
    }
}
