package com.example.myapplication

import android.Manifest
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.window.Dialog
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
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import okhttp3.Request
import androidx.core.content.FileProvider
import java.io.File
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.ui.viewinterop.AndroidView
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors


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
    var showDeleteReminderConfirm by remember { mutableStateOf(false) }
    var showStoryBuilder by remember { mutableStateOf(false) }
    var storyBuilderCompetingIn by remember { mutableStateOf("") }
    var connectionCount by remember { mutableIntStateOf(0) }
    var pendingRequestsCount by remember { mutableIntStateOf(0) }
    var unreadMessageCount by remember { mutableIntStateOf(0) }
    var showMyQr by remember { mutableStateOf(false) }
    var showQrScanner by remember { mutableStateOf(false) }
    var showQrOptions by remember { mutableStateOf(false) }

// Dynamic text colors

    val isTablet = LocalConfiguration.current.screenWidthDp >= 600
    val reduceMotion = LocalReduceMotion.current
    val textColor = if (lightMode) Color(0xFF1A1A2E) else Color.White
    val textColorSecondary = if (lightMode) Color(0xFF1A1A2E).copy(alpha = 0.8f) else Color.White.copy(alpha = 0.9f)
    val textColorTertiary = if (lightMode) Color(0xFF1A1A2E).copy(alpha = 0.75f) else Color.White.copy(alpha = 0.85f)

    LaunchedEffect(Unit) {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            db.collection("users").document(uid).addSnapshotListener { snapshot, _ ->
                profile = snapshot?.toObject(UserProfile::class.java)
            }
            db.collection("connectionRequests")
                .whereEqualTo("toUid", uid)
                .whereEqualTo("status", "pending")
                .addSnapshotListener { snapshot, _ -> pendingRequestsCount = snapshot?.size() ?: 0 }
            db.collection("connections").document(uid)
                .addSnapshotListener { snapshot, _ ->
                    connectionCount = ((snapshot?.get("connectedUids") as? List<*>)?.size) ?: 0
                }
            db.collection("messages").addSnapshotListener { snapshot, _ ->
                unreadMessageCount = (snapshot?.documents?.mapNotNull { it.toObject(ChatMessage::class.java) } ?: emptyList())
                    .filter { it.receiverId == uid && it.senderId != uid }
                    .groupBy { it.senderId }.size
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
// Background — white in light mode, dark blue in dark mode
        Box(modifier = Modifier.fillMaxSize().background(
            if (lightMode) Color(0xFFF0F4FF) else Color(0xFF0A1628)
        ))
        if (!lightMode) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = bgOpacity)))
        }

        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = if (isTablet) PaddingValues(horizontal = 40.dp, vertical = 32.dp) else PaddingValues(24.dp)) {
            item {
                Spacer(modifier = Modifier.height(if (isTablet) 24.dp else 16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "WELCOME, ${profile?.name?.split(" ")?.get(0)?.uppercase() ?: "MEMBER"}",
                            style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 4.sp, fontWeight = FontWeight.Bold, color = primaryColor, fontSize = (MaterialTheme.typography.labelLarge.fontSize.value * textSizeMultiplier).sp)
                        )
                        Text(text = "FBLA Hub Dashboard", style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Black, color = textColor, fontSize = ((if (isTablet) 40f else MaterialTheme.typography.displaySmall.fontSize.value) * textSizeMultiplier).sp))
                    }
                    Box(
                        modifier = Modifier
                            .size(if (isTablet) 56.dp else 48.dp)
                            .background(primaryColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                            .border(1.dp, primaryColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .clickable { showGeminiChat = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, tint = primaryColor, modifier = Modifier.size(if (isTablet) 26.dp else 22.dp))
                    }
                }
                Spacer(modifier = Modifier.height(if (isTablet) 32.dp else 24.dp))
            }

            if (isTablet) {
                // ── TABLET: side-by-side two-column layout ───────────────
                item {
                    val cardBg = if (lightMode) Color(0xFFE8EDF5) else Color.White.copy(alpha = 0.08f)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        // Left column: Reminders
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text("UPCOMING EVENTS", style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 3.sp, fontWeight = FontWeight.Black, color = primaryColor, fontSize = (11 * textSizeMultiplier).sp))
                            if (profile?.reminders?.isNotEmpty() == true) {
                                profile!!.reminders.forEach { reminder ->
                                    val daysLeft = if (reminder.timestamp > System.currentTimeMillis()) {
                                        val cal = Calendar.getInstance()
                                        cal.timeInMillis = System.currentTimeMillis()
                                        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
                                        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                                        val eventCal = Calendar.getInstance()
                                        eventCal.timeInMillis = reminder.timestamp
                                        eventCal.set(Calendar.HOUR_OF_DAY, 0); eventCal.set(Calendar.MINUTE, 0)
                                        eventCal.set(Calendar.SECOND, 0); eventCal.set(Calendar.MILLISECOND, 0)
                                        TimeUnit.MILLISECONDS.toDays(eventCal.timeInMillis - cal.timeInMillis)
                                    } else 0L
                                    Card(
                                        onClick = { selectedReminder = reminder },
                                        modifier = Modifier.fillMaxWidth(),
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
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = if (lightMode) Color(0xFFF0F4FF) else Color.White.copy(alpha = 0.1f)),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Column(modifier = Modifier.padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.AutoMirrored.Filled.EventNote, null, tint = textColor.copy(alpha = 0.65f), modifier = Modifier.size(56.dp))
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text("No Active Reminders", color = textColorTertiary, style = TextStyle(fontSize = (16 * textSizeMultiplier).sp))
                                        Text("Set one in the Events tab", color = textColor.copy(alpha = 0.65f), style = MaterialTheme.typography.bodySmall.copy(fontSize = (MaterialTheme.typography.bodySmall.fontSize.value * textSizeMultiplier).sp))
                                    }
                                }
                            }
                        }
                        // Right column: Networking Hub
                        Column(modifier = Modifier.weight(1f)) {
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("NETWORKING HUB", style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 3.sp, fontWeight = FontWeight.Black, color = primaryColor, fontSize = (11 * textSizeMultiplier).sp))
                                    Text("Your FBLA Network", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, color = textColor, fontSize = (18 * textSizeMultiplier).sp))
                                }
                                Surface(shape = RoundedCornerShape(20.dp), color = primaryColor.copy(alpha = 0.12f), border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.3f))) {
                                    Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(Icons.Default.People, null, tint = primaryColor, modifier = Modifier.size(15.dp))
                                        Text("$connectionCount", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black, fontSize = (13 * textSizeMultiplier).sp), color = primaryColor)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Row(modifier = Modifier.fillMaxWidth().height(100.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Card(modifier = Modifier.weight(1f).fillMaxHeight().pressScale(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.Transparent), onClick = { onNavigate(2, "messages") }) {
                                    Box(modifier = Modifier.fillMaxSize().background(brush = Brush.linearGradient(listOf(primaryColor, primaryColor.copy(alpha = 0.72f))), shape = RoundedCornerShape(20.dp))) {
                                        Box(modifier = Modifier.size(120.dp).align(Alignment.CenterEnd).offset(x = 15.dp).background(Color.White.copy(alpha = 0.09f), CircleShape))
                                        Row(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                            Box(modifier = Modifier.size(46.dp).background(Color.Black.copy(alpha = 0.15f), RoundedCornerShape(13.dp)), contentAlignment = Alignment.Center) {
                                                Icon(Icons.Default.Chat, null, tint = Color.Black, modifier = Modifier.size(25.dp))
                                            }
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("Messages", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, fontSize = (16 * textSizeMultiplier).sp), color = Color.Black)
                                                Text(if (unreadMessageCount > 0) "$unreadMessageCount unread" else "No unread messages", style = MaterialTheme.typography.bodySmall.copy(fontSize = (12 * textSizeMultiplier).sp), color = Color.Black.copy(alpha = 0.65f))
                                            }
                                            if (unreadMessageCount > 0) {
                                                Box(modifier = Modifier.size(24.dp).background(Color.White.copy(alpha = 0.28f), CircleShape), contentAlignment = Alignment.Center) {
                                                    Text(if (unreadMessageCount > 9) "9+" else unreadMessageCount.toString(), color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                                                }
                                            } else {
                                                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color.Black.copy(alpha = 0.65f), modifier = Modifier.size(22.dp))
                                            }
                                        }
                                    }
                                }
                                Box(modifier = Modifier.width(84.dp).fillMaxHeight()) {
                                    Card(modifier = Modifier.fillMaxSize().pressScale(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.Transparent), onClick = { showQrOptions = true }) {
                                        Box(modifier = Modifier.fillMaxSize().background(brush = Brush.linearGradient(listOf(primaryColor.copy(alpha = 0.9f), primaryColor.copy(alpha = 0.6f))), shape = RoundedCornerShape(20.dp)), contentAlignment = Alignment.Center) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                                Icon(Icons.Default.QrCode2, null, tint = Color.Black, modifier = Modifier.size(30.dp))
                                                Text("QR", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontSize = (10 * textSizeMultiplier).sp), color = Color.Black)
                                            }
                                        }
                                    }
                                    Box(modifier = Modifier.align(Alignment.TopEnd).padding(5.dp)) {
                                        InfoIconButton("QR Connect lets you add other FBLA members instantly.\n\n• My QR Code — show your personal QR code for others to scan\n• Scan QR Code — point your camera at another member's QR to send them a connection request\n\nNo searching by name needed — just scan and connect.", primaryColor, lightMode, size = 16)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            // 4-column grid on tablet
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Box(modifier = Modifier.weight(1f)) {
                                    Card(modifier = Modifier.fillMaxWidth().height(110.dp).pressScale(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = cardBg), onClick = { onNavigate(2, "connection_requests") }) {
                                        Column(modifier = Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
                                            Box(modifier = Modifier.size(36.dp).background(primaryColor.copy(alpha = 0.13f), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                                                Icon(Icons.Default.PersonAdd, null, tint = primaryColor, modifier = Modifier.size(20.dp))
                                            }
                                            Column {
                                                Text("Requests", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black, fontSize = (13 * textSizeMultiplier).sp), color = textColor)
                                                Text(if (pendingRequestsCount > 0) "$pendingRequestsCount pending" else "None pending", style = MaterialTheme.typography.labelSmall.copy(fontSize = (10 * textSizeMultiplier).sp), color = if (pendingRequestsCount > 0) primaryColor else textColorSecondary)
                                            }
                                        }
                                    }
                                    if (pendingRequestsCount > 0) {
                                        Box(modifier = Modifier.align(Alignment.TopEnd).offset(x = (-7).dp, y = (-7).dp).size(20.dp).background(Color.Red, CircleShape), contentAlignment = Alignment.Center) {
                                            Text(if (pendingRequestsCount > 9) "9+" else pendingRequestsCount.toString(), color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                Card(modifier = Modifier.weight(1f).height(110.dp).pressScale(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = cardBg), onClick = { onNavigate(2, "find_links") }) {
                                    Column(modifier = Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
                                        Box(modifier = Modifier.size(36.dp).background(primaryColor.copy(alpha = 0.13f), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.PersonSearch, null, tint = primaryColor, modifier = Modifier.size(20.dp))
                                        }
                                        Column {
                                            Text("Find Peers", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black, fontSize = (13 * textSizeMultiplier).sp), color = textColor)
                                            Text("Search members", style = MaterialTheme.typography.labelSmall.copy(fontSize = (10 * textSizeMultiplier).sp), color = textColorSecondary)
                                        }
                                    }
                                }
                                Card(modifier = Modifier.weight(1f).height(110.dp).pressScale(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = cardBg), onClick = { onNavigate(2, "my_connections") }) {
                                    Column(modifier = Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
                                        Box(modifier = Modifier.size(36.dp).background(primaryColor.copy(alpha = 0.13f), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.People, null, tint = primaryColor, modifier = Modifier.size(20.dp))
                                        }
                                        Column {
                                            Text("Network", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black, fontSize = (13 * textSizeMultiplier).sp), color = textColor)
                                            Text("$connectionCount peer${if (connectionCount != 1) "s" else ""}", style = MaterialTheme.typography.labelSmall.copy(fontSize = (10 * textSizeMultiplier).sp), color = textColorSecondary)
                                        }
                                    }
                                }
                                Card(modifier = Modifier.weight(1f).height(110.dp).pressScale(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = cardBg), onClick = { onNavigate(2, "classrooms") }) {
                                    Column(modifier = Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
                                        Box(modifier = Modifier.size(36.dp).background(primaryColor.copy(alpha = 0.13f), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.Groups, null, tint = primaryColor, modifier = Modifier.size(20.dp))
                                        }
                                        Column {
                                            Text("Classrooms", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black, fontSize = (13 * textSizeMultiplier).sp), color = textColor)
                                            Text("Group chats", style = MaterialTheme.typography.labelSmall.copy(fontSize = (10 * textSizeMultiplier).sp), color = textColorSecondary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(28.dp))
                }
            } else {
                // ── PHONE: original single-column layout ─────────────────
                if (profile?.reminders?.isNotEmpty() == true) {
                    itemsIndexed(profile!!.reminders) { index, reminder ->
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
                                    animationSpec = if (reduceMotion) snap() else spring(
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
                    Spacer(modifier = Modifier.height(28.dp))
                    val cardBg = if (lightMode) Color(0xFFE8EDF5) else Color.White.copy(alpha = 0.08f)
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("NETWORKING HUB", style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 3.sp, fontWeight = FontWeight.Black, color = primaryColor, fontSize = (11 * textSizeMultiplier).sp))
                            Text("Your FBLA Network", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, color = textColor, fontSize = (18 * textSizeMultiplier).sp))
                        }
                        Surface(shape = RoundedCornerShape(20.dp), color = primaryColor.copy(alpha = 0.12f), border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.3f))) {
                            Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.People, null, tint = primaryColor, modifier = Modifier.size(15.dp))
                                Text("$connectionCount", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black, fontSize = (13 * textSizeMultiplier).sp), color = primaryColor)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(modifier = Modifier.fillMaxWidth().height(88.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Card(modifier = Modifier.weight(1f).fillMaxHeight().pressScale(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.Transparent), onClick = { onNavigate(2, "messages") }) {
                            Box(modifier = Modifier.fillMaxSize().background(brush = Brush.linearGradient(listOf(primaryColor, primaryColor.copy(alpha = 0.72f))), shape = RoundedCornerShape(20.dp))) {
                                Box(modifier = Modifier.size(100.dp).align(Alignment.CenterEnd).offset(x = 15.dp).background(Color.White.copy(alpha = 0.09f), CircleShape))
                                Row(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Box(modifier = Modifier.size(42.dp).background(Color.Black.copy(alpha = 0.15f), RoundedCornerShape(13.dp)), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Chat, null, tint = Color.Black, modifier = Modifier.size(23.dp))
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Messages", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, fontSize = (15 * textSizeMultiplier).sp), color = Color.Black)
                                        Text(if (unreadMessageCount > 0) "$unreadMessageCount unread" else "No unread messages", style = MaterialTheme.typography.bodySmall.copy(fontSize = (11 * textSizeMultiplier).sp), color = Color.Black.copy(alpha = 0.65f))
                                    }
                                    if (unreadMessageCount > 0) {
                                        Box(modifier = Modifier.size(22.dp).background(Color.White.copy(alpha = 0.28f), CircleShape), contentAlignment = Alignment.Center) {
                                            Text(if (unreadMessageCount > 9) "9+" else unreadMessageCount.toString(), color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                                        }
                                    } else {
                                        Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color.Black.copy(alpha = 0.65f), modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }
                        Box(modifier = Modifier.width(72.dp).fillMaxHeight()) {
                            Card(modifier = Modifier.fillMaxSize().pressScale(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.Transparent), onClick = { showQrOptions = true }) {
                                Box(modifier = Modifier.fillMaxSize().background(brush = Brush.linearGradient(listOf(primaryColor.copy(alpha = 0.9f), primaryColor.copy(alpha = 0.6f))), shape = RoundedCornerShape(20.dp)), contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                        Icon(Icons.Default.QrCode2, null, tint = Color.Black, modifier = Modifier.size(28.dp))
                                        Text("QR", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontSize = (10 * textSizeMultiplier).sp), color = Color.Black)
                                    }
                                }
                            }
                            Box(modifier = Modifier.align(Alignment.TopEnd).padding(5.dp)) {
                                InfoIconButton("QR Connect lets you add other FBLA members instantly.\n\n• My QR Code — show your personal QR code for others to scan\n• Scan QR Code — point your camera at another member's QR to send them a connection request\n\nNo searching by name needed — just scan and connect.", primaryColor, lightMode, size = 16)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(modifier = Modifier.weight(1f)) {
                            Card(modifier = Modifier.fillMaxWidth().height(96.dp).pressScale(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = cardBg), onClick = { onNavigate(2, "connection_requests") }) {
                                Column(modifier = Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
                                    Box(modifier = Modifier.size(34.dp).background(primaryColor.copy(alpha = 0.13f), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.PersonAdd, null, tint = primaryColor, modifier = Modifier.size(19.dp))
                                    }
                                    Column {
                                        Text("Requests", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black, fontSize = (13 * textSizeMultiplier).sp), color = textColor)
                                        Text(if (pendingRequestsCount > 0) "$pendingRequestsCount pending" else "None pending", style = MaterialTheme.typography.labelSmall.copy(fontSize = (10 * textSizeMultiplier).sp), color = if (pendingRequestsCount > 0) primaryColor else textColorSecondary)
                                    }
                                }
                            }
                            if (pendingRequestsCount > 0) {
                                Box(modifier = Modifier.align(Alignment.TopEnd).offset(x = (-7).dp, y = (-7).dp).size(20.dp).background(Color.Red, CircleShape), contentAlignment = Alignment.Center) {
                                    Text(if (pendingRequestsCount > 9) "9+" else pendingRequestsCount.toString(), color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Card(modifier = Modifier.weight(1f).height(96.dp).pressScale(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = cardBg), onClick = { onNavigate(2, "find_links") }) {
                            Column(modifier = Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
                                Box(modifier = Modifier.size(34.dp).background(primaryColor.copy(alpha = 0.13f), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.PersonSearch, null, tint = primaryColor, modifier = Modifier.size(19.dp))
                                }
                                Column {
                                    Text("Find Peers", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black, fontSize = (13 * textSizeMultiplier).sp), color = textColor)
                                    Text("Search members", style = MaterialTheme.typography.labelSmall.copy(fontSize = (10 * textSizeMultiplier).sp), color = textColorSecondary)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Card(modifier = Modifier.weight(1f).height(96.dp).pressScale(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = cardBg), onClick = { onNavigate(2, "my_connections") }) {
                            Column(modifier = Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
                                Box(modifier = Modifier.size(34.dp).background(primaryColor.copy(alpha = 0.13f), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.People, null, tint = primaryColor, modifier = Modifier.size(19.dp))
                                }
                                Column {
                                    Text("Network", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black, fontSize = (13 * textSizeMultiplier).sp), color = textColor)
                                    Text("$connectionCount peer${if (connectionCount != 1) "s" else ""}", style = MaterialTheme.typography.labelSmall.copy(fontSize = (10 * textSizeMultiplier).sp), color = textColorSecondary)
                                }
                            }
                        }
                        Card(modifier = Modifier.weight(1f).height(96.dp).pressScale(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = cardBg), onClick = { onNavigate(2, "classrooms") }) {
                            Column(modifier = Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
                                Box(modifier = Modifier.size(34.dp).background(primaryColor.copy(alpha = 0.13f), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Groups, null, tint = primaryColor, modifier = Modifier.size(19.dp))
                                }
                                Column {
                                    Text("Classrooms", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black, fontSize = (13 * textSizeMultiplier).sp), color = textColor)
                                    Text("Group chats", style = MaterialTheme.typography.labelSmall.copy(fontSize = (10 * textSizeMultiplier).sp), color = textColorSecondary)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(28.dp))
                }
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
                        storyBuilderCompetingIn = ""
                        showStoryBuilder = true
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp).pressScale(),
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
                        modifier = Modifier.weight(1f).height(50.dp).pressScale(),
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
                        onClick = { showDeleteReminderConfirm = true },
                        modifier = Modifier.weight(1f).height(50.dp).pressScale(),
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

                    if (showDeleteReminderConfirm) {
                        AlertDialog(
                            onDismissRequest = { showDeleteReminderConfirm = false },
                            containerColor = if (lightMode) Color.White else Color(0xFF1A2540),
                            shape = RoundedCornerShape(20.dp),
                            icon = {
                                Box(
                                    modifier = Modifier.size(56.dp).background(Color(0xFFDC143C).copy(alpha = 0.15f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Delete, null, tint = Color(0xFFDC143C), modifier = Modifier.size(28.dp))
                                }
                            },
                            title = {
                                Text(
                                    "Delete Reminder?",
                                    color = if (lightMode) Color(0xFF1A1A2E) else Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 20.sp
                                )
                            },
                            text = {
                                Text(
                                    "Remove \"${reminder.name}\" from your reminders? This can't be undone.",
                                    color = if (lightMode) Color(0xFF1A1A2E).copy(alpha = 0.75f) else Color.White.copy(alpha = 0.8f),
                                    textAlign = TextAlign.Center
                                )
                            },
                            confirmButton = {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                            showDeleteReminderConfirm = false
                                            selectedReminder = null
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC143C)),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth().pressScale()
                                    ) {
                                        Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                    OutlinedButton(
                                        onClick = { showDeleteReminderConfirm = false },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth().pressScale(),
                                        border = BorderStroke(1.dp, if (lightMode) Color(0xFF1A1A2E).copy(alpha = 0.3f) else Color.White.copy(alpha = 0.3f))
                                    ) {
                                        Text("Cancel", color = if (lightMode) Color(0xFF1A1A2E) else Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            },
                            dismissButton = {}
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
// ── END REMINDER DETAIL OVERLAY ────────────────────────────────────────

// Story Builder full-screen overlay
    if (showStoryBuilder) {
        StoryBuilderScreen(
            competitionName = selectedReminder?.name ?: "",
            initialCompetingIn = storyBuilderCompetingIn,
            onCompetingInChange = { storyBuilderCompetingIn = it },
            primaryColor = primaryColor,
            lightMode = lightMode,
            context = context,
            pfpUrl = profile?.pfpUrl ?: "",
            onDismiss = { showStoryBuilder = false }
        )
    }

// Gemini Chat Overlay
    if (showGeminiChat) {
        GeminiChatOverlay(
            onDismiss = { showGeminiChat = false },
            primaryColor = primaryColor,
            lightMode = lightMode
        )
    }
// QR Code Dialogs
    val homeUid = auth.currentUser?.uid ?: ""
    if (showQrOptions) {
        AlertDialog(
            onDismissRequest = { showQrOptions = false },
            containerColor = if (lightMode) Color.White else Color(0xFF1A1A2E),
            shape = RoundedCornerShape(24.dp),
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.size(48.dp).background(primaryColor.copy(alpha = 0.12f), CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.QrCode2, null, tint = primaryColor, modifier = Modifier.size(26.dp))
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("QR Connect", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black), color = if (lightMode) Color(0xFF1A1A2E) else Color.White)
                    Text("Connect instantly with FBLA peers", style = MaterialTheme.typography.bodySmall, color = if (lightMode) Color(0xFF6B7280) else Color.White.copy(alpha = 0.6f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(modifier = Modifier.fillMaxWidth().pressScale(), shape = RoundedCornerShape(16.dp), color = primaryColor.copy(alpha = 0.08f), border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.25f)), onClick = { showQrOptions = false; showMyQr = true }) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            Box(modifier = Modifier.size(40.dp).background(primaryColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.QrCode2, null, tint = primaryColor, modifier = Modifier.size(22.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("My QR Code", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black), color = if (lightMode) Color(0xFF1A1A2E) else Color.White)
                                Text("Let others scan to find you", style = MaterialTheme.typography.labelSmall, color = if (lightMode) Color(0xFF6B7280) else Color.White.copy(alpha = 0.55f))
                            }
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = primaryColor, modifier = Modifier.size(18.dp))
                        }
                    }
                    Surface(modifier = Modifier.fillMaxWidth().pressScale(), shape = RoundedCornerShape(16.dp), color = primaryColor.copy(alpha = 0.08f), border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.25f)), onClick = { showQrOptions = false; showQrScanner = true }) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            Box(modifier = Modifier.size(40.dp).background(primaryColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.QrCodeScanner, null, tint = primaryColor, modifier = Modifier.size(22.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Scan a Code", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black), color = if (lightMode) Color(0xFF1A1A2E) else Color.White)
                                Text("Scan someone else's QR to connect", style = MaterialTheme.typography.labelSmall, color = if (lightMode) Color(0xFF6B7280) else Color.White.copy(alpha = 0.55f))
                            }
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = primaryColor, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }
    if (showMyQr && homeUid.isNotEmpty()) {
        QrCodeDialog(uid = homeUid, name = profile?.name ?: "", chapter = profile?.school ?: "", primaryColor = primaryColor, lightMode = lightMode, onDismiss = { showMyQr = false })
    }
    if (showQrScanner) {
        QrScannerScreen(currentUid = homeUid, currentProfile = profile, primaryColor = primaryColor, onDismiss = { showQrScanner = false })
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
        return "Hey! I'm FBLA Hub AI. Ask me anything about FBLA competitions, this app, conferences, or career tips!"

    if (q.matches(Regex("(good morning|good afternoon|good evening|gm|gn|good night).*")))
        return "Hey, good to see you! Hope your day's going well. What can I help you with?"

    if (q.contains("how are you") || q.contains("how r u") || q.contains("how are u") || q.contains("u good") || q.contains("you good"))
        return "Doing great, thanks! Ready to help you with your FBLA goals. What do you need?"

    if (q.contains("what can you do") || q.contains("what do you do") || q.contains("help me") && q.length < 15 || q == "help" || q == "?")
        return "I can help you with:\n\n**This App** — How to use any screen (Home, Events, News, Resources, Profile, Messages, Settings)\n**FBLA Competitions** — Event prep, study tips for any event\n**Conferences** — Regional, State (SLC), and National (NLC)\n**Career & Business** — Resume, LinkedIn, networking, interviews\n**FBLA Basics** — How to join, officer roles, membership\n\nJust ask anything!"

    if (q.contains("thank") || q.contains("thx") || q.contains("ty") || q.contains("thanks"))
        return "No problem! Good luck with everything."

    if (q.contains("bye") || q.contains("goodbye") || q.contains("see ya") || q.contains("later") || q.contains("peace"))
        return "Later! Come back anytime. Good luck!"

    if (q.contains("ok") && q.length < 6 || q == "okay" || q == "k" || q == "cool" || q == "got it" || q == "nice" || q == "lol" || q == "haha" || q == "lmao")
        return "Anything else I can help with?"

    if (q.contains("you're great") || q.contains("ur great") || q.contains("you're awesome") || q.contains("this is helpful") || q.contains("love this") || q.contains("this is cool"))
        return "Appreciate it! Let me know if you need anything else."

    if (q.contains("who are you") || q.contains("what are you") || q.contains("who made you"))
        return "I'm FBLA Hub AI — a built-in assistant for this app! I'm here to help you navigate the app, prep for FBLA competitions, and get career-ready. Ask me anything!"

    if (q.contains("are you real") || q.contains("are you a bot") || q.contains("are you ai") || q.contains("are you human"))
        return "I'm an AI built into FBLA Hub — not human, but I know my FBLA stuff. What do you need help with?"

    if (q.contains("bored") || q.contains("nothing to do"))
        return "There's always FBLA prep to do! Want study tips for a competition event, or want to explore the app?"

    // ── App Navigation Help ────────────────────────────────────────────────
    if (q.contains("home") && (q.contains("screen") || q.contains("tab") || q.contains("page") || q.contains("what")))
        return "The **Home** screen (center tab) is your dashboard. It shows:\n\n• Your upcoming competition reminders with countdown\n• A motivational business tip\n• Quick-access search to jump to any screen\n• The AI chat button\n\nTip: tap any reminder card on Home to see full event details."

    if ((q.contains("event") || q.contains("calendar")) && (q.contains("screen") || q.contains("tab") || q.contains("how") || q.contains("page") || q.contains("where")))
        return "The **Events & Calendar** tab (second from left) lets you:\n\n• Browse FBLA competitions filtered by your state\n• Tap dates on the calendar to filter events for that day\n• Add reminders for events you're entering\n• See a countdown for each upcoming event\n\nYour reminders also show up on the Home screen dashboard!"

    if (q.contains("news") && (q.contains("screen") || q.contains("tab") || q.contains("how") || q.contains("page") || q.contains("where")))
        return "The **News** tab (second from right) shows live articles pulled from FBLA.org.\n\n• Articles are auto-tagged by category (National, Conference, etc.)\n• Tap any article to read the full thing\n• Use the share button to send an article to someone in one tap\n• News updates automatically — no refresh needed!"

    if (q.contains("resource") && (q.contains("screen") || q.contains("tab") || q.contains("how") || q.contains("page") || q.contains("where")))
        return "The **Resources** tab (far right) has:\n\n• Competitive Events guidelines\n• HS 101 Toolkit\n• Brand Center (logos & assets)\n• Dress Code standards\n• NLC Info\n• Scholarships\n• FBLA Study Hub (event prep + practice tests)\n• AI Outfit Checker\n• National FBLA Instagram feed\n• Your chapter's Instagram (if set in your profile)"

    if (q.contains("profile") && (q.contains("screen") || q.contains("tab") || q.contains("how") || q.contains("edit") || q.contains("change") || q.contains("update")))
        return "The **Profile** tab (far left) is where you manage your account:\n\n• Edit your name, email, chapter, graduation year, and state\n• Upload a profile picture\n• View your connections\n• Toggle your profile between public and private\n\nTap **Edit Profile** to update any of your info. Changes save to your account automatically."

    if (q.contains("message") || q.contains("chat") || q.contains("dm") || q.contains("inbox"))
        return "The **Messages** feature lets you chat in real time with your connections.\n\n• Go to Profile tab → tap Messages, or use the search bar on Home\n• Messages are instant once you're connected with someone\n• You can only message people you're connected with\n\nTo connect with someone first, use **Find Peers** from the Home search or Profile tab!"

    if (q.contains("find peer") || q.contains("find member") || q.contains("search user") || q.contains("find people") || q.contains("connect with"))
        return "To find and connect with other FBLA members:\n\n1. Go to the **Home** screen and tap the search icon\n2. Tap **Find Peers** from the quick access list\n3. Search by name or chapter\n4. Tap **Connect** to send a connection request\n\nOnce they accept, you can message each other!"

    if (q.contains("connection request") || q.contains("pending request") || q.contains("accept") && q.contains("request"))
        return "To see your connection requests:\n\n1. Tap the search icon on the Home screen\n2. Select **Connection Requests**\n3. Accept or decline incoming requests\n\nOr go to Profile tab and look for the requests section. Once accepted, you can message each other!"

    if (q.contains("setting") && (q.contains("screen") || q.contains("tab") || q.contains("where") || q.contains("how") || q.contains("find")))
        return "To access **Settings**:\n\n• Go to the **Profile** tab → tap the settings icon\n\nIn Settings you can:\n- Change your accent color\n- Adjust background darkness\n- Scale text size\n- Enable Simplified UI\n- Toggle light/dark mode\n- Manage your account and 2FA\n- Launch the app tutorial again"

    if (q.contains("customize") || q.contains("theme") || q.contains("color") || q.contains("dark mode") || q.contains("light mode") || q.contains("appearance"))
        return "You can fully customize FBLA Hub in **Settings**.\n\n**Accent Color** — Full color picker to set any color you want\n**Background Darkness** — Slider to control overlay darkness\n**Text Size** — Scale text up or down\n**Simplified UI** — Hides background images for a cleaner look\n**Light/Dark Mode** — Switch between modes\n\nGo to Profile tab → Settings to find all of these!"

    if (q.contains("reminder") && (q.contains("add") || q.contains("set") || q.contains("create") || q.contains("how")))
        return "To add a competition reminder:\n\n1. Go to the **Events** tab\n2. Browse or find the competition you're entering\n3. Tap the event and select **Add Reminder**\n4. It'll appear on your Home dashboard with a countdown!\n\nYou can tap any reminder on the Home screen to see full event details and remove it when done."

    if (q.contains("search") && (q.contains("how") || q.contains("use") || q.contains("app")))
        return "The app's **Search** lets you jump anywhere fast.\n\n• Tap the search icon on the top-right of the Home screen\n• Type to search events, screens, news, connections\n• Or use the quick access shortcuts (Messages, Events, Find Peers, News, Resources) without typing\n\nYou can search for your own saved reminders too!"

    if (q.contains("tutorial") || q.contains("how to use") || q.contains("tour") || q.contains("walkthrough"))
        return "There's a built-in app tour.\n\nTo launch it:\n• Go to **Profile** tab → **Settings** → **View Tutorial**\n\nThe tutorial walks you through every screen: Home, Messages, Events, News Feed, Resources, and Customization. Great if you're new or want a refresher!"

    if (q.contains("2fa") || q.contains("two factor") || q.contains("verification code") || q.contains("login code"))
        return "FBLA Hub uses **2-Factor Authentication (2FA)** for extra security.\n\nWhen you log in:\n1. Enter your email and password\n2. A verification code is sent to your email\n3. Enter the code to complete login\n\nIf you're having trouble with 2FA, check your spam folder for the code email, or try signing in again to request a new code."

    if (q.contains("log out") || q.contains("logout") || q.contains("sign out"))
        return "To log out:\n\n• Go to the **Profile** tab → **Settings** → scroll down to find **Sign Out**\n\nYour data is saved to your account, so everything will be there when you log back in!"

    if (q.contains("delete account") || q.contains("delete my account"))
        return "To delete your account:\n\n• Go to **Profile** tab → **Settings** → scroll to **Delete Account**\n• You'll need to type a confirmation to prevent accidental deletion\n\nThis is permanent and removes all your data, connections, and messages."

    if (q.contains("public profile") || q.contains("private profile") || q.contains("profile visibility"))
        return "You can control who sees your profile.\n\n• Go to **Profile** tab\n• Look for the **Public/Private** toggle\n• **Public** = other FBLA Hub members can find and view your profile\n• **Private** = only your connections can see your details\n\nYou can change this anytime!"

    // ── What is FBLA ───────────────────────────────────────────────────────
    if (q.contains("what is fbla") || q.contains("what's fbla") || q.contains("about fbla") || q.contains("explain fbla") || q.contains("tell me about fbla"))
        return "FBLA (Future Business Leaders of America) is the largest student business organization in the world with 230,000+ members.\n\nIt helps high school students:\n• Develop real business and leadership skills\n• Compete in 70+ events (tech, finance, speaking, marketing, and more)\n• Network with students and professionals nationwide\n• Prepare for college and careers\n\nMembers compete at Regional → State → National levels. It's one of the best things you can put on a college app or resume."

    if (q.contains("how do i join") || q.contains("how to join") || q.contains("sign up") || q.contains("membership") || q.contains("how do i become"))
        return "To join FBLA:\n\n1. Find out if your school has a chapter — ask a business teacher or counselor\n2. Talk to the chapter adviser to sign up\n3. Pay membership dues (usually \$20–\$30/year)\n4. If your school doesn't have a chapter, you can help start one!\n\nVisit fbla.org for official membership info."

    if (q.contains("fbla.org") || q.contains("official website") || q.contains("fbla website"))
        return "The official FBLA website is **fbla.org** — you'll find competition guidelines, event descriptions, membership info, study guides, and conference dates there."

    // ── Competitions Overview ──────────────────────────────────────────────
    if ((q.contains("competition") || q.contains("event") || q.contains("compete")) &&
        (q.contains("list") || q.contains("what") || q.contains("types") || q.contains("kind") || q.contains("all")))
        return "FBLA has 70+ competitive events. Here's a breakdown:\n\n**Business & Finance:** Accounting, Banking & Finance, Economics, Personal Finance\n**Tech:** Computer Science, Coding & Programming, Cybersecurity, Digital Storytelling\n**Speaking:** Public Speaking, Impromptu Speaking, Business Presentation\n**Written:** Business Plan, Business Report, Research Paper\n**Knowledge Tests:** Business Law, Intro to Business, Management\n**Team Events:** Business Ethics, Community Service, Marketing\n\nWhich event are you in? I can give you specific prep tips!"

    if ((q.contains("how") || q.contains("tips")) && (q.contains("prepare") || q.contains("study") || q.contains("practice")) && (q.contains("competition") || q.contains("event")))
        return "General competition prep tips:\n\n- Download the official study guide for your event from fbla.org\n- Take practice tests — past tests are on the FBLA website\n- Know the format (objective test, presentation, role play, written report, etc.)\n- Practice time management — presentations are usually 5–7 min\n- Dress professionally on competition day\n- For team events, rehearse with your partner/team\n\nWhat specific event are you prepping for? I'll give you targeted tips!"

    // ── Specific Events ────────────────────────────────────────────────────
    if (q.contains("public speaking"))
        return "**Public Speaking** — a prepared 5–7 min speech on a business topic.\n\n- Pick a topic you genuinely care about\n- Structure: strong intro → 3 main points → memorable conclusion\n- Practice out loud at least 10 times\n- Record yourself — catch filler words (um, like, uh)\n- Make eye contact, vary your pace and tone\n- No notes allowed during delivery!\n\nPro tip: judges love passion and a clear takeaway message."

    if (q.contains("business plan"))
        return "**Business Plan** — one of FBLA's most popular events. Create a full plan for a new or existing business.\n\nRequired sections:\n• Executive Summary\n• Company Description\n• Market Analysis\n• Products/Services\n• Marketing Strategy\n• Financial Projections\n• Management Team\n\n- Make financials realistic and detailed\n- Know your target market inside out\n- Prepare for tough judge Q&A\n- Use charts/graphs for financial data\n- Keep the executive summary tight — judges read it first"

    if (q.contains("computer science") || q.contains("coding") || q.contains("programming") && q.contains("fbla"))
        return "**Computer Science / Coding** events:\n\nStudy topics:\n• Data structures (arrays, linked lists, stacks, queues)\n• Algorithms (sorting, Big O notation)\n• OOP (classes, inheritance, polymorphism)\n• Basic SQL and databases\n• Networking fundamentals\n\n- Practice on LeetCode, HackerRank, or CodeSignal\n- Review past FBLA CS objective tests on fbla.org\n- Know Java or Python well\n- Study the official FBLA CS study guide"

    if (q.contains("accounting"))
        return "**Accounting** event prep:\n\nKey topics:\n• Journal entries & T-accounts\n• Income statement, balance sheet, trial balance\n• Payroll and depreciation\n• Bank reconciliation\n• Accounting equation: Assets = Liabilities + Equity\n\n- Know debits vs. credits cold\n- Practice objective tests from fbla.org\n- Review financial statement preparation\n- Study accounting vocabulary"

    if (q.contains("entrepreneurship") || q.contains("entrepreneur"))
        return "**Entrepreneurship** events test your ability to develop and pitch business ideas.\n\nKey concepts:\n• Identifying market opportunities\n• Value proposition and differentiators\n• Revenue models and pricing\n• SWOT analysis\n• Startup costs and break-even analysis\n• Elevator pitch\n\n- Study real startup stories\n- Practice your pitch out loud\n- Know your financials\n- Be ready for judge Q&A about your idea"

    if (q.contains("marketing"))
        return "**Marketing** events:\n\nKey topics:\n• The 4 P's: Product, Price, Place, Promotion\n• Target market & segmentation\n• Branding and positioning\n• Digital marketing (social, SEO, email)\n• Market research\n• Consumer behavior\n\n- Study real ad campaigns\n- Know current social media trends\n- Practice case study analysis\n- Understand data-driven marketing decisions"

    if (q.contains("cybersecurity") || q.contains("cyber security"))
        return "**Cybersecurity** event prep:\n\nKey topics:\n• Attack types (phishing, malware, DDoS, SQL injection)\n• Encryption and cryptography basics\n• CIA Triad: Confidentiality, Integrity, Availability\n• Firewalls, VPNs, intrusion detection\n• Ethical hacking basics\n• Security frameworks (NIST, ISO 27001)\n\n- CompTIA Security+ prep materials overlap a lot with FBLA Cybersecurity\n- Stay current on recent cyber incidents\n- Practice FBLA objective tests"

    if (q.contains("economics") || q.contains("econ"))
        return "**Economics** event prep:\n\nKey topics:\n• Supply and demand\n• GDP, inflation, unemployment\n• Monetary vs. fiscal policy\n• Market structures (monopoly, oligopoly, perfect competition)\n• International trade\n• Business cycles\n\n- AP Econ materials overlap well with FBLA Economics\n- Read economic news (WSJ, Bloomberg)\n- Know key indicators and what they signal\n- Practice FBLA objective tests on fbla.org"

    if (q.contains("personal finance") || q.contains("finance event"))
        return "**Personal Finance** event prep:\n\nKey topics:\n• Budgeting and savings strategies\n• Credit scores and debt management\n• Types of investments (stocks, bonds, mutual funds, ETFs)\n• Insurance basics (health, auto, life)\n• Taxes (W-2, 1040, deductions)\n• Retirement accounts (401k, IRA, Roth IRA)\n\n- Khan Academy has great free personal finance content\n- Practice FBLA objective tests\n- Understand compound interest — it shows up a lot"

    if (q.contains("business law") || q.contains("business ethics"))
        return "**Business Law / Business Ethics** prep:\n\nKey topics for Business Law:\n• Contract law (offer, acceptance, consideration)\n• Employment law basics\n• Intellectual property (patents, trademarks, copyrights)\n• Business structures (LLC, Corp, Partnership)\n• Consumer protection laws\n\nFor Business Ethics:\n• Ethical decision-making frameworks\n• Corporate social responsibility\n• Stakeholder vs. shareholder theory\n• Real-world case studies\n\n- Study the FBLA study guide for your specific event on fbla.org"

    if (q.contains("impromptu") || q.contains("impromptu speaking"))
        return "**Impromptu Speaking** — you get a random topic and have a short time to prepare a speech on the spot.\n\n- Practice the **PREP formula**: Point → Reason → Example → Point (restate)\n- Stay calm — judges expect you to think on your feet\n- Speak slowly and clearly, especially at the start\n- Pick a clear stance right away — don't waffle\n- Practice with random topics daily\n- 1–2 min is usually the target length\n\nThe more you practice, the faster your brain gets at organizing thoughts."

    if (q.contains("dress code") || q.contains("what to wear") || q.contains("dress for"))
        return "FBLA has a **professional dress code** for competitions.\n\n**Business Professional** (most competitions):\n• Suit or blazer with dress pants/skirt\n• Collared dress shirt or blouse\n• Dress shoes (closed toe)\n• No jeans, sneakers, or casual wear\n\n**Business Casual** (some chapter events):\n• Dress pants/slacks, khakis\n• Polo or button-down shirt\n• Clean, neat appearance\n\nWhen in doubt, overdress — judges do notice. Check your specific event guidelines on fbla.org for exact requirements."

    // ── Conferences ────────────────────────────────────────────────────────
    if (q.contains("nlc") || (q.contains("national") && q.contains("conference")))
        return "The **National Leadership Conference (NLC)** is the biggest FBLA event of the year.\n\n**Upcoming NLCs:**\n• **2026** — June 29–July 2 · San Antonio, TX\n• **2027** — June 23–26 · Columbus, OH\n• **2028** — June 23–26 · Anaheim, CA\n• **2029** — June 25–28 · Indianapolis, IN\n\n**About NLC:**\n• 12,000–16,000 students competing nationwide\n• Must qualify by placing at your State Leadership Conference\n• Compete against the best in the country\n• Keynote speakers, workshops, and officer elections\n\nCheck fbla.org for registration deadlines and full details!"

    if (q.contains("slc") || (q.contains("state") && q.contains("conference")))
        return "The **State Leadership Conference (SLC)** is your ticket to Nationals.\n\n• Held once a year — date and location vary by state\n• Qualify by placing at your Regional/District conference\n• Top finishers advance to the National Leadership Conference (NLC)\n\nCheck your state's FBLA website for exact dates, registration deadlines, and qualifying requirements."

    if (q.contains("regional") || q.contains("district") && q.contains("conference"))
        return "**Regional/District Conferences** are the first step in FBLA competition.\n\n• Usually held in fall or early spring\n• Compete against students in your area\n• Top finishers qualify for the State Leadership Conference (SLC)\n\nYour chapter adviser will have specific dates and details for your region."

    if (q.contains("how to qualify") || q.contains("qualify for") || q.contains("how do i advance"))
        return "FBLA competition has 3 levels — you advance by placing at each:\n\n1. **Regional/District** → place in top spots → advance to State\n2. **State Leadership Conference (SLC)** → place in top spots → advance to National\n3. **National Leadership Conference (NLC)** → compete with best in the country\n\nQualifying cutoffs vary by state and event. Ask your chapter adviser for your state's specific rules."

    if (q.contains("when") && (q.contains("deadline") || q.contains("register") || q.contains("date")))
        return "Dates and deadlines vary by state. General timeline:\n\n**Fall** — Local/chapter competitions\n**Winter/Spring** — Regional/District conferences\n**Spring** — State Leadership Conference (SLC)\n**June/July** — National Leadership Conference (NLC)\n\nTo find your state's exact dates:\n1. Go to fbla.org\n2. Navigate to your state chapter's page\n3. Look for the current year's conference calendar\n\nDon't miss registration deadlines — they sneak up fast."

    // ── Networking & Career ────────────────────────────────────────────────
    if (q.contains("network") || q.contains("networking"))
        return "Networking tips for FBLA students:\n\n**At conferences:**\n• Introduce yourself to competitors — they're future colleagues\n• Sit next to people you don't know at workshops\n• Exchange contact info and connect on LinkedIn\n• Talk to judges and advisers after events\n\n**Building your network:**\n• Create a LinkedIn profile now (even as a student)\n• Attend local business events\n• Do informational interviews with professionals\n\nNetworking = genuine connection, not just collecting contacts. Use the **Find Peers** feature in the app to connect with other FBLA students too!"

    if (q.contains("linkedin"))
        return "LinkedIn tips for FBLA students:\n\n- Professional headshot (no selfies or casual photos)\n- Write a bio mentioning you're an FBLA member\n- List your FBLA events, officer roles, and awards\n- Connect with classmates, teachers, and business contacts\n- Follow companies you're interested in\n- Post about your FBLA wins — it gets noticed\n- Engage thoughtfully with posts\n\nRecruiters genuinely look at student profiles. Having one already puts you ahead of most peers."

    if (q.contains("resume") || q.contains("résumé"))
        return "Resume tips for FBLA students:\n\n**Key sections:**\n• Contact info\n• Education (include GPA if 3.5+)\n• FBLA involvement (events competed, officer roles, awards placed)\n• Work / internship experience\n• Skills (software, languages)\n• Volunteer/community service\n\n- Keep it to ONE page\n- Use action verbs: led, managed, developed, increased, built\n- Quantify wins: \"Placed 2nd at State in Business Plan\"\n- Tailor it to each application\n- Proofread carefully — typos kill first impressions"

    if (q.contains("internship") || (q.contains("job") && !q.contains("interview")) || q.contains("career"))
        return "Career tips for FBLA students:\n\n**Getting internships/jobs:**\n• Use your FBLA network — advisers often know local businesses\n• Check LinkedIn, Indeed, and your school's job board\n• Apply early — summer internships often close Feb/March\n• Reference your FBLA competitions as real experience in interviews\n\n**Skills that stand out:**\n• Public speaking (massive advantage)\n• Microsoft Office / Google Workspace\n• Basic financial literacy\n• Leadership (officer roles count)\n\nFBLA experience genuinely impresses both colleges and employers."

    if (q.contains("interview") || q.contains("job interview"))
        return "Interview tips:\n\n- Research the organization beforehand\n- Prepare for common questions:\n   • \"Tell me about yourself\"\n   • \"What are your strengths/weaknesses?\"\n   • \"Why do you want this role?\"\n- Use the **STAR method** for behavioral questions:\n   Situation → Task → Action → Result\n- Dress professionally\n- Bring printed resume copies\n- Prepare 2–3 thoughtful questions to ask them\n- Send a thank-you email within 24 hours\n\nYour FBLA competition stories are great interview material."

    if (q.contains("college") || q.contains("application") || q.contains("college app"))
        return "FBLA is a strong college application asset.\n\n• List competitions and placements (especially state/national)\n• Include any officer roles with responsibilities\n• Write about specific skills you developed\n• Use FBLA experiences in your personal essay — real stories stand out\n• Awards and scholarships from FBLA look great on applications\n\nCollege admissions officers recognize FBLA as a serious commitment. A state or national placement is especially impressive."

    if (q.contains("scholarship") || q.contains("scholarships"))
        return "FBLA offers scholarships for members.\n\n• FBLA awards scholarships at the national level each year\n• Some state chapters have their own scholarship programs\n• Officer roles and competition placements strengthen scholarship applications\n\nVisit fbla.org → Scholarships section for current opportunities and deadlines. Also check if your state chapter has separate scholarships."

    // ── Officer Roles ──────────────────────────────────────────────────────
    if (q.contains("officer") || (q.contains("president") && q.contains("fbla")) || q.contains("leadership role"))
        return "FBLA chapter officer positions:\n\n**President** — Runs meetings, leads the chapter, represents at conferences\n**VP** — Assists president, often oversees committees\n**Treasurer** — Manages chapter budget and dues\n**Secretary** — Meeting minutes, communications\n**Reporter/Historian** — Documents chapter activities, social media\n\nTo become an officer:\n- Be consistently active and show up\n- Volunteer for events before you run\n- Build relationships with your adviser and members\n- Campaign with a clear, specific platform for the chapter"

    // ── Business Knowledge ────────────────────────────────────────────────
    if (q.contains("business tip") || q.contains("business skill") || q.contains("what skills"))
        return "Key business skills every FBLA student should build:\n\n**Communication** — Present and write professionally\n**Financial literacy** — Budgets, profit/loss, investing basics\n**Networking** — Build genuine relationships early\n**Time management** — Balance everything without burning out\n**Problem solving** — Practice case studies and real scenarios\n**Data analysis** — Basic Excel/spreadsheet skills go a long way\n**Digital skills** — Social media, basic coding, design tools\n\nFBLA competitions literally test all of these — you're building real, marketable skills."

    if (q.contains("swot") || q.contains("swot analysis"))
        return "A **SWOT Analysis** is a framework for evaluating a business or idea:\n\n**S — Strengths:** What does the business do well? What advantages does it have?\n**W — Weaknesses:** What are the limitations or areas to improve?\n**O — Opportunities:** External factors the business could capitalize on\n**T — Threats:** External factors that could hurt the business\n\nSWOT shows up in Business Plan, Entrepreneurship, and many other FBLA events. Always back up each point with specific evidence."

    if (q.contains("elevator pitch") || q.contains("pitch"))
        return "An **elevator pitch** is a 30–60 second summary of your business idea or yourself.\n\nStructure:\n1. Hook — grab attention immediately\n2. Problem — what problem are you solving?\n3. Solution — your product/service\n4. Value — why it's better/unique\n5. Ask — what do you want from the listener?\n\n- Practice until it feels natural, not memorized\n- Adapt it based on your audience\n- End with a clear call to action\n\nThis skill is tested in Entrepreneurship and Business Plan events."

    // ── Default Prompt Chips ───────────────────────────────────────────────
    if (q == "fbla competitive events" || q.contains("competitive events"))
        return "FBLA has 70+ competitive events in 6 categories:\n\n**Business & Finance**\nAccounting, Banking & Finance, Economics, Personal Finance, Business Law\n\n**Technology**\nComputer Science, Coding & Programming, Cybersecurity, Digital Storytelling, Website Design\n\n**Speaking & Presentation**\nPublic Speaking, Impromptu Speaking, Business Presentation, Introduction to Public Speaking\n\n**Written Events**\nBusiness Plan, Business Report, Research Paper, Future Business Educator\n\n**Objective Tests**\nIntro to Business, Management, Marketing, Business Ethics, Entrepreneurship\n\n**Team Events**\nBusiness Ethics Team, Community Service Project, Partnership with Business Project\n\nAsk me about any specific event for prep tips!"

    if (q == "networking tips" || q == "networking tip")
        return "Networking tips for FBLA students:\n\n**At conferences & competitions:**\n• Introduce yourself to competitors — they're future colleagues\n• Sit next to people you don't know at workshops\n• Talk to judges after events — they love engaged students\n• Exchange LinkedIn info, not just phone numbers\n\n**Online networking:**\n• Set up a LinkedIn profile now (even as a student)\n• Post about your FBLA wins — people notice\n• Connect with alumni from your school who went to college\n• Follow FBLA's official accounts and engage with posts\n\n**The key rule:**\nNetworking isn't about collecting contacts — it's about building real relationships. Follow up within 48 hours after meeting someone new.\n\nAlso use this app's **Find Peers** feature to connect with other FBLA members!"

    if (q == "nlc 2026 info" || q.contains("nlc 2026") || q.contains("national 2026"))
        return "The **2026 National Leadership Conference (NLC)** is FBLA's biggest annual event.\n\nLocation and exact dates are announced by FBLA — check fbla.org for the latest confirmed info since details update each year.\n\n**What to expect at NLC:**\n• 12,000+ students competing from across the country\n• Multiple rounds of competition in your event\n• Keynote speakers from the business world\n• Leadership workshops and sessions\n• Voting for national FBLA officers\n• Networking with the best FBLA students nationwide\n\n**How to qualify:**\n• Place in the top finishers at your State Leadership Conference (SLC)\n• Cutoffs vary by state and event\n\nCheck fbla.org → Conferences for confirmed 2026 NLC details."

    if (q == "business etiquette" || q.contains("business etiquette") || q.contains("professional etiquette") || q.contains("etiquette"))
        return "Business etiquette every FBLA student should know:\n\n**Professional Appearance**\n• Dress for the role you want, not the one you have\n• Keep clothes neat, pressed, and conservative for business settings\n• Minimal accessories and fragrance at interviews/conferences\n\n**Meetings & Interactions**\n• Firm handshake, eye contact, and a genuine smile\n• Arrive 5–10 minutes early (on time = late in business)\n• Put your phone away — full attention shows respect\n• Address people formally (Mr./Ms.) until told otherwise\n\n**Communication**\n• Use proper grammar in emails — no slang or emojis\n• Reply within 24 hours\n• Keep emails concise with a clear subject line\n• Always send a thank-you note after interviews\n\n**Dining Etiquette**\n• Wait for everyone to be served before eating\n• No phones at the table\n• Order mid-range items, never the most expensive\n• The person who invites pays — but always offer\n\nBusiness etiquette is tested in FBLA's Business Communications and related events."

    if (q == "leadership skills" || q.contains("leadership skill") || q.contains("how to be a leader") || q.contains("become a leader"))
        return "Leadership skills every FBLA student should build:\n\n**Communication**\nSpeak clearly, listen actively, and adapt your style to your audience. Practice in FBLA's public speaking events.\n\n**Decision Making**\nGather info, weigh options, commit to a choice, and learn from outcomes. Don't let perfect be the enemy of good.\n\n**Team Building**\nDelegate effectively, recognize others' strengths, and create an environment where people want to show up.\n\n**Time Management**\nPrioritize ruthlessly. Use tools like task lists, time blocking, and deadlines to stay on track.\n\n**Problem Solving**\nApproach problems with curiosity, not panic. Define the problem clearly before jumping to solutions.\n\n**Initiative**\nLeaders don't wait to be told — they see what needs doing and do it.\n\n**How to practice now:**\n• Run for a chapter officer position\n• Volunteer to lead a team event\n• Organize a chapter community service project\n• Mentor newer FBLA members\n\nLeadership shows up in almost every FBLA event — and every college application."

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
    return "I'm not sure about that one. I'm best at helping with:\n\n${suggestions.joinToString("\n") { "• $it" }}\n\nTry rephrasing or ask about one of the topics above!"
}

@Composable
fun GeminiChatOverlay(
    onDismiss: () -> Unit,
    primaryColor: Color,
    lightMode: Boolean = false
) {
    val textColor = if (lightMode) Color(0xFF1A1A2E) else Color.White
    val bgColor = if (lightMode) Color(0xFFF0F4FF) else Color(0xFF0A1628)
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
                                if (containsProfanity(query)) {
                                    messages = messages + GeminiMessage(query, true)
                                    inputText = ""
                                    messages = messages + GeminiMessage(
                                        "Please keep the conversation respectful. I'm here to help with FBLA questions!",
                                        false
                                    )
                                } else {
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

@Composable
fun QrCodeDialog(
    uid: String,
    name: String,
    chapter: String,
    primaryColor: Color,
    lightMode: Boolean = false,
    onDismiss: () -> Unit
) {
    val qrBitmap = remember(uid) { generateQrBitmap("leadlink://user/$uid") }
    val bgColor = if (lightMode) Color(0xFFF0F4FF) else Color(0xFF0A1628)
    val textColor = if (lightMode) Color(0xFF1A1A2E) else Color.White

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(bgColor)
                .padding(28.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("My QR Code", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = primaryColor, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(name, color = textColor, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                if (chapter.isNotBlank()) {
                    Text(chapter, color = textColor.copy(alpha = 0.6f), fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.height(20.dp))
                Box(
                    modifier = Modifier
                        .background(Color.White, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Image(
                        bitmap = qrBitmap.asImageBitmap(),
                        contentDescription = "QR Code",
                        modifier = Modifier.size(220.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Have a friend scan this to connect",
                    color = textColor.copy(alpha = 0.5f),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, primaryColor),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = primaryColor)
                ) {
                    Text("Close", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@android.annotation.SuppressLint("UnsafeOptInUsageError")
@Composable
fun QrScannerScreen(
    currentUid: String,
    currentProfile: UserProfile?,
    primaryColor: Color,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val scope = rememberCoroutineScope()

    var hasCameraPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        hasCameraPermission = it
    }
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    var scannedUid by remember { mutableStateOf("") }
    var scannedName by remember { mutableStateOf("") }
    var showConfirm by remember { mutableStateOf(false) }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(Unit) { onDispose { analysisExecutor.shutdown() } }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (hasCameraPermission) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        val scanner = BarcodeScanning.getClient(
                            BarcodeScannerOptions.Builder()
                                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                                .build()
                        )
                        var processed = false
                        val analysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                        analysis.setAnalyzer(analysisExecutor) { imageProxy ->
                            val mediaImage = imageProxy.image
                            if (mediaImage != null && !processed) {
                                val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                                scanner.process(inputImage)
                                    .addOnSuccessListener { barcodes ->
                                        for (barcode in barcodes) {
                                            val raw = barcode.rawValue ?: continue
                                            if (raw.startsWith("leadlink://user/") && !processed) {
                                                val uid = raw.removePrefix("leadlink://user/")
                                                if (uid.isNotEmpty() && uid != currentUid) {
                                                    processed = true
                                                    db.collection("users").document(uid).get()
                                                        .addOnSuccessListener { doc ->
                                                            if (doc.exists()) {
                                                                scannedUid = uid
                                                                scannedName = doc.getString("name") ?: "Unknown"
                                                                showConfirm = true
                                                            }
                                                        }
                                                }
                                            }
                                        }
                                    }
                                    .addOnCompleteListener { imageProxy.close() }
                            } else {
                                imageProxy.close()
                            }
                        }
                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                ctx as androidx.lifecycle.LifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview, analysis
                            )
                        } catch (e: Exception) { Log.e("QrScanner", "Camera bind failed", e) }
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.size(240.dp).border(3.dp, primaryColor, RoundedCornerShape(16.dp)))
                Text(
                    "Point at a FBLA Hub QR code",
                    color = Color.White,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 120.dp)
                        .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                    Icon(Icons.Default.CameraAlt, null, tint = Color.White, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Camera permission required to scan QR codes", color = Color.White, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                    ) { Text("Grant Permission", color = Color(0xFF0A1628), fontWeight = FontWeight.Bold) }
                }
            }
        }

        IconButton(
            onClick = onDismiss,
            modifier = Modifier.align(Alignment.TopStart).statusBarsPadding().padding(8.dp).pressScale()
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White, modifier = Modifier.size(28.dp))
        }

        if (showConfirm) {
            AlertDialog(
                onDismissRequest = { showConfirm = false; onDismiss() },
                title = { Text("Connect with $scannedName?") },
                text = { Text("Send a connection request to $scannedName?") },
                confirmButton = {
                    Button(
                        onClick = {
                            scope.launch {
                                try {
                                    val req = ConnectionRequest(
                                        id = "${currentUid}_${scannedUid}",
                                        fromUid = currentUid,
                                        fromName = currentProfile?.name ?: "",
                                        fromEmail = currentProfile?.email ?: "",
                                        fromSchool = currentProfile?.school ?: "",
                                        fromPfpUrl = currentProfile?.pfpUrl ?: "",
                                        toUid = scannedUid,
                                        toName = scannedName,
                                        status = "pending",
                                        timestamp = System.currentTimeMillis()
                                    )
                                    db.collection("connectionRequests").document(req.id).set(req).await()
                                    Toast.makeText(context, "Request sent to $scannedName!", Toast.LENGTH_SHORT).show()
                                    showConfirm = false
                                    onDismiss()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                    ) { Text("Send Request", color = Color(0xFF0A1628), fontWeight = FontWeight.Bold) }
                },
                dismissButton = {
                    TextButton(onClick = { showConfirm = false; onDismiss() }) { Text("Cancel") }
                }
            )
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

    val textColor = if (lightMode) Color(0xFF1A1A2E) else Color.White
    val textColorSecondary = if (lightMode) Color(0xFF1A1A2E).copy(alpha = 0.8f) else Color.White.copy(alpha = 0.9f)
    val textColorTertiary = if (lightMode) Color(0xFF1A1A2E).copy(alpha = 0.75f) else Color.White.copy(alpha = 0.85f)

    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var members by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var sentRequests by remember { mutableStateOf<Set<String>>(emptySet()) }
    var connectedUsers by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedRoleTab by remember { mutableStateOf(0) } //0=Students and 1=Teachers

    LaunchedEffect(Unit) {
        val currentUid = auth.currentUser?.uid
        if (currentUid != null) {
//Load users
            db.collection("users").addSnapshotListener { snapshot, _ ->
                members = snapshot?.toObjects(UserProfile::class.java)?.filter { it.uid != currentUid } ?: emptyList()
                isLoading = false
            }

//Load sent requests
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

    val filteredMembers = remember(searchQuery, members, selectedRoleTab) {
        val roleFilter = if (selectedRoleTab == 0) "student" else "teacher"
        val byRole = members.filter { it.role == roleFilter }
        if (searchQuery.isEmpty()) byRole
        else {
            val q = searchQuery.trimStart('@')
            byRole.filter {
                it.name.contains(q, ignoreCase = true) || it.username.contains(q, ignoreCase = true)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
// Background — white in light mode, dark blue in dark mode
        Box(modifier = Modifier.fillMaxSize().background(
            if (lightMode) Color(0xFFF0F4FF) else Color(0xFF0A1628)
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
                BackButton(onClick = onBack, tint = textColor)
                Spacer(modifier = Modifier.width(16.dp))
                Text("FIND PEERS", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black, fontSize = (MaterialTheme.typography.headlineSmall.fontSize.value * textSizeMultiplier).sp), color = textColor)
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search by name or @username…", color = textColorTertiary, style = TextStyle(fontSize = (14 * textSizeMultiplier).sp)) },
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

            // Student / Teacher tabs
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                listOf("Students", "Teachers").forEachIndexed { idx, label ->
                    val active = selectedRoleTab == idx
                    Box(
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                            .background(if (active) primaryColor else Color.Transparent)
                            .clickable { selectedRoleTab = idx }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            label,
                            color = if (active) Color.Black else textColorSecondary,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                            style = MaterialTheme.typography.labelLarge.copy(fontSize = (13 * textSizeMultiplier).sp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))

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
                                        if (member.username.isNotEmpty()) {
                                            Text("@${member.username}", color = primaryColor.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall.copy(fontSize = (12 * textSizeMultiplier).sp))
                                        }
                                        Text(member.school, color = primaryColor, style = MaterialTheme.typography.bodyMedium.copy(fontSize = (MaterialTheme.typography.bodyMedium.fontSize.value * textSizeMultiplier).sp))
                                    }
                                }

                                if (member.bio.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(text = member.bio, color = textColor.copy(alpha = 0.92f), style = MaterialTheme.typography.bodySmall.copy(fontSize = (MaterialTheme.typography.bodySmall.fontSize.value * textSizeMultiplier).sp))
                                }

                                if (member.reminders.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.EmojiEvents, null, tint = primaryColor, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text("COMPETING IN", color = primaryColor, style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp, fontWeight = FontWeight.Bold, fontSize = (MaterialTheme.typography.labelSmall.fontSize.value * textSizeMultiplier).sp))
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    member.reminders.forEach { reminder ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 4.dp)
                                                .background(primaryColor.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Icon(Icons.Default.EmojiEvents, null, tint = primaryColor, modifier = Modifier.size(13.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(reminder.name, color = textColor, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall.copy(fontSize = (12 * textSizeMultiplier).sp))
                                                if (reminder.date.isNotEmpty()) {
                                                    Text(reminder.date, color = textColorTertiary, style = MaterialTheme.typography.bodySmall.copy(fontSize = (10 * textSizeMultiplier).sp))
                                                }
                                            }
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
                                        modifier = Modifier.weight(1f).height(40.dp).pressScale(),
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
fun ConnectionRequestsScreen(onBack: () -> Unit, primaryColor: Color, bgOpacity: Float, isSimplifiedUI: Boolean, textSizeMultiplier: Float, lightMode: Boolean = false) {
// Dynamic text colors

    val textColor = if (lightMode) Color(0xFF1A1A2E) else Color.White
    val textColorSecondary = if (lightMode) Color(0xFF1A1A2E).copy(alpha = 0.8f) else Color.White.copy(alpha = 0.9f)
    val textColorTertiary = if (lightMode) Color(0xFF1A1A2E).copy(alpha = 0.75f) else Color.White.copy(alpha = 0.85f)

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
            if (lightMode) Color(0xFFF0F4FF) else Color(0xFF0A1628)
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
                BackButton(onClick = onBack, tint = textColor)
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
                                        modifier = Modifier.weight(1f).height(40.dp).pressScale(),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D52)),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
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
                                        modifier = Modifier.weight(1f).height(40.dp).pressScale(),
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

    val textColor = if (lightMode) Color(0xFF1A1A2E) else Color.White
    val textColorSecondary = if (lightMode) Color(0xFF1A1A2E).copy(alpha = 0.8f) else Color.White.copy(alpha = 0.9f)
    val textColorTertiary = if (lightMode) Color(0xFF1A1A2E).copy(alpha = 0.75f) else Color.White.copy(alpha = 0.85f)
    val cardBg = if (lightMode) Color(0xFFF0F4FF) else Color.White.copy(alpha = 0.08f)

    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var connections by remember { mutableStateOf<List<Connection>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showRemoveDialog by remember { mutableStateOf(false) }
    var connectionToRemove by remember { mutableStateOf<Connection?>(null) }
    var selectedConnection by remember { mutableStateOf<Connection?>(null) }
    var showReportDialog by remember { mutableStateOf(false) }
    var connectionToReport by remember { mutableStateOf<Connection?>(null) }

    LaunchedEffect(Unit) {
        val currentUid = auth.currentUser?.uid
        if (currentUid != null) {
            db.collection("connections").document(currentUid).addSnapshotListener { snapshot, _ ->
                val connectedUids = (snapshot?.get("connectedUids") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                if (connectedUids.isEmpty()) {
                    connections = emptyList()
                    isLoading = false
                } else {
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
                                linkedin = profile.linkedin,
                                username = profile.username,
                                bio = profile.bio,
                                hideConnections = profile.hideConnections,
                                allowPeerSharing = profile.allowPeerSharing,
                                role = profile.role
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
        Box(modifier = Modifier.fillMaxSize().background(if (lightMode) Color(0xFFF0F4FF) else Color(0xFF0A1628)))
        if (!lightMode) Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = bgOpacity)))

// ── Main connections list ──────────────────────────────
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp, end = 16.dp, bottom = 16.dp, start = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BackButton(onClick = onBack, tint = textColor)
                Spacer(modifier = Modifier.width(16.dp))
                Text("MY CONNECTIONS", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black, fontSize = (MaterialTheme.typography.headlineSmall.fontSize.value * textSizeMultiplier).sp), color = textColor)
                if (connections.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("(${connections.size})", color = primaryColor, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = (14 * textSizeMultiplier).sp))
                }
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
                LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item { Spacer(modifier = Modifier.height(0.dp)) }
                    itemsIndexed(connections) { _, connection ->
                        var showMenu by remember { mutableStateOf(false) }
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { selectedConnection = connection },
                            colors = CardDefaults.cardColors(containerColor = cardBg),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = if (lightMode) 2.dp else 0.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
// Avatar
                                Box(modifier = Modifier.size(56.dp).background(primaryColor.copy(alpha = 0.9f), CircleShape).clip(CircleShape), contentAlignment = Alignment.Center) {
                                    if (connection.pfpUrl.isNotEmpty()) {
                                        AsyncImage(model = connection.pfpUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                    } else {
                                        Text(connection.name.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Black, style = TextStyle(fontSize = (20 * textSizeMultiplier).sp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(14.dp))
// Info
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(connection.name, color = textColor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium.copy(fontSize = (MaterialTheme.typography.titleMedium.fontSize.value * textSizeMultiplier).sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    if (connection.username.isNotEmpty()) {
                                        Text("@${connection.username}", color = primaryColor, style = MaterialTheme.typography.bodySmall.copy(fontSize = (11 * textSizeMultiplier).sp), maxLines = 1)
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    val infoLine = buildString {
                                        if (connection.school.isNotEmpty()) append(connection.school)
                                        if (connection.year.isNotEmpty()) {
                                            if (isNotEmpty()) append("  ·  ")
                                            append("'${connection.year.takeLast(2)}")
                                        }
                                    }
                                    if (infoLine.isNotEmpty()) {
                                        Text(infoLine, color = textColorTertiary, style = MaterialTheme.typography.bodySmall.copy(fontSize = (11 * textSizeMultiplier).sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
// Three-dot options menu
                                Box {
                                    IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp).pressScale()) {
                                        Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = textColor.copy(alpha = 0.45f), modifier = Modifier.size(18.dp))
                                    }
                                    DropdownMenu(
                                        expanded = showMenu,
                                        onDismissRequest = { showMenu = false },
                                        modifier = Modifier.background(if (lightMode) Color.White else Color(0xFF1E2A3A))
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Remove Connection", color = Color(0xFFFF6B6B), style = MaterialTheme.typography.bodyMedium.copy(fontSize = (MaterialTheme.typography.bodyMedium.fontSize.value * textSizeMultiplier).sp)) },
                                            leadingIcon = { Icon(Icons.Default.PersonRemove, null, tint = Color(0xFFFF6B6B), modifier = Modifier.size(18.dp)) },
                                            onClick = {
                                                showMenu = false
                                                connectionToRemove = connection
                                                showRemoveDialog = true
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Report Connection", color = Color(0xFFCC3333), style = MaterialTheme.typography.bodyMedium.copy(fontSize = (MaterialTheme.typography.bodyMedium.fontSize.value * textSizeMultiplier).sp)) },
                                            leadingIcon = { Icon(Icons.Default.Flag, null, tint = Color(0xFFCC3333), modifier = Modifier.size(18.dp)) },
                                            onClick = {
                                                showMenu = false
                                                connectionToReport = connection
                                                showReportDialog = true
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }

// ── Detail overlay (shown when a card is tapped) ───────
        if (selectedConnection != null) {
            val conn = selectedConnection!!
            var connOfConnList by remember(conn.uid) { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
            var connOfConnCount by remember(conn.uid) { mutableStateOf(0) }
            var loadingConnections by remember(conn.uid) { mutableStateOf(true) }

            LaunchedEffect(conn.uid) {
                try {
                    val doc = db.collection("connections").document(conn.uid).get().await()
                    val uids = (doc.get("connectedUids") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                    connOfConnCount = uids.size
                    if (!conn.hideConnections && uids.isNotEmpty()) {
                        val profiles = db.collection("users").whereIn("uid", uids.take(10)).get().await()
                        connOfConnList = profiles.documents.mapNotNull { d ->
                            val n = d.getString("name") ?: ""
                            val p = d.getString("pfpUrl") ?: ""
                            if (n.isNotEmpty()) Pair(n, p) else null
                        }
                    }
                } catch (e: Exception) { }
                loadingConnections = false
            }

            Box(modifier = Modifier.fillMaxSize().background(if (lightMode) Color(0xFFF0F4FF) else Color(0xFF0A1628))) {
                if (!lightMode) Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = bgOpacity)))
                Column(modifier = Modifier.fillMaxSize()) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp, end = 16.dp, bottom = 8.dp, start = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BackButton(onClick = { selectedConnection = null }, tint = textColor)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("CONNECTION PROFILE", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black, fontSize = (MaterialTheme.typography.headlineSmall.fontSize.value * textSizeMultiplier).sp), color = textColor)
                    }
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        item {
                            Spacer(modifier = Modifier.height(20.dp))
// Large avatar
                            Box(modifier = Modifier.size(100.dp).background(primaryColor, CircleShape).clip(CircleShape), contentAlignment = Alignment.Center) {
                                if (conn.pfpUrl.isNotEmpty()) {
                                    AsyncImage(model = conn.pfpUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                } else {
                                    Text(conn.name.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Black, style = TextStyle(fontSize = (38 * textSizeMultiplier).sp))
                                }
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(conn.name, color = textColor, fontWeight = FontWeight.Black, style = MaterialTheme.typography.headlineSmall.copy(fontSize = (MaterialTheme.typography.headlineSmall.fontSize.value * textSizeMultiplier).sp), textAlign = TextAlign.Center)
                            if (conn.username.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("@${conn.username}", color = primaryColor, style = MaterialTheme.typography.bodyMedium.copy(fontSize = (13 * textSizeMultiplier).sp), textAlign = TextAlign.Center)
                            }
                            Spacer(modifier = Modifier.height(20.dp))

// Info card
                            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = cardBg), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(if (lightMode) 2.dp else 0.dp)) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    if (conn.school.isNotEmpty()) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.School, null, tint = primaryColor, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(conn.school, color = textColor, style = MaterialTheme.typography.bodyMedium.copy(fontSize = (MaterialTheme.typography.bodyMedium.fontSize.value * textSizeMultiplier).sp))
                                        }
                                    }
                                    if (conn.state.isNotEmpty()) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.LocationOn, null, tint = primaryColor, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(conn.state, color = textColor, style = MaterialTheme.typography.bodyMedium.copy(fontSize = (MaterialTheme.typography.bodyMedium.fontSize.value * textSizeMultiplier).sp))
                                        }
                                    }
                                    if (conn.year.isNotEmpty()) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.CalendarToday, null, tint = primaryColor, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text("Class of ${conn.year}", color = textColor, style = MaterialTheme.typography.bodyMedium.copy(fontSize = (MaterialTheme.typography.bodyMedium.fontSize.value * textSizeMultiplier).sp))
                                        }
                                    }
                                    if (conn.email.isNotEmpty()) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable {
                                            context.startActivity(Intent(Intent.ACTION_SENDTO).apply { data = Uri.parse("mailto:${conn.email}") })
                                        }) {
                                            Icon(Icons.Default.Email, null, tint = primaryColor, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(conn.email, color = primaryColor, style = MaterialTheme.typography.bodyMedium.copy(fontSize = (MaterialTheme.typography.bodyMedium.fontSize.value * textSizeMultiplier).sp))
                                        }
                                    }
                                }
                            }

// Bio card
                            if (conn.bio.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = cardBg), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(if (lightMode) 2.dp else 0.dp)) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text("BIO", color = primaryColor, style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold, fontSize = (MaterialTheme.typography.labelSmall.fontSize.value * textSizeMultiplier).sp))
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(conn.bio, color = textColorSecondary, style = MaterialTheme.typography.bodyMedium.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, fontSize = (MaterialTheme.typography.bodyMedium.fontSize.value * textSizeMultiplier).sp))
                                    }
                                }
                            }

// Social card
                            val hasSocial = conn.instagram.isNotEmpty() || conn.twitter.isNotEmpty() || conn.facebook.isNotEmpty() || conn.linkedin.isNotEmpty()
                            if (hasSocial) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = cardBg), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(if (lightMode) 2.dp else 0.dp)) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text("SOCIAL", color = primaryColor, style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold, fontSize = (MaterialTheme.typography.labelSmall.fontSize.value * textSizeMultiplier).sp))
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                            if (conn.instagram.isNotEmpty()) Box(modifier = Modifier.size(36.dp).clickable { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(normalizeSocialMediaUrl(conn.instagram)))) }) { SocialMediaLogo("Instagram", Modifier.size(36.dp)) }
                                            if (conn.twitter.isNotEmpty()) Box(modifier = Modifier.size(36.dp).clickable { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(normalizeSocialMediaUrl(conn.twitter)))) }) { SocialMediaLogo("X (Twitter)", Modifier.size(36.dp)) }
                                            if (conn.facebook.isNotEmpty()) Box(modifier = Modifier.size(36.dp).clickable { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(normalizeSocialMediaUrl(conn.facebook)))) }) { SocialMediaLogo("Facebook", Modifier.size(36.dp)) }
                                            if (conn.linkedin.isNotEmpty()) Box(modifier = Modifier.size(36.dp).clickable { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(normalizeSocialMediaUrl(conn.linkedin)))) }) { SocialMediaLogo("LinkedIn", Modifier.size(36.dp)) }
                                        }
                                    }
                                }
                            }

// Competitions card
                            if (conn.reminders.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = cardBg), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(if (lightMode) 2.dp else 0.dp)) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text("COMPETITIONS", color = primaryColor, style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold, fontSize = (MaterialTheme.typography.labelSmall.fontSize.value * textSizeMultiplier).sp))
                                        Spacer(modifier = Modifier.height(8.dp))
                                        conn.reminders.forEach { reminder ->
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 6.dp)) {
                                                Icon(Icons.Default.EmojiEvents, null, tint = primaryColor, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("${reminder.name} – ${reminder.date}", color = textColorSecondary, style = MaterialTheme.typography.bodySmall.copy(fontSize = (MaterialTheme.typography.bodySmall.fontSize.value * textSizeMultiplier).sp))
                                            }
                                        }
                                    }
                                }
                            }

