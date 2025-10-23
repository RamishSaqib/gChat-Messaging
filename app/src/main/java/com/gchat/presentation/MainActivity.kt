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
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.gchat.presentation.auth.AuthViewModel
import com.gchat.presentation.navigation.NavGraph
import com.gchat.presentation.navigation.Screen
import com.gchat.presentation.theme.GChatTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Main Activity for gChat
 * 
 * Entry point for the application.
 * Uses Jetpack Compose for UI with Navigation.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    private val _navigationEvents = MutableStateFlow<NavigationEvent?>(null)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        android.util.Log.d("MainActivity", "onCreate called")
        android.util.Log.d("MainActivity", "Intent: ${intent?.toString()}")
        android.util.Log.d("MainActivity", "Intent extras: ${intent?.extras?.keySet()?.joinToString()}")
        android.util.Log.d("MainActivity", "conversationId: ${intent?.getStringExtra("conversationId")}")
        android.util.Log.d("MainActivity", "openChat: ${intent?.getBooleanExtra("openChat", false)}")
        android.util.Log.d("MainActivity", "Intent action: ${intent?.action}")
        
        // Check intent immediately
        handleIntent(intent)
        
        setContent {
            GChatTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val authViewModel: AuthViewModel = hiltViewModel()
                    val isAuthenticated by authViewModel.isAuthenticated.collectAsState()
                    val navigationEvent by _navigationEvents.asStateFlow().collectAsState()
                    
                    // Handle navigation events
                    LaunchedEffect(isAuthenticated, navigationEvent) {
                        android.util.Log.d("MainActivity", "LaunchedEffect triggered - auth: $isAuthenticated, event: $navigationEvent")
                        
                        if (isAuthenticated) {
                            when (val event = navigationEvent) {
                                is NavigationEvent.OpenChat -> {
                                    android.util.Log.d("MainActivity", "Navigating to chat: ${event.conversationId}")
                                    // Navigate directly to chat
                                    navController.navigate(Screen.Chat.createRoute(event.conversationId)) {
                                        popUpTo(Screen.Login.route) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                    _navigationEvents.value = null // Clear event
                                }
                                null -> {
                                    // Regular navigation to conversation list
                                    android.util.Log.d("MainActivity", "No pending event - navigating to conversation list")
                                    navController.navigate(Screen.ConversationList.createRoute()) {
                                        popUpTo(Screen.Login.route) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                }
                            }
                        } else {
                            android.util.Log.d("MainActivity", "Not authenticated yet, waiting...")
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
        handleIntent(intent)
    }
    
    private fun handleIntent(intent: Intent?) {
        android.util.Log.d("MainActivity", "handleIntent called")
        val conversationId = intent?.getStringExtra("conversationId")
        val openChat = intent?.getBooleanExtra("openChat", false) ?: false
        
        android.util.Log.d("MainActivity", "Parsed - conversationId: $conversationId, openChat: $openChat")
        
        if (openChat && conversationId != null) {
            android.util.Log.d("MainActivity", "Setting navigation event for: $conversationId")
            _navigationEvents.value = NavigationEvent.OpenChat(conversationId)
        } else {
            android.util.Log.d("MainActivity", "No navigation event - missing data or openChat=false")
        }
    }
}

sealed class NavigationEvent {
    data class OpenChat(val conversationId: String) : NavigationEvent()
}
