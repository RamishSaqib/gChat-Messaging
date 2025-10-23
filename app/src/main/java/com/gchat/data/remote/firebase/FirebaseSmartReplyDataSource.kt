package com.gchat.data.remote.firebase

import com.gchat.domain.model.EmojiUsage
import com.gchat.domain.model.PunctuationStyle
import com.gchat.domain.model.ReplyCategory
import com.gchat.domain.model.SmartReply
import com.gchat.domain.model.Tone
import com.gchat.domain.model.UserCommunicationStyle
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase data source for smart reply operations
 * 
 * Calls Cloud Functions for AI-powered smart reply generation with RAG
 */
@Singleton
class FirebaseSmartReplyDataSource @Inject constructor(
    private val functions: FirebaseFunctions
) {
    
    /**
     * Generate smart reply suggestions using Cloud Function
     * 
     * @param conversationId The conversation ID
     * @param incomingMessageId The ID of the message to reply to
     * @param targetLanguage The language for reply suggestions
     * @return Result containing smart replies and user style analysis
     */
    suspend fun generateSmartReplies(
        conversationId: String,
        incomingMessageId: String,
        targetLanguage: String
    ): Result<SmartReplyResponse> {
        return try {
            val data = hashMapOf(
                "conversationId" to conversationId,
                "incomingMessageId" to incomingMessageId,
                "targetLanguage" to targetLanguage
            )
            
            android.util.Log.d("FirebaseSmartReply", "Calling generateSmartReplies function: $data")
            
            val result = functions
                .getHttpsCallable("generateSmartReplies")
                .call(data)
                .await()
            
            val resultData = result.data as? Map<*, *>
                ?: return Result.failure(Exception("Invalid response from smart reply service"))
            
            android.util.Log.d("FirebaseSmartReply", "Smart reply result received: ${resultData.keys}")
            
            // Parse replies
            val repliesData = resultData["replies"] as? List<*>
                ?: return Result.failure(Exception("Replies data missing"))
            
            val replies = repliesData.mapNotNull { replyData ->
                val reply = replyData as? Map<*, *> ?: return@mapNotNull null
                
                val replyText = reply["replyText"] as? String
                    ?: return@mapNotNull null
                
                val confidence = when (val conf = reply["confidence"]) {
                    is Double -> conf.toFloat()
                    is Number -> conf.toFloat()
                    else -> 0.9f
                }
                
                val categoryStr = reply["category"] as? String ?: "NEUTRAL"
                val category = try {
                    ReplyCategory.valueOf(categoryStr)
                } catch (e: IllegalArgumentException) {
                    ReplyCategory.NEUTRAL
                }
                
                SmartReply(
                    replyText = replyText,
                    confidence = confidence,
                    category = category
                )
            }
            
            if (replies.isEmpty()) {
                return Result.failure(Exception("No valid replies generated"))
            }
            
            // Parse user style
            val userStyleData = resultData["userStyle"] as? Map<*, *>
            val userStyle = userStyleData?.let { parseUserStyle(it) }
            
            val cached = resultData["cached"] as? Boolean ?: false
            
            android.util.Log.d("FirebaseSmartReply", "Parsed ${replies.size} smart replies")
            
            Result.success(
                SmartReplyResponse(
                    replies = replies,
                    userStyle = userStyle,
                    cached = cached
                )
            )
        } catch (e: Exception) {
            android.util.Log.e("FirebaseSmartReply", "Smart reply generation error", e)
            Result.failure(e)
        }
    }
    
    /**
     * Parse user communication style from Cloud Function response
     */
    private fun parseUserStyle(styleData: Map<*, *>): UserCommunicationStyle {
        val avgMessageLength = when (val val1 = styleData["avgMessageLength"]) {
            is Number -> val1.toInt()
            else -> 10
        }
        
        val emojiUsageStr = styleData["emojiUsage"] as? String ?: "OCCASIONAL"
        val emojiUsage = try {
            EmojiUsage.valueOf(emojiUsageStr)
        } catch (e: IllegalArgumentException) {
            EmojiUsage.OCCASIONAL
        }
        
        val toneStr = styleData["tone"] as? String ?: "CONVERSATIONAL"
        val tone = try {
            Tone.valueOf(toneStr)
        } catch (e: IllegalArgumentException) {
            Tone.CONVERSATIONAL
        }
        
        @Suppress("UNCHECKED_CAST")
        val commonPhrases = (styleData["commonPhrases"] as? List<String>) ?: emptyList()
        
        val usesContractions = styleData["usesContractions"] as? Boolean ?: true
        
        val punctuationStyleStr = styleData["punctuationStyle"] as? String ?: "standard"
        val punctuationStyle = when (punctuationStyleStr.lowercase()) {
            "minimal" -> PunctuationStyle.MINIMAL
            "expressive" -> PunctuationStyle.EXPRESSIVE
            else -> PunctuationStyle.STANDARD
        }
        
        return UserCommunicationStyle(
            avgMessageLength = avgMessageLength,
            emojiUsage = emojiUsage,
            tone = tone,
            commonPhrases = commonPhrases,
            usesContractions = usesContractions,
            punctuationStyle = punctuationStyle
        )
    }
}

/**
 * Response from smart reply Cloud Function
 */
data class SmartReplyResponse(
    val replies: List<SmartReply>,
    val userStyle: UserCommunicationStyle?,
    val cached: Boolean
)