// Connections card
                            Spacer(modifier = Modifier.height(12.dp))
                            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = cardBg), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(if (lightMode) 2.dp else 0.dp)) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("CONNECTIONS", color = primaryColor, style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold, fontSize = (MaterialTheme.typography.labelSmall.fontSize.value * textSizeMultiplier).sp))
                                        if (!conn.hideConnections && connOfConnCount > 0) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("($connOfConnCount)", color = textColorTertiary, style = MaterialTheme.typography.labelSmall.copy(fontSize = (MaterialTheme.typography.labelSmall.fontSize.value * textSizeMultiplier).sp))
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    if (conn.hideConnections) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Lock, null, tint = textColorTertiary, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("This user has hidden their connections", color = textColorTertiary, style = MaterialTheme.typography.bodySmall.copy(fontSize = (MaterialTheme.typography.bodySmall.fontSize.value * textSizeMultiplier).sp))
                                        }
                                    } else if (loadingConnections) {
                                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = primaryColor, strokeWidth = 2.dp)
                                    } else if (connOfConnList.isEmpty()) {
                                        Text("No connections yet", color = textColorTertiary, style = MaterialTheme.typography.bodySmall.copy(fontSize = (MaterialTheme.typography.bodySmall.fontSize.value * textSizeMultiplier).sp))
                                    } else {
                                        Row {
                                            connOfConnList.take(8).forEachIndexed { idx, (name, pfp) ->
                                                Box(
                                                    modifier = Modifier.padding(start = if (idx == 0) 0.dp else (-10).dp).size(36.dp).background(primaryColor, CircleShape).border(2.dp, if (lightMode) Color.White else Color(0xFF0A1628), CircleShape).clip(CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (pfp.isNotEmpty()) {
                                                        AsyncImage(model = pfp, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                                    } else {
                                                        Text(name.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold, style = TextStyle(fontSize = (12 * textSizeMultiplier).sp))
                                                    }
                                                }
                                            }
                                            if (connOfConnCount > 8) {
                                                Box(
                                                    modifier = Modifier.padding(start = (-10).dp).size(36.dp).background(textColor.copy(alpha = 0.15f), CircleShape).border(2.dp, if (lightMode) Color.White else Color(0xFF0A1628), CircleShape).clip(CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text("+${connOfConnCount - 8}", color = textColor, fontWeight = FontWeight.Bold, style = TextStyle(fontSize = (10 * textSizeMultiplier).sp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }

// Business card section
                            Spacer(modifier = Modifier.height(12.dp))
                            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = cardBg), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(if (lightMode) 2.dp else 0.dp)) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("BUSINESS CARD", color = primaryColor, style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold, fontSize = (MaterialTheme.typography.labelSmall.fontSize.value * textSizeMultiplier).sp))
                                    Spacer(modifier = Modifier.height(12.dp))
                                    BusinessCardComposable(
                                        name = conn.name,
                                        username = conn.username,
                                        chapter = conn.school,
                                        year = conn.year,
                                        state = conn.state,
                                        bio = conn.bio,
                                        primaryColor = primaryColor,
                                        modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(12.dp))
                                    )
                                    if (conn.allowPeerSharing) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            OutlinedButton(
                                                onClick = { shareMemberProfile(context, conn.name, conn.school, conn.year, conn.state, conn.bio) },
                                                modifier = Modifier.weight(1f).height(44.dp).pressScale(),
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = primaryColor),
                                                border = BorderStroke(1.5.dp, primaryColor.copy(alpha = 0.6f)),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Icon(Icons.Default.TextFields, null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Share Text", fontWeight = FontWeight.Bold, fontSize = (12 * textSizeMultiplier).sp)
                                            }
                                            Button(
                                                onClick = {
                                                    scope.launch(Dispatchers.Default) {
                                                        val bm = generateBusinessCardBitmap(conn.name, conn.username, conn.school, conn.year, conn.state, conn.bio, primaryColor.toArgb())
                                                        withContext(Dispatchers.Main) { shareBusinessCard(context, bm, conn.name) }
                                                    }
                                                },
                                                modifier = Modifier.weight(1f).height(44.dp).pressScale(),
                                                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Icon(Icons.Default.CreditCard, null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Share Card", fontWeight = FontWeight.Bold, fontSize = (12 * textSizeMultiplier).sp, color = Color.Black)
                                            }
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Lock, null, tint = textColorTertiary, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("This user has disabled peer sharing", color = textColorTertiary, style = MaterialTheme.typography.bodySmall.copy(fontSize = (MaterialTheme.typography.bodySmall.fontSize.value * textSizeMultiplier).sp))
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))
// Remove connection button
                            OutlinedButton(
                                onClick = { connectionToRemove = conn; showRemoveDialog = true },
                                modifier = Modifier.fillMaxWidth().height(50.dp).pressScale(),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF6B6B)),
                                border = BorderStroke(1.5.dp, Color(0xFFFF6B6B).copy(alpha = 0.7f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.PersonRemove, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("REMOVE CONNECTION", fontWeight = FontWeight.Bold, fontSize = (13 * textSizeMultiplier).sp, letterSpacing = 0.5.sp)
                            }
                            Spacer(modifier = Modifier.height(40.dp))
                        }
                    }
                }
            }
        }

