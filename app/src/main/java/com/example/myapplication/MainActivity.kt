
package com.example.myapplication

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.util.*
import androidx.core.content.FileProvider
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import java.io.File
import java.io.FileOutputStream
import androidx.camera.core.Preview
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned

fun generateQrBitmap(content: String, size: Int = 512): Bitmap {
    val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size)
    val bm = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
    for (x in 0 until size) for (y in 0 until size) {
        bm.setPixel(x, y, if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
    }
    return bm
}

suspend fun sendVerificationEmail(toEmail: String, code: String) = withContext(Dispatchers.IO) {
    val data = hashMapOf(
        "email" to toEmail,
        "code" to code
    )
    FirebaseFunctions.getInstance()
        .getHttpsCallable("send2FAEmail")
        .call(data)
        .await()
    android.util.Log.d("EmailJS", "2FA email sent to $toEmail")
}

suspend fun sendPasswordResetEmail(toEmail: String) = withContext(Dispatchers.IO) {
    val data = hashMapOf("email" to toEmail)
    FirebaseFunctions.getInstance()
        .getHttpsCallable("sendPasswordReset")
        .call(data)
        .await()
    android.util.Log.d("EmailJS", "Password reset email requested for $toEmail")
}

/** Returns (valid, reason). reason is a user-facing message when valid is false. */
suspend fun verifyPasswordResetCode(email: String, code: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
    val data = hashMapOf("email" to email, "code" to code)
    val result = FirebaseFunctions.getInstance()
        .getHttpsCallable("verifyPasswordResetCode")
        .call(data)
        .await()
    @Suppress("UNCHECKED_CAST")
    val map = result.getData() as? Map<String, Any?> ?: emptyMap()
    val valid = map["valid"] as? Boolean ?: false
    val reason = map["reason"] as? String ?: ""
    valid to reason
}

/** Sets the new password. Throws (with a user-facing message) on any failure. */
suspend fun confirmPasswordReset(email: String, code: String, newPassword: String) = withContext(Dispatchers.IO) {
    val data = hashMapOf("email" to email, "code" to code, "newPassword" to newPassword)
    FirebaseFunctions.getInstance()
        .getHttpsCallable("confirmPasswordReset")
        .call(data)
        .await()
}

fun shareToInstagramStory(context: android.content.Context, title: String, subtitle: String, eventName: String = "", pfpBitmap: android.graphics.Bitmap? = null) {
    try {
        val width = 1080
        val height = 1920
        val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val yellow = android.graphics.Color.parseColor("#FFB800")
        val navy = android.graphics.Color.parseColor("#0A1628")
        val lightBlue = android.graphics.Color.parseColor("#B0C4DE")

        // Background
        val bgPaint = Paint().apply { color = navy }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Top accent bar
        val accentPaint = Paint().apply { color = yellow }
        canvas.drawRect(0f, 0f, width.toFloat(), 20f, accentPaint)

        // Bottom accent bar
        canvas.drawRect(0f, height - 20f, width.toFloat(), height.toFloat(), accentPaint)

        // --- TOP SECTION: FBLA HUB header ---
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = yellow
            textSize = 56f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.3f
        }
        canvas.drawText("FBLA HUB", 80f, 160f, labelPaint)
        val linePaint = Paint().apply { color = yellow; strokeWidth = 3f }
        canvas.drawLine(80f, 185f, 1000f, 185f, linePaint)

        // Profile avatar circle in upper area
        val cx = 540f; val cy = 480f; val radius = 220f
        if (pfpBitmap != null) {
            val clipPath = android.graphics.Path().apply { addCircle(cx, cy, radius, android.graphics.Path.Direction.CW) }
            canvas.save()
            canvas.clipPath(clipPath)
            val scaled = android.graphics.Bitmap.createScaledBitmap(pfpBitmap, (radius * 2).toInt(), (radius * 2).toInt(), true)
            canvas.drawBitmap(scaled, cx - radius, cy - radius, null)
            canvas.restore()
        } else {
            val ringFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.argb(25, 255, 184, 0); style = Paint.Style.FILL }
            canvas.drawCircle(cx, cy, radius, ringFill)
            val catPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = yellow; textSize = 44f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                letterSpacing = 0.2f; textAlign = Paint.Align.CENTER
            }
            canvas.drawText("COMPETITION", cx, cy - 20f, catPaint)
            val subCatPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = lightBlue; textSize = 36f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL); textAlign = Paint.Align.CENTER
            }
            canvas.drawText("REMINDER", cx, cy + 30f, subCatPaint)
        }
        val ringBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.argb(80, 255, 184, 0); style = Paint.Style.STROKE; strokeWidth = 8f
        }
        canvas.drawCircle(cx, cy, radius, ringBorderPaint)

        // --- MIDDLE SECTION: main content centered vertically (y=800–1400) ---
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = 90f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        // Wrap title text, starting at y=840
        val words = title.split(" ")
        var line = ""
        var y = 840f
        val titleLines = mutableListOf<String>()
        for (word in words) {
            val test = if (line.isEmpty()) word else "$line $word"
            if (titlePaint.measureText(test) > 920f) {
                titleLines.add(line)
                line = word
            } else line = test
        }
        if (line.isNotEmpty()) titleLines.add(line)
        for (tLine in titleLines) {
            canvas.drawText(tLine, 80f, y, titlePaint)
            y += 115f
        }

        // Subtitle
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = lightBlue
            textSize = 52f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        y += 30f
        canvas.drawText(subtitle, 80f, y, subPaint)
        y += 30f

        // Competing event badge
        if (eventName.isNotBlank()) {
            val badgeY = y + 100f
            val badgePadH = 40f
            val badgePadV = 28f
            val eventLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = yellow
                textSize = 52f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val eventTagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = lightBlue
                textSize = 38f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }
            canvas.drawText("COMPETING IN", 80f, badgeY, eventTagPaint)
            val badgeTextWidth = eventLabelPaint.measureText(eventName)
            val badgeRect = android.graphics.RectF(
                76f, badgeY + 16f,
                80f + badgeTextWidth + badgePadH * 2, badgeY + 16f + eventLabelPaint.textSize + badgePadV * 2
            )
            val badgeBgPaint = Paint().apply {
                color = android.graphics.Color.argb(30, 255, 184, 0)
            }
            val badgeBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = yellow
                style = Paint.Style.STROKE
                strokeWidth = 3f
            }
            canvas.drawRoundRect(badgeRect, 16f, 16f, badgeBgPaint)
            canvas.drawRoundRect(badgeRect, 16f, 16f, badgeBorderPaint)
            canvas.drawText(eventName, 80f + badgePadH, badgeY + 16f + badgePadV + eventLabelPaint.textSize - 10f, eventLabelPaint)
        }

        // --- BOTTOM SECTION: branding ---
        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = yellow
            textSize = 42f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("fbla.org  •  FBLA Hub App", 80f, height - 70f, brandPaint)

        // Save to cache
        val imagesDir = File(context.cacheDir, "images").also { it.mkdirs() }
        val imageFile = File(imagesDir, "story.jpg")
        FileOutputStream(imageFile).use { bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 95, it) }

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", imageFile)

        context.grantUriPermission("com.instagram.android", uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)

        try {
            val instagramIntent = Intent("com.instagram.share.ADD_TO_STORY").apply {
                setDataAndType(uri, "image/jpeg")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                setPackage("com.instagram.android")
            }
            context.startActivity(instagramIntent)
        } catch (e: android.content.ActivityNotFoundException) {
            // Instagram not installed — fall back to generic share
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, "$title\n\nShared via FBLA Hub")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share to..."))
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Could not share: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

