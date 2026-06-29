package com.example.mymp

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.work.WorkManager
import com.example.mymp.ui.theme.MyMPTheme
import android.Manifest

class MainActivity : ComponentActivity() {

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (!isGranted) {
                Log.d("MainActivity", "Permesso notifiche negato dall'utente")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        //sezione permesso notifiche
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        val db = (application as MympApplication).database

        val repository = MympRepository(db.mympDao(), db.serverDao(), db.playlistDao())

        val app = application as MympApplication

        setContent {
            MyMPTheme {
                val vm: MympViewModel = viewModel(
                    factory = MympViewModel.Factory(repository, WorkManager.getInstance(applicationContext),
                        app.currentSongState, app.isPlayingState, app.playbackProgressState)
                )
                MyMPNavHost(vm)
            }
        }
    }
}

@Composable
fun MyMPNavHost(viewModel: MympViewModel) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "main") {

        composable("main") {
            mympMainScreen(
                viewModel = viewModel,
                onSettingsClick = { navController.navigate("settings") }
            )
        }

        composable("settings") {
            SettingsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}