// Confirmation dialog
        if (showRemoveDialog && connectionToRemove != null) {
            AlertDialog(
                onDismissRequest = { showRemoveDialog = false; connectionToRemove = null },
                title = { Text("Remove Connection?", color = Color.White, fontWeight = FontWeight.Bold) },
                text = { Text("Are you sure you want to remove ${connectionToRemove?.name} from your connections?", color = textColor.copy(alpha = 0.92f)) },
                confirmButton = {
                    TextButton(onClick = {
                        scope.launch {
                            try {
                                val currentUid = auth.currentUser?.uid ?: return@launch
                                val otherUid = connectionToRemove?.uid ?: return@launch
                                db.collection("connections").document(currentUid).update("connectedUids", FieldValue.arrayRemove(otherUid)).await()
                                db.collection("connections").document(otherUid).update("connectedUids", FieldValue.arrayRemove(currentUid)).await()
                                Toast.makeText(context, "Connection removed", Toast.LENGTH_SHORT).show()
                                if (selectedConnection?.uid == connectionToRemove?.uid) selectedConnection = null
                                showRemoveDialog = false
                                connectionToRemove = null
                            } catch (e: Exception) {
                                Log.e("RemoveConnection", "Error removing connection: ${e.message}", e)
                                Toast.makeText(context, "Failed to remove: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }) { Text("REMOVE", color = Color(0xFFFF6B6B), fontWeight = FontWeight.Bold) }
                },
                dismissButton = {
                    TextButton(onClick = { showRemoveDialog = false; connectionToRemove = null }) {
                        Text("CANCEL", color = Color.White.copy(alpha = 0.7f))
                    }
                },
                containerColor = Color(0xFF1A1A1A),
                titleContentColor = Color.White,
                textContentColor = Color.White
            )
        }

        if (showReportDialog && connectionToReport != null) {
            val reportReasons = listOf(
                "Harassment or bullying",
                "Inappropriate or explicit content",
                "Spam or scam",
                "Fake account or impersonation",
                "Other"
            )
            var selectedReason by remember(connectionToReport?.uid) { mutableStateOf(reportReasons.first()) }
            AlertDialog(
                onDismissRequest = { showReportDialog = false; connectionToReport = null },
                containerColor = if (lightMode) Color.White else Color(0xFF1A2740),
                title = { Text("Report ${connectionToReport?.name ?: "this user"}", color = textColor, fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text("Why are you reporting this account?", color = textColorSecondary, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(8.dp))
                        reportReasons.forEach { reason ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedReason = reason }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedReason == reason,
                                    onClick = { selectedReason = reason },
                                    colors = RadioButtonDefaults.colors(selectedColor = primaryColor, unselectedColor = textColorTertiary)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(reason, color = textColor, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val reported = connectionToReport
                        showReportDialog = false
                        connectionToReport = null
                        val reportedName = reported?.name ?: "Unknown"
                        val reporterEmail = auth.currentUser?.email ?: "Unknown"
                        val body = "I would like to report a connection on FBLA Hub.\n\n" +
                            "Reported user: $reportedName\n" +
                            "Reported username: @${reported?.username ?: ""}\n" +
                            "Reported user ID: ${reported?.uid ?: ""}\n" +
                            "Reason: $selectedReason\n" +
                            "Reported by: $reporterEmail\n\n" +
                            "Additional details (optional):\n"
                        sendSupportEmail(
                            context,
                            subject = "FBLA Hub - Report: $reportedName",
                            body = body
                        )
                    }) {
                        Text("REPORT", color = Color(0xFFCC3333), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showReportDialog = false; connectionToReport = null }) {
                        Text("CANCEL", color = textColorSecondary)
                    }
                }
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
    val textColor = if (lightMode) Color(0xFF1A1A2E) else Color.White
    val textColorSecondary = if (lightMode) Color(0xFF1A1A2E).copy(alpha = 0.8f) else Color.White.copy(alpha = 0.9f)
    val textColorTertiary = if (lightMode) Color(0xFF1A1A2E).copy(alpha = 0.75f) else Color.White.copy(alpha = 0.85f)

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
            containerColor = if (lightMode) Color(0xFFF0F4FF) else Color(0xFF1A1A1A)
        )
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { if (!isBusy) showCreateDialog = false },
            title = { Text("New classroom", color = textColor) },
            text = {
                OutlinedTextField(
                    value = newClassName,
                    onValueChange = { if (it.length <= 50) newClassName = it },
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
            containerColor = if (lightMode) Color(0xFFF0F4FF) else Color(0xFF1A1A1A)
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
            containerColor = if (lightMode) Color(0xFFF0F4FF) else Color(0xFF1A1A1A)
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize().background(if (lightMode) Color(0xFFF0F4FF) else Color(0xFF0A1628)))
        if (!lightMode) Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = bgOpacity)))

        Column(modifier = Modifier.fillMaxSize()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = if (lightMode) Color(0xFFF0F4FF) else Color(0xFF1A1A1A),
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp, start = 4.dp, end = 16.dp, bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BackButton(onClick = onBack, tint = textColor)
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
                            modifier = Modifier.fillMaxWidth().height(50.dp).pressScale(),
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
                        modifier = Modifier.fillMaxWidth().height(50.dp).pressScale(),
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
    val textColor = if (lightMode) Color(0xFF1A1A2E) else Color.White
    val textColorSecondary = if (lightMode) Color(0xFF1A1A2E).copy(alpha = 0.8f) else Color.White.copy(alpha = 0.9f)
    val cardBg = if (lightMode) Color(0xFFF0F4FF) else Color(0xFF1E2D45)
    val surfaceBg = if (lightMode) Color(0xFFF0F4FF) else Color(0xFF131F35)

    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentUserId = auth.currentUser?.uid ?: ""

    var selectedTab by remember { mutableStateOf(0) }
    var classroom by remember { mutableStateOf<Classroom?>(null) }
    var messages by remember { mutableStateOf<List<ClassroomMessage>>(emptyList()) }
    var posts by remember { mutableStateOf<List<ClassroomPost>>(emptyList()) }
    var assignments by remember { mutableStateOf<List<ClassroomAssignment>>(emptyList()) }
    var messageText by remember { mutableStateOf("") }
    var myName by remember { mutableStateOf("") }
    var myRole by remember { mutableStateOf("student") }
    val listState = rememberLazyListState()
    var messageToDelete by remember { mutableStateOf<ClassroomMessage?>(null) }

    // Stream state
    var showPostDialog by remember { mutableStateOf(false) }
    var newPostContent by remember { mutableStateOf("") }

    // Assignment state
    var showAssignmentDialog by remember { mutableStateOf(false) }
    var newAssignmentTitle by remember { mutableStateOf("") }
    var newAssignmentDesc by remember { mutableStateOf("") }
    var newAssignmentDueMs by remember { mutableStateOf(0L) }
    var newAssignmentDueDisplay by remember { mutableStateOf("") }
    var newAssignmentPoints by remember { mutableStateOf("100") }
    var selectedAssignment by remember { mutableStateOf<ClassroomAssignment?>(null) }
    var submissions by remember { mutableStateOf<List<AssignmentSubmission>>(emptyList()) }
    var mySubmission by remember { mutableStateOf<AssignmentSubmission?>(null) }
    var submissionText by remember { mutableStateOf("") }
    var isBusy by remember { mutableStateOf(false) }
    var gradingSubmission by remember { mutableStateOf<AssignmentSubmission?>(null) }
    var gradeInput by remember { mutableStateOf("") }
    var feedbackInput by remember { mutableStateOf("") }

    // Comments (replies on stream posts)
    var expandedPostId by remember { mutableStateOf<String?>(null) }
    var fullscreenImageUrl by remember { mutableStateOf<String?>(null) }
    var postComments by remember { mutableStateOf<List<ClassroomComment>>(emptyList()) }
    var commentText by remember { mutableStateOf("") }

    // Polls
    var classroomPolls by remember { mutableStateOf<List<Poll>>(emptyList()) }
    var showPollDialog by remember { mutableStateOf(false) }
    var pollQuestion by remember { mutableStateOf("") }
    var pollOptions by remember { mutableStateOf(listOf("", "")) }

    // File upload state
    var isUploadingPost by remember { mutableStateOf(false) }
    var isUploadingChat by remember { mutableStateOf(false) }
    var isUploadingSubmission by remember { mutableStateOf(false) }
    var pendingPostAttachmentUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var pendingPostAttachmentName by remember { mutableStateOf("") }
    var pendingPostAttachmentType by remember { mutableStateOf("") }
    var thumbnailBiasY by remember { mutableStateOf(0.5f) }
    var pendingChatAttachmentUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var pendingChatAttachmentName by remember { mutableStateOf("") }
    var pendingChatAttachmentType by remember { mutableStateOf("") }
    var pendingSubmissionAttachmentUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var pendingSubmissionAttachmentName by remember { mutableStateOf("") }

    val postFileLauncher = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val name = uri.lastPathSegment?.substringAfterLast('/') ?: "file"
            val mime = context.contentResolver.getType(uri) ?: ""
            pendingPostAttachmentUri = uri; pendingPostAttachmentName = name; pendingPostAttachmentType = if (mime.startsWith("image")) "image" else "file"
        }
    }
    val chatFileLauncher = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val name = uri.lastPathSegment?.substringAfterLast('/') ?: "file"
            val mime = context.contentResolver.getType(uri) ?: ""
            pendingChatAttachmentUri = uri; pendingChatAttachmentName = name; pendingChatAttachmentType = if (mime.startsWith("image")) "image" else "file"
        }
    }
    val submissionFileLauncher = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val name = uri.lastPathSegment?.substringAfterLast('/') ?: "file"
            pendingSubmissionAttachmentUri = uri; pendingSubmissionAttachmentName = name
        }
    }

    fun uploadAndRun(uri: android.net.Uri, path: String, onUrl: (String) -> Unit, onError: () -> Unit) {
        val ref = FirebaseStorage.getInstance().reference.child(path)
        val stream = try { context.contentResolver.openInputStream(uri) } catch (e: Exception) {
            android.util.Log.e("Upload", "Cannot open URI: ${e.message}"); onError(); return
        }
        if (stream == null) { android.util.Log.e("Upload", "Null stream for URI"); onError(); return }
        ref.putStream(stream).addOnSuccessListener {
            stream.close()
            ref.downloadUrl.addOnSuccessListener { url -> onUrl(url.toString()) }
                .addOnFailureListener { e -> android.util.Log.e("Upload", "downloadUrl failed: ${e.message}"); onError() }
        }.addOnFailureListener { e ->
            stream.close()
            android.util.Log.e("Upload", "putStream failed: ${e.message}")
            onError()
        }
    }

    LaunchedEffect(currentUserId) {
        if (currentUserId.isEmpty()) return@LaunchedEffect
        db.collection("users").document(currentUserId).addSnapshotListener { snap, _ ->
            val p = snap?.toObject(UserProfile::class.java)
            myName = p?.name ?: ""; myRole = p?.role ?: "student"
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
                messages = snap?.documents?.mapNotNull { it.toObject(ClassroomMessage::class.java)?.copy(id = it.id) } ?: emptyList()
            }
    }
    LaunchedEffect(classroomId) {
        if (classroomId.isEmpty()) return@LaunchedEffect
        db.collection("classrooms").document(classroomId).collection("posts")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) { android.util.Log.e("Classroom", "Posts listener error: ${err.message}"); return@addSnapshotListener }
                posts = snap?.documents?.mapNotNull { it.toObject(ClassroomPost::class.java)?.copy(id = it.id) } ?: emptyList()
            }
    }
    LaunchedEffect(classroomId) {
        if (classroomId.isEmpty()) return@LaunchedEffect
        db.collection("classrooms").document(classroomId).collection("polls")
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snap, _ ->
                classroomPolls = snap?.documents?.mapNotNull { it.toObject(Poll::class.java)?.copy(id = it.id) } ?: emptyList()
            }
    }
    LaunchedEffect(classroomId) {
        if (classroomId.isEmpty()) return@LaunchedEffect
        db.collection("classrooms").document(classroomId).collection("assignments")
            .orderBy("dueDateMs", com.google.firebase.firestore.Query.Direction.ASCENDING)
            .addSnapshotListener { snap, _ ->
                assignments = snap?.documents?.mapNotNull { it.toObject(ClassroomAssignment::class.java)?.copy(id = it.id) } ?: emptyList()
            }
    }
    LaunchedEffect(classroom, currentUserId) {
        val c = classroom
        if (c == null || currentUserId.isEmpty()) return@LaunchedEffect
        if (currentUserId !in c.memberIds) { Toast.makeText(context, "No access", Toast.LENGTH_SHORT).show(); onBack() }
    }
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty() && selectedTab == 2) listState.animateScrollToItem(messages.size - 1)
    }

    val isTeacherInRoom = classroom?.teacherId == currentUserId

    LaunchedEffect(selectedAssignment?.id) {
        val asgn = selectedAssignment ?: return@LaunchedEffect
        if (isTeacherInRoom) {
            db.collection("classrooms").document(classroomId).collection("assignments").document(asgn.id)
                .collection("submissions").addSnapshotListener { snap, _ ->
                    submissions = snap?.documents?.mapNotNull { it.toObject(AssignmentSubmission::class.java) } ?: emptyList()
                }
        } else {
            db.collection("classrooms").document(classroomId).collection("assignments").document(asgn.id)
                .collection("submissions").document(currentUserId).addSnapshotListener { snap, _ ->
                    mySubmission = if (snap?.exists() == true) snap.toObject(AssignmentSubmission::class.java) else null
                    if (mySubmission == null) submissionText = ""
                }
        }
    }

    LaunchedEffect(expandedPostId) {
        val pid = expandedPostId ?: run { postComments = emptyList(); return@LaunchedEffect }
        db.collection("classrooms").document(classroomId).collection("posts").document(pid)
            .collection("comments").orderBy("timestamp", com.google.firebase.firestore.Query.Direction.ASCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) return@addSnapshotListener
                postComments = snap?.documents?.mapNotNull { it.toObject(ClassroomComment::class.java)?.copy(id = it.id) } ?: emptyList()
            }
    }

    val sdf = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
    fun fmtDate(ms: Long) = if (ms > 0) sdf.format(java.util.Date(ms)) else "No due date"
    fun timeAgo(ms: Long): String {
        val d = System.currentTimeMillis() - ms
        return when { d < 60_000L -> "just now"; d < 3_600_000L -> "${d/60_000}m ago"; d < 86_400_000L -> "${d/3_600_000}h ago"; else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(java.util.Date(ms)) }
    }

    // --- Dialogs ---
    if (showPostDialog) {
        AlertDialog(
            onDismissRequest = { showPostDialog = false; newPostContent = "" },
            containerColor = if (lightMode) Color(0xFFF0F4FF) else Color(0xFF1A2840),
            title = { Text("Post Announcement", color = textColor, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = newPostContent, onValueChange = { if (it.length <= 2000) newPostContent = it },
                        placeholder = { Text("Share something with your class…", color = textColor.copy(alpha = 0.5f)) },
                        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 100.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = textColor, unfocusedTextColor = textColor, focusedBorderColor = primaryColor, unfocusedBorderColor = textColor.copy(alpha = 0.3f)), maxLines = 6
                    )
                    Spacer(Modifier.height(8.dp))
                    if (pendingPostAttachmentName.isNotEmpty() && pendingPostAttachmentType == "image") {
                        // Draggable thumbnail preview for images
                        val dragState = rememberDraggableState { delta ->
                            thumbnailBiasY = (thumbnailBiasY - delta / 600f).coerceIn(0f, 1f)
                        }
                        Box(modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(10.dp))
                            .draggable(state = dragState, orientation = Orientation.Vertical)
                        ) {
                            coil.compose.AsyncImage(
                                model = pendingPostAttachmentUri,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                alignment = androidx.compose.ui.BiasAlignment(0f, thumbnailBiasY)
                            )
                            Box(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color.Black.copy(alpha = 0.45f)).padding(vertical = 4.dp), contentAlignment = Alignment.Center) {
                                Text("Drag to reposition thumbnail", color = Color.White, style = MaterialTheme.typography.labelSmall)
                            }
                            IconButton(onClick = { pendingPostAttachmentUri = null; pendingPostAttachmentName = ""; pendingPostAttachmentType = ""; thumbnailBiasY = 0.5f }, modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(28.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)) {
                                Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                        }
                    } else if (pendingPostAttachmentName.isNotEmpty()) {
                        // Non-image file chip
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().background(primaryColor.copy(alpha = 0.1f), RoundedCornerShape(8.dp)).padding(8.dp)) {
                            Icon(Icons.Default.AttachFile, null, tint = primaryColor, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(pendingPostAttachmentName, color = textColor, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            IconButton(onClick = { pendingPostAttachmentUri = null; pendingPostAttachmentName = ""; pendingPostAttachmentType = "" }, modifier = Modifier.size(20.dp).pressScale()) {
                                Icon(Icons.Default.Close, null, tint = textColor.copy(alpha = 0.6f), modifier = Modifier.size(14.dp))
                            }
                        }
                    } else {
                        OutlinedButton(onClick = { postFileLauncher.launch("*/*") }, modifier = Modifier.fillMaxWidth().pressScale(), border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.5f))) {
                            Icon(Icons.Default.AttachFile, null, tint = primaryColor, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Attach File", color = primaryColor, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (newPostContent.isBlank() && pendingPostAttachmentUri == null) return@Button
                    val id = db.collection("classrooms").document(classroomId).collection("posts").document().id
                    val postContent = newPostContent.trim()
                    val attachUri = pendingPostAttachmentUri
                    val attachName = pendingPostAttachmentName
                    val attachType = pendingPostAttachmentType
                    val savedBias = thumbnailBiasY
                    isUploadingPost = true
                    fun savePost(url: String, name: String, type: String) {
                        db.collection("classrooms").document(classroomId).collection("posts").document(id).set(
                            ClassroomPost(id = id, authorId = currentUserId, authorName = myName, content = postContent, timestamp = System.currentTimeMillis(), attachmentUrl = url, attachmentName = name, attachmentType = type, thumbnailBiasY = savedBias)
                        ).addOnSuccessListener {
                            isUploadingPost = false; showPostDialog = false; newPostContent = ""; pendingPostAttachmentUri = null; pendingPostAttachmentName = ""; pendingPostAttachmentType = ""; thumbnailBiasY = 0.5f
                        }.addOnFailureListener { e ->
                            isUploadingPost = false; Toast.makeText(context, "Failed to post: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                    if (attachUri != null) {
                        uploadAndRun(attachUri, "classrooms/$classroomId/posts/$id/$attachName", { url -> savePost(url, attachName, attachType) }, { isUploadingPost = false; Toast.makeText(context, "Upload failed", Toast.LENGTH_SHORT).show() })
                    } else { savePost("", "", "") }
                }, enabled = !isUploadingPost, colors = ButtonDefaults.buttonColors(containerColor = primaryColor)) {
                    if (isUploadingPost) androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                    else Text("Post", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { showPostDialog = false; newPostContent = ""; pendingPostAttachmentUri = null; pendingPostAttachmentName = ""; pendingPostAttachmentType = ""; thumbnailBiasY = 0.5f }) { Text("Cancel") } }
        )
    }

    if (showPollDialog) {
        AlertDialog(
            onDismissRequest = { showPollDialog = false; pollQuestion = ""; pollOptions = listOf("", "") },
            containerColor = if (lightMode) Color(0xFFF0F4FF) else Color(0xFF1A2840),
            title = { Text("Create Poll", color = textColor, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val fieldColors = OutlinedTextFieldDefaults.colors(focusedTextColor = textColor, unfocusedTextColor = textColor, focusedBorderColor = primaryColor, unfocusedBorderColor = textColor.copy(alpha = 0.3f), focusedLabelColor = primaryColor, unfocusedLabelColor = textColor.copy(alpha = 0.6f))
                    OutlinedTextField(value = pollQuestion, onValueChange = { if (it.length <= 200) pollQuestion = it }, label = { Text("Question") }, modifier = Modifier.fillMaxWidth(), colors = fieldColors)
                    pollOptions.forEachIndexed { i, opt ->
                        OutlinedTextField(
                            value = opt,
                            onValueChange = { newVal -> if (newVal.length <= 100) pollOptions = pollOptions.toMutableList().also { it[i] = newVal } },
                            label = { Text("Option ${i + 1}") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = fieldColors,
                            trailingIcon = {
                                if (pollOptions.size > 2) IconButton(onClick = { pollOptions = pollOptions.toMutableList().also { list -> list.removeAt(i) } }) {
                                    Icon(Icons.Default.Close, null, tint = textColor.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                                }
                            }
                        )
                    }
                    if (pollOptions.size < 5) {
                        OutlinedButton(onClick = { pollOptions = pollOptions + "" }, modifier = Modifier.fillMaxWidth().pressScale(), border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.5f))) {
                            Icon(Icons.Default.Add, null, tint = primaryColor, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp))
                            Text("Add Option", color = primaryColor, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val validOptions = pollOptions.filter { it.isNotBlank() }
                    if (pollQuestion.isBlank() || validOptions.size < 2) { Toast.makeText(context, "Add a question and at least 2 options", Toast.LENGTH_SHORT).show(); return@Button }
                    val pollId = db.collection("classrooms").document(classroomId).collection("polls").document().id
                    db.collection("classrooms").document(classroomId).collection("polls").document(pollId)
                        .set(Poll(id = pollId, question = pollQuestion.trim(), options = validOptions, createdBy = currentUserId, createdByName = myName, createdAt = System.currentTimeMillis()))
                    showPollDialog = false; pollQuestion = ""; pollOptions = listOf("", "")
                }, colors = ButtonDefaults.buttonColors(containerColor = primaryColor)) { Text("Create", color = Color.Black, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { showPollDialog = false; pollQuestion = ""; pollOptions = listOf("", "") }) { Text("Cancel") } }
        )
    }

    if (showAssignmentDialog) {
        AlertDialog(
            onDismissRequest = { showAssignmentDialog = false },
            containerColor = if (lightMode) Color(0xFFF0F4FF) else Color(0xFF1A2840),
            title = { Text("Create Assignment", color = textColor, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val fieldColors = OutlinedTextFieldDefaults.colors(focusedTextColor = textColor, unfocusedTextColor = textColor, focusedBorderColor = primaryColor, unfocusedBorderColor = textColor.copy(alpha = 0.3f), focusedLabelColor = primaryColor, unfocusedLabelColor = textColor.copy(alpha = 0.6f))
                    OutlinedTextField(value = newAssignmentTitle, onValueChange = { if (it.length <= 100) newAssignmentTitle = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth(), colors = fieldColors)
                    OutlinedTextField(value = newAssignmentDesc, onValueChange = { if (it.length <= 1000) newAssignmentDesc = it }, label = { Text("Instructions (optional)") }, modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 80.dp), colors = fieldColors, maxLines = 4)
                    OutlinedTextField(value = newAssignmentPoints, onValueChange = { newAssignmentPoints = it.filter { c -> c.isDigit() }.take(5) }, label = { Text("Points") }, modifier = Modifier.fillMaxWidth(), colors = fieldColors)
                    OutlinedButton(
                        onClick = {
                            val cal = java.util.Calendar.getInstance()
                            android.app.DatePickerDialog(context, { _, y, m, d ->
                                val sel = java.util.Calendar.getInstance().apply { set(y, m, d, 23, 59, 0) }
                                newAssignmentDueMs = sel.timeInMillis
                                newAssignmentDueDisplay = "${m+1}/$d/$y"
                            }, cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH), cal.get(java.util.Calendar.DAY_OF_MONTH)).show()
                        },
                        modifier = Modifier.fillMaxWidth().pressScale(), border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.6f))
                    ) {
                        Icon(Icons.Default.CalendarToday, null, tint = primaryColor, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (newAssignmentDueDisplay.isEmpty()) "Set Due Date" else "Due: $newAssignmentDueDisplay", color = primaryColor)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (newAssignmentTitle.isBlank()) { Toast.makeText(context, "Add a title", Toast.LENGTH_SHORT).show(); return@Button }
                    val id = db.collection("classrooms").document(classroomId).collection("assignments").document().id
                    db.collection("classrooms").document(classroomId).collection("assignments").document(id).set(
                        ClassroomAssignment(id = id, title = newAssignmentTitle.trim(), description = newAssignmentDesc.trim(), dueDateMs = newAssignmentDueMs, points = newAssignmentPoints.toIntOrNull() ?: 100, createdAt = System.currentTimeMillis(), createdByName = myName)
                    ).addOnCompleteListener { showAssignmentDialog = false; newAssignmentTitle = ""; newAssignmentDesc = ""; newAssignmentDueMs = 0L; newAssignmentDueDisplay = ""; newAssignmentPoints = "100" }
                }, colors = ButtonDefaults.buttonColors(containerColor = primaryColor)) { Text("Create", color = Color.Black, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { showAssignmentDialog = false }) { Text("Cancel") } }
        )
    }

    gradingSubmission?.let { sub ->
        AlertDialog(
            onDismissRequest = { gradingSubmission = null; gradeInput = ""; feedbackInput = "" },
            containerColor = if (lightMode) Color(0xFFF0F4FF) else Color(0xFF1A2840),
            title = { Text("Grade: ${sub.studentName}", color = textColor, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val fieldColors = OutlinedTextFieldDefaults.colors(focusedTextColor = textColor, unfocusedTextColor = textColor, focusedBorderColor = primaryColor, unfocusedBorderColor = textColor.copy(alpha = 0.3f), focusedLabelColor = primaryColor, unfocusedLabelColor = textColor.copy(alpha = 0.6f))
                    Text("Submission:", color = textColor.copy(alpha = 0.6f), style = MaterialTheme.typography.labelMedium)
                    Text(sub.content, color = textColor, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(value = gradeInput, onValueChange = { gradeInput = it.filter { c -> c.isDigit() } }, label = { Text("Grade (0–${selectedAssignment?.points ?: 100})") }, modifier = Modifier.fillMaxWidth(), colors = fieldColors)
                    OutlinedTextField(value = feedbackInput, onValueChange = { if (it.length <= 500) feedbackInput = it }, label = { Text("Feedback (optional)") }, modifier = Modifier.fillMaxWidth(), colors = fieldColors)
                }
            },
            confirmButton = {
                Button(onClick = {
                    val asgn = selectedAssignment ?: return@Button
                    db.collection("classrooms").document(classroomId).collection("assignments").document(asgn.id)
                        .collection("submissions").document(sub.studentId)
                        .set(sub.copy(grade = gradeInput.toIntOrNull() ?: -1, feedback = feedbackInput.trim()))
                    gradingSubmission = null; gradeInput = ""; feedbackInput = ""
                }, colors = ButtonDefaults.buttonColors(containerColor = primaryColor)) { Text("Save Grade", color = Color.Black, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { gradingSubmission = null; gradeInput = ""; feedbackInput = "" }) { Text("Cancel") } }
        )
    }

    messageToDelete?.let { msgToDelete ->
        AlertDialog(
            onDismissRequest = { messageToDelete = null },
            title = { Text("Delete message?", color = textColor) },
            text = { Text("This message will be permanently removed.", color = textColorSecondary) },
            confirmButton = {
                Button(onClick = {
                    val msgId = msgToDelete.id; messageToDelete = null
                    db.collection("classrooms").document(classroomId).collection("messages").document(msgId).delete()
                }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))) { Text("Delete", color = Color.White, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { messageToDelete = null }) { Text("Cancel") } },
            containerColor = if (lightMode) Color(0xFFF0F4FF) else Color(0xFF1A1A1A)
        )
    }

    fullscreenImageUrl?.let { imgUrl ->
        androidx.compose.ui.window.Dialog(onDismissRequest = { fullscreenImageUrl = null }, properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black).pointerInput(Unit) { detectTapGestures { fullscreenImageUrl = null } }) {
                coil.compose.AsyncImage(model = imgUrl, contentDescription = null, modifier = Modifier.fillMaxWidth().align(Alignment.Center), contentScale = androidx.compose.ui.layout.ContentScale.Fit)
                IconButton(onClick = { fullscreenImageUrl = null }, modifier = Modifier.align(Alignment.TopEnd).padding(top = 40.dp, end = 16.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)) {
                    Icon(Icons.Default.Close, null, tint = Color.White)
                }
            }
        }
    }

    // --- Main Layout ---
    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize().background(if (lightMode) Color(0xFFF0F4FF) else Color(0xFF0A1628)))
        if (!lightMode) Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = bgOpacity)))

        Column(modifier = Modifier.fillMaxSize()) {
            // Header + Tab Bar
            Surface(modifier = Modifier.fillMaxWidth(), color = surfaceBg, shadowElevation = 4.dp) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 40.dp, start = 4.dp, end = 16.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BackButton(
                            onClick = {
                                if (selectedAssignment != null) { selectedAssignment = null; submissions = emptyList(); mySubmission = null }
                                else onBack()
                            },
                            tint = textColor
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                selectedAssignment?.title ?: (classroom?.name ?: "Classroom"),
                                color = textColor, fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium.copy(fontSize = (MaterialTheme.typography.titleMedium.fontSize.value * textSizeMultiplier).sp),
                                maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                if (selectedAssignment != null) (if (selectedAssignment!!.dueDateMs > 0) "Due ${fmtDate(selectedAssignment!!.dueDateMs)}" else "No due date")
                                else classroom?.teacherName ?: "",
                                color = primaryColor,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = (MaterialTheme.typography.bodySmall.fontSize.value * textSizeMultiplier).sp)
                            )
                        }
                    }
                    if (selectedAssignment == null) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            listOf("Stream", "Classwork", "Chat").forEachIndexed { idx, label ->
                                Box(
                                    modifier = Modifier.weight(1f).clickable { selectedTab = idx; selectedAssignment = null }.padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(label, color = if (selectedTab == idx) primaryColor else textColor.copy(alpha = 0.5f), fontWeight = if (selectedTab == idx) FontWeight.Bold else FontWeight.Normal, style = MaterialTheme.typography.bodyMedium.copy(fontSize = (13 * textSizeMultiplier).sp))
                                        if (selectedTab == idx) { Spacer(Modifier.height(3.dp)); Box(Modifier.width(32.dp).height(2.dp).background(primaryColor, RoundedCornerShape(1.dp))) }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            when {
                // Assignment detail
                selectedAssignment != null -> {
                    val asgn = selectedAssignment!!
                    LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
                        item {
                            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = cardBg), shape = RoundedCornerShape(16.dp)) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Assignment, null, tint = primaryColor, modifier = Modifier.size(22.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(asgn.title, color = textColor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium.copy(fontSize = (15 * textSizeMultiplier).sp))
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                        Text("${asgn.points} pts", color = primaryColor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                        if (asgn.dueDateMs > 0) Text("Due ${fmtDate(asgn.dueDateMs)}", color = textColorSecondary, style = MaterialTheme.typography.bodySmall)
                                    }
                                    if (asgn.description.isNotBlank()) {
                                        Spacer(Modifier.height(10.dp))
                                        Text(asgn.description, color = textColorSecondary, style = MaterialTheme.typography.bodyMedium.copy(fontSize = (13 * textSizeMultiplier).sp))
                                    }
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                        }
                        if (isTeacherInRoom) {
                            item {
                                Text("SUBMISSIONS (${submissions.size})", color = primaryColor, fontWeight = FontWeight.Black, style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.sp, fontSize = (11 * textSizeMultiplier).sp))
                                Spacer(Modifier.height(8.dp))
                                if (submissions.isEmpty()) Text("No submissions yet.", color = textColorSecondary)
                            }
                            items(submissions.size) { i ->
                                val sub = submissions[i]
                                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp), colors = CardDefaults.cardColors(containerColor = cardBg), shape = RoundedCornerShape(12.dp)) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Person, null, tint = primaryColor, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text(sub.studentName, color = textColor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                            Spacer(Modifier.weight(1f))
                                            if (sub.grade >= 0) Text("${sub.grade}/${asgn.points}", color = primaryColor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                            else Text("Ungraded", color = textColorSecondary.copy(alpha = 0.5f), style = MaterialTheme.typography.labelSmall)
                                        }
                                        Spacer(Modifier.height(6.dp))
                                        Text(sub.content, color = textColorSecondary, style = MaterialTheme.typography.bodySmall.copy(fontSize = (12 * textSizeMultiplier).sp), maxLines = 3, overflow = TextOverflow.Ellipsis)
                                        if (sub.feedback.isNotBlank()) { Spacer(Modifier.height(4.dp)); Text("Feedback: ${sub.feedback}", color = primaryColor.copy(alpha = 0.8f), style = MaterialTheme.typography.labelSmall) }
                                        Spacer(Modifier.height(8.dp))
                                        OutlinedButton(onClick = { gradingSubmission = sub; gradeInput = if (sub.grade >= 0) sub.grade.toString() else ""; feedbackInput = sub.feedback }, modifier = Modifier.fillMaxWidth().height(36.dp).pressScale(), border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.5f)), shape = RoundedCornerShape(8.dp)) {
                                            Text(if (sub.grade >= 0) "Edit Grade" else "Grade", color = primaryColor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                            }
                        } else {
                            item {
                                Text("YOUR WORK", color = primaryColor, fontWeight = FontWeight.Black, style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.sp, fontSize = (11 * textSizeMultiplier).sp))
                                Spacer(Modifier.height(8.dp))
                                if (mySubmission != null) {
                                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = cardBg), shape = RoundedCornerShape(12.dp)) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF2ECC71), modifier = Modifier.size(18.dp))
                                                Spacer(Modifier.width(6.dp))
                                                Text("Turned in", color = Color(0xFF2ECC71), fontWeight = FontWeight.Bold)
                                                Spacer(Modifier.weight(1f))
                                                if ((mySubmission?.grade ?: -1) >= 0) Text("${mySubmission?.grade}/${asgn.points}", color = primaryColor, fontWeight = FontWeight.Bold)
                                            }
                                            Spacer(Modifier.height(8.dp))
                                            if ((mySubmission?.content ?: "").isNotBlank()) Text(mySubmission?.content ?: "", color = textColorSecondary, style = MaterialTheme.typography.bodySmall)
                                            if ((mySubmission?.attachmentUrl ?: "").isNotEmpty()) {
                                                Spacer(Modifier.height(6.dp))
                                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.background(primaryColor.copy(alpha = 0.08f), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                                                    Icon(Icons.Default.AttachFile, null, tint = primaryColor, modifier = Modifier.size(14.dp))
                                                    Spacer(Modifier.width(4.dp))
                                                    Text(mySubmission?.attachmentName ?: "File", color = primaryColor, style = MaterialTheme.typography.labelSmall)
                                                }
                                            }
                                            if ((mySubmission?.feedback ?: "").isNotBlank()) { Spacer(Modifier.height(6.dp)); Text("Feedback: ${mySubmission?.feedback}", color = primaryColor.copy(alpha = 0.8f), style = MaterialTheme.typography.labelSmall) }
                                            Spacer(Modifier.height(10.dp))
                                            OutlinedButton(onClick = {
                                                db.collection("classrooms").document(classroomId).collection("assignments").document(asgn.id).collection("submissions").document(currentUserId).delete()
                                                mySubmission = null; submissionText = ""
                                            }, modifier = Modifier.fillMaxWidth().height(36.dp), border = BorderStroke(1.dp, Color(0xFFE74C3C).copy(alpha = 0.6f)), shape = RoundedCornerShape(8.dp)) {
                                                Text("Unsubmit", color = Color(0xFFE74C3C), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                            }
                                        }
                                    }
                                } else {
                                    OutlinedTextField(
                                        value = submissionText, onValueChange = { if (it.length <= 5000) submissionText = it },
                                        placeholder = { Text("Type your answer or paste a link…", color = textColor.copy(alpha = 0.45f)) },
                                        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 100.dp),
                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = textColor, unfocusedTextColor = textColor, focusedBorderColor = primaryColor, unfocusedBorderColor = textColor.copy(alpha = 0.3f), focusedContainerColor = cardBg, unfocusedContainerColor = cardBg),
                                        shape = RoundedCornerShape(12.dp), maxLines = 8
                                    )
                                    Spacer(Modifier.height(10.dp))
                                    if (pendingSubmissionAttachmentName.isNotEmpty()) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().background(primaryColor.copy(alpha = 0.1f), RoundedCornerShape(8.dp)).padding(10.dp)) {
                                            Icon(Icons.Default.AttachFile, null, tint = primaryColor, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text(pendingSubmissionAttachmentName, color = textColor, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            IconButton(onClick = { pendingSubmissionAttachmentUri = null; pendingSubmissionAttachmentName = "" }, modifier = Modifier.size(20.dp).pressScale()) {
                                                Icon(Icons.Default.Close, null, tint = textColor.copy(alpha = 0.6f), modifier = Modifier.size(14.dp))
                                            }
                                        }
                                    } else {
                                        OutlinedButton(onClick = { submissionFileLauncher.launch("*/*") }, modifier = Modifier.fillMaxWidth().pressScale(), border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.5f)), shape = RoundedCornerShape(10.dp)) {
                                            Icon(Icons.Default.AttachFile, null, tint = primaryColor, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text("Attach File", color = primaryColor, style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                    Spacer(Modifier.height(12.dp))
                                    Button(
                                        onClick = {
                                            if (submissionText.isBlank() && pendingSubmissionAttachmentUri == null) { Toast.makeText(context, "Add an answer or attach a file", Toast.LENGTH_SHORT).show(); return@Button }
                                            isBusy = true
                                            val attachUri = pendingSubmissionAttachmentUri
                                            val attachName = pendingSubmissionAttachmentName
                                            fun saveSubmission(url: String, name: String) {
                                                db.collection("classrooms").document(classroomId).collection("assignments").document(asgn.id).collection("submissions").document(currentUserId)
                                                    .set(AssignmentSubmission(studentId = currentUserId, studentName = myName, submittedAt = System.currentTimeMillis(), content = submissionText.trim(), attachmentUrl = url, attachmentName = name))
                                                    .addOnCompleteListener { isBusy = false; pendingSubmissionAttachmentUri = null; pendingSubmissionAttachmentName = "" }
                                            }
                                            if (attachUri != null) {
                                                uploadAndRun(attachUri, "classrooms/$classroomId/submissions/${currentUserId}_${asgn.id}/$attachName", { url -> saveSubmission(url, attachName) }, { isBusy = false; Toast.makeText(context, "Upload failed", Toast.LENGTH_SHORT).show() })
                                            } else { saveSubmission("", "") }
                                        },
                                        enabled = !isBusy && (submissionText.isNotBlank() || pendingSubmissionAttachmentUri != null),
                                        modifier = Modifier.fillMaxWidth().height(48.dp).pressScale(),
                                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor), shape = RoundedCornerShape(12.dp)
                                    ) {
                                        if (isBusy) androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.Black, strokeWidth = 2.dp)
                                        else Text("Turn In", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = (14 * textSizeMultiplier).sp)
                                    }
                                }
                                Spacer(Modifier.height(32.dp))
                            }
                        }
                    }
                }

                // Stream tab
                selectedTab == 0 -> {
                    LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
                        if (isTeacherInRoom) {
                            item {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(onClick = { showPostDialog = true }, modifier = Modifier.weight(1f).height(48.dp).pressScale(), colors = ButtonDefaults.buttonColors(containerColor = primaryColor), shape = RoundedCornerShape(12.dp)) {
                                        Icon(Icons.Default.Add, null, tint = Color.Black, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp))
                                        Text("Announce", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = (12 * textSizeMultiplier).sp)
                                    }
                                    Button(onClick = { showPollDialog = true }, modifier = Modifier.weight(1f).height(48.dp).pressScale(), colors = ButtonDefaults.buttonColors(containerColor = primaryColor.copy(alpha = 0.15f)), shape = RoundedCornerShape(12.dp)) {
                                        Icon(Icons.Default.Poll, null, tint = primaryColor, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp))
                                        Text("Poll", color = primaryColor, fontWeight = FontWeight.Bold, fontSize = (12 * textSizeMultiplier).sp)
                                    }
                                }
                                Spacer(Modifier.height(16.dp))
                            }
                        }
                        if (classroomPolls.isNotEmpty()) {
                            items(classroomPolls.size) { i ->
                                val poll = classroomPolls[i]
                                PollCard(
                                    poll = poll,
                                    currentUserId = currentUserId,
                                    onVote = { optionIndex ->
                                        db.collection("classrooms").document(classroomId).collection("polls").document(poll.id)
                                            .update(mapOf("votes.$currentUserId" to optionIndex))
                                    },
                                    primaryColor = primaryColor,
                                    lightMode = lightMode,
                                    textSizeMultiplier = textSizeMultiplier,
                                    modifier = Modifier.padding(bottom = 12.dp).pressScale()
                                )
                            }
                        }
                        if (posts.isEmpty() && classroomPolls.isEmpty()) {
                            item {
                                Column(modifier = Modifier.fillMaxWidth().padding(top = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Notifications, null, tint = textColor.copy(alpha = 0.35f), modifier = Modifier.size(60.dp))
                                    Spacer(Modifier.height(12.dp))
                                    Text(if (isTeacherInRoom) "Post your first announcement above!" else "No announcements yet", color = textColorSecondary, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        if (posts.isNotEmpty()) {
                            items(posts.size) { i ->
                                val post = posts[i]
                                val isExpanded = expandedPostId == post.id
                                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), colors = CardDefaults.cardColors(containerColor = cardBg), shape = RoundedCornerShape(16.dp)) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(modifier = Modifier.size(36.dp).background(primaryColor.copy(alpha = 0.15f), CircleShape), contentAlignment = Alignment.Center) {
                                                Text(post.authorName.firstOrNull()?.toString() ?: "T", color = primaryColor, fontWeight = FontWeight.Bold)
                                            }
                                            Spacer(Modifier.width(10.dp))
                                            Column {
                                                Text(post.authorName, color = textColor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                                Text(timeAgo(post.timestamp), color = textColorSecondary.copy(alpha = 0.55f), style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                        if (post.content.isNotBlank()) {
                                            Spacer(Modifier.height(12.dp))
                                            Text(post.content, color = textColor, style = MaterialTheme.typography.bodyMedium.copy(fontSize = (13 * textSizeMultiplier).sp))
                                        }
                                        if (post.attachmentUrl.isNotEmpty()) {
                                            Spacer(Modifier.height(10.dp))
                                            if (post.attachmentType == "image") {
                                                coil.compose.AsyncImage(model = post.attachmentUrl, contentDescription = null, modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(10.dp)).pointerInput(Unit) { detectTapGestures { fullscreenImageUrl = post.attachmentUrl } }, contentScale = androidx.compose.ui.layout.ContentScale.Crop, alignment = androidx.compose.ui.BiasAlignment(0f, post.thumbnailBiasY))
                                            } else {
                                                Row(modifier = Modifier.fillMaxWidth().background(primaryColor.copy(alpha = 0.08f), RoundedCornerShape(8.dp)).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.AttachFile, null, tint = primaryColor, modifier = Modifier.size(18.dp))
                                                    Spacer(Modifier.width(8.dp))
                                                    Text(post.attachmentName.ifBlank { "Attachment" }, color = primaryColor, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                }
                                            }
                                        }
                                        HorizontalDivider(modifier = Modifier.padding(top = 12.dp, bottom = 4.dp), color = textColor.copy(alpha = 0.08f))
                                        TextButton(onClick = { expandedPostId = if (isExpanded) null else post.id; commentText = "" }, modifier = Modifier.fillMaxWidth().height(32.dp)) {
                                            Icon(Icons.Default.Comment, null, tint = primaryColor, modifier = Modifier.size(14.dp))
                                            Spacer(Modifier.width(5.dp))
                                            Text(if (isExpanded) "Hide replies" else "Reply", color = primaryColor, style = MaterialTheme.typography.labelMedium)
                                        }
                                        if (isExpanded) {
                                            Spacer(Modifier.height(4.dp))
                                            if (postComments.isEmpty()) {
                                                Text("No replies yet", color = textColorSecondary.copy(alpha = 0.45f), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(start = 4.dp, bottom = 6.dp))
                                            } else {
                                                postComments.forEach { comment ->
                                                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
                                                        Box(modifier = Modifier.size(26.dp).background(primaryColor.copy(alpha = 0.15f), CircleShape), contentAlignment = Alignment.Center) {
                                                            Text(comment.authorName.firstOrNull()?.toString() ?: "?", color = primaryColor, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                                        }
                                                        Spacer(Modifier.width(8.dp))
                                                        Column {
                                                            Text(comment.authorName, color = textColor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                                                            Text(comment.content, color = textColorSecondary, style = MaterialTheme.typography.bodySmall.copy(fontSize = (12 * textSizeMultiplier).sp))
                                                        }
                                                    }
                                                }
                                            }
                                            Spacer(Modifier.height(6.dp))
                                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                                OutlinedTextField(
                                                    value = commentText, onValueChange = { if (it.length <= 500) commentText = it },
                                                    placeholder = { Text("Add a reply…", color = textColor.copy(alpha = 0.4f), style = MaterialTheme.typography.bodySmall) },
                                                    modifier = Modifier.weight(1f),
                                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = textColor, unfocusedTextColor = textColor, focusedBorderColor = primaryColor, unfocusedBorderColor = textColor.copy(alpha = 0.25f), focusedContainerColor = if (lightMode) Color.White else Color(0xFF1A2840), unfocusedContainerColor = if (lightMode) Color.White else Color(0xFF1A2840)),
                                                    shape = RoundedCornerShape(20.dp), maxLines = 2,
                                                    textStyle = MaterialTheme.typography.bodySmall
                                                )
                                                Spacer(Modifier.width(6.dp))
                                                IconButton(
                                                    onClick = {
                                                        if (commentText.isBlank()) return@IconButton
                                                        val cid = db.collection("classrooms").document(classroomId).collection("posts").document(post.id).collection("comments").document().id
                                                        val text = commentText.trim(); commentText = ""
                                                        db.collection("classrooms").document(classroomId).collection("posts").document(post.id).collection("comments").document(cid)
                                                            .set(ClassroomComment(id = cid, authorId = currentUserId, authorName = myName, content = text, timestamp = System.currentTimeMillis()))
                                                    },
                                                    modifier = Modifier.size(36.dp).background(if (commentText.isNotBlank()) primaryColor else Color.Transparent, CircleShape)
                                                ) { Icon(Icons.AutoMirrored.Filled.Send, "Reply", tint = if (commentText.isNotBlank()) Color.Black else textColor.copy(alpha = 0.4f), modifier = Modifier.size(16.dp)) }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Classwork tab
                selectedTab == 1 -> {
                    LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
                        if (isTeacherInRoom) {
                            item {
                                Button(onClick = { showAssignmentDialog = true }, modifier = Modifier.fillMaxWidth().height(48.dp).pressScale(), colors = ButtonDefaults.buttonColors(containerColor = primaryColor), shape = RoundedCornerShape(12.dp)) {
                                    Icon(Icons.Default.Add, null, tint = Color.Black, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                                    Text("Create Assignment", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = (13 * textSizeMultiplier).sp)
                                }
                                Spacer(Modifier.height(16.dp))
                            }
                        }
                        if (assignments.isEmpty()) {
                            item {
                                Column(modifier = Modifier.fillMaxWidth().padding(top = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Assignment, null, tint = textColor.copy(alpha = 0.35f), modifier = Modifier.size(60.dp))
                                    Spacer(Modifier.height(12.dp))
                                    Text(if (isTeacherInRoom) "Create your first assignment above!" else "No assignments yet", color = textColorSecondary, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            items(assignments.size) { i ->
                                val asgn = assignments[i]
                                val isPastDue = asgn.dueDateMs > 0 && asgn.dueDateMs < System.currentTimeMillis()
                                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp).pressScale().clickable { selectedAssignment = asgn; submissions = emptyList(); mySubmission = null; submissionText = "" }, colors = CardDefaults.cardColors(containerColor = cardBg), shape = RoundedCornerShape(14.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(40.dp).background(primaryColor.copy(alpha = 0.15f), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.Assignment, null, tint = primaryColor, modifier = Modifier.size(22.dp))
                                        }
                                        Spacer(Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(asgn.title, color = textColor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium.copy(fontSize = (14 * textSizeMultiplier).sp))
                                            Spacer(Modifier.height(3.dp))
                                            Text(
                                                buildString { append("${asgn.points} pts"); if (asgn.dueDateMs > 0) { append(" • "); if (isPastDue) append("Past due ") else append("Due "); append(fmtDate(asgn.dueDateMs)) } },
                                                color = if (isPastDue) Color(0xFFE74C3C) else textColorSecondary,
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = (12 * textSizeMultiplier).sp)
                                            )
                                        }
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = textColor.copy(alpha = 0.35f), modifier = Modifier.size(18.dp).graphicsLayer { rotationZ = 180f })
                                    }
                                }
                            }
                        }
                    }
                }

                // Chat tab
                else -> {
                    Box(modifier = Modifier.weight(1f)) {
                        if (messages.isEmpty()) {
                            Column(modifier = Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                Icon(Icons.Default.Chat, null, tint = textColor.copy(alpha = 0.5f), modifier = Modifier.size(64.dp))
                                Spacer(Modifier.height(12.dp))
                                Text("No messages yet", color = textColorSecondary, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
                                items(messages.size) { index ->
                                    val message = messages[index]
                                    val isMe = message.senderId == currentUserId
                                    val canDelete = isTeacherInRoom || isMe
                                    val bubbleColor = if (isMe) primaryColor else (if (lightMode) Color(0xFFF0F4FF) else Color(0xFF1E2D45))
                                    val textCol = if (isMe) Color.Black else textColor
                                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start) {
                                        Column(horizontalAlignment = if (isMe) Alignment.End else Alignment.Start) {
                                            if (!isMe && message.senderName.isNotEmpty()) Text(message.senderName, color = textColorSecondary, style = MaterialTheme.typography.labelSmall.copy(fontSize = (10 * textSizeMultiplier).sp), modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                                            Card(
                                                colors = CardDefaults.cardColors(containerColor = bubbleColor),
                                                shape = RoundedCornerShape(16.dp),
                                                modifier = Modifier.pointerInput(canDelete) { if (canDelete) detectTapGestures(onLongPress = { messageToDelete = message }) }
                                            ) {
                                                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                                                    if (message.attachmentUrl.isNotEmpty()) {
                                                        if (message.attachmentType == "image") {
                                                            coil.compose.AsyncImage(model = message.attachmentUrl, contentDescription = null, modifier = Modifier.widthIn(max = 220.dp).clip(RoundedCornerShape(8.dp)), contentScale = androidx.compose.ui.layout.ContentScale.FillWidth)
                                                            if (message.message.isNotBlank()) Spacer(Modifier.height(6.dp))
                                                        } else {
                                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.background(textCol.copy(alpha = 0.08f), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                                                                Icon(Icons.Default.AttachFile, null, tint = textCol, modifier = Modifier.size(14.dp))
                                                                Spacer(Modifier.width(4.dp))
                                                                Text(message.attachmentName.ifBlank { "File" }, color = textCol, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                            }
                                                            if (message.message.isNotBlank()) Spacer(Modifier.height(6.dp))
                                                        }
                                                    }
                                                    if (message.message.isNotBlank()) Text(message.message, color = textCol, style = MaterialTheme.typography.bodyMedium.copy(fontSize = (MaterialTheme.typography.bodyMedium.fontSize.value * textSizeMultiplier).sp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Surface(modifier = Modifier.fillMaxWidth(), color = surfaceBg, shadowElevation = 8.dp) {
                        Column {
                            if (pendingChatAttachmentName.isNotEmpty()) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp).background(primaryColor.copy(alpha = 0.1f), RoundedCornerShape(8.dp)).padding(8.dp)) {
                                    Icon(if (pendingChatAttachmentType == "image") Icons.Default.Image else Icons.Default.AttachFile, null, tint = primaryColor, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(pendingChatAttachmentName, color = textColor, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    IconButton(onClick = { pendingChatAttachmentUri = null; pendingChatAttachmentName = ""; pendingChatAttachmentType = "" }, modifier = Modifier.size(18.dp).pressScale()) {
                                        Icon(Icons.Default.Close, null, tint = textColor.copy(alpha = 0.6f), modifier = Modifier.size(12.dp))
                                    }
                                }
                            }
                            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { chatFileLauncher.launch("*/*") }, modifier = Modifier.size(40.dp).pressScale()) {
                                    Icon(Icons.Default.AttachFile, "Attach", tint = primaryColor, modifier = Modifier.size(22.dp))
                                }
                                Spacer(Modifier.width(4.dp))
                                OutlinedTextField(
                                    value = messageText, onValueChange = { if (it.length <= 1000) messageText = it },
                                    placeholder = { Text("Message…", color = textColor.copy(alpha = 0.45f)) },
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = textColor, unfocusedTextColor = textColor, focusedBorderColor = primaryColor, unfocusedBorderColor = textColor.copy(alpha = 0.2f), cursorColor = primaryColor, focusedContainerColor = if (lightMode) Color.White else Color(0xFF1A2840), unfocusedContainerColor = if (lightMode) Color.White else Color(0xFF1A2840)),
                                    modifier = Modifier.weight(1f), shape = RoundedCornerShape(24.dp), maxLines = 3
                                )
                                Spacer(Modifier.width(8.dp))
                                val canSend = (messageText.isNotBlank() || pendingChatAttachmentUri != null) && !isUploadingChat
                                IconButton(
                                    onClick = {
                                        val c = classroom
                                        if (c == null || currentUserId !in c.memberIds) { Toast.makeText(context, "Can't send", Toast.LENGTH_SHORT).show(); return@IconButton }
                                        if (!canSend) return@IconButton
                                        val id = db.collection("classrooms").document(classroomId).collection("messages").document().id
                                        val textToSend = messageText.trim()
                                        val attachUri = pendingChatAttachmentUri
                                        val attachName = pendingChatAttachmentName
                                        val attachType = pendingChatAttachmentType
                                        messageText = ""; pendingChatAttachmentUri = null; pendingChatAttachmentName = ""; pendingChatAttachmentType = ""
                                        fun saveMsg(url: String, name: String, type: String) {
                                            db.collection("classrooms").document(classroomId).collection("messages").document(id)
                                                .set(ClassroomMessage(id = id, senderId = currentUserId, senderName = myName, message = textToSend, timestamp = System.currentTimeMillis(), attachmentUrl = url, attachmentName = name, attachmentType = type))
                                            isUploadingChat = false
                                        }
                                        if (attachUri != null) {
                                            isUploadingChat = true
                                            uploadAndRun(attachUri, "classrooms/$classroomId/messages/$id/$attachName", { url -> saveMsg(url, attachName, attachType) }, { isUploadingChat = false; Toast.makeText(context, "Upload failed", Toast.LENGTH_SHORT).show() })
                                        } else { saveMsg("", "", "") }
                                    },
                                    enabled = canSend,
                                    modifier = Modifier.size(48.dp).background(if (canSend) primaryColor else Color.Transparent, CircleShape)
                                ) {
                                    if (isUploadingChat) androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.Black, strokeWidth = 2.dp)
                                    else Icon(Icons.AutoMirrored.Filled.Send, "Send", tint = if (canSend) Color.Black else textColor.copy(alpha = 0.4f), modifier = Modifier.size(22.dp))
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
fun MessagesScreen(
    primaryColor: Color,
    bgOpacity: Float,
    isSimplifiedUI: Boolean,
    textSizeMultiplier: Float,
    lightMode: Boolean = false,
    onBack: () -> Unit,
    onOpenChat: (String, String) -> Unit,
    onOpenGroup: (String) -> Unit = {}
) {
// Dynamic text colors
    val textColor = if (lightMode) Color(0xFF1A1A2E) else Color.White
    val textColorSecondary = if (lightMode) Color(0xFF1A1A2E).copy(alpha = 0.8f) else Color.White.copy(alpha = 0.9f)
    val textColorTertiary = if (lightMode) Color(0xFF1A1A2E).copy(alpha = 0.75f) else Color.White.copy(alpha = 0.85f)

    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    val currentUid = auth.currentUser?.uid ?: ""

    var connections by remember { mutableStateOf<List<Connection>>(emptyList()) }
    var conversations by remember { mutableStateOf<Map<String, Conversation>>(emptyMap()) }
    var groupChats by remember { mutableStateOf<List<GroupChat>>(emptyList()) }

    var showNewGroupDialog by remember { mutableStateOf(false) }
    var newGroupName by remember { mutableStateOf("") }
    var selectedGroupMembers by remember { mutableStateOf<Set<String>>(emptySet()) }

    var selectedTab by remember { mutableStateOf(0) } // 0=DMs, 1=Groups

// Load connections + conversations + groups
    LaunchedEffect(Unit) {
        val uid = currentUid
        if (uid.isEmpty()) return@LaunchedEffect

        db.collection("connections").document(uid)
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
                                        pfpUrl = profile.pfpUrl,
                                        role = profile.role
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
                com.google.firebase.firestore.Filter.equalTo("senderId", uid),
                com.google.firebase.firestore.Filter.equalTo("receiverId", uid)
            ))
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                val convos = mutableMapOf<String, Conversation>()
                snapshot?.documents?.forEach { doc ->
                    val msg = doc.toObject(ChatMessage::class.java) ?: return@forEach
                    val otherUserId = if (msg.senderId == uid) msg.receiverId else msg.senderId

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

// Load group chats — no orderBy so no composite index needed; sort client-side
        db.collection("groupChats")
            .whereArrayContains("memberIds", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { android.util.Log.e("GroupChats", "Load error: ${error.message}"); return@addSnapshotListener }
                groupChats = (snapshot?.documents?.mapNotNull { it.toObject(GroupChat::class.java) } ?: emptyList())
                    .sortedByDescending { it.lastMessageTime }
            }
    }

// New group creation dialog
    if (showNewGroupDialog) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showNewGroupDialog = false; newGroupName = ""; selectedGroupMembers = emptySet() },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(0.9f),
                    colors = CardDefaults.cardColors(containerColor = if (lightMode) Color.White else Color(0xFF1A2740)),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(
                            "New Group Chat",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = primaryColor)
                        )
                        Spacer(Modifier.height(16.dp))
                        OutlinedTextField(
                            value = newGroupName,
                            onValueChange = { if (it.length <= 40) newGroupName = it },
                            label = { Text("Group Name", color = textColorSecondary) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryColor,
                                unfocusedBorderColor = textColor.copy(alpha = 0.3f),
                                focusedTextColor = textColor,
                                unfocusedTextColor = textColor
                            )
                        )
                        Spacer(Modifier.height(16.dp))
                        Text("Add Members", color = textColorSecondary, style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.height(8.dp))
                        if (connections.isEmpty()) {
                            Text("No connections to add", color = textColorTertiary, style = MaterialTheme.typography.bodySmall)
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                connections.forEach { conn ->
                                    val checked = conn.uid in selectedGroupMembers
                                    Row(
                                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                                            .background(if (checked) primaryColor.copy(alpha = 0.15f) else Color.Transparent)
                                            .clickable {
                                                selectedGroupMembers = if (checked) selectedGroupMembers - conn.uid else selectedGroupMembers + conn.uid
                                            }
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = checked,
                                            onCheckedChange = {
                                                selectedGroupMembers = if (checked) selectedGroupMembers - conn.uid else selectedGroupMembers + conn.uid
                                            },
                                            colors = CheckboxDefaults.colors(checkedColor = primaryColor)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(conn.name, color = textColor, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(20.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { showNewGroupDialog = false; newGroupName = ""; selectedGroupMembers = emptySet() }) {
                                Text("Cancel", color = textColorSecondary)
                            }
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    val name = newGroupName.trim()
                                    if (name.isEmpty()) { Toast.makeText(context, "Enter a group name", Toast.LENGTH_SHORT).show(); return@Button }
                                    if (selectedGroupMembers.isEmpty()) { Toast.makeText(context, "Select at least one member", Toast.LENGTH_SHORT).show(); return@Button }
                                    val db2 = FirebaseFirestore.getInstance()
                                    val myProfile = auth.currentUser
                                    val allMemberIds = selectedGroupMembers + currentUid
                                    val memberNames = mutableMapOf<String, String>()
                                    memberNames[currentUid] = myProfile?.displayName ?: "Me"
                                    connections.filter { it.uid in selectedGroupMembers }.forEach { memberNames[it.uid] = it.name }
                                    val docRef = db2.collection("groupChats").document()
                                    val group = GroupChat(
                                        id = docRef.id,
                                        name = name,
                                        memberIds = allMemberIds.toList(),
                                        memberNames = memberNames,
                                        createdBy = currentUid,
                                        createdAt = System.currentTimeMillis(),
                                        lastMessage = "",
                                        lastMessageTime = System.currentTimeMillis()
                                    )
                                    docRef.set(group).addOnSuccessListener {
                                        showNewGroupDialog = false; newGroupName = ""; selectedGroupMembers = emptySet()
                                        onOpenGroup(docRef.id)
                                    }.addOnFailureListener { e ->
                                        android.util.Log.e("GroupChat", "Create failed: ${e.message}", e)
                                        Toast.makeText(context, "Error: ${e.message?.take(80)}", Toast.LENGTH_LONG).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                            ) {
                                Text("Create", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
// Background — white in light mode, dark blue in dark mode
        Box(modifier = Modifier.fillMaxSize().background(
            if (lightMode) Color(0xFFF0F4FF) else Color(0xFF0A1628)
        ))
        if (!lightMode) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = bgOpacity)))
        }

        Column(modifier = Modifier.fillMaxSize()) {
// Header with back button
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = if (lightMode) Color(0xFFF0F4FF) else Color(0xFF1A1A1A),
                shadowElevation = 4.dp
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp, start = 4.dp, end = 16.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BackButton(onClick = onBack, tint = textColor)

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

                        if (selectedTab == 1) {
                            IconButton(onClick = { showNewGroupDialog = true }) {
                                Icon(Icons.Default.Add, "New Group", tint = primaryColor)
                            }
                        }
                    }
// Tab row
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                        listOf("Direct Messages", "Groups").forEachIndexed { idx, label ->
                            val active = selectedTab == idx
                            Box(
                                modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                                    .background(if (active) primaryColor else Color.Transparent)
                                    .clickable { selectedTab = idx }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    label,
                                    color = if (active) Color.Black else textColorSecondary,
                                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                                    style = MaterialTheme.typography.labelLarge.copy(fontSize = (13 * textSizeMultiplier).sp)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(24.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }

                if (selectedTab == 0) {
// DMs tab
                val teacherConns = connections.filter { it.role == "teacher" }
                val studentConns = connections.filter { it.role != "teacher" }

                @Composable fun ConnectionCard(connection: Connection) {
                    val conversation = conversations[connection.uid]
                    val sdf = remember { java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault()) }
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                        onClick = { onOpenChat(connection.uid, connection.name) },
                        colors = CardDefaults.cardColors(containerColor = if (lightMode) Color.White else Color(0xFF131F35)),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(50.dp).clip(CircleShape).background(primaryColor.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                                if (connection.pfpUrl.isNotEmpty()) {
                                    AsyncImage(model = connection.pfpUrl, contentDescription = null, modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
                                } else {
                                    Text(connection.name.take(1).uppercase(), color = primaryColor, fontWeight = FontWeight.Bold, fontSize = (18 * textSizeMultiplier).sp)
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(connection.name, color = textColor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall.copy(fontSize = (MaterialTheme.typography.titleSmall.fontSize.value * textSizeMultiplier).sp), modifier = Modifier.weight(1f))
                                    if (conversation != null) {
                                        Text(sdf.format(java.util.Date(conversation.lastMessageTime)), color = textColorTertiary, style = MaterialTheme.typography.bodySmall.copy(fontSize = (10 * textSizeMultiplier).sp))
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    conversation?.lastMessage?.take(50) ?: connection.school.ifEmpty { "Tap to start chatting" },
                                    color = if (conversation != null) textColorSecondary else textColorTertiary,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = (MaterialTheme.typography.bodySmall.fontSize.value * textSizeMultiplier).sp),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                if (connections.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = if (lightMode) Color(0xFFF0F4FF) else Color.White.copy(alpha = 0.1f)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Chat, contentDescription = null, tint = textColor.copy(alpha = 0.65f), modifier = Modifier.size(64.dp))
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("No Connections Yet", color = textColorSecondary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium.copy(fontSize = (MaterialTheme.typography.titleMedium.fontSize.value * textSizeMultiplier).sp))
                                Text("Connect with other members to start messaging", color = textColor.copy(alpha = 0.75f), style = MaterialTheme.typography.bodySmall.copy(fontSize = (MaterialTheme.typography.bodySmall.fontSize.value * textSizeMultiplier).sp), textAlign = TextAlign.Center)
                            }
                        }
                    }
                } else {
                    if (teacherConns.isNotEmpty()) {
                        item {
                            Text("TEACHERS", color = primaryColor, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black, letterSpacing = 2.sp, fontSize = (11 * textSizeMultiplier).sp), modifier = Modifier.padding(bottom = 8.dp))
                        }
                        items(teacherConns) { connection -> ConnectionCard(connection) }
                    }
                    if (studentConns.isNotEmpty()) {
                        item {
                            Text("STUDENTS", color = primaryColor, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black, letterSpacing = 2.sp, fontSize = (11 * textSizeMultiplier).sp), modifier = Modifier.padding(top = if (teacherConns.isNotEmpty()) 8.dp else 0.dp, bottom = 8.dp))
                        }
                        items(studentConns) { connection -> ConnectionCard(connection) }
                    }
                }
                } else {
// Groups tab
                    if (groupChats.isEmpty()) {
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
                                    Icon(Icons.Default.Group, null, tint = textColor.copy(alpha = 0.65f), modifier = Modifier.size(64.dp))
                                    Spacer(Modifier.height(16.dp))
                                    Text("No Group Chats", color = textColorSecondary, fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium.copy(fontSize = (MaterialTheme.typography.titleMedium.fontSize.value * textSizeMultiplier).sp))
                                    Text("Tap + to create a group chat with your connections",
                                        color = textColor.copy(alpha = 0.75f),
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = (MaterialTheme.typography.bodySmall.fontSize.value * textSizeMultiplier).sp),
                                        textAlign = TextAlign.Center)
                                }
                            }
                        }
                    } else {
                        items(groupChats.size) { idx ->
                            val group = groupChats[idx]
                            val sdf = remember { java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault()) }
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                                onClick = { onOpenGroup(group.id) },
                                colors = CardDefaults.cardColors(containerColor = if (lightMode) Color.White else Color(0xFF131F35)),
                                shape = RoundedCornerShape(16.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(50.dp).clip(CircleShape).background(primaryColor.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Group, null, tint = primaryColor, modifier = Modifier.size(28.dp))
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(group.name, color = textColor, fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.titleSmall.copy(fontSize = (MaterialTheme.typography.titleSmall.fontSize.value * textSizeMultiplier).sp),
                                                modifier = Modifier.weight(1f))
                                            if (group.lastMessageTime > 0L) {
                                                Text(sdf.format(java.util.Date(group.lastMessageTime)), color = textColorTertiary,
                                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = (10 * textSizeMultiplier).sp))
                                            }
                                        }
                                        Spacer(Modifier.height(2.dp))
                                        val preview = if (group.lastMessage.isNotEmpty())
                                            "${group.lastMessageSender.substringBefore(" ")}: ${group.lastMessage.take(40)}"
                                        else "${group.memberIds.size} members"
                                        Text(preview, color = textColorSecondary,
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = (MaterialTheme.typography.bodySmall.fontSize.value * textSizeMultiplier).sp),
                                            maxLines = 1)
                                    }
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
    val textColor = if (lightMode) Color(0xFF1A1A2E) else Color.White
    val textColorSecondary = if (lightMode) Color(0xFF1A1A2E).copy(alpha = 0.8f) else Color.White.copy(alpha = 0.9f)
    val textColorTertiary = if (lightMode) Color(0xFF1A1A2E).copy(alpha = 0.75f) else Color.White.copy(alpha = 0.85f)

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

    var pendingAttachmentUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var pendingAttachmentName by remember { mutableStateOf("") }
    var pendingAttachmentType by remember { mutableStateOf("") }
    var isUploading by remember { mutableStateOf(false) }
    var fullscreenImageUrl by remember { mutableStateOf<String?>(null) }
    var showImageCrop by remember { mutableStateOf(false) }
    var croppedImageBytes by remember { mutableStateOf<ByteArray?>(null) }
    var replyingTo by remember { mutableStateOf<ChatMessage?>(null) }
    var editingMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var editText by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val reactionEmojis = remember { listOf("👍", "❤️", "😂", "😮", "😢", "🔥") }
    var showEmojiPicker by remember { mutableStateOf(false) }
    var emojiSearch by remember { mutableStateOf("") }
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showAttachmentOptions by remember { mutableStateOf(false) }
    var cameraImageUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var showChatMenu by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }

    // Pick any file
    val fileLauncher = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val name = uri.lastPathSegment?.substringAfterLast('/') ?: "file"
            val mime = context.contentResolver.getType(uri) ?: ""
            pendingAttachmentUri = uri; pendingAttachmentName = name; pendingAttachmentType = if (mime.startsWith("image")) "image" else "file"
        }
    }
    // Pick from camera roll (images only)
    val galleryLauncher = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val name = uri.lastPathSegment?.substringAfterLast('/') ?: "photo.jpg"
            pendingAttachmentUri = uri; pendingAttachmentName = name; pendingAttachmentType = "image"
        }
    }
    // Take a photo with camera
    val cameraLauncher = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.TakePicture()) { success ->
        if (success && cameraImageUri != null) {
            pendingAttachmentUri = cameraImageUri
            pendingAttachmentName = "photo_${System.currentTimeMillis()}.jpg"
            pendingAttachmentType = "image"
        }
    }

    fun uploadAndSend(uri: android.net.Uri, path: String, onUrl: (String) -> Unit) {
        val ref = FirebaseStorage.getInstance().reference.child(path)
        val stream = try { context.contentResolver.openInputStream(uri) } catch (e: Exception) { isUploading = false; Toast.makeText(context, "Can't read file", Toast.LENGTH_SHORT).show(); return }
        if (stream == null) { isUploading = false; return }
        ref.putStream(stream).addOnSuccessListener {
            stream.close()
            ref.downloadUrl.addOnSuccessListener { url -> onUrl(url.toString()) }.addOnFailureListener { isUploading = false; Toast.makeText(context, "Upload failed", Toast.LENGTH_SHORT).show() }
        }.addOnFailureListener { stream.close(); isUploading = false; Toast.makeText(context, "Upload failed", Toast.LENGTH_SHORT).show() }
    }

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

// Always update messages (needed for reactions, edits, seen status)
                val hadNewMessages = newFromFirebase.isNotEmpty()
                messages = conversationMessages
                if (hadNewMessages) {
                    scope.launch {
                        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
                    }
                }
// Mark incoming unseen messages as seen
                conversationMessages
                    .filter { it.senderId == otherUserId && it.seenAt == 0L }
                    .forEach { msg ->
                        db.collection("messages").document(msg.id)
                            .update("seenAt", System.currentTimeMillis())
                    }

                previousMessageCount = conversationMessages.size
            }
    }

    Box(modifier = Modifier.fillMaxSize()) {
// Background — white in light mode, dark blue in dark mode
        Box(modifier = Modifier.fillMaxSize().background(
            if (lightMode) Color(0xFFF0F4FF) else Color(0xFF0A1628)
        ))
        if (!lightMode) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = bgOpacity)))
        }

        Column(modifier = Modifier.fillMaxSize()) {
// Header
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = if (lightMode) Color(0xFFF0F4FF) else Color(0xFF131F35),
                shadowElevation = 4.dp
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp, start = 0.dp, end = 4.dp, bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BackButton(onClick = onBack, tint = textColor)

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
                        IconButton(onClick = { showSearch = !showSearch; if (!showSearch) searchQuery = "" }) {
                            Icon(if (showSearch) Icons.Default.Close else Icons.Default.Search, null, tint = textColor)
                        }
                        Box {
                            IconButton(onClick = { showChatMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More options", tint = textColor)
                            }
                            DropdownMenu(
                                expanded = showChatMenu,
                                onDismissRequest = { showChatMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Report user", color = Color(0xFFCC3333)) },
                                    leadingIcon = { Icon(Icons.Default.Flag, null, tint = Color(0xFFCC3333)) },
                                    onClick = { showChatMenu = false; showReportDialog = true }
                                )
                            }
                        }
                    }
                    if (showSearch) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search messages…", color = textColorTertiary) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryColor, unfocusedBorderColor = textColor.copy(alpha = 0.2f),
                                focusedTextColor = textColor, unfocusedTextColor = textColor,
                                focusedContainerColor = if (lightMode) Color(0xFFF0F4FF) else Color(0xFF1E2D45),
                                unfocusedContainerColor = if (lightMode) Color(0xFFF0F4FF) else Color(0xFF1E2D45)
                            ),
                            leadingIcon = { Icon(Icons.Default.Search, null, tint = textColorTertiary, modifier = Modifier.size(18.dp)) }
                        )
                        Spacer(Modifier.height(6.dp))
                    }
                }
            }

