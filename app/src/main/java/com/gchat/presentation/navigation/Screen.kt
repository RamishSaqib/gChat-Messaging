package com.gchat.presentation.navigation

/**
 * Navigation destinations
 */
sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object ConversationList : Screen("conversations")
    object NewConversation : Screen("new_conversation")
    data object Chat : Screen("chat/{conversationId}") {
        fun createRoute(conversationId: String) = "chat/$conversationId"
    }
    data object ImageViewer : Screen("image_viewer/{imageUrl}") {
        fun createRoute(imageUrl: String) = "image_viewer/${java.net.URLEncoder.encode(imageUrl, "UTF-8")}"
    }
}

