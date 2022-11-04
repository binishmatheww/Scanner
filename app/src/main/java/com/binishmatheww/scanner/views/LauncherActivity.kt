package com.binishmatheww.scanner.views

import android.os.Bundle
import android.os.Handler
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.binishmatheww.scanner.R
import com.binishmatheww.scanner.common.constants.Routes
import com.binishmatheww.scanner.views.fragments.HomeFragment
import com.binishmatheww.scanner.views.fragments.PdfEditorFragment
import com.binishmatheww.scanner.views.fragments.SplashScreenFragment
import com.binishmatheww.scanner.views.screens.CameraScreen
import com.binishmatheww.scanner.views.screens.HomeScreen
import com.binishmatheww.scanner.views.screens.WelcomeScreen
import com.binishmatheww.scanner.views.utils.clearTemporaryLocation
import com.binishmatheww.scanner.views.utils.storageLocation
import com.binishmatheww.scanner.views.utils.temporaryLocation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class LauncherActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {

            temporaryLocation(this@LauncherActivity)

            storageLocation(this@LauncherActivity)

            delay(400)

            window.setBackgroundDrawableResource(android.R.color.transparent)

        }

        setContent {

            val navController = rememberNavController()

            NavHost(
                navController = navController,
                startDestination = Routes.welcomeScreen,
            ) {

                composable(
                    route = Routes.welcomeScreen
                ) {

                    WelcomeScreen(
                        onComplete = {
                            navController.popBackStack()
                            navController.navigate(Routes.homeScreen)
                        }
                    )

                }

                composable(
                    route = Routes.homeScreen
                ) {

                    HomeScreen(
                        onCameraClick = {
                            navController.navigate(Routes.cameraScreen)
                        }
                    )

                }

                composable(
                    route = Routes.cameraScreen
                ) {

                    CameraScreen()

                }

            }

        }

    }

    override fun onDestroy() {
        clearTemporaryLocation(this)
        super.onDestroy()
    }


}