// Messages list
            val displayedMessages = remember(messages, searchQuery) {
                if (searchQuery.isBlank()) messages
                else messages.filter { it.message.contains(searchQuery, ignoreCase = true) || it.attachmentName.contains(searchQuery, ignoreCase = true) }
            }
            // Index of the last message sent by me (for seen indicator)
            val lastMyMsgIdx = remember(messages) { messages.indexOfLast { it.senderId == currentUserId } }

            Box(modifier = Modifier.weight(1f)) {
                if (displayedMessages.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (searchQuery.isNotBlank()) {
                            Icon(Icons.Default.Search, null, tint = textColorTertiary, modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(12.dp))
                            Text("No results for \"$searchQuery\"", color = textColorTertiary, style = MaterialTheme.typography.bodyMedium)
                        } else {
                            Box(
                                modifier = Modifier.size(72.dp).clip(CircleShape).background(primaryColor.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) { Icon(Icons.Default.Chat, null, tint = primaryColor, modifier = Modifier.size(36.dp)) }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("No messages yet", color = textColor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium.copy(fontSize = (MaterialTheme.typography.titleMedium.fontSize.value * textSizeMultiplier).sp))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Say hi to ${otherUserProfile?.name?.substringBefore(" ") ?: "them"}!", color = textColorTertiary, style = MaterialTheme.typography.bodySmall.copy(fontSize = (MaterialTheme.typography.bodySmall.fontSize.value * textSizeMultiplier).sp))
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures { focusManager.clearFocus() } },
                        state = listState,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        items(displayedMessages.size) { index ->
                            val message = displayedMessages[index]
                            val isMe = message.senderId == currentUserId
                            val isNew = message.id in newMessageIds
                            val isLastInGroup = index == displayedMessages.size - 1 || displayedMessages[index + 1].senderId != message.senderId
                            val isFirstInGroup = index == 0 || displayedMessages[index - 1].senderId != message.senderId
                            val isLastMyMsg = isMe && messages.indexOf(message) == lastMyMsgIdx

                            val animatedAlpha = remember { androidx.compose.animation.core.Animatable(if (isNew) 0f else 1f) }
                            val animatedOffset = remember { androidx.compose.animation.core.Animatable(if (isNew) 30f else 0f) }

                            LaunchedEffect(isNew) {
                                if (isNew) {
                                    launch { animatedAlpha.animateTo(1f, animationSpec = androidx.compose.animation.core.tween(250)) }
                                    launch { animatedOffset.animateTo(0f, animationSpec = androidx.compose.animation.core.spring(dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy, stiffness = androidx.compose.animation.core.Spring.StiffnessMedium)) }
                                }
                            }

                            val groupedReactions = message.reactions.entries.groupBy({ it.value }, { it.key })

                            Column(
                                modifier = Modifier.fillMaxWidth()
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
                                    if (isMe) Spacer(modifier = Modifier.width(64.dp))

                                    val bubbleShape = RoundedCornerShape(
                                        topStart = 18.dp, topEnd = 18.dp,
                                        bottomStart = if (isMe) 18.dp else if (isLastInGroup) 6.dp else 4.dp,
                                        bottomEnd = if (isMe) if (isLastInGroup) 6.dp else 4.dp else 18.dp
                                    )
                                    val bubbleColor = if (isMe) primaryColor else (if (lightMode) Color(0xFFF0F4FF) else Color(0xFF1E2D45))
                                    val contentColor = if (isMe) Color.Black else textColor

                                    if (message.attachmentType == "link") {
                                        val linkUrl = message.attachmentUrl
                                        val domain = message.attachmentName.ifEmpty { urlDomain(linkUrl) }
                                        val domainColor = when {
                                            domain.contains("instagram") -> Color(0xFF833AB4)
                                            domain.contains("youtube") -> Color(0xFFFF0000)
                                            domain.contains("tiktok") -> Color(0xFF010101)
                                            domain.contains("twitter") || domain.contains("x.com") -> Color(0xFF1DA1F2)
                                            domain.contains("facebook") -> Color(0xFF1877F2)
                                            else -> primaryColor
                                        }
                                        Surface(
                                            shape = bubbleShape,
                                            color = bubbleColor,
                                            modifier = Modifier.widthIn(max = 260.dp).clickable {
                                                try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(linkUrl))) } catch (e: Exception) {}
                                            }
                                        ) {
                                            Column {
                                                Box(modifier = Modifier.fillMaxWidth().height(6.dp).background(domainColor))
                                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                        Box(modifier = Modifier.size(16.dp).clip(RoundedCornerShape(3.dp)).background(domainColor))
                                                        Text(domain, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = (10 * textSizeMultiplier).sp), color = contentColor.copy(alpha = 0.7f))
                                                    }
                                                    Text(message.message.ifEmpty { linkUrl }, style = MaterialTheme.typography.bodySmall.copy(fontSize = (12 * textSizeMultiplier).sp), color = contentColor, maxLines = 3, overflow = TextOverflow.Ellipsis)
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(Icons.Default.Launch, null, tint = domainColor, modifier = Modifier.size(12.dp))
                                                        Spacer(Modifier.width(3.dp))
                                                        Text("Open", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp), color = domainColor)
                                                    }
                                                }
                                            }
                                        }
                                    } else if (message.attachmentType == "article") {
                                        val articleImageUrl = message.message.substringBefore("\n")
                                        val articleSummary = message.message.substringAfter("\n", "")
                                        Surface(
                                            shape = bubbleShape,
                                            color = bubbleColor,
                                            modifier = Modifier.widthIn(max = 260.dp).clickable {
                                                try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(message.attachmentUrl))) } catch (e: Exception) {}
                                            }
                                        ) {
                                            Column {
                                                if (articleImageUrl.startsWith("http")) {
                                                    AsyncImage(model = articleImageUrl, contentDescription = null, contentScale = ContentScale.Crop,
                                                        modifier = Modifier.fillMaxWidth().height(130.dp).clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)))
                                                }
                                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                                    Text("FBLA NEWS", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontSize = 9.sp), color = contentColor.copy(alpha = 0.6f))
                                                    Text(message.attachmentName, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = (13 * textSizeMultiplier).sp, lineHeight = (17 * textSizeMultiplier).sp), color = contentColor, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                                    if (articleSummary.isNotEmpty()) Text(articleSummary, style = MaterialTheme.typography.bodySmall.copy(fontSize = (11 * textSizeMultiplier).sp), color = contentColor.copy(alpha = 0.75f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text("Read Article", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp), color = contentColor.copy(alpha = 0.8f))
                                                        Spacer(Modifier.width(2.dp))
                                                        Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = contentColor.copy(alpha = 0.8f), modifier = Modifier.size(10.dp))
                                                    }
                                                }
                                            }
                                        }
                                    } else if (message.attachmentUrl.isNotEmpty() && message.attachmentType == "image") {
                                        Box(modifier = Modifier.widthIn(max = 240.dp)
                                            .then(if (isMe) Modifier.border(2.dp, primaryColor, bubbleShape) else Modifier)
                                            .clip(bubbleShape).pointerInput(Unit) {
                                            detectTapGestures(onTap = { fullscreenImageUrl = message.attachmentUrl }, onLongPress = { selectedMessage = message; showMessageOptions = true })
                                        }) {
                                            coil.compose.AsyncImage(model = message.attachmentUrl, contentDescription = null, modifier = Modifier.widthIn(max = 240.dp).heightIn(max = 300.dp), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
                                        }
                                    } else {
                                        Box(modifier = Modifier
                                            .widthIn(max = 280.dp)
                                            .background(color = bubbleColor, shape = bubbleShape)
                                            .pointerInput(Unit) { detectTapGestures(onLongPress = { selectedMessage = message; showMessageOptions = true }) }
                                        ) {
                                            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)) {
                                                if (message.replyToId.isNotEmpty()) {
                                                    Row(
                                                        modifier = Modifier.background(contentColor.copy(alpha = 0.1f), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 5.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Box(modifier = Modifier.width(2.dp).height(28.dp).background(primaryColor, RoundedCornerShape(1.dp)))
                                                        Spacer(Modifier.width(7.dp))
                                                        Column {
                                                            Text(message.replyToSender, color = primaryColor, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                                            Text(message.replyToText, color = contentColor.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                        }
                                                    }
                                                    Spacer(Modifier.height(6.dp))
                                                }
                                                if (message.attachmentUrl.isNotEmpty()) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier
                                                            .background(contentColor.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                                                            .pointerInput(message.id) {
                                                                detectTapGestures(
                                                                    onTap = {
                                                                        try {
                                                                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(message.attachmentUrl))
                                                                            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                                                            context.startActivity(android.content.Intent.createChooser(intent, "Open file"))
                                                                        } catch (e: Exception) { Toast.makeText(context, "No app to open this file", Toast.LENGTH_SHORT).show() }
                                                                    },
                                                                    onLongPress = { selectedMessage = message; showMessageOptions = true }
                                                                )
                                                            }
                                                            .padding(horizontal = 8.dp, vertical = 6.dp)
                                                    ) {
                                                        Icon(Icons.Default.AttachFile, null, tint = contentColor, modifier = Modifier.size(14.dp))
                                                        Spacer(Modifier.width(5.dp))
                                                        Text(message.attachmentName.ifBlank { "File" }, color = contentColor, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                        Spacer(Modifier.width(4.dp))
                                                        Icon(Icons.Default.Download, null, tint = contentColor.copy(alpha = 0.7f), modifier = Modifier.size(12.dp))
                                                    }
                                                    if (message.message.isNotBlank()) Spacer(Modifier.height(5.dp))
                                                }
                                                if (message.message.isNotBlank()) {
                                                    val mentionRegex = remember { Regex("@[a-z0-9_]+") }
                                                    val annotatedMsg = remember(message.message, isMe, primaryColor) {
                                                        buildAnnotatedString {
                                                            val text = message.message
                                                            var last = 0
                                                            mentionRegex.findAll(text).forEach { match ->
                                                                append(text.substring(last, match.range.first))
                                                                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = if (isMe) Color.Black else primaryColor)) {
                                                                    append(match.value)
                                                                }
                                                                last = match.range.last + 1
                                                            }
                                                            append(text.substring(last))
                                                        }
                                                    }
                                                    Text(annotatedMsg, color = contentColor, style = MaterialTheme.typography.bodyMedium.copy(fontSize = (MaterialTheme.typography.bodyMedium.fontSize.value * textSizeMultiplier).sp), softWrap = true)
                                                }
                                            }
                                        }
                                    }

                                    if (!isMe) Spacer(modifier = Modifier.width(64.dp))
                                }

                                // Reaction pills — always shown below bubble (images and text)
                                if (groupedReactions.isNotEmpty()) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        groupedReactions.forEach { (emoji, uids) ->
                                            val iMine = currentUserId in uids
                                            Box(
                                                modifier = Modifier
                                                    .background(if (iMine) primaryColor.copy(alpha = 0.22f) else (if (lightMode) Color(0xFFE4E9F7) else Color(0xFF253552)), RoundedCornerShape(10.dp))
                                                    .border(1.dp, if (iMine) primaryColor else Color.Transparent, RoundedCornerShape(10.dp))
                                                    .clickable {
                                                        // Optimistic local update so reactor sees change instantly
                                                        val newReactions = message.reactions.toMutableMap()
                                                        if (iMine) newReactions.remove(currentUserId) else newReactions[currentUserId] = emoji
                                                        messages = messages.map { if (it.id == message.id) it.copy(reactions = newReactions) else it }
                                                        val upd = if (iMine) mapOf("reactions.$currentUserId" to FieldValue.delete()) else mapOf("reactions.$currentUserId" to emoji)
                                                        db.collection("messages").document(message.id).update(upd)
                                                    }
                                                    .padding(horizontal = 7.dp, vertical = 3.dp)
                                            ) {
                                                Text("$emoji ${uids.size}", style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp), color = if (iMine) primaryColor else textColor)
                                            }
                                        }
                                    }
                                }

                                // Timestamp + seen indicator
                                if (isLastInGroup || message.edited || isLastMyMsg) {
                                    val sdf = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (message.edited) Text("edited · ", color = textColorTertiary, style = MaterialTheme.typography.bodySmall.copy(fontSize = (9 * textSizeMultiplier).sp))
                                        if (isLastInGroup) Text(sdf.format(java.util.Date(message.timestamp)), color = textColorTertiary, style = MaterialTheme.typography.bodySmall.copy(fontSize = (10 * textSizeMultiplier).sp))
                                        if (isLastMyMsg && isLastInGroup) {
                                            Spacer(Modifier.width(4.dp))
                                            if (message.seenAt > 0L) {
                                                Text("· Seen", color = primaryColor.copy(alpha = 0.8f), style = MaterialTheme.typography.labelSmall.copy(fontSize = (10 * textSizeMultiplier).sp))
                                            } else {
                                                Icon(Icons.Default.DoneAll, null, tint = textColorTertiary, modifier = Modifier.size(13.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

// Message input
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = if (lightMode) Color(0xFFF0F4FF) else Color(0xFF131F35),
                shadowElevation = 8.dp
            ) {
                Column {
                    // Reply bar
                    if (replyingTo != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth().background(primaryColor.copy(alpha = 0.1f)).padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.width(3.dp).height(36.dp).background(primaryColor, RoundedCornerShape(2.dp)))
                            Spacer(Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(replyingTo!!.senderName.ifBlank { "Message" }, color = primaryColor, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                Text(replyingTo!!.message.ifBlank { "Attachment" }, color = textColor.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            IconButton(onClick = { replyingTo = null }, modifier = Modifier.size(28.dp).pressScale()) {
                                Icon(Icons.Default.Close, null, tint = textColor.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    // Edit mode bar
                    if (editingMessage != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth().background(Color(0xFFFFF3CD)).padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Edit, null, tint = Color(0xFF856404), modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Editing message", color = Color(0xFF856404), style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                            IconButton(onClick = { editingMessage = null; editText = "" }, modifier = Modifier.size(28.dp).pressScale()) {
                                Icon(Icons.Default.Close, null, tint = Color(0xFF856404), modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                    if (pendingAttachmentName.isNotEmpty()) {
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (pendingAttachmentType == "image") {
                                Box(modifier = Modifier.size(72.dp).clip(RoundedCornerShape(12.dp))) {
                                    coil.compose.AsyncImage(
                                        model = if (croppedImageBytes != null) croppedImageBytes else pendingAttachmentUri,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                    )
                                    // Crop button
                                    Box(modifier = Modifier.align(Alignment.BottomEnd).background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(topStart = 8.dp)).padding(4.dp).clickable { showImageCrop = true }) {
                                        Icon(Icons.Default.Crop, null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    }
                                    // Remove button
                                    Box(modifier = Modifier.align(Alignment.TopEnd).padding(3.dp).size(18.dp).background(Color.Black.copy(alpha = 0.6f), CircleShape).clickable { pendingAttachmentUri = null; pendingAttachmentName = ""; pendingAttachmentType = ""; croppedImageBytes = null }, contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(11.dp))
                                    }
                                }
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f).background(primaryColor.copy(alpha = 0.1f), RoundedCornerShape(10.dp)).padding(8.dp)) {
                                    Icon(Icons.Default.AttachFile, null, tint = primaryColor, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(pendingAttachmentName, color = textColor, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    IconButton(onClick = { pendingAttachmentUri = null; pendingAttachmentName = ""; pendingAttachmentType = "" }, modifier = Modifier.size(20.dp).pressScale()) {
                                        Icon(Icons.Default.Close, null, tint = textColor.copy(alpha = 0.6f), modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (editingMessage == null) {
                            IconButton(onClick = { showAttachmentOptions = true }, modifier = Modifier.size(40.dp).pressScale()) {
                                Icon(Icons.Default.AttachFile, "Attach", tint = primaryColor, modifier = Modifier.size(22.dp))
                            }
                        }
                        OutlinedTextField(
                            value = if (editingMessage != null) editText else messageText,
                            onValueChange = { if (it.length <= 1000) { if (editingMessage != null) editText = it else messageText = it } },
                            placeholder = { Text("Message…", color = textColorTertiary, style = MaterialTheme.typography.bodyMedium.copy(fontSize = (14 * textSizeMultiplier).sp)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = textColor, unfocusedTextColor = textColor,
                                focusedBorderColor = primaryColor, unfocusedBorderColor = textColor.copy(alpha = 0.2f),
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
                        val canSend = if (editingMessage != null) editText.isNotBlank() else (messageText.isNotBlank() || pendingAttachmentUri != null) && !isUploading
                        IconButton(
                            onClick = {
                                if (currentUserId.isEmpty() || otherUserId.isEmpty() || !canSend) return@IconButton
                                // Edit mode
                                if (editingMessage != null) {
                                    val newText = editText.trim()
                                    val mid = editingMessage!!.id
                                    messages = messages.map { if (it.id == mid) it.copy(message = newText, edited = true) else it }
                                    db.collection("messages").document(mid).update(mapOf("message" to newText, "edited" to true))
                                    editingMessage = null; editText = ""
                                    return@IconButton
                                }
                                val msgText = messageText.trim()
                                val attachUri = pendingAttachmentUri
                                val attachName = pendingAttachmentName
                                val attachType = pendingAttachmentType
                                val cropBytes = croppedImageBytes
                                messageText = ""; pendingAttachmentUri = null; pendingAttachmentName = ""; pendingAttachmentType = ""; croppedImageBytes = null

                                val replyTarget = replyingTo
                                replyingTo = null
                                fun sendTextMsg(text: String) {
                                    if (text.isBlank()) return
                                    val tid = db.collection("messages").document().id
                                    val tm = ChatMessage(id = tid, senderId = currentUserId, receiverId = otherUserId, message = text, timestamp = System.currentTimeMillis() + 1L, senderName = auth.currentUser?.displayName ?: "", replyToId = replyTarget?.id ?: "", replyToText = replyTarget?.message?.take(60) ?: "", replyToSender = replyTarget?.senderName ?: "")
                                    messages = messages + tm
                                    newMessageIds = newMessageIds + tid
                                    scope.launch { listState.animateScrollToItem(messages.size - 1) }
                                    scope.launch { delay(500); newMessageIds = newMessageIds - tid }
                                    db.collection("messages").document(tid).set(tm).addOnFailureListener { messages = messages.filter { it.id != tid } }
                                }
                                fun saveAttachMsg(url: String, name: String, type: String) {
                                    val aid = db.collection("messages").document().id
                                    val am = ChatMessage(id = aid, senderId = currentUserId, receiverId = otherUserId, message = "", timestamp = System.currentTimeMillis(), senderName = auth.currentUser?.displayName ?: "", attachmentUrl = url, attachmentName = name, attachmentType = type)
                                    messages = messages + am
                                    newMessageIds = newMessageIds + aid
                                    scope.launch { listState.animateScrollToItem(messages.size - 1) }
                                    scope.launch { delay(500); newMessageIds = newMessageIds - aid }
                                    db.collection("messages").document(aid).set(am).addOnFailureListener { messages = messages.filter { it.id != aid } }
                                    isUploading = false
                                    sendTextMsg(msgText)
                                }
                                when {
                                    cropBytes != null -> {
                                        isUploading = true
                                        val aid = db.collection("messages").document().id
                                        val ref = FirebaseStorage.getInstance().reference.child("messages/$aid/$attachName")
                                        ref.putBytes(cropBytes).addOnSuccessListener {
                                            ref.downloadUrl.addOnSuccessListener { url -> saveAttachMsg(url.toString(), attachName, "image") }.addOnFailureListener { isUploading = false; Toast.makeText(context, "Upload failed", Toast.LENGTH_SHORT).show() }
                                        }.addOnFailureListener { isUploading = false; Toast.makeText(context, "Upload failed", Toast.LENGTH_SHORT).show() }
                                    }
                                    attachUri != null -> {
                                        isUploading = true
                                        val aid = db.collection("messages").document().id
                                        val ref = FirebaseStorage.getInstance().reference.child("messages/$aid/$attachName")
                                        val stream = try { context.contentResolver.openInputStream(attachUri) } catch (e: Exception) { isUploading = false; return@IconButton }
                                        if (stream == null) { isUploading = false; return@IconButton }
                                        ref.putStream(stream).addOnSuccessListener {
                                            stream.close()
                                            ref.downloadUrl.addOnSuccessListener { url -> saveAttachMsg(url.toString(), attachName, attachType) }.addOnFailureListener { isUploading = false; Toast.makeText(context, "Upload failed", Toast.LENGTH_SHORT).show() }
                                        }.addOnFailureListener { stream.close(); isUploading = false; Toast.makeText(context, "Upload failed", Toast.LENGTH_SHORT).show() }
                                    }
                                    else -> sendTextMsg(msgText)
                                }
                            },
                            enabled = canSend,
                            modifier = Modifier.size(48.dp).background(if (canSend) primaryColor else Color.Transparent, CircleShape)
                        ) {
                            if (isUploading) androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.Black, strokeWidth = 2.dp)
                            else if (editingMessage != null) Icon(Icons.Default.Check, contentDescription = "Save", tint = if (canSend) Color.Black else textColorTertiary, modifier = Modifier.size(22.dp))
                            else Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = if (canSend) Color.Black else textColorTertiary, modifier = Modifier.size(22.dp))
                        }
                    }
                }
            }
        }

// Attachment source picker
        if (showReportDialog) {
            val reportReasons = listOf(
                "Harassment or bullying",
                "Inappropriate or explicit content",
                "Spam or scam",
                "Fake account or impersonation",
                "Other"
            )
            var selectedReason by remember { mutableStateOf(reportReasons.first()) }
            AlertDialog(
                onDismissRequest = { showReportDialog = false },
                containerColor = if (lightMode) Color.White else Color(0xFF1A2740),
                title = { Text("Report ${otherUserProfile?.name ?: "this user"}", color = textColor, fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text("Why are you reporting this account?", color = textColorSecondary, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(8.dp))
                        reportReasons.forEach { reason ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedReason = reason }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedReason == reason,
                                    onClick = { selectedReason = reason },
                                    colors = RadioButtonDefaults.colors(selectedColor = primaryColor, unselectedColor = textColorTertiary)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(reason, color = textColor, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        showReportDialog = false
                        val reportedName = otherUserProfile?.name ?: "Unknown"
                        val reporterEmail = FirebaseAuth.getInstance().currentUser?.email ?: "Unknown"
                        val body = "I would like to report a user on FBLA Hub.\n\n" +
                            "Reported user: $reportedName\n" +
                            "Reported user ID: $otherUserId\n" +
                            "Reason: $selectedReason\n" +
                            "Reported by: $reporterEmail\n\n" +
                            "Additional details (optional):\n"
                        sendSupportEmail(
                            context,
                            subject = "FBLA Hub — Report: $reportedName",
                            body = body
                        )
                    }) {
                        Text("REPORT", color = Color(0xFFCC3333), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showReportDialog = false }) {
                        Text("CANCEL", color = textColorSecondary)
                    }
                }
            )
        }

        if (showAttachmentOptions) {
            androidx.compose.ui.window.Dialog(
                onDismissRequest = { showAttachmentOptions = false },
                properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
            ) {
                val sheetBgA = if (lightMode) Color(0xFFF0F4FF) else Color(0xFF1A2740)
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f))
                        .pointerInput(Unit) { detectTapGestures { showAttachmentOptions = false } },
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                            .background(sheetBgA, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                            .pointerInput(Unit) { detectTapGestures { } }
                            .navigationBarsPadding().padding(bottom = 8.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 16.dp), contentAlignment = Alignment.Center) {
                            Box(modifier = Modifier.width(40.dp).height(4.dp).background(textColor.copy(alpha = 0.2f), RoundedCornerShape(2.dp)))
                        }
                        Text("Add Attachment", color = textColor, fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 12.dp))
                        // Camera Roll
                        Row(modifier = Modifier.fillMaxWidth().clickable {
                            showAttachmentOptions = false; galleryLauncher.launch("image/*")
                        }.padding(horizontal = 24.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(40.dp).background(primaryColor.copy(alpha = 0.12f), CircleShape), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.PhotoLibrary, null, tint = primaryColor, modifier = Modifier.size(22.dp))
                            }
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text("Camera Roll", color = textColor, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge)
                                Text("Choose a photo or video", color = textColorTertiary, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        // Take Photo
                        Row(modifier = Modifier.fillMaxWidth().clickable {
                            showAttachmentOptions = false
                            try {
                                val photoFile = java.io.File.createTempFile("camera_", ".jpg", context.cacheDir)
                                val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
                                cameraImageUri = uri
                                cameraLauncher.launch(uri)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Camera not available", Toast.LENGTH_SHORT).show()
                            }
                        }.padding(horizontal = 24.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(40.dp).background(primaryColor.copy(alpha = 0.12f), CircleShape), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.CameraAlt, null, tint = primaryColor, modifier = Modifier.size(22.dp))
                            }
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text("Take Photo", color = textColor, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge)
                                Text("Use your camera", color = textColorTertiary, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        // Choose File
                        Row(modifier = Modifier.fillMaxWidth().clickable {
                            showAttachmentOptions = false; fileLauncher.launch("*/*")
                        }.padding(horizontal = 24.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(40.dp).background(primaryColor.copy(alpha = 0.12f), CircleShape), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.AttachFile, null, tint = primaryColor, modifier = Modifier.size(22.dp))
                            }
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text("Choose File", color = textColor, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge)
                                Text("Documents, PDFs, and more", color = textColorTertiary, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                }
            }
        }

// Image crop dialog
        if (showImageCrop && pendingAttachmentUri != null) {
            var cropScale by remember { mutableStateOf(1f) }
            var cropOffsetX by remember { mutableStateOf(0f) }
            var cropOffsetY by remember { mutableStateOf(0f) }
            var containerW by remember { mutableStateOf(0f) }
            var containerH by remember { mutableStateOf(0f) }
            var isCropping by remember { mutableStateOf(false) }
            val cropTransformState = rememberTransformableState { zoomChange, panChange, _ ->
                cropScale = (cropScale * zoomChange).coerceIn(0.5f, 8f)
                cropOffsetX += panChange.x; cropOffsetY += panChange.y
            }
            androidx.compose.ui.window.Dialog(onDismissRequest = { showImageCrop = false }, properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                    coil.compose.AsyncImage(
                        model = pendingAttachmentUri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                            .onSizeChanged { containerW = it.width.toFloat(); containerH = it.height.toFloat() }
                            .graphicsLayer(scaleX = cropScale, scaleY = cropScale, translationX = cropOffsetX, translationY = cropOffsetY)
                            .transformable(state = cropTransformState),
                        contentScale = androidx.compose.ui.layout.ContentScale.Fit
                    )
                    // Top bar
                    Row(modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().background(Color.Black.copy(alpha = 0.55f)).padding(top = 40.dp, start = 8.dp, end = 8.dp, bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { showImageCrop = false }) { Text("Cancel", color = Color.White) }
                        Text("Pinch to zoom · Drag to reposition", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
                        if (isCropping) {
                            androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.size(20.dp), color = primaryColor, strokeWidth = 2.dp)
                        } else {
                            TextButton(onClick = {
                                isCropping = true
                                scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                    try {
                                        val uri = pendingAttachmentUri ?: return@launch
                                        val stream = context.contentResolver.openInputStream(uri) ?: return@launch
                                        val original = android.graphics.BitmapFactory.decodeStream(stream); stream.close()
                                        val W = containerW; val H = containerH
                                        val imgW = original.width.toFloat(); val imgH = original.height.toFloat()
                                        val baseScale = minOf(W / imgW, H / imgH)
                                        val imgOffsetX = (W - imgW * baseScale) / 2f
                                        val imgOffsetY = (H - imgH * baseScale) / 2f
                                        fun toImgX(sx: Float) = ((sx - cropOffsetX - W / 2f) / cropScale + W / 2f - imgOffsetX) / baseScale
                                        fun toImgY(sy: Float) = ((sy - cropOffsetY - H / 2f) / cropScale + H / 2f - imgOffsetY) / baseScale
                                        val l = toImgX(0f).toInt().coerceIn(0, original.width - 1)
                                        val t = toImgY(0f).toInt().coerceIn(0, original.height - 1)
                                        val r = toImgX(W).toInt().coerceIn(l + 1, original.width)
                                        val b = toImgY(H).toInt().coerceIn(t + 1, original.height)
                                        val cropped = android.graphics.Bitmap.createBitmap(original, l, t, r - l, b - t)
                                        val out = java.io.ByteArrayOutputStream()
                                        cropped.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
                                        croppedImageBytes = out.toByteArray()
                                        withContext(kotlinx.coroutines.Dispatchers.Main) { isCropping = false; showImageCrop = false }
                                    } catch (e: Exception) {
                                        android.util.Log.e("Crop", "Failed: ${e.message}")
                                        withContext(kotlinx.coroutines.Dispatchers.Main) { isCropping = false; showImageCrop = false }
                                    }
                                }
                            }) { Text("Done", color = primaryColor, fontWeight = FontWeight.Bold) }
                        }
                    }
                }
            }
        }

// Emoji picker dialog
        if (showEmojiPicker && selectedMessage != null) {
            val pickerMsg = selectedMessage!!
            val allEmojis = remember {
                listOf(
                    "😀" to "grinning","😃" to "happy","😄" to "laugh","😁" to "beam","😆" to "haha",
                    "😅" to "sweat smile","🤣" to "rolling","😂" to "joy","🙂" to "smile","😉" to "wink",
                    "😊" to "blush","😇" to "angel","🥰" to "hearts","😍" to "heart eyes","🤩" to "star eyes",
                    "😘" to "kiss","😋" to "yum","😛" to "tongue","😜" to "wink tongue","🤪" to "crazy",
                    "🤔" to "thinking","🤗" to "hug","🤫" to "shush","🤐" to "zip","😐" to "neutral",
                    "😑" to "expressionless","😶" to "no mouth","😏" to "smirk","😒" to "unamused",
                    "🙄" to "eye roll","😬" to "grimace","😔" to "pensive","😪" to "sleepy","😴" to "sleeping",
                    "😷" to "mask","🤒" to "sick","🤕" to "injured","🤢" to "nauseous","🤧" to "sneeze",
                    "🥵" to "hot","🥶" to "cold","😵" to "dizzy","🤯" to "mind blown","🥳" to "party",
                    "😎" to "cool","🤓" to "nerd","🧐" to "monocle","😕" to "confused","😟" to "worried",
                    "😮" to "surprised","😲" to "astonished","😳" to "flushed","🥺" to "pleading",
                    "😢" to "cry","😭" to "sobbing","😱" to "scream","😡" to "angry","😠" to "mad",
                    "🤬" to "cursing","😈" to "devil","💀" to "skull","💩" to "poop","👻" to "ghost",
                    "❤️" to "heart","🧡" to "orange heart","💛" to "yellow heart","💚" to "green heart",
                    "💙" to "blue heart","💜" to "purple heart","🖤" to "black heart","💔" to "broken heart",
                    "💕" to "two hearts","💖" to "sparkling heart","💗" to "growing heart","💓" to "beating heart",
                    "👍" to "thumbs up","👎" to "thumbs down","👌" to "ok","✌️" to "peace","🤞" to "fingers crossed",
                    "🤘" to "rock on","🤙" to "call me","👋" to "wave","👏" to "clap","🙌" to "raising hands",
                    "🙏" to "pray","💪" to "muscle","✍️" to "writing hand",
                    "🔥" to "fire","⭐" to "star","🌟" to "glowing star","✨" to "sparkles","💫" to "dizzy",
                    "🎉" to "party","🎊" to "confetti","🎈" to "balloon","🎁" to "gift","🏆" to "trophy",
                    "🥇" to "gold","🎯" to "bullseye","🎮" to "game","🚀" to "rocket","🌈" to "rainbow",
                    "☀️" to "sun","🌙" to "moon","⚡" to "lightning","🌊" to "wave","💯" to "hundred",
                    "✅" to "check","❌" to "cross","💡" to "idea","🔑" to "key","📱" to "phone",
                    "💻" to "laptop","📚" to "books","🎵" to "music","🎶" to "notes","🍕" to "pizza",
                    "🍔" to "burger","🍰" to "cake","☕" to "coffee","🐶" to "dog","🐱" to "cat",
                    "🦁" to "lion","🐼" to "panda","🦊" to "fox","🐸" to "frog","🦋" to "butterfly"
                )
            }
            val filtered = remember(emojiSearch) {
                if (emojiSearch.isBlank()) allEmojis
                else allEmojis.filter { it.second.contains(emojiSearch.trim(), ignoreCase = true) }
            }
            fun pickEmoji(emoji: String) {
                val alreadyReacted = pickerMsg.reactions[currentUserId] == emoji
                val newReactions = pickerMsg.reactions.toMutableMap()
                if (alreadyReacted) newReactions.remove(currentUserId) else newReactions[currentUserId] = emoji
                messages = messages.map { if (it.id == pickerMsg.id) it.copy(reactions = newReactions) else it }
                val update = if (alreadyReacted)
                    mapOf("reactions.$currentUserId" to FieldValue.delete())
                else
                    mapOf("reactions.$currentUserId" to emoji)
                db.collection("messages").document(pickerMsg.id).update(update)
                if (!alreadyReacted) RecentReactions.add(emoji)
                showEmojiPicker = false; emojiSearch = ""; showMessageOptions = false; selectedMessage = null
            }
            androidx.compose.ui.window.Dialog(
                onDismissRequest = { showEmojiPicker = false; emojiSearch = "" },
                properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
                    Card(
                        modifier = Modifier.fillMaxWidth(0.92f).fillMaxHeight(0.72f),
                        colors = CardDefaults.cardColors(containerColor = if (lightMode) Color.White else Color(0xFF1A2740)),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Pick a Reaction", color = textColor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                IconButton(onClick = { showEmojiPicker = false; emojiSearch = "" }, modifier = Modifier.size(32.dp).pressScale()) {
                                    Icon(Icons.Default.Close, "Close", tint = textColorTertiary)
                                }
                            }
                            // Show remove option if user already has a reaction on this message
                            val myCurrentReaction = pickerMsg.reactions[currentUserId]
                            if (myCurrentReaction != null) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                                        .background(Color(0xFFE53935).copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                                        .clickable {
                                            val newReactions = pickerMsg.reactions.toMutableMap().also { it.remove(currentUserId) }
                                            messages = messages.map { if (it.id == pickerMsg.id) it.copy(reactions = newReactions) else it }
                                            db.collection("messages").document(pickerMsg.id).update(mapOf("reactions.$currentUserId" to FieldValue.delete()))
                                            showEmojiPicker = false; emojiSearch = ""; showMessageOptions = false; selectedMessage = null
                                        }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(myCurrentReaction, fontSize = 20.sp)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Remove your reaction", color = Color(0xFFE53935), style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                                    Icon(Icons.Default.Close, null, tint = Color(0xFFE53935), modifier = Modifier.size(16.dp))
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            OutlinedTextField(
                                value = emojiSearch,
                                onValueChange = { emojiSearch = it },
                                placeholder = { Text("Search emoji…", color = textColorTertiary) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = primaryColor, unfocusedBorderColor = textColor.copy(alpha = 0.2f),
                                    focusedTextColor = textColor, unfocusedTextColor = textColor
                                ),
                                leadingIcon = { Icon(Icons.Default.Search, null, tint = textColorTertiary) },
                                trailingIcon = {
                                    if (emojiSearch.isNotEmpty()) IconButton(onClick = { emojiSearch = "" }) {
                                        Icon(Icons.Default.Close, null, tint = textColorTertiary, modifier = Modifier.size(18.dp))
                                    }
                                }
                            )
                            Spacer(Modifier.height(10.dp))
                            LazyColumn(modifier = Modifier.weight(1f)) {
                                if (RecentReactions.list.isNotEmpty() && emojiSearch.isBlank()) {
                                    item {
                                        Text("Recents", color = textColorSecondary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp))
                                        androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                                            columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(8),
                                            modifier = Modifier.fillMaxWidth().heightIn(max = 80.dp),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            items(RecentReactions.list.size) { i ->
                                                val e = RecentReactions.list[i]
                                                val mine = pickerMsg.reactions[currentUserId] == e
                                                Box(
                                                    modifier = Modifier.size(36.dp)
                                                        .background(if (mine) primaryColor.copy(alpha = 0.2f) else Color.Transparent, RoundedCornerShape(8.dp))
                                                        .clickable { pickEmoji(e) },
                                                    contentAlignment = Alignment.Center
                                                ) { Text(e, fontSize = 22.sp) }
                                            }
                                        }
                                        Spacer(Modifier.height(10.dp))
                                        HorizontalDivider(color = textColor.copy(alpha = 0.08f))
                                        Spacer(Modifier.height(8.dp))
                                        Text("All Emojis", color = textColorSecondary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp))
                                    }
                                }
                                item {
                                    androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                                        columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(8),
                                        modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        items(filtered.size) { i ->
                                            val (e, _) = filtered[i]
                                            val mine = pickerMsg.reactions[currentUserId] == e
                                            Box(
                                                modifier = Modifier.size(36.dp)
                                                    .background(if (mine) primaryColor.copy(alpha = 0.2f) else Color.Transparent, RoundedCornerShape(8.dp))
                                                    .clickable { pickEmoji(e) },
                                                contentAlignment = Alignment.Center
                                            ) { Text(e, fontSize = 22.sp) }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

// Fullscreen image viewer
        fullscreenImageUrl?.let { imgUrl ->
            androidx.compose.ui.window.Dialog(onDismissRequest = { fullscreenImageUrl = null }, properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black).pointerInput(Unit) { detectTapGestures { fullscreenImageUrl = null } }) {
                    coil.compose.AsyncImage(model = imgUrl, contentDescription = null, modifier = Modifier.fillMaxWidth().align(Alignment.Center), contentScale = androidx.compose.ui.layout.ContentScale.Fit)
                    IconButton(onClick = { fullscreenImageUrl = null }, modifier = Modifier.align(Alignment.TopEnd).padding(top = 40.dp, end = 16.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)) {
                        Icon(Icons.Default.Close, null, tint = Color.White)
                    }
                }
            }
        }

// Message Options — themed bottom sheet
        if (showMessageOptions && selectedMessage != null) {
            val msg = selectedMessage!!
            val isOwn = msg.senderId == currentUserId
            val sheetBg = if (lightMode) Color(0xFFF0F4FF) else Color(0xFF131F35)
            val rowBg = if (lightMode) Color(0xFFF0F4FF) else Color(0xFF1E2D45)
            androidx.compose.ui.window.Dialog(
                onDismissRequest = { showMessageOptions = false; selectedMessage = null },
                properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f))
                        .pointerInput(Unit) { detectTapGestures { showMessageOptions = false; selectedMessage = null } },
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                            .background(sheetBg, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                            .pointerInput(Unit) { detectTapGestures { /* consume taps so they don't dismiss */ } }
                            .navigationBarsPadding()
                            .padding(bottom = 12.dp)
                    ) {
                        // Handle pill
                        Box(modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp), contentAlignment = Alignment.Center) {
                            Box(modifier = Modifier.width(40.dp).height(4.dp).background(textColor.copy(alpha = 0.2f), RoundedCornerShape(2.dp)))
                        }
                        // Reaction emoji bar
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            reactionEmojis.forEach { emoji ->
                                val alreadyReacted = msg.reactions[currentUserId] == emoji
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .background(
                                            if (alreadyReacted) primaryColor.copy(alpha = 0.2f) else (if (lightMode) Color(0xFFE8EDF5) else Color(0xFF1E2D45)),
                                            CircleShape
                                        )
                                        .border(if (alreadyReacted) 2.dp else 0.dp, primaryColor, CircleShape)
                                        .clickable {
                                            val newReactions = msg.reactions.toMutableMap()
                                            if (alreadyReacted) newReactions.remove(currentUserId) else newReactions[currentUserId] = emoji
                                            messages = messages.map { if (it.id == msg.id) it.copy(reactions = newReactions) else it }
                                            val update = if (alreadyReacted)
                                                mapOf("reactions.$currentUserId" to FieldValue.delete())
                                            else
                                                mapOf("reactions.$currentUserId" to emoji)
                                            db.collection("messages").document(msg.id).update(update)
                                            if (!alreadyReacted) RecentReactions.add(emoji)
                                            showMessageOptions = false; selectedMessage = null
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(emoji, fontSize = 21.sp)
                                }
                            }
                            // "+" custom emoji button
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(if (lightMode) Color(0xFFE8EDF5) else Color(0xFF1E2D45), CircleShape)
                                    .border(1.dp, primaryColor.copy(alpha = 0.5f), CircleShape)
                                    .clickable { showEmojiPicker = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Add, null, tint = primaryColor, modifier = Modifier.size(20.dp))
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp), color = textColor.copy(alpha = 0.08f))
                        // Message preview
                        if (msg.message.isNotBlank() || msg.attachmentUrl.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)
                                    .background(rowBg, RoundedCornerShape(12.dp)).padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.width(3.dp).height(36.dp).background(primaryColor, RoundedCornerShape(2.dp)))
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        if (isOwn) "You" else msg.senderName,
                                        color = primaryColor, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        msg.message.ifBlank { if (msg.attachmentType == "image") "Photo" else "Attachment" },
                                        color = textColor.copy(alpha = 0.75f), style = MaterialTheme.typography.bodySmall,
                                        maxLines = 2, overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        // Action rows
                        @Composable
                        fun ActionRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, tint: Color, onClick: () -> Unit) {
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
                                    .padding(horizontal = 20.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.size(38.dp).background(tint.copy(alpha = 0.12f), CircleShape), contentAlignment = Alignment.Center) {
                                    Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
                                }
                                Spacer(Modifier.width(16.dp))
                                Text(label, color = tint, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        ActionRow(Icons.Default.Reply, "Reply", primaryColor) {
                            replyingTo = msg; showMessageOptions = false; selectedMessage = null
                        }
                        if (isOwn && msg.attachmentUrl.isEmpty() && msg.message.isNotBlank()) {
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = textColor.copy(alpha = 0.07f))
                            ActionRow(Icons.Default.Edit, "Edit", textColor) {
                                editingMessage = msg; editText = msg.message; showMessageOptions = false; selectedMessage = null
                            }
                        }
                        if (isOwn) {
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = textColor.copy(alpha = 0.07f))
                            ActionRow(Icons.Default.Delete, "Delete", Color(0xFFE53935)) {
                                messages = messages.filter { it.id != msg.id }
                                db.collection("messages").document(msg.id).delete()
                                showMessageOptions = false; selectedMessage = null
                            }
                        }
                    }
                }
            }
        }
    }
}

// Helper function to get state FBLA social media links

@Composable
fun GroupChatScreen(
    groupId: String,
    onBack: () -> Unit,
    primaryColor: Color,
    bgOpacity: Float,
    isSimplifiedUI: Boolean,
    textSizeMultiplier: Float,
    lightMode: Boolean = false
) {
    val textColor = if (lightMode) Color(0xFF1A1A2E) else Color.White
    val textColorSecondary = if (lightMode) Color(0xFF1A1A2E).copy(alpha = 0.8f) else Color.White.copy(alpha = 0.9f)
    val textColorTertiary = if (lightMode) Color(0xFF1A1A2E).copy(alpha = 0.75f) else Color.White.copy(alpha = 0.85f)
    val bubbleBg = if (lightMode) Color(0xFFE8EEF8) else Color(0xFF1E2D45)
    val myBubbleBg = primaryColor.copy(alpha = 0.85f)
    val sheetBg = if (lightMode) Color(0xFFF0F4FF) else Color(0xFF1A2740)

    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    val currentUid = auth.currentUser?.uid ?: ""

    var group by remember { mutableStateOf<GroupChat?>(null) }
    var messages by remember { mutableStateOf<List<GroupMessage>>(emptyList()) }
    var messageText by remember { mutableStateOf("") }
    var isUploading by remember { mutableStateOf(false) }
    var replyingTo by remember { mutableStateOf<GroupMessage?>(null) }
    var editingMessage by remember { mutableStateOf<GroupMessage?>(null) }
    var editText by remember { mutableStateOf("") }
    var selectedMessage by remember { mutableStateOf<GroupMessage?>(null) }
    var showMessageOptions by remember { mutableStateOf(false) }
    var fullscreenImageUrl by remember { mutableStateOf<String?>(null) }
    var pendingAttachmentUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var pendingAttachmentName by remember { mutableStateOf("") }
    var pendingAttachmentType by remember { mutableStateOf("") }
    var showAttachmentOptions by remember { mutableStateOf(false) }
    var cameraImageUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var groupPolls by remember { mutableStateOf<Map<String, Poll>>(emptyMap()) }
    var showGroupPollDialog by remember { mutableStateOf(false) }
    var groupPollQuestion by remember { mutableStateOf("") }
    var groupPollOptions by remember { mutableStateOf(listOf("", "")) }

    val fileLauncher = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val name = uri.lastPathSegment?.substringAfterLast('/') ?: "file"
            val mime = context.contentResolver.getType(uri) ?: ""
            pendingAttachmentUri = uri; pendingAttachmentName = name
            pendingAttachmentType = if (mime.startsWith("image")) "image" else "file"
        }
    }
    val galleryLauncher = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val name = uri.lastPathSegment?.substringAfterLast('/') ?: "photo.jpg"
            pendingAttachmentUri = uri; pendingAttachmentName = name; pendingAttachmentType = "image"
        }
    }
    val cameraLauncher = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.TakePicture()) { success ->
        if (success && cameraImageUri != null) {
            pendingAttachmentUri = cameraImageUri
            pendingAttachmentName = "photo_${System.currentTimeMillis()}.jpg"
            pendingAttachmentType = "image"
        }
    }

    fun uploadAndRun(uri: android.net.Uri, path: String, onUrl: (String) -> Unit) {
        val ref = FirebaseStorage.getInstance().reference.child(path)
        val stream = try { context.contentResolver.openInputStream(uri) } catch (e: Exception) {
            isUploading = false; Toast.makeText(context, "Can't read file", Toast.LENGTH_SHORT).show(); return
        }
        if (stream == null) { isUploading = false; return }
        ref.putStream(stream).addOnSuccessListener {
            stream.close()
            ref.downloadUrl.addOnSuccessListener { url -> onUrl(url.toString()) }
                .addOnFailureListener { isUploading = false; Toast.makeText(context, "Upload failed", Toast.LENGTH_SHORT).show() }
        }.addOnFailureListener { stream.close(); isUploading = false; Toast.makeText(context, "Upload failed", Toast.LENGTH_SHORT).show() }
    }

    LaunchedEffect(groupId) {
        db.collection("groupChats").document(groupId).addSnapshotListener { snap, _ ->
            group = snap?.toObject(GroupChat::class.java)
        }
        db.collection("groupChats").document(groupId).collection("messages")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.ASCENDING)
            .addSnapshotListener { snap, _ ->
                val newMsgs = snap?.documents?.mapNotNull { it.toObject(GroupMessage::class.java) } ?: emptyList()
                val wasAtBottom = messages.size == newMsgs.size || newMsgs.size > messages.size
                messages = newMsgs
                if (wasAtBottom && newMsgs.isNotEmpty()) {
                    scope.launch { listState.animateScrollToItem(newMsgs.size - 1) }
                }
            }
        db.collection("groupChats").document(groupId).collection("polls")
            .addSnapshotListener { snap, _ ->
                groupPolls = snap?.documents?.mapNotNull { it.toObject(Poll::class.java)?.copy(id = it.id) }
                    ?.associateBy { it.id } ?: emptyMap()
            }
    }

    fun sendMessage(text: String, attachUrl: String = "", attachName: String = "", attachType: String = "") {
        val senderName = group?.memberNames?.get(currentUid) ?: auth.currentUser?.displayName ?: "Unknown"
        val msgRef = db.collection("groupChats").document(groupId).collection("messages").document()
        val now = System.currentTimeMillis()
        val msg = GroupMessage(
            id = msgRef.id, senderId = currentUid, senderName = senderName,
            message = text, timestamp = now,
            attachmentUrl = attachUrl, attachmentName = attachName, attachmentType = attachType,
            replyToId = replyingTo?.id ?: "", replyToText = replyingTo?.message?.take(60) ?: "",
            replyToSender = replyingTo?.senderName ?: ""
        )
        msgRef.set(msg).addOnFailureListener { e ->
            android.util.Log.e("GroupChat", "Message send failed: ${e.message}")
        }
        // Use set+merge so it works even if fields don't exist yet
        db.collection("groupChats").document(groupId).set(
            mapOf("lastMessage" to text.ifEmpty { attachName }, "lastMessageTime" to now, "lastMessageSender" to senderName),
            com.google.firebase.firestore.SetOptions.merge()
        ).addOnFailureListener { e ->
            android.util.Log.e("GroupChat", "Group update failed: ${e.message}")
        }
        replyingTo = null
    }

    val focusManager = LocalFocusManager.current
    val reactionEmojis = remember { listOf("👍", "❤️", "😂", "😮", "😢", "🔥") }
    var showEmojiPicker by remember { mutableStateOf(false) }
    var emojiSearch by remember { mutableStateOf("") }

