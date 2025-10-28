package cl.alercelab.centrointegral

import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import cl.alercelab.centrointegral.data.Repos
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val repos = Repos()
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Obtener controlador de navegación
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)

        // Configurar navegación inferior
        bottomNav.setupWithNavController(navController)

        // 🔹 Ocultar barra inferior en login, registro y recuperación
        navController.addOnDestinationChangedListener { _, destination, _ ->
            bottomNav.visibility = when (destination.id) {
                R.id.loginFragment, R.id.registerFragment, R.id.forgotFragment -> View.GONE
                else -> View.VISIBLE
            }
        }

        // 🚀 Comenzar flujo de sesión
        lifecycleScope.launch {
            val perfil = repos.currentUserProfile()

            if (perfil == null) {
                // No hay sesión → mostrar login
                navController.navigate(R.id.loginFragment)
                bottomNav.visibility = View.GONE
                return@launch
            }

            // Si ya hay sesión → ir directamente al calendario
            val options = NavOptions.Builder()
                .setPopUpTo(R.id.nav_graph, true)
                .build()
            navController.navigate(R.id.calendarFragment, null, options)
            bottomNav.selectedItemId = R.id.calendarFragment
        }

        // 🔹 Control manual del menú inferior con validación de sesión y rol
        bottomNav.setOnItemSelectedListener { item ->
            lifecycleScope.launch {
                val perfil = repos.currentUserProfile()

                if (perfil == null) {
                    // Si no hay sesión, volver al login
                    navController.navigate(R.id.loginFragment)
                    bottomNav.visibility = View.GONE
                    return@launch
                }

                val navOptions = NavOptions.Builder()
                    .setLaunchSingleTop(true)
                    .setRestoreState(true)
                    .setPopUpTo(R.id.calendarFragment, false)
                    .build()

                // Evitar navegar al mismo destino actual
                if (item.itemId == navController.currentDestination?.id) return@launch

                when (item.itemId) {
                    R.id.calendarFragment -> {
                        navController.navigate(R.id.calendarFragment, null, navOptions)
                    }

                    R.id.activitiesFragment -> {
                        if (perfil.rol == "admin" || perfil.rol == "gestor") {
                            navController.navigate(R.id.activitiesFragment, null, navOptions)
                        } else {
                            navController.navigate(R.id.calendarFragment, null, navOptions)
                            bottomNav.selectedItemId = R.id.calendarFragment
                        }
                    }

                    R.id.adminMainFragment -> {
                        if (perfil.rol == "admin") {
                            navController.navigate(R.id.adminMainFragment, null, navOptions)
                        } else {
                            navController.navigate(R.id.calendarFragment, null, navOptions)
                            bottomNav.selectedItemId = R.id.calendarFragment
                        }
                    }

                    R.id.settingsFragment -> {
                        navController.navigate(R.id.settingsFragment, null, navOptions)
                    }
                }
            }
            true
        }

        // 🔙 Botón atrás coherente con la navegación
        onBackPressedDispatcher.addCallback(this) {
            when (navController.currentDestination?.id) {
                R.id.calendarFragment,
                R.id.activitiesFragment,
                R.id.adminMainFragment,
                R.id.settingsFragment -> finish()
                else -> navController.popBackStack()
            }
        }
    }
}