@dagger.hilt.android.AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
// Removed enableEdgeToEdge() to prevent content from going under status bar

// Create notification channel
        createNotificationChannel()

        setContent {
            MyApplicationTheme {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF0A1628)),
                    contentAlignment = Alignment.Center
                ) {
                    val isTablet = maxWidth > 600.dp
                    Box(
                        modifier = if (isTablet) {
                            Modifier
                                .width(412.dp)
                                .fillMaxHeight()
                        } else {
                            Modifier.fillMaxSize()
                        }
                    ) {
                        MainScreen()
                    }
                }
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channels = listOf(
                NotificationChannel("chat_messages", "Direct Messages", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Notifications for new direct messages"
                },
                NotificationChannel("group_messages", "Group Messages", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Notifications for new group chat messages"
                },
                NotificationChannel("connections", "Connections", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Connection requests and acceptances"
                },
                NotificationChannel("event_reminders", "Event Reminders", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Reminders for upcoming FBLA competitions"
                }
            )
            channels.forEach { nm.createNotificationChannel(it) }
        }
    }
}

fun sendMessageNotification(context: Context, senderName: String, message: String) {
    try {
        val notificationId = System.currentTimeMillis().toInt()
        val builder = NotificationCompat.Builder(context, "chat_messages")
            .setSmallIcon(R.drawable.fbla_tuff)
            .setContentTitle("New message from $senderName")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        val notificationManager = NotificationManagerCompat.from(context)
        try {
            notificationManager.notify(notificationId, builder.build())
        } catch (e: SecurityException) {
// Permission denied on Android 13+
            android.util.Log.e("Notification", "Permission not granted", e)
        }
    } catch (e: Exception) {
        android.util.Log.e("Notification", "Error showing notification", e)
    }
}

data class CompetitionReminder(
    val name: String = "",
    val date: String = "",
    val timestamp: Long = 0L,
    val calendarEventId: Long = -1L
)

private val PROFANITY_LIST = setOf(
    // fuck variants
    "fuck", "f*ck", "f**k", "fuk", "fck", "fucc", "fvck",
    // shit variants
    "shit", "sh*t", "sh!t", "sht",
    // bitch variants
    "bitch", "b*tch", "b!tch", "biatch",
    // ass variants
    "ass", "a**", "a55", "azz", "arse",
    // asshole variants
    "asshole", "a**hole", "jackass",
    // bastard
    "bastard", "bastrd",
    // damn
    "damn", "damnit",
    // crap
    "crap",
    // dick variants
    "dick", "d*ck", "dik", "d1ck",
    // cock variants
    "cock", "c*ck",
    // pussy variants
    "pussy", "p*ssy",
    // cunt variants
    "cunt", "c*nt",
    // whore variants
    "whore", "wh*re",
    // slut variants
    "slut", "sl*t",
    // racial slurs
    "nigga", "nigger", "n*gga", "n*gger",
    // homophobic slurs
    "faggot", "fag", "f*ggot",
    // ableist slurs
    "retard", "ret*rd",
    // insults
    "idiot", "stupid", "moron", "dumbass", "jerk", "prick",
    // harmful phrases
    "kill yourself", "kys",
    // abbreviations
    "wtf", "stfu", "gtfo",
    // piss variants
    "piss", "p*ss",
    // explicit content
    "sex", "porn", "nude", "naked", "rape",
    // misc
    "hell", "bollocks", "wanker", "twat", "motherfucker",
    "dumb", "ugly", "hate"
)

fun containsProfanity(text: String): Boolean {
    val normalized = text.lowercase()
        .replace("@", "a").replace("0", "o").replace("3", "e")
        .replace("1", "i").replace("5", "s").replace("$", "s")
        .replace("+", "t").replace("!", "i")
    return PROFANITY_LIST.any { word ->
        val pattern = Regex("\\b${Regex.escape(word)}\\b")
        pattern.containsMatchIn(normalized) || normalized.contains(word)
    }
}

