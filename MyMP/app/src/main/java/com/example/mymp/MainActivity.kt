package com.example.mymp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.work.WorkManager
import com.example.mymp.ui.theme.MyMPTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

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