//fix15
package com.example.myapplication

import android.app.NotificationChannel
import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

private val emailJsClient = OkHttpClient()

suspend fun sendVerificationEmail(toEmail: String, code: String) = withContext(Dispatchers.IO) {
    val body = """{"service_id":"service_cbcdanj","template_id":"template_soygwa9","user_id":"wCNL4V8dkOjhd1fq4","template_params":{"email":"$toEmail","passcode":"$code","time":"5 minutes"}}"""
        .toRequestBody("application/json".toMediaType())
    val request = Request.Builder()
        .url("https://api.emailjs.com/api/v1.0/email/send")
        .post(body)
        .build()
    emailJsClient.newCall(request).execute().use { response ->
        val body = response.body?.string() ?: ""
        android.util.Log.d("EmailJS", "Response ${response.code}: $body")
        if (!response.isSuccessful) throw Exception("EmailJS ${response.code}: $body")
    }
}

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
            val channelId = "chat_messages"
            val name = "Chat Messages"
            val descriptionText = "Notifications for new chat messages"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
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
    val role: String = "student"
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
    val linkedin: String = ""
)

data class ChatMessage(
    val id: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val senderName: String = ""
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
    val timestamp: Long = 0L
)

fun normalizeChapterKey(chapter: String): String {
    var s = chapter.trim().lowercase(Locale.getDefault())
    if (s.endsWith(" fbla")) s = s.removeSuffix(" fbla").trim()
    return s
}

fun randomClassroomInviteCode(): String {
    val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    return buildString(8) { repeat(8) { append(chars.random()) } }
}

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onTutorialClick: () -> Unit,
    primaryColor: Color,
    onPrimaryColorChange: (Color) -> Unit,
    bgOpacity: Float,
    onBgOpacityChange: (Float) -> Unit,
    savedColors: List<Int>,
    onSavedColorsChange: (List<Int>) -> Unit,
    textSizeMultiplier: Float,
    onTextSizeChange: (Float) -> Unit = {},
    isSimplifiedUI: Boolean,
    onSimplifiedUIChange: (Boolean) -> Unit,
    lightMode: Boolean = false,
    onLightModeChange: (Boolean) -> Unit = {},
    boldText: Boolean = false,
    onBoldTextChange: (Boolean) -> Unit = {},
    highContrast: Boolean = false,
    onHighContrastChange: (Boolean) -> Unit = {},
    reduceMotion: Boolean = false,
    onReduceMotionChange: (Boolean) -> Unit = {},
    onDeleteAccount: () -> Unit
) {
    val context = LocalContext.current
    var showColorPicker by remember { mutableStateOf(false) }
    var pickerHue by remember { mutableFloatStateOf(45f) }
    var pickerSaturation by remember { mutableFloatStateOf(1f) }
    var pickerValue by remember { mutableFloatStateOf(1f) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deleteConfirmText by remember { mutableStateOf("") }
    var showAboutDialog by remember { mutableStateOf(false) }

    val textColor = if (lightMode) Color(0xFF1A1A2E) else Color.White
    val textColorSecondary = if (lightMode) Color(0xFF1A1A2E).copy(alpha = 0.8f) else Color.White.copy(alpha = 0.9f)
    val textColorTertiary = if (lightMode) Color(0xFF1A1A2E).copy(alpha = 0.75f) else Color.White.copy(alpha = 0.85f)

    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize().background(if (lightMode) Color.White else Color(0xFF0A1628)))
        if (!lightMode) Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = bgOpacity)))

        Column(modifier = Modifier.fillMaxSize()) {
// Top bar
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 48.dp, start = 0.dp, end = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = textColor)
                }
                Text("Settings", color = textColor, fontWeight = FontWeight.Black, fontSize = 22.sp)
            }

            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)) {
                item {
// ── VISUALS ──────────────────────────────────────────
                    Text("VISUALS", style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 2.sp, fontWeight = FontWeight.Bold), color = primaryColor, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(16.dp))
                    SettingToggle(
                        title = "Light Mode",
                        subtitle = "Switch to a light colour scheme",
                        icon = Icons.Default.LightMode,
                        checked = lightMode,
                        onCheckedChange = onLightModeChange,
                        primaryColor = primaryColor,
                        textSizeMultiplier = textSizeMultiplier,
                        lightMode = lightMode
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Background Darkness", color = textColor, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.fillMaxWidth())
                    Slider(value = bgOpacity, onValueChange = onBgOpacityChange, valueRange = 0.0f..0.95f, colors = SliderDefaults.colors(thumbColor = primaryColor, activeTrackColor = primaryColor))
                    Text("Slide RIGHT to darken", color = textColorTertiary, style = MaterialTheme.typography.bodySmall, modifier = Modifier.fillMaxWidth())

                    Spacer(modifier = Modifier.height(32.dp))
                    HorizontalDivider(color = textColor.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(24.dp))

// ── THEME COLORS ─────────────────────────────────────
                    Text("THEME COLORS", style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 2.sp, fontWeight = FontWeight.Bold), color = primaryColor, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Tap to apply • Long press to remove", color = textColorSecondary, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                        Text("${savedColors.size}/5", color = primaryColor, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                    }
                    Spacer(modifier = Modifier.height(16.dp))
// Color swatches — swatches in a row, add button always at end, never duplicated
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        savedColors.forEachIndexed { index, colorInt ->
                            val color = Color(colorInt)
                            val isSelected = color == primaryColor
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .shadow(if (isSelected) 6.dp else 2.dp, CircleShape)
                                    .background(color, CircleShape)
                                    .border(if (isSelected) 3.dp else 0.dp, Color.White, CircleShape)
                                    .pointerInput(colorInt) {
                                        detectTapGestures(
                                            onTap = { onPrimaryColorChange(color) },
                                            onLongPress = {
                                                if (index >= 2) {
                                                    val newColors = savedColors.toMutableList()
                                                    newColors.removeAt(index)
                                                    onSavedColorsChange(newColors)
                                                    if (isSelected) {
                                                        onPrimaryColorChange(Color(0xFFFFB800))
                                                    }
                                                    Toast.makeText(context, "Color removed", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    Toast.makeText(context, "Can't remove default colors", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    val luminance = 0.299f * color.red + 0.587f * color.green + 0.114f * color.blue
                                    Icon(Icons.Default.Check, null, tint = if (luminance > 0.5f) Color.Black else Color.White, modifier = Modifier.size(24.dp))
                                }
                            }
                        }
                        // Single add button — only shown when under 5 colors
                        if (savedColors.size < 5) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .shadow(2.dp, CircleShape)
                                    .background(if (lightMode) Color(0xFFE8EDF5) else Color(0xFF333333), CircleShape)
                                    .border(1.dp, Color.White.copy(alpha = 0.7f), CircleShape)
                                    .clickable { showColorPicker = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Add, null, tint = if (lightMode) Color(0xFF1565C0) else Color.White, modifier = Modifier.size(24.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                    HorizontalDivider(color = textColor.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(24.dp))

// ── ACCESSIBILITY ─────────────────────────────────────
                    Text("ACCESSIBILITY", style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 2.sp, fontWeight = FontWeight.Bold), color = primaryColor, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.FormatSize, contentDescription = null, tint = primaryColor, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Text & Button Size", color = textColor, style = MaterialTheme.typography.bodyLarge)
                            Text("Adjust to improve readability", color = textColorTertiary, style = MaterialTheme.typography.bodySmall)
                        }
                        Text(
                            when {
                                textSizeMultiplier < 0.9f -> "Small"
                                textSizeMultiplier < 1.05f -> "Normal"
                                textSizeMultiplier < 1.2f -> "Large"
                                else -> "X-Large"
                            },
                            color = primaryColor,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    Slider(
                        value = textSizeMultiplier,
                        onValueChange = onTextSizeChange,
                        valueRange = 0.8f..1.4f,
                        steps = 5,
                        colors = SliderDefaults.colors(thumbColor = primaryColor, activeTrackColor = primaryColor)
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("A", color = textColorTertiary, fontSize = 11.sp)
                        Text("A", color = textColorTertiary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    SettingToggle(
                        title = "Bold Text",
                        subtitle = "Makes all text heavier and easier to read",
                        icon = Icons.Default.FormatBold,
                        checked = boldText,
                        onCheckedChange = onBoldTextChange,
                        primaryColor = primaryColor,
                        textSizeMultiplier = textSizeMultiplier,
                        lightMode = lightMode
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    SettingToggle(
                        title = "High Contrast",
                        subtitle = "Increases contrast between text and backgrounds",
                        icon = Icons.Default.Contrast,
                        checked = highContrast,
                        onCheckedChange = onHighContrastChange,
                        primaryColor = primaryColor,
                        textSizeMultiplier = textSizeMultiplier,
                        lightMode = lightMode
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    SettingToggle(
                        title = "Reduce Motion",
                        subtitle = "Minimizes animations and transitions",
                        icon = Icons.Default.Animation,
                        checked = reduceMotion,
                        onCheckedChange = onReduceMotionChange,
                        primaryColor = primaryColor,
                        textSizeMultiplier = textSizeMultiplier,
                        lightMode = lightMode
                    )

                    Spacer(modifier = Modifier.height(32.dp))
                    HorizontalDivider(color = textColor.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(24.dp))

                    Text("ACCOUNT", style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 2.sp, fontWeight = FontWeight.Bold), color = primaryColor, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(onClick = { showAboutDialog = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("About FBLA Hub v1.0", color = textColorTertiary, style = MaterialTheme.typography.bodySmall)
                    }
                    TextButton(onClick = { showDeleteDialog = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Delete Account", color = Color.Red, style = MaterialTheme.typography.bodySmall)
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                    HorizontalDivider(color = textColor.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(24.dp))

// ── TUTORIAL ──────────────────────────────────────────
                    Text("HELP", style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 2.sp, fontWeight = FontWeight.Bold), color = primaryColor, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = onTutorialClick,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = primaryColor),
                        border = BorderStroke(1.5.dp, primaryColor.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("TUTORIAL", fontWeight = FontWeight.Black, fontSize = 13.sp, letterSpacing = 1.sp)
                    }
                    Spacer(modifier = Modifier.height(48.dp))
                }
            }
        }
    }

// Color Picker Dialog
    if (showColorPicker) {
        LaunchedEffect(showColorPicker) {
            if (showColorPicker) {
                val hsv = FloatArray(3)
                android.graphics.Color.colorToHSV(primaryColor.toArgb(), hsv)
                pickerHue = hsv[0]; pickerSaturation = hsv[1]; pickerValue = hsv[2]
            }
        }
        AlertDialog(
            onDismissRequest = { showColorPicker = false },
            title = { Text("Pick a Color", color = textColor, fontWeight = FontWeight.Bold) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(modifier = Modifier.size(280.dp), contentAlignment = Alignment.Center) {
                        Canvas(modifier = Modifier.size(280.dp).pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    val cx = size.width/2f; val cy = size.height/2f
                                    val dx = offset.x-cx; val dy = offset.y-cy
                                    val dist = kotlin.math.sqrt(dx*dx+dy*dy)
                                    val outerR = size.width/2f; val innerR = outerR*0.65f; val sqSz = innerR*1.4f
                                    if (dist >= innerR && dist <= outerR) { var a = kotlin.math.atan2(dy,dx)*180f/Math.PI.toFloat(); if (a<0) a+=360f; pickerHue=a }
                                    else if (kotlin.math.abs(dx)<=sqSz/2f && kotlin.math.abs(dy)<=sqSz/2f) { pickerSaturation=((dx+sqSz/2f)/sqSz).coerceIn(0f,1f); pickerValue=(1f-(dy+sqSz/2f)/sqSz).coerceIn(0f,1f) }
                                },
                                onDrag = { change, _ ->
                                    val cx = size.width/2f; val cy = size.height/2f
                                    val dx = change.position.x-cx; val dy = change.position.y-cy
                                    val dist = kotlin.math.sqrt(dx*dx+dy*dy)
                                    val outerR = size.width/2f; val innerR = outerR*0.65f; val sqSz = innerR*1.4f
                                    if (dist >= innerR && dist <= outerR) { var a = kotlin.math.atan2(dy,dx)*180f/Math.PI.toFloat(); if (a<0) a+=360f; pickerHue=a }
                                    else if (kotlin.math.abs(dx)<=sqSz/2f && kotlin.math.abs(dy)<=sqSz/2f) { pickerSaturation=((dx+sqSz/2f)/sqSz).coerceIn(0f,1f); pickerValue=(1f-(dy+sqSz/2f)/sqSz).coerceIn(0f,1f) }
                                }
                            )
                        }) {
                            val cx=size.width/2f; val cy=size.height/2f; val outerR=size.width/2f; val innerR=outerR*0.65f; val ring=outerR-innerR
                            for (angle in 0 until 360) { val hc=android.graphics.Color.HSVToColor(floatArrayOf(angle.toFloat(),1f,1f)); drawArc(color=Color(hc),startAngle=angle.toFloat(),sweepAngle=1f,useCenter=false,topLeft=androidx.compose.ui.geometry.Offset(cx-outerR,cy-outerR),size=androidx.compose.ui.geometry.Size(outerR*2,outerR*2),style=androidx.compose.ui.graphics.drawscope.Stroke(width=ring)) }
                            val sqSz=innerR*1.4f; val sqL=cx-sqSz/2f; val sqT=cy-sqSz/2f
                            for (x in 0 until sqSz.toInt() step 3) for (y in 0 until sqSz.toInt() step 3) { val c=android.graphics.Color.HSVToColor(floatArrayOf(pickerHue,x/sqSz,1f-(y/sqSz))); drawCircle(color=Color(c),radius=3f,center=androidx.compose.ui.geometry.Offset(sqL+x,sqT+y)) }
                            val hRad=pickerHue*Math.PI.toFloat()/180f; val hR=(innerR+outerR)/2f; val hPos=androidx.compose.ui.geometry.Offset(cx+hR*kotlin.math.cos(hRad),cy+hR*kotlin.math.sin(hRad))
                            drawCircle(color=Color.White,radius=10f,center=hPos,style=androidx.compose.ui.graphics.drawscope.Stroke(width=3f))
                            val sPos=androidx.compose.ui.geometry.Offset(sqL+pickerSaturation*sqSz,sqT+(1f-pickerValue)*sqSz)
                            drawCircle(color=Color.White,radius=8f,center=sPos,style=androidx.compose.ui.graphics.drawscope.Stroke(width=3f))
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    val selColor = android.graphics.Color.HSVToColor(floatArrayOf(pickerHue, pickerSaturation, pickerValue))
                    Box(modifier = Modifier.fillMaxWidth().height(60.dp).background(Color(selColor), RoundedCornerShape(12.dp)).border(2.dp, Color.White.copy(alpha = 0.7f), RoundedCornerShape(12.dp)))
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val newColor = android.graphics.Color.HSVToColor(floatArrayOf(pickerHue, pickerSaturation, pickerValue))
                    if (savedColors.size < 5 && !savedColors.contains(newColor)) onSavedColorsChange(savedColors + newColor)
                    onPrimaryColorChange(Color(newColor)); showColorPicker = false
                }) { Text("SAVE COLOR", color = primaryColor, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { showColorPicker = false }) { Text("CANCEL", color = textColorSecondary) } },
            containerColor = if (lightMode) Color.White else Color(0xFF1A1A1A)
        )
    }

// Delete Account Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false; deleteConfirmText = "" },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, null, tint = Color(0xFFDC143C), modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Delete Account", color = Color(0xFFDC143C), fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text("This action is PERMANENT. All your data will be deleted.", color = textColor, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Type 'DELETE' to confirm:", color = textColor, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = deleteConfirmText, onValueChange = { deleteConfirmText = it.uppercase() },
                        colors = OutlinedTextFieldDefaults.colors(unfocusedTextColor = textColor, focusedTextColor = textColor, focusedBorderColor = Color(0xFFDC143C)), singleLine = true)
                }
            },
            confirmButton = {
                Button(
                    onClick = { if (deleteConfirmText == "DELETE") { onDeleteAccount(); showDeleteDialog = false; deleteConfirmText = "" } else Toast.makeText(context, "Type DELETE to confirm", Toast.LENGTH_SHORT).show() },
                    enabled = deleteConfirmText == "DELETE",
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC143C))
                ) { Text("DELETE ACCOUNT", color = Color.White, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false; deleteConfirmText = "" }) { Text("CANCEL", color = textColorSecondary) } },
            containerColor = if (lightMode) Color.White else Color(0xFF1A1A1A)
        )
    }

// About Dialog
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, null, tint = primaryColor, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("About FBLA Hub", color = textColor, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text("FBLA Hub v1.0", color = primaryColor, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), modifier = Modifier.padding(bottom = 16.dp))
                    Text("The premier networking platform for FBLA members.", color = textColor, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 12.dp))
                    Text("Built with passion for the FBLA community.", color = textColorSecondary, style = MaterialTheme.typography.bodySmall.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic))
                }
            },
            confirmButton = { TextButton(onClick = { showAboutDialog = false }) { Text("GOT IT", color = primaryColor, fontWeight = FontWeight.Bold) } },
            containerColor = if (lightMode) Color.White else Color(0xFF1A1A1A)
        )
    }
}


@Composable
fun TutorialScreen(onDismiss: () -> Unit, primaryColor: Color = Color(0xFFFFB800), lightMode: Boolean = false) {
    var currentPage by remember { mutableIntStateOf(0) }

    data class TutorialPage(
        val title: String,
        val description: String,
        val emoji: String,
        val mockup: @Composable () -> Unit
    )

    val pages = listOf(
        TutorialPage(
            title = "Your Profile",
            description = "Set up your name, chapter, graduation year, state, and bio. Add a photo and link your social media so connections can find you.",
            emoji = "👤",
            mockup = {
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(modifier = Modifier.size(56.dp).background(primaryColor, CircleShape), contentAlignment = Alignment.Center) {
                        Text("T", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(modifier = Modifier.width(120.dp).height(10.dp).background(Color.White.copy(alpha = 0.7f), RoundedCornerShape(5.dp)))
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(modifier = Modifier.width(80.dp).height(8.dp).background(primaryColor.copy(alpha = 0.7f), RoundedCornerShape(4.dp)))
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(36.dp).background(primaryColor, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                        Text("EDIT PROFILE", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        ),
        TutorialPage(
            title = "Find & Connect",
            description = "Search for FBLA members by name. View their chapter, competitions, and bio — then send a connection request to link up.",
            emoji = "🔗",
            mockup = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.fillMaxWidth().height(36.dp).background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp)), contentAlignment = Alignment.CenterStart) {
                        Row(modifier = Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Search, null, tint = primaryColor, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Search members...", color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    repeat(2) { i ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(8.dp)).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(32.dp).background(primaryColor.copy(alpha = if (i == 0) 1f else 0.6f), CircleShape), contentAlignment = Alignment.Center) {
                                Text(if (i == 0) "A" else "B", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Box(modifier = Modifier.width(80.dp).height(8.dp).background(Color.White.copy(alpha = 0.7f), RoundedCornerShape(4.dp)))
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(modifier = Modifier.width(60.dp).height(6.dp).background(primaryColor.copy(alpha = 0.5f), RoundedCornerShape(3.dp)))
                            }
                            Box(modifier = Modifier.background(primaryColor, RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                                Text("Connect", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        ),
        TutorialPage(
            title = "Messages",
            description = "Chat in real time with your connections. Messages are instant — tap any connection to open a conversation.",
            emoji = "💬",
            mockup = {
                Column(modifier = Modifier.fillMaxWidth()) {
// Received message
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                        Box(modifier = Modifier.size(28.dp).background(primaryColor.copy(alpha = 0.6f), CircleShape))
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(modifier = Modifier.background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomEnd = 12.dp)).padding(horizontal = 12.dp, vertical = 8.dp)) {
                            Text("Hey! Are you going to SLC?", color = Color.White, fontSize = 11.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
// Sent message
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Box(modifier = Modifier.background(primaryColor, RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 12.dp)).padding(horizontal = 12.dp, vertical = 8.dp)) {
                            Text("Yes! See you there 👋", color = Color.Black, fontSize = 11.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
// Input bar
                    Row(modifier = Modifier.fillMaxWidth().background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp)).padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Type a message...", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp, modifier = Modifier.weight(1f))
                        Box(modifier = Modifier.size(28.dp).background(primaryColor, CircleShape), contentAlignment = Alignment.Center) {
                            Icon(Icons.AutoMirrored.Filled.Send, null, tint = Color.Black, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        ),
        TutorialPage(
            title = "Events & Calendar",
            description = "Browse FBLA competitions by state. Tap any date on the calendar to filter events, and set reminders for the ones you're entering.",
            emoji = "📅",
            mockup = {
                Column(modifier = Modifier.fillMaxWidth()) {
// Mini calendar grid
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        listOf("S","M","T","W","T","F","S").forEach {
                            Text(it, color = primaryColor, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    listOf(listOf("","1","2","3","4","5","6"), listOf("7","8","9","10","11","12","13")).forEach { week ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            week.forEach { day ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .padding(2.dp)
                                        .background(if (day == "10") primaryColor else Color.Transparent, RoundedCornerShape(4.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (day.isNotEmpty()) Text(day, color = if (day == "10") Color.Black else Color.White, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(modifier = Modifier.fillMaxWidth().background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp)).padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.EmojiEvents, null, tint = primaryColor, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("State Leadership Conference", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text("April 10 • Green Bay, WI", color = Color.White.copy(alpha = 0.85f), fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        ),
        TutorialPage(
            title = "News Feed",
            description = "Stay up to date with live articles pulled straight from FBLA.org. Articles are auto-tagged by category and you can share any article in one tap.",
            emoji = "📰",
            mockup = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    repeat(2) { i ->
                        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(10.dp)).padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.background(primaryColor.copy(alpha = 0.2f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                    Text(if (i == 0) "NATIONAL" else "CONFERENCE", color = primaryColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.weight(1f))
                                Text("Jan ${if (i == 0) "26" else "20"}, 2026", color = Color.White.copy(alpha = 0.75f), fontSize = 9.sp)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(modifier = Modifier.width(if (i == 0) 160.dp else 130.dp).height(8.dp).background(Color.White.copy(alpha = 0.7f), RoundedCornerShape(4.dp)))
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(modifier = Modifier.fillMaxWidth().height(6.dp).background(Color.White.copy(alpha = 0.7f), RoundedCornerShape(3.dp)))
                        }
                    }
                }
            }
        ),
        TutorialPage(
            title = "Resources",
            description = "Quick access to official FBLA guidelines, toolkits, brand assets, and dress code. Also find national and your state chapter's social media links.",
            emoji = "📚",
            mockup = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Guidelines" to Icons.AutoMirrored.Filled.Assignment, "Toolkit" to Icons.AutoMirrored.Filled.LibraryBooks).forEach { (label, icon) ->
                            Box(modifier = Modifier.weight(1f).height(72.dp).background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(icon, null, tint = primaryColor, modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(label, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    listOf("Instagram" to "📸", "X (Twitter)" to "✕").forEach { (name, icon) ->
                        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp).background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(8.dp)).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(icon, fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                            Icon(Icons.Default.OpenInNew, null, tint = primaryColor, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        ),
        TutorialPage(
            title = "Customize",
            description = "Make FBLA Hub yours. Change the accent color with a full color picker, adjust background darkness, scale text size, or enable simplified UI.",
            emoji = "🎨",
            mockup = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("THEME COLOR", color = primaryColor, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(Color(0xFFFFB800), Color(0xFF4CAF50), Color(0xFF2196F3), Color(0xFFE91E63), Color(0xFF9C27B0)).forEach { c ->
                            Box(modifier = Modifier.size(32.dp).background(c, CircleShape).border(if (c == primaryColor) 2.dp else 0.dp, Color.White, CircleShape))
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("BACKGROUND DARKNESS", color = primaryColor, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(6.dp).background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(3.dp))) {
                        Box(modifier = Modifier.fillMaxWidth(0.7f).height(6.dp).background(primaryColor, RoundedCornerShape(3.dp)))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth().background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(8.dp)).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Simplified UI", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("Hide background images", color = Color.White.copy(alpha = 0.85f), fontSize = 10.sp)
                        }
                        Box(modifier = Modifier.width(36.dp).height(20.dp).background(primaryColor.copy(alpha = 0.3f), RoundedCornerShape(10.dp)).border(1.dp, primaryColor, RoundedCornerShape(10.dp)))
                    }
                }
            }
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0A1628))) {
        Column(modifier = Modifier.fillMaxSize()) {
// Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .statusBarsPadding(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "FBLA Hub Tour",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onDismiss) {
                    Text("Skip", color = Color.White.copy(alpha = 0.85f), fontSize = 14.sp)
                }
            }

// Page indicator dots
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                pages.forEachIndexed { i, _ ->
                    val isSelected = i == pagerState.currentPage
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .size(if (isSelected) 24.dp else 8.dp, 8.dp)
                            .background(
                                if (isSelected) primaryColor else Color.White.copy(alpha = 0.5f),
                                RoundedCornerShape(4.dp)
                            )
                    )
                }
            }

// Pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(horizontal = 24.dp),
                pageSpacing = 16.dp
            ) { pageIndex ->
                val page = pages[pageIndex]
                Card(
                    modifier = Modifier.fillMaxSize(),
                    colors = CardDefaults.cardColors(containerColor = if (lightMode) Color(0xFFF0F4FF) else Color(0xFF0F1F35)),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.25f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
// Mockup area
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .background(Color(0xFF0A1628), RoundedCornerShape(16.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                                .padding(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            page.mockup()
                        }

                        Spacer(modifier = Modifier.height(24.dp))

// Emoji + title
                        Text(page.emoji, fontSize = 32.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            page.title,
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            page.description,
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 21.sp
                        )
                    }
                }
            }

// Bottom buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
// Back button
                if (pagerState.currentPage > 0) {
                    OutlinedButton(
                        onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.7f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text("← Back", fontSize = 14.sp)
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

// Next / Done button
                Button(
                    onClick = {
                        if (pagerState.currentPage < pages.size - 1) {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        } else {
                            onDismiss()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    Text(
                        if (pagerState.currentPage == pages.size - 1) "Let's Go! 🚀" else "Next →",
                        color = Color.Black,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun AuthScreen(onLoginSuccess: () -> Unit, onBack: () -> Unit, lightMode: Boolean = false, primaryColor: Color = Color(0xFFFFB800)) {
    val titleColor = if (lightMode) Color(0xFF1A1A2E) else Color.White
    val mutedLine = if (lightMode) titleColor.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.85f)
    val backTint = if (lightMode) Color(0xFF1A1A2E) else Color.White

    var isLogin by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var chapter by remember { mutableStateOf("") }
    var accountRole by remember { mutableStateOf("student") }
    var year by remember { mutableStateOf("") }
    var statePref by remember { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(false) }
    var show2FAVerification by remember { mutableStateOf(false) }
    var generated2FACode by remember { mutableStateOf("") }
    var userEmailFor2FA by remember { mutableStateOf("") }

    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val scope = rememberCoroutineScope()

    if (show2FAVerification) {
        TwoFactorVerificationScreen(
            email = userEmailFor2FA,
            expectedCode = generated2FACode,
            lightMode = lightMode,
            onVerified = {
                show2FAVerification = false
                onLoginSuccess()
            },
            onBack = {
                show2FAVerification = false
                auth.signOut()
                Toast.makeText(context, "Login cancelled", Toast.LENGTH_SHORT).show()
            }
        )
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize().background(if (lightMode) Color(0xFFF5F8FF) else Color(0xFF0A1628)))
        if (!lightMode) Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.9f)))

        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar with back button — sits above scroll, always tappable
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp, start = 0.dp, end = 16.dp, bottom = 0.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = backTint)
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f).padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                item {
                    Image(
                        painter = painterResource(R.drawable.fbla_tuff),
                        contentDescription = null,
                        modifier = Modifier.size(88.dp),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (isLogin) "WELCOME TO FBLA HUB" else "JOIN FBLA HUB",
                        style = MaterialTheme.typography.headlineMedium,
                        color = titleColor,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.height(32.dp))

                    if (!isLogin) {
                        AuthTextField(value = firstName, onValueChange = { firstName = it }, label = "First Name", lightMode = lightMode, accentColor = primaryColor)
                        Spacer(modifier = Modifier.height(12.dp))
                        AuthTextField(value = lastName, onValueChange = { lastName = it }, label = "Last Name", lightMode = lightMode, accentColor = primaryColor)
                        Spacer(modifier = Modifier.height(12.dp))
                        YearDropdown(selectedYear = year, onYearSelected = { year = it }, lightMode = lightMode, accentColor = primaryColor)
                        Spacer(modifier = Modifier.height(12.dp))
                        StateDropdown(selectedState = statePref, onStateSelected = { statePref = it }, lightMode = lightMode, accentColor = primaryColor)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("I am a", color = mutedLine, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            listOf("student" to "Student", "teacher" to "Teacher").forEach { (value, label) ->
                                val selected = accountRole == value
                                Button(
                                    onClick = { accountRole = value },
                                    modifier = Modifier.weight(1f).height(48.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (selected) primaryColor else if (lightMode) Color(0xFFE8EDF5) else Color.White.copy(alpha = 0.12f),
                                        contentColor = if (selected) Color.Black else if (lightMode) Color(0xFF1A1A2E) else Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) { Text(label, fontWeight = FontWeight.Bold) }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    AuthTextField(value = email, onValueChange = { email = it }, label = "Email Address", lightMode = lightMode, accentColor = primaryColor)
                    Spacer(modifier = Modifier.height(12.dp))
                    AuthTextField(value = password, onValueChange = { password = it }, label = "Password", isPassword = true, lightMode = lightMode, accentColor = primaryColor)
                    if (!isLogin) {
                        Spacer(modifier = Modifier.height(12.dp))
                        AuthTextField(value = confirmPassword, onValueChange = { confirmPassword = it }, label = "Confirm Password", isPassword = true, lightMode = lightMode, accentColor = primaryColor)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            if (email.isEmpty() || password.isEmpty() || (!isLogin && (firstName.isBlank() || lastName.isBlank()))) {
                                Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (!isLogin && password != confirmPassword) {
                                Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            isLoading = true
                            if (isLogin) {
                                auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
// Generate 6-digit 2FA code
                                        val code = (100000..999999).random().toString()
                                        generated2FACode = code
                                        userEmailFor2FA = email

                                        scope.launch {
                                            try {
// Store code in Firestore (expires in 5 minutes)
                                                val twoFactorCode = TwoFactorCode(
                                                    code = code,
                                                    email = email,
                                                    expiresAt = System.currentTimeMillis() + (5 * 60 * 1000),
                                                    verified = false
                                                )
                                                db.collection("twoFactorCodes").document(email).set(twoFactorCode).await()
                                                launch {
                                                    try { sendVerificationEmail(email, code) } catch (e: Exception) { android.util.Log.e("EmailJS", "Failed: ${e.message}") }
                                                }

// Proceed to 2FA verification
                                                isLoading = false
                                                show2FAVerification = true
                                            } catch (e: Exception) {
                                                isLoading = false
                                                Toast.makeText(context, "Error generating code: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    } else {
                                        isLoading = false
                                        Toast.makeText(context, "Error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } else {
                                auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        val displayName = "${firstName.trim()} ${lastName.trim()}".trim()
                                        val profile = UserProfile(uid = auth.currentUser?.uid ?: "", name = displayName, email = email, school = "", year = year, state = statePref, role = accountRole)
                                        db.collection("users").document(auth.currentUser!!.uid).set(profile)
                                            .addOnSuccessListener {
                                                // Generate 2FA code after successful registration
                                                val code = (100000..999999).random().toString()
                                                generated2FACode = code
                                                userEmailFor2FA = email
                                                scope.launch {
                                                    try {
                                                        val twoFactorCode = TwoFactorCode(
                                                            code = code,
                                                            email = email,
                                                            expiresAt = System.currentTimeMillis() + (5 * 60 * 1000),
                                                            verified = false
                                                        )
                                                        db.collection("twoFactorCodes").document(email).set(twoFactorCode).await()
                                                        launch {
                                                            try { sendVerificationEmail(email, code) } catch (e: Exception) { android.util.Log.e("EmailJS", "Failed: ${e.message}") }
                                                        }
                                                        isLoading = false
                                                        show2FAVerification = true
                                                    } catch (e: Exception) {
                                                        isLoading = false
                                                        Toast.makeText(context, "Error generating code: ${e.message}", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            }
                                            .addOnFailureListener { isLoading = false }
                                    } else {
                                        isLoading = false
                                        Toast.makeText(context, "Error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp).shadow(4.dp, RoundedCornerShape(12.dp)),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isLoading
                    ) {
                        if (isLoading) CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp))
                        else Text(if (isLogin) "SIGN IN" else "REGISTER", color = Color.Black, fontWeight = FontWeight.Bold)
                    }

                    TextButton(onClick = { isLogin = !isLogin }) {
                        Text(if (isLogin) "New here? Create an account" else "Have an account? Sign in", color = primaryColor, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } // end Column
    }
}

@Composable
fun TwoFactorVerificationScreen(email: String, expectedCode: String, onVerified: () -> Unit, onBack: () -> Unit, lightMode: Boolean = false) {
    var enteredCode by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
// Background replaced with solid FBLA dark blue
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0A1628)))
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.9f)))

        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar with back button — always above scroll content
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp, start = 0.dp, end = 16.dp, bottom = 0.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                }
                Spacer(modifier = Modifier.weight(1f))
                // TEMP: skip 2FA for testing
                TextButton(onClick = onVerified) {
                    Text("Skip 2FA", color = Color(0xFFFFB800))
                }
            }

            Column(
                modifier = Modifier.weight(1f).padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.Lock, null, modifier = Modifier.size(80.dp), tint = Color(0xFFFFB800))
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "TWO-FACTOR AUTHENTICATION",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "A 6-digit verification code has been sent to:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = email,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFFFFB800),
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(32.dp))

                OutlinedTextField(
                    value = enteredCode,
                    onValueChange = {
                        if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                            enteredCode = it
                            errorMessage = ""
                        }
                    },
                    label = { Text("Enter 6-Digit Code", color = Color.White.copy(alpha = 0.7f)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedTextColor = Color.White,
                        focusedTextColor = Color.White,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.7f),
                        focusedBorderColor = Color(0xFFFFB800)
                    ),
                    textStyle = MaterialTheme.typography.headlineMedium.copy(
                        letterSpacing = 8.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                if (errorMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage,
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (enteredCode.length != 6) {
                            errorMessage = "Please enter a 6-digit code"
                            return@Button
                        }

                        isLoading = true
                        scope.launch {
                            try {
                                val doc = db.collection("twoFactorCodes").document(email).get().await()
                                val storedCode = doc.toObject(TwoFactorCode::class.java)

                                if (storedCode == null) {
                                    errorMessage = "No verification code found. Please try again."
                                    isLoading = false
                                } else if (storedCode.expiresAt < System.currentTimeMillis()) {
                                    errorMessage = "Code expired. Please request a new one."
                                    isLoading = false
                                } else if (storedCode.code != enteredCode) {
                                    errorMessage = "Invalid code. Please try again."
                                    isLoading = false
                                } else {
// Code is valid!
                                    db.collection("twoFactorCodes").document(email).delete().await()
                                    Toast.makeText(context, "Verified successfully!", Toast.LENGTH_SHORT).show()
                                    onVerified()
                                }
                            } catch (e: Exception) {
                                errorMessage = "Verification failed: ${e.message}"
                                isLoading = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp).shadow(4.dp, RoundedCornerShape(12.dp)),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB800)),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isLoading && enteredCode.length == 6
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp))
                    } else {
                        Text("VERIFY", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Code expires in 5 minutes",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.85f)
                )

                Spacer(modifier = Modifier.height(24.dp))

// View verification code option
                var showCode by remember { mutableStateOf(false) }

                OutlinedButton(
                    onClick = { showCode = !showCode },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFFFB800)
                    ),
                    border = BorderStroke(2.dp, Color(0xFFFFB800)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = if (showCode) Icons.Default.Close else Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (showCode) "Hide Code" else "View Verification Code",
                        fontWeight = FontWeight.Bold
                    )
                }

                if (showCode) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = if (lightMode) Color.White else Color(0xFF1A1A1A)),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(2.dp, Color(0xFFFFB800))
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Your Verification Code",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = expectedCode,
                                color = Color(0xFFFFB800),
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 8.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Enter this code above to verify",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        } // end inner Column
    } // end outer Column
}

@Composable
fun AuthTextField(value: String, onValueChange: (String) -> Unit, label: String, isPassword: Boolean = false, lightMode: Boolean = false, accentColor: Color = Color(0xFFFFB800)) {
    val fg = if (lightMode) Color(0xFF1A1A2E) else Color.White
    val border = if (lightMode) fg.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.7f)
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedTextColor = fg,
            focusedTextColor = fg,
            unfocusedBorderColor = border,
            focusedBorderColor = accentColor,
            cursorColor = accentColor,
            unfocusedLabelColor = fg.copy(alpha = 0.8f),
            focusedLabelColor = accentColor
        ),
        shape = RoundedCornerShape(12.dp)
    )
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

data class NewsArticle(val title: String, val summary: String, val date: String, val url: String, val tag: String = "NATIONAL", val timestamp: Long = 0L)
data class ResourceItem(val title: String, val description: String, val url: String, val icon: ImageVector)

// Helper function to parse event dates
fun parseEventDates(eventDateString: String): List<String> {
    val dates = mutableListOf<String>()

// Patterns to match: "April 9–10, 2026", "June 29 - July 2, 2026", "Feb 7, 2026", etc.
    val patterns = listOf(
// Single day: "Feb 7, 2026"
        Regex("""(\w+)\s+(\d+),\s+(\d{4})"""),
// Date range same month: "April 9–10, 2026" or "April 9-10, 2026"
        Regex("""(\w+)\s+(\d+)[–-](\d+),\s+(\d{4})"""),
// Date range different months: "June 29 - July 2, 2026"
        Regex("""(\w+)\s+(\d+)\s*[-–]\s*(\w+)\s+(\d+),\s+(\d{4})""")
    )

    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    val parseFormat = SimpleDateFormat("MMMM d, yyyy", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    try {
// Try cross-month range first (most specific): "June 29 - July 2, 2026"
        patterns[2].find(eventDateString)?.let { match ->
            val (startMonth, startDay, endMonth, endDay, year) = match.destructured
            val startDateStr = "$startMonth $startDay, $year"
            val endDateStr = "$endMonth $endDay, $year"
            val startDate = parseFormat.parse(startDateStr)
            val endDate = parseFormat.parse(endDateStr)
            if (startDate != null && endDate != null) {
                val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                cal.time = startDate
                while (!cal.time.after(endDate)) {
                    dates.add(dateFormat.format(cal.time))
                    cal.add(Calendar.DAY_OF_MONTH, 1)
                }
            }
            return dates
        }

// Try same-month range pattern: "April 9–10, 2026"
        patterns[1].find(eventDateString)?.let { match ->
            val (month, startDay, endDay, year) = match.destructured
            val start = startDay.toInt()
            val end = endDay.toInt()

            for (day in start..end) {
                val dateStr = "$month $day, $year"
                parseFormat.parse(dateStr)?.let { parsedDate ->
                    dates.add(dateFormat.format(parsedDate))
                }
            }
            return dates
        }

// Try single day pattern: "Feb 7, 2026"
        patterns[0].find(eventDateString)?.let { match ->
            val (month, day, year) = match.destructured
            val dateStr = "$month $day, $year"
            parseFormat.parse(dateStr)?.let { parsedDate ->
                dates.add(dateFormat.format(parsedDate))
            }
            return dates
        }
    } catch (e: Exception) {
// If parsing fails, return empty list
    }

    return dates
}

fun insertCalendarEvent(context: android.content.Context, name: String, date: String, location: String): Long {
    val daysFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
    val days = parseEventDates(date)
    val startMs = days.firstOrNull()?.let { daysFmt.parse(it)?.time } ?: return -1L
    val endMs = days.lastOrNull()?.let {
        val c = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        c.time = daysFmt.parse(it)!!
        c.add(Calendar.DAY_OF_MONTH, 1)
        c.timeInMillis
    } ?: (startMs + 24 * 60 * 60 * 1000L)

    // Query any available calendar — no VISIBLE filter so we don't miss hidden ones
    val calId = try {
        var id = -1L
        context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            arrayOf(CalendarContract.Calendars._ID),
            null, null, "${CalendarContract.Calendars._ID} ASC"
        )?.use { if (it.moveToFirst()) id = it.getLong(0) }
        id
    } catch (e: Exception) { -1L }

    if (calId == -1L) return -1L

    val values = ContentValues().apply {
        put(CalendarContract.Events.CALENDAR_ID, calId)
        put(CalendarContract.Events.TITLE, name)
        put(CalendarContract.Events.DTSTART, startMs)
        put(CalendarContract.Events.DTEND, endMs)
        put(CalendarContract.Events.ALL_DAY, 1)
        put(CalendarContract.Events.DESCRIPTION, "FBLA Competition — added via FBLA Hub")
        put(CalendarContract.Events.EVENT_LOCATION, location)
        put(CalendarContract.Events.HAS_ALARM, 1)
        put(CalendarContract.Events.EVENT_TIMEZONE, "UTC")
    }
    return try {
        context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            ?.lastPathSegment?.toLongOrNull() ?: -1L
    } catch (e: Exception) { -1L }
}

// Animated button modifier for press effect
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

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

// Load user profile data
    LaunchedEffect(auth.currentUser?.uid) {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            db.collection("users").document(uid).addSnapshotListener { snapshot, _ ->
                val profile = snapshot?.toObject(UserProfile::class.java)
                userState = profile?.state ?: ""
                userChapter = profile?.school ?: ""
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
    val publicProfile by SettingsStore.publicProfileFlow(context).collectAsState(initial = true)
    val boldText by SettingsStore.boldTextFlow(context).collectAsState(initial = false)
    val highContrast by SettingsStore.highContrastFlow(context).collectAsState(initial = false)
    val reduceMotion by SettingsStore.reduceMotionFlow(context).collectAsState(initial = false)

    val effectivePrimary = primaryColor
    val effectiveBgOpacity = bgOpacity

    val effectivePrimaryColor = invertColorValues(effectivePrimary, lightMode)

// Dynamic navigation bar colors
    val navBarBgColor = if (lightMode) Color(0xFFF5F5F5) else Color(0xFF000D1A)
    val navBarContentColor = if (lightMode) Color.Black else Color.White

// Show tutorial as full-screen overlay
    if (showTutorial) {
        TutorialScreen(
            onDismiss = { showTutorial = false },
            primaryColor = invertColorValues(primaryColor, false),
            lightMode = lightMode
        )
        return
    }

    Scaffold(
        bottomBar = {
            if (currentSubScreen == null) {
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
            }
        }
    ) { padding ->
        Crossfade(
            targetState = if (currentSubScreen != null) "subscreen" to currentSubScreen else "main" to selectedTab,
            animationSpec = tween(durationMillis = 600, easing = LinearEasing),
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
                            highContrast = highContrast,
                            onHighContrastChange = { scope.launch { SettingsStore.saveHighContrast(context, it) } },
                            reduceMotion = reduceMotion,
                            onReduceMotionChange = { scope.launch { SettingsStore.saveReduceMotion(context, it) } },
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
                            onOpenChat = { userId, userName ->
                                currentSubScreen = "chat_$userId"
                            }
                        )
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
                                        primaryColor = effectivePrimaryColor
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
                        4 -> ResourcesScreen(effectivePrimary, effectiveBgOpacity, isSimplifiedUI, textSizeMultiplier, lightMode, userState, userChapter)
                    }
                }
            }
        }
    }
}

@Composable
fun LoggedOutProfileScreen(onLoginClick: () -> Unit, onTutorialClick: () -> Unit = {}, onSettingsClick: () -> Unit = {}, primaryColor: Color, bgOpacity: Float, isSimplifiedUI: Boolean, lightMode: Boolean = false) {
// Dynamic text color
    val textColor = if (lightMode) Color.Black else Color.White
    val textColorSecondary = if (lightMode) Color.Black.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.9f)
    val textColorTertiary = if (lightMode) Color.Black.copy(alpha = 0.75f) else Color.White.copy(alpha = 0.85f)

    Box(modifier = Modifier.fillMaxSize()) {
// Background — white in light mode, dark blue in dark mode
        Box(modifier = Modifier.fillMaxSize().background(
            if (lightMode) Color.White else Color(0xFF0A1628)
        ))
        if (!lightMode) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = bgOpacity)))
        }
// Gear icon always visible top-right
        IconButton(
            onClick = onSettingsClick,
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 48.dp, end = 8.dp)
        ) {
            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = primaryColor, modifier = Modifier.size(26.dp))
        }

        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Image(
                painter = painterResource(R.drawable.fbla_tuff),
                contentDescription = null,
                modifier = Modifier.size(100.dp),
                contentScale = ContentScale.Fit
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text("Sign in to view your profile", color = textColorSecondary)
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onLoginClick,
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.width(200.dp).height(50.dp)
            ) {
                Text("LOGIN OR SIGNUP", color = Color.Black, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onTutorialClick,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = primaryColor),
                border = BorderStroke(1.5.dp, primaryColor.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.width(200.dp).height(50.dp)
            ) {
                Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("TUTORIAL", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    onTutorialClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onFindLinksClick: () -> Unit,
    onConnectionRequestsClick: () -> Unit,
    onMyConnectionsClick: () -> Unit,
    onMessagesClick: () -> Unit,
    onClassroomsClick: () -> Unit,
    onDressCodeClick: () -> Unit = {},
    primaryColor: Color,
    bgOpacity: Float,
    textSizeMultiplier: Float,
    isSimplifiedUI: Boolean,
    lightMode: Boolean = false
) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current

    var profile by remember { mutableStateOf<UserProfile?>(null) }
    var isProfileLoading by remember { mutableStateOf(true) }
    var isEditing by remember { mutableStateOf(false) }
    var isProcessingImage by remember { mutableStateOf(false) }
    var unreadMessageCount by remember { mutableStateOf(0) }
    var unreadMessagesFrom by remember { mutableStateOf<List<String>>(emptyList()) }
    var showUnreadDialog by remember { mutableStateOf(false) }
    var showNetworkingMenu by remember { mutableStateOf(false) }
    var showLogoutConfirm by remember { mutableStateOf(false) }

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var chapter by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }
    var statePref by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var pfpUrl by remember { mutableStateOf("") }
    var instagram by remember { mutableStateOf("") }
    var twitter by remember { mutableStateOf("") }
    var facebook by remember { mutableStateOf("") }
    var linkedin by remember { mutableStateOf("") }

    var instagramError by remember { mutableStateOf("") }
    var twitterError by remember { mutableStateOf("") }
    var facebookError by remember { mutableStateOf("") }
    var linkedinError by remember { mutableStateOf("") }

    var connectionCount by remember { mutableIntStateOf(0) }
    var pendingRequestsCount by remember { mutableIntStateOf(0) }

    val effectiveBgOpacity = bgOpacity
    val textColor = if (lightMode) Color(0xFF1A1A2E) else Color.White
    val textColorSecondary = if (lightMode) Color(0xFF1A1A2E).copy(alpha = 0.8f) else Color.White.copy(alpha = 0.9f)
    val textColorTertiary = if (lightMode) Color(0xFF1A1A2E).copy(alpha = 0.75f) else Color.White.copy(alpha = 0.85f)

    val pfpLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            isProcessingImage = true
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                val resized = Bitmap.createScaledBitmap(bitmap, 200, 200, true)
                val outputStream = ByteArrayOutputStream()
                resized.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
                val bytes = outputStream.toByteArray()
                val base64String = "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.DEFAULT)
                pfpUrl = base64String
                db.collection("users").document(auth.currentUser!!.uid).update("pfpUrl", pfpUrl)
                isProcessingImage = false
                Toast.makeText(context, "Profile Picture Updated!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                isProcessingImage = false
                Toast.makeText(context, "Failed to process image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(Unit) {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            db.collection("users").document(uid).addSnapshotListener { snapshot, error ->
                isProfileLoading = false
                if (error != null) return@addSnapshotListener
                val data = snapshot?.toObject(UserProfile::class.java)
                profile = data
                if (data != null && !isEditing) {
                    name = data.name
                    email = data.email.ifEmpty { auth.currentUser?.email ?: "" }
                    chapter = data.school
                    year = data.year
                    statePref = data.state
                    bio = data.bio
                    pfpUrl = data.pfpUrl
                    instagram = data.instagram
                    twitter = data.twitter
                    facebook = data.facebook
                    linkedin = data.linkedin
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        val uid = auth.currentUser?.uid
        if (uid != null) {
// Listen for pending connection requests
            db.collection("connectionRequests")
                .whereEqualTo("toUid", uid)
                .whereEqualTo("status", "pending")
                .addSnapshotListener { snapshot, _ ->
                    pendingRequestsCount = snapshot?.size() ?: 0
                }

// Listen for total connections
            db.collection("connections").document(uid)
                .addSnapshotListener { snapshot, _ ->
                    val connectedUids = snapshot?.get("connectedUids") as? List<*>
                    connectionCount = connectedUids?.size ?: 0
                }

// Listen for unread messages
            db.collection("messages")
                .addSnapshotListener { snapshot, _ ->
                    val messages = snapshot?.documents?.mapNotNull { it.toObject(ChatMessage::class.java) } ?: emptyList()

// Filter messages where current user is receiver
                    val unreadSenders = messages
                        .filter { it.receiverId == uid && it.senderId != uid }
                        .groupBy { it.senderId }
                        .keys
                        .toList()

                    unreadMessageCount = unreadSenders.size

// Get sender names
                    if (unreadSenders.isNotEmpty()) {
                        db.collection("users").whereIn("uid", unreadSenders.take(10))
                            .get()
                            .addOnSuccessListener { users ->
                                unreadMessagesFrom = users.documents.mapNotNull {
                                    it.toObject(UserProfile::class.java)?.name
                                }
                            }
                    } else {
                        unreadMessagesFrom = emptyList()
                    }
                }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
// Background replaced with solid FBLA dark blue
        Box(modifier = Modifier.fillMaxSize().background(if (lightMode) Color.White else Color(0xFF0A1628)))
        if (!lightMode) Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = effectiveBgOpacity)))

        if (isProfileLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = primaryColor)
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        IconButton(onClick = onSettingsClick) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = primaryColor, modifier = Modifier.size(26.dp))
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(130.dp)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .background(primaryColor, CircleShape)
                                .clip(CircleShape)
                                .clickable { pfpLauncher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isProcessingImage) {
                                CircularProgressIndicator(color = Color.Black)
                            } else if (pfpUrl.isNotEmpty()) {
                                AsyncImage(
                                    model = pfpUrl,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text(name.take(1).ifEmpty { "U" }.uppercase(), color = Color.Black, style = MaterialTheme.typography.displayMedium.copy(fontSize = (MaterialTheme.typography.displayMedium.fontSize.value * textSizeMultiplier).sp), fontWeight = FontWeight.Bold)
                            }
                        }
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color.Black.copy(alpha = 0.85f), CircleShape)
                                .background(primaryColor.copy(alpha = 0.2f), CircleShape)
                                .align(Alignment.BottomEnd)
                                .clickable { pfpLauncher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CameraAlt, null, tint = if (lightMode) Color(0xFF1A1A2E) else Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))

                    if (isEditing) {
                        ProfileEditField(value = name, onValueChange = { name = it }, label = "Full Name", lightMode = lightMode, accentColor = primaryColor)
                        ProfileEditField(value = email, onValueChange = { email = it }, label = "Email", lightMode = lightMode, accentColor = primaryColor)
                        ChapterNameDropdown(selectedChapter = chapter, onChapterSelected = { chapter = it }, lightMode = lightMode, accentColor = primaryColor)
                        Spacer(modifier = Modifier.height(12.dp))
                        YearDropdown(selectedYear = year, onYearSelected = { year = it }, lightMode = lightMode, accentColor = primaryColor)
                        Spacer(modifier = Modifier.height(12.dp))
                        StateDropdown(selectedState = statePref, onStateSelected = { statePref = it }, lightMode = lightMode, accentColor = primaryColor)
                        ProfileEditField(value = bio, onValueChange = { bio = it }, label = "Short Bio", lightMode = lightMode, accentColor = primaryColor)

                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "SOCIAL MEDIA (Optional)",
                            style = MaterialTheme.typography.labelMedium.copy(
                                letterSpacing = 1.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontSize = (12 * textSizeMultiplier).sp
                            ),
                            color = primaryColor,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        ProfileEditField(
                            value = instagram,
                            onValueChange = {
                                instagram = it
                                val (isValid, error) = validateSocialMediaLink(it, "Instagram")
                                instagramError = error
                            },
                            label = "Instagram (instagram.com/username)",
                            errorMessage = instagramError,
                            lightMode = lightMode,
                            accentColor = primaryColor
                        )
                        ProfileEditField(
                            value = twitter,
                            onValueChange = {
                                twitter = it
                                val (isValid, error) = validateSocialMediaLink(it, "Twitter")
                                twitterError = error
                            },
                            label = "X/Twitter (twitter.com/username)",
                            errorMessage = twitterError,
                            lightMode = lightMode,
                            accentColor = primaryColor
                        )
                        ProfileEditField(
                            value = facebook,
                            onValueChange = {
                                facebook = it
                                val (isValid, error) = validateSocialMediaLink(it, "Facebook")
                                facebookError = error
                            },
                            label = "Facebook (facebook.com/username)",
                            errorMessage = facebookError,
                            lightMode = lightMode,
                            accentColor = primaryColor
                        )
                        ProfileEditField(
                            value = linkedin,
                            onValueChange = {
                                linkedin = it
                                val (isValid, error) = validateSocialMediaLink(it, "LinkedIn")
                                linkedinError = error
                            },
                            label = "LinkedIn (linkedin.com/in/username)",
                            errorMessage = linkedinError,
                            lightMode = lightMode,
                            accentColor = primaryColor
                        )
                    } else {
                        Text(text = name.ifEmpty { "Member" }, style = MaterialTheme.typography.headlineMedium.copy(fontSize = (MaterialTheme.typography.headlineMedium.fontSize.value * textSizeMultiplier).sp), color = textColor, fontWeight = FontWeight.Black)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (year.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .background(primaryColor.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                                        .border(1.dp, primaryColor.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                                        .padding(horizontal = 12.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = "Class of $year",
                                        color = primaryColor,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = (11 * textSizeMultiplier).sp),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            if (statePref.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .background(primaryColor.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                                        .border(1.dp, primaryColor.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                                        .padding(horizontal = 12.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = statePref,
                                        color = primaryColor,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = (11 * textSizeMultiplier).sp),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        if (email.isNotEmpty()) {
                            Text(text = email, style = MaterialTheme.typography.bodySmall.copy(fontSize = (MaterialTheme.typography.bodySmall.fontSize.value * textSizeMultiplier).sp), color = textColorTertiary, textAlign = TextAlign.Center)
                        }
                        if (bio.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = bio, style = MaterialTheme.typography.bodySmall.copy(fontSize = (MaterialTheme.typography.bodySmall.fontSize.value * textSizeMultiplier).sp), color = textColor.copy(alpha = 0.92f), textAlign = TextAlign.Center)
                        }

// Social Media Links
                        val hasSocialMedia = instagram.isNotEmpty() || twitter.isNotEmpty() || facebook.isNotEmpty() || linkedin.isNotEmpty()
                        if (hasSocialMedia) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (instagram.isNotEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clickable {
                                                val url = normalizeSocialMediaUrl(instagram)
                                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                            }
                                    ) {
                                        SocialMediaLogo(platform = "Instagram", modifier = Modifier.size(40.dp))
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                }
                                if (twitter.isNotEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clickable {
                                                val url = normalizeSocialMediaUrl(twitter)
                                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                            }
                                    ) {
                                        SocialMediaLogo(platform = "X (Twitter)", modifier = Modifier.size(40.dp))
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                }
                                if (facebook.isNotEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clickable {
                                                val url = normalizeSocialMediaUrl(facebook)
                                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                            }
                                    ) {
                                        SocialMediaLogo(platform = "Facebook", modifier = Modifier.size(40.dp))
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                }
                                if (linkedin.isNotEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clickable {
                                                val url = normalizeSocialMediaUrl(linkedin)
                                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                            }
                                    ) {
                                        SocialMediaLogo(platform = "LinkedIn", modifier = Modifier.size(40.dp))
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val statCardColor = if (lightMode) Color(0xFFE8EDF5) else Color.White.copy(alpha = 0.15f)
                            Card(
                                modifier = Modifier.weight(1f).height(90.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = statCardColor)
                            ) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.People, null, tint = primaryColor, modifier = Modifier.size(24.dp))
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(connectionCount.toString(), color = textColor, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium.copy(fontSize = (16 * textSizeMultiplier).sp))
                                        Text("Connections", color = textColorSecondary, style = MaterialTheme.typography.labelSmall.copy(fontSize = (10 * textSizeMultiplier).sp))
                                    }
                                }
                            }
                            Card(
                                modifier = Modifier.weight(1f).height(90.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = statCardColor)
                            ) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.School, null, tint = primaryColor, modifier = Modifier.size(24.dp))
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(chapter.ifEmpty { "—" }, color = textColor, fontWeight = FontWeight.Black, style = MaterialTheme.typography.labelMedium.copy(fontSize = (11 * textSizeMultiplier).sp), maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                                        Text("Chapter", color = textColorSecondary, style = MaterialTheme.typography.labelSmall.copy(fontSize = (10 * textSizeMultiplier).sp))
                                    }
                                }
                            }
                            Card(
                                modifier = Modifier.weight(1f).height(90.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = statCardColor)
                            ) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.CalendarToday, null, tint = primaryColor, modifier = Modifier.size(24.dp))
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(year.ifEmpty { "—" }, color = textColor, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium.copy(fontSize = (16 * textSizeMultiplier).sp))
                                        Text("Grad Year", color = textColorSecondary, style = MaterialTheme.typography.labelSmall.copy(fontSize = (10 * textSizeMultiplier).sp))
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        val completionFields = listOf(name.isNotEmpty(), bio.isNotEmpty(), pfpUrl.isNotEmpty(), chapter.isNotEmpty(), year.isNotEmpty(), statePref.isNotEmpty(), instagram.isNotEmpty() || twitter.isNotEmpty() || facebook.isNotEmpty() || linkedin.isNotEmpty())
                        val completionPercent = completionFields.count { it }.toFloat() / completionFields.size.toFloat()
                        val completionInt = (completionPercent * 100).toInt()
                        val completionHint = when {
                            pfpUrl.isEmpty() -> "Add a profile photo"
                            bio.isEmpty() -> "Add a bio"
                            instagram.isEmpty() && twitter.isEmpty() && facebook.isEmpty() && linkedin.isEmpty() -> "Add a social media link"
                            chapter.isEmpty() -> "Add your chapter"
                            year.isEmpty() -> "Add your graduation year"
                            statePref.isEmpty() -> "Add your state"
                            else -> null
                        }
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = if (lightMode) Color(0xFFE8EDF5) else Color.White.copy(alpha = 0.15f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Profile Strength", color = textColor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium.copy(fontSize = (12 * textSizeMultiplier).sp))
                                    Spacer(modifier = Modifier.weight(1f))
                                    Text("$completionInt%", color = primaryColor, fontWeight = FontWeight.Black, style = MaterialTheme.typography.labelMedium.copy(fontSize = (12 * textSizeMultiplier).sp))
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = { completionPercent },
                                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                    color = primaryColor,
                                    trackColor = primaryColor.copy(alpha = 0.2f)
                                )
                                if (completionHint != null && completionInt < 100) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("$completionHint to improve", color = textColorSecondary, style = MaterialTheme.typography.labelSmall.copy(fontSize = (10 * textSizeMultiplier).sp))
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(
                            onClick = {
                                shareMemberProfile(
                                    context = context,
                                    name = name.ifEmpty { "FBLA Member" },
                                    chapter = chapter,
                                    year = year,
                                    state = statePref,
                                    bio = bio
                                )
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            border = BorderStroke(1.5.dp, primaryColor),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = primaryColor),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Share, null, tint = primaryColor, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("SHARE PROFILE", fontWeight = FontWeight.Bold, style = TextStyle(fontSize = (13 * textSizeMultiplier).sp))
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = onFindLinksClick,
                        modifier = Modifier.fillMaxWidth().height(50.dp).shadow(4.dp, RoundedCornerShape(12.dp)),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Search, null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("LINK TO PEERS", color = Color.Black, fontWeight = FontWeight.Bold, style = TextStyle(fontSize = (14 * textSizeMultiplier).sp))
                    }

                    Spacer(modifier = Modifier.height(12.dp))

// ── NETWORKING HUB ──────────────────────────────────────────
                    val networkingTotalBadge = pendingRequestsCount + unreadMessageCount
                    Box(modifier = Modifier.fillMaxWidth()) {
// Main Networking button
                        Button(
                            onClick = { showNetworkingMenu = !showNetworkingMenu },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .shadow(4.dp, RoundedCornerShape(12.dp)),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = primaryColor,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Hub, null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "NETWORKING",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                style = TextStyle(fontSize = (14 * textSizeMultiplier).sp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                if (showNetworkingMenu) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                null,
                                modifier = Modifier.size(18.dp),
                                tint = Color.Black
                            )
                        }
// Total badge on the Networking button
                        if (networkingTotalBadge > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = (-6).dp, y = (-6).dp)
                                    .size(22.dp)
                                    .background(Color.Red, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (networkingTotalBadge > 9) "9+" else networkingTotalBadge.toString(),
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

// Animated expand panel
                    androidx.compose.animation.AnimatedVisibility(
                        visible = showNetworkingMenu,
                        enter = androidx.compose.animation.expandVertically(
                            animationSpec = androidx.compose.animation.core.spring(
                                dampingRatio = 0.7f, stiffness = 300f
                            )
                        ) + androidx.compose.animation.fadeIn(),
                        exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                                .shadow(8.dp, RoundedCornerShape(20.dp))
                                .background(
                                    if (lightMode) Color(0xFFF0F4FF) else Color(0xFF1A2540),
                                    RoundedCornerShape(20.dp)
                                )
                                .border(1.dp, primaryColor.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                                .padding(16.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
// Header label
                                Text(
                                    "Choose an option",
                                    color = primaryColor,
                                    fontSize = (11 * textSizeMultiplier).sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    modifier = Modifier.padding(bottom = 2.dp)
                                )

// --- Requests tile ---
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    Button(
                                        onClick = { showNetworkingMenu = false; onConnectionRequestsClick() },
                                        modifier = Modifier.fillMaxWidth().height(54.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (lightMode) Color(0xFFE3E8F5) else Color(0xFF243050)
                                        ),
                                        shape = RoundedCornerShape(14.dp),
                                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                                    ) {
                                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .background(primaryColor.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.PersonAdd, null, tint = primaryColor, modifier = Modifier.size(20.dp))
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("Connection Requests", color = textColor, fontWeight = FontWeight.Bold, fontSize = (13 * textSizeMultiplier).sp)
                                                Text("People who want to connect", color = textColor.copy(alpha = 0.6f), fontSize = (10 * textSizeMultiplier).sp)
                                            }
                                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = primaryColor, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                    if (pendingRequestsCount > 0) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .offset(x = (-8).dp, y = (-6).dp)
                                                .height(18.dp)
                                                .widthIn(min = 18.dp)
                                                .background(Color.Red, RoundedCornerShape(9.dp))
                                                .padding(horizontal = 5.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(pendingRequestsCount.toString(), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

// --- My Connections tile ---
                                Button(
                                    onClick = { showNetworkingMenu = false; onMyConnectionsClick() },
                                    modifier = Modifier.fillMaxWidth().height(54.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (lightMode) Color(0xFFE3E8F5) else Color(0xFF243050)
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                                ) {
                                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(primaryColor.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.People, null, tint = primaryColor, modifier = Modifier.size(20.dp))
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("My Connections", color = textColor, fontWeight = FontWeight.Bold, fontSize = (13 * textSizeMultiplier).sp)
                                            Text("$connectionCount peer${if (connectionCount != 1) "s" else ""} in your network", color = textColor.copy(alpha = 0.6f), fontSize = (10 * textSizeMultiplier).sp)
                                        }
                                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = primaryColor, modifier = Modifier.size(20.dp))
                                    }
                                }

// --- Messages tile ---
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    Button(
                                        onClick = {
                                            showNetworkingMenu = false
                                            if (unreadMessageCount > 0) showUnreadDialog = true else onMessagesClick()
                                        },
                                        modifier = Modifier.fillMaxWidth().height(54.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (lightMode) Color(0xFFE3E8F5) else Color(0xFF243050)
                                        ),
                                        shape = RoundedCornerShape(14.dp),
                                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                                    ) {
                                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .background(primaryColor.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.Chat, null, tint = primaryColor, modifier = Modifier.size(20.dp))
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("Messages", color = textColor, fontWeight = FontWeight.Bold, fontSize = (13 * textSizeMultiplier).sp)
                                                Text("Chat with your connections", color = textColor.copy(alpha = 0.6f), fontSize = (10 * textSizeMultiplier).sp)
                                            }
                                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = primaryColor, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                    if (unreadMessageCount > 0) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .offset(x = (-8).dp, y = (-6).dp)
                                                .height(18.dp)
                                                .widthIn(min = 18.dp)
                                                .background(Color.Red, RoundedCornerShape(9.dp))
                                                .padding(horizontal = 5.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(if (unreadMessageCount > 9) "9+" else unreadMessageCount.toString(), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

// --- Classrooms tile ---
                                Button(
                                    onClick = { showNetworkingMenu = false; onClassroomsClick() },
                                    modifier = Modifier.fillMaxWidth().height(54.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (lightMode) Color(0xFFE3E8F5) else Color(0xFF243050)
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                                ) {
                                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(primaryColor.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.People, null, tint = primaryColor, modifier = Modifier.size(20.dp))
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Classrooms", color = textColor, fontWeight = FontWeight.Bold, fontSize = (13 * textSizeMultiplier).sp)
                                            Text("Chapter group chats & invite codes", color = textColor.copy(alpha = 0.6f), fontSize = (10 * textSizeMultiplier).sp)
                                        }
                                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = primaryColor, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }
                    }
// ── END NETWORKING HUB ─────────────────────────────────────

                    Spacer(modifier = Modifier.height(12.dp))

// Edit (big) + Logout (small)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = {
                                if (isEditing) {
                                    val hasErrors = instagramError.isNotEmpty() ||
                                            twitterError.isNotEmpty() ||
                                            facebookError.isNotEmpty() ||
                                            linkedinError.isNotEmpty()
                                    if (hasErrors) {
                                        Toast.makeText(context, "Please fix social media link errors before saving", Toast.LENGTH_LONG).show()
                                        return@Button
                                    }
                                    val newProfile = profile!!.copy(name = name, email = email, school = chapter, year = year, state = statePref, bio = bio, pfpUrl = pfpUrl, instagram = instagram, twitter = twitter, facebook = facebook, linkedin = linkedin)
                                    db.collection("users").document(auth.currentUser!!.uid).set(newProfile)
                                    profile = newProfile
                                }
                                isEditing = !isEditing
                            },
                            modifier = Modifier.weight(1f).height(50.dp).shadow(4.dp, RoundedCornerShape(12.dp)),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isEditing) Color(0xFF2ECC71) else primaryColor,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(if (isEditing) Icons.Default.Check else Icons.Default.Edit, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (isEditing) "SAVE PROFILE" else "EDIT PROFILE",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                style = TextStyle(fontSize = (14 * textSizeMultiplier).sp)
                            )
                        }

// Small logout icon button
                        IconButton(
                            onClick = { showLogoutConfirm = true },
                            modifier = Modifier
                                .size(50.dp)
                                .shadow(4.dp, RoundedCornerShape(12.dp))
                                .background(
                                    if (lightMode) Color(0xFFFFE5E5) else Color(0xFF3D1515),
                                    RoundedCornerShape(12.dp)
                                )
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Logout, null, tint = Color(0xFFDC143C), modifier = Modifier.size(22.dp))
                        }
                    }

// Logout confirmation dialog
                    if (showLogoutConfirm) {
                        AlertDialog(
                            onDismissRequest = { showLogoutConfirm = false },
                            containerColor = if (lightMode) Color.White else Color(0xFF1A2540),
                            shape = RoundedCornerShape(20.dp),
                            icon = {
                                Box(
                                    modifier = Modifier.size(56.dp).background(Color(0xFFDC143C).copy(alpha = 0.15f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Logout, null, tint = Color(0xFFDC143C), modifier = Modifier.size(28.dp))
                                }
                            },
                            title = {
                                Text(
                                    "Log Out?",
                                    color = if (lightMode) Color(0xFF1A1A2E) else Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 20.sp
                                )
                            },
                            text = {
                                Text(
                                    "You'll need to sign in again to access your profile and connections.",
                                    color = if (lightMode) Color(0xFF1A1A2E).copy(alpha = 0.75f) else Color.White.copy(alpha = 0.8f),
                                    textAlign = TextAlign.Center
                                )
                            },
                            confirmButton = {
                                Button(
                                    onClick = { showLogoutConfirm = false; onLogout() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC143C)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Yes, Log Out", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            },
                            dismissButton = {
                                OutlinedButton(
                                    onClick = { showLogoutConfirm = false },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    border = BorderStroke(1.dp, if (lightMode) Color(0xFF1A1A2E).copy(alpha = 0.3f) else Color.White.copy(alpha = 0.3f))
                                ) {
                                    Text("Cancel", color = if (lightMode) Color(0xFF1A1A2E) else Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }

}


@Composable
fun SettingToggle(title: String, subtitle: String, icon: ImageVector, checked: Boolean, onCheckedChange: (Boolean) -> Unit, primaryColor: Color, textSizeMultiplier: Float, lightMode: Boolean = false) {
    val textColor = if (lightMode) Color(0xFF1A1A2E) else Color.White
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = textColor.copy(alpha = 0.9f), modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = textColor, style = MaterialTheme.typography.bodyLarge.copy(fontSize = (MaterialTheme.typography.bodyLarge.fontSize.value * textSizeMultiplier).sp))
            Text(subtitle, color = textColor.copy(alpha = 0.85f), style = MaterialTheme.typography.bodySmall.copy(fontSize = (MaterialTheme.typography.bodySmall.fontSize.value * textSizeMultiplier).sp))
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, colors = SwitchDefaults.colors(checkedThumbColor = primaryColor, checkedTrackColor = primaryColor.copy(alpha = 0.3f), uncheckedThumbColor = Color.Gray, uncheckedTrackColor = Color.Gray.copy(alpha = 0.3f)))
    }
}

fun normalizeSocialMediaUrl(link: String): String {
    if (link.isEmpty()) return ""
    val trimmed = link.trim()
// If it already starts with http:// or https://, return as is
    if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
        return trimmed
    }
// Otherwise, add https://
    return "https://$trimmed"
}

fun validateSocialMediaLink(link: String, platform: String): Pair<Boolean, String> {
    if (link.isEmpty()) return Pair(true, "") // Empty is OK

    val trimmedLink = link.trim()

    return when (platform) {
        "Instagram" -> {
            if (trimmedLink.contains("instagram.com", ignoreCase = true)) {
                Pair(true, "")
            } else {
                Pair(false, "Please enter a valid Instagram link (instagram.com/username)")
            }
        }
        "Twitter" -> {
            if (trimmedLink.contains("twitter.com", ignoreCase = true) || trimmedLink.contains("x.com", ignoreCase = true)) {
                Pair(true, "")
            } else {
                Pair(false, "Please enter a valid Twitter/X link (twitter.com/username)")
            }
        }
        "Facebook" -> {
            if (trimmedLink.contains("facebook.com", ignoreCase = true) || trimmedLink.contains("fb.com", ignoreCase = true)) {
                Pair(true, "")
            } else {
                Pair(false, "Please enter a valid Facebook link (facebook.com/username)")
            }
        }
        "LinkedIn" -> {
            if (trimmedLink.contains("linkedin.com", ignoreCase = true)) {
                Pair(true, "")
            } else {
                Pair(false, "Please enter a valid LinkedIn link (linkedin.com/in/username)")
            }
        }
        else -> Pair(true, "")
    }
}

@Composable
fun ProfileEditField(value: String, onValueChange: (String) -> Unit, label: String, errorMessage: String = "", lightMode: Boolean = false, accentColor: Color = Color(0xFFFFB800)) {
    val fg = if (lightMode) Color(0xFF1A1A2E) else Color.White
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedTextColor = fg,
                focusedTextColor = fg,
                errorTextColor = Color(0xFFFF5252),
                unfocusedBorderColor = fg.copy(alpha = 0.5f),
                focusedBorderColor = accentColor,
                cursorColor = accentColor,
                unfocusedLabelColor = fg.copy(alpha = 0.85f),
                focusedLabelColor = accentColor
            ),
            isError = errorMessage.isNotEmpty()
        )
        if (errorMessage.isNotEmpty()) {
            Text(
                text = errorMessage,
                color = Color(0xFFFF5252),
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}

@Composable
fun YearDropdown(selectedYear: String, onYearSelected: (String) -> Unit, lightMode: Boolean = false, accentColor: Color = Color(0xFFFFB800)) {
    var expanded by remember { mutableStateOf(false) }
    val years = (2026..2033).map { it.toString() }
    val fg = if (lightMode) Color(0xFF1A1A2E) else Color.White
    val border = if (lightMode) fg.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.7f)

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selectedYear,
            onValueChange = { },
            readOnly = true,
            label = { Text("Graduation Year") },
            trailingIcon = {
                Icon(
                    imageVector = if (expanded) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                    contentDescription = "Dropdown",
                    tint = fg,
                    modifier = Modifier.clickable { expanded = !expanded }
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedTextColor = fg,
                focusedTextColor = fg,
                unfocusedBorderColor = border,
                focusedBorderColor = accentColor,
                disabledTextColor = fg,
                disabledBorderColor = border,
                disabledLabelColor = fg.copy(alpha = 0.85f),
                unfocusedLabelColor = fg.copy(alpha = 0.85f),
                focusedLabelColor = accentColor,
                cursorColor = accentColor
            ),
            shape = RoundedCornerShape(12.dp)
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            years.forEach { year ->
                DropdownMenuItem(
                    text = { Text(year) },
                    onClick = {
                        onYearSelected(year)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun StateDropdown(selectedState: String, onStateSelected: (String) -> Unit, lightMode: Boolean = false, accentColor: Color = Color(0xFFFFB800)) {
    var expanded by remember { mutableStateOf(false) }
    val fg = if (lightMode) Color(0xFF1A1A2E) else Color.White
    val border = if (lightMode) fg.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.7f)

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selectedState,
            onValueChange = { },
            readOnly = true,
            label = { Text("State") },
            trailingIcon = {
                Icon(
                    imageVector = if (expanded) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                    contentDescription = "Dropdown",
                    tint = fg,
                    modifier = Modifier.clickable { expanded = !expanded }
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedTextColor = fg,
                focusedTextColor = fg,
                unfocusedBorderColor = border,
                focusedBorderColor = accentColor,
                disabledTextColor = fg,
                disabledBorderColor = border,
                disabledLabelColor = fg.copy(alpha = 0.85f),
                unfocusedLabelColor = fg.copy(alpha = 0.85f),
                focusedLabelColor = accentColor,
                cursorColor = accentColor
            ),
            shape = RoundedCornerShape(12.dp)
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.9f).heightIn(max = 300.dp)
        ) {
            US_STATES.forEach { state ->
                DropdownMenuItem(
                    text = { Text(state) },
                    onClick = {
                        onStateSelected(state)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun ChapterNameDropdown(selectedChapter: String, onChapterSelected: (String) -> Unit, lightMode: Boolean = false, accentColor: Color = Color(0xFFFFB800)) {
    var expanded by remember { mutableStateOf(false) }
    val fg = if (lightMode) Color(0xFF1A1A2E) else Color.White
    val border = if (lightMode) fg.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.7f)

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selectedChapter,
            onValueChange = { },
            readOnly = true,
            label = { Text("Chapter Name (State FBLA)") },
            trailingIcon = {
                Icon(
                    imageVector = if (expanded) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                    contentDescription = "Dropdown",
                    tint = fg,
                    modifier = Modifier.clickable { expanded = !expanded }
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedTextColor = fg,
                focusedTextColor = fg,
                unfocusedBorderColor = border,
                focusedBorderColor = accentColor,
                disabledTextColor = fg,
                disabledBorderColor = border,
                disabledLabelColor = fg.copy(alpha = 0.85f),
                unfocusedLabelColor = fg.copy(alpha = 0.85f),
                focusedLabelColor = accentColor,
                cursorColor = accentColor
            ),
            shape = RoundedCornerShape(12.dp)
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.9f).heightIn(max = 320.dp)
        ) {
            FBLA_STATE_CHAPTERS.forEach { ch ->
                DropdownMenuItem(
                    text = { Text(ch, maxLines = 2) },
                    onClick = {
                        onChapterSelected(ch)
                        expanded = false
                    }
                )
            }
        }
    }
}


@Composable
fun HomeScreen(primaryColor: Color, bgOpacity: Float, isSimplifiedUI: Boolean, textSizeMultiplier: Float, lightMode: Boolean = false, onNavigate: (Int, String?) -> Unit) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    var profile by remember { mutableStateOf<UserProfile?>(null) }
    var showSearch by remember { mutableStateOf(false) }
    var showGeminiChat by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedReminder by remember { mutableStateOf<CompetitionReminder?>(null) }

// Dynamic text colors
    val textColor = if (lightMode) Color.Black else Color.White
    val textColorSecondary = if (lightMode) Color.Black.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.9f)
    val textColorTertiary = if (lightMode) Color.Black.copy(alpha = 0.75f) else Color.White.copy(alpha = 0.85f)

    val tips = listOf(
        "Networking isn't just about collecting contacts; it's about planting relations.",
        "Success is not final, failure is not fatal: it is the courage to continue that counts.",
        "The best way to predict the future is to create it.",
        "Business opportunities are like buses, there's always another one coming.",
        "Build your own dreams, or someone else will hire you to build theirs.",
        "Never give up. Today is hard and tomorrow will be worse, but the day after tomorrow will be sunshine.",
        "Innovation distinguishes between a leader and a follower.",
        "Failure doesn't mean you are a failure it just means you haven't succeeded yet.",
        "Work like there is someone working 24 hours a day to take it all away from you."
    )
    var currentTipIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            db.collection("users").document(uid).addSnapshotListener { snapshot, _ ->
                profile = snapshot?.toObject(UserProfile::class.java)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
// Background — white in light mode, dark blue in dark mode
        Box(modifier = Modifier.fillMaxSize().background(
            if (lightMode) Color.White else Color(0xFF0A1628)
        ))
        if (!lightMode) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = bgOpacity)))
        }

        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(24.dp)) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "WELCOME, ${profile?.name?.split(" ")?.get(0)?.uppercase() ?: "MEMBER"}",
                            style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 4.sp, fontWeight = FontWeight.Bold, color = primaryColor, fontSize = (MaterialTheme.typography.labelLarge.fontSize.value * textSizeMultiplier).sp)
                        )
                        Text(text = "FBLA Hub Dashboard", style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Black, color = textColor, fontSize = (MaterialTheme.typography.displaySmall.fontSize.value * textSizeMultiplier).sp))
                    }
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(primaryColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                            .border(1.dp, primaryColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .clickable { showGeminiChat = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✦", color = primaryColor, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            if (profile?.reminders?.isNotEmpty() == true) {
                itemsIndexed(profile!!.reminders) { index, reminder ->
// Days calculation
                    val daysLeft = if (reminder.timestamp > System.currentTimeMillis()) {
                        val cal = Calendar.getInstance()
                        cal.timeInMillis = System.currentTimeMillis()
                        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
                        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                        val todayStart = cal.timeInMillis
                        val eventCal = Calendar.getInstance()
                        eventCal.timeInMillis = reminder.timestamp
                        eventCal.set(Calendar.HOUR_OF_DAY, 0); eventCal.set(Calendar.MINUTE, 0)
                        eventCal.set(Calendar.SECOND, 0); eventCal.set(Calendar.MILLISECOND, 0)
                        TimeUnit.MILLISECONDS.toDays(eventCal.timeInMillis - todayStart)
                    } else 0

                    Card(
                        onClick = { selectedReminder = reminder },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                            .animateContentSize(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                )
                            ),
                        colors = CardDefaults.cardColors(containerColor = primaryColor.copy(alpha = 0.15f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, primaryColor.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.AutoMirrored.Filled.EventNote, null, tint = primaryColor, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("UPCOMING COMPETITION", color = textColorSecondary, style = MaterialTheme.typography.labelSmall.copy(fontSize = (MaterialTheme.typography.labelSmall.fontSize.value * textSizeMultiplier).sp))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(reminder.name, color = textColor, style = MaterialTheme.typography.titleLarge.copy(fontSize = (MaterialTheme.typography.titleLarge.fontSize.value * textSizeMultiplier).sp), fontWeight = FontWeight.Bold)
                            Text(reminder.date, color = textColorSecondary, style = MaterialTheme.typography.bodyMedium.copy(fontSize = (MaterialTheme.typography.bodyMedium.fontSize.value * textSizeMultiplier).sp))
                            Spacer(modifier = Modifier.height(16.dp))
                            LinearProgressIndicator(
                                progress = { (100 - daysLeft.coerceIn(0, 100)) / 100f },
                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                                color = primaryColor,
                                trackColor = if (lightMode) primaryColor.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.1f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("$daysLeft DAYS REMAINING", color = primaryColor, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black, fontSize = (MaterialTheme.typography.labelLarge.fontSize.value * textSizeMultiplier).sp))
                        }
                    }
                }
            } else {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = if (lightMode) Color(0xFFF0F4FF) else Color.White.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.AutoMirrored.Filled.EventNote, null, tint = textColor.copy(alpha = 0.65f), modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("No Active Reminders", color = textColorTertiary, style = TextStyle(fontSize = (14 * textSizeMultiplier).sp))
                            Text("Set one in the Events tab", color = textColor.copy(alpha = 0.65f), style = MaterialTheme.typography.bodySmall.copy(fontSize = (MaterialTheme.typography.bodySmall.fontSize.value * textSizeMultiplier).sp))
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(40.dp))
                Card(
                    onClick = { currentTipIndex = (currentTipIndex + 1) % tips.size },
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = primaryColor),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(Icons.Default.Lightbulb, null, tint = Color.Black)
                            Icon(
                                Icons.Default.TouchApp,
                                contentDescription = "Tap to change",
                                tint = Color.Black
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("LEADERSHIP TIP", color = Color.Black.copy(alpha = 0.8f), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = (MaterialTheme.typography.labelSmall.fontSize.value * textSizeMultiplier).sp))
                        Spacer(modifier = Modifier.height(8.dp))
                        AnimatedContent(
                            targetState = tips[currentTipIndex],
                            transitionSpec = {
                                fadeIn(animationSpec = tween(500)) + slideInVertically(
                                    initialOffsetY = { it / 2 },
                                    animationSpec = tween(500)
                                ) togetherWith fadeOut(animationSpec = tween(300))
                            },
                            label = "tip_transition"
                        ) { tip ->
                            Text(
                                text = tip,
                                color = Color.Black,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Serif,
                                    fontSize = (MaterialTheme.typography.bodyLarge.fontSize.value * textSizeMultiplier).sp
                                )
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

// Global Search Dialog
    if (showSearch) {
        AlertDialog(
            onDismissRequest = { showSearch = false },
            title = {
                Text("Search", color = textColor, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Search anything...", color = textColorSecondary) },
                        placeholder = { Text("Events, News, Resources, Connections...", color = textColor.copy(alpha = 0.75f)) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = primaryColor)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = textColor.copy(alpha = 0.9f))
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor,
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = textColor.copy(alpha = 0.55f),
                            cursorColor = primaryColor,
                            unfocusedLabelColor = textColorSecondary,
                            focusedLabelColor = primaryColor
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

// Search Results
                    if (searchQuery.isNotEmpty()) {
                        val query = searchQuery.lowercase()
                        val results = buildList {
// Tab/Screen results
                            if ("profile".contains(query) || "account".contains(query) || "settings".contains(query)) {
                                add(SearchResult("Profile & Settings", "View your profile and app settings", 0, null, Icons.Default.Person))
                            }
                            if ("events".contains(query) || "calendar".contains(query) || "competition".contains(query) || "reminder".contains(query)) {
                                add(SearchResult("Events & Calendar", "Manage competitions and reminders", 1, null, Icons.Default.Event))
                            }
                            if ("messages".contains(query) || "chat".contains(query) || "messaging".contains(query) || "dm".contains(query)) {
                                add(SearchResult("Messages", "Chat with your connections", 0, "messages", Icons.Default.Chat))
                            }
                            if ("news".contains(query) || "updates".contains(query) || "articles".contains(query)) {
                                add(SearchResult("FBLA News", "Latest FBLA news and updates", 3, null, Icons.AutoMirrored.Filled.Article))
                            }
                            if ("resources".contains(query) || "social media".contains(query) || "tools".contains(query)) {
                                add(SearchResult("Resources", "Helpful resources and social media", 4, null, Icons.AutoMirrored.Filled.MenuBook))
                            }
                            if ("connections".contains(query) || "peers".contains(query) || "network".contains(query) || "friends".contains(query)) {
                                add(SearchResult("My Connections", "View your network", 0, "my_connections", Icons.Default.People))
                            }
                            if ("find peers".contains(query) || "search users".contains(query) || "find members".contains(query)) {
                                add(SearchResult("Find Peers", "Search for FBLA members", 0, "find_links", Icons.Default.Search))
                            }
                            if ("requests".contains(query) || "pending".contains(query)) {
                                add(SearchResult("Connection Requests", "View pending requests", 0, "connection_requests", Icons.Default.PersonAdd))
                            }

// Search events from profile
                            profile?.reminders?.forEach { reminder ->
                                if (reminder.name.lowercase().contains(query) || reminder.date.lowercase().contains(query)) {
                                    add(SearchResult(reminder.name, "Event on ${reminder.date}", 1, null, Icons.Default.EmojiEvents))
                                }
                            }
                        }

                        if (results.isNotEmpty()) {
                            LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                                items(results.size) { index ->
                                    val result = results[index]
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        onClick = {
                                            onNavigate(result.tabIndex, result.subScreen)
                                            showSearch = false
                                            searchQuery = ""
                                        },
                                        colors = CardDefaults.cardColors(containerColor = if (lightMode) Color(0xFFE8EDF5) else Color(0xFF2A2A2A))
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                result.icon,
                                                contentDescription = null,
                                                tint = primaryColor,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    result.title,
                                                    color = textColor,
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.bodyLarge
                                                )
                                                Text(
                                                    result.subtitle,
                                                    color = textColorSecondary,
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }
                                            Icon(
                                                Icons.AutoMirrored.Filled.ArrowForward,
                                                contentDescription = null,
                                                tint = textColor.copy(alpha = 0.75f),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.SearchOff,
                                        contentDescription = null,
                                        tint = textColor.copy(alpha = 0.65f),
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "No results found",
                                        color = textColorSecondary,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    } else {
// Show quick shortcuts when no query
                        Text("Quick Access", color = primaryColor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(8.dp))

                        listOf(
                            SearchResult("Messages", "Chat with connections", 0, "messages", Icons.Default.Chat),
                            SearchResult("Events & Calendar", "Manage competitions", 1, null, Icons.Default.Event),
                            SearchResult("Find Peers", "Search for members", 0, "find_links", Icons.Default.Search),
                            SearchResult("FBLA News", "Latest updates", 3, null, Icons.AutoMirrored.Filled.Article),
                            SearchResult("Resources", "Tools & social media", 4, null, Icons.AutoMirrored.Filled.MenuBook)
                        ).forEach { result ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                onClick = {
                                    onNavigate(result.tabIndex, result.subScreen)
                                    showSearch = false
                                },
                                colors = CardDefaults.cardColors(containerColor = if (lightMode) Color(0xFFE8EDF5) else Color(0xFF2A2A2A))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        result.icon,
                                        contentDescription = null,
                                        tint = primaryColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            result.title,
                                            color = textColor,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            result.subtitle,
                                            color = textColorTertiary,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSearch = false }) {
                    Text("CLOSE", color = primaryColor, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = if (lightMode) Color.White else Color(0xFF1A1A1A),
            titleContentColor = if (lightMode) Color(0xFF1A1A2E) else Color.White,
            textContentColor = if (lightMode) Color(0xFF1A1A2E) else Color.White
        )
    }

// ── REMINDER DETAIL OVERLAY ────────────────────────────────────────────
    selectedReminder?.let { reminder ->
        val daysLeftDetail = if (reminder.timestamp > System.currentTimeMillis()) {
            val cal = Calendar.getInstance()
            cal.timeInMillis = System.currentTimeMillis()
            cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
            val todayStart = cal.timeInMillis
            val eventCal = Calendar.getInstance()
            eventCal.timeInMillis = reminder.timestamp
            eventCal.set(Calendar.HOUR_OF_DAY, 0); eventCal.set(Calendar.MINUTE, 0)
            eventCal.set(Calendar.SECOND, 0); eventCal.set(Calendar.MILLISECOND, 0)
            TimeUnit.MILLISECONDS.toDays(eventCal.timeInMillis - todayStart)
        } else 0L

        val urgencyColorDetail = when {
            daysLeftDetail <= 7  -> Color(0xFFFF4444)
            daysLeftDetail <= 30 -> Color(0xFFFF9800)
            else                 -> primaryColor
        }
        val urgencyLabel = when {
            daysLeftDetail <= 0  -> "TODAY"
            daysLeftDetail == 1L -> "TOMORROW"
            daysLeftDetail <= 7  -> "THIS WEEK"
            daysLeftDetail <= 30 -> "THIS MONTH"
            else                 -> "UPCOMING"
        }

// Dim scrim
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable { selectedReminder = null }
        )

// Detail panel anchored to bottom
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(24.dp, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(
                        if (lightMode) Color.White else Color(0xFF0D1B35),
                        RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                    )
                    .clickable { /* consume clicks so scrim doesn't fire */ }
                    .padding(28.dp)
            ) {
// Drag handle
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .background(
                            if (lightMode) Color(0xFFCCCCCC) else Color.White.copy(alpha = 0.25f),
                            RoundedCornerShape(2.dp)
                        )
                        .align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(24.dp))

// Urgency badge + label row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(urgencyColorDetail.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            urgencyLabel,
                            color = urgencyColorDetail,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            letterSpacing = 1.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        "COMPETITION REMINDER",
                        color = if (lightMode) Color(0xFF1A1A2E).copy(alpha = 0.5f) else Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

// Event name
                Text(
                    reminder.name,
                    color = if (lightMode) Color(0xFF1A1A2E) else Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = (22 * textSizeMultiplier).sp
                )
                Spacer(modifier = Modifier.height(6.dp))

// Date row with icon
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CalendarToday, null,
                        tint = urgencyColorDetail,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        reminder.date,
                        color = if (lightMode) Color(0xFF1A1A2E).copy(alpha = 0.75f) else Color.White.copy(alpha = 0.8f),
                        fontSize = (14 * textSizeMultiplier).sp
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

// Days remaining + progress bar
                Text(
                    if (daysLeftDetail <= 0) "EVENT IS TODAY!" else "$daysLeftDetail DAYS REMAINING",
                    color = urgencyColorDetail,
                    fontWeight = FontWeight.Black,
                    fontSize = (13 * textSizeMultiplier).sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { (100 - daysLeftDetail.coerceIn(0, 100)).toFloat() / 100f },
                    modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape),
                    color = urgencyColorDetail,
                    trackColor = if (lightMode) Color(0xFFE0E0E0) else Color.White.copy(alpha = 0.12f)
                )

                Spacer(modifier = Modifier.height(20.dp))

                OutlinedButton(
                    onClick = {
                        shareCompetition(context, reminder.name, reminder.date, "")
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, primaryColor),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = primaryColor)
                ) {
                    Icon(Icons.Default.Share, null, tint = primaryColor, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SHARE (Instagram, X, Facebook…)", fontWeight = FontWeight.Bold, style = TextStyle(fontSize = (12 * textSizeMultiplier).sp))
                }

                Spacer(modifier = Modifier.height(16.dp))

// CLOSE + DELETE buttons
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { selectedReminder = null },
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            1.dp,
                            if (lightMode) Color(0xFF1A1A2E).copy(alpha = 0.3f) else Color.White.copy(alpha = 0.3f)
                        )
                    ) {
                        Text(
                            "CLOSE",
                            color = if (lightMode) Color(0xFF1A1A2E) else Color.White,
                            fontWeight = FontWeight.Bold,
                            style = TextStyle(fontSize = (13 * textSizeMultiplier).sp)
                        )
                    }
                    Button(
                        onClick = {
                            val auth2 = FirebaseAuth.getInstance()
                            val db2 = FirebaseFirestore.getInstance()
                            auth2.currentUser?.uid?.let { uid ->
                                db2.collection("users").document(uid).get()
                                    .addOnSuccessListener { doc ->
                                        @Suppress("UNCHECKED_CAST")
                                        val current = doc.get("reminders") as? List<Map<String, Any>> ?: emptyList()
                                        val filtered = current.filter { it["name"] != reminder.name || it["date"] != reminder.date }
                                        db2.collection("users").document(uid).update("reminders", filtered)
                                    }
                            }
                            if (reminder.calendarEventId != -1L &&
                                ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED) {
                                try {
                                    context.contentResolver.delete(
                                        CalendarContract.Events.CONTENT_URI,
                                        "${CalendarContract.Events._ID} = ?",
                                        arrayOf(reminder.calendarEventId.toString())
                                    )
                                } catch (_: Exception) {}
                            }
                            selectedReminder = null
                        },
                        modifier = Modifier.weight(1f).height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC143C)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Delete, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "DELETE",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            style = TextStyle(fontSize = (13 * textSizeMultiplier).sp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
// ── END REMINDER DETAIL OVERLAY ────────────────────────────────────────

// Gemini Chat Overlay
    if (showGeminiChat) {
        GeminiChatOverlay(
            onDismiss = { showGeminiChat = false },
            primaryColor = primaryColor,
            lightMode = lightMode
        )
    }
}
// ─────────────────────────────────────────────────────────────────────────────
// FBLA HUB AI CHATBOT
// ─────────────────────────────────────────────────────────────────────────────

data class GeminiMessage(val text: String, val isUser: Boolean)

suspend fun callGeminiAPI(messages: List<GeminiMessage>, userQuery: String): String {
    return withContext(Dispatchers.IO) {
        try {
            val data = hashMapOf(
                "messages" to messages.map { mapOf("text" to it.text, "isUser" to it.isUser) },
                "query" to userQuery
            )
            val result = FirebaseFunctions.getInstance()
                .getHttpsCallable("chatWithAI")
                .call(data)
                .await()
            @Suppress("UNCHECKED_CAST")
            val map = result.getData() as? Map<String, Any>
            map?.get("reply") as? String ?: fblaAIResponse(userQuery)
        } catch (e: Exception) {
            fblaAIResponse(userQuery)
        }
    }
}

fun fblaAIResponse(query: String): String {
    val q = query.lowercase().trim()

    // ── Greetings & Small Talk ─────────────────────────────────────────────
    if (q.matches(Regex("(hi+|hello+|hey+|sup|what'?s up|howdy|yo|hiya|heya|helo|hii+|hiiii*).*")))
        return "Hey! 👋 I'm FBLA Hub AI. Ask me anything about FBLA competitions, this app, conferences, or career tips!"

    if (q.matches(Regex("(good morning|good afternoon|good evening|gm|gn|good night).*")))
        return "Hey, good to see you! 😊 Hope your day's going well. What can I help you with?"

    if (q.contains("how are you") || q.contains("how r u") || q.contains("how are u") || q.contains("u good") || q.contains("you good"))
        return "Doing great, thanks! 😄 Ready to help you crush your FBLA goals. What do you need?"

    if (q.contains("what can you do") || q.contains("what do you do") || q.contains("help me") && q.length < 15 || q == "help" || q == "?")
        return "I can help you with:\n\n📱 **This App** — How to use any screen (Home, Events, News, Resources, Profile, Messages, Settings)\n🏆 **FBLA Competitions** — Event prep, study tips for any event\n📅 **Conferences** — Regional, State (SLC), and National (NLC)\n💼 **Career & Business** — Resume, LinkedIn, networking, interviews\n🎓 **FBLA Basics** — How to join, officer roles, membership\n\nJust ask anything!"

    if (q.contains("thank") || q.contains("thx") || q.contains("ty") || q.contains("thanks"))
        return "No problem! Good luck with everything — you've got this! 🙌"

    if (q.contains("bye") || q.contains("goodbye") || q.contains("see ya") || q.contains("later") || q.contains("peace"))
        return "Later! Come back anytime. Good luck! 👋"

    if (q.contains("ok") && q.length < 6 || q == "okay" || q == "k" || q == "cool" || q == "got it" || q == "nice" || q == "lol" || q == "haha" || q == "lmao")
        return "😄 Anything else I can help with?"

    if (q.contains("you're great") || q.contains("ur great") || q.contains("you're awesome") || q.contains("this is helpful") || q.contains("love this") || q.contains("this is cool"))
        return "Appreciate it! 🙏 Let me know if you need anything else."

    if (q.contains("who are you") || q.contains("what are you") || q.contains("who made you"))
        return "I'm FBLA Hub AI — a built-in assistant for this app! I'm here to help you navigate the app, prep for FBLA competitions, and get career-ready. Ask me anything!"

    if (q.contains("are you real") || q.contains("are you a bot") || q.contains("are you ai") || q.contains("are you human"))
        return "I'm an AI built into FBLA Hub! Not human, but I know my FBLA stuff. 😄 What do you need help with?"

    if (q.contains("bored") || q.contains("nothing to do"))
        return "No time for boredom when there's FBLA prep to do! 😄 Want study tips for a competition event, or want to explore the app?"

    // ── App Navigation Help ────────────────────────────────────────────────
    if (q.contains("home") && (q.contains("screen") || q.contains("tab") || q.contains("page") || q.contains("what")))
        return "The **Home** screen (center tab 🏠) is your dashboard! It shows:\n\n• Your upcoming competition reminders with countdown\n• A motivational business tip\n• Quick-access search (tap the ✦ icon top-right) to jump to any screen\n• The AI chat button (that's me! 😄)\n\nTip: tap any reminder card on Home to see full event details."

    if ((q.contains("event") || q.contains("calendar")) && (q.contains("screen") || q.contains("tab") || q.contains("how") || q.contains("page") || q.contains("where")))
        return "The **Events & Calendar** tab 📅 (second from left) lets you:\n\n• Browse FBLA competitions filtered by your state\n• Tap dates on the calendar to filter events for that day\n• Add reminders for events you're entering\n• See a countdown for each upcoming event\n\nYour reminders also show up on the Home screen dashboard!"

    if (q.contains("news") && (q.contains("screen") || q.contains("tab") || q.contains("how") || q.contains("page") || q.contains("where")))
        return "The **News** tab 📰 (second from right) shows live articles pulled from FBLA.org.\n\n• Articles are auto-tagged by category (National, Conference, etc.)\n• Tap any article to read the full thing\n• Use the share button to send an article to someone in one tap\n• News updates automatically — no refresh needed!"

    if (q.contains("resource") && (q.contains("screen") || q.contains("tab") || q.contains("how") || q.contains("page") || q.contains("where")))
        return "The **Resources** tab 📚 (far right) has:\n\n• Official FBLA Guidelines\n• FBLA Toolkit and brand assets\n• Dress code info\n• Social media links for national FBLA and your state chapter (Instagram, X/Twitter, etc.)\n\nIt auto-loads resources for your state based on your profile settings!"

    if (q.contains("profile") && (q.contains("screen") || q.contains("tab") || q.contains("how") || q.contains("edit") || q.contains("change") || q.contains("update")))
        return "The **Profile** tab 👤 (far left) is where you manage your account:\n\n• Edit your name, email, chapter, graduation year, and state\n• Upload a profile picture\n• View your connections\n• Toggle your profile between public and private\n\nTap **Edit Profile** to update any of your info. Changes save to your account automatically."

    if (q.contains("message") || q.contains("chat") || q.contains("dm") || q.contains("inbox"))
        return "The **Messages** feature 💬 lets you chat in real time with your connections!\n\n• Go to Profile tab → tap Messages, or use the search bar on Home\n• Messages are instant once you're connected with someone\n• You can only message people you're connected with\n\nTo connect with someone first, use **Find Peers** from the Home search or Profile tab!"

    if (q.contains("find peer") || q.contains("find member") || q.contains("search user") || q.contains("find people") || q.contains("connect with"))
        return "To find and connect with other FBLA members:\n\n1. Go to the **Home** screen and tap the search icon (✦ top right)\n2. Tap **Find Peers** from the quick access list\n3. Search by name or chapter\n4. Tap **Connect** to send a connection request\n\nOnce they accept, you can message each other! 🤝"

    if (q.contains("connection request") || q.contains("pending request") || q.contains("accept") && q.contains("request"))
        return "To see your connection requests:\n\n1. Tap the search icon (✦) on the Home screen\n2. Select **Connection Requests**\n3. Accept or decline incoming requests\n\nOr go to Profile tab and look for the requests section. Once accepted, you can message each other!"

    if (q.contains("setting") && (q.contains("screen") || q.contains("tab") || q.contains("where") || q.contains("how") || q.contains("find")))
        return "To access **Settings**:\n\n• Go to the **Profile** tab → tap the settings icon\n\nIn Settings you can:\n🎨 Change your accent color (full color picker!)\n🌑 Adjust background darkness\n🔤 Scale text size\n🧹 Enable Simplified UI (hides background images)\n🌙 Toggle light/dark mode\n🔐 Manage your account and 2FA\n📖 Launch the app tutorial again"

    if (q.contains("customize") || q.contains("theme") || q.contains("color") || q.contains("dark mode") || q.contains("light mode") || q.contains("appearance"))
        return "You can fully customize FBLA Hub in **Settings**! ⚙️\n\n🎨 **Accent Color** — Full color picker to set any color you want\n🌑 **Background Darkness** — Slider to control overlay darkness\n🔤 **Text Size** — Scale text up or down\n🧹 **Simplified UI** — Hides background images for a cleaner look\n☀️ **Light/Dark Mode** — Switch between modes\n\nGo to Profile tab → Settings to find all of these!"

    if (q.contains("reminder") && (q.contains("add") || q.contains("set") || q.contains("create") || q.contains("how")))
        return "To add a competition reminder:\n\n1. Go to the **Events** tab 📅\n2. Browse or find the competition you're entering\n3. Tap the event and select **Add Reminder**\n4. It'll appear on your Home dashboard with a countdown!\n\nYou can tap any reminder on the Home screen to see full event details and remove it when done."

    if (q.contains("search") && (q.contains("how") || q.contains("use") || q.contains("app")))
        return "The app's **Search** lets you jump anywhere fast!\n\n• Tap the **✦** icon on the top-right of the Home screen\n• Type to search events, screens, news, connections\n• Or use the quick access shortcuts (Messages, Events, Find Peers, News, Resources) without typing\n\nYou can search for your own saved reminders too!"

    if (q.contains("tutorial") || q.contains("how to use") || q.contains("tour") || q.contains("walkthrough"))
        return "There's a built-in app tour! 🎓\n\nTo launch it:\n• Go to **Profile** tab → **Settings** → **View Tutorial**\n\nThe tutorial walks you through every screen: Home, Messages, Events, News Feed, Resources, and Customization. Great if you're new or want a refresher!"

    if (q.contains("2fa") || q.contains("two factor") || q.contains("verification code") || q.contains("login code"))
        return "FBLA Hub uses **2-Factor Authentication (2FA)** for extra security! 🔐\n\nWhen you log in:\n1. Enter your email and password\n2. A verification code is sent to your email\n3. Enter the code to complete login\n\nIf you're having trouble with 2FA, check your spam folder for the code email, or try signing in again to request a new code."

    if (q.contains("log out") || q.contains("logout") || q.contains("sign out"))
        return "To log out:\n\n• Go to the **Profile** tab → **Settings** → scroll down to find **Sign Out**\n\nYour data is saved to your account, so everything will be there when you log back in!"

    if (q.contains("delete account") || q.contains("delete my account"))
        return "To delete your account:\n\n• Go to **Profile** tab → **Settings** → scroll to **Delete Account**\n• You'll need to type a confirmation to prevent accidental deletion\n\n⚠️ This is permanent and removes all your data, connections, and messages."

    if (q.contains("public profile") || q.contains("private profile") || q.contains("profile visibility"))
        return "You can control who sees your profile!\n\n• Go to **Profile** tab\n• Look for the **Public/Private** toggle\n• **Public** = other FBLA Hub members can find and view your profile\n• **Private** = only your connections can see your details\n\nYou can change this anytime!"

    // ── What is FBLA ───────────────────────────────────────────────────────
    if (q.contains("what is fbla") || q.contains("what's fbla") || q.contains("about fbla") || q.contains("explain fbla") || q.contains("tell me about fbla"))
        return "FBLA (Future Business Leaders of America) is the largest student business organization in the world with 230,000+ members! 🏆\n\nIt helps high school students:\n• Develop real business and leadership skills\n• Compete in 70+ events (tech, finance, speaking, marketing, and more)\n• Network with students and professionals nationwide\n• Prepare for college and careers\n\nMembers compete at Regional → State → National levels. It's genuinely one of the best things you can put on a college app or resume!"

    if (q.contains("how do i join") || q.contains("how to join") || q.contains("sign up") || q.contains("membership") || q.contains("how do i become"))
        return "To join FBLA:\n\n1. Find out if your school has a chapter — ask a business teacher or counselor\n2. Talk to the chapter adviser to sign up\n3. Pay membership dues (usually \$20–\$30/year)\n4. If your school doesn't have a chapter, you can help start one!\n\nVisit fbla.org for official membership info. 📝"

    if (q.contains("fbla.org") || q.contains("official website") || q.contains("fbla website"))
        return "The official FBLA website is **fbla.org** — you'll find competition guidelines, event descriptions, membership info, study guides, and conference dates there. 🌐"

    // ── Competitions Overview ──────────────────────────────────────────────
    if ((q.contains("competition") || q.contains("event") || q.contains("compete")) &&
        (q.contains("list") || q.contains("what") || q.contains("types") || q.contains("kind") || q.contains("all")))
        return "FBLA has 70+ competitive events! Here's a breakdown:\n\n📊 **Business & Finance:** Accounting, Banking & Finance, Economics, Personal Finance\n💻 **Tech:** Computer Science, Coding & Programming, Cybersecurity, Digital Storytelling\n🎤 **Speaking:** Public Speaking, Impromptu Speaking, Business Presentation\n✍️ **Written:** Business Plan, Business Report, Research Paper\n🧠 **Knowledge Tests:** Business Law, Intro to Business, Management\n👥 **Team Events:** Business Ethics, Community Service, Marketing\n\nWhich event are you in? I can give you specific prep tips!"

    if ((q.contains("how") || q.contains("tips")) && (q.contains("prepare") || q.contains("study") || q.contains("practice")) && (q.contains("competition") || q.contains("event")))
        return "General competition prep tips:\n\n📚 Download the official study guide for your event from fbla.org\n🧠 Take practice tests — past tests are on the FBLA website\n📝 Know the format (objective test, presentation, role play, written report, etc.)\n⏱️ Practice time management — presentations are usually 5–7 min\n👔 Dress professionally on competition day\n🤝 For team events, rehearse with your partner/team\n\nWhat specific event are you prepping for? I'll give you targeted tips!"

    // ── Specific Events ────────────────────────────────────────────────────
    if (q.contains("public speaking"))
        return "**Public Speaking** — a prepared 5–7 min speech on a business topic.\n\n✅ Pick a topic you genuinely care about\n✅ Structure: strong intro → 3 main points → memorable conclusion\n✅ Practice out loud at least 10 times\n✅ Record yourself — catch filler words (um, like, uh)\n✅ Make eye contact, vary your pace and tone\n✅ No notes allowed during delivery!\n\nPro tip: judges love passion and a clear takeaway message."

    if (q.contains("business plan"))
        return "**Business Plan** — one of FBLA's most popular events! Create a full plan for a new or existing business.\n\n📋 Required sections:\n• Executive Summary\n• Company Description\n• Market Analysis\n• Products/Services\n• Marketing Strategy\n• Financial Projections\n• Management Team\n\n✅ Make financials realistic and detailed\n✅ Know your target market inside out\n✅ Prepare for tough judge Q&A\n✅ Use charts/graphs for financial data\n✅ Keep the executive summary tight — judges read it first"

    if (q.contains("computer science") || q.contains("coding") || q.contains("programming") && q.contains("fbla"))
        return "**Computer Science / Coding** events:\n\n📚 Study topics:\n• Data structures (arrays, linked lists, stacks, queues)\n• Algorithms (sorting, Big O notation)\n• OOP (classes, inheritance, polymorphism)\n• Basic SQL and databases\n• Networking fundamentals\n\n✅ Practice on LeetCode, HackerRank, or CodeSignal\n✅ Review past FBLA CS objective tests on fbla.org\n✅ Know Java or Python well\n✅ Study the official FBLA CS study guide"

    if (q.contains("accounting"))
        return "**Accounting** event prep:\n\n📚 Key topics:\n• Journal entries & T-accounts\n• Income statement, balance sheet, trial balance\n• Payroll and depreciation\n• Bank reconciliation\n• Accounting equation: Assets = Liabilities + Equity\n\n✅ Know debits vs. credits cold\n✅ Practice objective tests from fbla.org\n✅ Review financial statement preparation\n✅ Study accounting vocabulary"

    if (q.contains("entrepreneurship") || q.contains("entrepreneur"))
        return "**Entrepreneurship** events test your ability to develop and pitch business ideas!\n\n📚 Key concepts:\n• Identifying market opportunities\n• Value proposition and differentiators\n• Revenue models and pricing\n• SWOT analysis\n• Startup costs and break-even analysis\n• Elevator pitch\n\n✅ Study real startup stories\n✅ Practice your pitch out loud\n✅ Know your financials\n✅ Be ready for judge Q&A about your idea"

    if (q.contains("marketing"))
        return "**Marketing** events:\n\n📚 Key topics:\n• The 4 P's: Product, Price, Place, Promotion\n• Target market & segmentation\n• Branding and positioning\n• Digital marketing (social, SEO, email)\n• Market research\n• Consumer behavior\n\n✅ Study real ad campaigns\n✅ Know current social media trends\n✅ Practice case study analysis\n✅ Understand data-driven marketing decisions"

    if (q.contains("cybersecurity") || q.contains("cyber security"))
        return "**Cybersecurity** event prep:\n\n📚 Key topics:\n• Attack types (phishing, malware, DDoS, SQL injection)\n• Encryption and cryptography basics\n• CIA Triad: Confidentiality, Integrity, Availability\n• Firewalls, VPNs, intrusion detection\n• Ethical hacking basics\n• Security frameworks (NIST, ISO 27001)\n\n✅ CompTIA Security+ prep materials overlap a lot with FBLA Cybersecurity\n✅ Stay current on recent cyber incidents\n✅ Practice FBLA objective tests"

    if (q.contains("economics") || q.contains("econ"))
        return "**Economics** event prep:\n\n📚 Key topics:\n• Supply and demand\n• GDP, inflation, unemployment\n• Monetary vs. fiscal policy\n• Market structures (monopoly, oligopoly, perfect competition)\n• International trade\n• Business cycles\n\n✅ AP Econ materials overlap well with FBLA Economics\n✅ Read economic news (WSJ, Bloomberg)\n✅ Know key indicators and what they signal\n✅ Practice FBLA objective tests on fbla.org"

    if (q.contains("personal finance") || q.contains("finance event"))
        return "**Personal Finance** event prep:\n\n📚 Key topics:\n• Budgeting and savings strategies\n• Credit scores and debt management\n• Types of investments (stocks, bonds, mutual funds, ETFs)\n• Insurance basics (health, auto, life)\n• Taxes (W-2, 1040, deductions)\n• Retirement accounts (401k, IRA, Roth IRA)\n\n✅ Khan Academy has great free personal finance content\n✅ Practice FBLA objective tests\n✅ Understand compound interest — it shows up a lot"

    if (q.contains("business law") || q.contains("business ethics"))
        return "**Business Law / Business Ethics** prep:\n\n📚 Key topics for Business Law:\n• Contract law (offer, acceptance, consideration)\n• Employment law basics\n• Intellectual property (patents, trademarks, copyrights)\n• Business structures (LLC, Corp, Partnership)\n• Consumer protection laws\n\n📚 For Business Ethics:\n• Ethical decision-making frameworks\n• Corporate social responsibility\n• Stakeholder vs. shareholder theory\n• Real-world case studies\n\n✅ Study the FBLA study guide for your specific event on fbla.org"

    if (q.contains("impromptu") || q.contains("impromptu speaking"))
        return "**Impromptu Speaking** — you get a random topic and have a short time to prepare a speech on the spot!\n\n✅ Practice the **PREP formula**: Point → Reason → Example → Point (restate)\n✅ Stay calm — judges expect you to think on your feet\n✅ Speak slowly and clearly, especially at the start\n✅ Pick a clear stance right away — don't waffle\n✅ Practice with random topics daily (Google \"random debate topics\")\n✅ 1–2 min is usually the target length\n\nThe more you practice, the faster your brain gets at organizing thoughts!"

    if (q.contains("dress code") || q.contains("what to wear") || q.contains("dress for"))
        return "FBLA has a **professional dress code** for competitions:\n\n👔 **Business Professional** (most competitions):\n• Suit or blazer with dress pants/skirt\n• Collared dress shirt or blouse\n• Dress shoes (closed toe)\n• No jeans, sneakers, or casual wear\n\n👗 **Business Casual** (some chapter events):\n• Dress pants/slacks, khakis\n• Polo or button-down shirt\n• Clean, neat appearance\n\nWhen in doubt, overdress! Judges do notice. Check your specific event guidelines on fbla.org for exact requirements."

    // ── Conferences ────────────────────────────────────────────────────────
    if (q.contains("nlc") || (q.contains("national") && q.contains("conference")))
        return "The **National Leadership Conference (NLC)** is the biggest FBLA event of the year! 🌎\n\n• Held every June/July in a major US city (changes each year)\n• 12,000+ students competing nationwide\n• Must qualify by placing at your State Leadership Conference\n\nAt NLC:\n• Compete against the best in the country\n• Attend keynote speakers and workshops\n• Network like crazy\n• Vote for national FBLA officers\n\nCheck fbla.org for the current year's location and dates!"

    if (q.contains("slc") || (q.contains("state") && q.contains("conference")))
        return "The **State Leadership Conference (SLC)** is your ticket to Nationals! 🏛️\n\n• Held once a year — date and location vary by state\n• Qualify by placing at your Regional/District conference\n• Top finishers advance to the National Leadership Conference (NLC)\n\nCheck your state's FBLA website for exact dates, registration deadlines, and qualifying requirements — they vary a lot by state!"

    if (q.contains("regional") || q.contains("district") && q.contains("conference"))
        return "**Regional/District Conferences** are the first step in FBLA competition! 🏫\n\n• Usually held in fall or early spring\n• Compete against students in your area\n• Top finishers qualify for the State Leadership Conference (SLC)\n\nYour chapter adviser will have specific dates and details for your region!"

    if (q.contains("how to qualify") || q.contains("qualify for") || q.contains("how do i advance"))
        return "FBLA competition has 3 levels — you advance by placing at each:\n\n1️⃣ **Regional/District** → place in top spots → advance to State\n2️⃣ **State Leadership Conference (SLC)** → place in top spots → advance to National\n3️⃣ **National Leadership Conference (NLC)** → compete with best in the country!\n\nQualifying cutoffs vary by state and event. Ask your chapter adviser for your state's specific rules!"

    if (q.contains("when") && (q.contains("deadline") || q.contains("register") || q.contains("date")))
        return "Dates and deadlines vary by state. General timeline:\n\n📅 **Fall** — Local/chapter competitions\n📅 **Winter/Spring** — Regional/District conferences\n📅 **Spring** — State Leadership Conference (SLC)\n📅 **June/July** — National Leadership Conference (NLC)\n\nTo find your state's exact dates:\n1. Go to fbla.org\n2. Navigate to your state chapter's page\n3. Look for the current year's conference calendar\n\nDon't sleep on registration deadlines — they sneak up fast!"

    // ── Networking & Career ────────────────────────────────────────────────
    if (q.contains("network") || q.contains("networking"))
        return "Networking tips for FBLA students:\n\n🤝 **At conferences:**\n• Introduce yourself to competitors — they're future colleagues!\n• Sit next to people you don't know at workshops\n• Exchange contact info and connect on LinkedIn\n• Talk to judges and advisers after events\n\n💼 **Building your network:**\n• Create a LinkedIn profile now (even as a student)\n• Attend local business events\n• Do informational interviews with professionals\n\n📌 Networking = genuine connection, not just collecting contacts. Use this app's **Find Peers** feature to connect with other FBLA students too!"

    if (q.contains("linkedin"))
        return "LinkedIn tips for FBLA students:\n\n✅ Professional headshot (no selfies or casual photos)\n✅ Write a bio mentioning you're an FBLA member\n✅ List your FBLA events, officer roles, and awards\n✅ Connect with classmates, teachers, and business contacts\n✅ Follow companies you're interested in\n✅ Post about your FBLA wins — it gets noticed!\n✅ Engage thoughtfully with posts\n\nRecruiters genuinely look at student profiles. Having one already puts you ahead of most peers. 🌟"

    if (q.contains("resume") || q.contains("résumé"))
        return "Resume tips for FBLA students:\n\n📄 **Key sections:**\n• Contact info\n• Education (include GPA if 3.5+)\n• FBLA involvement (events competed, officer roles, awards placed)\n• Work / internship experience\n• Skills (software, languages)\n• Volunteer/community service\n\n✅ Keep it to ONE page\n✅ Use action verbs: led, managed, developed, increased, built\n✅ Quantify wins: \"Placed 2nd at State in Business Plan\"\n✅ Tailor it to each application\n✅ Proofread carefully — typos kill first impressions"

    if (q.contains("internship") || (q.contains("job") && !q.contains("interview")) || q.contains("career"))
        return "Career tips for FBLA students:\n\n🎯 **Getting internships/jobs:**\n• Use your FBLA network — advisers often know local businesses\n• Check LinkedIn, Indeed, and your school's job board\n• Apply early — summer internships often close Feb/March\n• Reference your FBLA competitions as real experience in interviews\n\n💡 **Skills that stand out:**\n• Public speaking (massive advantage)\n• Microsoft Office / Google Workspace\n• Basic financial literacy\n• Leadership (officer roles count!)\n\n🎓 FBLA experience genuinely impresses both colleges and employers!"

    if (q.contains("interview") || q.contains("job interview"))
        return "Interview tips:\n\n✅ Research the organization beforehand\n✅ Prepare for common questions:\n   • \"Tell me about yourself\"\n   • \"What are your strengths/weaknesses?\"\n   • \"Why do you want this role?\"\n✅ Use the **STAR method** for behavioral questions:\n   Situation → Task → Action → Result\n✅ Dress professionally\n✅ Bring printed resume copies\n✅ Prepare 2–3 thoughtful questions to ask them\n✅ Send a thank-you email within 24 hours\n\nYour FBLA competition stories are great interview material! 🎯"

    if (q.contains("college") || q.contains("application") || q.contains("college app"))
        return "FBLA is a fantastic college application booster! 🎓\n\n• List competitions and placements (especially state/national)\n• Include any officer roles with responsibilities\n• Write about specific skills you developed\n• Use FBLA experiences in your personal essay — real stories stand out\n• Awards and scholarships from FBLA look great on applications\n\nCollege admissions officers recognize FBLA as a serious commitment. A state or national placement is especially impressive!"

    if (q.contains("scholarship") || q.contains("scholarships"))
        return "FBLA offers scholarships for members! 🏅\n\n• FBLA awards scholarships at the national level each year\n• Some state chapters have their own scholarship programs\n• Officer roles and competition placements strengthen scholarship applications\n\nVisit fbla.org → Scholarships section for current opportunities and deadlines. Also check if your state chapter has separate scholarships!"

    // ── Officer Roles ──────────────────────────────────────────────────────
    if (q.contains("officer") || (q.contains("president") && q.contains("fbla")) || q.contains("leadership role"))
        return "FBLA chapter officer positions:\n\n👑 **President** — Runs meetings, leads the chapter, represents at conferences\n📋 **VP** — Assists president, often oversees committees\n💰 **Treasurer** — Manages chapter budget and dues\n📝 **Secretary** — Meeting minutes, communications\n📣 **Reporter/Historian** — Documents chapter activities, social media\n\n💡 To become an officer:\n✅ Be consistently active and show up\n✅ Volunteer for events before you run\n✅ Build relationships with your adviser and members\n✅ Campaign with a clear, specific platform for the chapter"

    // ── Business Knowledge ────────────────────────────────────────────────
    if (q.contains("business tip") || q.contains("business skill") || q.contains("what skills"))
        return "Key business skills every FBLA student should build:\n\n🗣️ **Communication** — Present and write professionally\n💰 **Financial literacy** — Budgets, profit/loss, investing basics\n🤝 **Networking** — Build genuine relationships early\n⏰ **Time management** — Balance everything without burning out\n💡 **Problem solving** — Practice case studies and real scenarios\n📊 **Data analysis** — Basic Excel/spreadsheet skills go a long way\n🌐 **Digital skills** — Social media, basic coding, design tools\n\nFBLA competitions literally test all of these — you're building real, marketable skills! 💪"

    if (q.contains("swot") || q.contains("swot analysis"))
        return "A **SWOT Analysis** is a framework for evaluating a business or idea:\n\n💪 **S — Strengths:** What does the business do well? What advantages does it have?\n⚠️ **W — Weaknesses:** What are the limitations or areas to improve?\n🚀 **O — Opportunities:** External factors the business could capitalize on\n🔥 **T — Threats:** External factors that could hurt the business\n\nSWOT shows up in Business Plan, Entrepreneurship, and many other FBLA events. Always back up each point with specific evidence!"

    if (q.contains("elevator pitch") || q.contains("pitch"))
        return "An **elevator pitch** is a 30–60 second summary of your business idea or yourself.\n\n📋 Structure:\n1. Hook — grab attention immediately\n2. Problem — what problem are you solving?\n3. Solution — your product/service\n4. Value — why it's better/unique\n5. Ask — what do you want from the listener?\n\n✅ Practice until it feels natural, not memorized\n✅ Adapt it based on your audience\n✅ End with a clear call to action\n\nThis skill is tested in Entrepreneurship and Business Plan events!"

    // ── Default Prompt Chips ───────────────────────────────────────────────
    if (q == "fbla competitive events" || q.contains("competitive events"))
        return "FBLA has 70+ competitive events in 6 categories:\n\n📊 **Business & Finance**\nAccounting, Banking & Finance, Economics, Personal Finance, Business Law\n\n💻 **Technology**\nComputer Science, Coding & Programming, Cybersecurity, Digital Storytelling, Website Design\n\n🎤 **Speaking & Presentation**\nPublic Speaking, Impromptu Speaking, Business Presentation, Introduction to Public Speaking\n\n✍️ **Written Events**\nBusiness Plan, Business Report, Research Paper, Future Business Educator\n\n🧠 **Objective Tests**\nIntro to Business, Management, Marketing, Business Ethics, Entrepreneurship\n\n👥 **Team Events**\nBusiness Ethics Team, Community Service Project, Partnership with Business Project\n\nAsk me about any specific event for prep tips! 💪"

    if (q == "networking tips" || q == "networking tip")
        return "Networking tips for FBLA students:\n\n🤝 **At conferences & competitions:**\n• Introduce yourself to competitors — they're future colleagues!\n• Sit next to people you don't know at workshops\n• Talk to judges after events — they love engaged students\n• Exchange LinkedIn info, not just phone numbers\n\n📱 **Online networking:**\n• Set up a LinkedIn profile now (even as a student)\n• Post about your FBLA wins — people notice\n• Connect with alumni from your school who went to college\n• Follow FBLA's official accounts and engage with posts\n\n💡 **The golden rule:**\nNetworking isn't about collecting contacts — it's about building real relationships. Follow up within 48 hours after meeting someone new!\n\nAlso use this app's **Find Peers** feature to connect with other FBLA members! 🙌"

    if (q == "nlc 2026 info" || q.contains("nlc 2026") || q.contains("national 2026"))
        return "The **2026 National Leadership Conference (NLC)** is FBLA's biggest annual event! 🌎\n\n📍 Location and exact dates are announced by FBLA — check fbla.org for the latest confirmed info since details update each year.\n\n**What to expect at NLC:**\n• 12,000+ students competing from across the country\n• Multiple rounds of competition in your event\n• Keynote speakers from the business world\n• Leadership workshops and sessions\n• Voting for national FBLA officers\n• Networking with the best FBLA students nationwide\n\n**How to qualify:**\n• Place in the top finishers at your State Leadership Conference (SLC)\n• Cutoffs vary by state and event\n\nCheck fbla.org → Conferences for confirmed 2026 NLC details! 🏆"

    if (q == "business etiquette" || q.contains("business etiquette") || q.contains("professional etiquette") || q.contains("etiquette"))
        return "Business etiquette every FBLA student should know:\n\n👔 **Professional Appearance**\n• Dress for the role you want, not the one you have\n• Keep clothes neat, pressed, and conservative for business settings\n• Minimal accessories and fragrance at interviews/conferences\n\n🤝 **Meetings & Interactions**\n• Firm handshake, eye contact, and a genuine smile\n• Arrive 5–10 minutes early (on time = late in business)\n• Put your phone away — full attention shows respect\n• Address people formally (Mr./Ms.) until told otherwise\n\n📧 **Communication**\n• Use proper grammar in emails — no slang or emojis\n• Reply within 24 hours\n• Keep emails concise with a clear subject line\n• Always send a thank-you note after interviews\n\n🍽️ **Dining Etiquette**\n• Wait for everyone to be served before eating\n• No phones at the table\n• Order mid-range items, never the most expensive\n• The person who invites pays — but always offer\n\nBusiness etiquette is tested in FBLA's Business Communications and related events! 💼"

    if (q == "leadership skills" || q.contains("leadership skill") || q.contains("how to be a leader") || q.contains("become a leader"))
        return "Leadership skills every FBLA student should build:\n\n🎯 **Communication**\nSpeak clearly, listen actively, and adapt your style to your audience. Practice in FBLA's public speaking events!\n\n🧩 **Decision Making**\nGather info, weigh options, commit to a choice, and learn from outcomes. Don't let perfect be the enemy of good.\n\n🤝 **Team Building**\nDelegate effectively, recognize others' strengths, and create an environment where people want to show up.\n\n⏰ **Time Management**\nPrioritize ruthlessly. Use tools like task lists, time blocking, and deadlines to stay on track.\n\n💡 **Problem Solving**\nApproach problems with curiosity, not panic. Define the problem clearly before jumping to solutions.\n\n🔥 **Initiative**\nLeaders don't wait to be told — they see what needs doing and do it.\n\n📈 **How to practice NOW:**\n• Run for a chapter officer position\n• Volunteer to lead a team event\n• Organize a chapter community service project\n• Mentor newer FBLA members\n\nLeadership shows up in almost every FBLA event — and every college application! 🏆"

    // ── Fallback ───────────────────────────────────────────────────────────
    val suggestions = listOf(
        "Using the app (Home, Events, News, Resources, Profile, Messages, Settings)",
        "Competition events and how to prepare",
        "What FBLA is and how to join",
        "Conference levels (Regional, State/SLC, National/NLC)",
        "Networking, LinkedIn, resume, and career tips",
        "Specific events: Business Plan, Public Speaking, Coding, Accounting, Marketing...",
        "FBLA officer roles and scholarships"
    )
    return "Hmm, not sure about that one! 🤔 I'm best at helping with:\n\n${suggestions.joinToString("\n") { "• $it" }}\n\nTry rephrasing or ask about one of the topics above!"
}

@Composable
fun GeminiChatOverlay(
    onDismiss: () -> Unit,
    primaryColor: Color,
    lightMode: Boolean = false
) {
    val textColor = if (lightMode) Color.Black else Color.White
    val bgColor = if (lightMode) Color.White else Color(0xFF0A1628)
    val bubbleBg = if (lightMode) Color(0xFFE8EDF5) else Color(0xFF1A2540)

    var inputText by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf(listOf(
        GeminiMessage("Hi! I'm your FBLA Hub AI assistant. Ask me anything about FBLA, competitions, networking, or business!", false)
    )) }
    var isLoading by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Using direct REST API - no model object needed

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onDismiss() }
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .align(Alignment.BottomCenter)
                .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { /* consume */ },
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            colors = CardDefaults.cardColors(containerColor = bgColor)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(40.dp).background(primaryColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("\u2726", color = Color.Black, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("FBLA Hub AI", color = textColor, fontWeight = FontWeight.Black, fontSize = 18.sp)
                        Text("FBLA Hub AI", color = primaryColor, fontSize = 12.sp)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = textColor.copy(alpha = 0.7f))
                    }
                }

                HorizontalDivider(color = textColor.copy(alpha = 0.08f))

                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    state = listState,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(messages.size) { index ->
                        val msg = messages[index]
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start
                        ) {
                            if (!msg.isUser) {
                                Box(
                                    modifier = Modifier.size(32.dp).background(primaryColor, CircleShape).align(Alignment.Bottom),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("\u2726", color = Color.Black, fontSize = 14.sp)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Card(
                                colors = CardDefaults.cardColors(containerColor = if (msg.isUser) primaryColor else bubbleBg),
                                shape = RoundedCornerShape(
                                    topStart = 16.dp, topEnd = 16.dp,
                                    bottomStart = if (msg.isUser) 16.dp else 4.dp,
                                    bottomEnd = if (msg.isUser) 4.dp else 16.dp
                                ),
                                modifier = Modifier.widthIn(max = 280.dp)
                            ) {
                                Text(
                                    text = msg.text,
                                    color = if (msg.isUser) Color.Black else textColor,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp
                                )
                            }
                            if (msg.isUser) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier.size(32.dp).background(textColor.copy(alpha = 0.1f), CircleShape).align(Alignment.Bottom),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Person, null, tint = textColor.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }

                    if (isLoading) {
                        item {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                                Box(
                                    modifier = Modifier.size(32.dp).background(primaryColor, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("\u2726", color = Color.Black, fontSize = 14.sp)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = bubbleBg),
                                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        repeat(3) { i ->
                                            val infiniteTransition = rememberInfiniteTransition(label = "dot_$i")
                                            val alpha by infiniteTransition.animateFloat(
                                                initialValue = 0.3f, targetValue = 1f,
                                                animationSpec = infiniteRepeatable(
                                                    animation = tween(500, delayMillis = i * 150),
                                                    repeatMode = RepeatMode.Reverse
                                                ),
                                                label = "dot_alpha_$i"
                                            )
                                            Box(modifier = Modifier.size(8.dp).background(primaryColor.copy(alpha = alpha), CircleShape))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (messages.size <= 1) {
                    androidx.compose.foundation.lazy.LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val suggestions = listOf("FBLA competitive events", "Networking tips", "NLC 2026 info", "Business etiquette", "Leadership skills")
                        items(suggestions.size) { i ->
                            OutlinedButton(
                                onClick = { inputText = suggestions[i] },
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.5f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = primaryColor),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(suggestions[i], fontSize = 12.sp)
                            }
                        }
                    }
                }

                HorizontalDivider(color = textColor.copy(alpha = 0.08f))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Ask me anything...", color = textColor.copy(alpha = 0.5f), fontSize = 14.sp) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textColor, unfocusedTextColor = textColor,
                            focusedBorderColor = primaryColor, unfocusedBorderColor = textColor.copy(alpha = 0.2f),
                            cursorColor = primaryColor
                        ),
                        maxLines = 4
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            val query = inputText.trim()
                            if (query.isNotEmpty() && !isLoading) {
                                messages = messages + GeminiMessage(query, true)
                                inputText = ""
                                isLoading = true
                                scope.launch {
                                    try {
                                        val reply = callGeminiAPI(messages.dropLast(1), query)
                                        messages = messages + GeminiMessage(reply, false)
                                    } catch (e: Exception) {
                                        messages = messages + GeminiMessage(
                                            "Sorry, I hit an error: ${e.message ?: "Unknown error"}. Check your connection and try again.",
                                            false
                                        )
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            }
                        },
                        enabled = inputText.isNotBlank() && !isLoading,
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                if (inputText.isNotBlank() && !isLoading) primaryColor else textColor.copy(alpha = 0.1f),
                                CircleShape
                            )
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = if (inputText.isNotBlank() && !isLoading) Color.Black else textColor.copy(alpha = 0.3f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}



data class SearchResult(
    val title: String,
    val subtitle: String,
    val tabIndex: Int,
    val subScreen: String?,
    val icon: ImageVector
)

@Composable
fun MemberSearchScreen(onBack: () -> Unit, primaryColor: Color, bgOpacity: Float, isSimplifiedUI: Boolean, textSizeMultiplier: Float, lightMode: Boolean = false) {
// Dynamic text colors
    val textColor = if (lightMode) Color.Black else Color.White
    val textColorSecondary = if (lightMode) Color.Black.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.9f)
    val textColorTertiary = if (lightMode) Color.Black.copy(alpha = 0.75f) else Color.White.copy(alpha = 0.85f)

    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var members by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var sentRequests by remember { mutableStateOf<Set<String>>(emptySet()) }
    var connectedUsers by remember { mutableStateOf<Set<String>>(emptySet()) }

    LaunchedEffect(Unit) {
        val currentUid = auth.currentUser?.uid
        if (currentUid != null) {
// Load users
            db.collection("users").addSnapshotListener { snapshot, _ ->
                members = snapshot?.toObjects(UserProfile::class.java)?.filter { it.uid != currentUid } ?: emptyList()
                isLoading = false
            }

// Load sent requests
            db.collection("connectionRequests")
                .whereEqualTo("fromUid", currentUid)
                .whereEqualTo("status", "pending")
                .addSnapshotListener { snapshot, _ ->
                    sentRequests = snapshot?.documents?.mapNotNull { it.getString("toUid") }?.toSet() ?: emptySet()
                }

// Load connections
            db.collection("connections")
                .document(currentUid)
                .addSnapshotListener { snapshot, _ ->
                    connectedUsers = (snapshot?.get("connectedUids") as? List<*>)?.mapNotNull { it as? String }?.toSet() ?: emptySet()
                }
        } else {
            isLoading = false
        }
    }

    val filteredMembers = remember(searchQuery, members) {
        if (searchQuery.isEmpty()) members
        else members.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
// Background — white in light mode, dark blue in dark mode
        Box(modifier = Modifier.fillMaxSize().background(
            if (lightMode) Color.White else Color(0xFF0A1628)
        ))
        if (!lightMode) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = bgOpacity)))
        }

        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp, end = 16.dp, bottom = 16.dp, start = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = textColor)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text("FIND PEERS", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black, fontSize = (MaterialTheme.typography.headlineSmall.fontSize.value * textSizeMultiplier).sp), color = textColor)
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search by name...", color = textColorTertiary, style = TextStyle(fontSize = (14 * textSizeMultiplier).sp)) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = primaryColor) },
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedTextColor = textColor,
                    focusedTextColor = textColor,
                    unfocusedBorderColor = textColor.copy(alpha = 0.45f),
                    focusedBorderColor = primaryColor,
                    cursorColor = primaryColor
                ),
                shape = RoundedCornerShape(12.dp)
            )

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = primaryColor)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                    itemsIndexed(filteredMembers) { _, member ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            colors = CardDefaults.cardColors(containerColor = if (lightMode) Color(0xFFF0F4FF) else Color.White.copy(alpha = 0.1f)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(50.dp).background(primaryColor, CircleShape).clip(CircleShape)) {
                                        if (member.pfpUrl.isNotEmpty()) {
                                            AsyncImage(model = member.pfpUrl, contentDescription = null, contentScale = ContentScale.Crop)
                                        } else {
                                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                Text(member.name.take(1).uppercase(), color = Color.Black, fontWeight = FontWeight.Bold, style = TextStyle(fontSize = (16 * textSizeMultiplier).sp))
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text(member.name, color = textColor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge.copy(fontSize = (MaterialTheme.typography.titleLarge.fontSize.value * textSizeMultiplier).sp))
                                        Text(member.school, color = primaryColor, style = MaterialTheme.typography.bodyMedium.copy(fontSize = (MaterialTheme.typography.bodyMedium.fontSize.value * textSizeMultiplier).sp))
                                    }
                                }

                                if (member.bio.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(text = member.bio, color = textColor.copy(alpha = 0.92f), style = MaterialTheme.typography.bodySmall.copy(fontSize = (MaterialTheme.typography.bodySmall.fontSize.value * textSizeMultiplier).sp))
                                }

                                if (member.reminders.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text("Competitions:", color = textColorSecondary, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = (MaterialTheme.typography.labelSmall.fontSize.value * textSizeMultiplier).sp))
                                    member.reminders.forEach { reminder ->
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                                            Icon(Icons.Default.EmojiEvents, null, tint = primaryColor, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(reminder.name, color = textColorSecondary, style = MaterialTheme.typography.bodySmall.copy(fontSize = (MaterialTheme.typography.bodySmall.fontSize.value * textSizeMultiplier).sp))
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                val isConnected = connectedUsers.contains(member.uid)
                                val requestSent = sentRequests.contains(member.uid)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = {
                                            if (!isConnected && !requestSent) {
                                                scope.launch {
                                                    try {
                                                        val currentUser = auth.currentUser ?: return@launch
                                                        val currentProfile = db.collection("users").document(currentUser.uid).get().await().toObject(UserProfile::class.java)

                                                        val request = ConnectionRequest(
                                                            id = UUID.randomUUID().toString(),
                                                            fromUid = currentUser.uid,
                                                            fromName = currentProfile?.name ?: "",
                                                            fromEmail = currentProfile?.email ?: currentUser.email ?: "",
                                                            fromSchool = currentProfile?.school ?: "",
                                                            fromPfpUrl = currentProfile?.pfpUrl ?: "",
                                                            toUid = member.uid,
                                                            toName = member.name,
                                                            status = "pending",
                                                            timestamp = System.currentTimeMillis()
                                                        )

                                                        db.collection("connectionRequests").document(request.id).set(request).await()
                                                        Toast.makeText(context, "Connection request sent!", Toast.LENGTH_SHORT).show()
                                                    } catch (e: Exception) {
                                                        Log.e("ConnectionRequest", "Error sending request: ${e.message}", e)
                                                        Toast.makeText(context, "Failed to send: ${e.message}", Toast.LENGTH_LONG).show()
                                                    }
                                                }
                                            }
                                        },
                                        modifier = Modifier.weight(1f).height(40.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = when {
                                                isConnected -> Color.Green.copy(alpha = 0.3f)
                                                requestSent -> Color.Gray.copy(alpha = 0.3f)
                                                else -> primaryColor
                                            }
                                        ),
                                        enabled = !isConnected && !requestSent,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(
                                            when {
                                                isConnected -> Icons.Default.Check
                                                requestSent -> Icons.Default.Schedule
                                                else -> Icons.Default.PersonAdd
                                            },
                                            null,
                                            tint = if (isConnected || requestSent) Color.White.copy(alpha = 0.85f) else Color.Black,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            when {
                                                isConnected -> "CONNECTED"
                                                requestSent -> "REQUEST SENT"
                                                else -> "SEND REQUEST"
                                            },
                                            color = if (isConnected || requestSent) Color.White.copy(alpha = 0.85f) else Color.Black,
                                            fontWeight = FontWeight.Bold,
                                            style = TextStyle(fontSize = (12 * textSizeMultiplier).sp)
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            shareMemberProfile(
                                                context = context,
                                                name = member.name,
                                                chapter = member.school,
                                                year = member.year,
                                                state = member.state,
                                                bio = member.bio
                                            )
                                        },
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(
                                                if (lightMode) Color(0xFFE8EDF5) else Color.White.copy(alpha = 0.12f),
                                                RoundedCornerShape(8.dp)
                                            )
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = "Share member", tint = primaryColor)
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

@Composable
fun CalendarView(
    selectedDate: Calendar?,
    onDateSelected: (Calendar) -> Unit,
    eventDates: Set<String>,
    primaryColor: Color,
    textSizeMultiplier: Float,
    lightMode: Boolean = false
) {
    val textColor = if (lightMode) Color.Black else Color.White
    var currentMonth by remember {
        mutableStateOf(Calendar.getInstance(TimeZone.getTimeZone("UTC")))
    }
    var showMonthPicker by remember { mutableStateOf(false) }
    var showYearPicker by remember { mutableStateOf(false) }

    val dateFormat = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }
    val monthYearFormat = remember {
        SimpleDateFormat("MMMM yyyy", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }
    val monthFormat = remember {
        SimpleDateFormat("MMMM", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
// Month/Year Header with navigation
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: prev/next arrows + month-year picker
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    currentMonth = (currentMonth.clone() as Calendar).apply {
                        add(Calendar.MONTH, -1)
                        timeZone = TimeZone.getTimeZone("UTC")
                    }
                }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, tint = textColor)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { showMonthPicker = true }.padding(horizontal = 4.dp)
                ) {
                    Text(
                        text = monthYearFormat.format(currentMonth.time).uppercase(),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = (MaterialTheme.typography.titleMedium.fontSize.value * textSizeMultiplier).sp
                        ),
                        color = textColor
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Icon(Icons.Default.ArrowDropDown, null, tint = primaryColor, modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = {
                    currentMonth = (currentMonth.clone() as Calendar).apply {
                        add(Calendar.MONTH, 1)
                        timeZone = TimeZone.getTimeZone("UTC")
                    }
                }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = textColor)
                }
            }

            // Right: Today chip
            val isAlreadyThisMonth = remember(currentMonth) {
                val now = Calendar.getInstance()
                currentMonth.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                    currentMonth.get(Calendar.MONTH) == now.get(Calendar.MONTH)
            }
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isAlreadyThisMonth) primaryColor.copy(alpha = 0.08f) else primaryColor.copy(alpha = 0.18f),
                border = BorderStroke(1.dp, if (isAlreadyThisMonth) primaryColor.copy(alpha = 0.25f) else primaryColor.copy(alpha = 0.7f)),
                modifier = Modifier.clickable {
                    currentMonth = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                    onDateSelected(Calendar.getInstance())
                }
            ) {
                Text(
                    "TODAY",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    color = if (isAlreadyThisMonth) primaryColor.copy(alpha = 0.5f) else primaryColor,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = (10 * textSizeMultiplier).sp, letterSpacing = 1.sp)
                )
            }
        }

// Day of week headers
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            listOf("S", "M", "T", "W", "T", "F", "S").forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    color = primaryColor,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = (MaterialTheme.typography.labelSmall.fontSize.value * textSizeMultiplier).sp
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

// Calendar grid
        val firstDayOfMonth = (currentMonth.clone() as Calendar).apply {
            timeZone = TimeZone.getTimeZone("UTC")
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val lastDayOfMonth = (currentMonth.clone() as Calendar).apply {
            timeZone = TimeZone.getTimeZone("UTC")
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
        }

        val startDayOfWeek = firstDayOfMonth.get(Calendar.DAY_OF_WEEK) - 1
        val daysInMonth = lastDayOfMonth.get(Calendar.DAY_OF_MONTH)
        val totalCells = ((startDayOfWeek + daysInMonth + 6) / 7) * 7

        Column {
            for (week in 0 until (totalCells / 7)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (day in 0..6) {
                        val cellIndex = week * 7 + day
                        val dayOfMonth = cellIndex - startDayOfWeek + 1

                        if (cellIndex >= startDayOfWeek && dayOfMonth <= daysInMonth) {
                            val cellDate = (currentMonth.clone() as Calendar).apply {
                                set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                set(Calendar.HOUR_OF_DAY, 0)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                                timeZone = TimeZone.getTimeZone("UTC")
                            }
                            val dateString = dateFormat.format(cellDate.time)
                            val hasEvent = eventDates.contains(dateString)
                            val isSelected = selectedDate?.let {
                                dateFormat.format(it.time) == dateString
                            } ?: false

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(4.dp)
                                    .background(
                                        when {
                                            isSelected -> primaryColor
                                            hasEvent -> primaryColor.copy(alpha = 0.2f)
                                            else -> Color.Transparent
                                        },
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { onDateSelected(cellDate) },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = dayOfMonth.toString(),
                                        color = if (isSelected) Color.Black else textColor,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (hasEvent || isSelected) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = (MaterialTheme.typography.bodyMedium.fontSize.value * textSizeMultiplier).sp
                                        )
                                    )
                                    if (hasEvent && !isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .size(4.dp)
                                                .background(primaryColor, CircleShape)
                                        )
                                    }
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                        }
                    }
                }
            }
        }
    }

// Month Picker Dialog
    if (showMonthPicker) {
        AlertDialog(
            onDismissRequest = { showMonthPicker = false },
            containerColor = if (lightMode) Color.White else Color(0xFF1E1E1E),
            title = {
                Text(
                    "Select Month & Year",
                    color = textColor,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
// Year selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            currentMonth = (currentMonth.clone() as Calendar).apply {
                                add(Calendar.YEAR, -1)
                            }
                        }) {
                            Icon(Icons.Default.KeyboardArrowLeft, null, tint = primaryColor)
                        }
                        Text(
                            text = currentMonth.get(Calendar.YEAR).toString(),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = primaryColor,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        IconButton(onClick = {
                            currentMonth = (currentMonth.clone() as Calendar).apply {
                                add(Calendar.YEAR, 1)
                            }
                        }) {
                            Icon(Icons.Default.KeyboardArrowRight, null, tint = primaryColor)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

// Month grid (3x4)
                    val months = listOf(
                        "January", "February", "March", "April",
                        "May", "June", "July", "August",
                        "September", "October", "November", "December"
                    )

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.height(300.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(months) { index, month ->
                            val isCurrentMonth = currentMonth.get(Calendar.MONTH) == index
                            Button(
                                onClick = {
                                    currentMonth = (currentMonth.clone() as Calendar).apply {
                                        set(Calendar.MONTH, index)
                                    }
                                    showMonthPicker = false
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isCurrentMonth) primaryColor else if (lightMode) Color(0xFFE8EDF5) else Color(0xFF2A2A2A),
                                    contentColor = if (isCurrentMonth) Color.Black else textColor
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                            ) {
                                Text(
                                    text = month.take(3),
                                    fontSize = 12.sp,
                                    fontWeight = if (isCurrentMonth) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMonthPicker = false }) {
                    Text("Close", color = textColor.copy(alpha = 0.9f))
                }
            }
        )
    }
}

@Composable
fun EventsPlaceholder(primaryColor: Color, bgOpacity: Float, isSimplifiedUI: Boolean, textSizeMultiplier: Float, lightMode: Boolean = false, userState: String = "") {
// Dynamic text colors
    val textColor = if (lightMode) Color.Black else Color.White
    val textColorSecondary = if (lightMode) Color.Black.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.9f)
    val textColorTertiary = if (lightMode) Color.Black.copy(alpha = 0.75f) else Color.White.copy(alpha = 0.85f)

    var selectedState by remember { mutableStateOf(userState) }
    var expanded by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf<Calendar?>(null) }
    var showListView by remember { mutableStateOf(false) }
    var userReminders by remember { mutableStateOf<List<CompetitionReminder>>(emptyList()) }
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val scope = rememberCoroutineScope()
    var pendingCalEvent by remember { mutableStateOf<FblaEvent?>(null) }
    val calPermLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            pendingCalEvent?.let { ev ->
                val uid = auth.currentUser?.uid ?: return@let
                scope.launch(Dispatchers.IO) {
                    val calId = insertCalendarEvent(context, ev.name, ev.date, ev.location)
                    if (calId != -1L) {
                        val oldReminder = CompetitionReminder(ev.name, ev.date, ev.timestamp)
                        val newReminder = oldReminder.copy(calendarEventId = calId)
                        db.collection("users").document(uid).update("reminders", com.google.firebase.firestore.FieldValue.arrayRemove(oldReminder))
                        db.collection("users").document(uid).update("reminders", FieldValue.arrayUnion(newReminder))
                    }
                }
            }
        }
        pendingCalEvent = null
    }
    val dateFormat = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }
    val displayDateFormat = remember {
        SimpleDateFormat("MMMM dd, yyyy", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }

// Load user reminders
    LaunchedEffect(Unit) {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            db.collection("users").document(uid).addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val data = snapshot?.toObject(UserProfile::class.java)
                userReminders = data?.reminders ?: emptyList()
            }
        }
    }

    val nationalConference = FblaEvent("National Leadership Conference (NLC)", "June 29 - July 2, 2026", "San Antonio, TX", 1782709200000L)
    val eventsByState = remember {
        val statesList = listOf("Alabama", "Alaska", "Arizona", "Arkansas", "California", "Colorado", "Connecticut", "Delaware", "Florida", "Georgia", "Hawaii", "Idaho", "Illinois", "Indiana", "Iowa", "Kansas", "Kentucky", "Louisiana", "Maine", "Maryland", "Massachusetts", "Michigan", "Minnesota", "Mississippi", "Missouri", "Montana", "Nebraska", "Nevada", "New Hampshire", "New Jersey", "New Mexico", "New York", "North Carolina", "North Dakota", "Ohio", "Oklahoma", "Oregon", "Pennsylvania", "Rhode Island", "South Carolina", "South Dakota", "Tennessee", "Texas", "Utah", "Vermont", "Virginia", "Washington", "West Virginia", "Wisconsin", "Wyoming")
        val baseEvents = mapOf(
            "Alabama" to listOf(FblaEvent("District Workshops", "Jan-Feb 2026", "Various Locations"), FblaEvent("State Leadership Conference", "April 9–10, 2026", "Mobile", 1775710800000L)),
            "California" to listOf(FblaEvent("Section Leadership Conferences", "Feb-March 2026", "Various Sections"), FblaEvent("State Leadership Conference", "April 23–26, 2026", "Sacramento", 1776920400000L)),
            "Texas" to listOf(FblaEvent("Area Leadership Conferences", "Jan-Feb 2026", "Areas 1-9"), FblaEvent("State Leadership Conference", "March 25–27, 2026", "Galveston", 1774414800000L)),
            "Wisconsin" to listOf(FblaEvent("Region 1 Leadership Conference", "Feb 7, 2026", "Shell Lake", 1770444000000L), FblaEvent("State Leadership Conference", "April 13–14, 2026", "Green Bay", 1776056400000L))
        )
        statesList.associateWith { (baseEvents[it] ?: listOf(FblaEvent("Regional Leadership Conference", "Jan-Feb 2026", "Various"), FblaEvent("State Leadership Conference", "March-April 2026", "TBD", 1774674000000L))) + nationalConference }
    }
    val states = remember { eventsByState.keys.toList().sorted() }

// Get all events based on selected state (or all states if none selected)
    val relevantEvents = remember(selectedState, eventsByState) {
        if (selectedState.isEmpty()) {
// All events from all states - remove duplicates by name and date
            eventsByState.values.flatten().distinctBy { it.name + it.date }
        } else {
// Events from selected state only
            eventsByState[selectedState] ?: emptyList()
        }
    }

// Calculate event dates for calendar by parsing date strings
    val eventDates = remember(relevantEvents) {
        relevantEvents
            .flatMap { event -> parseEventDates(event.date) }
            .toSet()
    }

// Filter events by selected date (only when not in list view)
    val filteredEvents = remember(selectedDate, relevantEvents, showListView) {
// In list view, show all events regardless of selected date
        if (showListView) {
            relevantEvents
        } else {
            val currentSelectedDate = selectedDate
            if (currentSelectedDate == null) {
                relevantEvents
            } else {
                val selectedDateString = dateFormat.format(currentSelectedDate.time)
                relevantEvents.filter { event ->
// Check if the selected date falls within any of the event's dates
                    val eventDates = parseEventDates(event.date)
                    eventDates.contains(selectedDateString)
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
// Background — white in light mode, dark blue in dark mode
        Box(modifier = Modifier.fillMaxSize().background(
            if (lightMode) Color.White else Color(0xFF0A1628)
        ))
        if (!lightMode) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = bgOpacity)))
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "FUTURE BUSINESS LEADERS",
                    style = MaterialTheme.typography.labelLarge.copy(
                        letterSpacing = 5.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = (MaterialTheme.typography.labelLarge.fontSize.value * textSizeMultiplier).sp
                    ),
                    color = if (lightMode) primaryColor else Color.White.copy(alpha = 0.9f)
                )
                Text(
                    text = if (selectedState.isEmpty()) "Events 2026" else "$selectedState Events",
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = (MaterialTheme.typography.displaySmall.fontSize.value * textSizeMultiplier).sp
                    ),
                    color = textColor
                )
                Spacer(modifier = Modifier.height(20.dp))

                // NLC Countdown card
                val nlcTimestamp = 1782709200000L
                val daysUntilNLC = remember {
                    val today = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                    }
                    val nlc = Calendar.getInstance().apply { timeInMillis = nlcTimestamp
                        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                    }
                    TimeUnit.MILLISECONDS.toDays(nlc.timeInMillis - today.timeInMillis).coerceAtLeast(0)
                }
                // Progress: fraction of year elapsed (Sept 1 2025 → June 29 2026)
                val nlcProgress = remember {
                    val yearStart = Calendar.getInstance().apply { set(2025, Calendar.SEPTEMBER, 1, 0, 0, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
                    val now = System.currentTimeMillis()
                    ((now - yearStart).toFloat() / (nlcTimestamp - yearStart).toFloat()).coerceIn(0f, 1f)
                }

                val nlcCardBg = if (lightMode) Color(0xFFF0F4FF) else Color.White.copy(alpha = 0.1f)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = nlcCardBg)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Box(modifier = Modifier.size(130.dp).align(Alignment.TopEnd).background(primaryColor.copy(alpha = 0.06f), CircleShape))

                        Column(modifier = Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            // Top row: badge + location icon
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Surface(shape = RoundedCornerShape(8.dp), color = primaryColor.copy(alpha = 0.15f)) {
                                    Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.EmojiEvents, null, tint = primaryColor, modifier = Modifier.size(12.dp))
                                        Text("NLC 2026", color = primaryColor, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = (9 * textSizeMultiplier).sp, letterSpacing = 1.5.sp))
                                    }
                                }
                                Icon(Icons.Default.Place, null, tint = textColorTertiary, modifier = Modifier.size(20.dp))
                            }

                            // Countdown number
                            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "$daysUntilNLC",
                                    color = textColor,
                                    style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Black, fontSize = (56 * textSizeMultiplier).sp),
                                    lineHeight = (56 * textSizeMultiplier).sp
                                )
                                Column(modifier = Modifier.padding(bottom = 6.dp)) {
                                    Text("DAYS", color = primaryColor, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black, fontSize = (12 * textSizeMultiplier).sp, letterSpacing = 2.sp))
                                    Text("UNTIL NLC", color = textColorTertiary, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = (10 * textSizeMultiplier).sp, letterSpacing = 1.sp))
                                }
                            }

                            // Progress bar
                            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                LinearProgressIndicator(
                                    progress = { nlcProgress },
                                    modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape),
                                    color = primaryColor,
                                    trackColor = if (lightMode) primaryColor.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.1f)
                                )
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Sept 2025", color = textColorTertiary, style = MaterialTheme.typography.labelSmall.copy(fontSize = (9 * textSizeMultiplier).sp))
                                    Text("June 29 · San Antonio, TX", color = textColorTertiary, style = MaterialTheme.typography.labelSmall.copy(fontSize = (9 * textSizeMultiplier).sp))
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

// Calendar card
            if (!showListView) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                )
                            ),
                        colors = CardDefaults.cardColors(containerColor = if (lightMode) Color(0xFFF0F4FF) else Color.White.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .animateContentSize(
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessLow
                                    )
                                )
                        ) {
                            CalendarView(
                                selectedDate = selectedDate,
                                onDateSelected = { date -> selectedDate = date },
                                eventDates = eventDates,
                                primaryColor = primaryColor,
                                textSizeMultiplier = textSizeMultiplier,
                                lightMode = lightMode
                            )

                            selectedDate?.let { date ->
                                Spacer(modifier = Modifier.height(16.dp))
                                HorizontalDivider(color = if (lightMode) Color.Black.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.1f))
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = displayDateFormat.format(date.time),
                                        color = primaryColor,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = (MaterialTheme.typography.bodyMedium.fontSize.value * textSizeMultiplier).sp
                                        )
                                    )
                                    TextButton(onClick = { selectedDate = null }) {
                                        Text(
                                            "CLEAR",
                                            color = textColorSecondary,
                                            style = TextStyle(fontSize = (12 * textSizeMultiplier).sp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

// State selection / List view button
            item {
                if (selectedState.isEmpty()) {
// Show state selection button
                    Box {
                        Button(
                            onClick = { expanded = true },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.LocationOn, null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "SELECT STATE",
                                    fontWeight = FontWeight.Bold,
                                    style = TextStyle(fontSize = (14 * textSizeMultiplier).sp),
                                    color = Color.Black
                                )
                                Text(
                                    text = "Filter events by your state",
                                    style = TextStyle(fontSize = (10 * textSizeMultiplier).sp),
                                    color = Color.Black.copy(alpha = 0.85f)
                                )
                            }
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            states.forEach { state ->
                                DropdownMenuItem(
                                    onClick = {
                                        selectedState = state
                                        expanded = false
                                        selectedDate = null
                                        showListView = false
                                    },
                                    text = {
                                        Text(state, style = TextStyle(fontSize = (14 * textSizeMultiplier).sp))
                                    }
                                )
                            }
                        }
                    }
                } else {
// Show clear state and list view buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                selectedState = ""
                                selectedDate = null
                                showListView = false
                            },
                            modifier = Modifier.weight(1f).height(64.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = if (lightMode) Color(0xFFE8EDF5) else Color(0xFF505050)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Close, null, tint = textColor, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "VIEW ALL STATES",
                                    fontWeight = FontWeight.Bold,
                                    style = TextStyle(fontSize = (11 * textSizeMultiplier).sp),
                                    color = textColor,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        Button(
                            onClick = {
// Clear selected date when entering list view
                                if (!showListView) {
                                    selectedDate = null
                                }
                                showListView = !showListView
                            },
                            modifier = Modifier.weight(1f).height(64.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (showListView) primaryColor else if (lightMode) Color(0xFFE8EDF5) else Color.White.copy(alpha = 0.1f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    if (showListView) Icons.Default.CalendarMonth else Icons.AutoMirrored.Filled.List,
                                    null,
                                    tint = if (showListView) Color.Black else textColor,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (showListView) "SHOW CALENDAR" else "VIEW FULL LIST",
                                    fontWeight = FontWeight.Bold,
                                    style = TextStyle(fontSize = (11 * textSizeMultiplier).sp),
                                    color = if (showListView) Color.Black else textColor,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

// Events list header
            if (showListView || selectedDate != null) {
                item {
                    val eventCount = filteredEvents.size
                    val headerText = when {
                        showListView && selectedState.isNotEmpty() -> "All $selectedState Events ($eventCount)"
                        showListView -> "All Events ($eventCount)"
                        selectedDate != null && eventCount == 0 -> "No events on this date"
                        selectedDate != null -> "$eventCount event${if (eventCount != 1) "s" else ""} on this date"
                        else -> ""
                    }
                    if (headerText.isNotEmpty()) {
                        Text(
                            text = headerText,
                            color = primaryColor,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = (MaterialTheme.typography.labelLarge.fontSize.value * textSizeMultiplier).sp
                            )
                        )
                    }
                }
            }

// Events list items
            if (showListView || selectedDate != null) {
                itemsIndexed(filteredEvents) { _, event ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            ),
                        colors = CardDefaults.cardColors(containerColor = if (lightMode) Color(0xFFE8EDF5) else Color.White.copy(alpha = 0.1f)),
                        shape = MaterialTheme.shapes.extraSmall
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                                Text(
                                    text = event.name,
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = (MaterialTheme.typography.titleMedium.fontSize.value * textSizeMultiplier).sp
                                    ),
                                    color = textColor
                                )
                                IconButton(
                                    onClick = { shareCompetition(context, event.name, event.date, event.location) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = "Share event", tint = primaryColor)
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CalendarMonth, null, modifier = Modifier.size(16.dp), tint = textColorSecondary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = event.date, color = textColorSecondary, style = TextStyle(fontSize = (14 * textSizeMultiplier).sp))
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(16.dp), tint = textColorSecondary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = event.location, color = textColorSecondary, style = TextStyle(fontSize = (14 * textSizeMultiplier).sp))
                            }
                            if (auth.currentUser != null && event.timestamp > 0) {
                                Spacer(modifier = Modifier.height(16.dp))

// Check if reminder already exists
                                val hasReminder = userReminders.any { it.name == event.name && it.date == event.date }

                                Button(
                                    onClick = {
                                        if (!hasReminder) {
                                            val currentUid = auth.currentUser?.uid
                                            if (!currentUid.isNullOrEmpty()) {
                                                val reminder = CompetitionReminder(event.name, event.date, event.timestamp)
                                                db.collection("users").document(currentUid).update("reminders", FieldValue.arrayUnion(reminder))
                                                    .addOnSuccessListener { Toast.makeText(context, "Reminder Set!", Toast.LENGTH_SHORT).show() }
                                                val daysFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
                                                val eventDays = parseEventDates(event.date)
                                                val startMs = eventDays.firstOrNull()?.let { daysFmt.parse(it)?.time } ?: event.timestamp
                                                val endMs = eventDays.lastOrNull()?.let {
                                                    val c = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                                                    c.time = daysFmt.parse(it)!!; c.add(Calendar.DAY_OF_MONTH, 1); c.timeInMillis
                                                } ?: (startMs + 24 * 60 * 60 * 1000L)
                                                context.startActivity(Intent(Intent.ACTION_INSERT, CalendarContract.Events.CONTENT_URI).apply {
                                                    putExtra(CalendarContract.Events.TITLE, event.name)
                                                    putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMs)
                                                    putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMs)
                                                    putExtra(CalendarContract.Events.ALL_DAY, 1)
                                                    putExtra(CalendarContract.Events.DESCRIPTION, "FBLA Competition — added via FBLA Hub")
                                                    putExtra(CalendarContract.Events.EVENT_LOCATION, event.location)
                                                    putExtra(CalendarContract.Events.HAS_ALARM, 1)
                                                })
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (hasReminder) Color.Gray else primaryColor,
                                        contentColor = if (hasReminder) Color.White.copy(alpha = 0.85f) else Color.Black
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    enabled = !hasReminder
                                ) {
                                    Icon(
                                        if (hasReminder) Icons.Default.Check else Icons.Default.NotificationsActive,
                                        null,
                                        tint = if (hasReminder) Color.White.copy(alpha = 0.85f) else Color.Black,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        if (hasReminder) "REMINDER SET" else "SET REMINDER",
                                        color = if (hasReminder) Color.White.copy(alpha = 0.85f) else Color.Black,
                                        fontWeight = FontWeight.Bold,
                                        style = TextStyle(fontSize = (14 * textSizeMultiplier).sp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

// Bottom spacer
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
fun NewsScreen(primaryColor: Color, bgOpacity: Float, isSimplifiedUI: Boolean, textSizeMultiplier: Float, lightMode: Boolean = false) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

// Dynamic text colors
    val textColor = if (lightMode) Color.Black else Color.White
    val textColorSecondary = if (lightMode) Color.Black.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.9f)
    val textColorTertiary = if (lightMode) Color.Black.copy(alpha = 0.75f) else Color.White.copy(alpha = 0.85f)

    // Hardcoded articles shown instantly — accurate as of April 2026
    val hardcodedArticles = remember {
        listOf(
            NewsArticle("NLC 2026 Registration Now Open", "Register now for the National Leadership Conference in Anaheim, California, June 28 – July 2, 2026. Early-bird rates available through May 1.", "Apr 10, 2026", "https://www.fbla.org/nlc/", "CONFERENCE", System.currentTimeMillis() - 259200000L),
            NewsArticle("Architects of Ambition — 2025-2026 National Theme", "FBLA's official theme for the 2025-2026 year challenges members to build bold futures through leadership, innovation, and community impact.", "Apr 5, 2026", "https://www.fbla.org/newsroom/", "NATIONAL", System.currentTimeMillis() - 691200000L),
            NewsArticle("SLC Season Recap: Record Turnout Nationwide", "State Leadership Conferences wrapped up across all 50 states and territories this spring, with record attendance and fierce competition in over 70 events.", "Mar 28, 2026", "https://www.fbla.org/slc/", "CONFERENCE", System.currentTimeMillis() - 1382400000L),
            NewsArticle("2026 Competitive Event Guidelines Updated", "FBLA has released final updated guidelines for all 2025-2026 high school competitive events. Members are encouraged to review changes before NLC.", "Mar 20, 2026", "https://www.fbla.org/high-school/competitive-events/", "HS", System.currentTimeMillis() - 2073600000L),
            NewsArticle("Scholarship Applications Closing Soon", "Over \$250,000 in FBLA scholarships are available for graduating seniors. Deadlines vary — apply now at fbla.org before opportunities close.", "Mar 15, 2026", "https://www.fbla.org/high-school/program/scholarships/", "OPPORTUNITY", System.currentTimeMillis() - 2505600000L),
            NewsArticle("Business Achievement Awards: Nominations Open", "FBLA members can now submit nominations for the 2026 Business Achievement Awards, recognizing outstanding chapter and individual accomplishments.", "Mar 8, 2026", "https://www.fbla.org/high-school/recognition/business-achievement-awards/", "OPPORTUNITY", System.currentTimeMillis() - 3110400000L),
            NewsArticle("New: AI & Technology Competitive Event Pilot", "FBLA is piloting a new Artificial Intelligence & Technology event at select SLCs this year, with plans for full rollout at NLC 2027.", "Mar 1, 2026", "https://www.fbla.org/high-school/competitive-events/", "HS", System.currentTimeMillis() - 3715200000L),
            NewsArticle("Chapter Excellence Awards — Apply Now", "Chapters nationwide are encouraged to apply for the 2026 Chapter Excellence Awards, highlighting community service, membership growth, and event performance.", "Feb 22, 2026", "https://www.fbla.org/chapter-excellence-awards/", "HS", System.currentTimeMillis() - 4320000000L),
            NewsArticle("PBL Announces 2026 Corporate Partner Cohort", "Phi Beta Lambda welcomes 12 new national corporate partners for 2026, expanding internship and job placement pipelines for collegiate members.", "Feb 15, 2026", "https://www.fbla.org/pbl/", "PBL", System.currentTimeMillis() - 4924800000L),
            NewsArticle("FBLA Surpasses 230,000 Members Nationwide", "FBLA has officially surpassed 230,000 active members for the 2025-2026 school year — a new all-time high for the organization.", "Feb 10, 2026", "https://www.fbla.org/membership/", "NATIONAL", System.currentTimeMillis() - 5356800000L),
            NewsArticle("Middle Level Championships Set for June", "The FBLA Middle Level national championships will be held alongside NLC in Anaheim this June, featuring 25 competitive events for grades 5-9.", "Feb 3, 2026", "https://www.fbla.org/middle-level/", "ML", System.currentTimeMillis() - 5961600000L),
            NewsArticle("Financial Literacy Month: Free Resources Available", "In honor of Financial Literacy Month in April, FBLA has released a free toolkit of classroom and chapter activities available to all members.", "Apr 1, 2026", "https://www.fbla.org/financial-literacy/", "NATIONAL", System.currentTimeMillis() - 518400000L),
            NewsArticle("Adviser of the Year Nominations Open", "FBLA-PBL is accepting nominations for the 2026 National Adviser of the Year award. Submit your chapter adviser's nomination by May 15.", "Mar 25, 2026", "https://www.fbla.org/advisers/", "NATIONAL", System.currentTimeMillis() - 1728000000L),
            NewsArticle("Career & Leadership Webinar Series Launched", "FBLA's new free webinar series features monthly sessions with business leaders covering resume writing, networking, and career exploration.", "Mar 12, 2026", "https://www.fbla.org/careers/", "OPPORTUNITY", System.currentTimeMillis() - 2764800000L),
            NewsArticle("Digital Marketing Event Draws Highest Registration", "The Digital Marketing competitive event has become one of the top-10 most entered events for 2026, reflecting growing student interest in marketing careers.", "Feb 28, 2026", "https://www.fbla.org/competitions/", "HS", System.currentTimeMillis() - 3888000000L),
            NewsArticle("Alumni Network Launches Mentorship Program", "FBLA's alumni network is connecting current student members with professionals across industries through a new structured mentorship program.", "Feb 20, 2026", "https://www.fbla.org/alumni/", "NATIONAL", System.currentTimeMillis() - 4492800000L),
            NewsArticle("Entrepreneurship Challenge: $10K Grand Prize", "FBLA's national Entrepreneurship Challenge is back with a \$10,000 grand prize for the best student business plan. Submissions due April 30.", "Feb 6, 2026", "https://www.fbla.org/entrepreneurship/", "OPPORTUNITY", System.currentTimeMillis() - 5702400000L),
            NewsArticle("Leadership Development Retreat Applications Open", "Applications are open for FBLA's 2026 National Leadership Development Retreat, a competitive program for top chapter officers.", "Jan 30, 2026", "https://www.fbla.org/leadership/", "NATIONAL", System.currentTimeMillis() - 6307200000L),
            NewsArticle("Community Service: 1.2M Hours and Counting", "FBLA chapters collectively logged over 1.2 million community service hours in the 2024-2025 school year, setting a new organizational record.", "Jan 22, 2026", "https://www.fbla.org/community-service/", "NATIONAL", System.currentTimeMillis() - 6912000000L),
            NewsArticle("New Study Guides Released for Objective Tests", "FBLA has published updated official study guides for all objective test competitive events, now available as free downloads on fbla.org.", "Jan 14, 2026", "https://www.fbla.org/high-school/competitive-events/", "HS", System.currentTimeMillis() - 7603200000L)
        )
    }

    var newsList by remember { mutableStateOf<List<NewsArticle>>(hardcodedArticles) }
    var isLoading by remember { mutableStateOf(false) }
    var lastFetchTime by remember { mutableStateOf(System.currentTimeMillis()) }

    // Function to scrape FBLA news (runs in background after instant load)
    suspend fun fetchFBLANews(): List<NewsArticle> = withContext(Dispatchers.IO) {
        try {
            val articles = mutableListOf<NewsArticle>()
            val doc = org.jsoup.Jsoup.connect("https://www.fbla.org/newsroom/")
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .timeout(8000)
                .get()
            doc.select("article, .post, .news-item, .press-release, div[class*='news'], div[class*='article']").take(20).forEach { article ->
                try {
                    val titleElement = article.select("h1, h2, h3, h4, .title, .headline, .entry-title, .post-title, [class*='title']").first()
                    val title = titleElement?.text()?.trim() ?: ""
                    var link = ""
                    if (titleElement != null) {
                        val titleLink = titleElement.select("a[href]").first()
                        if (titleLink != null) link = titleLink.attr("abs:href") ?: titleLink.attr("href") ?: ""
                        if (link.isEmpty()) {
                            val parentLink = titleElement.parent()
                            if (parentLink != null && parentLink.tagName() == "a") link = parentLink.attr("abs:href") ?: parentLink.attr("href") ?: ""
                        }
                    }
                    if (link.isEmpty()) {
                        article.select("a[href]").forEach { linkElement ->
                            val href = linkElement.attr("abs:href") ?: linkElement.attr("href") ?: ""
                            if (!href.contains("/author/") && !href.contains("facebook.com") && !href.contains("twitter.com") && !href.contains("linkedin.com") && !href.contains("/category/") && href.isNotEmpty()) {
                                link = href; return@forEach
                            }
                        }
                    }
                    val summary = article.select("p, .excerpt, .entry-summary, .description, [class*='excerpt'], [class*='summary']").first()?.text()?.trim()?.take(200) ?: ""
                    val dateStr = article.select("time, .date, .published, .entry-date, [class*='date']").first()?.text()?.trim() ?: ""
                    if (title.isNotEmpty() && link.isNotEmpty() && !link.contains("/author/")) {
                        val fullUrl = if (link.startsWith("http")) link else "https://www.fbla.org$link"
                        val formattedDate = if (dateStr.isNotEmpty()) dateStr else SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date())
                        val finalSummary = if (summary.isNotEmpty()) summary else "Click to read the full article on FBLA.org"
                        articles.add(NewsArticle(title = title, summary = finalSummary, date = formattedDate, url = fullUrl, tag = determineNewsTag(title, finalSummary), timestamp = System.currentTimeMillis()))
                    }
                } catch (e: Exception) { Log.e("NewsScreen", "Error parsing article", e) }
            }
            if (articles.size >= 5) articles else hardcodedArticles
        } catch (e: Exception) {
            Log.e("NewsScreen", "Error fetching news", e)
            hardcodedArticles
        }
    }

// Show hardcoded articles instantly, then silently try to refresh in background
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val fetched = fetchFBLANews()
                if (fetched.isNotEmpty() && fetched !== hardcodedArticles) {
                    newsList = fetched
                    lastFetchTime = System.currentTimeMillis()
                }
            } catch (e: Exception) {
                Log.e("NewsScreen", "Background fetch failed, keeping hardcoded", e)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
// Background — white in light mode, dark blue in dark mode
        Box(modifier = Modifier.fillMaxSize().background(
            if (lightMode) Color.White else Color(0xFF0A1628)
        ))
        if (!lightMode) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = bgOpacity)))
        }

        if (isLoading && newsList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = primaryColor)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "THE FEED",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        letterSpacing = 4.sp,
                                        fontWeight = FontWeight.Black,
                                        color = primaryColor,
                                        fontSize = (14 * textSizeMultiplier).sp
                                    )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Latest FBLA News",
                                    style = MaterialTheme.typography.displaySmall.copy(
                                        fontWeight = FontWeight.Black,
                                        color = textColor,
                                        fontSize = (32 * textSizeMultiplier).sp
                                    )
                                )
                            }

// Refresh button
                            Surface(
                                shape = CircleShape,
                                color = if (lightMode) Color(0xFFE8EDF5) else Color.White.copy(alpha = 0.1f),
                                border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.5f))
                            ) {
                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            isLoading = true
                                            newsList = fetchFBLANews()
                                            lastFetchTime = System.currentTimeMillis()
                                            isLoading = false
                                            Toast.makeText(context, "News refreshed!", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.Refresh,
                                        contentDescription = "Refresh news",
                                        tint = primaryColor,
                                        modifier = if (isLoading) {
                                            Modifier
                                                .size(24.dp)
                                                .graphicsLayer {
                                                    rotationZ = (System.currentTimeMillis() % 1000) / 1000f * 360f
                                                }
                                        } else {
                                            Modifier.size(24.dp)
                                        }
                                    )
                                }
                            }
                        }

// Show last updated time
                        if (lastFetchTime > 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color.Green.copy(alpha = 0.6f),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                val minutes = (System.currentTimeMillis() - lastFetchTime) / 60000
                                Text(
                                    text = if (minutes < 1) "Just updated" else "Updated $minutes min ago",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = textColorSecondary,
                                        fontSize = (12 * textSizeMultiplier).sp
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }

                itemsIndexed(newsList) { index, article ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.3f)),
                        onClick = {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(article.url)))
                        }
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
// Tag and date row
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
// Tag pill
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = primaryColor,
                                    border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.5f))
                                ) {
                                    Text(
                                        text = article.tag,
                                        color = Color.Black,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = (10 * textSizeMultiplier).sp,
                                            letterSpacing = 1.sp
                                        ),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.weight(1f))

// Date
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = textColorSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = article.date,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = (11 * textSizeMultiplier).sp
                                        ),
                                        color = textColorSecondary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

// Title
                            Text(
                                text = article.title,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = (20 * textSizeMultiplier).sp,
                                    lineHeight = (26 * textSizeMultiplier).sp
                                ),
                                color = textColor
                            )

                            Spacer(modifier = Modifier.height(8.dp))

// Summary
                            Text(
                                text = article.summary,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = (14 * textSizeMultiplier).sp,
                                    lineHeight = (20 * textSizeMultiplier).sp
                                ),
                                color = textColorSecondary,
                                maxLines = 3
                            )

                            Spacer(modifier = Modifier.height(12.dp))

// Read more indicator
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Read Article",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = (12 * textSizeMultiplier).sp
                                    ),
                                    color = primaryColor
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = primaryColor,
                                    modifier = Modifier.size(16.dp)
                                )

                                Spacer(modifier = Modifier.weight(1f))

// Share button
                                Surface(
                                    shape = CircleShape,
                                    color = if (lightMode) Color(0xFFE8EDF5) else Color.White.copy(alpha = 0.1f),
                                    border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.5f)),
                                    modifier = Modifier.clickable {
                                        shareArticle(context, article.title, article.url)
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.Share,
                                        contentDescription = "Share Article",
                                        tint = primaryColor,
                                        modifier = Modifier
                                            .padding(8.dp)
                                            .size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun determineNewsTag(title: String, summary: String): String {
    val text = (title + " " + summary).lowercase()

    return when {
        text.contains("nlc") || text.contains("slc") || text.contains("conference") -> "CONFERENCE"
        text.contains("scholarship") || text.contains("award") || text.contains("opportunity") -> "OPPORTUNITY"
        text.contains("high school") || text.contains("hs") -> "HS"
        text.contains("middle") || text.contains("ml") -> "ML"
        text.contains("college") || text.contains("pbl") -> "PBL"
        else -> "NATIONAL"
    }
}

fun sharePlainText(context: Context, subject: String, body: String, chooserTitle: String) {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, body)
        putExtra(Intent.EXTRA_SUBJECT, subject)
    }
    context.startActivity(Intent.createChooser(shareIntent, chooserTitle))
}

fun shareArticle(context: Context, title: String, url: String) {
    sharePlainText(context, title, "$title\n\nRead more: $url", "Share article")
}

fun shareCompetition(context: Context, name: String, date: String, location: String = "") {
    val locLine = if (location.isNotBlank()) "\n📍 $location" else ""
    val body = "🏆 FBLA competition\n\n$name\n📅 $date$locLine\n\nShared from FBLA Hub"
    sharePlainText(context, name, body, "Share competition")
}

fun shareMemberProfile(context: Context, name: String, chapter: String, year: String, state: String, bio: String = "") {
    val bioLine = if (bio.isNotBlank()) "\n\n$bio" else ""
    val body = "👤 $name on FBLA Hub\n\nChapter: ${chapter.ifBlank { "—" }}\nClass of: ${year.ifBlank { "—" }} • ${state.ifBlank { "—" }}$bioLine\n\n#FBLA #FBLAHub"
    sharePlainText(context, "$name — FBLA Hub", body, "Share profile")
}

@Composable
fun SocialMediaLogo(platform: String, modifier: Modifier = Modifier) {
    when (platform) {
        "Instagram" -> {
            Image(
                painter = painterResource(id = R.drawable.instagram_logo),
                contentDescription = "Instagram",
                modifier = modifier
                    .size(32.dp)
                    .border(2.dp, Color.White, CircleShape)
                    .clip(CircleShape)
            )
        }
        "X (Twitter)" -> {
            Box(
                modifier = modifier
                    .size(32.dp)
                    .border(2.dp, Color.White, CircleShape)
                    .background(Color(0xFF000000), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "𝕏",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        "Facebook" -> {
            Box(
                modifier = modifier
                    .size(32.dp)
                    .border(2.dp, Color.White, CircleShape)
                    .background(Color(0xFF1877F2), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "f",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif
                )
            }
        }
        "LinkedIn" -> {
            Box(
                modifier = modifier
                    .size(32.dp)
                    .border(2.dp, Color.White, CircleShape)
                    .background(Color(0xFF0A66C2), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "in",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun EventPrepHub(
    onBack: () -> Unit,
    primaryColor: Color,
    bgOpacity: Float,
    isSimplifiedUI: Boolean,
    textSizeMultiplier: Float,
    lightMode: Boolean = false
) {
    val textColor = if (lightMode) Color.Black else Color.White
    val textColorSecondary = if (lightMode) Color.Black.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.9f)
    val textColorTertiary = if (lightMode) Color.Black.copy(alpha = 0.75f) else Color.White.copy(alpha = 0.85f)
    val cardBg = if (lightMode) Color(0xFFE8EDF5) else Color.White.copy(alpha = 0.1f)
    val scope = rememberCoroutineScope()

    data class FBLAEvent(val name: String, val category: String, val icon: ImageVector)

    val events = remember {
        listOf(
            FBLAEvent("Accounting I", "INDIVIDUAL", Icons.Default.Calculate),
            FBLAEvent("Accounting II", "INDIVIDUAL", Icons.Default.Calculate),
            FBLAEvent("Agribusiness", "INDIVIDUAL", Icons.Default.Agriculture),
            FBLAEvent("Banking & Financial Systems", "INDIVIDUAL", Icons.Default.AccountBalance),
            FBLAEvent("Business Calculations", "INDIVIDUAL", Icons.Default.Functions),
            FBLAEvent("Business Communication", "INDIVIDUAL", Icons.Default.Forum),
            FBLAEvent("Business Law", "INDIVIDUAL", Icons.Default.Gavel),
            FBLAEvent("Business Math", "INDIVIDUAL", Icons.Default.Calculate),
            FBLAEvent("Computer Applications", "INDIVIDUAL", Icons.Default.Computer),
            FBLAEvent("Computer Problem Solving", "INDIVIDUAL", Icons.Default.Code),
            FBLAEvent("Cybersecurity", "INDIVIDUAL", Icons.Default.Security),
            FBLAEvent("Database Design & Applications", "INDIVIDUAL", Icons.Default.Storage),
            FBLAEvent("Economics", "INDIVIDUAL", Icons.Default.TrendingUp),
            FBLAEvent("Electronic Career Portfolio", "INDIVIDUAL", Icons.Default.WorkspacePremium),
            FBLAEvent("Entrepreneurship", "INDIVIDUAL", Icons.Default.Lightbulb),
            FBLAEvent("Global Business", "INDIVIDUAL", Icons.Default.Language),
            FBLAEvent("Healthcare Administration", "INDIVIDUAL", Icons.Default.LocalHospital),
            FBLAEvent("Help Desk", "INDIVIDUAL", Icons.Default.HeadsetMic),
            FBLAEvent("Hospitality & Event Management", "INDIVIDUAL", Icons.Default.Hotel),
            FBLAEvent("Human Resource Management", "INDIVIDUAL", Icons.Default.People),
            FBLAEvent("Insurance & Risk Management", "INDIVIDUAL", Icons.Default.VerifiedUser),
            FBLAEvent("Introduction to Business", "INDIVIDUAL", Icons.Default.Business),
            FBLAEvent("Introduction to Business Communication", "INDIVIDUAL", Icons.Default.RecordVoiceOver),
            FBLAEvent("Introduction to Financial Math", "INDIVIDUAL", Icons.Default.AttachMoney),
            FBLAEvent("Introduction to Information Technology", "INDIVIDUAL", Icons.Default.Devices),
            FBLAEvent("Introduction to Parliamentary Procedure", "INDIVIDUAL", Icons.Default.Gavel),
            FBLAEvent("Job Interview", "INDIVIDUAL", Icons.Default.Badge),
            FBLAEvent("Management Decision Making", "TEAM", Icons.Default.Psychology),
            FBLAEvent("Marketing", "INDIVIDUAL", Icons.Default.Campaign),
            FBLAEvent("Network Design", "TEAM", Icons.Default.AccountTree),
            FBLAEvent("Networking Concepts", "INDIVIDUAL", Icons.Default.Hub),
            FBLAEvent("Parliamentary Procedure", "TEAM", Icons.Default.HowToVote),
            FBLAEvent("Personal Finance", "INDIVIDUAL", Icons.Default.AccountBalanceWallet),
            FBLAEvent("Political Science", "INDIVIDUAL", Icons.Default.Balance),
            FBLAEvent("Public Speaking", "INDIVIDUAL", Icons.Default.Mic),
            FBLAEvent("Securities & Investments", "INDIVIDUAL", Icons.Default.ShowChart),
            FBLAEvent("Social Media Strategies", "INDIVIDUAL", Icons.Default.Share),
            FBLAEvent("Sports & Entertainment Management", "INDIVIDUAL", Icons.Default.EmojiEvents),
            FBLAEvent("Supply Chain Management", "INDIVIDUAL", Icons.Default.LocalShipping),
            FBLAEvent("UX Design", "INDIVIDUAL", Icons.Default.Palette),
            FBLAEvent("Word Processing", "INDIVIDUAL", Icons.Default.Description),
            FBLAEvent("Business Ethics", "TEAM", Icons.Default.Handshake),
            FBLAEvent("Business Plan", "TEAM", Icons.AutoMirrored.Filled.Assignment),
            FBLAEvent("Client Service", "TEAM", Icons.Default.SupportAgent),
            FBLAEvent("Community Service Project", "TEAM", Icons.Default.VolunteerActivism),
            FBLAEvent("Digital Video Production", "TEAM", Icons.Default.VideoLibrary),
            FBLAEvent("E-business", "TEAM", Icons.Default.ShoppingCart),
            FBLAEvent("Emerging Business Issues", "TEAM", Icons.Default.Insights),
            FBLAEvent("Future Business Educator", "INDIVIDUAL", Icons.Default.School),
            FBLAEvent("Graphic Design", "TEAM", Icons.Default.Brush),
            FBLAEvent("Impromptu Speaking", "INDIVIDUAL", Icons.Default.Bolt),
            FBLAEvent("Mobile Application Development", "TEAM", Icons.Default.PhoneAndroid),
            FBLAEvent("Sales Presentation", "TEAM", Icons.Default.Sell),
            FBLAEvent("Website Design", "TEAM", Icons.Default.Web)
        )
    }

    var selectedEvent by remember { mutableStateOf<FBLAEvent?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var showSearch by remember { mutableStateOf(false) }
    var activeCategory by remember { mutableStateOf("ALL") }

    // Prep screen state
    var prepContent by remember { mutableStateOf("") }
    var isLoadingPrep by remember { mutableStateOf(false) }
    var activeTab by remember { mutableStateOf(0) } // 0=Overview 1=Questions 2=Tips

    val filteredEvents = remember(searchQuery, activeCategory) {
        events.filter { event ->
            (searchQuery.isEmpty() || event.name.contains(searchQuery, ignoreCase = true)) &&
                    (activeCategory == "ALL" || event.category == activeCategory)
        }
    }

    fun getPrepContent(event: FBLAEvent, tab: Int): String {
        val n = event.name
        val isTeam = event.category == "TEAM"

        if (tab == 0) return when {
            n == "Business Law" -> "**Format**\nObjective test — 100 questions, 50 minutes.\n\n**Topics Covered**\n• Contract law (offer, acceptance, consideration, capacity)\n• Business organizations (sole proprietorship, partnership, LLC, corporation)\n• Agency relationships and authority types\n• Uniform Commercial Code (UCC) — sale of goods\n• Tort law and negligence\n• Intellectual property (copyright, trademark, patent)\n• Employment and labor law\n• Consumer protection and bankruptcy (Ch. 7, Ch. 11)\n\n**Scoring**\nNumber correct out of 100. Top scorers advance to the next round.\n\n**Key Resources**\nFBLA Business Law study guide at fbla.org, Cliffs Notes Business Law."
            n == "Economics" -> "**Format**\nObjective test — 100 questions, 50 minutes.\n\n**Topics Covered**\n• Supply and demand (shifts, elasticity, equilibrium price)\n• GDP, inflation, CPI, and unemployment measurement\n• Monetary policy and the Federal Reserve\n• Fiscal policy — government spending and taxes\n• Market structures (perfect competition, monopoly, oligopoly, monopolistic competition)\n• International trade, tariffs, comparative advantage\n• Opportunity cost, trade-offs, and scarcity\n• Business cycle phases (expansion, peak, contraction, trough)\n• Banking and financial markets\n\n**Scoring**\nNumber correct out of 100. Ties broken by timed scoring.\n\n**Key Resources**\nFBLA Economics study guide, Khan Academy Macroeconomics."
            n == "Accounting I" -> "**Format**\nObjective test — 100 questions, 50 minutes.\n\n**Topics Covered**\n• Accounting equation: Assets = Liabilities + Owner's Equity\n• Journalizing and posting transactions (debits and credits)\n• Normal balances for all account types\n• Trial balance preparation\n• Income statement and balance sheet\n• Adjusting entries and closing entries\n• Cash and banking procedures\n• Payroll accounting basics\n• GAAP principles (matching, revenue recognition)\n\n**Scoring**\nNumber correct out of 100.\n\n**Key Resources**\nCentury 21 Accounting textbook, FBLA Accounting I study guide."
            n == "Accounting II" -> "**Format**\nObjective test — 100 questions, 50 minutes.\n\n**Topics Covered**\n• Departmental accounting and cost allocation\n• Partnership accounting (formation, profit sharing, dissolution)\n• Corporate accounting (stock issuance, dividends, retained earnings)\n• Manufacturing cost accounting (job order, process costing)\n• Budgeting and variance analysis\n• Financial statement analysis (ratios: liquidity, profitability, solvency)\n• Long-term assets and depreciation methods\n• Bonds payable and notes payable\n\n**Scoring**\nNumber correct out of 100."
            n == "Personal Finance" -> "**Format**\nObjective test — 100 questions, 50 minutes.\n\n**Topics Covered**\n• Budgeting, cash flow, and the 50/30/20 rule\n• Banking (checking, savings, interest types)\n• Credit scores, credit cards, and debt management\n• Investing (stocks, bonds, mutual funds, ETFs, index funds)\n• Retirement accounts (401k, Roth IRA, Traditional IRA)\n• Insurance types (health, auto, life, homeowners, renters)\n• Taxes (W-2, 1040, deductions, credits)\n• Rule of 72 and compound interest\n• Net worth = Assets − Liabilities\n• Consumer rights and protection laws\n\n**Scoring**\nNumber correct out of 100."
            n == "Marketing" -> "**Format**\nObjective test — 100 questions, 50 minutes.\n\n**Topics Covered**\n• The 4 Ps (Product, Price, Place, Promotion)\n• Market segmentation and target market identification\n• Consumer behavior and buying decision process\n• Brand equity and brand management\n• Pricing strategies (penetration, skimming, cost-plus, loss leader)\n• Distribution channels and logistics\n• Promotional mix (advertising, PR, sales promotion, personal selling)\n• Digital marketing, social media, and SEO\n• Market research methods (primary vs. secondary)\n• SWOT analysis in a marketing context\n\n**Scoring**\nNumber correct out of 100."
            n == "Cybersecurity" -> "**Format**\nObjective test — 100 questions, 50 minutes.\n\n**Topics Covered**\n• CIA Triad (Confidentiality, Integrity, Availability)\n• Malware types (virus, worm, ransomware, trojan, spyware, adware)\n• Network security (firewalls, IDS/IPS, VPNs, DMZ)\n• Cryptography basics and encryption (symmetric vs. asymmetric)\n• Authentication methods (MFA, biometrics, tokens)\n• Social engineering (phishing, vishing, pretexting, baiting)\n• Security frameworks (NIST, ISO 27001)\n• Incident response lifecycle\n• Wireless security (WEP, WPA2, WPA3)\n• Common vulnerabilities (SQL injection, XSS, buffer overflow)\n\n**Scoring**\nNumber correct out of 100."
            n == "Introduction to Business" -> "**Format**\nObjective test — 100 questions, 50 minutes.\n\n**Topics Covered**\n• Forms of business ownership (sole prop, partnership, LLC, corp)\n• Core business functions (marketing, finance, HR, operations)\n• Supply and demand fundamentals\n• Management functions (planning, organizing, leading, controlling)\n• Entrepreneurship and small business formation\n• Business ethics and social responsibility\n• International business and globalization\n• Business math (markup, break-even, ROI)\n• Human resources basics and employment law\n• E-commerce and technology in business\n\n**Scoring**\nNumber correct out of 100."
            n == "Business Communication" -> "**Format**\nObjective test — 100 questions, 50 minutes.\n\n**Topics Covered**\n• The 7 Cs of communication (Clear, Concise, Concrete, Correct, Coherent, Complete, Courteous)\n• Business writing formats (letters, memos, emails, reports)\n• Verbal vs. nonverbal communication\n• Active listening techniques and barriers\n• Presentation skills and visual aids\n• Meeting management and agenda writing\n• Grammar, punctuation, and mechanics\n• Proofreading and editing strategies\n• Cross-cultural communication\n• Digital communication etiquette\n\n**Scoring**\nNumber correct out of 100."
            n == "Business Calculations" -> "**Format**\nObjective test — 100 questions, 50 minutes. Calculator typically permitted.\n\n**Topics Covered**\n• Markup and markdown (retail pricing math)\n• Gross and net profit margin calculations\n• Simple interest: I = PRT\n• Compound interest: A = P(1 + r)^t\n• Commission, wages, and payroll calculations\n• Sales tax and income tax computations\n• Trade discounts and cash discounts\n• Break-even analysis: Fixed Costs ÷ (Price − Variable Cost)\n• Depreciation (straight-line, double-declining)\n• Statistics: mean, median, mode, range\n\n**Scoring**\nNumber correct out of 100."
            n == "Networking Concepts" -> "**Format**\nObjective test — 100 questions, 50 minutes.\n\n**Topics Covered**\n• OSI model — all 7 layers and their functions\n• TCP/IP model and key protocols (HTTP/S, FTP, SSH, SMTP, DNS, DHCP)\n• IP addressing: IPv4, IPv6, subnetting, CIDR notation\n• Network devices: router, switch, hub, firewall, access point\n• Network topologies (bus, star, ring, mesh, hybrid)\n• Wireless standards (802.11a/b/g/n/ac/ax)\n• Private vs. public IP ranges (RFC 1918)\n• Cloud computing and virtualization concepts\n• Troubleshooting tools: ping, traceroute, ipconfig/ifconfig\n• Network security: VPN, firewall, IDS/IPS, NAT\n\n**Scoring**\nNumber correct out of 100."
            n == "Public Speaking" -> "**Format**\nPrepared speech — 5 to 7 minutes on a business topic chosen by the competitor. No notes allowed during delivery.\n\n**Choosing Your Topic**\nPick a current business issue you genuinely care about:\n• Leadership and organizational culture\n• Technology's impact on business\n• Entrepreneurship and innovation\n• Environmental sustainability in business\n• Ethics and corporate responsibility\n\n**Scoring Criteria**\n• Introduction and conclusion strength\n• Organization and logical flow\n• Research quality and supporting evidence\n• Delivery (eye contact, vocal variety, posture, gestures)\n• Language use and vocabulary\n• Overall effectiveness and persuasiveness"
            n == "Job Interview" -> "**Format**\nSimulated job interview with judges acting as employers — 7 to 10 minutes total.\n\n**What to Expect**\n• Receive a job description before competition\n• Dress formally — business professional is required\n• Bring multiple printed resume copies\n• Judges score from the moment you walk in\n\n**Skills Tested**\n• Self-presentation and professional appearance\n• Communication and listening skills\n• Industry and role knowledge\n• Handling behavioral and situational questions\n• Asking thoughtful questions to the interviewer\n\n**Scoring**\nPoise, appearance, communication, knowledge of the role, resume quality, and closing."
            n == "Entrepreneurship" -> "**Format**\nObjective test (100 questions) + case study presentation to judges (7 minutes + Q&A).\n\n**Objective Test Topics**\n• Entrepreneurship process and opportunity recognition\n• Business plan components (all sections)\n• Financing options (bootstrapping, angel, VC, crowdfunding, SBA loans)\n• Marketing for startups and growth strategies\n• Operations, supply chain, and business law basics\n\n**Case Study**\nYou receive a business scenario and must present a structured solution including problem analysis, proposed solutions, recommended action, and implementation plan.\n\n**Scoring**\nObjective test score + presentation score are combined."
            n == "Business Plan" -> "**Format**\nWritten business plan (submitted in advance) + oral presentation (10 minutes) + Q&A with judges. Teams of 1–3.\n\n**Required Plan Sections**\n• Executive Summary (write this last, present it first)\n• Company Description and Mission\n• Market Analysis (target market, competitors, market size)\n• Organization & Management structure\n• Products/Services description\n• Marketing & Sales Strategy\n• Financial Projections (income statement, cash flow, balance sheet)\n• Funding Request and use of funds\n\n**Scoring**\nWritten plan quality (50%), oral presentation (30%), Q&A responses (20%)."
            n == "Impromptu Speaking" -> "**Format**\nNo preparation allowed — receive a topic and speak for 1–3 minutes immediately after a brief draw period.\n\n**What to Expect**\n• Business-related topics (ethics, leadership, economics, technology, etc.)\n• 1–2 minutes to collect your thoughts (varies by level)\n• Speak immediately — no notes during delivery\n\n**Scoring**\n• Organization (clear intro, body, conclusion)\n• Delivery confidence and vocal variety\n• Vocabulary and language use\n• Coverage and insight on the topic\n\n**The Core Skill**\nStructuring thoughts instantly under pressure — this is almost entirely a trainable skill."
            else -> "**Event Overview: $n**\n${if (isTeam) "Team event — 2–3 members collaborate on this." else "Individual event — just you."}\n\n**Typical Format**\n${if (n.contains("Objective", true) || listOf("Accounting", "Economics", "Marketing", "Law", "Math", "Finance", "Cybersecurity", "Networking", "Business Calc", "Introduction").any { n.contains(it, true) }) "Objective test — approximately 100 questions, 50 minutes." else if (listOf("Speaking", "Interview", "Presentation", "Sales").any { n.contains(it, true) }) "Performance event — you present to a panel of judges. Dress professionally." else if (listOf("Plan", "Portfolio", "Project", "Design", "Video", "Production").any { n.contains(it, true) }) "Project-based event — submit a written/digital project plus an oral presentation." else "Check fbla.org for the exact format — it may be an objective test, project, or performance event."}\n\n**Topics & Skills**\n• Download the official topic outline from fbla.org → Competitive Events → $n\n• Study your school's textbook chapters relevant to this subject area\n• Practice with past FBLA sample tests if available on fbla.org\n${if (isTeam) "• Assign clear roles to each team member and rehearse together" else ""}\n\n**Scoring**\nJudges use official FBLA rubrics. Ask your adviser for the current scoring guide for this event."
        }

        if (tab == 1) return when {
            n == "Business Law" -> "**Master the Big Four Chapters**\nContracts, business organizations, UCC, and torts make up the majority of questions. Spend 60% of your study time here.\n\n**Know Rules AND Exceptions**\nFBLA loves testing exceptions (e.g., which contracts don't require writing, when minors CAN'T void contracts). Memorize the rule, then memorize its exceptions.\n\n**Use Process of Elimination**\nWhen two answers look plausible, focus on the legal nuance — usually one option is slightly too broad or too narrow. Eliminate the extremes first.\n\n**Practice Past Tests**\nDownload FBLA past objective tests at fbla.org. The question style and topic weighting repeat — familiarity with the format is a real advantage.\n\n**Time Management on Test Day**\n~30 seconds per question on a 100-question, 50-minute test. Flag hard ones and come back — never stall on a single question."
            n == "Economics" -> "**Understand Cause and Effect Deeply**\nDon't memorize facts in isolation — know WHY demand curves shift, WHY the Fed raises rates, WHY tariffs cause deadweight loss. FBLA questions test understanding, not recall.\n\n**Master Supply & Demand First**\nHalf of economics questions trace back to supply/demand dynamics. Know every shifter for both curves and how equilibrium changes as a result.\n\n**Know the Big Numbers**\nFamiliarize yourself with what \"normal\" looks like: healthy GDP growth (~2–3%), natural unemployment rate (~4–5%), Fed inflation target (2%). Questions often test recognition of abnormal conditions.\n\n**Use Elimination on Math Questions**\nFor elasticity and GDP calculation questions, eliminate obviously wrong magnitudes first. Then verify with the formula.\n\n**Study Recent Monetary Policy**\nFBLA Economics questions frequently reference current economic conditions. Know what the Fed has been doing recently and why."
            n == "Public Speaking" -> "**Open with Power, Not Your Name**\nNever start with \"Hi, my name is...\" Open with a gripping hook: shocking statistic, vivid story, or bold question. Your first 15 seconds set the judges' impression.\n\n**Record Every Practice Session**\nWatch with sound off (gestures, eye contact, posture) then listen without looking (filler words, pacing, volume). Do this at least 5 times before competition.\n\n**No Notes = Weeks of Practice, Not Days**\nPractice no-notes delivery at least 10 days before competition. Last-minute memorization causes on-stage freezing. The speech should feel automatic.\n\n**Time to the Second**\nBeing under 5:00 or over 7:00 can mean disqualification. In practice, hit 5:45–6:15 every single time.\n\n**Transitions Are What Make You Sound Polished**\nSmooth transitions between points signal preparation and make your speech feel cohesive, not choppy."
            n == "Job Interview" -> "**Research the Job Description Cold**\nKnow every line of the job description given to you. Be ready to connect your skills and experiences to each requirement. Generic answers lose to specific ones every time.\n\n**Dress One Level Above Business Casual**\nBusiness professional — suit jacket, dress pants or skirt, dress shoes — is the floor, not the ceiling. First impression scoring starts the second you walk in.\n\n**Prepare 5 STAR Stories**\nHave 5 adaptable stories ready that can answer behavioral questions from different angles. Practice delivering each one in under 90 seconds.\n\n**Firm Handshake + Sustained Eye Contact**\nJudges score poise from the first moment. Firm handshake, direct eye contact, genuine smile.\n\n**Practice Live, Not Just in Your Head**\nDo 3 full mock interviews with a real person before competition. The nervousness of being questioned live is very different from rehearsing alone."
            n == "Business Plan" -> "**Know Your Financials from Memory**\nJudges will drill your revenue projections. Know your break-even, year 1 revenue, and cost structure from memory — stumbling on your own numbers is a massive red flag.\n\n**Lead with the Problem, Not the Product**\nProblem → Solution → Market → Business Model → Financials → Ask. Judges are more convinced when they feel the pain before you present the cure.\n\n**Prepare for the 3 Hardest Judge Questions**\n\"How did you arrive at this market size?\" / \"Who are your top competitors and what's your real advantage?\" / \"What if revenue assumptions are 50% wrong?\"\n\n**Every Team Member Answers Every Section**\nJudges intentionally direct questions to the person who didn't present that section.\n\n**Over-Practice Q&A, Not the Script**\nMost teams over-rehearse the script and under-rehearse Q&A. Flip this — Q&A is where placements are won."
            n == "Entrepreneurship" -> "**Ace the Objective Test First**\nThe objective test score gives you a buffer going into the case study. Know entrepreneurship fundamentals and all business plan components cold.\n\n**Use a Framework for Case Studies**\nIdentify the core problem → Analyze root cause → Propose 2–3 solutions → Recommend one with rationale → Implementation and risks.\n\n**Give Specific, Not Generic Answers**\nWeak: \"Increase marketing.\" Strong: \"Launch targeted LinkedIn ads with a \$2K/month budget, A/B testing two messages, measuring leads vs. cost per lead over 60 days.\"\n\n**Know Real-World Examples**\nJudges respond to references to real companies and market disruptions. This shows you engage with entrepreneurship beyond the classroom.\n\n**Show Genuine Excitement**\nEntrepreneurship judges respond to energy and passion. If you're solving a problem you care about, let that show."
            n == "Impromptu Speaking" -> "**Lock in Your Instant Framework**\nThe second you hear the topic: Position statement → Point 1 + example → Point 2 + example → Restate position with impact. Commit to this structure before you speak.\n\n**Start Confident, Never with Filler**\nPause silently for 2–3 seconds, then open with your position — not \"Um, well, this is a great topic...\"\n\n**Use Concrete Examples**\nJudges score higher when you reference a real company, event, or statistic. Vague statements score lower than specific ones.\n\n**Practice Daily in the Two Weeks Before Competition**\nHave a friend give you random topics and speak for 90 seconds on the spot. This is almost entirely a learnable skill.\n\n**Hit 2:00–2:45 Every Time**\nFinishing at 1:20 looks underprepared. Over 3:00 is a time penalty."
            else -> "**Master the Official Topic Outline**\nDownload the official FBLA topic outline for $n from fbla.org. This is literally what the test or rubric is built from. Cross off each topic as you master it.\n\n**${if (isTeam) "Assign Roles, Then Cross-Train\nEach team member should own specific sections AND be able to speak about all sections. Judges target the person who looks least prepared." else "Use Active Recall, Not Passive Re-reading\nMake flashcards, do practice questions, and have someone quiz you. Active recall builds memory far more effectively than re-reading notes."}\n\n**Study the Scoring Rubric**\nFor performance events, the scoring guide tells you exactly what judges are looking for and how many points each element is worth. Practice specifically hitting each criterion.\n\n**Simulate Competition Conditions**\n${if (isTeam) "Do a full run-through with your team, timed, standing up, in presentation mode. Practice Q&A responses as a team — make sure transitions between speakers are smooth." else "Take at least 2 full timed practice tests or run-throughs before competition day. Identify weak areas and spend extra time there in the final week."}\n\n**Day-Of Mindset**\nArrive early, stay hydrated, and review your key notes one last time (no new material). Confidence comes from preparation — trust your work and commit to every answer."
        }

        return ""
    }

    data class EventFormatInfo(
        val formatType: String,
        val teamSize: String,
        val duration: String,
        val structure: String,
        val materials: String,
        val scoring: String,
        val keyDetails: List<String>
    )

    fun getFormatInfo(event: FBLAEvent): EventFormatInfo {
        val n = event.name
        val calcTest = setOf("Business Calculations", "Business Math")
        val pureObjTest = setOf(
            "Accounting I", "Accounting II", "Agribusiness", "Banking & Financial Systems",
            "Business Communication", "Business Law", "Computer Problem Solving", "Cybersecurity",
            "Economics", "Healthcare Administration", "Insurance & Risk Management",
            "Introduction to Business", "Introduction to Financial Math",
            "Introduction to Information Technology", "Marketing", "Networking Concepts",
            "Personal Finance", "Political Science", "Securities & Investments",
            "Supply Chain Management"
        )
        if (n in pureObjTest || n in calcTest) return EventFormatInfo(
            formatType = "Objective Test",
            teamSize = "Individual",
            duration = "50 minutes",
            structure = "100 multiple-choice questions",
            materials = if (n in calcTest) "Silent, non-printing calculator" else "None",
            scoring = "Number correct out of 100. Top scorers from each state qualify for nationals.",
            keyDetails = listOf(
                "100 questions, machine-graded at the conference testing center",
                if (n in calcTest) "Calculator required — confirm if provided or bring a silent one" else "No notes, textbooks, or devices allowed",
                "~30 seconds per question — skip hard ones and return at the end",
                "No penalty for guessing — always fill in every answer",
                "Top scorers from state advance; ties broken by completion time"
            )
        )
        val mixedIndividual = setOf(
            "Entrepreneurship", "Global Business", "Hospitality & Event Management",
            "Human Resource Management", "Sports & Entertainment Management"
        )
        if (n in mixedIndividual) return EventFormatInfo(
            formatType = "Mixed",
            teamSize = "Individual",
            duration = "50 min test + 7–10 min presentation",
            structure = "Objective test followed by a case study oral presentation",
            materials = "Case study packet provided ~30 min before presentation",
            scoring = "Combined: objective test score + judges' oral presentation rubric.",
            keyDetails = listOf(
                "Part 1: 100-question objective test (50 minutes)",
                "Part 2: Receive case study ~30 minutes before your presentation slot",
                "Presentation: up to 7–10 minutes + 3–5 min Q&A with judges",
                "No outside notes during presentation — only provided case materials",
                "Business professional attire required"
            )
        )
        return when (n) {
            "Public Speaking" -> EventFormatInfo("Performance", "Individual", "5–7 minutes", "Prepared speech on a business topic of your choice", "No notes allowed on stage", "Judges score on content, organization, delivery, and language use.", listOf("Memorized delivery — absolutely no notes allowed on stage", "Time penalties apply under 5:00 or over 7:00 — target 5:45–6:15", "Topic must relate to a current business issue", "Business professional attire required", "Judged on hook/intro, organization, evidence, delivery, and closing"))
            "Impromptu Speaking" -> EventFormatInfo("Performance", "Individual", "1–3 minutes", "Speak immediately on an assigned business topic", "1–2 min to collect thoughts — no notes during delivery", "Judges score on organization, confidence, vocabulary, and topic depth.", listOf("Topic assigned on the spot — no advance preparation allowed", "1–2 minutes to think silently before you begin speaking", "No notes of any kind during delivery", "Target 2:00–2:45 — finishing under 1:30 looks underprepared", "Business professional attire required"))
            "Job Interview" -> EventFormatInfo("Performance", "Individual", "7–10 minutes", "Simulated job interview with a judge panel", "Printed resume copies (bring at least 3)", "Judges score on appearance, communication, knowledge, and overall poise.", listOf("Job description provided before competition — know every line cold", "Scoring begins the moment you enter the room", "Bring at least 3 printed copies of your resume", "Firm handshake, sustained eye contact, and a confident close matter", "Business professional attire is mandatory"))
            "Introduction to Parliamentary Procedure" -> EventFormatInfo("Performance", "Individual", "~10–15 minutes", "Demonstrate parliamentary procedure knowledge individually", "None", "Judges score on motion accuracy, procedure knowledge, and delivery.", listOf("Demonstrate motions, voting procedures, and proper parliamentary decorum", "Study Robert's Rules of Order thoroughly — know every main motion", "Common errors: incorrect seconding, wrong voting thresholds", "Business professional attire required", "Practice with a partner calling random scenarios at you"))
            "Parliamentary Procedure" -> EventFormatInfo("Performance", "Team: 5–8 members", "~20–30 minutes", "Team conducts a full simulated parliamentary meeting", "Official agenda provided at event", "Entire team scored on procedure accuracy, roles, and execution.", listOf("Team of 5–8 conducts a complete meeting following Robert's Rules of Order", "Roles: Chairperson, Secretary, and floor members — all are judged", "Every member must know ALL motions — judges target the quiet ones", "Practice running complete mock meetings from opening to adjournment", "Business professional attire required for all team members"))
            "Help Desk" -> EventFormatInfo("Role Play", "Individual", "~15–30 minutes", "Simulated IT help desk scenario with an evaluator acting as the customer", "Scenario packet provided at the event", "Evaluator scores on technical knowledge, problem-solving, and communication.", listOf("Given a technical support scenario with an evaluator playing a customer", "Diagnose the problem and walk the 'customer' through a solution", "Scored on accuracy, communication clarity, and professional conduct", "Requires strong IT knowledge: networking, software, security, hardware", "Business professional attire required"))
            "Computer Applications" -> EventFormatInfo("Production Task", "Individual", "~45–60 minutes", "Complete assigned computer production tasks on provided equipment", "Computer and Microsoft Office provided at testing center", "Scored on accuracy, formatting compliance, and task completion.", listOf("Tasks include word processing, spreadsheets, and presentation work", "Completed on a provided computer — software is Microsoft Office", "Formatting precision matters — follow every instruction exactly", "Practice speed: both accuracy and time factor into scoring", "Business casual or professional attire"))
            "Word Processing" -> EventFormatInfo("Production Task", "Individual", "~45 minutes", "Complete timed word processing production tasks", "Computer and word processing software provided at testing center", "Speed and accuracy both scored — errors are penalized by point deductions.", listOf("Tasks include letters, memos, tables, and formatted reports", "Both speed AND accuracy scored — errors deduct points", "Know business letter, memo, and report formatting cold", "MLA and APA formatting knowledge is also helpful", "Business professional attire required"))
            "Future Business Educator" -> EventFormatInfo("Performance", "Individual", "~30 min lesson + interview", "Teach a business lesson to a judge panel + complete an objective test", "Lesson plan and teaching materials you create in advance", "Scored on lesson design, instructional delivery, and objective test score.", listOf("Prepare and deliver a full business lesson to a panel of judges", "Also includes an objective test component on business education content", "Create your own lesson plan, activities, and student-facing materials", "Scored on content accuracy, student engagement, and instructional design", "Business professional attire required"))
            "Electronic Career Portfolio" -> EventFormatInfo("Project", "Individual", "Portfolio submitted before conference + on-site interview", "Build a digital career portfolio, then defend it in a live interview", "Portfolio submitted digitally before the conference deadline", "Combined: pre-scored portfolio quality + live interview performance at conference.", listOf("Build a professional online portfolio: resume, career goals, work samples, references", "Portfolio is scored before the conference on completeness and quality", "On-site interview at conference adds to your final score", "Treat the interview like a real job interview — business professional attire required", "Use a clean, professional portfolio platform and proofread everything"))
            "Database Design & Applications" -> EventFormatInfo("Project", "Individual", "Project submitted before conference + on-site component", "Design and build a functional relational database for a given scenario", "Project files and documentation submitted before conference deadline", "Scored on database design quality, functionality, relationships, and documentation.", listOf("Design a relational database solving a real-world business problem", "Submit database files and full documentation before the deadline", "May include a live demonstration component at the conference", "Relationships, queries, forms, and reports are all scored", "Business professional attire"))
            "Graphic Design" -> EventFormatInfo("Project", "1–3 members", "Project submitted before conference", "Design a graphic piece meeting given topic and specifications", "Project submitted digitally before the conference deadline", "Scored on design quality, visual principles, creativity, and specification adherence.", listOf("Design topic and specifications provided at the start of the competitive year", "Submit final design files before the conference deadline", "Judged on visual hierarchy, typography, color, creativity, and spec compliance", "Common tools: Adobe Creative Suite, Canva, Figma", "Business professional attire for any conference appearance"))
            "Digital Video Production" -> EventFormatInfo("Project", "1–3 members", "3–5 minute video submitted before conference", "Produce a short professional business video on an assigned topic", "Video submitted digitally before the conference deadline", "Scored on production quality, content accuracy, editing, and creativity.", listOf("Topic assigned at the start of the competitive year", "Produce a 3–5 minute professional-quality business video", "Scored on script, production quality, audio, editing, and message clarity", "Clearly define team roles: director, editor, on-camera talent", "Submit video file and all required documentation before the deadline"))
            "Social Media Strategies" -> EventFormatInfo("Project", "Individual", "Project submitted before conference", "Develop a complete social media marketing strategy for a given scenario", "Plan submitted digitally before the conference deadline", "Scored on strategy depth, creativity, feasibility, and professional presentation.", listOf("Topic/scenario provided at the start of the competitive year", "Create a comprehensive social media marketing plan", "Include: platform selection rationale, content calendar, and KPI tracking plan", "Scored on strategy quality, creativity, and professional document formatting", "Submit project before the conference deadline"))
            "UX Design" -> EventFormatInfo("Project", "Individual", "Project submitted before conference + on-site presentation", "Design a UX solution for a given business scenario", "Project submitted digitally; visual aids allowed for the presentation", "Scored on UX process rigor, prototype quality, and presentation clarity.", listOf("Design scenario provided at the start of the competitive year", "Create wireframes, interactive prototypes, and UX documentation", "Submit project files before the conference deadline", "Present and defend design decisions to judges at the conference", "Scored on research, usability, visual design quality, and rationale"))
            "Mobile Application Development" -> EventFormatInfo("Project", "1–3 members", "App submitted before conference + oral presentation at conference", "Build a fully working mobile app for a business use case", "App and documentation submitted before the conference deadline", "Scored on app functionality, UI/UX design, code quality, and oral presentation.", listOf("Develop a fully functional iOS or Android application", "Topic/scenario provided at the start of the competitive year", "Submit app files and complete documentation before the deadline", "Live presentation + Q&A with judges at conference (~10 minutes)", "Scored on functionality, design, technical quality, and presentation"))
            "Website Design" -> EventFormatInfo("Project", "1–3 members", "Website submitted before conference + oral presentation at conference", "Design and build a website for a given business scenario", "Website submitted before conference; visual aids allowed during presentation", "Scored on site design, functionality, and oral presentation quality.", listOf("Scenario and specifications provided at the start of the competitive year", "Build a fully functional, live website and submit the URL + documentation", "Oral presentation + Q&A with judges at conference (7–10 minutes)", "Scored on design, usability, technical quality, and content accuracy", "Business professional attire required"))
            "Business Plan" -> EventFormatInfo("Project + Presentation", "1–3 members", "Written plan submitted + 10 min oral presentation", "Write a full business plan and present it to a judge panel", "Plan submitted before conference; visual aids and handouts allowed", "Combined score: written plan + oral presentation + Q&A performance.", listOf("Submit a complete business plan before the conference deadline", "Oral presentation: up to 10 minutes + Q&A with judges", "Plan must cover all sections — exec summary through financial projections", "Know your financials from memory — judges will drill your numbers", "Business professional attire required"))
            "Business Ethics" -> EventFormatInfo("Project + Presentation", "1–3 members", "Research project submitted + 10 min oral presentation", "Research a real business ethics case and present findings to judges", "Research project submitted before conference; visual aids allowed", "Combined score: written project quality + oral presentation + Q&A.", listOf("Research and analyze a real-world business ethics situation", "Submit research project and full documentation before the deadline", "Oral presentation: up to 10 minutes + Q&A with judges", "Scored on research depth, ethical analysis quality, and delivery", "Business professional attire required"))
            "E-business" -> EventFormatInfo("Project + Presentation", "1–3 members", "Project submitted before conference + 7 min oral presentation", "Develop an e-business strategy or solution and present it to judges", "Project submitted before conference; visual aids allowed", "Combined score: project quality + oral presentation + Q&A.", listOf("Create a comprehensive e-business plan or functional website", "Submit project before the conference deadline", "Oral presentation: up to 7 minutes + Q&A with judges", "Address a real e-commerce business opportunity with detailed analysis", "Business professional attire required"))
            "Network Design" -> EventFormatInfo("Project + Presentation", "1–3 members", "Project submitted before conference + oral presentation at conference", "Design a complete network infrastructure solution for a given scenario", "Project submitted before conference; visual aids allowed", "Scored on design accuracy, scalability, security, and oral presentation.", listOf("Network infrastructure scenario provided at the start of the competitive year", "Submit detailed network diagrams and documentation before the deadline", "Oral presentation + Q&A explaining every design decision to judges", "Scored on technical accuracy, scalability, security, and cost-effectiveness", "Business professional attire required"))
            "Community Service Project" -> EventFormatInfo("Project + Presentation", "Chapter team", "Project submitted before conference + 10 min oral presentation", "Execute a community service project, document it, and present to judges", "Project portfolio submitted; visual aids and display boards allowed", "Scored on community impact, documentation quality, and presentation delivery.", listOf("Entire chapter participates in a meaningful community service initiative", "Document everything: photos, volunteer hours, impact metrics, and reports", "Submit project portfolio before the conference deadline", "Representative team presents at the conference — up to 10 min + Q&A", "Scored on project scope, measurable community impact, and documentation"))
            "Management Decision Making" -> EventFormatInfo("Case Study", "1–3 members", "30 min prep + 7 min presentation + Q&A", "Analyze a business case study and present your solution to judges", "Case study packet provided — no outside materials allowed", "Judges score on analysis depth, solution quality, and presentation delivery.", listOf("Receive the business case study 30 minutes before your presentation slot", "Work with your team to analyze the problem and build a structured solution", "Present to judges: 7 minutes + Q&A — stay composed under questioning", "No outside notes or resources — only the provided case materials", "Business professional attire required"))
            "Emerging Business Issues" -> EventFormatInfo("Mixed", "1–3 members", "50 min test + 7 min presentation + Q&A", "Objective test on current business issues + team case study presentation", "Case study packet provided for the presentation phase", "Combined: objective test score + judges' presentation rubric.", listOf("Part 1: 100-question objective test on current business issues (50 min)", "Part 2: Case study distributed 30 min before your presentation slot", "Team presentation: 7 minutes + Q&A with judges", "Covers technology trends, global business, ethics, and economic issues", "Business professional attire required"))
            "Client Service" -> EventFormatInfo("Role Play", "1–3 members", "~15 minutes", "Simulated customer service scenario with an evaluator playing a customer", "Scenario provided at the event", "Evaluator scores on customer handling, communication, and problem resolution.", listOf("Given a customer service scenario with an evaluator playing the customer", "Resolve the customer's problem or complaint professionally and empathetically", "Scored on professionalism, empathy, communication, and resolution quality", "Active listening and de-escalation skills are critical", "Business professional attire required"))
            "Sales Presentation" -> EventFormatInfo("Performance", "1–3 members", "10–12 minutes", "Prepared sales presentation delivered to a panel of judge-buyers", "Visual aids, demonstrations, and product samples allowed and encouraged", "Judges score on sales technique, product knowledge, objection handling, and close.", listOf("Present a complete sales pitch for an assigned or chosen product/service", "Judges act as potential buyers — handle every objection professionally", "Visual aids, live demos, and handouts are strongly encouraged", "Scored on opening, needs assessment, benefits pitch, objection handling, and close", "Business professional attire required"))
            "Introduction to Business Communication" -> EventFormatInfo("Performance", "Individual", "3–5 minutes", "Prepared business presentation on a topic announced for the competitive year", "Limited notes may be allowed — check current official guidelines", "Scored on organization, delivery, eye contact, and overall communication.", listOf("Topic announced at the start of the competitive year", "Introductory-level prepared speech — organize clearly and deliver confidently", "Practice timing to hit the middle of the range (around 4 minutes)", "Scored on organization, eye contact, vocal variety, and content quality", "Business professional attire required"))
            else -> EventFormatInfo(
                formatType = if (event.category == "TEAM") "Team Event" else "Individual Event",
                teamSize = if (event.category == "TEAM") "Team" else "Individual",
                duration = "Varies",
                structure = "See official FBLA guidelines at fbla.org",
                materials = "See the official event guidelines from your chapter adviser",
                scoring = "Official FBLA scoring rubric — available from your chapter adviser.",
                keyDetails = listOf("Download the official topic outline from fbla.org", "Ask your chapter adviser for the current event guidelines packet", "Business professional attire is standard for all competitive events", "Practice under realistic competition conditions well before the event")
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize().background(if (lightMode) Color.White else Color(0xFF0A1628)))
        if (!lightMode) Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = bgOpacity)))

        if (selectedEvent == null) {
            // ── EVENT SELECTION SCREEN ──────────────────────────────────────
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 48.dp, start = 0.dp, end = 16.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = textColor)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("EVENT PREP HUB", style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 3.sp, fontWeight = FontWeight.Black, color = primaryColor, fontSize = (12 * textSizeMultiplier).sp))
                        Text("Choose Your Event", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black, color = textColor, fontSize = (22 * textSizeMultiplier).sp))
                    }
                    // Search toggle
                    Surface(shape = CircleShape, color = cardBg, border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.4f))) {
                        IconButton(onClick = { showSearch = !showSearch; if (!showSearch) searchQuery = "" }) {
                            Icon(if (showSearch) Icons.Default.Close else Icons.Default.Search, null, tint = primaryColor)
                        }
                    }
                }

                // Search bar
                androidx.compose.animation.AnimatedVisibility(visible = showSearch) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search events...", color = textColorTertiary) },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textColor, unfocusedTextColor = textColor,
                            focusedBorderColor = primaryColor, unfocusedBorderColor = primaryColor.copy(alpha = 0.4f)
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = primaryColor) }
                    )
                }

                // Category filter chips
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf("ALL", "INDIVIDUAL", "TEAM").forEach { cat ->
                        val isActive = activeCategory == cat
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isActive) primaryColor else cardBg,
                            border = BorderStroke(1.dp, if (isActive) primaryColor else primaryColor.copy(alpha = 0.3f)),
                            modifier = Modifier.clickable { activeCategory = cat }
                        ) {
                            Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(when (cat) { "ALL" -> Icons.Default.GridView; "INDIVIDUAL" -> Icons.Default.Person; else -> Icons.Default.Group }, null, tint = if (isActive) Color.Black else primaryColor, modifier = Modifier.size(14.dp))
                                Text(when (cat) { "ALL" -> "All"; "INDIVIDUAL" -> "Individual"; else -> "Team" }, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = (12 * textSizeMultiplier).sp), color = if (isActive) Color.Black else textColor)
                            }
                        }
                    }
                }

                // Count
                Text(
                    "${filteredEvents.size} events",
                    color = textColorTertiary,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = (11 * textSizeMultiplier).sp),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp)
                )

                // Event grid
                LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
                    filteredEvents.chunked(2).forEach { rowItems ->
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                rowItems.forEach { event ->
                                    Card(
                                        modifier = Modifier.weight(1f).height(126.dp),
                                        colors = CardDefaults.cardColors(containerColor = cardBg),
                                        shape = RoundedCornerShape(16.dp),
                                        onClick = {
                                            selectedEvent = event
                                            isLoadingPrep = true
                                            activeTab = 0
                                            scope.launch {
                                                prepContent = withContext(Dispatchers.Default) { getPrepContent(event, 0) }
                                                isLoadingPrep = false
                                            }
                                        }
                                    ) {
                                        Column(
                                            modifier = Modifier.fillMaxSize().padding(12.dp),
                                            verticalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column {
                                                Icon(event.icon, null, tint = primaryColor, modifier = Modifier.size((26 * textSizeMultiplier).dp))
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(
                                                    event.name,
                                                    color = textColor,
                                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = (12 * textSizeMultiplier).sp),
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    event.category,
                                                    color = primaryColor,
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = (10 * textSizeMultiplier).sp, letterSpacing = 1.sp)
                                                )
                                            }
                                            val fmtLabel = when (getFormatInfo(event).formatType) {
                                                "Objective Test" -> "Obj. Test"
                                                "Project + Presentation" -> "Proj. + Pres."
                                                "Case Study" -> "Case Study"
                                                "Role Play" -> "Role Play"
                                                "Performance" -> "Performance"
                                                "Mixed" -> "Mixed"
                                                else -> getFormatInfo(event).formatType
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = primaryColor.copy(alpha = 0.13f)
                                            ) {
                                                Text(
                                                    fmtLabel,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    color = primaryColor,
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = (9 * textSizeMultiplier).sp)
                                                )
                                            }
                                        }
                                    }
                                }
                                if (rowItems.size == 1) Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(40.dp)) }
                }
            }
        } else {
            // ── EVENT PREP DETAIL SCREEN ────────────────────────────────────
            val event = selectedEvent!!
            Column(modifier = Modifier.fillMaxSize()) {
                // Header with back + event name
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 48.dp, start = 0.dp, end = 16.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { selectedEvent = null; prepContent = "" }, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = textColor)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("EVENT PREP HUB", style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.sp, fontWeight = FontWeight.Black, color = primaryColor, fontSize = (10 * textSizeMultiplier).sp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(event.icon, null, tint = primaryColor, modifier = Modifier.size(22.dp))
                            Text(event.name, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black, color = textColor, fontSize = (19 * textSizeMultiplier).sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = primaryColor.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.5f))
                    ) {
                        Text(
                            event.category,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = (10 * textSizeMultiplier).sp, letterSpacing = 1.sp),
                            color = primaryColor
                        )
                    }
                }

                // Tab row
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    data class TabDef(val label: String, val icon: ImageVector)
                    listOf(TabDef("Overview", Icons.Default.Info), TabDef("Format", Icons.Default.TableChart), TabDef("Win Tips", Icons.Default.EmojiEvents)).forEachIndexed { index, tabDef ->
                        val isActive = activeTab == index
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isActive) primaryColor else cardBg,
                            border = BorderStroke(1.dp, if (isActive) primaryColor else primaryColor.copy(alpha = 0.25f)),
                            modifier = Modifier.weight(1f).clickable {
                                if (activeTab != index) {
                                    activeTab = index
                                    if (index != 1) {
                                        isLoadingPrep = true
                                        val tabArg = if (index == 0) 0 else 1
                                        scope.launch {
                                            prepContent = withContext(Dispatchers.Default) { getPrepContent(event, tabArg) }
                                            isLoadingPrep = false
                                        }
                                    }
                                }
                            }
                        ) {
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 11.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                                Icon(tabDef.icon, null, tint = if (isActive) Color.Black else primaryColor, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(tabDef.label, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = (11 * textSizeMultiplier).sp), color = if (isActive) Color.Black else textColorSecondary)
                            }
                        }
                    }
                }

                // Content area
                if (isLoadingPrep) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            CircularProgressIndicator(color = primaryColor, modifier = Modifier.size(40.dp))
                            Text("Loading prep content...", color = textColorTertiary, style = MaterialTheme.typography.bodySmall.copy(fontSize = (13 * textSizeMultiplier).sp))
                        }
                    }
                } else if (activeTab == 1) {
                    val fmt = getFormatInfo(event)
                    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        item {
                            // Header card — format type + stat chips
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = primaryColor.copy(alpha = 0.15f)),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.4f))
                            ) {
                                Column(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Icon(Icons.Default.TableChart, null, tint = primaryColor, modifier = Modifier.size(22.dp))
                                        Text(fmt.formatType, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, fontSize = (16 * textSizeMultiplier).sp), color = primaryColor)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        listOf(
                                            Triple(Icons.Default.Group, "TEAM SIZE", fmt.teamSize),
                                            Triple(Icons.Default.Timer, "DURATION", fmt.duration)
                                        ).forEach { (icon, label, value) ->
                                            Surface(shape = RoundedCornerShape(10.dp), color = cardBg, border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.25f)), modifier = Modifier.weight(1f)) {
                                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                        Icon(icon, null, tint = primaryColor, modifier = Modifier.size(12.dp))
                                                        Text(label, color = textColorTertiary, style = MaterialTheme.typography.labelSmall.copy(fontSize = (9 * textSizeMultiplier).sp, fontWeight = FontWeight.Bold))
                                                    }
                                                    Text(value, color = textColor, style = MaterialTheme.typography.bodySmall.copy(fontSize = (11 * textSizeMultiplier).sp, fontWeight = FontWeight.Bold), maxLines = 2, overflow = TextOverflow.Ellipsis)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        item {
                            // Details card — structure, materials, scoring
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = cardBg),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                    @Composable
                                    fun DetailSection(icon: ImageVector, label: String, body: String) {
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Icon(icon, null, tint = primaryColor, modifier = Modifier.size(14.dp))
                                                Text(label, color = primaryColor, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = (10 * textSizeMultiplier).sp, letterSpacing = 1.sp))
                                            }
                                            Text(body, color = textColorSecondary, style = MaterialTheme.typography.bodySmall.copy(fontSize = (13 * textSizeMultiplier).sp))
                                        }
                                    }
                                    DetailSection(Icons.Default.AccountTree, "STRUCTURE", fmt.structure)
                                    androidx.compose.material3.HorizontalDivider(color = primaryColor.copy(alpha = 0.12f))
                                    DetailSection(Icons.AutoMirrored.Filled.LibraryBooks, "MATERIALS ALLOWED", fmt.materials)
                                    androidx.compose.material3.HorizontalDivider(color = primaryColor.copy(alpha = 0.12f))
                                    DetailSection(Icons.Default.Star, "SCORING", fmt.scoring)
                                }
                            }
                        }
                        item {
                            // Key details numbered list
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = cardBg),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(Icons.Default.Lightbulb, null, tint = primaryColor, modifier = Modifier.size(14.dp))
                                        Text("KEY DETAILS", color = primaryColor, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = (10 * textSizeMultiplier).sp, letterSpacing = 1.sp))
                                    }
                                    fmt.keyDetails.forEachIndexed { i, detail ->
                                        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            Surface(shape = CircleShape, color = primaryColor, modifier = Modifier.size(20.dp)) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text("${i + 1}", color = Color.Black, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = (9 * textSizeMultiplier).sp))
                                                }
                                            }
                                            Text(detail, color = textColorSecondary, style = MaterialTheme.typography.bodySmall.copy(fontSize = (13 * textSizeMultiplier).sp), modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(60.dp))
                        }
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = cardBg),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                                    // Parse and render the content with bold markdown-style text
                                    prepContent.split("\n").forEach { line ->
                                        when {
                                            line.startsWith("**") && line.endsWith("**") -> {
                                                Text(
                                                    line.removePrefix("**").removeSuffix("**"),
                                                    color = primaryColor,
                                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black, fontSize = (14 * textSizeMultiplier).sp),
                                                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                                                )
                                            }
                                            line.startsWith("•") || line.startsWith("-") -> {
                                                Row(modifier = Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.Top) {
                                                    Text("•", color = primaryColor, style = MaterialTheme.typography.bodyMedium.copy(fontSize = (14 * textSizeMultiplier).sp), modifier = Modifier.padding(end = 8.dp, top = 1.dp))
                                                    Text(line.removePrefix("•").removePrefix("-").trim(), color = textColorSecondary, style = MaterialTheme.typography.bodyMedium.copy(fontSize = (14 * textSizeMultiplier).sp))
                                                }
                                            }
                                            line.isNotBlank() -> {
                                                Text(line, color = textColor, style = MaterialTheme.typography.bodyMedium.copy(fontSize = (14 * textSizeMultiplier).sp), modifier = Modifier.padding(vertical = 2.dp))
                                            }
                                            else -> Spacer(modifier = Modifier.height(4.dp))
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))

                            // Quick-action cards at the bottom
                            Text("QUICK ACTIONS", color = primaryColor, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, letterSpacing = 2.sp, fontSize = (11 * textSizeMultiplier).sp), modifier = Modifier.padding(bottom = 10.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                data class QuickAction(val icon: ImageVector, val label: String)
                                listOf(
                                    QuickAction(Icons.Default.NotificationAdd, "Reminders"),
                                    QuickAction(Icons.Default.FormatListBulleted, "All Events"),
                                    QuickAction(Icons.AutoMirrored.Filled.LibraryBooks, "Guidelines")
                                ).forEach { action ->
                                    Card(
                                        modifier = Modifier.weight(1f).height(70.dp),
                                        colors = CardDefaults.cardColors(containerColor = primaryColor.copy(alpha = 0.15f)),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.3f)),
                                        onClick = {}
                                    ) {
                                        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                            Icon(action.icon, null, tint = primaryColor, modifier = Modifier.size((20 * textSizeMultiplier).dp))
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(action.label, color = textColorSecondary, textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall.copy(fontSize = (10 * textSizeMultiplier).sp, fontWeight = FontWeight.Bold))
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(60.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TestingHub(
    onBack: () -> Unit,
    primaryColor: Color,
    bgOpacity: Float,
    isSimplifiedUI: Boolean,
    textSizeMultiplier: Float,
    lightMode: Boolean = false
) {
    val textColor = if (lightMode) Color.Black else Color.White
    val textColorSecondary = if (lightMode) Color.Black.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.9f)
    val cardBg = if (lightMode) Color(0xFFE8EDF5) else Color.White.copy(alpha = 0.1f)

    data class QuizQuestion(val question: String, val options: List<String>, val correctIndex: Int, val explanation: String = "")
    data class TestEvent(val name: String, val icon: ImageVector, val questions: List<QuizQuestion>)

    val allEvents = remember {
        listOf(
            TestEvent("Business Law", Icons.Default.Gavel, listOf(
                QuizQuestion("What type of contract is formed when both parties exchange promises?", listOf("Void contract", "Bilateral contract", "Unilateral contract", "Quasi-contract"), 1, "A bilateral contract involves mutual promises — one offer, one acceptance, creating obligations on both sides."),
                QuizQuestion("Which element is NOT required for a valid contract?", listOf("Offer", "Acceptance", "Written form", "Consideration"), 2, "Contracts don't have to be in writing to be valid — most oral contracts are fully enforceable."),
                QuizQuestion("A contract signed by a minor is generally:", listOf("Void", "Voidable", "Valid and binding", "Illegal"), 1, "Contracts with minors are voidable — the minor can choose to disaffirm the contract."),
                QuizQuestion("The Statute of Frauds requires certain contracts to be:", listOf("Notarized", "In writing", "Witnessed by two people", "Registered with the state"), 1, "The Statute of Frauds requires certain contracts (like real estate sales) to be in writing to be enforceable."),
                QuizQuestion("Which business entity offers limited liability to ALL owners?", listOf("Sole proprietorship", "General partnership", "LLC", "Limited partnership"), 2, "An LLC protects all members from personal liability for business debts."),
                QuizQuestion("Bankruptcy under Chapter 11 allows a business to:", listOf("Liquidate all assets", "Reorganize and continue operating", "Transfer debts to owners", "Avoid all creditors permanently"), 1, "Chapter 11 is a reorganization bankruptcy that lets businesses restructure debt while continuing operations."),
                QuizQuestion("The Uniform Commercial Code (UCC) primarily governs:", listOf("Employment law", "Real estate transactions", "Sale of goods", "Intellectual property"), 2, "The UCC provides consistent rules for commercial transactions, especially the sale of goods."),
                QuizQuestion("Which of the following is an example of a tort?", listOf("Breach of contract", "Tax evasion", "Negligence", "Robbery"), 2, "A tort is a civil wrong — negligence (failure to exercise reasonable care) is a classic example."),
                QuizQuestion("Copyright protects:", listOf("Inventions", "Original works of authorship", "Brand names", "Trade secrets"), 1, "Copyright protects original creative works like books, music, and software automatically upon creation."),
                QuizQuestion("Apparent authority arises when:", listOf("An agent exceeds written authority", "A principal's actions make a third party reasonably believe authority exists", "An agent acts illegally", "A contract is breached"), 1, "Apparent authority exists when the principal's conduct leads a reasonable third party to believe the agent has authority.")
            )),
            TestEvent("Economics", Icons.Default.TrendingUp, listOf(
                QuizQuestion("When demand increases and supply stays constant, price will:", listOf("Decrease", "Stay the same", "Increase", "Become inelastic"), 2, "Higher demand with unchanged supply means buyers compete, driving price up."),
                QuizQuestion("GDP stands for:", listOf("General Demand Price", "Gross Domestic Product", "Government Distribution Plan", "Gross Deficit Percentage"), 1, "GDP measures the total value of all goods and services produced within a country."),
                QuizQuestion("The Federal Reserve controls inflation primarily by:", listOf("Changing tax rates", "Adjusting interest rates", "Printing currency", "Setting import tariffs"), 1, "The Fed raises interest rates to slow borrowing and spending, reducing inflation."),
                QuizQuestion("A market with only one seller is called:", listOf("Oligopoly", "Monopoly", "Perfect competition", "Monopsony"), 1, "A monopoly exists when a single company controls the entire market for a product."),
                QuizQuestion("Opportunity cost is best defined as:", listOf("The price of a good", "The cost of production", "The value of the next best alternative foregone", "Total sunk costs"), 2, "Opportunity cost is what you give up when choosing one option over the next best alternative."),
                QuizQuestion("A command economy is controlled by:", listOf("Free markets", "Consumer choices", "The government", "Corporations"), 2, "In a command economy, the government makes all major economic decisions about production and distribution."),
                QuizQuestion("Inflation is best described as:", listOf("A decrease in GDP", "A general rise in price levels", "An increase in unemployment", "A drop in interest rates"), 1, "Inflation is the rate at which the general level of prices rises over time, reducing purchasing power."),
                QuizQuestion("The law of diminishing returns states that:", listOf("Profits always increase with production", "Adding more of one input eventually yields smaller gains", "Costs decrease as output rises", "Supply always meets demand"), 1, "After a point, adding more of one factor of production yields progressively smaller output gains."),
                QuizQuestion("A tariff is:", listOf("A domestic production subsidy", "A tax on imported goods", "A limit on the quantity of imports", "A trade agreement"), 1, "A tariff is a tax imposed on goods imported from another country, making them more expensive."),
                QuizQuestion("For normal goods, as consumer income increases, demand typically:", listOf("Decreases", "Stays the same", "Increases", "Becomes perfectly inelastic"), 2, "For normal goods, higher income means consumers can afford more, so demand increases.")
            )),
            TestEvent("Accounting I", Icons.Default.Calculate, listOf(
                QuizQuestion("The accounting equation is:", listOf("Assets = Revenue - Expenses", "Assets = Liabilities + Owner's Equity", "Revenue = Assets + Liabilities", "Equity = Assets - Revenue"), 1, "Assets = Liabilities + Owner's Equity — this must always balance."),
                QuizQuestion("Which statement shows revenues and expenses?", listOf("Balance sheet", "Cash flow statement", "Income statement", "Statement of equity"), 2, "The income statement summarizes revenues, costs, and expenses during a specific period."),
                QuizQuestion("A debit to an asset account will:", listOf("Decrease it", "Increase it", "Have no effect", "Close it"), 1, "Assets have a normal debit balance, so debiting an asset account increases it."),
                QuizQuestion("Accounts Receivable is classified as:", listOf("A liability", "An equity account", "A current asset", "A long-term asset"), 2, "Accounts Receivable is money owed to the business, expected within a year — a current asset."),
                QuizQuestion("The matching principle states that:", listOf("Assets match liabilities", "Expenses are recorded in the same period as related revenues", "Revenue equals expenses", "Cash equals net income"), 1, "The matching principle ensures expenses are recognized in the same period as the revenues they generate."),
                QuizQuestion("Accumulated Depreciation appears on the balance sheet as:", listOf("A liability", "A contra-asset", "Owner's equity", "An expense"), 1, "Accumulated Depreciation is a contra-asset that reduces the asset's book value."),
                QuizQuestion("Which of the following is a liability?", listOf("Prepaid insurance", "Accounts payable", "Equipment", "Retained earnings"), 1, "Accounts Payable is money owed by the business to suppliers — a current liability."),
                QuizQuestion("A trial balance verifies that:", listOf("Net income is correct", "Total debits equal total credits", "Taxes are paid", "Inventory is accurate"), 1, "A trial balance lists all accounts and confirms that total debits equal total credits."),
                QuizQuestion("GAAP stands for:", listOf("General Accounting and Auditing Principles", "Generally Accepted Accounting Principles", "Government Approved Accounting Procedures", "General Asset Allocation Plan"), 1, "GAAP is the standard framework of accounting rules used in the United States."),
                QuizQuestion("Owner withdrawals are recorded by:", listOf("Debiting Cash, crediting Capital", "Debiting Drawings, crediting Cash", "Debiting Revenue, crediting Cash", "Crediting Drawings, debiting Capital"), 1, "Owner withdrawals: Debit Drawings (reduces equity) and Credit Cash (reduces assets).")
            )),
            TestEvent("Personal Finance", Icons.Default.AccountBalanceWallet, listOf(
                QuizQuestion("A credit score is primarily determined by:", listOf("Your income level", "Payment history and credit utilization", "Number of bank accounts", "Employment status"), 1, "Payment history (35%) and credit utilization (30%) are the two biggest FICO score factors."),
                QuizQuestion("The Rule of 72 estimates:", listOf("Loan repayment time", "How long to double an investment", "Monthly budget allocations", "Credit card interest"), 1, "Divide 72 by the annual interest rate to estimate years for an investment to double."),
                QuizQuestion("Which investment typically has the highest risk AND highest potential return?", listOf("Savings account", "Government bonds", "Stocks", "Certificates of deposit"), 2, "Stocks historically offer the highest returns but carry the most volatility and risk."),
                QuizQuestion("Roth IRA withdrawals in retirement are:", listOf("Fully taxed", "Tax-free", "Penalized", "Limited to $500/month"), 1, "Roth IRA contributions are after-tax, so qualified withdrawals in retirement are completely tax-free."),
                QuizQuestion("Net worth is calculated as:", listOf("Income - Expenses", "Assets - Liabilities", "Revenue - Costs", "Savings - Debt"), 1, "Net worth = Total Assets - Total Liabilities, representing your true financial position."),
                QuizQuestion("Collision insurance covers:", listOf("Damage to another person's car", "Damage to your car in an accident", "Medical bills after an accident", "Theft of your vehicle"), 1, "Collision insurance covers damage to your own vehicle regardless of fault."),
                QuizQuestion("Compound interest differs from simple interest because:", listOf("It uses a lower rate", "Interest is earned on interest", "It only applies to savings", "It has fewer fees"), 1, "Compound interest calculates interest on the principal AND previously earned interest."),
                QuizQuestion("An emergency fund should cover:", listOf("1 week of expenses", "1 month of expenses", "3–6 months of expenses", "1 year of expenses"), 2, "Financial experts recommend 3–6 months of living expenses for a proper emergency fund."),
                QuizQuestion("A 401(k) plan is:", listOf("A government pension program", "An employer-sponsored retirement savings plan", "A type of savings account", "Federal health insurance"), 1, "A 401(k) is an employer-sponsored retirement plan with pre-tax contributions, often employer-matched."),
                QuizQuestion("Diversification in investing means:", listOf("Investing only in bonds", "Putting all money in one stock", "Spreading investments across different assets", "Avoiding international markets"), 2, "Diversification reduces risk by spreading investments across various asset types, sectors, and geographies.")
            )),
            TestEvent("Marketing", Icons.Default.Campaign, listOf(
                QuizQuestion("The 4 Ps of marketing are:", listOf("People, Process, Planning, Profit", "Product, Price, Place, Promotion", "Product, Profit, Public, Positioning", "Price, Placement, People, Platform"), 1, "The marketing mix: Product, Price, Place (distribution), and Promotion."),
                QuizQuestion("A target market is:", listOf("The largest possible audience", "A specific group of consumers a business aims at", "All people who buy a product", "The media used for advertising"), 1, "A target market is the defined segment most likely to want and buy your product."),
                QuizQuestion("Brand equity refers to:", listOf("The cash value of a brand", "The additional value a brand name gives a product", "The cost to advertise", "Legal trademark protection"), 1, "Brand equity is the premium value a product gains from a recognizable brand name."),
                QuizQuestion("A loss leader pricing strategy involves:", listOf("Pricing above competitors", "Selling a product below cost to attract customers", "Reducing production costs", "Bundling products together"), 1, "Loss leader pricing draws customers in with a below-cost product, expecting profitable secondary purchases."),
                QuizQuestion("SWOT analysis stands for:", listOf("Sales, Work, Operations, Trends", "Strengths, Weaknesses, Opportunities, Threats", "Strategy, Win, Optimize, Target", "Speed, Width, Output, Technology"), 1, "SWOT examines internal Strengths/Weaknesses and external Opportunities/Threats."),
                QuizQuestion("SEO aims to:", listOf("Buy ad space on Google", "Improve organic search rankings", "Create viral social media posts", "Design better websites"), 1, "SEO improves a website's visibility in organic (unpaid) search engine results."),
                QuizQuestion("Market segmentation divides consumers by:", listOf("Random selection", "Demographics, psychographics, behavior, or geography", "Income levels only", "Government regulations"), 1, "Segmentation groups consumers by demographics (age, gender), psychographics, behavior, or geography."),
                QuizQuestion("Customer lifetime value (CLV) measures:", listOf("One-time purchase value", "Total revenue from a customer over the relationship", "Cost to acquire a customer", "Monthly sales average"), 1, "CLV is the total net profit expected from a customer throughout their entire relationship with a business."),
                QuizQuestion("A push distribution strategy:", listOf("Pulls consumers to buy", "Pushes product through the supply chain to retailers", "Focuses on online sales only", "Eliminates intermediaries"), 1, "Push strategy promotes products through the supply chain — manufacturer to distributor to retailer."),
                QuizQuestion("Advertising differs from publicity in that advertising is:", listOf("Free", "Paid and from an identified sponsor", "Always digital", "Word of mouth"), 1, "Advertising is paid, non-personal communication from an identified sponsor through media channels.")
            )),
            TestEvent("Introduction to Business", Icons.Default.Business, listOf(
                QuizQuestion("Which business form is easiest to start?", listOf("Corporation", "Partnership", "Sole proprietorship", "LLC"), 2, "A sole proprietorship requires minimal legal formalities — one person owns and operates the business."),
                QuizQuestion("A corporation's owners are called:", listOf("Partners", "Members", "Shareholders", "Proprietors"), 2, "Corporations issue stock; those who own that stock are shareholders."),
                QuizQuestion("Working capital is:", listOf("Total assets - Total liabilities", "Current assets - Current liabilities", "Revenue - Expenses", "Cash + Receivables"), 1, "Working capital = Current Assets - Current Liabilities, measuring short-term financial health."),
                QuizQuestion("The primary goal of most businesses is to:", listOf("Create jobs", "Pay taxes", "Earn a profit", "Help the community"), 2, "While businesses have many stakeholders, generating profit for owners is the primary financial objective."),
                QuizQuestion("Human Resources manages:", listOf("Product development", "Financial reporting", "Hiring and employee relations", "Supply chain"), 2, "HR handles recruitment, hiring, training, benefits, and employee relations."),
                QuizQuestion("E-commerce refers to:", listOf("Electronic accounting", "Buying and selling over the internet", "Email marketing", "Stock trading"), 1, "E-commerce is the buying and selling of goods and services via the internet."),
                QuizQuestion("A fixed cost:", listOf("Changes with production volume", "Stays constant regardless of output", "Is always zero at zero output", "Is the same as variable cost"), 1, "Fixed costs (like rent) remain constant regardless of how much is produced."),
                QuizQuestion("Business ethics means:", listOf("Maximizing profit at any cost", "Acting in accordance with moral principles", "Avoiding all regulations", "Prioritizing only shareholders"), 1, "Business ethics involves fair, legally compliant decisions that are morally responsible to all stakeholders."),
                QuizQuestion("Supply chain management oversees:", listOf("Employee pay scales", "Flow of goods from supplier to customer", "Setting product prices", "Customer complaints"), 1, "Supply chain management coordinates product movement from raw materials through production to the final customer."),
                QuizQuestion("Entrepreneurship involves:", listOf("Managing an existing large company", "Creating and running a new business venture", "Working for the government", "Only investing in stocks"), 1, "Entrepreneurs identify opportunities, take calculated risks, and create new business ventures.")
            )),
            TestEvent("Cybersecurity", Icons.Default.Security, listOf(
                QuizQuestion("Phishing attacks typically occur through:", listOf("Physical theft", "Deceptive emails or websites", "Software bugs", "Hardware failures"), 1, "Phishing uses fake emails or websites that appear legitimate to steal sensitive information."),
                QuizQuestion("A firewall primarily:", listOf("Encrypts data", "Monitors and filters network traffic", "Backs up data", "Removes viruses"), 1, "A firewall monitors incoming and outgoing network traffic and blocks unauthorized access."),
                QuizQuestion("Multi-factor authentication requires:", listOf("A very long password", "Multiple independent verification methods", "A physical key only", "Biometrics only"), 1, "MFA requires two or more verification factors (password + phone code, etc.) to increase security."),
                QuizQuestion("A DDoS attack:", listOf("Steals data silently", "Floods a server with traffic to shut it down", "Installs ransomware", "Intercepts emails"), 1, "A DDoS (Distributed Denial of Service) attack overwhelms a server with traffic from multiple sources."),
                QuizQuestion("Encryption converts data into:", listOf("Compressed files", "An unreadable format without the key", "Smaller file sizes", "Backup copies"), 1, "Encryption scrambles data so only someone with the correct decryption key can read it."),
                QuizQuestion("Social engineering exploits:", listOf("Software vulnerabilities", "Human psychology and trust", "Hardware weaknesses", "Network protocols"), 1, "Social engineering manipulates people rather than systems — tricking users into giving access or information."),
                QuizQuestion("Ransomware:", listOf("Slows down a computer", "Encrypts files and demands payment", "Steals passwords silently", "Deletes system logs"), 1, "Ransomware encrypts a victim's files and demands a ransom in exchange for the decryption key."),
                QuizQuestion("The CIA Triad stands for:", listOf("Computers, Internet, Access", "Confidentiality, Integrity, Availability", "Cyber, Intelligence, Authentication", "Control, Identify, Audit"), 1, "The CIA Triad is the foundational security model: Confidentiality, Integrity, and Availability."),
                QuizQuestion("A VPN provides:", listOf("Faster internet speed", "Encrypted and private network connections", "Free internet access", "Cloud storage"), 1, "A VPN (Virtual Private Network) creates an encrypted tunnel for internet traffic, protecting privacy."),
                QuizQuestion("Best practice for password security is:", listOf("Use the same password everywhere", "Use short memorable passwords", "Use long unique passwords with a password manager", "Share passwords with trusted colleagues"), 2, "Strong password hygiene: unique, long passwords for each account, managed by a password manager.")
            )),
            TestEvent("Business Communication", Icons.Default.Forum, listOf(
                QuizQuestion("The most formal written business communication is:", listOf("Text message", "Email", "Business letter", "Instant message"), 2, "A formal business letter on letterhead follows strict formatting conventions — the most formal written form."),
                QuizQuestion("Active listening involves:", listOf("Waiting for your turn to speak", "Fully concentrating, understanding, and responding thoughtfully", "Only taking detailed notes", "Agreeing with everything"), 1, "Active listening means fully engaging: eye contact, no distractions, and thoughtful responses."),
                QuizQuestion("An executive summary:", listOf("Replaces the full report", "Provides a concise overview of a longer document", "Lists all data sources", "Includes all appendices"), 1, "An executive summary gives busy readers a quick overview of key points and recommendations."),
                QuizQuestion("The subject line of a business email should:", listOf("Be left blank", "Be vague to build curiosity", "Clearly summarize the email's purpose", "Include the sender's name"), 2, "A clear, concise subject line immediately tells the recipient what the email is about."),
                QuizQuestion("The 7 Cs of effective communication include:", listOf("Clear, Concise, Concrete, Correct, Coherent, Complete, Courteous", "Creative, Candid, Concise, Caring, Clear, Correct, Cohesive", "Careful, Clear, Consistent, Correct, Complete, Constructive, Courteous", "Concise, Creative, Clear, Casual, Complete, Correct, Consistent"), 0, "The 7 Cs: Clear, Concise, Concrete, Correct, Coherent, Complete, and Courteous."),
                QuizQuestion("A memo is used for:", listOf("External customer communications", "Internal organizational communication", "Legal contracts", "Marketing campaigns"), 1, "Memos are internal documents used within an organization to share information or directives."),
                QuizQuestion("Jargon should be avoided in business writing because:", listOf("It adds professionalism", "It may confuse readers unfamiliar with the terms", "It makes writing longer", "It is always grammatically incorrect"), 1, "Jargon can exclude or confuse readers who don't share the specialized vocabulary."),
                QuizQuestion("For sensitive negative news, the best channel is:", listOf("Mass email", "Social media post", "Face-to-face meeting", "Text message"), 2, "Sensitive or complex messages warrant face-to-face delivery to allow immediate feedback and empathy."),
                QuizQuestion("Nonverbal communication includes:", listOf("Only written messages", "Body language, facial expressions, and tone", "Email and texts only", "Formal presentations"), 1, "Nonverbal communication includes body language, gestures, eye contact, expressions, and vocal tone."),
                QuizQuestion("Plagiarism in business writing means:", listOf("Using technical language", "Using someone's work without attribution", "Writing too informally", "Including too many facts"), 1, "Plagiarism is using another person's words or ideas without proper credit — unethical and potentially illegal.")
            )),
            TestEvent("Business Calculations", Icons.Default.Functions, listOf(
                QuizQuestion("An item costs \$80 with a 25% markup. Selling price is:", listOf("\$95", "\$100", "\$105", "\$110"), 1, "Selling price = \$80 × 1.25 = \$100."),
                QuizQuestion("Gross profit margin on \$200 cost / \$250 selling price:", listOf("20%", "25%", "50%", "80%"), 0, "Gross profit margin = (\$250 - \$200) / \$250 = \$50 / \$250 = 20%."),
                QuizQuestion("Simple interest on \$1,000 at 5% for 3 years:", listOf("\$50", "\$150", "\$157.63", "\$1,150"), 1, "Simple interest = Principal × Rate × Time = \$1,000 × 0.05 × 3 = \$150."),
                QuizQuestion("6% commission on \$15,000 in sales equals:", listOf("\$600", "\$900", "\$750", "\$1,050"), 1, "Commission = \$15,000 × 0.06 = \$900."),
                QuizQuestion("\$5,000 at 4% compounded annually for 2 years equals:", listOf("\$5,200", "\$5,400", "\$5,408", "\$5,416"), 2, "A = \$5,000 × (1.04)² = \$5,000 × 1.0816 = \$5,408."),
                QuizQuestion("20% discount on \$75 gives a sale price of:", listOf("\$55", "\$60", "\$65", "\$70"), 1, "Discount = \$75 × 0.20 = \$15. Sale price = \$75 - \$15 = \$60."),
                QuizQuestion("Payroll tax at 7.65% on \$2,500:", listOf("\$159.25", "\$191.25", "\$200.00", "\$215.75"), 1, "Payroll tax = \$2,500 × 0.0765 = \$191.25."),
                QuizQuestion("Break-even: Fixed costs \$10,000, price \$50, variable cost \$30:", listOf("200 units", "333 units", "500 units", "667 units"), 2, "Break-even = \$10,000 / (\$50 - \$30) = \$10,000 / \$20 = 500 units."),
                QuizQuestion("Inventory turnover of 4 with COGS \$120,000 gives avg inventory of:", listOf("\$20,000", "\$30,000", "\$40,000", "\$50,000"), 1, "Average Inventory = COGS / Turnover = \$120,000 / 4 = \$30,000."),
                QuizQuestion("15% of \$340 equals:", listOf("\$45", "\$51", "\$48", "\$56"), 1, "15% × \$340 = 0.15 × \$340 = \$51.")
            )),
            TestEvent("Networking Concepts", Icons.Default.Hub, listOf(
                QuizQuestion("IP stands for:", listOf("Internet Protocol", "Internal Process", "Integrated Platform", "Information Packet"), 0, "IP (Internet Protocol) is the fundamental protocol for addressing and routing data packets."),
                QuizQuestion("Which OSI layer handles routing between networks?", listOf("Physical", "Data Link", "Network", "Transport"), 2, "The Network layer (Layer 3) handles logical addressing (IP) and routing between networks."),
                QuizQuestion("DNS translates:", listOf("IP addresses to MAC addresses", "Domain names to IP addresses", "Packets into frames", "HTTP to HTTPS"), 1, "DNS (Domain Name System) converts human-readable domain names (google.com) into IP addresses."),
                QuizQuestion("HTTPS provides security through:", listOf("Faster routing", "SSL/TLS encryption", "Stronger passwords", "VPN tunneling"), 1, "HTTPS uses SSL/TLS encryption to secure communication between a browser and web server."),
                QuizQuestion("A LAN covers:", listOf("A country", "A continent", "A city", "A small local area like a building or campus"), 3, "A LAN (Local Area Network) connects devices in a limited geographic area like a home, office, or school."),
                QuizQuestion("The default gateway is:", listOf("The internet's main server", "A router connecting local network to external networks", "A DNS server", "A firewall"), 1, "The default gateway is the router that forwards traffic from your local network to external networks."),
                QuizQuestion("Which is a private IP address?", listOf("8.8.8.8", "172.16.0.1", "44.192.140.1", "208.67.222.222"), 1, "192.168.x.x, 10.x.x.x, and 172.16–31.x.x are private IP ranges not routable on the public internet."),
                QuizQuestion("TCP differs from UDP in that TCP:", listOf("Is faster and connectionless", "Guarantees delivery and is connection-oriented", "Is only for video streaming", "Does not use IP"), 1, "TCP provides reliable, ordered, error-checked delivery. UDP is faster but does not guarantee delivery."),
                QuizQuestion("A subnet mask is used to:", listOf("Encrypt network traffic", "Divide an IP address into network and host portions", "Assign MAC addresses", "Block malicious traffic"), 1, "A subnet mask identifies which part of an IP address is the network and which is the host."),
                QuizQuestion("A MAC address is:", listOf("A software license key", "A unique hardware identifier for a network interface", "A type of IP address", "A wireless security protocol"), 1, "A MAC (Media Access Control) address is a unique hardware identifier assigned to a network interface card.")
            ))
        )
    }

    var screen by remember { mutableStateOf("home") }
    var selectedEvent by remember { mutableStateOf<TestEvent?>(null) }
    var isTimed by remember { mutableStateOf(false) }
    var isFullTest by remember { mutableStateOf(false) }
    var questionCount by remember { mutableStateOf(10) }
    var questions by remember { mutableStateOf<List<QuizQuestion>>(emptyList()) }
    var currentIndex by remember { mutableStateOf(0) }
    var selectedAnswer by remember { mutableStateOf(-1) }
    var showFeedback by remember { mutableStateOf(false) }
    var userAnswers by remember { mutableStateOf<Map<Int, Int>>(emptyMap()) }
    var timeLeftSeconds by remember { mutableStateOf(0) }
    var shuffleQuestions by remember { mutableStateOf(true) }
    var showExplanations by remember { mutableStateOf(true) }
    var timerMode by remember { mutableStateOf("total") }
    var secondsPerQuestion by remember { mutableStateOf(30) }
    var timerRestartSignal by remember { mutableStateOf(0) }

    LaunchedEffect(screen, timerRestartSignal) {
        if (screen == "quiz" && isTimed && timeLeftSeconds > 0) {
            while (timeLeftSeconds > 0) {
                delay(1000L)
                timeLeftSeconds = maxOf(0, timeLeftSeconds - 1)
            }
            if (screen == "quiz") {
                if (timerMode == "per_question" && currentIndex < questions.size - 1) {
                    if (selectedAnswer != -1) userAnswers = userAnswers + (currentIndex to selectedAnswer)
                    currentIndex++
                    selectedAnswer = -1
                    showFeedback = false
                    timeLeftSeconds = secondsPerQuestion
                    timerRestartSignal++
                } else {
                    if (selectedAnswer != -1) userAnswers = userAnswers + (currentIndex to selectedAnswer)
                    screen = "results"
                }
            }
        }
    }

    val score = userAnswers.entries.count { (i, ans) -> questions.getOrNull(i)?.correctIndex == ans }

    fun startQuiz() {
        val pool = if (shuffleQuestions) (selectedEvent?.questions ?: return).shuffled() else (selectedEvent?.questions ?: return)
        questions = pool.take(questionCount.coerceAtMost(pool.size))
        currentIndex = 0
        selectedAnswer = -1
        showFeedback = false
        userAnswers = emptyMap()
        timeLeftSeconds = when {
            !isTimed -> 0
            timerMode == "per_question" -> secondsPerQuestion
            else -> questionCount * 60
        }
        timerRestartSignal++
        screen = "quiz"
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize().background(if (lightMode) Color.White else Color(0xFF0A1628)))
        if (!lightMode) Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = bgOpacity)))

        when (screen) {

            "home" -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 48.dp, start = 4.dp, end = 16.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = textColor)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("TESTING HUB", style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 3.sp, fontWeight = FontWeight.Black, color = primaryColor, fontSize = (12 * textSizeMultiplier).sp))
                            Text("Choose Your Mode", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black, color = textColor, fontSize = (22 * textSizeMultiplier).sp))
                        }
                    }
                    Column(modifier = Modifier.fillMaxSize().padding(start = 16.dp, end = 16.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Card(modifier = Modifier.fillMaxWidth().weight(1f), shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = Color.Transparent), onClick = { isFullTest = false; screen = "selectEvent" }) {
                            Box(modifier = Modifier.fillMaxSize().background(brush = androidx.compose.ui.graphics.Brush.linearGradient(listOf(primaryColor, primaryColor.copy(alpha = 0.7f))), shape = RoundedCornerShape(28.dp))) {
                                Box(modifier = Modifier.size(180.dp).align(Alignment.BottomEnd).offset(x = 40.dp, y = 40.dp).background(Color.White.copy(alpha = 0.06f), CircleShape))
                                Box(modifier = Modifier.size(110.dp).align(Alignment.TopEnd).offset(x = 20.dp, y = (-20).dp).background(Color.Black.copy(alpha = 0.05f), CircleShape))
                                Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                                    Text("PRACTICE MODE", style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 3.sp, fontWeight = FontWeight.Black, fontSize = (11 * textSizeMultiplier).sp), color = Color.Black.copy(alpha = 0.65f))
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("Study\nSmart.", style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Black, fontSize = (30 * textSizeMultiplier).sp, lineHeight = (34 * textSizeMultiplier).sp), color = Color.Black)
                                    Spacer(modifier = Modifier.weight(1f))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        listOf("10 Topics", "Instant Feedback", "Explained").forEach { label ->
                                            Surface(shape = RoundedCornerShape(20.dp), color = Color.Black.copy(alpha = 0.15f)) {
                                                Text(label, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = (11 * textSizeMultiplier).sp), color = Color.Black.copy(alpha = 0.8f))
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Column {
                                            Text("Pick a Topic", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, fontSize = (16 * textSizeMultiplier).sp), color = Color.Black)
                                            Text("Get feedback after every answer", style = MaterialTheme.typography.bodySmall.copy(fontSize = (12 * textSizeMultiplier).sp), color = Color.Black.copy(alpha = 0.65f))
                                        }
                                        Spacer(modifier = Modifier.weight(1f))
                                        Surface(shape = CircleShape, color = Color.Black.copy(alpha = 0.18f), modifier = Modifier.size(44.dp)) {
                                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) { Icon(Icons.Default.ArrowForward, null, tint = Color.Black, modifier = Modifier.size(22.dp)) }
                                        }
                                    }
                                }
                            }
                        }
                        Card(modifier = Modifier.fillMaxWidth().weight(1f), shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = cardBg), border = BorderStroke(1.5.dp, primaryColor.copy(alpha = 0.4f)), onClick = { isFullTest = true; screen = "selectEvent" }) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                Box(modifier = Modifier.size(160.dp).align(Alignment.BottomEnd).offset(x = 30.dp, y = 30.dp).background(primaryColor.copy(alpha = 0.05f), CircleShape))
                                Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                                    Text("FULL TEST", style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 3.sp, fontWeight = FontWeight.Black, fontSize = (11 * textSizeMultiplier).sp), color = primaryColor.copy(alpha = 0.8f))
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("Test\nYourself.", style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Black, fontSize = (30 * textSizeMultiplier).sp, lineHeight = (34 * textSizeMultiplier).sp), color = textColor)
                                    Spacer(modifier = Modifier.weight(1f))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        listOf("Answer All", "Then Review", "Timed Option").forEach { label ->
                                            Surface(shape = RoundedCornerShape(20.dp), color = primaryColor.copy(alpha = 0.12f), border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.25f))) {
                                                Text(label, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = (11 * textSizeMultiplier).sp), color = primaryColor)
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Column {
                                            Text("Pick a Topic", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, fontSize = (16 * textSizeMultiplier).sp), color = textColor)
                                            Text("Submit all answers at the end", style = MaterialTheme.typography.bodySmall.copy(fontSize = (12 * textSizeMultiplier).sp), color = textColorSecondary)
                                        }
                                        Spacer(modifier = Modifier.weight(1f))
                                        Surface(shape = CircleShape, color = primaryColor.copy(alpha = 0.15f), modifier = Modifier.size(44.dp)) {
                                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) { Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = primaryColor, modifier = Modifier.size(22.dp)) }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            "selectEvent" -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 48.dp, end = 16.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { screen = "home" }, modifier = Modifier.size(48.dp)) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = textColor) }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(if (isFullTest) "FULL TEST" else "PRACTICE MODE", style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 3.sp, fontWeight = FontWeight.Black, color = primaryColor, fontSize = (12 * textSizeMultiplier).sp))
                            Text("Pick a Topic", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black, color = textColor, fontSize = (22 * textSizeMultiplier).sp))
                        }
                    }
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 40.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(allEvents) { index, ev ->
                            val isFilled = index % 2 == 0
                            Card(
                                modifier = Modifier.fillMaxWidth().height(165.dp),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                                border = if (isFilled) null else BorderStroke(1.5.dp, primaryColor.copy(alpha = 0.4f)),
                                onClick = { selectedEvent = ev; screen = "config" }
                            ) {
                                Box(modifier = Modifier.fillMaxSize().then(if (isFilled) Modifier.background(brush = androidx.compose.ui.graphics.Brush.linearGradient(listOf(primaryColor, primaryColor.copy(alpha = 0.75f))), shape = RoundedCornerShape(20.dp)) else Modifier.background(cardBg, shape = RoundedCornerShape(20.dp)))) {
                                    Box(modifier = Modifier.size(90.dp).align(Alignment.TopEnd).offset(x = 20.dp, y = (-20).dp).background(if (isFilled) Color.White.copy(alpha = 0.08f) else primaryColor.copy(alpha = 0.06f), CircleShape))
                                    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
                                        Box(modifier = Modifier.size(42.dp).background(if (isFilled) Color.White.copy(alpha = 0.2f) else primaryColor.copy(alpha = 0.12f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                                            Icon(ev.icon, null, tint = if (isFilled) Color.Black else primaryColor, modifier = Modifier.size(24.dp))
                                        }
                                        Column {
                                            Text(ev.name, color = if (isFilled) Color.Black else textColor, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, fontSize = (14 * textSizeMultiplier).sp), maxLines = 2, overflow = TextOverflow.Ellipsis)
                                            Spacer(modifier = Modifier.height(3.dp))
                                            Text("${ev.questions.size} questions", color = if (isFilled) Color.Black.copy(alpha = 0.7f) else textColorSecondary, style = MaterialTheme.typography.labelSmall.copy(fontSize = (11 * textSizeMultiplier).sp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            "config" -> {
                val ev = selectedEvent ?: return@Box
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 48.dp, end = 16.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { screen = "selectEvent" }, modifier = Modifier.size(48.dp)) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = textColor) }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("CONFIGURE", style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 3.sp, fontWeight = FontWeight.Black, color = primaryColor, fontSize = (12 * textSizeMultiplier).sp))
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(ev.icon, null, tint = primaryColor, modifier = Modifier.size(20.dp))
                                Text(ev.name, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black, color = textColor, fontSize = (20 * textSizeMultiplier).sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("NUMBER OF QUESTIONS", color = primaryColor, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black, letterSpacing = 2.sp, fontSize = (11 * textSizeMultiplier).sp), modifier = Modifier.padding(bottom = 12.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                listOf(5, 10, ev.questions.size).distinct().forEach { count ->
                                    val isSelected = questionCount == count
                                    Surface(shape = RoundedCornerShape(12.dp), color = if (isSelected) primaryColor else cardBg, border = BorderStroke(1.dp, if (isSelected) primaryColor else primaryColor.copy(alpha = 0.3f)), modifier = Modifier.weight(1f).clickable { questionCount = count }) {
                                        Column(modifier = Modifier.padding(vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("$count", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black, fontSize = (20 * textSizeMultiplier).sp), color = if (isSelected) Color.Black else textColor)
                                            Text(if (count == ev.questions.size) "All" else "Questions", style = MaterialTheme.typography.labelSmall.copy(fontSize = (10 * textSizeMultiplier).sp), color = if (isSelected) Color.Black.copy(alpha = 0.7f) else textColorSecondary)
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                            Text("TIMER", color = primaryColor, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black, letterSpacing = 2.sp, fontSize = (11 * textSizeMultiplier).sp), modifier = Modifier.padding(bottom = 12.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                data class TimerOpt(val label: String, val sub: String, val timed: Boolean, val mode: String)
                                listOf(
                                    TimerOpt("⏸️  Untimed", "No limit", false, "total"),
                                    TimerOpt("⏱️  Total Time", "${questionCount} min total", true, "total"),
                                    TimerOpt("⚡  Per Question", "Configurable", true, "per_question")
                                ).forEach { opt ->
                                    val isSelected = isTimed == opt.timed && (!opt.timed || timerMode == opt.mode)
                                    Surface(shape = RoundedCornerShape(12.dp), color = if (isSelected) primaryColor else cardBg, border = BorderStroke(1.dp, if (isSelected) primaryColor else primaryColor.copy(alpha = 0.3f)), modifier = Modifier.weight(1f).clickable { isTimed = opt.timed; timerMode = opt.mode }) {
                                        Column(modifier = Modifier.padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(opt.label, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = (11 * textSizeMultiplier).sp), color = if (isSelected) Color.Black else textColor, textAlign = TextAlign.Center)
                                            Text(opt.sub, style = MaterialTheme.typography.labelSmall.copy(fontSize = (10 * textSizeMultiplier).sp), color = if (isSelected) Color.Black.copy(alpha = 0.7f) else textColorSecondary, textAlign = TextAlign.Center)
                                        }
                                    }
                                }
                            }
                            if (isTimed && timerMode == "per_question") {
                                Spacer(modifier = Modifier.height(14.dp))
                                Text("SECONDS PER QUESTION", color = primaryColor, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black, letterSpacing = 2.sp, fontSize = (11 * textSizeMultiplier).sp), modifier = Modifier.padding(bottom = 12.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    listOf(15 to "15s", 30 to "30s", 60 to "60s").forEach { (secs, label) ->
                                        val isSelected = secondsPerQuestion == secs
                                        Surface(shape = RoundedCornerShape(12.dp), color = if (isSelected) primaryColor else cardBg, border = BorderStroke(1.dp, if (isSelected) primaryColor else primaryColor.copy(alpha = 0.3f)), modifier = Modifier.weight(1f).clickable { secondsPerQuestion = secs }) {
                                            Column(modifier = Modifier.padding(vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(label, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, fontSize = (16 * textSizeMultiplier).sp), color = if (isSelected) Color.Black else textColor)
                                                Text("per question", style = MaterialTheme.typography.labelSmall.copy(fontSize = (10 * textSizeMultiplier).sp), color = if (isSelected) Color.Black.copy(alpha = 0.7f) else textColorSecondary)
                                            }
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                            Text("QUESTION ORDER", color = primaryColor, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black, letterSpacing = 2.sp, fontSize = (11 * textSizeMultiplier).sp), modifier = Modifier.padding(bottom = 12.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                listOf(true to Pair("🔀  Shuffled", "Random order"), false to Pair("📋  Sequential", "Original order")).forEach { (shuffle, labels) ->
                                    val isSelected = shuffleQuestions == shuffle
                                    Surface(shape = RoundedCornerShape(14.dp), color = if (isSelected) primaryColor else cardBg, border = BorderStroke(1.dp, if (isSelected) primaryColor else primaryColor.copy(alpha = 0.3f)), modifier = Modifier.weight(1f).height(76.dp).clickable { shuffleQuestions = shuffle }) {
                                        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                            Text(labels.first, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = (13 * textSizeMultiplier).sp), color = if (isSelected) Color.Black else textColor)
                                            Text(labels.second, style = MaterialTheme.typography.labelSmall.copy(fontSize = (10 * textSizeMultiplier).sp), color = if (isSelected) Color.Black.copy(alpha = 0.7f) else textColorSecondary)
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                            Text("EXPLANATIONS", color = primaryColor, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black, letterSpacing = 2.sp, fontSize = (11 * textSizeMultiplier).sp), modifier = Modifier.padding(bottom = 12.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                listOf(true to Pair("💡  After Each", "See tips as you go"), false to Pair("📝  Results Only", "Review after test")).forEach { (showEach, labels) ->
                                    val isSelected = showExplanations == showEach
                                    Surface(shape = RoundedCornerShape(14.dp), color = if (isSelected) primaryColor else cardBg, border = BorderStroke(1.dp, if (isSelected) primaryColor else primaryColor.copy(alpha = 0.3f)), modifier = Modifier.weight(1f).height(76.dp).clickable { showExplanations = showEach }) {
                                        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                            Text(labels.first, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = (13 * textSizeMultiplier).sp), color = if (isSelected) Color.Black else textColor)
                                            Text(labels.second, style = MaterialTheme.typography.labelSmall.copy(fontSize = (10 * textSizeMultiplier).sp), color = if (isSelected) Color.Black.copy(alpha = 0.7f) else textColorSecondary)
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(28.dp))
                            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = primaryColor.copy(alpha = 0.1f)), border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.3f))) {
                                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                                    Text("SUMMARY", color = primaryColor, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, letterSpacing = 2.sp, fontSize = (10 * textSizeMultiplier).sp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    listOf(
                                        "Topic" to ev.name,
                                        "Mode" to if (isFullTest) "Full Test" else "Practice",
                                        "Questions" to "$questionCount",
                                        "Order" to if (shuffleQuestions) "Shuffled" else "Sequential",
                                        "Timer" to when { !isTimed -> "None"; timerMode == "per_question" -> "${secondsPerQuestion}s / question"; else -> "${questionCount} min total" },
                                        "Explanations" to if (showExplanations) "After each" else "Results only"
                                    ).forEach { (k, v) ->
                                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(k, color = textColorSecondary, style = MaterialTheme.typography.bodySmall.copy(fontSize = (12 * textSizeMultiplier).sp))
                                            Text(v, color = textColor, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = (12 * textSizeMultiplier).sp))
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(onClick = { startQuiz() }, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = primaryColor)) {
                                Text("Start ${if (isFullTest) "Test" else "Practice"}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, fontSize = (16 * textSizeMultiplier).sp), color = Color.Black)
                            }
                            Spacer(modifier = Modifier.height(40.dp))
                        }
                    }
                }
            }

            "quiz" -> {
                if (questions.isEmpty()) { screen = "home"; return@Box }
                val q = questions[currentIndex]
                val progress = (currentIndex + 1).toFloat() / questions.size
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 48.dp, end = 16.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { screen = "home" }, modifier = Modifier.size(48.dp)) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = textColor) }
                        Column(modifier = Modifier.weight(1f)) {
                            Text((selectedEvent?.name ?: "").uppercase(), style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.sp, fontWeight = FontWeight.Black, color = primaryColor, fontSize = (10 * textSizeMultiplier).sp))
                            Text("Question ${currentIndex + 1} of ${questions.size}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, color = textColor, fontSize = (16 * textSizeMultiplier).sp))
                        }
                        if (isTimed) {
                            val mins = timeLeftSeconds / 60
                            val secs = timeLeftSeconds % 60
                            val warningThreshold = if (timerMode == "per_question") 10 else 60
                            val isWarning = timeLeftSeconds < warningThreshold
                            Surface(shape = RoundedCornerShape(10.dp), color = if (isWarning) Color.Red.copy(alpha = 0.15f) else primaryColor.copy(alpha = 0.15f), border = BorderStroke(1.dp, if (isWarning) Color.Red else primaryColor)) {
                                Text(text = "${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black, fontSize = (14 * textSizeMultiplier).sp), color = if (isWarning) Color.Red else primaryColor)
                            }
                        }
                    }
                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(4.dp).padding(horizontal = 16.dp), color = primaryColor, trackColor = primaryColor.copy(alpha = 0.2f))
                    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = cardBg), border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.25f))) {
                                Text(q.question, modifier = Modifier.padding(20.dp), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold, fontSize = (16 * textSizeMultiplier).sp), color = textColor)
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            q.options.forEachIndexed { idx, option ->
                                val isSelected = selectedAnswer == idx
                                val isCorrect = idx == q.correctIndex
                                val bgColor = when {
                                    showFeedback && isCorrect -> Color(0xFF22C55E).copy(alpha = 0.18f)
                                    showFeedback && isSelected && !isCorrect -> Color.Red.copy(alpha = 0.18f)
                                    isSelected -> primaryColor.copy(alpha = 0.18f)
                                    else -> cardBg
                                }
                                val borderColor = when {
                                    showFeedback && isCorrect -> Color(0xFF22C55E)
                                    showFeedback && isSelected && !isCorrect -> Color.Red
                                    isSelected -> primaryColor
                                    else -> primaryColor.copy(alpha = 0.15f)
                                }
                                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = bgColor), border = BorderStroke(1.5.dp, borderColor), onClick = { if (!showFeedback) selectedAnswer = idx }) {
                                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Surface(shape = CircleShape, color = if (isSelected || (showFeedback && isCorrect)) borderColor else primaryColor.copy(alpha = 0.1f), modifier = Modifier.size(28.dp)) {
                                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                                when {
                                                    showFeedback && isCorrect -> Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                                    showFeedback && isSelected && !isCorrect -> Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                                    else -> Text(listOf("A", "B", "C", "D")[idx], style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black, fontSize = (12 * textSizeMultiplier).sp), color = if (isSelected) Color.White else textColorSecondary)
                                                }
                                            }
                                        }
                                        Text(option, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium, fontSize = (14 * textSizeMultiplier).sp), color = textColor)
                                    }
                                }
                            }
                            if (showFeedback && showExplanations && q.explanation.isNotEmpty()) {
                                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF22C55E).copy(alpha = 0.08f)), border = BorderStroke(1.dp, Color(0xFF22C55E).copy(alpha = 0.4f))) {
                                    Row(modifier = Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                                        Icon(Icons.Default.Lightbulb, null, tint = Color(0xFF22C55E), modifier = Modifier.size(18.dp).padding(top = 1.dp))
                                        Text(q.explanation, style = MaterialTheme.typography.bodySmall.copy(fontSize = (13 * textSizeMultiplier).sp), color = textColor)
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    if (isFullTest) {
                                        if (selectedAnswer != -1) userAnswers = userAnswers + (currentIndex to selectedAnswer)
                                        if (currentIndex < questions.size - 1) { currentIndex++; selectedAnswer = -1; if (isTimed && timerMode == "per_question") { timeLeftSeconds = secondsPerQuestion; timerRestartSignal++ } } else { screen = "results" }
                                    } else {
                                        if (!showFeedback) {
                                            if (selectedAnswer != -1) { userAnswers = userAnswers + (currentIndex to selectedAnswer); showFeedback = true }
                                        } else {
                                            if (currentIndex < questions.size - 1) { currentIndex++; selectedAnswer = -1; showFeedback = false; if (isTimed && timerMode == "per_question") { timeLeftSeconds = secondsPerQuestion; timerRestartSignal++ } } else { screen = "results" }
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                                enabled = selectedAnswer != -1 || showFeedback
                            ) {
                                Text(
                                    text = when {
                                        isFullTest && currentIndex < questions.size - 1 -> "Next →"
                                        isFullTest -> "Submit Test"
                                        !showFeedback -> "Check Answer"
                                        currentIndex < questions.size - 1 -> "Next Question →"
                                        else -> "See Results"
                                    },
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, fontSize = (15 * textSizeMultiplier).sp), color = Color.Black
                                )
                            }
                            Spacer(modifier = Modifier.height(40.dp))
                        }
                    }
                }
            }

            "results" -> {
                val pct = if (questions.isNotEmpty()) (score * 100) / questions.size else 0
                val gradeColor = when { pct >= 90 -> Color(0xFF22C55E); pct >= 80 -> Color(0xFF84CC16); pct >= 70 -> Color(0xFFF59E0B); pct >= 60 -> Color(0xFFF97316); else -> Color.Red }
                val gradeLetter = when { pct >= 90 -> "A"; pct >= 80 -> "B"; pct >= 70 -> "C"; pct >= 60 -> "D"; else -> "F" }
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 48.dp, end = 16.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { screen = "home" }, modifier = Modifier.size(48.dp)) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = textColor) }
                        Text("Results", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black, color = textColor, fontSize = (22 * textSizeMultiplier).sp))
                    }
                    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
                        item {
                            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.Transparent), border = BorderStroke(1.dp, gradeColor.copy(alpha = 0.5f))) {
                                Box(modifier = Modifier.fillMaxWidth().background(brush = androidx.compose.ui.graphics.Brush.linearGradient(listOf(gradeColor.copy(alpha = 0.18f), gradeColor.copy(alpha = 0.04f))), shape = RoundedCornerShape(24.dp))) {
                                    Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(gradeLetter, style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Black, fontSize = (72 * textSizeMultiplier).sp), color = gradeColor)
                                        Text("$pct%", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black, fontSize = (28 * textSizeMultiplier).sp), color = textColor)
                                        Text("$score / ${questions.size} correct", style = MaterialTheme.typography.bodyLarge.copy(fontSize = (15 * textSizeMultiplier).sp), color = textColorSecondary)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(when { pct >= 90 -> "Outstanding! You're ready to compete!"; pct >= 80 -> "Great job! A little more practice and you'll crush it!"; pct >= 70 -> "Good effort! Review what you missed and try again!"; pct >= 60 -> "Keep at it — you're getting there!"; else -> "Don't give up! Review the material and try again!" }, style = MaterialTheme.typography.bodySmall.copy(fontSize = (12 * textSizeMultiplier).sp), color = textColorSecondary, textAlign = TextAlign.Center)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Button(onClick = { startQuiz() }, modifier = Modifier.weight(1f).height(50.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = primaryColor)) {
                                    Text("Try Again", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black, fontSize = (14 * textSizeMultiplier).sp), color = Color.Black)
                                }
                                OutlinedButton(onClick = { screen = "home" }, modifier = Modifier.weight(1f).height(50.dp), shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, primaryColor)) {
                                    Text("New Test", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black, fontSize = (14 * textSizeMultiplier).sp), color = primaryColor)
                                }
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                            Text("ANSWER REVIEW", color = primaryColor, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black, letterSpacing = 2.sp, fontSize = (11 * textSizeMultiplier).sp), modifier = Modifier.padding(bottom = 12.dp))
                        }
                        questions.forEachIndexed { i, q ->
                            item {
                                val userAns = userAnswers[i]
                                val isCorrect = userAns == q.correctIndex
                                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = if (isCorrect) Color(0xFF22C55E).copy(alpha = 0.07f) else Color.Red.copy(alpha = 0.07f)), border = BorderStroke(1.dp, if (isCorrect) Color(0xFF22C55E).copy(alpha = 0.4f) else Color.Red.copy(alpha = 0.4f))) {
                                    Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                                        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            Icon(if (isCorrect) Icons.Default.CheckCircle else Icons.Default.Cancel, null, tint = if (isCorrect) Color(0xFF22C55E) else Color.Red, modifier = Modifier.size(18.dp).padding(top = 1.dp))
                                            Text("Q${i + 1}: ${q.question}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, fontSize = (13 * textSizeMultiplier).sp), color = textColor)
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text("Correct: ${q.options[q.correctIndex]}", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = (12 * textSizeMultiplier).sp), color = Color(0xFF22C55E))
                                        if (!isCorrect && userAns != null) Text("Your answer: ${q.options[userAns]}", style = MaterialTheme.typography.labelMedium.copy(fontSize = (12 * textSizeMultiplier).sp), color = Color.Red.copy(alpha = 0.8f))
                                        else if (userAns == null) Text("Not answered (time expired)", style = MaterialTheme.typography.labelSmall.copy(fontSize = (11 * textSizeMultiplier).sp), color = textColorSecondary)
                                        if (q.explanation.isNotEmpty()) { Spacer(modifier = Modifier.height(4.dp)); Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Top) { Icon(Icons.Default.Lightbulb, null, tint = textColorSecondary, modifier = Modifier.size(14.dp).padding(top = 1.dp)); Text(q.explanation, style = MaterialTheme.typography.bodySmall.copy(fontSize = (11 * textSizeMultiplier).sp), color = textColorSecondary) } }
                                    }
                                }
                            }
                        }
                        item { Spacer(modifier = Modifier.height(40.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
fun HubSelectorScreen(
    onSelectPrep: () -> Unit,
    onSelectTesting: () -> Unit,
    onBack: () -> Unit,
    primaryColor: Color,
    bgOpacity: Float,
    textSizeMultiplier: Float,
    lightMode: Boolean = false
) {
    val textColor = if (lightMode) Color.Black else Color.White
    val textColorSecondary = if (lightMode) Color.Black.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.9f)
    val cardBg = if (lightMode) Color(0xFFE8EDF5) else Color.White.copy(alpha = 0.1f)

    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize().background(if (lightMode) Color.White else Color(0xFF0A1628)))
        if (!lightMode) Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = bgOpacity)))

        Column(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxWidth().padding(top = 48.dp, start = 4.dp, end = 16.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = textColor)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("FBLA STUDY HUB", style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 3.sp, fontWeight = FontWeight.Black, color = primaryColor, fontSize = (12 * textSizeMultiplier).sp))
                    Text("Choose Your Hub", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black, color = textColor, fontSize = (22 * textSizeMultiplier).sp))
                }
            }
            Column(modifier = Modifier.fillMaxSize().padding(start = 16.dp, end = 16.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Event Prep Hub square
                Card(modifier = Modifier.fillMaxWidth().weight(1f), shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = Color.Transparent), onClick = onSelectPrep) {
                    Box(modifier = Modifier.fillMaxSize().background(brush = androidx.compose.ui.graphics.Brush.linearGradient(listOf(primaryColor, primaryColor.copy(alpha = 0.7f))), shape = RoundedCornerShape(28.dp))) {
                        Box(modifier = Modifier.size(180.dp).align(Alignment.BottomEnd).offset(x = 40.dp, y = 40.dp).background(Color.White.copy(alpha = 0.06f), CircleShape))
                        Box(modifier = Modifier.size(110.dp).align(Alignment.TopEnd).offset(x = 20.dp, y = (-20).dp).background(Color.Black.copy(alpha = 0.05f), CircleShape))
                        Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                            Text("EVENT PREP HUB", style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 3.sp, fontWeight = FontWeight.Black, fontSize = (11 * textSizeMultiplier).sp), color = Color.Black.copy(alpha = 0.65f))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Study.\nPractice.\nWin.", style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Black, fontSize = (30 * textSizeMultiplier).sp, lineHeight = (34 * textSizeMultiplier).sp), color = Color.Black)
                            Spacer(modifier = Modifier.weight(1f))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("54 Events", "Overview", "Win Tips").forEach { label ->
                                    Surface(shape = RoundedCornerShape(20.dp), color = Color.Black.copy(alpha = 0.15f)) {
                                        Text(label, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = (11 * textSizeMultiplier).sp), color = Color.Black.copy(alpha = 0.8f))
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text("Start Preparing", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, fontSize = (16 * textSizeMultiplier).sp), color = Color.Black)
                                    Text("Browse all FBLA events", style = MaterialTheme.typography.bodySmall.copy(fontSize = (12 * textSizeMultiplier).sp), color = Color.Black.copy(alpha = 0.65f))
                                }
                                Spacer(modifier = Modifier.weight(1f))
                                Surface(shape = CircleShape, color = Color.Black.copy(alpha = 0.18f), modifier = Modifier.size(44.dp)) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                        Icon(Icons.Default.ArrowForward, null, tint = Color.Black, modifier = Modifier.size(22.dp))
                                    }
                                }
                            }
                        }
                    }
                }
                // Testing Hub square
                Card(modifier = Modifier.fillMaxWidth().weight(1f), shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = cardBg), border = BorderStroke(1.5.dp, primaryColor.copy(alpha = 0.4f)), onClick = onSelectTesting) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Box(modifier = Modifier.size(160.dp).align(Alignment.BottomEnd).offset(x = 30.dp, y = 30.dp).background(primaryColor.copy(alpha = 0.05f), CircleShape))
                        Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                            Text("TESTING HUB", style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 3.sp, fontWeight = FontWeight.Black, fontSize = (11 * textSizeMultiplier).sp), color = primaryColor.copy(alpha = 0.8f))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Practice.\nTest.\nScore.", style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Black, fontSize = (30 * textSizeMultiplier).sp, lineHeight = (34 * textSizeMultiplier).sp), color = textColor)
                            Spacer(modifier = Modifier.weight(1f))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("10 Subjects", "100+ Qs", "Timed Mode").forEach { label ->
                                    Surface(shape = RoundedCornerShape(20.dp), color = primaryColor.copy(alpha = 0.12f), border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.25f))) {
                                        Text(label, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = (11 * textSizeMultiplier).sp), color = primaryColor)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text("Start Testing", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, fontSize = (16 * textSizeMultiplier).sp), color = textColor)
                                    Text("Practice & full test modes", style = MaterialTheme.typography.bodySmall.copy(fontSize = (12 * textSizeMultiplier).sp), color = textColorSecondary)
                                }
                                Spacer(modifier = Modifier.weight(1f))
                                Surface(shape = CircleShape, color = primaryColor.copy(alpha = 0.15f), modifier = Modifier.size(44.dp)) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = primaryColor, modifier = Modifier.size(22.dp))
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

@Composable
fun ResourcesScreen(primaryColor: Color, bgOpacity: Float, isSimplifiedUI: Boolean, textSizeMultiplier: Float, lightMode: Boolean = false, userState: String = "", userSchool: String = "") {
// Dynamic text colors
    val textColor = if (lightMode) Color.Black else Color.White
    val textColorSecondary = if (lightMode) Color.Black.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.9f)
    val textColorTertiary = if (lightMode) Color.Black.copy(alpha = 0.75f) else Color.White.copy(alpha = 0.85f)
    val cardBg = if (lightMode) Color(0xFFE8EDF5) else Color.White.copy(alpha = 0.1f)

    val context = LocalContext.current
    var activeHub by remember { mutableStateOf("") }

    when (activeHub) {
        "prep" -> { EventPrepHub(onBack = { activeHub = "" }, primaryColor = primaryColor, bgOpacity = bgOpacity, isSimplifiedUI = isSimplifiedUI, textSizeMultiplier = textSizeMultiplier, lightMode = lightMode); return }
        "testing" -> { TestingHub(onBack = { activeHub = "" }, primaryColor = primaryColor, bgOpacity = bgOpacity, isSimplifiedUI = isSimplifiedUI, textSizeMultiplier = textSizeMultiplier, lightMode = lightMode); return }
        "selector" -> { HubSelectorScreen(onSelectPrep = { activeHub = "prep" }, onSelectTesting = { activeHub = "testing" }, onBack = { activeHub = "" }, primaryColor = primaryColor, bgOpacity = bgOpacity, textSizeMultiplier = textSizeMultiplier, lightMode = lightMode); return }
        "dress_code" -> { DressCodeCheckerScreen(onBack = { activeHub = "" }, primaryColor = primaryColor, bgOpacity = bgOpacity, textSizeMultiplier = textSizeMultiplier, lightMode = lightMode); return }
    }

    val resourceList = remember {
        listOf(
            ResourceItem("Guidelines", "Requirements for all High School events.", "https://www.fbla.org/high-school/competitive-events/", Icons.AutoMirrored.Filled.Assignment),
            ResourceItem("HS 101 Toolkit", "Essential toolkit for high school management.", "https://www.fbla.org/media/2023/05/FBLA-High-School-101-Toolkit.pdf", Icons.AutoMirrored.Filled.LibraryBooks),
            ResourceItem("Brand Center", "Official logos and professional standards.", "https://www.fbla.org/brand-center/", Icons.Default.Brush),
            ResourceItem("Dress Code", "Attire standards for conferences.", "https://www.fbla.org/dress-code/", Icons.Default.Checkroom)
        )
    }

    data class SocialMedia(val name: String, val handle: String, val url: String)
    val socialMediaList = remember {
        listOf(
            SocialMedia("Instagram", "@fbla_national", "https://www.instagram.com/fbla_national/"),
            SocialMedia("X (Twitter)", "@FBLA_National", "https://twitter.com/FBLA_National"),
            SocialMedia("Facebook", "FutureBusinessLeaders", "https://www.facebook.com/FutureBusinessLeaders/"),
            SocialMedia("LinkedIn", "FBLA", "https://www.linkedin.com/company/future-business-leaders-america")
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
// Background — white in light mode, dark blue in dark mode
        Box(modifier = Modifier.fillMaxSize().background(
            if (lightMode) Color.White else Color(0xFF0A1628)
        ))
        if (!lightMode) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = bgOpacity)))
        }

        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(24.dp)) {
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text(text = "TOOLKIT", style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 4.sp, fontWeight = FontWeight.Bold, color = primaryColor, fontSize = (MaterialTheme.typography.labelLarge.fontSize.value * textSizeMultiplier).sp))
                Text(text = "FBLA Hub Resources", style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Black, color = textColor, fontSize = (MaterialTheme.typography.displaySmall.fontSize.value * textSizeMultiplier).sp))
                Spacer(modifier = Modifier.height(24.dp))

                // ── FBLA STUDY HUB BANNER ──────────────────────────────────
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    onClick = { activeHub = "selector" }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Box(modifier = Modifier.size(44.dp).background(primaryColor.copy(alpha = 0.12f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.School, null, tint = primaryColor, modifier = Modifier.size(26.dp))
                                }
                                Column {
                                    Text(
                                        "FBLA STUDY HUB",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black, letterSpacing = 2.sp, fontSize = (11 * textSizeMultiplier).sp),
                                        color = primaryColor
                                    )
                                    Text(
                                        "Prep, Practice & Test.",
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black, fontSize = (20 * textSizeMultiplier).sp),
                                        color = textColor
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("Event Prep", "Testing Hub").forEach { label ->
                                    Surface(shape = RoundedCornerShape(20.dp), color = primaryColor.copy(alpha = 0.1f)) {
                                        Text(label, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = (11 * textSizeMultiplier).sp), color = primaryColor)
                                    }
                                }
                            }
                        }
                        Surface(
                            shape = CircleShape,
                            color = primaryColor.copy(alpha = 0.12f),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = primaryColor, modifier = Modifier.size(22.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ── OUTFIT CHECKER BANNER ──────────────────────────────────
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    onClick = { activeHub = "dress_code" }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Box(modifier = Modifier.size(44.dp).background(primaryColor.copy(alpha = 0.12f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Checkroom, null, tint = primaryColor, modifier = Modifier.size(26.dp))
                                }
                                Column {
                                    Text(
                                        "OUTFIT CHECKER",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black, letterSpacing = 2.sp, fontSize = (11 * textSizeMultiplier).sp),
                                        color = primaryColor
                                    )
                                    Text(
                                        "AI Dress Code Analysis.",
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black, fontSize = (20 * textSizeMultiplier).sp),
                                        color = textColor
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("Business Professional", "Instant AI Check").forEach { label ->
                                    Surface(shape = RoundedCornerShape(20.dp), color = primaryColor.copy(alpha = 0.1f)) {
                                        Text(label, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = (11 * textSizeMultiplier).sp), color = primaryColor)
                                    }
                                }
                            }
                        }
                        Surface(shape = CircleShape, color = primaryColor.copy(alpha = 0.12f), modifier = Modifier.size(44.dp)) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = primaryColor, modifier = Modifier.size(22.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }

// Resources Grid Header
            item {
                Text(
                    text = "RESOURCES",
                    style = MaterialTheme.typography.labelMedium.copy(
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.Bold,
                        fontSize = (MaterialTheme.typography.labelMedium.fontSize.value * textSizeMultiplier).sp
                    ),
                    color = primaryColor
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

// Resources Grid
            resourceList.chunked(2).forEach { rowItems ->
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        rowItems.forEach { resource ->
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(180.dp),
                                colors = CardDefaults.cardColors(containerColor = if (lightMode) Color(0xFFE8EDF5) else Color.White.copy(alpha = 0.15f)),
                                shape = RoundedCornerShape(16.dp),
                                onClick = {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(resource.url)))
                                }
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize().padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(resource.icon, null, modifier = Modifier.size(40.dp), tint = primaryColor)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = resource.title,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = (MaterialTheme.typography.titleMedium.fontSize.value * textSizeMultiplier).sp
                                        ),
                                        color = textColor,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = "VIEW",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Black,
                                            fontSize = (MaterialTheme.typography.labelSmall.fontSize.value * textSizeMultiplier).sp
                                        ),
                                        color = primaryColor
                                    )
                                }
                            }
                        }
// Add spacer if odd number of items
                        if (rowItems.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }

// Social Media Section — redesigned
            item {
                // Header text
                Text(
                    "STAY CONNECTED",
                    style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 3.sp, fontWeight = FontWeight.Black, fontSize = (11 * textSizeMultiplier).sp),
                    color = primaryColor
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Want to stay connected?",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black, fontSize = (22 * textSizeMultiplier).sp),
                    color = textColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Follow FBLA national for competition updates, NLC news, and community highlights.",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = (14 * textSizeMultiplier).sp),
                    color = textColorSecondary
                )
                Spacer(modifier = Modifier.height(20.dp))

                // Competition graphic card
                Card(
                    modifier = Modifier.fillMaxWidth().height(185.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1B3E))
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Background gradient
                        Box(modifier = Modifier.fillMaxSize().background(
                            brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                colors = listOf(Color(0xFF0A1628), primaryColor.copy(alpha = 0.3f), Color(0xFF0D1B3E))
                            )
                        ))
                        // Decorative circles
                        Box(modifier = Modifier.size(150.dp).align(Alignment.TopStart).background(primaryColor.copy(alpha = 0.06f), CircleShape))
                        Box(modifier = Modifier.size(110.dp).align(Alignment.BottomEnd).background(primaryColor.copy(alpha = 0.07f), CircleShape))

                        // Content
                        Column(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 20.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Top badge
                            Surface(shape = RoundedCornerShape(8.dp), color = primaryColor.copy(alpha = 0.18f)) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.EmojiEvents, null, tint = primaryColor, modifier = Modifier.size(13.dp))
                                    Text("FBLA NATIONAL", color = primaryColor, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = (9 * textSizeMultiplier).sp, letterSpacing = 2.sp))
                                }
                            }

                            // Center icon cluster
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterHorizontally),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Groups, null, tint = Color.White.copy(alpha = 0.22f), modifier = Modifier.size(38.dp))
                                Icon(Icons.Default.EmojiEvents, null, tint = primaryColor, modifier = Modifier.size(54.dp))
                                Icon(Icons.Default.Groups, null, tint = Color.White.copy(alpha = 0.22f), modifier = Modifier.size(38.dp))
                            }

                            // Bottom text
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("National Leadership Conference", color = Color.White, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, fontSize = (15 * textSizeMultiplier).sp))
                                Text("Future Business Leaders of America", color = Color.White.copy(alpha = 0.5f), style = MaterialTheme.typography.bodySmall.copy(fontSize = (11 * textSizeMultiplier).sp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                // "Follow Us" label
                Text(
                    "FOLLOW US",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.sp, fontWeight = FontWeight.Black, fontSize = (10 * textSizeMultiplier).sp),
                    color = textColorTertiary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // 2-column social media grid
                socialMediaList.chunked(2).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        row.forEach { social ->
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = cardBg),
                                shape = RoundedCornerShape(14.dp),
                                onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(social.url))) }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 13.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    SocialMediaLogo(platform = social.name, modifier = Modifier.size(24.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(social.name, color = textColor, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = (12 * textSizeMultiplier).sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(social.handle, color = primaryColor, style = MaterialTheme.typography.labelSmall.copy(fontSize = (10 * textSizeMultiplier).sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    Icon(Icons.Default.OpenInNew, null, tint = primaryColor.copy(alpha = 0.55f), modifier = Modifier.size(13.dp))
                                }
                            }
                        }
                        if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }

// State FBLA Social Media Section
            if (userState.isNotEmpty()) {
                item {
                    Text(
                        text = "$userState FBLA",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = (MaterialTheme.typography.titleSmall.fontSize.value * textSizeMultiplier).sp,
                            letterSpacing = 1.2.sp
                        ),
                        color = primaryColor,
                        modifier = Modifier.padding(bottom = 12.dp, top = 8.dp)
                    )
                }

                val stateSocialMedia = getStateFBLASocialMedia(userState)
                if (stateSocialMedia.isNotEmpty()) {
                    items(stateSocialMedia.keys.toList().size) { index ->
                        val platform = stateSocialMedia.keys.toList()[index]
                        val url = stateSocialMedia[platform] ?: ""
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            colors = CardDefaults.cardColors(containerColor = primaryColor.copy(alpha = 0.15f)),
                            shape = RoundedCornerShape(12.dp),
                            onClick = {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                )
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 18.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    SocialMediaLogo(
                                        platform = platform,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Text(
                                        text = platform,
                                        color = textColor,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = (16 * textSizeMultiplier).sp
                                        )
                                    )
                                }
                                Icon(
                                    Icons.Default.OpenInNew,
                                    contentDescription = "Open $platform",
                                    tint = primaryColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

// Sun Prairie Chapter Section (Wisconsin only)
            if (userState.equals("Wisconsin", ignoreCase = true) && userSchool.contains("Sun Prairie", ignoreCase = true)) {
                item {
                    Text(
                        text = "Sun Prairie FBLA Chapter",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = (MaterialTheme.typography.titleSmall.fontSize.value * textSizeMultiplier).sp,
                            letterSpacing = 1.2.sp
                        ),
                        color = primaryColor,
                        modifier = Modifier.padding(bottom = 4.dp, top = 8.dp)
                    )
                    Text(
                        text = "SP West High School",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = (12 * textSizeMultiplier).sp
                        ),
                        color = textColorSecondary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                val chapterSocialMedia = getSunPrairieChapterSocialMedia()
                items(chapterSocialMedia.keys.toList().size) { index ->
                    val platform = chapterSocialMedia.keys.toList()[index]
                    val url = chapterSocialMedia[platform] ?: ""
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = primaryColor.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(12.dp),
                        onClick = {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            )
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                SocialMediaLogo(
                                    platform = platform,
                                    modifier = Modifier.size(32.dp)
                                )
                                Text(
                                    text = platform,
                                    color = textColor,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = (16 * textSizeMultiplier).sp
                                    )
                                )
                            }
                            Icon(
                                Icons.Default.OpenInNew,
                                contentDescription = "Open $platform",
                                tint = primaryColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun ConnectionRequestsScreen(onBack: () -> Unit, primaryColor: Color, bgOpacity: Float, isSimplifiedUI: Boolean, textSizeMultiplier: Float, lightMode: Boolean = false) {
// Dynamic text colors
    val textColor = if (lightMode) Color.Black else Color.White
    val textColorSecondary = if (lightMode) Color.Black.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.9f)
    val textColorTertiary = if (lightMode) Color.Black.copy(alpha = 0.75f) else Color.White.copy(alpha = 0.85f)

    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var requests by remember { mutableStateOf<List<ConnectionRequest>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val currentUid = auth.currentUser?.uid
        if (currentUid != null) {
            db.collection("connectionRequests")
                .whereEqualTo("toUid", currentUid)
                .whereEqualTo("status", "pending")
                .addSnapshotListener { snapshot, _ ->
                    requests = snapshot?.toObjects(ConnectionRequest::class.java) ?: emptyList()
                    isLoading = false
                }
        } else {
            isLoading = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
// Background — white in light mode, dark blue in dark mode
        Box(modifier = Modifier.fillMaxSize().background(
            if (lightMode) Color.White else Color(0xFF0A1628)
        ))
        if (!lightMode) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = bgOpacity)))
        }

        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp, end = 16.dp, bottom = 16.dp, start = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = textColor)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text("CONNECTION REQUESTS", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black, fontSize = (MaterialTheme.typography.headlineSmall.fontSize.value * textSizeMultiplier).sp), color = textColor)
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = primaryColor)
                }
            } else if (requests.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.PersonAdd, null, modifier = Modifier.size(80.dp), tint = textColor.copy(alpha = 0.6f))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No pending requests", color = textColorSecondary, style = MaterialTheme.typography.bodyLarge.copy(fontSize = (MaterialTheme.typography.bodyLarge.fontSize.value * textSizeMultiplier).sp))
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                    itemsIndexed(requests) { _, request ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            colors = CardDefaults.cardColors(containerColor = if (lightMode) Color(0xFFF0F4FF) else Color.White.copy(alpha = 0.1f)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(50.dp).background(primaryColor, CircleShape).clip(CircleShape)) {
                                        if (request.fromPfpUrl.isNotEmpty()) {
                                            AsyncImage(model = request.fromPfpUrl, contentDescription = null, contentScale = ContentScale.Crop)
                                        } else {
                                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                Text(request.fromName.take(1).uppercase(), color = Color.Black, fontWeight = FontWeight.Bold, style = TextStyle(fontSize = (16 * textSizeMultiplier).sp))
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(request.fromName, color = textColor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium.copy(fontSize = (MaterialTheme.typography.titleMedium.fontSize.value * textSizeMultiplier).sp))
                                        Text(request.fromSchool, color = primaryColor, style = MaterialTheme.typography.bodySmall.copy(fontSize = (MaterialTheme.typography.bodySmall.fontSize.value * textSizeMultiplier).sp))
                                        Text(request.fromEmail, color = textColorTertiary, style = MaterialTheme.typography.bodySmall.copy(fontSize = (MaterialTheme.typography.bodySmall.fontSize.value * textSizeMultiplier).sp))
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                try {
                                                    val currentUid = auth.currentUser?.uid ?: return@launch

// Add to both users' connections FIRST (before changing status)
                                                    db.collection("connections").document(currentUid)
                                                        .set(mapOf("connectedUids" to FieldValue.arrayUnion(request.fromUid)), com.google.firebase.firestore.SetOptions.merge()).await()

                                                    db.collection("connections").document(request.fromUid)
                                                        .set(mapOf("connectedUids" to FieldValue.arrayUnion(currentUid)), com.google.firebase.firestore.SetOptions.merge()).await()

// Update request status LAST (this removes it from the list)
                                                    db.collection("connectionRequests").document(request.id)
                                                        .update("status", "accepted").await()

                                                    Toast.makeText(context, "Request accepted!", Toast.LENGTH_SHORT).show()
                                                } catch (e: Exception) {
                                                    Log.e("ConnectionRequest", "Error accepting request: ${e.message}", e)
                                                    Toast.makeText(context, "Failed to accept: ${e.message}", Toast.LENGTH_LONG).show()
                                                }
                                            }
                                        },
                                        modifier = Modifier.weight(1f).height(40.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Green),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.Check, null, tint = textColor, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("ACCEPT", color = Color.White, fontWeight = FontWeight.Bold, style = TextStyle(fontSize = (12 * textSizeMultiplier).sp))
                                    }

                                    Button(
                                        onClick = {
                                            scope.launch {
                                                try {
                                                    db.collection("connectionRequests").document(request.id)
                                                        .update("status", "declined").await()
                                                    Toast.makeText(context, "Request declined", Toast.LENGTH_SHORT).show()
                                                } catch (e: Exception) {
                                                    Log.e("ConnectionRequest", "Error declining request: ${e.message}", e)
                                                    Toast.makeText(context, "Failed to decline: ${e.message}", Toast.LENGTH_LONG).show()
                                                }
                                            }
                                        },
                                        modifier = Modifier.weight(1f).height(40.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC143C)),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.Close, null, tint = textColor, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("DECLINE", color = Color.White, fontWeight = FontWeight.Bold, style = TextStyle(fontSize = (12 * textSizeMultiplier).sp))
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

@Composable
fun MyConnectionsScreen(onBack: () -> Unit, primaryColor: Color, bgOpacity: Float, isSimplifiedUI: Boolean, textSizeMultiplier: Float, lightMode: Boolean = false) {
// Dynamic text colors
    val textColor = if (lightMode) Color.Black else Color.White
    val textColorSecondary = if (lightMode) Color.Black.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.9f)
    val textColorTertiary = if (lightMode) Color.Black.copy(alpha = 0.75f) else Color.White.copy(alpha = 0.85f)

    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var connections by remember { mutableStateOf<List<Connection>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showRemoveDialog by remember { mutableStateOf(false) }
    var connectionToRemove by remember { mutableStateOf<Connection?>(null) }

    LaunchedEffect(Unit) {
        val currentUid = auth.currentUser?.uid
        if (currentUid != null) {
            db.collection("connections").document(currentUid).addSnapshotListener { snapshot, _ ->
                val connectedUids = (snapshot?.get("connectedUids") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()

                if (connectedUids.isEmpty()) {
                    connections = emptyList()
                    isLoading = false
                } else {
// Fetch user profiles for connected users
                    db.collection("users").whereIn("uid", connectedUids).addSnapshotListener { usersSnapshot, _ ->
                        connections = usersSnapshot?.toObjects(UserProfile::class.java)?.map { profile ->
                            Connection(
                                uid = profile.uid,
                                name = profile.name,
                                email = profile.email,
                                school = profile.school,
                                year = profile.year,
                                state = profile.state,
                                pfpUrl = profile.pfpUrl,
                                reminders = profile.reminders,
                                instagram = profile.instagram,
                                twitter = profile.twitter,
                                facebook = profile.facebook,
                                linkedin = profile.linkedin
                            )
                        } ?: emptyList()
                        isLoading = false
                    }
                }
            }
        } else {
            isLoading = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
// Background — white in light mode, dark blue in dark mode
        Box(modifier = Modifier.fillMaxSize().background(
            if (lightMode) Color.White else Color(0xFF0A1628)
        ))
        if (!lightMode) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = bgOpacity)))
        }

        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp, end = 16.dp, bottom = 16.dp, start = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = textColor)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text("MY CONNECTIONS", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black, fontSize = (MaterialTheme.typography.headlineSmall.fontSize.value * textSizeMultiplier).sp), color = textColor)
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = primaryColor)
                }
            } else if (connections.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.People, null, modifier = Modifier.size(80.dp), tint = textColor.copy(alpha = 0.6f))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No connections yet", color = textColorSecondary, style = MaterialTheme.typography.bodyLarge.copy(fontSize = (MaterialTheme.typography.bodyLarge.fontSize.value * textSizeMultiplier).sp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Send connection requests to other peers!", color = textColor.copy(alpha = 0.75f), style = MaterialTheme.typography.bodySmall.copy(fontSize = (MaterialTheme.typography.bodySmall.fontSize.value * textSizeMultiplier).sp), textAlign = TextAlign.Center)
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                    itemsIndexed(connections) { _, connection ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            colors = CardDefaults.cardColors(containerColor = if (lightMode) Color(0xFFF0F4FF) else Color.White.copy(alpha = 0.1f)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(50.dp).background(primaryColor, CircleShape).clip(CircleShape)) {
                                            if (connection.pfpUrl.isNotEmpty()) {
                                                AsyncImage(model = connection.pfpUrl, contentDescription = null, contentScale = ContentScale.Crop)
                                            } else {
                                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                    Text(connection.name.take(1).uppercase(), color = Color.Black, fontWeight = FontWeight.Bold, style = TextStyle(fontSize = (16 * textSizeMultiplier).sp))
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column {
                                            Text(connection.name, color = textColor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge.copy(fontSize = (MaterialTheme.typography.titleLarge.fontSize.value * textSizeMultiplier).sp))
                                            Text(connection.school, color = primaryColor, style = MaterialTheme.typography.bodyMedium.copy(fontSize = (MaterialTheme.typography.bodyMedium.fontSize.value * textSizeMultiplier).sp))
                                            if (connection.year.isNotEmpty() || connection.state.isNotEmpty()) {
                                                Text(
                                                    text = "Class of ${connection.year.ifEmpty { "?" }} • ${connection.state.ifEmpty { "?" }}",
                                                    color = textColorSecondary,
                                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = (MaterialTheme.typography.bodySmall.fontSize.value * textSizeMultiplier).sp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

// Email with clickable intent
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable {
                                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                                            data = Uri.parse("mailto:${connection.email}")
                                        }
                                        try {
                                            auth.currentUser?.let { intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                                        } catch (e: Exception) { }
                                    }) {
                                        Icon(Icons.Default.Email, null, tint = primaryColor, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(connection.email, color = textColorSecondary, style = MaterialTheme.typography.bodySmall.copy(fontSize = (MaterialTheme.typography.bodySmall.fontSize.value * textSizeMultiplier).sp))
                                    }

// Social Media Icons
                                    val hasSocial = connection.instagram.isNotEmpty() || connection.twitter.isNotEmpty() || connection.facebook.isNotEmpty() || connection.linkedin.isNotEmpty()
                                    if (hasSocial) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.Start,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Social Media:",
                                                color = textColorSecondary,
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = (MaterialTheme.typography.labelSmall.fontSize.value * textSizeMultiplier).sp),
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))

                                            if (connection.instagram.isNotEmpty()) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .clickable {
                                                            val url = normalizeSocialMediaUrl(connection.instagram)
                                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                                        }
                                                ) {
                                                    SocialMediaLogo(platform = "Instagram", modifier = Modifier.size(32.dp))
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                            }
                                            if (connection.twitter.isNotEmpty()) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .clickable {
                                                            val url = normalizeSocialMediaUrl(connection.twitter)
                                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                                        }
                                                ) {
                                                    SocialMediaLogo(platform = "X (Twitter)", modifier = Modifier.size(32.dp))
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                            }
                                            if (connection.facebook.isNotEmpty()) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .clickable {
                                                            val url = normalizeSocialMediaUrl(connection.facebook)
                                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                                        }
                                                ) {
                                                    SocialMediaLogo(platform = "Facebook", modifier = Modifier.size(32.dp))
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                            }
                                            if (connection.linkedin.isNotEmpty()) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .clickable {
                                                            val url = normalizeSocialMediaUrl(connection.linkedin)
                                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                                        }
                                                ) {
                                                    SocialMediaLogo(platform = "LinkedIn", modifier = Modifier.size(32.dp))
                                                }
                                            }
                                        }
                                    }

                                    if (connection.reminders.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text("Attending Competitions:", color = textColorSecondary, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = (MaterialTheme.typography.labelSmall.fontSize.value * textSizeMultiplier).sp))
                                        connection.reminders.forEach { reminder ->
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                                                Icon(Icons.Default.EmojiEvents, null, tint = primaryColor, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("${reminder.name} - ${reminder.date}", color = textColorSecondary, style = MaterialTheme.typography.bodySmall.copy(fontSize = (MaterialTheme.typography.bodySmall.fontSize.value * textSizeMultiplier).sp))
                                            }
                                        }
                                    }
                                }

// Small red X button in bottom right
                                IconButton(
                                    onClick = {
                                        connectionToRemove = connection
                                        showRemoveDialog = true
                                    },
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .size(32.dp)
                                        .padding(4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Remove connection",
                                        tint = Color(0xFFDC143C),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

// Confirmation Dialog
        if (showRemoveDialog && connectionToRemove != null) {
            AlertDialog(
                onDismissRequest = {
                    showRemoveDialog = false
                    connectionToRemove = null
                },
                title = {
                    Text("Remove Connection?", color = Color.White, fontWeight = FontWeight.Bold)
                },
                text = {
                    Text("Are you sure you want to remove ${connectionToRemove?.name} from your connections?", color = textColor.copy(alpha = 0.92f))
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            scope.launch {
                                try {
                                    val currentUid = auth.currentUser?.uid ?: return@launch
                                    val otherUid = connectionToRemove?.uid ?: return@launch

// Remove from both users' connections
                                    db.collection("connections").document(currentUid)
                                        .update("connectedUids", FieldValue.arrayRemove(otherUid)).await()

                                    db.collection("connections").document(otherUid)
                                        .update("connectedUids", FieldValue.arrayRemove(currentUid)).await()

                                    Toast.makeText(context, "Connection removed", Toast.LENGTH_SHORT).show()
                                    showRemoveDialog = false
                                    connectionToRemove = null
                                } catch (e: Exception) {
                                    Log.e("RemoveConnection", "Error removing connection: ${e.message}", e)
                                    Toast.makeText(context, "Failed to remove: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    ) {
                        Text("REMOVE", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showRemoveDialog = false
                            connectionToRemove = null
                        }
                    ) {
                        Text("CANCEL", color = Color.White.copy(alpha = 0.7f))
                    }
                },
                containerColor = Color(0xFF1A1A1A),
                titleContentColor = Color.White,
                textContentColor = Color.White
            )
        }
    }
}

@Composable
fun ClassroomsScreen(
    primaryColor: Color,
    bgOpacity: Float,
    isSimplifiedUI: Boolean,
    textSizeMultiplier: Float,
    lightMode: Boolean = false,
    onBack: () -> Unit,
    onOpenClassroom: (String) -> Unit
) {
    val textColor = if (lightMode) Color.Black else Color.White
    val textColorSecondary = if (lightMode) Color.Black.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.9f)
    val textColorTertiary = if (lightMode) Color.Black.copy(alpha = 0.75f) else Color.White.copy(alpha = 0.85f)

    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentUid = auth.currentUser?.uid

    var profile by remember { mutableStateOf<UserProfile?>(null) }
    var taughtClassrooms by remember { mutableStateOf<Map<String, Classroom>>(emptyMap()) }
    var memberClassrooms by remember { mutableStateOf<Map<String, Classroom>>(emptyMap()) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var newClassName by remember { mutableStateOf("") }
    var showJoinDialog by remember { mutableStateOf(false) }
    var joinCodeInput by remember { mutableStateOf("") }
    var isBusy by remember { mutableStateOf(false) }
    var lastCreatedCode by remember { mutableStateOf<String?>(null) }
    var showCodeDialog by remember { mutableStateOf(false) }

    LaunchedEffect(currentUid) {
        if (currentUid == null) return@LaunchedEffect
        db.collection("users").document(currentUid).addSnapshotListener { snap, _ ->
            profile = snap?.toObject(UserProfile::class.java)
        }
    }

    DisposableEffect(currentUid) {
        val uid = currentUid
        val regs = if (uid != null) {
            val reg1 = db.collection("classrooms").whereEqualTo("teacherId", uid)
                .addSnapshotListener { snapshot, _ ->
                    taughtClassrooms = snapshot?.documents?.mapNotNull { doc ->
                        doc.toObject(Classroom::class.java)?.copy(id = doc.id)
                    }?.associateBy { it.id } ?: emptyMap()
                }
            val reg2 = db.collection("classrooms").whereArrayContains("memberIds", uid)
                .addSnapshotListener { snapshot, _ ->
                    memberClassrooms = snapshot?.documents?.mapNotNull { doc ->
                        doc.toObject(Classroom::class.java)?.copy(id = doc.id)
                    }?.associateBy { it.id } ?: emptyMap()
                }
            listOf(reg1, reg2)
        } else emptyList()
        onDispose {
            regs.forEach { it.remove() }
        }
    }

    val allClassrooms = remember(taughtClassrooms, memberClassrooms) {
        (taughtClassrooms + memberClassrooms).values.sortedByDescending { it.createdAt }
    }

    val isTeacher = profile?.role == "teacher"

    if (showCodeDialog && lastCreatedCode != null) {
        AlertDialog(
            onDismissRequest = { showCodeDialog = false },
            title = { Text("Classroom created", color = textColor) },
            text = {
                Column {
                    Text("Share this invite code with students in your chapter:", color = textColorSecondary)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        lastCreatedCode!!,
                        style = MaterialTheme.typography.headlineSmall,
                        color = primaryColor,
                        fontWeight = FontWeight.Black
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showCodeDialog = false }) { Text("Done") }
            },
            containerColor = if (lightMode) Color(0xFFF5F8FF) else Color(0xFF1A1A1A)
        )
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { if (!isBusy) showCreateDialog = false },
            title = { Text("New classroom", color = textColor) },
            text = {
                OutlinedTextField(
                    value = newClassName,
                    onValueChange = { newClassName = it },
                    label = { Text("Classroom name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor,
                        focusedBorderColor = primaryColor,
                        unfocusedBorderColor = textColor.copy(alpha = 0.5f)
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (currentUid == null || profile == null) return@Button
                        val roomTitle = newClassName.trim().ifEmpty { "Classroom" }
                        isBusy = true
                        scope.launch {
                            try {
                                var invite = ""
                                for (i in 0 until 12) {
                                    val candidate = randomClassroomInviteCode()
                                    val taken = db.collection("classrooms").whereEqualTo("inviteCode", candidate).limit(1).get().await()
                                    if (taken.isEmpty) {
                                        invite = candidate
                                        break
                                    }
                                }
                                if (invite.isEmpty()) {
                                    Toast.makeText(context, "Could not generate a code. Try again.", Toast.LENGTH_SHORT).show()
                                    isBusy = false
                                    return@launch
                                }
                                val id = db.collection("classrooms").document().id
                                val room = Classroom(
                                    id = id,
                                    name = roomTitle,
                                    teacherId = currentUid,
                                    teacherName = profile!!.name.ifEmpty { "Teacher" },
                                    chapter = profile!!.school,
                                    state = profile!!.state,
                                    inviteCode = invite,
                                    memberIds = listOf(currentUid),
                                    createdAt = System.currentTimeMillis()
                                )
                                db.collection("classrooms").document(id).set(room).await()
                                newClassName = ""
                                showCreateDialog = false
                                lastCreatedCode = invite
                                showCodeDialog = true
                            } catch (e: Exception) {
                                Toast.makeText(context, e.message ?: "Error", Toast.LENGTH_LONG).show()
                            } finally {
                                isBusy = false
                            }
                        }
                    },
                    enabled = !isBusy && profile != null
                ) { Text(if (isBusy) "..." else "Create") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }, enabled = !isBusy) { Text("Cancel") }
            },
            containerColor = if (lightMode) Color(0xFFF5F8FF) else Color(0xFF1A1A1A)
        )
    }

    if (showJoinDialog) {
        AlertDialog(
            onDismissRequest = { if (!isBusy) showJoinDialog = false },
            title = { Text("Join with code", color = textColor) },
            text = {
                OutlinedTextField(
                    value = joinCodeInput,
                    onValueChange = { joinCodeInput = it.uppercase() },
                    label = { Text("Invite code") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor,
                        focusedBorderColor = primaryColor,
                        unfocusedBorderColor = textColor.copy(alpha = 0.5f)
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (currentUid == null || profile == null) return@Button
                        val code = joinCodeInput.trim().ifEmpty { return@Button }
                        isBusy = true
                        scope.launch {
                            try {
                                val snap = db.collection("classrooms").whereEqualTo("inviteCode", code).limit(1).get().await()
                                val doc = snap.documents.firstOrNull()
                                if (doc == null) {
                                    Toast.makeText(context, "Invalid code", Toast.LENGTH_SHORT).show()
                                    isBusy = false
                                    return@launch
                                }
                                val room = doc.toObject(Classroom::class.java)?.copy(id = doc.id) ?: run {
                                    isBusy = false
                                    return@launch
                                }
                                if (room.state.isNotEmpty() && profile!!.state.isNotEmpty() &&
                                    room.state.trim().lowercase() != profile!!.state.trim().lowercase()) {
                                    Toast.makeText(context, "This classroom is for ${room.state} students only", Toast.LENGTH_LONG).show()
                                    isBusy = false
                                    return@launch
                                }
                                if (currentUid in room.memberIds) {
                                    Toast.makeText(context, "You're already in this classroom", Toast.LENGTH_SHORT).show()
                                    joinCodeInput = ""
                                    showJoinDialog = false
                                    isBusy = false
                                    return@launch
                                }
                                doc.reference.update("memberIds", FieldValue.arrayUnion(currentUid)).await()
                                Toast.makeText(context, "Joined \"${room.name}\"", Toast.LENGTH_SHORT).show()
                                joinCodeInput = ""
                                showJoinDialog = false
                            } catch (e: Exception) {
                                Toast.makeText(context, e.message ?: "Error", Toast.LENGTH_LONG).show()
                            } finally {
                                isBusy = false
                            }
                        }
                    },
                    enabled = !isBusy && joinCodeInput.isNotBlank()
                ) { Text("Join") }
            },
            dismissButton = {
                TextButton(onClick = { showJoinDialog = false }, enabled = !isBusy) { Text("Cancel") }
            },
            containerColor = if (lightMode) Color(0xFFF5F8FF) else Color(0xFF1A1A1A)
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize().background(if (lightMode) Color.White else Color(0xFF0A1628)))
        if (!lightMode) Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = bgOpacity)))

        Column(modifier = Modifier.fillMaxSize()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = if (lightMode) Color(0xFFF5F8FF) else Color(0xFF1A1A1A),
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp, start = 4.dp, end = 16.dp, bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = textColor)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "CLASSROOMS",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                color = primaryColor,
                                fontSize = (MaterialTheme.typography.titleLarge.fontSize.value * textSizeMultiplier).sp
                            )
                        )
                        Text(
                            "Private chapter group chats",
                            color = textColorSecondary,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = (MaterialTheme.typography.bodySmall.fontSize.value * textSizeMultiplier).sp)
                        )
                    }
                }
            }

            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(24.dp)) {
                item { Spacer(modifier = Modifier.height(8.dp)) }

                if (isTeacher) {
                    item {
                        Button(
                            onClick = { showCreateDialog = true },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Add, null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("CREATE CLASSROOM", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                item {
                    OutlinedButton(
                        onClick = { showJoinDialog = true },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        border = BorderStroke(1.5.dp, primaryColor),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = primaryColor),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Login, null, tint = primaryColor)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("JOIN WITH CODE", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                if (allClassrooms.isEmpty()) {
                    item {
                        Text(
                            "No classrooms yet. ${if (isTeacher) "Create one for your chapter, or join with a code from another teacher." else "Ask your teacher for an invite code."}",
                            color = textColorTertiary,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = (MaterialTheme.typography.bodyMedium.fontSize.value * textSizeMultiplier).sp)
                        )
                    }
                } else {
                    items(allClassrooms.size) { index ->
                        val room = allClassrooms[index]
                        val isOwner = room.teacherId == currentUid
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                                .clickable { onOpenClassroom(room.id) },
                            colors = CardDefaults.cardColors(containerColor = if (lightMode) Color.White else Color(0xFF1A1A1A)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Chat, null, tint = primaryColor, modifier = Modifier.size(28.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        room.name,
                                        color = textColor,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium.copy(fontSize = (MaterialTheme.typography.titleMedium.fontSize.value * textSizeMultiplier).sp)
                                    )
                                    Text(
                                        if (isOwner) "You teach • Code: ${room.inviteCode}" else "${room.teacherName} • ${room.memberIds.size} members",
                                        color = textColorSecondary,
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = (MaterialTheme.typography.bodySmall.fontSize.value * textSizeMultiplier).sp)
                                    )
                                }
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = primaryColor)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ClassroomChatScreen(
    classroomId: String,
    onBack: () -> Unit,
    primaryColor: Color,
    bgOpacity: Float,
    isSimplifiedUI: Boolean,
    textSizeMultiplier: Float,
    lightMode: Boolean = false
) {
    val textColor = if (lightMode) Color.Black else Color.White
    val textColorSecondary = if (lightMode) Color.Black.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.9f)

    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentUserId = auth.currentUser?.uid ?: ""

    var classroom by remember { mutableStateOf<Classroom?>(null) }
    var messages by remember { mutableStateOf<List<ClassroomMessage>>(emptyList()) }
    var messageText by remember { mutableStateOf("") }
    var myName by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    var messageToDelete by remember { mutableStateOf<ClassroomMessage?>(null) }

    LaunchedEffect(currentUserId) {
        if (currentUserId.isEmpty()) return@LaunchedEffect
        db.collection("users").document(currentUserId).addSnapshotListener { snap, _ ->
            myName = snap?.toObject(UserProfile::class.java)?.name ?: ""
        }
    }

    LaunchedEffect(classroomId) {
        if (classroomId.isEmpty()) return@LaunchedEffect
        db.collection("classrooms").document(classroomId).addSnapshotListener { snap, _ ->
            classroom = snap?.toObject(Classroom::class.java)?.copy(id = snap.id)
        }
    }

    LaunchedEffect(classroomId) {
        if (classroomId.isEmpty()) return@LaunchedEffect
        db.collection("classrooms").document(classroomId).collection("messages")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.ASCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) return@addSnapshotListener
                messages = snap?.documents?.mapNotNull { doc ->
                    doc.toObject(ClassroomMessage::class.java)?.copy(id = doc.id)
                } ?: emptyList()
            }
    }

    LaunchedEffect(classroom, currentUserId) {
        val c = classroom
        if (c == null || currentUserId.isEmpty()) return@LaunchedEffect
        if (currentUserId !in c.memberIds) {
            Toast.makeText(context, "You don't have access to this classroom", Toast.LENGTH_SHORT).show()
            onBack()
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val isTeacherInRoom = classroom?.teacherId == currentUserId

    // Delete message confirmation dialog
    messageToDelete?.let { msgToDelete ->
        AlertDialog(
            onDismissRequest = { messageToDelete = null },
            title = { Text("Delete message?", color = if (lightMode) Color.Black else Color.White) },
            text = { Text("This message will be permanently removed.", color = if (lightMode) Color.Black.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.8f)) },
            confirmButton = {
                Button(
                    onClick = {
                        val msgId = msgToDelete.id
                        messageToDelete = null
                        db.collection("classrooms").document(classroomId)
                            .collection("messages").document(msgId)
                            .delete()
                            .addOnSuccessListener {
                                Toast.makeText(context, "Message deleted", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                ) { Text("Delete", color = Color.White, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { messageToDelete = null }) { Text("Cancel") }
            },
            containerColor = if (lightMode) Color(0xFFF5F8FF) else Color(0xFF1A1A1A)
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize().background(if (lightMode) Color.White else Color(0xFF0A1628)))
        if (!lightMode) Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = bgOpacity)))

        Column(modifier = Modifier.fillMaxSize()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = if (lightMode) Color(0xFFF5F8FF) else Color(0xFF131F35),
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp, start = 4.dp, end = 16.dp, bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = textColor)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            classroom?.name ?: "Classroom",
                            color = textColor,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = (MaterialTheme.typography.titleMedium.fontSize.value * textSizeMultiplier).sp)
                        )
                        Text(
                            if (isTeacherInRoom) "Long-press a message to delete" else "Group chat",
                            color = primaryColor,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = (MaterialTheme.typography.bodySmall.fontSize.value * textSizeMultiplier).sp)
                        )
                    }
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                if (messages.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Chat, null, tint = textColor.copy(alpha = 0.5f), modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No messages yet", color = textColorSecondary, fontWeight = FontWeight.Bold)
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        items(messages.size) { index ->
                            val message = messages[index]
                            val isMe = message.senderId == currentUserId
                            val canDelete = isTeacherInRoom || isMe
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                            ) {
                                Column(horizontalAlignment = if (isMe) Alignment.End else Alignment.Start) {
                                    if (!isMe && message.senderName.isNotEmpty()) {
                                        Text(
                                            message.senderName,
                                            color = textColorSecondary,
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = (10 * textSizeMultiplier).sp),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isMe) primaryColor else (if (lightMode) Color(0xFFEEF2FB) else Color(0xFF1E2D45))
                                        ),
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier.pointerInput(canDelete) {
                                            if (canDelete) {
                                                detectTapGestures(onLongPress = { messageToDelete = message })
                                            }
                                        }
                                    ) {
                                        Text(
                                            message.message,
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                            color = if (isMe) Color.Black else textColor,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = (MaterialTheme.typography.bodyMedium.fontSize.value * textSizeMultiplier).sp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = if (lightMode) Color(0xFFF5F8FF) else Color(0xFF131F35),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        placeholder = { Text("Message…", color = textColor.copy(alpha = 0.45f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor,
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = textColor.copy(alpha = 0.2f),
                            cursorColor = primaryColor,
                            focusedContainerColor = if (lightMode) Color.White else Color(0xFF1A2840),
                            unfocusedContainerColor = if (lightMode) Color.White else Color(0xFF1A2840)
                        ),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 3
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            val c = classroom
                            if (c == null || currentUserId !in c.memberIds) {
                                Toast.makeText(context, "Can't send", Toast.LENGTH_SHORT).show()
                                return@IconButton
                            }
                            if (messageText.isBlank()) return@IconButton
                            val id = db.collection("classrooms").document(classroomId).collection("messages").document().id
                            val msg = ClassroomMessage(
                                id = id,
                                senderId = currentUserId,
                                senderName = myName,
                                message = messageText.trim(),
                                timestamp = System.currentTimeMillis()
                            )
                            messageText = ""
                            db.collection("classrooms").document(classroomId).collection("messages").document(id).set(msg)
                                .addOnFailureListener { e ->
                                    Toast.makeText(context, e.message ?: "Send failed", Toast.LENGTH_SHORT).show()
                                }
                        },
                        enabled = messageText.isNotBlank(),
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                if (messageText.isNotBlank()) primaryColor else Color.Transparent,
                                CircleShape
                            )
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = if (messageText.isNotBlank()) Color.Black else textColor.copy(alpha = 0.4f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MessagesScreen(
    primaryColor: Color,
    bgOpacity: Float,
    isSimplifiedUI: Boolean,
    textSizeMultiplier: Float,
    lightMode: Boolean = false,
    onBack: () -> Unit,
    onOpenChat: (String, String) -> Unit
) {
// Dynamic text colors
    val textColor = if (lightMode) Color.Black else Color.White
    val textColorSecondary = if (lightMode) Color.Black.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.9f)
    val textColorTertiary = if (lightMode) Color.Black.copy(alpha = 0.75f) else Color.White.copy(alpha = 0.85f)

    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current

    var connections by remember { mutableStateOf<List<Connection>>(emptyList()) }
    var conversations by remember { mutableStateOf<Map<String, Conversation>>(emptyMap()) }

// Load connections
    LaunchedEffect(Unit) {
        val currentUid = auth.currentUser?.uid ?: return@LaunchedEffect

        db.collection("connections").document(currentUid)
            .addSnapshotListener { snapshot, _ ->
                val connectedUids = snapshot?.get("connectedUids") as? List<String> ?: emptyList()

                if (connectedUids.isNotEmpty()) {
                    db.collection("users").whereIn("uid", connectedUids)
                        .addSnapshotListener { usersSnapshot, _ ->
                            connections = usersSnapshot?.documents?.mapNotNull { doc ->
                                doc.toObject(UserProfile::class.java)?.let { profile ->
                                    Connection(
                                        uid = profile.uid,
                                        name = profile.name,
                                        email = profile.email,
                                        school = profile.school,
                                        pfpUrl = profile.pfpUrl
                                    )
                                }
                            } ?: emptyList()
                        }
                } else {
                    connections = emptyList()
                }
            }

// Load conversations with last messages
        db.collection("messages")
            .where(com.google.firebase.firestore.Filter.or(
                com.google.firebase.firestore.Filter.equalTo("senderId", currentUid),
                com.google.firebase.firestore.Filter.equalTo("receiverId", currentUid)
            ))
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                val convos = mutableMapOf<String, Conversation>()
                snapshot?.documents?.forEach { doc ->
                    val msg = doc.toObject(ChatMessage::class.java) ?: return@forEach
                    val otherUserId = if (msg.senderId == currentUid) msg.receiverId else msg.senderId

                    if (!convos.containsKey(otherUserId)) {
                        val connection = connections.find { it.uid == otherUserId }
                        convos[otherUserId] = Conversation(
                            otherUserId = otherUserId,
                            otherUserName = connection?.name ?: "Unknown User",
                            otherUserPfp = connection?.pfpUrl ?: "",
                            lastMessage = msg.message,
                            lastMessageTime = msg.timestamp
                        )
                    }
                }
                conversations = convos
            }
    }

    Box(modifier = Modifier.fillMaxSize()) {
// Background — white in light mode, dark blue in dark mode
        Box(modifier = Modifier.fillMaxSize().background(
            if (lightMode) Color.White else Color(0xFF0A1628)
        ))
        if (!lightMode) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = bgOpacity)))
        }

        Column(modifier = Modifier.fillMaxSize()) {
// Header with back button
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = if (lightMode) Color(0xFFF5F8FF) else Color(0xFF1A1A1A),
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp, start = 4.dp, end = 16.dp, bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = textColor)
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "MESSAGES",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                color = primaryColor,
                                fontSize = (MaterialTheme.typography.titleLarge.fontSize.value * textSizeMultiplier).sp
                            )
                        )
                        Text(
                            "Chat with your connections",
                            color = textColorSecondary,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = (MaterialTheme.typography.bodySmall.fontSize.value * textSizeMultiplier).sp)
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(24.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }

                if (connections.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = if (lightMode) Color(0xFFF0F4FF) else Color.White.copy(alpha = 0.1f)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(40.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.Chat,
                                    contentDescription = null,
                                    tint = textColor.copy(alpha = 0.65f),
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "No Connections Yet",
                                    color = textColorSecondary,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium.copy(fontSize = (MaterialTheme.typography.titleMedium.fontSize.value * textSizeMultiplier).sp)
                                )
                                Text(
                                    "Connect with other members to start messaging",
                                    color = textColor.copy(alpha = 0.75f),
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = (MaterialTheme.typography.bodySmall.fontSize.value * textSizeMultiplier).sp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(connections.size) { index ->
                        val connection = connections[index]
                        val conversation = conversations[connection.uid]
                        val sdf = remember { java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault()) }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp),
                            onClick = { onOpenChat(connection.uid, connection.name) },
                            colors = CardDefaults.cardColors(
                                containerColor = if (lightMode) Color.White else Color(0xFF131F35)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Avatar
                                Box(
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(CircleShape)
                                        .background(primaryColor.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (connection.pfpUrl.isNotEmpty()) {
                                        AsyncImage(
                                            model = connection.pfpUrl,
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Text(
                                            connection.name.take(1).uppercase(),
                                            color = primaryColor,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = (18 * textSizeMultiplier).sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            connection.name,
                                            color = textColor,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontSize = (MaterialTheme.typography.titleSmall.fontSize.value * textSizeMultiplier).sp
                                            ),
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (conversation != null) {
                                            Text(
                                                sdf.format(java.util.Date(conversation.lastMessageTime)),
                                                color = textColorTertiary,
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontSize = (10 * textSizeMultiplier).sp
                                                )
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        conversation?.lastMessage?.take(50) ?: connection.school.ifEmpty { "Tap to start chatting" },
                                        color = if (conversation != null) textColorSecondary else textColorTertiary,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = (MaterialTheme.typography.bodySmall.fontSize.value * textSizeMultiplier).sp
                                        ),
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
fun ChatScreen(
    otherUserId: String,
    onBack: () -> Unit,
    primaryColor: Color,
    bgOpacity: Float,
    isSimplifiedUI: Boolean,
    textSizeMultiplier: Float,
    lightMode: Boolean = false
) {
// Dynamic text colors
    val textColor = if (lightMode) Color.Black else Color.White
    val textColorSecondary = if (lightMode) Color.Black.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.9f)
    val textColorTertiary = if (lightMode) Color.Black.copy(alpha = 0.75f) else Color.White.copy(alpha = 0.85f)

    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    val currentUserId = auth.currentUser?.uid ?: ""

    var messages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var messageText by remember { mutableStateOf("") }
    var otherUserProfile by remember { mutableStateOf<UserProfile?>(null) }
    var previousMessageCount by remember { mutableStateOf(0) }
    var newMessageIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var showMessageOptions by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

// Load other user profile
    LaunchedEffect(otherUserId) {
        db.collection("users").document(otherUserId).get().addOnSuccessListener { snapshot ->
            otherUserProfile = snapshot.toObject(UserProfile::class.java)
        }
    }

// Load messages
    LaunchedEffect(Unit) {
// Listen for messages in this conversation
        val listener = db.collection("messages")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("ChatScreen", "Error loading messages", error)
                    Toast.makeText(context, "Error loading messages: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

// Filter messages for this conversation
                val allMessages = snapshot?.documents?.mapNotNull {
                    try {
                        it.toObject(ChatMessage::class.java)
                    } catch (e: Exception) {
                        android.util.Log.e("ChatScreen", "Error parsing message", e)
                        null
                    }
                } ?: emptyList()

                val conversationMessages = allMessages.filter { msg ->
                    (msg.senderId == currentUserId && msg.receiverId == otherUserId) ||
                            (msg.senderId == otherUserId && msg.receiverId == currentUserId)
                }

                android.util.Log.d("ChatScreen", "Loaded ${conversationMessages.size} messages from Firebase")

// Merge with existing messages (keep optimistic updates)
                val currentMessageIds = messages.map { it.id }.toSet()
                val newFromFirebase = conversationMessages.filter { it.id !in currentMessageIds }

// Check if there's a new message from the other user
                if (newFromFirebase.isNotEmpty()) {
                    val latestNew = newFromFirebase.lastOrNull()
                    if (latestNew?.senderId == otherUserId) {
// Send notification for new message
                        sendMessageNotification(
                            context,
                            latestNew.senderName.ifEmpty { otherUserProfile?.name ?: "Unknown User" },
                            latestNew.message
                        )
// Mark as new for animation
                        newMessageIds = newMessageIds + latestNew.id
                        scope.launch {
                            delay(500)
                            newMessageIds = newMessageIds - latestNew.id
                        }
                    }
                }

// Update messages list (keeping existing + adding new from Firebase)
                if (newFromFirebase.isNotEmpty()) {
                    messages = conversationMessages
                    scope.launch {
                        if (messages.isNotEmpty()) {
                            listState.animateScrollToItem(messages.size - 1)
                        }
                    }
                }

                previousMessageCount = conversationMessages.size
            }
    }

    Box(modifier = Modifier.fillMaxSize()) {
// Background — white in light mode, dark blue in dark mode
        Box(modifier = Modifier.fillMaxSize().background(
            if (lightMode) Color.White else Color(0xFF0A1628)
        ))
        if (!lightMode) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = bgOpacity)))
        }

        Column(modifier = Modifier.fillMaxSize()) {
// Header
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = if (lightMode) Color(0xFFF5F8FF) else Color(0xFF131F35),
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp, start = 0.dp, end = 16.dp, bottom = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = textColor)
                    }

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(primaryColor.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (otherUserProfile?.pfpUrl?.isNotEmpty() == true) {
                            AsyncImage(
                                model = otherUserProfile!!.pfpUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(
                                (otherUserProfile?.name ?: "?").take(1).uppercase(),
                                color = primaryColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            otherUserProfile?.name ?: "Loading...",
                            color = textColor,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = (MaterialTheme.typography.titleMedium.fontSize.value * textSizeMultiplier).sp)
                        )
                        Text(
                            otherUserProfile?.school?.takeIf { it.isNotEmpty() } ?: "${messages.size} messages",
                            color = primaryColor,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = (MaterialTheme.typography.bodySmall.fontSize.value * textSizeMultiplier).sp)
                        )
                    }
                }
            }

// Messages list
            Box(modifier = Modifier.weight(1f)) {
                if (messages.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(primaryColor.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Chat, null, tint = primaryColor, modifier = Modifier.size(36.dp))
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No messages yet",
                            color = textColor,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = (MaterialTheme.typography.titleMedium.fontSize.value * textSizeMultiplier).sp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Say hi to ${otherUserProfile?.name?.substringBefore(" ") ?: "them"}!",
                            color = textColorTertiary,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = (MaterialTheme.typography.bodySmall.fontSize.value * textSizeMultiplier).sp)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = listState,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        items(messages.size) { index ->
                            val message = messages[index]
                            val isMe = message.senderId == currentUserId
                            val isNew = message.id in newMessageIds
                            val isLastInGroup = index == messages.size - 1 || messages[index + 1].senderId != message.senderId
                            val isFirstInGroup = index == 0 || messages[index - 1].senderId != message.senderId

                            val animatedAlpha = remember { androidx.compose.animation.core.Animatable(if (isNew) 0f else 1f) }
                            val animatedOffset = remember { androidx.compose.animation.core.Animatable(if (isNew) 30f else 0f) }

                            LaunchedEffect(isNew) {
                                if (isNew) {
                                    launch { animatedAlpha.animateTo(1f, animationSpec = androidx.compose.animation.core.tween(250)) }
                                    launch {
                                        animatedOffset.animateTo(0f, animationSpec = androidx.compose.animation.core.spring(
                                            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                                            stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
                                        ))
                                    }
                                }
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = if (isFirstInGroup) 8.dp else 2.dp, bottom = if (isLastInGroup) 2.dp else 1.dp)
                                    .graphicsLayer(alpha = animatedAlpha.value, translationY = animatedOffset.value),
                                horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    if (!isMe) Spacer(modifier = Modifier.width(4.dp))
                                    if (isMe) Spacer(modifier = Modifier.width(56.dp))

                                    Box(
                                        modifier = Modifier
                                            .background(
                                                color = if (isMe) primaryColor else (if (lightMode) Color(0xFFEEF2FB) else Color(0xFF1E2D45)),
                                                shape = RoundedCornerShape(
                                                    topStart = 18.dp,
                                                    topEnd = 18.dp,
                                                    bottomStart = if (isMe) 18.dp else if (isLastInGroup) 6.dp else 4.dp,
                                                    bottomEnd = if (isMe) if (isLastInGroup) 6.dp else 4.dp else 18.dp
                                                )
                                            )
                                            .pointerInput(Unit) {
                                                detectTapGestures(onLongPress = {
                                                    if (isMe) { selectedMessage = message; showMessageOptions = true }
                                                })
                                            }
                                    ) {
                                        Text(
                                            message.message,
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                                            color = if (isMe) Color.Black else textColor,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontSize = (MaterialTheme.typography.bodyMedium.fontSize.value * textSizeMultiplier).sp
                                            )
                                        )
                                    }

                                    if (!isMe) Spacer(modifier = Modifier.width(56.dp))
                                }

                                if (isLastInGroup) {
                                    val sdf = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
                                    Text(
                                        sdf.format(java.util.Date(message.timestamp)),
                                        color = textColorTertiary,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = (10 * textSizeMultiplier).sp
                                        ),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

// Message input
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = if (lightMode) Color(0xFFF5F8FF) else Color(0xFF131F35),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        placeholder = { Text("Message…", color = textColorTertiary, style = MaterialTheme.typography.bodyMedium.copy(fontSize = (14 * textSizeMultiplier).sp)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor,
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = textColor.copy(alpha = 0.2f),
                            cursorColor = primaryColor,
                            focusedContainerColor = if (lightMode) Color.White else Color(0xFF1A2840),
                            unfocusedContainerColor = if (lightMode) Color.White else Color(0xFF1A2840)
                        ),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 4,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = (14 * textSizeMultiplier).sp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            if (currentUserId.isEmpty() || otherUserId.isEmpty()) return@IconButton
                            if (messageText.isNotBlank()) {
                                val messageId = db.collection("messages").document().id
                                val msgText = messageText.trim()
                                val newMessage = ChatMessage(
                                    id = messageId,
                                    senderId = currentUserId,
                                    receiverId = otherUserId,
                                    message = msgText,
                                    timestamp = System.currentTimeMillis(),
                                    senderName = auth.currentUser?.displayName ?: ""
                                )
                                messages = messages + newMessage
                                newMessageIds = newMessageIds + messageId
                                messageText = ""
                                scope.launch { listState.animateScrollToItem(messages.size - 1) }
                                scope.launch { delay(500); newMessageIds = newMessageIds - messageId }
                                db.collection("messages").document(messageId).set(newMessage)
                                    .addOnFailureListener { e ->
                                        messages = messages.filter { it.id != messageId }
                                        Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                            }
                        },
                        enabled = messageText.isNotBlank(),
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                if (messageText.isNotBlank()) primaryColor else Color.Transparent,
                                CircleShape
                            )
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = if (messageText.isNotBlank()) Color.Black else textColorTertiary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

// Message Options Dialog
        if (showMessageOptions && selectedMessage != null) {
            val message = selectedMessage!!

            AlertDialog(
                onDismissRequest = { showMessageOptions = false },
                containerColor = Color(0xFF2A2A2A),
                title = {
                    Text(
                        "Delete Message?",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column {
                        Text(
                            message.message,
                            color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 3
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
// IMMEDIATE delete from UI
                            messages = messages.filter { it.id != message.id }

// Delete from Firebase
                            db.collection("messages").document(message.id).delete()
                                .addOnSuccessListener {
                                    Toast.makeText(context, "Message deleted", Toast.LENGTH_SHORT).show()
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(context, "Failed to delete: ${e.message}", Toast.LENGTH_SHORT).show()
                                }

                            showMessageOptions = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFDC143C),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Delete, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showMessageOptions = false }) {
                        Text("Cancel", color = textColorSecondary)
                    }
                }
            )
        }
    }
}

// Helper function to get state FBLA social media links
fun getStateFBLASocialMedia(state: String): Map<String, String> {
    return when (state.trim().lowercase()) {
        "wisconsin" -> mapOf(
            "Instagram" to "https://www.instagram.com/wifbla/",
            "Facebook" to "https://www.facebook.com/wifbla/",
            "X (Twitter)" to "https://twitter.com/wifbla"
        )
        "california" -> mapOf(
            "Instagram" to "https://www.instagram.com/cafbla/",
            "Facebook" to "https://www.facebook.com/cafbla/",
            "X (Twitter)" to "https://twitter.com/cafbla"
        )
        "texas" -> mapOf(
            "Instagram" to "https://www.instagram.com/texasfbla/",
            "Facebook" to "https://www.facebook.com/txfbla/",
            "X (Twitter)" to "https://twitter.com/txfbla"
        )
        "florida" -> mapOf(
            "Instagram" to "https://www.instagram.com/floridafbla/",
            "Facebook" to "https://www.facebook.com/floridafbla/",
            "X (Twitter)" to "https://twitter.com/floridafbla"
        )
        "new york" -> mapOf(
            "Instagram" to "https://www.instagram.com/nyfbla/",
            "Facebook" to "https://www.facebook.com/nyfbla/",
            "X (Twitter)" to "https://twitter.com/nyfbla"
        )
        "pennsylvania" -> mapOf(
            "Instagram" to "https://www.instagram.com/pafbla/",
            "Facebook" to "https://www.facebook.com/pafbla/",
            "X (Twitter)" to "https://twitter.com/pafbla"
        )
        "illinois" -> mapOf(
            "Instagram" to "https://www.instagram.com/ilfbla/",
            "Facebook" to "https://www.facebook.com/ilfbla/",
            "X (Twitter)" to "https://twitter.com/ilfbla"
        )
        "ohio" -> mapOf(
            "Instagram" to "https://www.instagram.com/ohiofbla/",
            "Facebook" to "https://www.facebook.com/ohiofbla/",
            "X (Twitter)" to "https://twitter.com/ohiofbla"
        )
        "georgia" -> mapOf(
            "Instagram" to "https://www.instagram.com/gafbla/",
            "Facebook" to "https://www.facebook.com/gafbla/",
            "X (Twitter)" to "https://twitter.com/gafbla"
        )
        "north carolina" -> mapOf(
            "Instagram" to "https://www.instagram.com/ncfbla/",
            "Facebook" to "https://www.facebook.com/ncfbla/",
            "X (Twitter)" to "https://twitter.com/ncfbla"
        )
        "michigan" -> mapOf(
            "Instagram" to "https://www.instagram.com/mifbla/",
            "Facebook" to "https://www.facebook.com/mifbla/",
            "X (Twitter)" to "https://twitter.com/mifbla"
        )
        "virginia" -> mapOf(
            "Instagram" to "https://www.instagram.com/vafbla/",
            "Facebook" to "https://www.facebook.com/vafbla/",
            "X (Twitter)" to "https://twitter.com/vafbla"
        )
        else -> emptyMap() // No state-specific links for other states
    }
}

// Helper function to get Sun Prairie FBLA chapter social media links
fun getSunPrairieChapterSocialMedia(): Map<String, String> {
    return mapOf(
        "Instagram" to "https://www.instagram.com/spw_fbla/",
        "X (Twitter)" to "https://twitter.com/sunprairieFBLA",
        "Facebook" to "https://www.facebook.com/SPWHSFBLA"
    )
}

data class OutfitAnalysisResult(
    val acceptable: Boolean,
    val verdict: String,
    val explanation: String,
    val tips: List<String>
)

@Composable
fun DressCodeCheckerScreen(
    onBack: () -> Unit,
    primaryColor: Color,
    bgOpacity: Float,
    textSizeMultiplier: Float,
    lightMode: Boolean = false
) {
    val context = LocalContext.current
    val textColor = if (lightMode) Color(0xFF1A1A2E) else Color.White
    val textColorSecondary = if (lightMode) Color(0xFF1A1A2E).copy(alpha = 0.8f) else Color.White.copy(alpha = 0.9f)
    val cardColor = if (lightMode) Color(0xFFE8EDF5) else Color.White.copy(alpha = 0.15f)

    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var selectedBase64 by remember { mutableStateOf("") }
    var isAnalyzing by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<OutfitAnalysisResult?>(null) }
    var errorMessage by remember { mutableStateOf("") }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            try {
                val stream = context.contentResolver.openInputStream(it)
                val bitmap = BitmapFactory.decodeStream(stream) ?: throw Exception("Unable to decode image")
                val maxDim = 1024
                val scale = maxDim.toFloat() / maxOf(bitmap.width, bitmap.height).coerceAtLeast(1)
                val w = (bitmap.width * scale).toInt().coerceAtLeast(1)
                val h = (bitmap.height * scale).toInt().coerceAtLeast(1)
                val resized = Bitmap.createScaledBitmap(bitmap, w, h, true)
                selectedBitmap = resized
                val out = ByteArrayOutputStream()
                resized.compress(Bitmap.CompressFormat.JPEG, 85, out)
                selectedBase64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
                result = null
                errorMessage = ""
            } catch (e: Exception) {
                errorMessage = "Failed to load image: ${e.message}"
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize().background(if (lightMode) Color.White else Color(0xFF0A1628)))
        if (!lightMode) Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = bgOpacity)))

        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(24.dp)) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = primaryColor)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Text(
                            "DRESS CODE CHECKER",
                            style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 2.sp, fontWeight = FontWeight.Bold, fontSize = (11 * textSizeMultiplier).sp),
                            color = primaryColor
                        )
                        Text(
                            "AI-Powered FBLA Standard Analysis",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, fontSize = (18 * textSizeMultiplier).sp),
                            color = textColor
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Upload a full-body photo of your outfit and AI will check if it meets FBLA business professional standards.",
                    color = textColorSecondary,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = (13 * textSizeMultiplier).sp)
                )
                Spacer(modifier = Modifier.height(24.dp))

                Card(
                    modifier = Modifier.fillMaxWidth().height(260.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    onClick = { imagePicker.launch("image/*") }
                ) {
                    if (selectedBitmap != null) {
                        androidx.compose.foundation.Image(
                            bitmap = selectedBitmap!!.asImageBitmap(),
                            contentDescription = "Selected outfit",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.AddAPhoto, null, tint = primaryColor, modifier = Modifier.size(56.dp))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Tap to select a photo", color = textColor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge.copy(fontSize = (15 * textSizeMultiplier).sp))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Full-body outfit photo works best", color = textColorSecondary, style = MaterialTheme.typography.bodySmall.copy(fontSize = (12 * textSizeMultiplier).sp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (selectedBitmap != null) {
                    OutlinedButton(
                        onClick = { imagePicker.launch("image/*") },
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = primaryColor)
                    ) {
                        Icon(Icons.Default.Refresh, null, tint = primaryColor, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Change Photo", fontWeight = FontWeight.Bold, style = TextStyle(fontSize = (13 * textSizeMultiplier).sp))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            isAnalyzing = true
                            result = null
                            errorMessage = ""
                            val data = hashMapOf("imageBase64" to selectedBase64, "mimeType" to "image/jpeg")
                            FirebaseFunctions.getInstance()
                                .getHttpsCallable("checkOutfitDressCode")
                                .call(data)
                                .addOnSuccessListener { taskResult ->
                                    isAnalyzing = false
                                    @Suppress("UNCHECKED_CAST")
                                    val map = taskResult.getData() as? Map<String, Any>
                                    if (map != null) {
                                        result = OutfitAnalysisResult(
                                            acceptable = map["acceptable"] as? Boolean ?: false,
                                            verdict = map["verdict"] as? String ?: "",
                                            explanation = map["explanation"] as? String ?: "",
                                            tips = (map["tips"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                                        )
                                    } else {
                                        errorMessage = "Unexpected response format. Please try again."
                                    }
                                }
                                .addOnFailureListener { e ->
                                    isAnalyzing = false
                                    errorMessage = "Analysis failed: ${e.message}"
                                }
                        },
                        enabled = !isAnalyzing,
                        modifier = Modifier.fillMaxWidth().height(50.dp).shadow(4.dp, RoundedCornerShape(12.dp)),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor, contentColor = Color.Black),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isAnalyzing) {
                            CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(20.dp), strokeWidth = 2.5.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Analyzing...", color = Color.Black, fontWeight = FontWeight.Bold, style = TextStyle(fontSize = (14 * textSizeMultiplier).sp))
                        } else {
                            Icon(Icons.Default.AutoAwesome, null, tint = Color.Black, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("ANALYZE OUTFIT", color = Color.Black, fontWeight = FontWeight.Bold, style = TextStyle(fontSize = (14 * textSizeMultiplier).sp))
                        }
                    }
                } else {
                    Button(
                        onClick = { imagePicker.launch("image/*") },
                        modifier = Modifier.fillMaxWidth().height(50.dp).shadow(4.dp, RoundedCornerShape(12.dp)),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor, contentColor = Color.Black),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.AddAPhoto, null, tint = Color.Black, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("SELECT PHOTO", color = Color.Black, fontWeight = FontWeight.Bold, style = TextStyle(fontSize = (14 * textSizeMultiplier).sp))
                    }
                }

                if (errorMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(errorMessage, color = Color(0xFFE74C3C), style = MaterialTheme.typography.bodySmall.copy(fontSize = (12 * textSizeMultiplier).sp))
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = result != null,
                    enter = androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn()
                ) {
                    result?.let { res ->
                        val isOk = res.acceptable
                        val accentColor = if (isOk) Color(0xFF2ECC71) else Color(0xFFE74C3C)
                        val resultCardBg = if (lightMode) {
                            if (isOk) Color(0xFFE8F8EF) else Color(0xFFFDECEC)
                        } else {
                            if (isOk) Color(0xFF0D2B1A) else Color(0xFF2B0D0D)
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = resultCardBg)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        if (isOk) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                        null,
                                        tint = accentColor,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            if (isOk) "DRESS CODE COMPLIANT" else "DOES NOT MEET STANDARDS",
                                            color = accentColor,
                                            fontWeight = FontWeight.Black,
                                            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.sp, fontSize = (11 * textSizeMultiplier).sp)
                                        )
                                        Text(res.verdict, color = textColor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium.copy(fontSize = (14 * textSizeMultiplier).sp))
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(res.explanation, color = textColorSecondary, style = MaterialTheme.typography.bodySmall.copy(fontSize = (13 * textSizeMultiplier).sp))
                                if (res.tips.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Text(
                                        if (isOk) "WHAT YOU DID WELL" else "HOW TO IMPROVE",
                                        color = accentColor,
                                        fontWeight = FontWeight.Black,
                                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp, fontSize = (10 * textSizeMultiplier).sp)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    res.tips.forEach { tip ->
                                        Row(modifier = Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.Top) {
                                            Text("• ", color = accentColor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall.copy(fontSize = (13 * textSizeMultiplier).sp))
                                            Text(tip, color = textColorSecondary, style = MaterialTheme.typography.bodySmall.copy(fontSize = (13 * textSizeMultiplier).sp))
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { selectedBitmap = null; selectedBase64 = ""; result = null; errorMessage = "" },
                            modifier = Modifier.fillMaxWidth().height(44.dp),
                            border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = primaryColor)
                        ) {
                            Text("Try Another Photo", fontWeight = FontWeight.Bold, style = TextStyle(fontSize = (13 * textSizeMultiplier).sp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = cardColor)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                        Icon(Icons.Default.Info, null, tint = primaryColor, modifier = Modifier.size(20.dp).padding(top = 2.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("FBLA Dress Code Basics", color = textColor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall.copy(fontSize = (12 * textSizeMultiplier).sp))
                            Spacer(modifier = Modifier.height(4.dp))
                            listOf(
                                "Business suit or blazer with dress pants/skirt",
                                "Collared dress shirt with tie (men)",
                                "Closed-toe dress shoes",
                                "Skirts/dresses at or below the knee",
                                "No jeans, sneakers, or casual wear"
                            ).forEach { rule ->
                                Text("• $rule", color = textColorSecondary, style = MaterialTheme.typography.bodySmall.copy(fontSize = (11 * textSizeMultiplier).sp), modifier = Modifier.padding(vertical = 1.dp))
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}