fun generateRandomUsername(): String {
    val chars = "abcdefghijklmnopqrstuvwxyz0123456789"
    var name = (1..10).map { chars.random() }.joinToString("")
    while (containsProfanity(name)) { name = (1..10).map { chars.random() }.joinToString("") }
    return name
}

data class UserProfile(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val school: String = "",
    val year: String = "",
    val state: String = "",
    val bio: String = "",
    val pfpUrl: String = "",
    val reminders: List<CompetitionReminder> = emptyList(),
    val instagram: String = "",
    val twitter: String = "",
    val facebook: String = "",
    val linkedin: String = "",
    val role: String = "student",
    val chapterInstagram: String = "",
    val username: String = "",
    val usernameLastChanged: Long = 0L,
    val hideConnections: Boolean = false,
    val allowPeerSharing: Boolean = true
)

data class ConnectionRequest(
    val id: String = "",
    val fromUid: String = "",
    val fromName: String = "",
    val fromEmail: String = "",
    val fromSchool: String = "",
    val fromPfpUrl: String = "",
    val toUid: String = "",
    val toName: String = "",
    val status: String = "pending", // pending, accepted, declined
    val timestamp: Long = 0L
)

data class Connection(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val school: String = "",
    val year: String = "",
    val state: String = "",
    val pfpUrl: String = "",
    val reminders: List<CompetitionReminder> = emptyList(),
    val instagram: String = "",
    val twitter: String = "",
    val facebook: String = "",
    val linkedin: String = "",
    val username: String = "",
    val bio: String = "",
    val hideConnections: Boolean = false,
    val allowPeerSharing: Boolean = true,
    val role: String = "student"
)

data class ChatMessage(
    val id: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val senderName: String = "",
    val attachmentUrl: String = "",
    val attachmentName: String = "",
    val attachmentType: String = "",
    val replyToId: String = "",
    val replyToText: String = "",
    val replyToSender: String = "",
    val edited: Boolean = false,
    val reactions: Map<String, String> = emptyMap(),
    val seenAt: Long = 0L
)

data class Conversation(
    val otherUserId: String = "",
    val otherUserName: String = "",
    val otherUserPfp: String = "",
    val lastMessage: String = "",
    val lastMessageTime: Long = 0,
    val unreadCount: Int = 0
)

data class TwoFactorCode(
    val code: String = "",
    val email: String = "",
    val expiresAt: Long = 0L,
    val verified: Boolean = false
)

data class Classroom(
    val id: String = "",
    val name: String = "",
    val teacherId: String = "",
    val teacherName: String = "",
    val chapter: String = "",
    val state: String = "",
    val inviteCode: String = "",
    val memberIds: List<String> = emptyList(),
    val createdAt: Long = 0L
)

data class ClassroomMessage(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val message: String = "",
    val timestamp: Long = 0L,
    val attachmentUrl: String = "",
    val attachmentName: String = "",
    val attachmentType: String = ""
)

data class ClassroomPost(
    val id: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val content: String = "",
    val timestamp: Long = 0L,
    val attachmentUrl: String = "",
    val attachmentName: String = "",
    val attachmentType: String = "",
    val thumbnailBiasY: Float = 0.5f
)

data class ClassroomAssignment(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val dueDateMs: Long = 0L,
    val points: Int = 100,
    val createdAt: Long = 0L,
    val createdByName: String = ""
)

data class AssignmentSubmission(
    val studentId: String = "",
    val studentName: String = "",
    val submittedAt: Long = 0L,
    val content: String = "",
    val grade: Int = -1,
    val feedback: String = "",
    val attachmentUrl: String = "",
    val attachmentName: String = ""
)

data class GroupChat(
    val id: String = "",
    val name: String = "",
    val memberIds: List<String> = emptyList(),
    val memberNames: Map<String, String> = emptyMap(),
    val createdBy: String = "",
    val createdAt: Long = 0L,
    val lastMessage: String = "",
    val lastMessageTime: Long = 0L,
    val lastMessageSender: String = ""
)

data class GroupMessage(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val message: String = "",
    val timestamp: Long = 0L,
    val attachmentUrl: String = "",
    val attachmentName: String = "",
    val attachmentType: String = "",
    val replyToId: String = "",
    val replyToText: String = "",
    val replyToSender: String = "",
    val edited: Boolean = false,
    val reactions: Map<String, String> = emptyMap(),
    val type: String = "",
    val pollId: String = ""
)

data class ClassroomComment(
    val id: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val content: String = "",
    val timestamp: Long = 0L
)

data class Poll(
    val id: String = "",
    val question: String = "",
    val options: List<String> = emptyList(),
    val createdBy: String = "",
    val createdByName: String = "",
    val createdAt: Long = 0L,
    val closed: Boolean = false,
    val votes: Map<String, Long> = emptyMap()
)

sealed class StreamItem {
    data class PostItem(val post: ClassroomPost) : StreamItem()
    data class PollItem(val poll: Poll) : StreamItem()
}

object RecentReactions {
    val list = androidx.compose.runtime.mutableStateListOf<String>()
    fun add(emoji: String) {
        list.remove(emoji)
        list.add(0, emoji)
        if (list.size > 16) list.removeAt(list.size - 1)
    }
}

fun normalizeChapterKey(chapter: String): String {
    var s = chapter.trim().lowercase(Locale.getDefault())
    if (s.endsWith(" fbla")) s = s.removeSuffix(" fbla").trim()
    return s
}

fun randomClassroomInviteCode(): String {
    val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    return buildString(8) { repeat(8) { append(chars.random()) } }
}

val LocalReduceMotion = compositionLocalOf { false }

