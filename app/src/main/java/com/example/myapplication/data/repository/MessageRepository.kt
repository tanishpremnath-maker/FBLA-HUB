package com.example.myapplication.data.repository

import com.example.myapplication.ChatMessage
import com.example.myapplication.NewsArticle
import com.example.myapplication.UserProfile
import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Writes direct messages to Firestore. Grows as more chat features migrate to MVVM.
 */
@Singleton
class MessageRepository @Inject constructor(
    private val db: FirebaseFirestore
) {
    /** Sends a news article as a chat message attachment to [receiver]. */
    fun sendArticle(senderId: String, senderName: String, receiver: UserProfile, article: NewsArticle) {
        val msgId = db.collection("messages").document().id
        val msg = ChatMessage(
            id = msgId,
            senderId = senderId,
            receiverId = receiver.uid,
            message = "${article.imageUrl}\n${article.summary.take(200)}",
            timestamp = System.currentTimeMillis(),
            senderName = senderName,
            attachmentType = "article",
            attachmentUrl = article.url,
            attachmentName = article.title
        )
        db.collection("messages").document(msgId).set(msg)
    }
}
