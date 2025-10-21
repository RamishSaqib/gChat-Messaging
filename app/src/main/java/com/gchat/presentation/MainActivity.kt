package com.gchat.presentation

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
    
    private var latestIntent by mutableStateOf<Intent?>(null)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        latestIntent = intent
        
        setContent {
            GChatTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val authViewModel: AuthViewModel = hiltViewModel()
                    val isAuthenticated by authViewModel.isAuthenticated.collectAsState()
                    
                    // Handle notification navigation whenever intent or auth state changes
                    LaunchedEffect(isAuthenticated, latestIntent) {
                        if (isAuthenticated) {
                            val conversationId = latestIntent?.getStringExtra("conversationId")
                            val openChat = latestIntent?.getBooleanExtra("openChat", false) ?: false
                            
                            if (openChat && conversationId != null) {
                                // Navigate to specific conversation from notification
                                navController.navigate(Screen.ConversationList.route) {
                                    popUpTo(Screen.Login.route) { inclusive = true }
                                }
                                navController.navigate(Screen.Chat.createRoute(conversationId))
                                
                                // Clear the intent flags so we don't navigate again
                                latestIntent = Intent()
                            } else if (latestIntent != null) {
                                // Regular navigation to conversation list
                                navController.navigate(Screen.ConversationList.route) {
                                    popUpTo(Screen.Login.route) { inclusive = true }
                                }
                            }
                        }
                    }
                    
                    NavGraph(navController = navController, authViewModel = authViewModel)
                }
            }
        }
    }
    
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        latestIntent = intent
    }
}