@Composable
fun InfoIconButton(
    text: String,
    primaryColor: Color,
    lightMode: Boolean,
    size: Int = 18
) {
    var show by remember { mutableStateOf(false) }
    val textColor = if (lightMode) Color(0xFF1A1A2E) else Color.White
    Box(
        modifier = Modifier
            .size(size.dp)
            .background(primaryColor.copy(alpha = 0.15f), CircleShape)
            .clickable { show = true },
        contentAlignment = Alignment.Center
    ) {
        Text("i", color = primaryColor, fontSize = (size * 0.6f).sp, fontWeight = FontWeight.Bold)
    }
    if (show) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { show = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (lightMode) Color(0xFFFFFFFF) else Color(0xFF1A2540),
                tonalElevation = 4.dp,
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.size(28.dp).background(primaryColor.copy(alpha = 0.15f), CircleShape), contentAlignment = Alignment.Center) {
                            Text("i", color = primaryColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Text("How it works", color = textColor, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    Text(text, color = textColor.copy(alpha = 0.85f), fontSize = 13.sp, lineHeight = 19.sp)
                    TextButton(onClick = { show = false }, modifier = Modifier.align(Alignment.End)) {
                        Text("Got it", color = primaryColor, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

fun Modifier.pressScale(targetScale: Float = 0.94f): Modifier = composed {
    val reduceMotion = LocalReduceMotion.current
    if (reduceMotion) return@composed this
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) targetScale else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "pressScale"
    )
    this
        .graphicsLayer(scaleX = scale, scaleY = scale)
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    awaitPointerEvent(PointerEventPass.Main)
                    pressed = currentEvent.changes.any { it.pressed }
                }
            }
        }
}

@Composable
fun BackButton(onClick: () -> Unit, tint: Color = Color.White, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = tint.copy(alpha = 0.10f),
        modifier = modifier.pressScale(0.88f).size(36.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = tint,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

data class FblaEvent(val name: String, val date: String, val location: String, val timestamp: Long = 0L)

val US_STATES: List<String> = listOf(
    "Alabama", "Alaska", "Arizona", "Arkansas", "California", "Colorado", "Connecticut",
    "Delaware", "Florida", "Georgia", "Hawaii", "Idaho", "Illinois", "Indiana", "Iowa",
    "Kansas", "Kentucky", "Louisiana", "Maine", "Maryland", "Massachusetts", "Michigan",
    "Minnesota", "Mississippi", "Missouri", "Montana", "Nebraska", "Nevada", "New Hampshire",
    "New Jersey", "New Mexico", "New York", "North Carolina", "North Dakota", "Ohio",
    "Oklahoma", "Oregon", "Pennsylvania", "Rhode Island", "South Carolina", "South Dakota",
    "Tennessee", "Texas", "Utah", "Vermont", "Virginia", "Washington", "West Virginia",
    "Wisconsin", "Wyoming"
)

val FBLA_STATE_CHAPTERS: List<String> = US_STATES.map { "$it FBLA" }

data class NewsArticle(val title: String, val summary: String, val date: String, val url: String, val tag: String = "NATIONAL", val timestamp: Long = 0L, val imageUrl: String = "")
data class ResourceItem(val title: String, val description: String, val url: String, val icon: ImageVector)

// Helper function to parse event dates
@Composable
fun Modifier.animatedClickable(
    onClick: () -> Unit
): Modifier {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "button_scale"
    )

    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null
        ) {
            onClick()
        }
        .pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                    isPressed = true
                    tryAwaitRelease()
                    isPressed = false
                }
            )
        }
}

// Helper function to invert color lightness values (light ↔ dark)
fun invertColorValues(color: Color, invert: Boolean): Color {
    if (!invert) return color
    return Color(
        red = 1f - color.red,
        green = 1f - color.green,
        blue = 1f - color.blue,
        alpha = color.alpha
    )
}

@Composable
fun MainScreen() {
    var selectedTab by remember { mutableIntStateOf(2) }
    val auth = FirebaseAuth.getInstance()
    var currentUser by remember { mutableStateOf(auth.currentUser) }
    var showAuthFlow by remember { mutableStateOf(false) }
    var currentSubScreen by remember { mutableStateOf<String?>(null) }
    var showTutorial by remember { mutableStateOf(false) }

// User profile data for ResourcesScreen
    val db = FirebaseFirestore.getInstance()
    var userState by remember { mutableStateOf("") }
    var userChapter by remember { mutableStateOf("") }
    var userInstagram by remember { mutableStateOf("") }
    var hideConnections by remember { mutableStateOf(false) }
    var allowPeerSharing by remember { mutableStateOf(true) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val activity = context as? androidx.activity.ComponentActivity

// Incoming share from other apps
    var shareReceiveText by remember { mutableStateOf<String?>(null) }
    var shareReceiveUri by remember { mutableStateOf<Uri?>(null) }
    var shareReceiveConnections by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var shareReceiveLoadingConnections by remember { mutableStateOf(false) }
    var shareReceiveUploading by remember { mutableStateOf(false) }
    val showShareReceive = shareReceiveText != null || shareReceiveUri != null

// Load user profile data
    LaunchedEffect(auth.currentUser?.uid) {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            db.collection("users").document(uid).addSnapshotListener { snapshot, _ ->
                val profile = snapshot?.toObject(UserProfile::class.java)
                userState = profile?.state ?: ""
                userChapter = profile?.school ?: ""
                userInstagram = profile?.chapterInstagram ?: ""
                hideConnections = profile?.hideConnections ?: false
                allowPeerSharing = profile?.allowPeerSharing ?: true
            }
        }
    }

// Persisted settings
    val bgOpacity by SettingsStore.bgOpacityFlow(context).collectAsState(initial = 0.9f)
    val lightMode by SettingsStore.lightModeFlow(context).collectAsState(initial = false)
    val primaryColor by SettingsStore.primaryColorFlow(context).collectAsState(initial = Color(0xFFFFB800))
    val savedColors by SettingsStore.savedColorsFlow(context).collectAsState(initial = listOf())
    val textSizeMultiplier by SettingsStore.textSizeFlow(context).collectAsState(initial = 1.0f)
    val isSimplifiedUI by SettingsStore.simplifiedUiFlow(context).collectAsState(initial = false)
    val notificationsEnabled by SettingsStore.notificationsEnabledFlow(context).collectAsState(initial = true)
    val notifMessages by SettingsStore.notifMessagesFlow(context).collectAsState(initial = true)
    val notifGroupMessages by SettingsStore.notifGroupMessagesFlow(context).collectAsState(initial = true)
    val notifConnectionRequests by SettingsStore.notifConnectionRequestsFlow(context).collectAsState(initial = true)
    val notifConnectionAccepted by SettingsStore.notifConnectionAcceptedFlow(context).collectAsState(initial = true)
    val notifEventReminders by SettingsStore.notifEventRemindersFlow(context).collectAsState(initial = true)
    val publicProfile by SettingsStore.publicProfileFlow(context).collectAsState(initial = true)
    val boldText by SettingsStore.boldTextFlow(context).collectAsState(initial = false)
    val reduceMotion by SettingsStore.reduceMotionFlow(context).collectAsState(initial = false)

// Register FCM token and request notification permission when logged in
    LaunchedEffect(auth.currentUser?.uid) {
        val uid = auth.currentUser?.uid ?: return@LaunchedEffect
        com.google.firebase.messaging.FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                db.collection("users").document(uid).update(
                    mapOf(
                        "fcmToken" to token,
                        "notifEnabled" to true,
                        "notifMessages" to true,
                        "notifGroupMessages" to true,
                        "notifConnectionRequests" to true,
                        "notifConnectionAccepted" to true,
                        "notifEventReminders" to true
                    )
                )
            }
    }

