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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.gchat.presentation.auth.AuthViewModel
import com.gchat.presentation.navigation.NavGraph
import com.gchat.presentation.navigation.Screen
import com.gchat.presentation.theme.GChatTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
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
        
        android.util.Log.d("MainActivity", "═══════════════════════════════════════")
        android.util.Log.d("MainActivity", "onCreate called - savedInstanceState: ${if (savedInstanceState == null) "NEW" else "RESTORED"}")
        android.util.Log.d("MainActivity", "Intent: ${intent?.toString()}")
        android.util.Log.d("MainActivity", "Intent action: ${intent?.action}")
        android.util.Log.d("MainActivity", "Intent data: ${intent?.data}")
        android.util.Log.d("MainActivity", "Intent extras: ${intent?.extras?.keySet()?.joinToString() ?: "null"}")
        android.util.Log.d("MainActivity", "conversationId extra: ${intent?.getStringExtra("conversationId")}")
        android.util.Log.d("MainActivity", "openChat extra: ${intent?.getBooleanExtra("openChat", false)}")
        android.util.Log.d("MainActivity", "Intent flags: ${intent?.flags}")
        android.util.Log.d("MainActivity", "═══════════════════════════════════════")
        
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
                    
                    // Track if auth state has been initialized
                    var authInitialized by remember { mutableStateOf(false) }
                    
                    // Track if initial navigation is complete
                    var navigationHandled by remember { mutableStateOf(false) }
                    
                    // Wait for auth state to be determined on first launch
                    LaunchedEffect(Unit) {
                        // Give Firebase Auth a moment to initialize
                        delay(100)
                        authInitialized = true
                        android.util.Log.d("MainActivity", "Auth initialized, isAuthenticated: $isAuthenticated")
                    }
                    
                    // Handle navigation events from notifications
                    LaunchedEffect(navigationEvent, isAuthenticated, authInitialized) {
                        if (!authInitialized) {
                            android.util.Log.d("MainActivity", "Auth not initialized yet, waiting...")
                            return@LaunchedEffect
                        }
                        
                        if (!isAuthenticated) {
                            android.util.Log.d("MainActivity", "Not authenticated yet, waiting...")
                            return@LaunchedEffect
                        }
                        
                        val event = navigationEvent
                        android.util.Log.d("MainActivity", "LaunchedEffect - event: $event, handled: $navigationHandled")
                        
                        when {
                            event is NavigationEvent.OpenChat -> {
                                // ALWAYS navigate for notification events
                                android.util.Log.d("MainActivity", "Navigating to chat from notification: ${event.conversationId}")
                                navController.navigate(Screen.Chat.createRoute(event.conversationId)) {
                                    // Keep conversation list in backstack so back button works
                                    popUpTo(Screen.ConversationList.route) {
                                        inclusive = false
                                    }
                                    launchSingleTop = true
                                }
                                // Mark as handled
                                navigationHandled = true
                                // Wait a bit before clearing
                                delay(500)
                                _navigationEvents.value = null
                                // DON'T reset navigationHandled here!
                            }
                            event == null && !navigationHandled -> {
                                android.util.Log.d("MainActivity", "Initial navigation to conversation list")
                                navController.navigate(Screen.ConversationList.createRoute()) {
                                    popUpTo(Screen.Login.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                                navigationHandled = true
                            }
                        }
                    }
                    
                    // Only show NavGraph after auth is initialized to prevent login screen flicker
                    if (authInitialized) {
                        NavGraph(navController = navController, authViewModel = authViewModel)
                    }
                }
            }
        }
    }
    
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        android.util.Log.d("MainActivity", "═══════════════════════════════════════")
        android.util.Log.d("MainActivity", "onNewIntent called")
        android.util.Log.d("MainActivity", "Intent: ${intent?.toString()}")
        android.util.Log.d("MainActivity", "Intent action: ${intent?.action}")
        android.util.Log.d("MainActivity", "Intent data: ${intent?.data}")
        android.util.Log.d("MainActivity", "Intent extras: ${intent?.extras?.keySet()?.joinToString() ?: "null"}")
        android.util.Log.d("MainActivity", "conversationId extra: ${intent?.getStringExtra("conversationId")}")
        android.util.Log.d("MainActivity", "openChat extra: ${intent?.getBooleanExtra("openChat", false)}")
        android.util.Log.d("MainActivity", "Intent flags: ${intent?.flags}")
        android.util.Log.d("MainActivity", "═══════════════════════════════════════")
        setIntent(intent)
        handleIntent(intent)
    }
    
    private fun handleIntent(intent: Intent?) {
        android.util.Log.d("MainActivity", "handleIntent called")
        
        // Try to extract conversationId from extras first
        var conversationId = intent?.getStringExtra("conversationId")
        var openChat = intent?.getBooleanExtra("openChat", false) ?: false
        
        // If not in extras, try to extract from data URI (gchat://conversation/{id})
        if (conversationId == null && intent?.data != null) {
            val uri = intent.data
            android.util.Log.d("MainActivity", "Intent data URI: $uri")
            if (uri?.scheme == "gchat" && uri.host == "conversation") {
                conversationId = uri.pathSegments?.firstOrNull()
                openChat = true
                android.util.Log.d("MainActivity", "Extracted conversationId from URI: $conversationId")
            }
        }
        
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
