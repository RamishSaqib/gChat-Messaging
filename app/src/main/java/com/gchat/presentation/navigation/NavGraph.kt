package com.gchat.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.gchat.presentation.auth.AuthViewModel
import com.gchat.presentation.auth.LoginScreen
import com.gchat.presentation.auth.RegisterScreen
import com.gchat.presentation.chat.ChatScreen
import com.gchat.presentation.chat.ConversationListScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    // Use a fixed start destination to avoid crashes on auth state changes
    NavHost(
        navController = navController,
        startDestination = Screen.Login.route
    ) {
        // Authentication screens
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.ConversationList.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                }
            )
        }
        
        composable(Screen.Register.route) {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate(Screen.ConversationList.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                }
            )
        }
        
        // Main app screens
        composable(Screen.ConversationList.route) {
            ConversationListScreen(
                onConversationClick = { conversationId ->
                    navController.navigate(Screen.Chat.createRoute(conversationId))
                },
                onNewConversationClick = {
                    // TODO: Implement new conversation flow
                },
                onLogout = {
                    // Clear back stack and navigate to login
                    navController.navigate(Screen.Login.route) {
                        popUpTo(navController.graph.startDestinationId) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }
            )
        }
        
        composable(
            route = Screen.Chat.route,
            arguments = listOf(
                navArgument("conversationId") { type = NavType.StringType }
            )
        ) {
            ChatScreen(
                conversationId = it.arguments?.getString("conversationId") ?: "",
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}