// Process share intent helper
    fun processShareIntent(intent: Intent) {
        if (intent.action != Intent.ACTION_SEND) return
        when {
            intent.type == "text/plain" -> {
                val text = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return
                shareReceiveText = text
                shareReceiveUri = null
            }
            intent.type?.startsWith("image/") == true || intent.type?.startsWith("video/") == true -> {
                val uri = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU)
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                else @Suppress("DEPRECATION") intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                if (uri != null) { shareReceiveUri = uri; shareReceiveText = null }
            }
        }
    }

// Check initial launch intent
    LaunchedEffect(Unit) {
        activity?.intent?.let { processShareIntent(it) }
        activity?.intent = Intent()
    }

// Listen for share intents while app is already running (onNewIntent)
    DisposableEffect(activity) {
        val listener = androidx.core.util.Consumer<Intent> { processShareIntent(it) }
        activity?.addOnNewIntentListener(listener)
        onDispose { activity?.removeOnNewIntentListener(listener) }
    }

// Load connections when share dialog opens
    LaunchedEffect(showShareReceive) {
        if (!showShareReceive || auth.currentUser?.uid == null) return@LaunchedEffect
        shareReceiveLoadingConnections = true
        val uid = auth.currentUser!!.uid
        db.collection("connections").document(uid).get().addOnSuccessListener { doc ->
            val uids = (doc.get("connectedUids") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
            if (uids.isEmpty()) { shareReceiveLoadingConnections = false; return@addOnSuccessListener }
            val loaded = mutableListOf<UserProfile>()
            var remaining = uids.size.coerceAtMost(20)
            uids.take(20).forEach { cUid ->
                db.collection("users").document(cUid).get().addOnSuccessListener { d ->
                    d.toObject(UserProfile::class.java)?.let { loaded.add(it) }
                    remaining--; if (remaining <= 0) { shareReceiveConnections = loaded.toList(); shareReceiveLoadingConnections = false }
                }.addOnFailureListener { remaining--; if (remaining <= 0) { shareReceiveConnections = loaded.toList(); shareReceiveLoadingConnections = false } }
            }
        }.addOnFailureListener { shareReceiveLoadingConnections = false }
    }

    val effectivePrimary = primaryColor
    val effectiveBgOpacity = bgOpacity

    val effectivePrimaryColor = invertColorValues(effectivePrimary, lightMode)

// Dynamic navigation bar colors
    val navBarBgColor = if (lightMode) Color(0xFFD8E4F8) else Color(0xFF000D1A)
    val navBarContentColor = if (lightMode) Color(0xFF1A1A2E) else Color.White

    var navBarBounds by remember { mutableStateOf(Rect.Zero) }

    val baseTypography = MaterialTheme.typography
    val effectiveTypography = if (boldText) Typography(
        displayLarge = baseTypography.displayLarge.copy(fontWeight = FontWeight.ExtraBold),
        displayMedium = baseTypography.displayMedium.copy(fontWeight = FontWeight.ExtraBold),
        displaySmall = baseTypography.displaySmall.copy(fontWeight = FontWeight.Bold),
        headlineLarge = baseTypography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
        headlineMedium = baseTypography.headlineMedium.copy(fontWeight = FontWeight.Bold),
        headlineSmall = baseTypography.headlineSmall.copy(fontWeight = FontWeight.Bold),
        titleLarge = baseTypography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
        titleMedium = baseTypography.titleMedium.copy(fontWeight = FontWeight.Bold),
        titleSmall = baseTypography.titleSmall.copy(fontWeight = FontWeight.Bold),
        bodyLarge = baseTypography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
        bodyMedium = baseTypography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
        bodySmall = baseTypography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = baseTypography.labelLarge.copy(fontWeight = FontWeight.Bold),
        labelMedium = baseTypography.labelMedium.copy(fontWeight = FontWeight.Bold),
        labelSmall = baseTypography.labelSmall.copy(fontWeight = FontWeight.Bold),
    ) else baseTypography

    CompositionLocalProvider(
        LocalReduceMotion provides reduceMotion
    ) {
    MaterialTheme(typography = effectiveTypography) {
    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            if (currentSubScreen == null) {
                Box(modifier = Modifier.onGloballyPositioned { coords ->
                    navBarBounds = coords.boundsInRoot()
                }) {
                NavigationBar(containerColor = navBarBgColor, contentColor = navBarContentColor) {
                    listOf(
                        Triple(0, Icons.Filled.Person, "Profile"),
                        Triple(1, Icons.Filled.Event, "Events"),
                        Triple(2, Icons.Filled.Home, "Home"),
                        Triple(3, Icons.AutoMirrored.Filled.Article, "News"),
                        Triple(4, Icons.AutoMirrored.Filled.MenuBook, "Resources")
                    ).forEach { (index, icon, label) ->
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label, style = TextStyle(fontSize = (12 * textSizeMultiplier).sp)) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = effectivePrimaryColor,
                                selectedTextColor = effectivePrimaryColor,
                                unselectedIconColor = navBarContentColor.copy(alpha = 0.65f),
                                unselectedTextColor = navBarContentColor.copy(alpha = 0.65f),
                                indicatorColor = navBarContentColor.copy(alpha = 0.1f)
                            )
                        )
                    }
                }
                } // close onGloballyPositioned Box
            }
        }
    ) { padding ->
        val reduceMotionForCrossfade = LocalReduceMotion.current
        Crossfade(
            targetState = if (currentSubScreen != null) "subscreen" to currentSubScreen else "main" to selectedTab,
            animationSpec = if (reduceMotionForCrossfade) snap() else tween(durationMillis = 300, easing = LinearEasing),
            label = "screen_transition",
            modifier = Modifier.fillMaxSize()
        ) { (screenType, screenId) ->
            Box(
                modifier = Modifier
                    .padding(if (screenType == "subscreen") PaddingValues(0.dp) else padding)
                    .fillMaxSize()
            ) {
                if (screenType == "subscreen") {
                    when {
                        screenId == "settings" -> SettingsScreen(
                            onBack = { currentSubScreen = null },
                            onTutorialClick = { currentSubScreen = null; showTutorial = true },
                            primaryColor = primaryColor,
                            onPrimaryColorChange = { scope.launch { SettingsStore.savePrimaryColor(context, it) } },
                            bgOpacity = bgOpacity,
                            onBgOpacityChange = { scope.launch { SettingsStore.saveBgOpacity(context, it) } },
                            savedColors = savedColors,
                            onSavedColorsChange = { scope.launch { SettingsStore.saveSavedColors(context, it) } },
                            textSizeMultiplier = textSizeMultiplier,
                            onTextSizeChange = { scope.launch { SettingsStore.saveTextSize(context, it) } },
                            isSimplifiedUI = isSimplifiedUI,
                            onSimplifiedUIChange = { scope.launch { SettingsStore.saveSimplifiedUi(context, it) } },
                            lightMode = lightMode,
                            onLightModeChange = { scope.launch { SettingsStore.saveLightMode(context, it) } },
                            boldText = boldText,
                            onBoldTextChange = { scope.launch { SettingsStore.saveBoldText(context, it) } },
                            reduceMotion = reduceMotion,
                            onReduceMotionChange = { scope.launch { SettingsStore.saveReduceMotion(context, it) } },
                            notifEnabled = notificationsEnabled,
                            onNotifEnabledChange = { v ->
                                scope.launch { SettingsStore.saveNotificationsEnabled(context, v) }
                                val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@SettingsScreen
                                db.collection("users").document(uid).update("notifEnabled", v)
                            },
                            notifMessages = notifMessages,
                            onNotifMessagesChange = { v ->
                                scope.launch { SettingsStore.saveNotifMessages(context, v) }
                                val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@SettingsScreen
                                db.collection("users").document(uid).update("notifMessages", v)
                            },
                            notifGroupMessages = notifGroupMessages,
                            onNotifGroupMessagesChange = { v ->
                                scope.launch { SettingsStore.saveNotifGroupMessages(context, v) }
                                val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@SettingsScreen
                                db.collection("users").document(uid).update("notifGroupMessages", v)
                            },
                            notifConnectionRequests = notifConnectionRequests,
                            onNotifConnectionRequestsChange = { v ->
                                scope.launch { SettingsStore.saveNotifConnectionRequests(context, v) }
                                val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@SettingsScreen
                                db.collection("users").document(uid).update("notifConnectionRequests", v)
                            },
                            notifConnectionAccepted = notifConnectionAccepted,
                            onNotifConnectionAcceptedChange = { v ->
                                scope.launch { SettingsStore.saveNotifConnectionAccepted(context, v) }
                                val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@SettingsScreen
                                db.collection("users").document(uid).update("notifConnectionAccepted", v)
                            },
                            notifEventReminders = notifEventReminders,
                            onNotifEventRemindersChange = { v ->
                                scope.launch { SettingsStore.saveNotifEventReminders(context, v) }
                                val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@SettingsScreen
                                db.collection("users").document(uid).update("notifEventReminders", v)
                            },
                            hideConnections = hideConnections,
                            onHideConnectionsChange = { v ->
                                hideConnections = v
                                val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@SettingsScreen
                                db.collection("users").document(uid).update("hideConnections", v)
                            },
                            allowPeerSharing = allowPeerSharing,
                            onAllowPeerSharingChange = { v ->
                                allowPeerSharing = v
                                val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@SettingsScreen
                                db.collection("users").document(uid).update("allowPeerSharing", v)
                            },
                            onDeleteAccount = {
                                val db = FirebaseFirestore.getInstance()
                                val auth = FirebaseAuth.getInstance()
                                val uid = auth.currentUser?.uid
                                if (uid != null) {
                                    db.collection("users").document(uid).delete()
                                    db.collection("connections").document(uid).delete()
                                    auth.currentUser?.delete()
                                    currentSubScreen = null
                                }
                            }
                        )
                        screenId == "dress_code" -> DressCodeCheckerScreen(onBack = { currentSubScreen = null }, primaryColor = effectivePrimaryColor, bgOpacity = effectiveBgOpacity, textSizeMultiplier = textSizeMultiplier, lightMode = lightMode)
                        screenId == "find_links" -> MemberSearchScreen(onBack = { currentSubScreen = null }, primaryColor = effectivePrimaryColor, bgOpacity = effectiveBgOpacity, isSimplifiedUI = isSimplifiedUI, textSizeMultiplier = textSizeMultiplier, lightMode = lightMode)
                        screenId == "connection_requests" -> ConnectionRequestsScreen(onBack = { currentSubScreen = null }, primaryColor = effectivePrimaryColor, bgOpacity = effectiveBgOpacity, isSimplifiedUI = isSimplifiedUI, textSizeMultiplier = textSizeMultiplier, lightMode = lightMode)
                        screenId == "my_connections" -> MyConnectionsScreen(onBack = { currentSubScreen = null }, primaryColor = effectivePrimaryColor, bgOpacity = effectiveBgOpacity, isSimplifiedUI = isSimplifiedUI, textSizeMultiplier = textSizeMultiplier, lightMode = lightMode)
                        screenId == "messages" -> MessagesScreen(
                            primaryColor = effectivePrimaryColor,
                            bgOpacity = effectiveBgOpacity,
                            isSimplifiedUI = isSimplifiedUI,
                            textSizeMultiplier = textSizeMultiplier,
                            lightMode = lightMode,
                            onBack = { currentSubScreen = null },
                            onOpenChat = { userId, _ -> currentSubScreen = "chat_$userId" },
                            onOpenGroup = { groupId -> currentSubScreen = "group_$groupId" }
                        )
                        screenId is String && screenId.startsWith("group_") -> {
                            val groupId = screenId.removePrefix("group_")
                            GroupChatScreen(
                                groupId = groupId,
                                onBack = { currentSubScreen = "messages" },
                                primaryColor = effectivePrimaryColor,
                                bgOpacity = effectiveBgOpacity,
                                isSimplifiedUI = isSimplifiedUI,
                                textSizeMultiplier = textSizeMultiplier,
                                lightMode = lightMode
                            )
                        }
                        screenId == "classrooms" -> ClassroomsScreen(
                            primaryColor = effectivePrimaryColor,
                            bgOpacity = effectiveBgOpacity,
                            isSimplifiedUI = isSimplifiedUI,
                            textSizeMultiplier = textSizeMultiplier,
                            lightMode = lightMode,
                            onBack = { currentSubScreen = null },
                            onOpenClassroom = { classId -> currentSubScreen = "classroom_chat_$classId" }
                        )
                        screenId is String && screenId.startsWith("classroom_chat_") -> {
                            val classId = screenId.removePrefix("classroom_chat_")
                            ClassroomChatScreen(
                                classroomId = classId,
                                onBack = { currentSubScreen = "classrooms" },
                                primaryColor = effectivePrimaryColor,
                                bgOpacity = effectiveBgOpacity,
                                isSimplifiedUI = isSimplifiedUI,
                                textSizeMultiplier = textSizeMultiplier,
                                lightMode = lightMode
                            )
                        }
                        screenId is String && screenId.startsWith("chat_") -> {
                            val otherUserId = screenId.removePrefix("chat_")
                            ChatScreen(
                                otherUserId = otherUserId,
                                onBack = { currentSubScreen = null },
                                primaryColor = effectivePrimaryColor,
                                bgOpacity = effectiveBgOpacity,
                                isSimplifiedUI = isSimplifiedUI,
                                textSizeMultiplier = textSizeMultiplier,
                                lightMode = lightMode
                            )
                        }
                    }
                } else {
                    when (screenId as Int) {
                        0 -> {
                            if (currentUser == null) {
                                if (showAuthFlow) {
                                    AuthScreen(
                                        onLoginSuccess = {
                                            currentUser = auth.currentUser
                                            showAuthFlow = false
                                        },
                                        onBack = { showAuthFlow = false },
                                        lightMode = lightMode,
                                        primaryColor = effectivePrimaryColor,
                                        bgOpacity = effectiveBgOpacity
                                    )
                                } else {
                                    LoggedOutProfileScreen(onLoginClick = { showAuthFlow = true }, onTutorialClick = { showTutorial = true }, onSettingsClick = { currentSubScreen = "settings" }, primaryColor = effectivePrimaryColor, bgOpacity = effectiveBgOpacity, isSimplifiedUI = isSimplifiedUI, lightMode = lightMode)
                                }
                            } else {
                                ProfileScreen(
                                    onLogout = {
                                        auth.signOut()
                                        currentUser = null
                                    },
                                    onTutorialClick = { showTutorial = true },
                                    onSettingsClick = { currentSubScreen = "settings" },
                                    onFindLinksClick = { currentSubScreen = "find_links" },
                                    onConnectionRequestsClick = { currentSubScreen = "connection_requests" },
                                    onMyConnectionsClick = { currentSubScreen = "my_connections" },
                                    onMessagesClick = { currentSubScreen = "messages" },
                                    onClassroomsClick = { currentSubScreen = "classrooms" },
                                    onDressCodeClick = { currentSubScreen = "dress_code" },
                                    primaryColor = effectivePrimaryColor,
                                    bgOpacity = effectiveBgOpacity,
                                    textSizeMultiplier = textSizeMultiplier,
                                    isSimplifiedUI = isSimplifiedUI,
                                    lightMode = lightMode
                                )
                            }
                        }
                        1 -> EventsPlaceholder(effectivePrimary, effectiveBgOpacity, isSimplifiedUI, textSizeMultiplier, lightMode, userState)
                        2 -> HomeScreen(
                            primaryColor = effectivePrimaryColor,
                            bgOpacity = effectiveBgOpacity,
                            isSimplifiedUI = isSimplifiedUI,
                            textSizeMultiplier = textSizeMultiplier,
                            lightMode = lightMode,
                            onNavigate = { tab, subScreen ->
                                selectedTab = tab
                                currentSubScreen = subScreen
                            }
                        )
                        3 -> NewsScreen(effectivePrimary, effectiveBgOpacity, isSimplifiedUI, textSizeMultiplier, lightMode)
                        4 -> ResourcesScreen(effectivePrimary, effectiveBgOpacity, isSimplifiedUI, textSizeMultiplier, lightMode, userState, userChapter, userInstagram)
                    }
                }
            }
        }
    }
    } // end MaterialTheme
    } // end CompositionLocalProvider

    // Spotlight tutorial overlay — shown on top of the live app UI
    if (showTutorial) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = {},
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            )
        ) {
            SpotlightTutorialOverlay(
                onDismiss = { showTutorial = false },
                primaryColor = primaryColor,
                lightMode = lightMode,
                navBarBounds = navBarBounds // kept for signature compat, unused now
            )
        }
    }

