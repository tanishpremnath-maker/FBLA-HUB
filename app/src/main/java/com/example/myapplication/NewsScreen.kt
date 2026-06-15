package com.example.myapplication

import androidx.hilt.navigation.compose.hiltViewModel
import com.example.myapplication.ui.news.NewsViewModel
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
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
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.storage.FirebaseStorage
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.Request
import androidx.core.content.FileProvider
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import java.io.File
import java.io.FileOutputStream
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
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.util.concurrent.Executors
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned


@Composable
fun NewsScreen(primaryColor: Color, bgOpacity: Float, isSimplifiedUI: Boolean, textSizeMultiplier: Float, lightMode: Boolean = false, viewModel: NewsViewModel = hiltViewModel()) {
    val context = LocalContext.current

    val reduceMotion = LocalReduceMotion.current
    val textColor = if (lightMode) Color(0xFF1A1A2E) else Color.White
    val textColorSecondary = if (lightMode) Color(0xFF1A1A2E).copy(alpha = 0.8f) else Color.White.copy(alpha = 0.9f)
    val textColorTertiary = if (lightMode) Color(0xFF1A1A2E).copy(alpha = 0.75f) else Color.White.copy(alpha = 0.85f)

    // All news data + state now lives in NewsViewModel (MVVM). The screen renders from uiState.
    val state by viewModel.uiState.collectAsState()
    val allTags = viewModel.allTags
    val currentUid = viewModel.currentUid
    val newsList = state.articles
    val isLoading = state.isLoading
    val lastFetchTime = state.lastFetchTime
    val searchQuery = state.searchQuery
    val selectedTag = state.selectedTag
    val connections = state.connections
    val isLoadingConnections = state.isLoadingConnections
    val displayList = state.displayList

    // Local UI-only state for the share dialogs.
    var shareDialogArticle by remember { mutableStateOf<NewsArticle?>(null) }
    var showMemberPicker by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
// Background — white in light mode, dark blue in dark mode
        Box(modifier = Modifier.fillMaxSize().background(
            if (lightMode) Color(0xFFF0F4FF) else Color(0xFF0A1628)
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
                                        viewModel.refresh {
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

                    Spacer(modifier = Modifier.height(16.dp))

// Search bar
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (lightMode) Color(0xFFF0F4FF) else Color.White.copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null, tint = textColorSecondary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { viewModel.setSearchQuery(it) },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    color = textColor,
                                    fontSize = (14 * textSizeMultiplier).sp
                                ),
                                cursorBrush = SolidColor(primaryColor),
                                modifier = Modifier.weight(1f),
                                decorationBox = { inner ->
                                    if (searchQuery.isEmpty()) {
                                        Text("Search articles...", color = textColorSecondary, fontSize = (14 * textSizeMultiplier).sp)
                                    }
                                    inner()
                                }
                            )
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setSearchQuery("") }, modifier = Modifier.size(20.dp).pressScale()) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = textColorSecondary, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

// Filter chips
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(allTags) { tag ->
                            val active = selectedTag == tag
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (active) primaryColor else Color.Transparent,
                                border = BorderStroke(1.dp, if (active) primaryColor else primaryColor.copy(alpha = 0.35f)),
                                modifier = Modifier.clickable { viewModel.setSelectedTag(tag) }
                            ) {
                                Text(
                                    text = tag,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = (11 * textSizeMultiplier).sp
                                    ),
                                    color = if (active) Color.Black else textColorSecondary,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                itemsIndexed(displayList) { index, article ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize(animationSpec = if (reduceMotion) snap() else spring()),
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
                        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                            val bannerColor = when (article.tag) {
                                "CONFERENCE" -> Color(0xFF1A3A6B)
                                "OPPORTUNITY" -> Color(0xFF7B5E00)
                                "HS" -> Color(0xFF0D3B5E)
                                "ML" -> Color(0xFF3B1A6B)
                                "PBL" -> Color(0xFF0D5E4A)
                                else -> primaryColor.copy(alpha = 0.85f)
                            }

// Left: text content
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = primaryColor
                                    ) {
                                        Text(
                                            text = article.tag,
                                            color = Color.Black,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = (9 * textSizeMultiplier).sp,
                                                letterSpacing = 0.5.sp
                                            ),
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.weight(1f))
                                    Text(
                                        text = article.date,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = (10 * textSizeMultiplier).sp),
                                        color = textColorSecondary
                                    )
                                }
                                Text(
                                    text = article.title,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = (14 * textSizeMultiplier).sp,
                                        lineHeight = (19 * textSizeMultiplier).sp
                                    ),
                                    color = textColor,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = article.summary,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = (12 * textSizeMultiplier).sp,
                                        lineHeight = (17 * textSizeMultiplier).sp
                                    ),
                                    color = textColorSecondary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Read Article",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = (11 * textSizeMultiplier).sp
                                        ),
                                        color = primaryColor
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = primaryColor, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.weight(1f))
                                    Icon(
                                        Icons.Default.Share,
                                        contentDescription = "Share",
                                        tint = textColorSecondary,
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clickable { shareDialogArticle = article }
                                    )
                                }
                            }