// Attachment picker sheet
    if (showAttachmentOptions) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showAttachmentOptions = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f))
                    .pointerInput(Unit) { detectTapGestures { showAttachmentOptions = false } },
                contentAlignment = Alignment.BottomCenter
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                        .background(sheetBg, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .pointerInput(Unit) { detectTapGestures { } }
                        .navigationBarsPadding().padding(bottom = 8.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp), contentAlignment = Alignment.Center) {
                        Box(modifier = Modifier.width(40.dp).height(4.dp).background(textColor.copy(alpha = 0.2f), RoundedCornerShape(2.dp)))
                    }
                    Row(modifier = Modifier.fillMaxWidth().clickable {
                        showAttachmentOptions = false
                        galleryLauncher.launch("image/*")
                    }.padding(horizontal = 24.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PhotoLibrary, null, tint = primaryColor, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(16.dp))
                        Text("Camera Roll", color = textColor, style = MaterialTheme.typography.bodyLarge)
                    }
                    Row(modifier = Modifier.fillMaxWidth().clickable {
                        showAttachmentOptions = false
                        val imgFile = java.io.File(context.cacheDir, "camera_${System.currentTimeMillis()}.jpg")
                        val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", imgFile)
                        cameraImageUri = uri
                        cameraLauncher.launch(uri)
                    }.padding(horizontal = 24.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CameraAlt, null, tint = primaryColor, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(16.dp))
                        Text("Take Photo", color = textColor, style = MaterialTheme.typography.bodyLarge)
                    }
                    Row(modifier = Modifier.fillMaxWidth().clickable {
                        showAttachmentOptions = false
                        fileLauncher.launch("*/*")
                    }.padding(horizontal = 24.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AttachFile, null, tint = primaryColor, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(16.dp))
                        Text("Choose File", color = textColor, style = MaterialTheme.typography.bodyLarge)
                    }
                    Row(modifier = Modifier.fillMaxWidth().clickable {
                        showAttachmentOptions = false
                        showGroupPollDialog = true
                    }.padding(horizontal = 24.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Poll, null, tint = primaryColor, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(16.dp))
                        Text("Create Poll", color = textColor, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }

// Message options bottom sheet
    if (showMessageOptions && selectedMessage != null) {
        val msg = selectedMessage!!
        val isOwn = msg.senderId == currentUid
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showMessageOptions = false; selectedMessage = null },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f))
                    .pointerInput(Unit) { detectTapGestures { showMessageOptions = false; selectedMessage = null } },
                contentAlignment = Alignment.BottomCenter
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                        .background(sheetBg, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .pointerInput(Unit) { detectTapGestures { } }
                        .navigationBarsPadding().padding(bottom = 12.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp), contentAlignment = Alignment.Center) {
                        Box(modifier = Modifier.width(40.dp).height(4.dp).background(textColor.copy(alpha = 0.2f), RoundedCornerShape(2.dp)))
                    }
                    // Reaction emoji bar
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        reactionEmojis.forEach { emoji ->
                            val alreadyReacted = msg.reactions[currentUid] == emoji
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(
                                        if (alreadyReacted) primaryColor.copy(alpha = 0.2f) else (if (lightMode) Color(0xFFE8EDF5) else Color(0xFF1E2D45)),
                                        CircleShape
                                    )
                                    .border(if (alreadyReacted) 2.dp else 0.dp, primaryColor, CircleShape)
                                    .clickable {
                                        val update = if (alreadyReacted)
                                            mapOf("reactions.$currentUid" to FieldValue.delete())
                                        else
                                            mapOf("reactions.$currentUid" to emoji)
                                        db.collection("groupChats").document(groupId)
                                            .collection("messages").document(msg.id).update(update)
                                        if (!alreadyReacted) RecentReactions.add(emoji)
                                        showMessageOptions = false; selectedMessage = null
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(emoji, fontSize = 21.sp)
                            }
                        }
                        // "+" custom emoji button
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(if (lightMode) Color(0xFFE8EDF5) else Color(0xFF1E2D45), CircleShape)
                                .border(1.dp, primaryColor.copy(alpha = 0.5f), CircleShape)
                                .clickable { showEmojiPicker = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Add, null, tint = primaryColor, modifier = Modifier.size(20.dp))
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp), color = textColor.copy(alpha = 0.08f))
                    // Message preview
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = if (lightMode) Color(0xFFF0F4FF) else Color(0xFF243554)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(msg.senderName, color = primaryColor, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(2.dp))
                            Text(msg.message.ifEmpty { if (msg.attachmentType == "image") "Image" else msg.attachmentName }.take(80),
                                color = textColorSecondary, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    @Composable
                    fun ActionRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, color: Color = textColor, onClick: () -> Unit) {
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 24.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(16.dp))
                            Text(label, color = color, style = MaterialTheme.typography.bodyLarge.copy(fontSize = (15 * textSizeMultiplier).sp))
                        }
                    }
                    ActionRow(Icons.Default.Reply, "Reply") {
                        replyingTo = msg; showMessageOptions = false; selectedMessage = null
                    }
                    if (isOwn && msg.message.isNotEmpty()) {
                        ActionRow(Icons.Default.Edit, "Edit") {
                            editingMessage = msg; editText = msg.message; showMessageOptions = false; selectedMessage = null
                        }
                    }
                    if (isOwn) {
                        ActionRow(Icons.Default.Delete, "Delete", Color(0xFFE57373)) {
                            db.collection("groupChats").document(groupId).collection("messages").document(msg.id).delete()
                            messages = messages.filter { it.id != msg.id }
                            showMessageOptions = false; selectedMessage = null
                        }
                    }
                }
            }
        }
    }

