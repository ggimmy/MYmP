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
import androidx.room.Room
import androidx.work.WorkManager
import com.example.mymp.ui.theme.MyMPTheme
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val db = (application as MympApplication).database

        /*
        val json = Json { ignoreUnknownKeys = true }
        val api = Retrofit.Builder()
            .baseUrl("http://placeholder/")
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(mympApiService::class.java)*/

        val repository = mympRepository(db.mympDao(), db.ServerDao(), db.playlistDao())

        setContent {
            MyMPTheme {
                val vm: mympViewModel = viewModel(
                    factory = mympViewModel.Factory(repository, WorkManager.getInstance(applicationContext))
                )
                MyMPNavHost(vm)
            }
        }
    }
}

@Composable
fun MyMPNavHost(viewModel: mympViewModel) {
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