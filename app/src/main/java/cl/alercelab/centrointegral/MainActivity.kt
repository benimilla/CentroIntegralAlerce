package cl.alercelab.centrointegral

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import cl.alercelab.centrointegral.data.Repos
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val repos = Repos()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)

        bottomNav.setupWithNavController(navController)

        // 🔹 Ocultar barra inferior en pantallas de login, registro y recuperación
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.loginFragment,
                R.id.registerFragment,
                R.id.forgotFragment -> bottomNav.visibility = View.GONE
                else -> bottomNav.visibility = View.VISIBLE
            }
        }

        lifecycleScope.launch {
            val perfil = repos.currentUserProfile()

            if (perfil == null) {
                navController.navigate(R.id.loginFragment)
                bottomNav.visibility = View.GONE
                return@launch
            }

            // ✅ SIEMPRE iniciar en el Calendario
            navController.navigate(R.id.calendarDayFragment)
            bottomNav.selectedItemId = R.id.calendarDayFragment

            // 🔒 Control de acceso por rol
            when (perfil.rol) {
                "usuario" -> {
                    // Eliminar accesos restringidos
                    bottomNav.menu.removeItem(R.id.activitiesFragment)
                    bottomNav.menu.removeItem(R.id.adminMainFragment)
                }

                "gestor" -> {
                    // Solo eliminar administración
                    bottomNav.menu.removeItem(R.id.adminMainFragment)
                }

                "admin" -> {
                    // Tiene acceso total — no se elimina nada
                }
            }
        }

        // 🔹 Control manual de navegación inferior con verificación
        bottomNav.setOnItemSelectedListener { item ->
            lifecycleScope.launch {
                val perfil = repos.currentUserProfile()

                when (item.itemId) {
                    R.id.calendarDayFragment -> {
                        navController.navigate(R.id.calendarDayFragment)
                    }

                    R.id.activitiesFragment -> {
                        if (perfil?.rol == "admin" || perfil?.rol == "gestor") {
                            navController.navigate(R.id.activitiesFragment)
                        } else {
                            navController.navigate(R.id.calendarDayFragment)
                        }
                    }

                    R.id.adminMainFragment -> {
                        if (perfil?.rol == "admin") {
                            navController.navigate(R.id.adminMainFragment)
                        } else {
                            navController.navigate(R.id.calendarDayFragment)
                        }
                    }

                    R.id.settingsFragment -> {
                        navController.navigate(R.id.settingsFragment)
                    }
                }
            }
            true
        }
    }

    override fun onBackPressed() {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        when (navController.currentDestination?.id) {
            R.id.calendarDayFragment,
            R.id.activitiesFragment,
            R.id.adminMainFragment,
            R.id.settingsFragment -> finish()
            else -> super.onBackPressed()
        }
    }
}