// Poll creation dialog
    if (showGroupPollDialog) {
        AlertDialog(
            onDismissRequest = { showGroupPollDialog = false; groupPollQuestion = ""; groupPollOptions = listOf("", "") },
            containerColor = if (lightMode) Color(0xFFF0F4FF) else Color(0xFF1A2840),
            title = { Text("Create Poll", color = textColor, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val fieldColors = OutlinedTextFieldDefaults.colors(focusedTextColor = textColor, unfocusedTextColor = textColor, focusedBorderColor = primaryColor, unfocusedBorderColor = textColor.copy(alpha = 0.3f), focusedLabelColor = primaryColor, unfocusedLabelColor = textColor.copy(alpha = 0.6f))
                    OutlinedTextField(value = groupPollQuestion, onValueChange = { if (it.length <= 200) groupPollQuestion = it }, label = { Text("Question") }, modifier = Modifier.fillMaxWidth(), colors = fieldColors)
                    groupPollOptions.forEachIndexed { i, opt ->
                        OutlinedTextField(
                            value = opt,
                            onValueChange = { newVal -> if (newVal.length <= 100) groupPollOptions = groupPollOptions.toMutableList().also { it[i] = newVal } },
                            label = { Text("Option ${i + 1}") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = fieldColors,
                            trailingIcon = {
                                if (groupPollOptions.size > 2) IconButton(onClick = { groupPollOptions = groupPollOptions.toMutableList().also { list -> list.removeAt(i) } }) {
                                    Icon(Icons.Default.Close, null, tint = textColor.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                                }
                            }
                        )
                    }
                    if (groupPollOptions.size < 5) {
                        OutlinedButton(onClick = { groupPollOptions = groupPollOptions + "" }, modifier = Modifier.fillMaxWidth().pressScale(), border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.5f))) {
                            Icon(Icons.Default.Add, null, tint = primaryColor, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp))
                            Text("Add Option", color = primaryColor, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val validOptions = groupPollOptions.filter { it.isNotBlank() }
                    if (groupPollQuestion.isBlank() || validOptions.size < 2) { Toast.makeText(context, "Add a question and at least 2 options", Toast.LENGTH_SHORT).show(); return@Button }
                    val senderName = group?.memberNames?.get(currentUid) ?: "Unknown"
                    val pollId = db.collection("groupChats").document(groupId).collection("polls").document().id
                    db.collection("groupChats").document(groupId).collection("polls").document(pollId)
                        .set(Poll(id = pollId, question = groupPollQuestion.trim(), options = validOptions, createdBy = currentUid, createdByName = senderName, createdAt = System.currentTimeMillis()))
                    val msgRef = db.collection("groupChats").document(groupId).collection("messages").document()
                    val now = System.currentTimeMillis()
                    msgRef.set(GroupMessage(id = msgRef.id, senderId = currentUid, senderName = senderName, message = "📊 ${groupPollQuestion.trim()}", timestamp = now, type = "poll", pollId = pollId))
                    db.collection("groupChats").document(groupId).set(mapOf("lastMessage" to "📊 Poll: ${groupPollQuestion.trim().take(40)}", "lastMessageTime" to now, "lastMessageSender" to senderName), com.google.firebase.firestore.SetOptions.merge())
                    showGroupPollDialog = false; groupPollQuestion = ""; groupPollOptions = listOf("", "")
                }, colors = ButtonDefaults.buttonColors(containerColor = primaryColor)) { Text("Create", color = Color.Black, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { showGroupPollDialog = false; groupPollQuestion = ""; groupPollOptions = listOf("", "") }) { Text("Cancel") } }
        )
    }

// Emoji picker dialog
    if (showEmojiPicker && selectedMessage != null) {
        val pickerMsg = selectedMessage!!
        val allEmojis = remember {
            listOf(
                "😀" to "grinning","😃" to "happy","😄" to "laugh","😁" to "beam","😆" to "haha",
                "😅" to "sweat smile","🤣" to "rolling","😂" to "joy","🙂" to "smile","😉" to "wink",
                "😊" to "blush","😇" to "angel","🥰" to "hearts","😍" to "heart eyes","🤩" to "star eyes",
                "😘" to "kiss","😋" to "yum","😛" to "tongue","😜" to "wink tongue","🤪" to "crazy",
                "🤔" to "thinking","🤗" to "hug","🤫" to "shush","🤐" to "zip","😐" to "neutral",
                "😑" to "expressionless","😶" to "no mouth","😏" to "smirk","😒" to "unamused",
                "🙄" to "eye roll","😬" to "grimace","😔" to "pensive","😪" to "sleepy","😴" to "sleeping",
                "😷" to "mask","🤒" to "sick","🤕" to "injured","🤢" to "nauseous","🤧" to "sneeze",
                "🥵" to "hot","🥶" to "cold","😵" to "dizzy","🤯" to "mind blown","🥳" to "party",
                "😎" to "cool","🤓" to "nerd","🧐" to "monocle","😕" to "confused","😟" to "worried",
                "😮" to "surprised","😲" to "astonished","😳" to "flushed","🥺" to "pleading",
                "😢" to "cry","😭" to "sobbing","😱" to "scream","😡" to "angry","😠" to "mad",
                "🤬" to "cursing","😈" to "devil","💀" to "skull","💩" to "poop","👻" to "ghost",
                "❤️" to "heart","🧡" to "orange heart","💛" to "yellow heart","💚" to "green heart",
                "💙" to "blue heart","💜" to "purple heart","🖤" to "black heart","💔" to "broken heart",
                "💕" to "two hearts","💖" to "sparkling heart","💗" to "growing heart","💓" to "beating heart",
                "👍" to "thumbs up","👎" to "thumbs down","👌" to "ok","✌️" to "peace","🤞" to "fingers crossed",
                "🤘" to "rock on","🤙" to "call me","👋" to "wave","👏" to "clap","🙌" to "raising hands",
                "🙏" to "pray","💪" to "muscle","✍️" to "writing hand",
                "🔥" to "fire","⭐" to "star","🌟" to "glowing star","✨" to "sparkles","💫" to "dizzy",
                "🎉" to "party","🎊" to "confetti","🎈" to "balloon","🎁" to "gift","🏆" to "trophy",
                "🥇" to "gold","🎯" to "bullseye","🎮" to "game","🚀" to "rocket","🌈" to "rainbow",
                "☀️" to "sun","🌙" to "moon","⚡" to "lightning","🌊" to "wave","💯" to "hundred",
                "✅" to "check","❌" to "cross","💡" to "idea","🔑" to "key","📱" to "phone",
                "💻" to "laptop","📚" to "books","🎵" to "music","🎶" to "notes","🍕" to "pizza",
                "🍔" to "burger","🍰" to "cake","☕" to "coffee","🐶" to "dog","🐱" to "cat",
                "🦁" to "lion","🐼" to "panda","🦊" to "fox","🐸" to "frog","🦋" to "butterfly"
            )
        }
        val filtered = remember(emojiSearch) {
            if (emojiSearch.isBlank()) allEmojis
            else allEmojis.filter { it.second.contains(emojiSearch.trim(), ignoreCase = true) }
        }
        fun pickGroupEmoji(emoji: String) {
            val alreadyReacted = pickerMsg.reactions[currentUid] == emoji
            val update = if (alreadyReacted)
                mapOf("reactions.$currentUid" to FieldValue.delete())
            else
                mapOf("reactions.$currentUid" to emoji)
            db.collection("groupChats").document(groupId).collection("messages").document(pickerMsg.id).update(update)
            if (!alreadyReacted) RecentReactions.add(emoji)
            showEmojiPicker = false; emojiSearch = ""; showMessageOptions = false; selectedMessage = null
        }
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showEmojiPicker = false; emojiSearch = "" },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
                Card(
                    modifier = Modifier.fillMaxWidth(0.92f).fillMaxHeight(0.72f),
                    colors = CardDefaults.cardColors(containerColor = if (lightMode) Color.White else Color(0xFF1A2740)),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Pick a Reaction", color = textColor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            IconButton(onClick = { showEmojiPicker = false; emojiSearch = "" }, modifier = Modifier.size(32.dp).pressScale()) {
                                Icon(Icons.Default.Close, "Close", tint = textColorTertiary)
                            }
                        }
                        val myCurrentGroupReaction = pickerMsg.reactions[currentUid]
                        if (myCurrentGroupReaction != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                                    .background(Color(0xFFE53935).copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                                    .clickable {
                                        val newReactions = pickerMsg.reactions.toMutableMap().also { it.remove(currentUid) }
                                        messages = messages.map { if (it.id == pickerMsg.id) it.copy(reactions = newReactions) else it }
                                        db.collection("groupChats").document(groupId).collection("messages").document(pickerMsg.id)
                                            .update(mapOf("reactions.$currentUid" to FieldValue.delete()))
                                        showEmojiPicker = false; emojiSearch = ""; showMessageOptions = false; selectedMessage = null
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(myCurrentGroupReaction, fontSize = 20.sp)
                                Spacer(Modifier.width(8.dp))
                                Text("Remove your reaction", color = Color(0xFFE53935), style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                                Icon(Icons.Default.Close, null, tint = Color(0xFFE53935), modifier = Modifier.size(16.dp))
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        OutlinedTextField(
                            value = emojiSearch,
                            onValueChange = { emojiSearch = it },
                            placeholder = { Text("Search emoji…", color = textColorTertiary) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryColor, unfocusedBorderColor = textColor.copy(alpha = 0.2f),
                                focusedTextColor = textColor, unfocusedTextColor = textColor
                            ),
                            leadingIcon = { Icon(Icons.Default.Search, null, tint = textColorTertiary) },
                            trailingIcon = {
                                if (emojiSearch.isNotEmpty()) IconButton(onClick = { emojiSearch = "" }) {
                                    Icon(Icons.Default.Close, null, tint = textColorTertiary, modifier = Modifier.size(18.dp))
                                }
                            }
                        )
                        Spacer(Modifier.height(10.dp))
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            if (RecentReactions.list.isNotEmpty() && emojiSearch.isBlank()) {
                                item {
                                    Text("Recents", color = textColorSecondary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp))
                                    androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                                        columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(8),
                                        modifier = Modifier.fillMaxWidth().heightIn(max = 80.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        items(RecentReactions.list.size) { i ->
                                            val e = RecentReactions.list[i]
                                            val mine = pickerMsg.reactions[currentUid] == e
                                            Box(
                                                modifier = Modifier.size(36.dp)
                                                    .background(if (mine) primaryColor.copy(alpha = 0.2f) else Color.Transparent, RoundedCornerShape(8.dp))
                                                    .clickable { pickGroupEmoji(e) },
                                                contentAlignment = Alignment.Center
                                            ) { Text(e, fontSize = 22.sp) }
                                        }
                                    }
                                    Spacer(Modifier.height(10.dp))
                                    HorizontalDivider(color = textColor.copy(alpha = 0.08f))
                                    Spacer(Modifier.height(8.dp))
                                    Text("All Emojis", color = textColorSecondary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp))
                                }
                            }
                            item {
                                androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                                    columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(8),
                                    modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    items(filtered.size) { i ->
                                        val (e, _) = filtered[i]
                                        val mine = pickerMsg.reactions[currentUid] == e
                                        Box(
                                            modifier = Modifier.size(36.dp)
                                                .background(if (mine) primaryColor.copy(alpha = 0.2f) else Color.Transparent, RoundedCornerShape(8.dp))
                                                .clickable { pickGroupEmoji(e) },
                                            contentAlignment = Alignment.Center
                                        ) { Text(e, fontSize = 22.sp) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

// Fullscreen image
    if (fullscreenImageUrl != null) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { fullscreenImageUrl = null },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black).pointerInput(Unit) { detectTapGestures { fullscreenImageUrl = null } }) {
                AsyncImage(model = fullscreenImageUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
            }
        }
    }

    var showGroupMenu by remember { mutableStateOf(false) }
    var showLeaveConfirm by remember { mutableStateOf(false) }

// Leave group confirmation dialog
    if (showLeaveConfirm) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showLeaveConfirm = false },
            title = { Text("Leave Group?", color = textColor, fontWeight = FontWeight.Bold) },
            text = { Text("You'll be removed from \"${group?.name ?: "this group"}\" and won't receive new messages.", color = textColorSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        showLeaveConfirm = false
                        val g = group ?: return@Button
                        val newMemberIds = g.memberIds.filter { it != currentUid }
                        val newMemberNames = g.memberNames.toMutableMap().also { it.remove(currentUid) }
                        db.collection("groupChats").document(groupId).update(
                            mapOf("memberIds" to newMemberIds, "memberNames" to newMemberNames)
                        ).addOnSuccessListener { onBack() }
                         .addOnFailureListener { e ->
                             android.util.Log.e("GroupChat", "Leave failed: ${e.message}")
                             Toast.makeText(context, "Failed to leave group", Toast.LENGTH_SHORT).show()
                         }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                ) { Text("Leave", color = Color.White, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveConfirm = false }) { Text("Cancel", color = textColorSecondary) }
            },
            containerColor = if (lightMode) Color.White else Color(0xFF1A2740)
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize().background(if (lightMode) Color(0xFFF0F4FF) else Color(0xFF0A1628)))
        if (!lightMode) Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = bgOpacity)))

        Column(modifier = Modifier.fillMaxSize()) {
// Header
            Surface(modifier = Modifier.fillMaxWidth(), color = if (lightMode) Color(0xFFF0F4FF) else Color(0xFF1A1A1A), shadowElevation = 4.dp) {
                Row(modifier = Modifier.fillMaxWidth().padding(top = 40.dp, start = 4.dp, end = 4.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    BackButton(onClick = onBack, tint = textColor)
                    Box(modifier = Modifier.size(42.dp).clip(CircleShape).background(primaryColor.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Group, null, tint = primaryColor, modifier = Modifier.size(24.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(group?.name ?: "Group Chat", color = textColor, fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = (MaterialTheme.typography.titleMedium.fontSize.value * textSizeMultiplier).sp))
                        Text("${group?.memberIds?.size ?: 0} members", color = textColorSecondary,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = (11 * textSizeMultiplier).sp))
                    }
                    Box {
                        IconButton(onClick = { showGroupMenu = true }) {
                            Icon(Icons.Default.MoreVert, "Options", tint = textColor)
                        }
                        androidx.compose.material3.DropdownMenu(
                            expanded = showGroupMenu,
                            onDismissRequest = { showGroupMenu = false },
                            containerColor = if (lightMode) Color.White else Color(0xFF1A2740)
                        ) {
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text("Leave Group", color = Color(0xFFE53935), fontWeight = FontWeight.SemiBold) },
                                leadingIcon = { Icon(Icons.Default.ExitToApp, null, tint = Color(0xFFE53935)) },
                                onClick = { showGroupMenu = false; showLeaveConfirm = true }
                            )
                        }
                    }
                }
            }

