package com.gchat.presentation.navigation

/**
 * Navigation destinations
 */
sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object ConversationList : Screen("conversations?conversationId={conversationId}") {
        fun createRoute(conversationId: String? = null) = 
            if (conversationId != null) "conversations?conversationId=$conversationId" 
            else "conversations"
    }
    object Profile : Screen("profile")
    object NewConversation : Screen("new_conversation")
    object CreateGroup : Screen("create_group")
    data object Chat : Screen("chat/{conversationId}") {
        fun createRoute(conversationId: String) = "chat/$conversationId"
    }
    data object GroupInfo : Screen("group_info/{conversationId}") {
        fun createRoute(conversationId: String) = "group_info/$conversationId"
    }
    data object DMInfo : Screen("dm_info/{conversationId}") {
        fun createRoute(conversationId: String) = "dm_info/$conversationId"
    }
    data object ImageViewer : Screen("image_viewer/{imageUrl}") {
        fun createRoute(imageUrl: String) = "image_viewer/${java.net.URLEncoder.encode(imageUrl, "UTF-8")}"
    }
}

