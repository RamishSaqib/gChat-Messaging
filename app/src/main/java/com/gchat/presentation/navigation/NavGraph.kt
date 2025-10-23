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
import com.gchat.presentation.creategroup.CreateGroupScreen
import com.gchat.presentation.dminfo.DMInfoScreen
import com.gchat.presentation.groupinfo.GroupInfoScreen
import com.gchat.presentation.imageviewer.ImageViewerScreen
import com.gchat.presentation.newconversation.NewConversationScreen
import com.gchat.presentation.profile.ProfileScreen
import java.net.URLDecoder

@Composable
fun NavGraph(
    navController: NavHostController,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val isAuthenticated by authViewModel.isAuthenticated.collectAsState()
    
    // Use dynamic start destination based on auth state
    NavHost(
        navController = navController,
        startDestination = if (isAuthenticated) Screen.ConversationList.route else Screen.Login.route
    ) {
        // Authentication screens
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.ConversationList.createRoute()) {
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
                    navController.navigate(Screen.ConversationList.createRoute()) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                }
            )
        }
        
        // Main app screens
        composable(
            route = Screen.ConversationList.route,
            arguments = listOf(
                navArgument("conversationId") { 
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) {
            val pendingConversationId = it.arguments?.getString("conversationId")
            ConversationListScreen(
                pendingConversationId = pendingConversationId,
                onConversationClick = { conversationId ->
                    navController.navigate(Screen.Chat.createRoute(conversationId))
                },
                onNewConversationClick = {
                    navController.navigate(Screen.NewConversation.route)
                },
                onNewGroupClick = {
                    navController.navigate(Screen.CreateGroup.route)
                },
                onProfileClick = {
                    navController.navigate(Screen.Profile.route)
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
        
        composable(Screen.Profile.route) {
            ProfileScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.NewConversation.route) {
            NewConversationScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onConversationCreated = { conversationId ->
                    // Navigate to the chat screen and remove new conversation from back stack
                    navController.navigate(Screen.Chat.createRoute(conversationId)) {
                        popUpTo(Screen.ConversationList.createRoute())
                    }
                }
            )
        }
        
        composable(Screen.CreateGroup.route) {
            CreateGroupScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onGroupCreated = { conversationId ->
                    // Navigate to the chat screen and remove create group from back stack
                    navController.navigate(Screen.Chat.createRoute(conversationId)) {
                        popUpTo(Screen.ConversationList.createRoute())
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
            val conversationId = it.arguments?.getString("conversationId") ?: ""
            ChatScreen(
                conversationId = conversationId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToGroupInfo = { convId ->
                    navController.navigate(Screen.GroupInfo.createRoute(convId))
                },
                onNavigateToDMInfo = { convId ->
                    navController.navigate(Screen.DMInfo.createRoute(convId))
                }
            )
        }
        
        composable(
            route = Screen.GroupInfo.route,
            arguments = listOf(
                navArgument("conversationId") { type = NavType.StringType }
            )
        ) {
            GroupInfoScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onAddMembers = {
                    // TODO: Navigate to add members screen
                }
            )
        }
        
        composable(
            route = Screen.DMInfo.route,
            arguments = listOf(
                navArgument("conversationId") { type = NavType.StringType }
            )
        ) {
            DMInfoScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(
            route = Screen.ImageViewer.route,
            arguments = listOf(
                navArgument("imageUrl") { type = NavType.StringType }
            )
        ) {
            val encodedUrl = it.arguments?.getString("imageUrl") ?: ""
            val imageUrl = URLDecoder.decode(encodedUrl, "UTF-8")
            ImageViewerScreen(
                imageUrl = imageUrl,
                onDismiss = {
                    navController.popBackStack()
                }
            )
        }
    }
}