// Messages
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth().pointerInput(Unit) { detectTapGestures { focusManager.clearFocus() } },
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(messages.size) { idx ->
                    val msg = messages[idx]
                    val isOwn = msg.senderId == currentUid
                    if (msg.type == "poll") {
                        val poll = groupPolls[msg.pollId]
                        if (poll == null && msg.pollId.isNotEmpty()) {
                            LaunchedEffect(msg.pollId) {
                                try {
                                    val doc = db.collection("groupChats").document(groupId)
                                        .collection("polls").document(msg.pollId).get().await()
                                    doc.toObject(Poll::class.java)?.copy(id = doc.id)?.let { fetched ->
                                        groupPolls = groupPolls + (fetched.id to fetched)
                                    }
                                } catch (e: Exception) { /* ignore */ }
                            }
                        }
                        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = if (isOwn) Alignment.End else Alignment.Start) {
                            if (!isOwn) Text(msg.senderName, color = primaryColor, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp, bottom = 2.dp))
                            if (poll != null) {
                                PollCard(poll = poll, currentUserId = currentUid, onVote = { optionIndex ->
                                    db.collection("groupChats").document(groupId).collection("polls").document(poll.id)
                                        .update(mapOf("votes.$currentUid" to optionIndex))
                                }, primaryColor = primaryColor, lightMode = lightMode, textSizeMultiplier = textSizeMultiplier, modifier = Modifier.widthIn(max = 320.dp))
                            } else {
                                Card(modifier = Modifier.widthIn(max = 320.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = if (lightMode) Color(0xFFE8EDF5) else Color.White.copy(alpha = 0.1f))) {
                                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = primaryColor)
                                        Text("Loading poll…", color = textColorSecondary, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                            val pollSdf = remember { java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault()) }
                            Text(pollSdf.format(java.util.Date(msg.timestamp)), color = textColorTertiary, style = MaterialTheme.typography.labelSmall.copy(fontSize = (10 * textSizeMultiplier).sp), modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                        }
                    } else {
                    val groupedReactions = msg.reactions.entries.groupBy({ it.value }, { it.key })
                    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = if (isOwn) Alignment.End else Alignment.Start) {
                        if (!isOwn) {
                            Text(msg.senderName, color = primaryColor, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp))
                        }
                        Box(
                            modifier = Modifier.widthIn(max = 280.dp)
                                .pointerInput(msg.id) { detectTapGestures(onLongPress = { selectedMessage = msg; showMessageOptions = true }) }
                        ) {
                            Column(
                                modifier = Modifier.background(if (isOwn) myBubbleBg else bubbleBg, RoundedCornerShape(
                                    topStart = 16.dp, topEnd = 16.dp,
                                    bottomStart = if (isOwn) 16.dp else 4.dp,
                                    bottomEnd = if (isOwn) 4.dp else 16.dp
                                )).padding(10.dp)
                            ) {
// Reply preview
                                if (msg.replyToId.isNotEmpty()) {
                                    Box(modifier = Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.12f), RoundedCornerShape(6.dp)).padding(6.dp)) {
                                        Column {
                                            Text(msg.replyToSender, color = primaryColor, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                            Text(msg.replyToText, color = textColor.copy(alpha = 0.7f), style = MaterialTheme.typography.bodySmall, maxLines = 2)
                                        }
                                    }
                                    Spacer(Modifier.height(4.dp))
                                }
// Attachment
                                if (msg.attachmentType == "link") {
                                    val linkUrl = msg.attachmentUrl
                                    val domain = msg.attachmentName.ifEmpty { urlDomain(linkUrl) }
                                    val gcContentColor = if (isOwn) Color.Black else textColor
                                    val domainColor = when {
                                        domain.contains("instagram") -> Color(0xFF833AB4)
                                        domain.contains("youtube") -> Color(0xFFFF0000)
                                        domain.contains("tiktok") -> Color(0xFF010101)
                                        domain.contains("twitter") || domain.contains("x.com") -> Color(0xFF1DA1F2)
                                        domain.contains("facebook") -> Color(0xFF1877F2)
                                        else -> primaryColor
                                    }
                                    Column(modifier = Modifier.clickable {
                                        try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(linkUrl))) } catch (e: Exception) {}
                                    }) {
                                        Box(modifier = Modifier.fillMaxWidth().height(5.dp).background(domainColor))
                                        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                                Box(modifier = Modifier.size(14.dp).clip(RoundedCornerShape(3.dp)).background(domainColor))
                                                Text(domain, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp), color = gcContentColor.copy(alpha = 0.7f))
                                            }
                                            Text(msg.message.ifEmpty { linkUrl }, style = MaterialTheme.typography.bodySmall.copy(fontSize = (12 * textSizeMultiplier).sp), color = gcContentColor, maxLines = 3, overflow = TextOverflow.Ellipsis)
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Launch, null, tint = domainColor, modifier = Modifier.size(11.dp))
                                                Spacer(Modifier.width(3.dp))
                                                Text("Open", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp), color = domainColor)
                                            }
                                        }
                                    }
                                } else if (msg.attachmentType == "article") {
                                    val articleImageUrl = msg.message.substringBefore("\n")
                                    val articleSummary = msg.message.substringAfter("\n", "")
                                    val articleContentColor = if (isOwn) Color.Black else textColor
                                    Column(modifier = Modifier.clickable {
                                        try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(msg.attachmentUrl))) } catch (e: Exception) {}
                                    }) {
                                        if (articleImageUrl.startsWith("http")) {
                                            AsyncImage(model = articleImageUrl, contentDescription = null, contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)))
                                        }
                                        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                            Text("FBLA NEWS", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontSize = 9.sp), color = articleContentColor.copy(alpha = 0.6f))
                                            Text(msg.attachmentName, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = (13 * textSizeMultiplier).sp), color = articleContentColor, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                            if (articleSummary.isNotEmpty()) Text(articleSummary, style = MaterialTheme.typography.bodySmall.copy(fontSize = (11 * textSizeMultiplier).sp), color = articleContentColor.copy(alpha = 0.75f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("Read Article", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp), color = articleContentColor.copy(alpha = 0.8f))
                                                Spacer(Modifier.width(2.dp))
                                                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = articleContentColor.copy(alpha = 0.8f), modifier = Modifier.size(10.dp))
                                            }
                                        }
                                    }
                                } else if (msg.attachmentUrl.isNotEmpty() && msg.attachmentType == "image") {
                                    AsyncImage(model = msg.attachmentUrl, contentDescription = null,
                                        modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp).clip(RoundedCornerShape(8.dp))
                                            .pointerInput(Unit) { detectTapGestures { fullscreenImageUrl = msg.attachmentUrl } },
                                        contentScale = ContentScale.Crop)
                                    if (msg.message.isNotEmpty()) Spacer(Modifier.height(4.dp))
                                } else if (msg.attachmentUrl.isNotEmpty()) {
                                    val fileColor = if (isOwn) Color.Black else primaryColor
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .pointerInput(msg.id) {
                                                detectTapGestures(
                                                    onTap = {
                                                        try {
                                                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(msg.attachmentUrl))
                                                            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                                            context.startActivity(android.content.Intent.createChooser(intent, "Open file"))
                                                        } catch (e: Exception) {
                                                            Toast.makeText(context, "No app to open this file", Toast.LENGTH_SHORT).show()
                                                        }
                                                    },
                                                    onLongPress = { selectedMessage = msg; showMessageOptions = true }
                                                )
                                            }
                                            .padding(4.dp)
                                    ) {
                                        Icon(Icons.Default.AttachFile, null, tint = fileColor, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text(msg.attachmentName, color = fileColor, style = MaterialTheme.typography.bodySmall)
                                        Spacer(Modifier.width(4.dp))
                                        Icon(Icons.Default.Download, null, tint = fileColor.copy(alpha = 0.7f), modifier = Modifier.size(12.dp))
                                    }
                                    if (msg.message.isNotEmpty()) Spacer(Modifier.height(4.dp))
                                }
                                if (msg.message.isNotEmpty()) {
                                    Text(msg.message, color = if (isOwn) Color.Black else textColor,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = (14 * textSizeMultiplier).sp),
                                        softWrap = true)
                                }
                            }
                        }
