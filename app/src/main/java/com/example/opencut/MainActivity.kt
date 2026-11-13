package com.example.opencut

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.opencut.ui.screens.EditorScreen
import com.example.opencut.ui.screens.EditorViewModel
import com.example.opencut.ui.screens.HomeScreen
import com.example.opencut.ui.screens.HomeViewModel
import com.example.opencut.ui.theme.OpenCutTheme
import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

@Serializable
data class EditorRoute(val projectId: String)

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val systemDark = isSystemInDarkTheme()
            var isDarkTheme by rememberSaveable { mutableStateOf(true) }
            val windowSizeClass = calculateWindowSizeClass(this)
            val isExpandedWidth = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact

            OpenCutTheme(darkTheme = isDarkTheme) {
                val bgArgb = OpenCutTheme.colors.background.toArgb()
                LaunchedEffect(isDarkTheme) {
                    enableEdgeToEdge(
                        statusBarStyle = if (isDarkTheme) {
                            SystemBarStyle.dark(bgArgb)
                        } else {
                            SystemBarStyle.light(bgArgb, bgArgb)
                        },
                        navigationBarStyle = if (isDarkTheme) {
                            SystemBarStyle.dark(bgArgb)
                        } else {
                            SystemBarStyle.light(bgArgb, bgArgb)
                        }
                    )
                }

                val app = LocalContext.current.applicationContext as OpenCutApp
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = HomeRoute
                ) {
                    composable<HomeRoute> {
                        val homeViewModel: HomeViewModel = viewModel(
                            factory = HomeViewModel.Factory(app.repository)
                        )
                        HomeScreen(
                            viewModel = homeViewModel,
                            isDarkTheme = isDarkTheme,
                            isExpandedWidth = isExpandedWidth,
                            onToggleTheme = { isDarkTheme = !isDarkTheme },
                            onOpenProject = { projectId ->
                                navController.navigate(EditorRoute(projectId))
                            }
                        )
                    }

                    composable<EditorRoute> { backStackEntry ->
                        val route = backStackEntry.toRoute<EditorRoute>()
                        val editorViewModel: EditorViewModel = viewModel(
                            key = route.projectId,
                            factory = EditorViewModel.Factory(
                                projectId = route.projectId,
                                repository = app.repository,
                                context = app
                            )
                        )
                        EditorScreen(
                            viewModel = editorViewModel,
                            isDarkTheme = isDarkTheme,
                            onToggleTheme = { isDarkTheme = !isDarkTheme },
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
