package com.gchat.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.gchat.presentation.auth.AuthViewModel
import com.gchat.presentation.navigation.NavGraph
import com.gchat.presentation.navigation.Screen
import com.gchat.presentation.theme.GChatTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main Activity for gChat
 * 
 * Entry point for the application.
 * Uses Jetpack Compose for UI with Navigation.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            GChatTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val authViewModel: AuthViewModel = hiltViewModel()
                    val isAuthenticated by authViewModel.isAuthenticated.collectAsState()
                    
                    // Auto-navigate to conversation list if authenticated on app start
                    LaunchedEffect(Unit) {
                        if (isAuthenticated) {
                            navController.navigate(Screen.ConversationList.route) {
                                popUpTo(Screen.Login.route) { inclusive = true }
                            }
                        }
                    }
                    
                    NavGraph(navController = navController, authViewModel = authViewModel)
                }
            }
        }
    }
}