// Reaction pills
                        if (groupedReactions.isNotEmpty()) {
                            Row(
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                groupedReactions.forEach { (emoji, uids) ->
                                    val iMine = currentUid in uids
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                if (iMine) primaryColor.copy(alpha = 0.25f) else (if (lightMode) Color(0xFFE8EDF5) else Color(0xFF1E2D45)),
                                                RoundedCornerShape(12.dp)
                                            )
                                            .border(1.dp, if (iMine) primaryColor else Color.Transparent, RoundedCornerShape(12.dp))
                                            .clickable {
                                                val update = if (iMine)
                                                    mapOf("reactions.$currentUid" to FieldValue.delete())
                                                else
                                                    mapOf("reactions.$currentUid" to emoji)
                                                db.collection("groupChats").document(groupId)
                                                    .collection("messages").document(msg.id).update(update)
                                            }
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text("$emoji ${uids.size}", style = MaterialTheme.typography.labelSmall, color = if (iMine) primaryColor else textColor)
                                    }
                                }
                            }
                        }
                        Row(modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                            val sdf = remember { java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault()) }
                            Text(sdf.format(java.util.Date(msg.timestamp)), color = textColorTertiary, style = MaterialTheme.typography.labelSmall.copy(fontSize = (10 * textSizeMultiplier).sp))
                            if (msg.edited) {
                                Spacer(Modifier.width(4.dp))
                                Text("edited", color = textColorTertiary, style = MaterialTheme.typography.labelSmall.copy(fontSize = (9 * textSizeMultiplier).sp))
                            }
                        }
                    }
                    } // closes else (non-poll message)
                }
            }

// Reply bar
            if (replyingTo != null) {
                Row(modifier = Modifier.fillMaxWidth().background(if (lightMode) Color(0xFFF0F4FF) else Color(0xFF1A2740)).padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Reply, null, tint = primaryColor, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(replyingTo!!.senderName, color = primaryColor, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Text(replyingTo!!.message.take(50), color = textColorSecondary, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                    }
                    IconButton(onClick = { replyingTo = null }, modifier = Modifier.size(32.dp).pressScale()) {
                        Icon(Icons.Default.Close, null, tint = textColorTertiary, modifier = Modifier.size(18.dp))
                    }
                }
            }

// Attachment preview
            if (pendingAttachmentUri != null) {
                Row(modifier = Modifier.fillMaxWidth().background(if (lightMode) Color(0xFFF0F4FF) else Color(0xFF1A2740)).padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (pendingAttachmentType == "image") {
                        AsyncImage(model = pendingAttachmentUri, contentDescription = null, modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                    } else {
                        Icon(Icons.Default.AttachFile, null, tint = primaryColor, modifier = Modifier.size(24.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(pendingAttachmentName, color = textColor, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f), maxLines = 1)
                    IconButton(onClick = { pendingAttachmentUri = null; pendingAttachmentName = ""; pendingAttachmentType = "" }, modifier = Modifier.size(32.dp).pressScale()) {
                        Icon(Icons.Default.Close, null, tint = textColorTertiary, modifier = Modifier.size(18.dp))
                    }
                }
            }

// Input bar
            Surface(modifier = Modifier.fillMaxWidth(), color = if (lightMode) Color(0xFFF0F4FF) else Color(0xFF131F35), shadowElevation = 8.dp) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp).navigationBarsPadding(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { showAttachmentOptions = true }, modifier = Modifier.size(40.dp).pressScale()) {
                        Icon(Icons.Default.Add, "Attach", tint = primaryColor, modifier = Modifier.size(22.dp))
                    }
                    val inputBg = if (lightMode) Color(0xFFF0F4FF) else Color(0xFF1E2D45)
                    if (editingMessage != null) {
                        OutlinedTextField(
                            value = editText,
                            onValueChange = { if (it.length <= 1000) editText = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Edit message...", color = textColorTertiary) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryColor, unfocusedBorderColor = Color.Transparent,
                                focusedContainerColor = inputBg, unfocusedContainerColor = inputBg,
                                focusedTextColor = textColor, unfocusedTextColor = textColor
                            ),
                            shape = RoundedCornerShape(24.dp), maxLines = 4
                        )
                        Spacer(Modifier.width(6.dp))
                        IconButton(onClick = {
                            val mid = editingMessage!!.id; val newText = editText.trim()
                            messages = messages.map { if (it.id == mid) it.copy(message = newText, edited = true) else it }
                            db.collection("groupChats").document(groupId).collection("messages").document(mid)
                                .update(mapOf("message" to newText, "edited" to true))
                            editingMessage = null; editText = ""
                        }, modifier = Modifier.size(44.dp).background(primaryColor, CircleShape)) {
                            Icon(Icons.Default.Check, "Save", tint = Color.Black, modifier = Modifier.size(20.dp))
                        }
                        IconButton(onClick = { editingMessage = null; editText = "" }, modifier = Modifier.size(40.dp).pressScale()) {
                            Icon(Icons.Default.Close, "Cancel", tint = textColorTertiary, modifier = Modifier.size(20.dp))
                        }
                    } else {
                        OutlinedTextField(
                            value = messageText,
                            onValueChange = { if (it.length <= 1000) messageText = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Message...", color = textColorTertiary) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryColor, unfocusedBorderColor = Color.Transparent,
                                focusedContainerColor = inputBg, unfocusedContainerColor = inputBg,
                                focusedTextColor = textColor, unfocusedTextColor = textColor
                            ),
                            shape = RoundedCornerShape(24.dp), maxLines = 4
                        )
                        Spacer(Modifier.width(6.dp))
                        val canSend = (messageText.isNotBlank() || pendingAttachmentUri != null) && !isUploading
                        IconButton(
                            onClick = {
                                if (!canSend) return@IconButton
                                val text = messageText.trim()
                                val attachUri = pendingAttachmentUri
                                val attachName = pendingAttachmentName
                                val attachType = pendingAttachmentType
                                messageText = ""; pendingAttachmentUri = null; pendingAttachmentName = ""; pendingAttachmentType = ""
                                if (attachUri != null) {
                                    isUploading = true
                                    val msgRef = db.collection("groupChats").document(groupId).collection("messages").document()
                                    uploadAndRun(attachUri, "groupChats/$groupId/messages/${msgRef.id}/$attachName") { url ->
                                        sendMessage("", url, attachName, attachType)
                                        isUploading = false
                                        if (text.isNotEmpty()) sendMessage(text)
                                    }
                                } else {
                                    sendMessage(text)
                                }
                            },
                            enabled = canSend,
                            modifier = Modifier.size(48.dp).background(if (canSend) primaryColor else Color.Transparent, CircleShape)
                        ) {
                            if (isUploading) androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.Black, strokeWidth = 2.dp)
                            else Icon(Icons.AutoMirrored.Filled.Send, "Send", tint = if (canSend) Color.Black else textColor.copy(alpha = 0.4f), modifier = Modifier.size(22.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PollCard(
    poll: Poll,
    currentUserId: String,
    onVote: (Int) -> Unit,
    primaryColor: Color,
    lightMode: Boolean,
    textSizeMultiplier: Float,
    modifier: Modifier = Modifier
) {
    val textColor = if (lightMode) Color(0xFF1A1A2E) else Color.White
    val textColorSecondary = if (lightMode) Color(0xFF1A1A2E).copy(alpha = 0.7f) else Color.White.copy(alpha = 0.8f)
    val cardBg = if (lightMode) Color(0xFFF0F4FF) else Color(0xFF1E2D45)
    val totalVotes = poll.votes.size
    val myVote: Long? = poll.votes[currentUserId]
    val hasVoted = myVote != null
    val db = FirebaseFirestore.getInstance()

    // uid -> pfpUrl for all voters
    var voterProfiles by remember(poll.id) { mutableStateOf<Map<String, String>>(emptyMap()) }
    val voterUids = poll.votes.keys.toSet()
    LaunchedEffect(voterUids) {
        voterUids.filter { it !in voterProfiles }.forEach { uid ->
            db.collection("users").document(uid).get().addOnSuccessListener { doc ->
                voterProfiles = voterProfiles + (uid to (doc.getString("pfpUrl") ?: ""))
            }
        }
    }

    Card(modifier = modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = cardBg), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(32.dp).background(primaryColor.copy(alpha = 0.15f), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Poll, null, tint = primaryColor, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(poll.createdByName, color = textColor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                    Text("Poll", color = primaryColor, style = MaterialTheme.typography.labelSmall)
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(poll.question, color = textColor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge.copy(fontSize = (15 * textSizeMultiplier).sp))
            Spacer(Modifier.height(12.dp))
            poll.options.forEachIndexed { index, option ->
                val votersForOption = poll.votes.entries.filter { it.value == index.toLong() }.map { it.key }
                val voteCount = votersForOption.size
                val percent = if (totalVotes > 0) voteCount.toFloat() / totalVotes else 0f
                val isMyVote = myVote == index.toLong()
                val maxAvatars = 5
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Box(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                            .border(1.dp, if (isMyVote) primaryColor else textColor.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                            .clickable(enabled = !poll.closed) { onVote(index) }
                    ) {
                        if (hasVoted) {
                            Box(modifier = Modifier.fillMaxWidth(percent.coerceAtLeast(0.02f)).height(44.dp).background(primaryColor.copy(alpha = if (isMyVote) 0.18f else 0.08f)))
                        }
                        Row(modifier = Modifier.fillMaxWidth().height(44.dp).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(option, color = textColor, style = MaterialTheme.typography.bodyMedium.copy(fontSize = (13 * textSizeMultiplier).sp), modifier = Modifier.weight(1f))
                            if (hasVoted) {
                                Text("${(percent * 100).toInt()}%", color = if (isMyVote) primaryColor else textColorSecondary, fontWeight = if (isMyVote) FontWeight.Bold else FontWeight.Normal, style = MaterialTheme.typography.bodySmall)
                                if (isMyVote) { Spacer(Modifier.width(6.dp)); Icon(Icons.Default.CheckCircle, null, tint = primaryColor, modifier = Modifier.size(16.dp)) }
                            }
                        }
                    }
                    if (hasVoted && votersForOption.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy((-6).dp)) {
                            votersForOption.take(maxAvatars).forEach { uid ->
                                val pfp = voterProfiles[uid]
                                Box(
                                    modifier = Modifier.size(22.dp).clip(CircleShape)
                                        .border(1.5.dp, cardBg, CircleShape)
                                ) {
                                    if (!pfp.isNullOrEmpty()) {
                                        AsyncImage(model = pfp, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                    } else {
                                        Box(modifier = Modifier.fillMaxSize().background(primaryColor.copy(alpha = 0.4f)), contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.Person, null, tint = Color.White, modifier = Modifier.size(12.dp))
                                        }
                                    }
                                }
                            }
                            if (votersForOption.size > maxAvatars) {
                                Spacer(Modifier.width(10.dp))
                                Text("+${votersForOption.size - maxAvatars}", color = textColorSecondary, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "$totalVotes vote${if (totalVotes != 1) "s" else ""}${if (poll.closed) " · Closed" else ""}",
                color = textColorSecondary, style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