// Right square thumbnail
                            if (article.imageUrl.isNotEmpty()) {
                                AsyncImage(
                                    model = article.imageUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .width(90.dp)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp))
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .width(90.dp)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp))
                                        .background(Brush.verticalGradient(listOf(bannerColor, bannerColor.copy(alpha = 0.55f)))),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = article.tag.take(2),
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Black,
                                            fontSize = (18 * textSizeMultiplier).sp
                                        ),
                                        color = Color.White.copy(alpha = 0.85f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

// Share options dialog
        shareDialogArticle?.let { article ->
            if (!showMemberPicker) {
                Dialog(onDismissRequest = { shareDialogArticle = null }) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (lightMode) Color.White else Color(0xFF1A2744),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = "Share Article",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = textColor
                            )
                            Text(
                                text = article.title,
                                style = MaterialTheme.typography.bodySmall,
                                color = textColorSecondary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (lightMode) Color(0xFFF0F4FF) else Color.White.copy(alpha = 0.07f),
                                modifier = Modifier.fillMaxWidth().clickable {
                                    shareArticle(context, article.title, article.url)
                                    shareDialogArticle = null
                                }
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(16.dp)) {
                                    Icon(Icons.Default.Share, contentDescription = null, tint = primaryColor, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text("Share via Android", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = textColor)
                                        Text("Copy, message, email, and more", style = MaterialTheme.typography.bodySmall, color = textColorSecondary)
                                    }
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (lightMode) Color(0xFFF0F4FF) else Color.White.copy(alpha = 0.07f),
                                modifier = Modifier.fillMaxWidth().clickable {
                                    showMemberPicker = true
                                    viewModel.loadConnections()
                                }
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(16.dp)) {
                                    Icon(Icons.Default.Message, contentDescription = null, tint = primaryColor, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text("Send to FBLA Member", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = textColor)
                                        Text("Share directly in FBLA Hub messages", style = MaterialTheme.typography.bodySmall, color = textColorSecondary)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
// Member picker dialog
                Dialog(onDismissRequest = { showMemberPicker = false; shareDialogArticle = null; viewModel.clearConnections() }) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (lightMode) Color.White else Color(0xFF1A2744),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { showMemberPicker = false }, modifier = Modifier.size(32.dp).pressScale()) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = textColor, modifier = Modifier.size(18.dp))
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Send to Member", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = textColor)
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            if (isLoadingConnections) {
                                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = primaryColor, modifier = Modifier.size(32.dp))
                                }
                            } else if (connections.isEmpty()) {
                                Text("No connections yet. Connect with FBLA members to share articles.", style = MaterialTheme.typography.bodyMedium, color = textColorSecondary)
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    connections.forEach { member ->
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (lightMode) Color(0xFFF0F4FF) else Color.White.copy(alpha = 0.07f),
                                            modifier = Modifier.fillMaxWidth().clickable {
                                                if (currentUid != null) {
                                                    viewModel.sendArticleToMember(member, article)
                                                    Toast.makeText(context, "Sent to ${member.name}", Toast.LENGTH_SHORT).show()
                                                }
                                                showMemberPicker = false
                                                shareDialogArticle = null
                                            }
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
                                                if (member.pfpUrl.isNotEmpty()) {
                                                    AsyncImage(model = member.pfpUrl, contentDescription = null, contentScale = ContentScale.Crop,
                                                        modifier = Modifier.size(36.dp).clip(CircleShape))
                                                } else {
                                                    Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(primaryColor), contentAlignment = Alignment.Center) {
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


fun shareArticle(context: Context, title: String, url: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, "$title\n$url")
    }
    context.startActivity(Intent.createChooser(intent, "Share article"))
}