// Share-receive dialog — shown on top of everything when another app shares to FBLA Hub
    if (showShareReceive) {
        val textColor = if (lightMode) Color(0xFF1A1A2E) else Color.White
        val textColorSecondary = if (lightMode) Color(0xFF1A1A2E).copy(alpha = 0.7f) else Color.White.copy(alpha = 0.75f)
        val storage = FirebaseStorage.getInstance()
        Dialog(onDismissRequest = {
            shareReceiveText = null; shareReceiveUri = null
            shareReceiveConnections = emptyList(); shareReceiveLoadingConnections = false
        }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = if (lightMode) Color.White else Color(0xFF1A2744),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Send to FBLA Hub", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = textColor)

// Preview of what's being shared
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = effectivePrimary.copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, effectivePrimary.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (shareReceiveUri != null) Icons.Default.Image else Icons.Default.Link,
                                contentDescription = null, tint = effectivePrimary, modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            if (shareReceiveUri != null) {
                                AsyncImage(model = shareReceiveUri, contentDescription = null, contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)))
                            } else {
                                Text(
                                    text = shareReceiveText ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = textColorSecondary,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Text("Choose a member:", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold), color = textColorSecondary)

                    if (shareReceiveUploading) {
                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                CircularProgressIndicator(color = effectivePrimary)
                                Text("Sending...", style = MaterialTheme.typography.bodySmall, color = textColorSecondary)
                            }
                        }
                    } else if (shareReceiveLoadingConnections) {
                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = effectivePrimary, modifier = Modifier.size(28.dp))
                        }
                    } else if (shareReceiveConnections.isEmpty()) {
                        Text("No connections yet.", style = MaterialTheme.typography.bodyMedium, color = textColorSecondary)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            shareReceiveConnections.forEach { member ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (lightMode) Color(0xFFF0F4FF) else Color.White.copy(alpha = 0.07f),
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        val uid = auth.currentUser?.uid ?: return@clickable
                                        val userName = auth.currentUser?.displayName ?: ""
                                        val uri = shareReceiveUri
                                        val text = shareReceiveText
                                        if (uri != null) {
                                            shareReceiveUploading = true
                                            scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                                try {
                                                    val bytes = context.contentResolver.openInputStream(uri)?.readBytes() ?: return@launch
                                                    val ref = storage.reference.child("shared_media/$uid/${System.currentTimeMillis()}.jpg")
                                                    ref.putBytes(bytes).addOnSuccessListener {
                                                        ref.downloadUrl.addOnSuccessListener { downloadUri ->
                                                            val msgId = db.collection("messages").document().id
                                                            val msg = ChatMessage(id = msgId, senderId = uid, receiverId = member.uid, message = "", timestamp = System.currentTimeMillis(), senderName = userName, attachmentUrl = downloadUri.toString(), attachmentName = "shared_image.jpg", attachmentType = "image")
                                                            db.collection("messages").document(msgId).set(msg)
                                                            shareReceiveUploading = false
                                                            shareReceiveText = null; shareReceiveUri = null; shareReceiveConnections = emptyList()
                                                            Toast.makeText(context, "Sent to ${member.name}", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }.addOnFailureListener { shareReceiveUploading = false }
                                                } catch (e: Exception) { shareReceiveUploading = false }
                                            }
                                        } else if (text != null) {
                                            val msgId = db.collection("messages").document().id
                                            val url = extractUrl(text)
                                            val msg = if (url != null) {
                                                ChatMessage(id = msgId, senderId = uid, receiverId = member.uid, message = text, timestamp = System.currentTimeMillis(), senderName = userName, attachmentType = "link", attachmentUrl = url, attachmentName = urlDomain(url))
                                            } else {
                                                ChatMessage(id = msgId, senderId = uid, receiverId = member.uid, message = text, timestamp = System.currentTimeMillis(), senderName = userName)
                                            }
                                            db.collection("messages").document(msgId).set(msg)
                                            shareReceiveText = null; shareReceiveUri = null; shareReceiveConnections = emptyList()
                                            Toast.makeText(context, "Sent to ${member.name}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
                                        if (member.pfpUrl.isNotEmpty()) {
                                            AsyncImage(model = member.pfpUrl, contentDescription = null, contentScale = ContentScale.Crop,
                                                modifier = Modifier.size(38.dp).clip(CircleShape))
                                        } else {
                                            Box(modifier = Modifier.size(38.dp).clip(CircleShape).background(effectivePrimary), contentAlignment = Alignment.Center) {
                                                Text(member.name.take(1).uppercase(), color = Color.Black, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(member.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = textColor)
                                            if (member.school.isNotEmpty()) Text(member.school, style = MaterialTheme.typography.bodySmall, color = textColorSecondary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun extractUrl(text: String): String? =
    Regex("https?://[^\\s]+").find(text)?.value

fun urlDomain(url: String): String = try {
    java.net.URI(url).host?.removePrefix("www.") ?: url
} catch (e: Exception) { url }
