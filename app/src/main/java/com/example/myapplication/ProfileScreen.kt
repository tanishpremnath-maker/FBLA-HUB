package com.example.myapplication

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
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
import java.util.*
import androidx.core.content.FileProvider
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import java.io.File
import java.io.FileOutputStream
import androidx.camera.core.Preview
import androidx.compose.ui.geometry.Rect


@Composable
fun LoggedOutProfileScreen(onLoginClick: () -> Unit, onTutorialClick: () -> Unit = {}, onSettingsClick: () -> Unit = {}, primaryColor: Color, bgOpacity: Float, isSimplifiedUI: Boolean, lightMode: Boolean = false) {
// Dynamic text color

    val textColor = if (lightMode) Color(0xFF1A1A2E) else Color.White
    val textColorSecondary = if (lightMode) Color(0xFF1A1A2E).copy(alpha = 0.8f) else Color.White.copy(alpha = 0.9f)
    val textColorTertiary = if (lightMode) Color(0xFF1A1A2E).copy(alpha = 0.75f) else Color.White.copy(alpha = 0.85f)

    Box(modifier = Modifier.fillMaxSize()) {
//Background — Should be mostly white (slight gray) in light mode - dark blue in dark mode
        Box(modifier = Modifier.fillMaxSize().background(
            if (lightMode) Color(0xFFF0F4FF) else Color(0xFF0A1628)
        ))
        if (!lightMode) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = bgOpacity)))
        }
//Gear icon is always visible top-right (even if signed out)
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
                modifier = Modifier.width(200.dp).height(50.dp).pressScale()
            ) {
                Text("LOGIN OR SIGNUP", color = Color.Black, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onTutorialClick,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = primaryColor),
                border = BorderStroke(1.5.dp, primaryColor.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.width(200.dp).height(50.dp).pressScale()
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
    var showProfilePasswordReset by remember { mutableStateOf(false) }
    var isProcessingImage by remember { mutableStateOf(false) }
    var unreadMessageCount by remember { mutableStateOf(0) }
    var unreadMessagesFrom by remember { mutableStateOf<List<String>>(emptyList()) }
    var showUnreadDialog by remember { mutableStateOf(false) }
    var showNetworkingMenu by remember { mutableStateOf(false) }
    var showLogoutConfirm by remember { mutableStateOf(false) }
    var showMyQr by remember { mutableStateOf(false) }
    var showQrScanner by remember { mutableStateOf(false) }
    var cropSourceUri by remember { mutableStateOf<Uri?>(null) }

    var name by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
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
    var chapterInstagram by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var usernameLastChanged by remember { mutableStateOf(0L) }
    var usernameError by remember { mutableStateOf("") }
    var usernameAvailable by remember { mutableStateOf<Boolean?>(null) }
    var isCheckingUsername by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    var instagramError by remember { mutableStateOf("") }
    var twitterError by remember { mutableStateOf("") }
    var facebookError by remember { mutableStateOf("") }
    var linkedinError by remember { mutableStateOf("") }

    var showBusinessCardPreview by remember { mutableStateOf(false) }

    var connectionCount by remember { mutableIntStateOf(0) }
    var pendingRequestsCount by remember { mutableIntStateOf(0) }

    val effectiveBgOpacity = bgOpacity

    val textColor = if (lightMode) Color(0xFF1A1A2E) else Color.White
    val textColorSecondary = if (lightMode) Color(0xFF1A1A2E).copy(alpha = 0.8f) else Color.White.copy(alpha = 0.9f)
    val textColorTertiary = if (lightMode) Color(0xFF1A1A2E).copy(alpha = 0.75f) else Color.White.copy(alpha = 0.85f)

    // Pick image from gallery → open custom crop screen
    val pfpLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { cropSourceUri = it }
    }
    // Take a photo with the camera → open the same crop screen
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }
    var showCameraPermissionDialog by remember { mutableStateOf(false) }
    val pfpCameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) cameraImageUri?.let { cropSourceUri = it }
    }
    fun openCamera() {
        try {
            val photoFile = File.createTempFile("camera_", ".jpg", context.cacheDir)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
            cameraImageUri = uri
            pfpCameraLauncher.launch(uri)
        } catch (e: Exception) {
            Toast.makeText(context, "Camera not available", Toast.LENGTH_SHORT).show()
        }
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) openCamera() else showCameraPermissionDialog = true
    }
    fun launchCamera() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            openCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    fun uploadCroppedBitmap(bitmap: Bitmap) {
        val uid = auth.currentUser?.uid ?: return
        isProcessingImage = true
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
        val bytes = out.toByteArray()
        val storageRef = FirebaseStorage.getInstance().reference.child("profile_pictures/$uid.jpg")
        storageRef.putBytes(bytes)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    val url = downloadUri.toString()
                    pfpUrl = url
                    db.collection("users").document(uid)
                        .update("pfpUrl", url)
                        .addOnSuccessListener {
                            isProcessingImage = false
                            Toast.makeText(context, "Profile picture updated!", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            isProcessingImage = false
                            Toast.makeText(context, "Failed to save: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener { e ->
                isProcessingImage = false
                Toast.makeText(context, "Upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
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
                if (data != null && data.username.isEmpty()) {
                    val generatedUsername = generateRandomUsername()
                    val uid = auth.currentUser?.uid ?: return@addSnapshotListener
                    db.collection("usernames").document(generatedUsername).set(mapOf("uid" to uid))
                    db.collection("users").document(uid).update(mapOf("username" to generatedUsername, "usernameLastChanged" to 0L))
                }
                if (data != null && !isEditing) {
                    name = data.name
                    val nameParts = data.name.trim().split(" ", limit = 2)
                    firstName = nameParts.getOrElse(0) { "" }
                    lastName = nameParts.getOrElse(1) { "" }
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
                    chapterInstagram = data.chapterInstagram
                    username = data.username
                    usernameLastChanged = data.usernameLastChanged
                }
            }
        }
    }

    // Debounced username availability check
    LaunchedEffect(username, isEditing) {
        if (!isEditing || username.isEmpty() || username == (profile?.username ?: "")) {
            usernameAvailable = null
            usernameError = ""
            return@LaunchedEffect
        }
        if (!username.matches(Regex("[a-z0-9_]+"))) {
            usernameError = "Only lowercase letters, numbers, and underscores"
            usernameAvailable = null
            return@LaunchedEffect
        }
        if (username.length < 3) {
            usernameError = "At least 3 characters"
            usernameAvailable = null
            return@LaunchedEffect
        }
        if (containsProfanity(username)) {
            usernameError = "Username contains inappropriate content"
            usernameAvailable = null
            return@LaunchedEffect
        }
        usernameError = ""
        delay(600)
        isCheckingUsername = true
        try {
            val uid = auth.currentUser?.uid ?: return@LaunchedEffect
            val doc = db.collection("usernames").document(username).get().await()
            usernameAvailable = !doc.exists() || doc.getString("uid") == uid
        } catch (e: Exception) {
            usernameAvailable = null
        } finally {
            isCheckingUsername = false
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

    if (showProfilePasswordReset) {
        PasswordResetScreen(
            email = (auth.currentUser?.email ?: email),
            lightMode = lightMode,
            bgOpacity = bgOpacity,
            primaryColor = primaryColor,
            onComplete = {
                showProfilePasswordReset = false
                Toast.makeText(context, "Password updated successfully!", Toast.LENGTH_LONG).show()
            },
            onBack = { showProfilePasswordReset = false }
        )
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
// Background replaced with solid FBLA dark blue
        Box(modifier = Modifier.fillMaxSize().background(if (lightMode) Color(0xFFF0F4FF) else Color(0xFF0A1628)))
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
                        var showPhotoOptions by remember { mutableStateOf(false) }
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .background(primaryColor, CircleShape)
                                .clip(CircleShape)
                                .clickable { showPhotoOptions = true },
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
                                Text(firstName.take(1).ifEmpty { name.take(1).ifEmpty { "U" } }.uppercase(), color = Color.Black, style = MaterialTheme.typography.displayMedium.copy(fontSize = (MaterialTheme.typography.displayMedium.fontSize.value * textSizeMultiplier).sp), fontWeight = FontWeight.Bold)
                            }
                        }
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color.Black.copy(alpha = 0.85f), CircleShape)
                                .background(primaryColor.copy(alpha = 0.2f), CircleShape)
                                .align(Alignment.BottomEnd)
                                .clickable { showPhotoOptions = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CameraAlt, null, tint = if (lightMode) Color(0xFF1A1A2E) else Color.White, modifier = Modifier.size(18.dp))
                        }
                        if (showPhotoOptions) {
                            Dialog(onDismissRequest = { showPhotoOptions = false }) {
                                Surface(shape = RoundedCornerShape(16.dp), color = if (lightMode) Color.White else Color(0xFF1A2744)) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        TextButton(
                                            onClick = { showPhotoOptions = false; launchCamera() },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(Icons.Default.CameraAlt, null, tint = primaryColor, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(10.dp))
                                            Text("Take Photo", color = if (lightMode) Color(0xFF1A1A2E) else Color.White, fontWeight = FontWeight.Medium)
                                        }
                                        TextButton(
                                            onClick = { showPhotoOptions = false; pfpLauncher.launch("image/*") },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(Icons.Default.Image, null, tint = primaryColor, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(10.dp))
                                            Text("Choose from Gallery", color = if (lightMode) Color(0xFF1A1A2E) else Color.White, fontWeight = FontWeight.Medium)
                                        }
                                        if (pfpUrl.isNotEmpty()) {
                                            TextButton(
                                                onClick = {
                                                    showPhotoOptions = false
                                                    val uid = auth.currentUser?.uid ?: return@TextButton
                                                    pfpUrl = ""
                                                    db.collection("users").document(uid).update("pfpUrl", "")
                                                },
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Icon(Icons.Default.Delete, null, tint = Color(0xFFCC3333), modifier = Modifier.size(18.dp))
                                                Spacer(Modifier.width(10.dp))
                                                Text("Remove Photo", color = Color(0xFFCC3333), fontWeight = FontWeight.Medium)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if (showCameraPermissionDialog) {
                            AlertDialog(
                                onDismissRequest = { showCameraPermissionDialog = false },
                                containerColor = if (lightMode) Color.White else Color(0xFF1A2740),
                                icon = { Icon(Icons.Default.CameraAlt, null, tint = primaryColor) },
                                title = { Text("Camera access needed", color = textColor, fontWeight = FontWeight.Bold) },
                                text = {
                                    Text(
                                        "To take a photo, FBLA Hub needs permission to use your camera. You can turn it on in Settings.",
                                        color = textColorSecondary,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                },
                                confirmButton = {
                                    TextButton(onClick = {
                                        showCameraPermissionDialog = false
                                        try {
                                            val intent = Intent(
                                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                                Uri.fromParts("package", context.packageName, null)
                                            )
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Couldn't open settings", Toast.LENGTH_SHORT).show()
                                        }
                                    }) {
                                        Text("OPEN SETTINGS", color = primaryColor, fontWeight = FontWeight.Bold)
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showCameraPermissionDialog = false }) {
                                        Text("CANCEL", color = textColorSecondary)
                                    }
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))

                    if (isEditing) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Box(modifier = Modifier.weight(1f)) {
                                ProfileEditField(value = firstName, onValueChange = { firstName = it }, label = "First Name", lightMode = lightMode, accentColor = primaryColor)
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                ProfileEditField(value = lastName, onValueChange = { lastName = it }, label = "Last Name", lightMode = lightMode, accentColor = primaryColor)
                            }
                        }
                        ProfileEditField(value = email, onValueChange = { email = it }, label = "Email", lightMode = lightMode, accentColor = primaryColor)
                        Spacer(modifier = Modifier.height(12.dp))
                        YearDropdown(selectedYear = year, onYearSelected = { year = it }, lightMode = lightMode, accentColor = primaryColor)
                        Spacer(modifier = Modifier.height(12.dp))
                        StateDropdown(selectedState = statePref, onStateSelected = { statePref = it }, lightMode = lightMode, accentColor = primaryColor)
                        val bioFg = if (lightMode) Color(0xFF1A1A2E) else Color.White
                        val bioBg = if (lightMode) Color(0xFFF0F4FF) else Color.White.copy(alpha = 0.06f)
                        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                            Text("Short Bio", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = bioFg.copy(alpha = 0.6f), modifier = Modifier.padding(bottom = 6.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = 100.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(bioBg)
                                    .border(1.dp, if (bio.isNotEmpty()) primaryColor.copy(alpha = 0.6f) else bioFg.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
                                    .padding(horizontal = 14.dp, vertical = 12.dp)
                            ) {
                                BasicTextField(
                                    value = bio,
                                    onValueChange = { if (it.length <= 150) bio = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    textStyle = androidx.compose.ui.text.TextStyle(color = bioFg, fontSize = 14.sp, lineHeight = 20.sp),
                                    cursorBrush = SolidColor(primaryColor),
                                    decorationBox = { inner ->
                                        if (bio.isEmpty()) Text("Tell people a little about yourself…", color = bioFg.copy(alpha = 0.35f), fontSize = 14.sp)
                                        inner()
                                    }
                                )
                            }
                            Text(
                                text = "${bio.length}/150",
                                fontSize = 11.sp,
                                color = if (bio.length >= 140) Color(0xFFFF7043) else bioFg.copy(alpha = 0.4f),
                                modifier = Modifier.align(Alignment.End).padding(end = 4.dp, top = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "USERNAME",
                            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold, fontSize = (12 * textSizeMultiplier).sp),
                            color = primaryColor,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "3–20 chars, lowercase letters, numbers, underscores only. Changeable once every 30 days.",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = (11 * textSizeMultiplier).sp),
                            color = if (lightMode) Color(0xFF1A1A2E).copy(alpha = 0.6f) else Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        val cooldownDaysLeft = if (usernameLastChanged > 0L && (profile?.username ?: "").isNotEmpty()) {
                            val cooldownMs = 30L * 24 * 60 * 60 * 1000
                            val elapsed = System.currentTimeMillis() - usernameLastChanged
                            if (elapsed < cooldownMs) ((cooldownMs - elapsed) / (24 * 60 * 60 * 1000) + 1).toInt() else 0
                        } else 0
                        ProfileEditField(
                            value = username,
                            onValueChange = {
                                val cleaned = it.lowercase().filter { c -> c.isLetterOrDigit() || c == '_' }.take(20)
                                username = cleaned
                                usernameAvailable = null
                            },
                            label = "@username",
                            errorMessage = when {
                                usernameError.isNotEmpty() -> usernameError
                                usernameAvailable == false -> "@$username is already taken"
                                else -> ""
                            },
                            lightMode = lightMode,
                            accentColor = primaryColor
                        )
                        when {
                            cooldownDaysLeft > 0 && username != (profile?.username ?: "") ->
                                Text("You can change your username again in $cooldownDaysLeft day(s)", style = MaterialTheme.typography.labelSmall.copy(fontSize = (11 * textSizeMultiplier).sp), color = Color(0xFFFF9800), modifier = Modifier.padding(start = 4.dp, top = 2.dp))
                            isCheckingUsername ->
                                Text("Checking availability…", style = MaterialTheme.typography.labelSmall.copy(fontSize = (11 * textSizeMultiplier).sp), color = if (lightMode) Color(0xFF1A1A2E).copy(alpha = 0.5f) else Color.White.copy(alpha = 0.5f), modifier = Modifier.padding(start = 4.dp, top = 2.dp))
                            usernameAvailable == true && usernameError.isEmpty() ->
                                Text("@$username is available!", style = MaterialTheme.typography.labelSmall.copy(fontSize = (11 * textSizeMultiplier).sp), color = Color(0xFF2ECC71), modifier = Modifier.padding(start = 4.dp, top = 2.dp))
                        }

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
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "CHAPTER INSTAGRAM (Optional)",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    letterSpacing = 1.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = (12 * textSizeMultiplier).sp
                                ),
                                color = primaryColor,
                                modifier = Modifier.weight(1f)
                            )
                            InfoIconButton("Enter your chapter's public Instagram username (without the @).\n\nOnce saved, the Resources tab will show a live feed of your chapter's latest Instagram posts — so all members can stay up to date without leaving the app.\n\nThe account must be public for the feed to load.", primaryColor, lightMode)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Your chapter's public Instagram handle — posts will show in the Resources tab.",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = (11 * textSizeMultiplier).sp),
                            color = if (lightMode) Color(0xFF1A1A2E).copy(alpha = 0.6f) else Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        ProfileEditField(
                            value = chapterInstagram,
                            onValueChange = { chapterInstagram = it.trimStart('@') },
                            label = "@chapter_handle",
                            lightMode = lightMode,
                            accentColor = primaryColor
                        )
                    } else {
                        Text(text = name.ifEmpty { "Member" }, style = MaterialTheme.typography.headlineMedium.copy(fontSize = (MaterialTheme.typography.headlineMedium.fontSize.value * textSizeMultiplier).sp), color = textColor, fontWeight = FontWeight.Black)
                        if (username.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "@$username",
                                    color = primaryColor,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = (14 * textSizeMultiplier).sp),
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(buildAnnotatedString { append("@$username") })
                                        Toast.makeText(context, "Copied @$username", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, null, tint = primaryColor.copy(alpha = 0.6f), modifier = Modifier.size(15.dp))
                                }
                            }
                        }
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
                                        Icon(Icons.Default.CalendarToday, null, tint = primaryColor, modifier = Modifier.size(24.dp))
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(year.ifEmpty { "—" }, color = textColor, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium.copy(fontSize = (16 * textSizeMultiplier).sp))
                                        Text("Grad Year", color = textColorSecondary, style = MaterialTheme.typography.labelSmall.copy(fontSize = (10 * textSizeMultiplier).sp))
                                    }
                                }
                            }
                            Card(
                                modifier = Modifier.weight(1f).height(90.dp).clickable {
                                    shareMemberProfile(
                                        context = context,
                                        name = name.ifEmpty { "FBLA Member" },
                                        chapter = chapter,
                                        year = year,
                                        state = statePref,
                                        bio = bio
                                    )
                                },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = statCardColor)
                            ) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.Share, null, tint = primaryColor, modifier = Modifier.size(24.dp))
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Share", color = textColor, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium.copy(fontSize = (16 * textSizeMultiplier).sp))
                                        Text("Profile", color = textColorSecondary, style = MaterialTheme.typography.labelSmall.copy(fontSize = (10 * textSizeMultiplier).sp))
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(
                            onClick = { showBusinessCardPreview = true },
                            modifier = Modifier.fillMaxWidth().height(48.dp).pressScale(),
                            border = BorderStroke(1.5.dp, primaryColor),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = primaryColor),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.CreditCard, null, tint = primaryColor, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("MY BUSINESS CARD", fontWeight = FontWeight.Bold, style = TextStyle(fontSize = (13 * textSizeMultiplier).sp))
                        }
                    }

// Business card preview dialog
                    if (showBusinessCardPreview) {
                        Dialog(onDismissRequest = { showBusinessCardPreview = false }) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color(0xFF0A1628),
                                tonalElevation = 8.dp
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        "Your Business Card",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = (16 * textSizeMultiplier).sp,
                                        modifier = Modifier.padding(bottom = 14.dp)
                                    )
                                    BusinessCardComposable(
                                        name = name.ifEmpty { "FBLA Member" },
                                        username = username,
                                        chapter = chapter,
                                        year = year,
                                        state = statePref,
                                        bio = bio,
                                        primaryColor = primaryColor,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(1050f / 600f)
                                            .clip(RoundedCornerShape(12.dp))
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = { showBusinessCardPreview = false },
                                            modifier = Modifier.weight(1f).pressScale(),
                                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text("Close", fontWeight = FontWeight.Bold, fontSize = (13 * textSizeMultiplier).sp)
                                        }
                                        Button(
                                            onClick = {
                                                val bitmap = generateBusinessCardBitmap(
                                                    name = name.ifEmpty { "FBLA Member" },
                                                    username = username,
                                                    chapter = chapter,
                                                    year = year,
                                                    state = statePref,
                                                    bio = bio,
                                                    primaryColorInt = primaryColor.toArgb()
                                                )
                                                shareBusinessCard(context, bitmap, name.ifEmpty { "FBLA Member" })
                                                showBusinessCardPreview = false
                                            },
                                            modifier = Modifier.weight(1f).pressScale(),
                                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor, contentColor = Color(0xFF0A1628)),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Icon(Icons.Default.Share, null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Share", fontWeight = FontWeight.Bold, fontSize = (13 * textSizeMultiplier).sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

// ── NETWORKING HUB — now lives in Home tab ──────────────────
                    @Suppress("ConstantConditionIf") if (false) {
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
                        val reduceMotionProfile = LocalReduceMotion.current
                        androidx.compose.animation.AnimatedVisibility(
                            visible = showNetworkingMenu,
                            enter = if (reduceMotionProfile) androidx.compose.animation.EnterTransition.None else androidx.compose.animation.expandVertically(
                                animationSpec = androidx.compose.animation.core.spring(
                                    dampingRatio = 0.7f, stiffness = 300f
                                )
                            ) + androidx.compose.animation.fadeIn(),
                            exit = if (reduceMotionProfile) androidx.compose.animation.ExitTransition.None else androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut()
                        ) {
                            val panelBg = if (lightMode) Color(0xFFF0F4FF) else Color(0xFF0A1628)
                            val tileBg = if (lightMode) Color.White else Color(0xFF0F1E38)
                            val tileBorder = if (lightMode) Color(0xFFDDE3F0) else primaryColor.copy(alpha = 0.18f)

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 6.dp)
                                    .shadow(12.dp, RoundedCornerShape(24.dp))
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(panelBg)
                                    .border(1.dp, primaryColor.copy(alpha = 0.25f), RoundedCornerShape(24.dp))
                            ) {
                                // Gold accent strip at top
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(3.dp)
                                        .background(
                                            Brush.horizontalGradient(listOf(primaryColor, primaryColor.copy(alpha = 0.3f)))
                                        )
                                )
                                Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {

                                    // Header
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp)) {
                                        Box(modifier = Modifier.size(28.dp).background(primaryColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.Hub, null, tint = primaryColor, modifier = Modifier.size(16.dp))
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text("NETWORKING HUB", color = primaryColor, fontSize = (11 * textSizeMultiplier).sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                                    }

                                    // --- Find Peers tile ---
                                    Surface(
                                        onClick = { showNetworkingMenu = false; onFindLinksClick() },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(16.dp),
                                        color = tileBg,
                                        border = BorderStroke(1.dp, tileBorder),
                                        tonalElevation = 0.dp
                                    ) {
                                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Box(modifier = Modifier.size(40.dp).background(primaryColor.copy(alpha = 0.12f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                                                Icon(Icons.Default.PersonSearch, null, tint = primaryColor, modifier = Modifier.size(22.dp))
                                            }
                                            Spacer(modifier = Modifier.width(14.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("Find Peers", color = textColor, fontWeight = FontWeight.Bold, fontSize = (13 * textSizeMultiplier).sp)
                                                Text("Search for FBLA members to connect", color = textColor.copy(alpha = 0.55f), fontSize = (10 * textSizeMultiplier).sp)
                                            }
                                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = primaryColor.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                                        }
                                    }

                                    // --- Requests tile ---
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        Surface(
                                            onClick = { showNetworkingMenu = false; onConnectionRequestsClick() },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(16.dp),
                                            color = tileBg,
                                            border = BorderStroke(1.dp, tileBorder),
                                            tonalElevation = 0.dp
                                        ) {
                                            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Box(modifier = Modifier.size(40.dp).background(primaryColor.copy(alpha = 0.12f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                                                    Icon(Icons.Default.PersonAdd, null, tint = primaryColor, modifier = Modifier.size(22.dp))
                                                }
                                                Spacer(modifier = Modifier.width(14.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text("Connection Requests", color = textColor, fontWeight = FontWeight.Bold, fontSize = (13 * textSizeMultiplier).sp)
                                                    Text("People who want to connect", color = textColor.copy(alpha = 0.55f), fontSize = (10 * textSizeMultiplier).sp)
                                                }
                                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = primaryColor.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                                            }
                                        }
                                        if (pendingRequestsCount > 0) {
                                            Box(
                                                modifier = Modifier.align(Alignment.TopEnd).offset(x = (-6).dp, y = (-6).dp)
                                                    .height(18.dp).widthIn(min = 18.dp)
                                                    .background(Color(0xFFE53935), RoundedCornerShape(9.dp))
                                                    .padding(horizontal = 5.dp),
                                                contentAlignment = Alignment.Center
                                            ) { Text(pendingRequestsCount.toString(), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                                        }
                                    }

                                    // --- My Connections tile ---
                                    Surface(
                                        onClick = { showNetworkingMenu = false; onMyConnectionsClick() },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(16.dp),
                                        color = tileBg,
                                        border = BorderStroke(1.dp, tileBorder),
                                        tonalElevation = 0.dp
                                    ) {
                                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Box(modifier = Modifier.size(40.dp).background(primaryColor.copy(alpha = 0.12f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                                                Icon(Icons.Default.People, null, tint = primaryColor, modifier = Modifier.size(22.dp))
                                            }
                                            Spacer(modifier = Modifier.width(14.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("My Connections", color = textColor, fontWeight = FontWeight.Bold, fontSize = (13 * textSizeMultiplier).sp)
                                                Text("$connectionCount peer${if (connectionCount != 1) "s" else ""} in your network", color = textColor.copy(alpha = 0.55f), fontSize = (10 * textSizeMultiplier).sp)
                                            }
                                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = primaryColor.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                                        }
                                    }

                                    // --- Messages tile ---
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        Surface(
                                            onClick = { showNetworkingMenu = false; if (unreadMessageCount > 0) showUnreadDialog = true else onMessagesClick() },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(16.dp),
                                            color = tileBg,
                                            border = BorderStroke(1.dp, tileBorder),
                                            tonalElevation = 0.dp
                                        ) {
                                            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Box(modifier = Modifier.size(40.dp).background(primaryColor.copy(alpha = 0.12f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                                                    Icon(Icons.Default.Chat, null, tint = primaryColor, modifier = Modifier.size(22.dp))
                                                }
                                                Spacer(modifier = Modifier.width(14.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text("Messages", color = textColor, fontWeight = FontWeight.Bold, fontSize = (13 * textSizeMultiplier).sp)
                                                    Text("Chat with your connections", color = textColor.copy(alpha = 0.55f), fontSize = (10 * textSizeMultiplier).sp)
                                                }
                                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = primaryColor.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                                            }
                                        }
                                        if (unreadMessageCount > 0) {
                                            Box(
                                                modifier = Modifier.align(Alignment.TopEnd).offset(x = (-6).dp, y = (-6).dp)
                                                    .height(18.dp).widthIn(min = 18.dp)
                                                    .background(Color(0xFFE53935), RoundedCornerShape(9.dp))
                                                    .padding(horizontal = 5.dp),
                                                contentAlignment = Alignment.Center
                                            ) { Text(if (unreadMessageCount > 9) "9+" else unreadMessageCount.toString(), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                                        }
                                    }

                                    // --- Classrooms tile ---
                                    Surface(
                                        onClick = { showNetworkingMenu = false; onClassroomsClick() },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(16.dp),
                                        color = tileBg,
                                        border = BorderStroke(1.dp, tileBorder),
                                        tonalElevation = 0.dp
                                    ) {
                                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Box(modifier = Modifier.size(40.dp).background(primaryColor.copy(alpha = 0.12f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                                                Icon(Icons.Default.Groups, null, tint = primaryColor, modifier = Modifier.size(22.dp))
                                            }
                                            Spacer(modifier = Modifier.width(14.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("Classrooms", color = textColor, fontWeight = FontWeight.Bold, fontSize = (13 * textSizeMultiplier).sp)
                                                Text("Chapter group chats & invite codes", color = textColor.copy(alpha = 0.55f), fontSize = (10 * textSizeMultiplier).sp)
                                            }
                                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = primaryColor.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                                        }
                                    }

                                    // --- QR divider + tiles ---
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                                        Box(modifier = Modifier.weight(1f).height(1.dp).background(primaryColor.copy(alpha = 0.2f)))
                                        Text("  QUICK CONNECT  ", color = primaryColor.copy(alpha = 0.6f), fontSize = (9 * textSizeMultiplier).sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                                        Box(modifier = Modifier.weight(1f).height(1.dp).background(primaryColor.copy(alpha = 0.2f)))
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Surface(
                                            onClick = { showNetworkingMenu = false; showMyQr = true },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(16.dp),
                                            color = primaryColor.copy(alpha = 0.12f),
                                            border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.35f)),
                                            tonalElevation = 0.dp
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 14.dp)) {
                                                Icon(Icons.Default.QrCode2, null, tint = primaryColor, modifier = Modifier.size(24.dp))
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text("My QR Code", color = primaryColor, fontWeight = FontWeight.Bold, fontSize = (10 * textSizeMultiplier).sp)
                                            }
                                        }
                                        Surface(
                                            onClick = { showNetworkingMenu = false; showQrScanner = true },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(16.dp),
                                            color = primaryColor.copy(alpha = 0.12f),
                                            border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.35f)),
                                            tonalElevation = 0.dp
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 14.dp)) {
                                                Icon(Icons.Default.QrCodeScanner, null, tint = primaryColor, modifier = Modifier.size(24.dp))
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text("Scan QR Code", color = primaryColor, fontWeight = FontWeight.Bold, fontSize = (10 * textSizeMultiplier).sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } // ── END NETWORKING HUB (now in Home tab) ────────────

                    Spacer(modifier = Modifier.height(12.dp))

// Edit(big) and the Logout(small)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = {
                                if (isEditing) {
                                    val hasErrors = instagramError.isNotEmpty() ||
                                            twitterError.isNotEmpty() ||
                                            facebookError.isNotEmpty() ||
                                            linkedinError.isNotEmpty() ||
                                            usernameError.isNotEmpty() ||
                                            usernameAvailable == false
                                    if (hasErrors) {
                                        Toast.makeText(context, "Please fix errors before saving", Toast.LENGTH_LONG).show()
                                        return@Button
                                    }
                                    val combinedNameCheck = "${firstName.trim()} ${lastName.trim()}".trim()
                                    if (containsProfanity(combinedNameCheck)) {
                                        Toast.makeText(context, "Name contains inappropriate content", Toast.LENGTH_LONG).show()
                                        return@Button
                                    }
                                    if (bio.isNotEmpty() && containsProfanity(bio)) {
                                        Toast.makeText(context, "Bio contains inappropriate content", Toast.LENGTH_LONG).show()
                                        return@Button
                                    }
                                    val usernameChanged = username.trim() != (profile?.username ?: "")
                                    val cooldownMs = 30L * 24 * 60 * 60 * 1000
                                    val now = System.currentTimeMillis()
                                    if (usernameChanged && username.trim().isNotEmpty() && (profile?.username ?: "").isNotEmpty()) {
                                        val lastChanged = profile?.usernameLastChanged ?: 0L
                                        if (lastChanged > 0L && now - lastChanged < cooldownMs) {
                                            val daysLeft = ((cooldownMs - (now - lastChanged)) / (24 * 60 * 60 * 1000) + 1).toInt()
                                            Toast.makeText(context, "You can change your username in $daysLeft more day(s)", Toast.LENGTH_LONG).show()
                                            return@Button
                                        }
                                    }
                                    val combinedName = "${firstName.trim()} ${lastName.trim()}".trim().ifEmpty { name }
                                    name = combinedName
                                    val newUsernameLastChanged = if (usernameChanged && username.trim().isNotEmpty()) now else (profile?.usernameLastChanged ?: 0L)
                                    val newProfile = profile!!.copy(name = combinedName, email = email, school = chapter, year = year, state = statePref, bio = bio, pfpUrl = pfpUrl, instagram = instagram, twitter = twitter, facebook = facebook, linkedin = linkedin, chapterInstagram = chapterInstagram.trim(), username = username.trim(), usernameLastChanged = newUsernameLastChanged)
                                    val uid = auth.currentUser!!.uid
                                    if (usernameChanged && username.trim().isNotEmpty()) {
                                        val newUname = username.trim()
                                        val oldUname = profile?.username ?: ""
                                        db.runTransaction { transaction ->
                                            val ref = db.collection("usernames").document(newUname)
                                            val existing = transaction.get(ref)
                                            if (existing.exists() && existing.getString("uid") != uid) {
                                                throw Exception("@$newUname was just taken — please choose another")
                                            }
                                            if (oldUname.isNotEmpty() && oldUname != newUname) {
                                                transaction.delete(db.collection("usernames").document(oldUname))
                                            }
                                            transaction.set(ref, mapOf("uid" to uid))
                                        }.addOnSuccessListener {
                                            db.collection("users").document(uid).set(newProfile)
                                        }.addOnFailureListener { e ->
                                            Toast.makeText(context, e.message ?: "Failed to save username", Toast.LENGTH_SHORT).show()
                                        }
                                    } else if (usernameChanged && username.trim().isEmpty() && (profile?.username ?: "").isNotEmpty()) {
                                        db.collection("usernames").document(profile!!.username).delete()
                                        db.collection("users").document(uid).set(newProfile)
                                    } else {
                                        db.collection("users").document(uid).set(newProfile)
                                    }
                                    profile = newProfile
                                }
                                isEditing = !isEditing
                            },
                            modifier = Modifier.weight(1f).height(50.dp).shadow(4.dp, RoundedCornerShape(12.dp)),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = primaryColor,
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

// Change password (sends a reset link to the account email) — shown while editing
                    if (isEditing) {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = {
                                val targetEmail = (auth.currentUser?.email ?: email).trim()
                                if (targetEmail.isBlank()) {
                                    Toast.makeText(context, "No email on file for this account", Toast.LENGTH_LONG).show()
                                    return@OutlinedButton
                                }
                                scope.launch {
                                    try {
                                        sendPasswordResetEmail(targetEmail)
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "We emailed an 8-digit code to $targetEmail", Toast.LENGTH_LONG).show()
                                            showProfilePasswordReset = true
                                        }
                                    } catch (e: Exception) {
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "Couldn't send reset email: ${e.message}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp).pressScale(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = primaryColor),
                            border = BorderStroke(1.5.dp, primaryColor.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.LockReset, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("CHANGE PASSWORD", fontWeight = FontWeight.Black, fontSize = (13 * textSizeMultiplier).sp, letterSpacing = 1.sp)
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
                                    modifier = Modifier.fillMaxWidth().pressScale()
                                ) {
                                    Text("Yes, Log Out", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            },
                            dismissButton = {
                                OutlinedButton(
                                    onClick = { showLogoutConfirm = false },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().pressScale(),
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

    cropSourceUri?.let { uri ->
        ImageCropScreen(
            imageUri = uri,
            primaryColor = primaryColor,
            onCropped = { bitmap ->
                cropSourceUri = null
                uploadCroppedBitmap(bitmap)
            },
            onDismiss = { cropSourceUri = null }
        )
    }

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
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedTextColor = fg,
                focusedTextColor = fg,
                errorTextColor = Color(0xFFFF5252),
                unfocusedBorderColor = fg.copy(alpha = 0.45f),
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
fun ImageCropScreen(
    imageUri: Uri,
    primaryColor: Color,
    onCropped: (Bitmap) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    val srcBitmap = remember(imageUri) {
        context.contentResolver.openInputStream(imageUri).use { BitmapFactory.decodeStream(it) }
    } ?: run { onDismiss(); return }

    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        val screenWpx = constraints.maxWidth.toFloat()
        val screenHpx = constraints.maxHeight.toFloat()
        val bottomBarPx = with(density) { 96.dp.toPx() }
        val availH = screenHpx - bottomBarPx
        val cropSizePx = minOf(screenWpx, availH) * 0.84f

        val minScale = maxOf(cropSizePx / srcBitmap.width, cropSizePx / srcBitmap.height)
        var scale by remember { mutableStateOf(minScale) }
        var offsetX by remember { mutableStateOf(0f) }
        var offsetY by remember { mutableStateOf(0f) }

        fun clamp(s: Float, ox: Float, oy: Float): Pair<Float, Float> {
            val hw = srcBitmap.width * s / 2f
            val hh = srcBitmap.height * s / 2f
            val hc = cropSizePx / 2f
            return ox.coerceIn(-(hw - hc), hw - hc) to oy.coerceIn(-(hh - hc), hh - hc)
        }

        val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
            val newScale = (scale * zoomChange).coerceIn(minScale, minScale * 5f)
            val (cx, cy) = clamp(newScale, offsetX + panChange.x, offsetY + panChange.y)
            scale = newScale; offsetX = cx; offsetY = cy
        }

        // Image + overlay area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(with(density) { availH.toDp() })
                .transformable(state = transformableState)
        ) {
            Image(
                bitmap = srcBitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.None,
                modifier = Modifier
                    .width(with(density) { (srcBitmap.width * scale).toDp() })
                    .height(with(density) { (srcBitmap.height * scale).toDp() })
                    .align(Alignment.Center)
                    .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            )

            Canvas(modifier = Modifier.fillMaxSize()) {
                val cl = (screenWpx - cropSizePx) / 2f
                val ct = (availH - cropSizePx) / 2f
                val overlayColor = Color.Black.copy(alpha = 0.63f)
                drawRect(overlayColor, topLeft = androidx.compose.ui.geometry.Offset(0f, 0f), size = androidx.compose.ui.geometry.Size(screenWpx, ct))
                drawRect(overlayColor, topLeft = androidx.compose.ui.geometry.Offset(0f, ct + cropSizePx), size = androidx.compose.ui.geometry.Size(screenWpx, availH - ct - cropSizePx))
                drawRect(overlayColor, topLeft = androidx.compose.ui.geometry.Offset(0f, ct), size = androidx.compose.ui.geometry.Size(cl, cropSizePx))
                drawRect(overlayColor, topLeft = androidx.compose.ui.geometry.Offset(cl + cropSizePx, ct), size = androidx.compose.ui.geometry.Size(screenWpx - cl - cropSizePx, cropSizePx))
                drawRect(primaryColor, topLeft = androidx.compose.ui.geometry.Offset(cl, ct), size = androidx.compose.ui.geometry.Size(cropSizePx, cropSizePx), style = Stroke(2.dp.toPx()))
                val t = cropSizePx / 3f
                val gc = Color.White.copy(alpha = 0.22f)
                for (i in 1..2) {
                    drawLine(gc, androidx.compose.ui.geometry.Offset(cl + i * t, ct), androidx.compose.ui.geometry.Offset(cl + i * t, ct + cropSizePx), 1f)
                    drawLine(gc, androidx.compose.ui.geometry.Offset(cl, ct + i * t), androidx.compose.ui.geometry.Offset(cl + cropSizePx, ct + i * t), 1f)
                }
                val hl = 18.dp.toPx(); val hw = 3.dp.toPx()
                listOf(
                    androidx.compose.ui.geometry.Offset(cl, ct) to Pair(1f, 1f),
                    androidx.compose.ui.geometry.Offset(cl + cropSizePx, ct) to Pair(-1f, 1f),
                    androidx.compose.ui.geometry.Offset(cl, ct + cropSizePx) to Pair(1f, -1f),
                    androidx.compose.ui.geometry.Offset(cl + cropSizePx, ct + cropSizePx) to Pair(-1f, -1f)
                ).forEach { (corner, dir) ->
                    drawLine(primaryColor, corner, androidx.compose.ui.geometry.Offset(corner.x + dir.first * hl, corner.y), hw)
                    drawLine(primaryColor, corner, androidx.compose.ui.geometry.Offset(corner.x, corner.y + dir.second * hl), hw)
                }
            }
        }

        // Bottom bar with Cancel + Use Photo
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color(0xFF0A1628))
                .padding(horizontal = 32.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Button(
                onClick = {
                    scope.launch(Dispatchers.Default) {
                        val cl = (screenWpx - cropSizePx) / 2f
                        val ct = (availH - cropSizePx) / 2f
                        val imgLeft = screenWpx / 2f - srcBitmap.width * scale / 2f + offsetX
                        val imgTop = availH / 2f - srcBitmap.height * scale / 2f + offsetY
                        val bmpX = ((cl - imgLeft) / scale).toInt().coerceIn(0, srcBitmap.width - 1)
                        val bmpY = ((ct - imgTop) / scale).toInt().coerceIn(0, srcBitmap.height - 1)
                        val bmpS = (cropSizePx / scale).toInt().coerceIn(1, minOf(srcBitmap.width - bmpX, srcBitmap.height - bmpY))
                        val cropped = Bitmap.createBitmap(srcBitmap, bmpX, bmpY, bmpS, bmpS)
                        val output = Bitmap.createScaledBitmap(cropped, 400, 400, true)
                        withContext(Dispatchers.Main) { onCropped(output) }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(48.dp).pressScale()
            ) {
                Text("Use Photo", color = Color(0xFF0A1628), fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
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
    reduceMotion: Boolean = false,
    onReduceMotionChange: (Boolean) -> Unit = {},
    notifEnabled: Boolean = true,
    onNotifEnabledChange: (Boolean) -> Unit = {},
    notifMessages: Boolean = true,
    onNotifMessagesChange: (Boolean) -> Unit = {},
    notifGroupMessages: Boolean = true,
    onNotifGroupMessagesChange: (Boolean) -> Unit = {},
    notifConnectionRequests: Boolean = true,
    onNotifConnectionRequestsChange: (Boolean) -> Unit = {},
    notifConnectionAccepted: Boolean = true,
    onNotifConnectionAcceptedChange: (Boolean) -> Unit = {},
    notifEventReminders: Boolean = true,
    onNotifEventRemindersChange: (Boolean) -> Unit = {},
    hideConnections: Boolean = false,
    onHideConnectionsChange: (Boolean) -> Unit = {},
    allowPeerSharing: Boolean = true,
    onAllowPeerSharingChange: (Boolean) -> Unit = {},
    onDeleteAccount: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val auth = FirebaseAuth.getInstance()
    var showColorPicker by remember { mutableStateOf(false) }
    var pickerHue by remember { mutableFloatStateOf(45f) }
    var pickerSaturation by remember { mutableFloatStateOf(1f) }
    var pickerValue by remember { mutableFloatStateOf(1f) }
    var showDeletionVerify by remember { mutableStateOf(false) }
    var deletionEmail by remember { mutableStateOf("") }
    var isSendingDeletionCode by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    val textColor = if (lightMode) Color(0xFF1A1A2E) else Color.White
    val textColorSecondary = if (lightMode) Color(0xFF1A1A2E).copy(alpha = 0.8f) else Color.White.copy(alpha = 0.9f)
    val textColorTertiary = if (lightMode) Color(0xFF1A1A2E).copy(alpha = 0.75f) else Color.White.copy(alpha = 0.85f)

    if (showDeletionVerify) {
        AccountDeletionScreen(
            email = deletionEmail,
            lightMode = lightMode,
            bgOpacity = bgOpacity,
            primaryColor = primaryColor,
            onConfirmed = {
                showDeletionVerify = false
                onDeleteAccount()
            },
            onBack = { showDeletionVerify = false }
        )
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize().background(if (lightMode) Color(0xFFF0F4FF) else Color(0xFF0A1628)))
        if (!lightMode) Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = bgOpacity)))

        Column(modifier = Modifier.fillMaxSize()) {
// Top bar
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 48.dp, start = 0.dp, end = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BackButton(onClick = onBack, tint = textColor)
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
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("THEME COLORS", style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 2.sp, fontWeight = FontWeight.Bold), color = primaryColor, modifier = Modifier.weight(1f))
                        InfoIconButton("Your accent color changes buttons, icons, and highlights throughout the whole app.\n\n• Tap a saved color circle to apply it\n• Tap the + button to open a full color picker with hue, saturation, and brightness sliders\n• Tap ✕ on a saved color to remove it\n\nYou can save up to 5 custom colors.", primaryColor, lightMode)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Tap to apply • Tap ✕ to remove", color = textColorSecondary, style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        savedColors.forEachIndexed { index, colorInt ->
                            val color = Color(colorInt)
                            val isSelected = color.toArgb() == primaryColor.toArgb()
                            Box(modifier = Modifier.size(54.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .align(Alignment.BottomStart)
                                        .shadow(if (isSelected) 8.dp else 2.dp, CircleShape)
                                        .background(color, CircleShape)
                                        .border(if (isSelected) 3.dp else 1.dp, if (isSelected) Color.White else Color.White.copy(alpha = 0.3f), CircleShape)
                                        .clickable { onPrimaryColorChange(color) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        val lum = 0.299f * color.red + 0.587f * color.green + 0.114f * color.blue
                                        Icon(Icons.Default.Check, null, tint = if (lum > 0.5f) Color.Black else Color.White, modifier = Modifier.size(22.dp))
                                    }
                                }
                                if (index >= 2) {
                                    Box(
                                        modifier = Modifier
                                            .size(18.dp)
                                            .align(Alignment.TopEnd)
                                            .background(Color(0xFFE53935), CircleShape)
                                            .clickable {
                                                val updated = savedColors.toMutableList().also { it.removeAt(index) }
                                                onSavedColorsChange(updated)
                                                if (isSelected) onPrimaryColorChange(Color(savedColors[0]))
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(11.dp))
                                    }
                                }
                            }
                        }
                        if (savedColors.size < 5) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .shadow(2.dp, CircleShape)
                                    .background(if (lightMode) Color(0xFFE8EDF5) else Color(0xFF2A3A50), CircleShape)
                                    .border(1.5.dp, primaryColor.copy(alpha = 0.5f), CircleShape)
                                    .clickable { showColorPicker = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Add, null, tint = primaryColor, modifier = Modifier.size(24.dp))
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

// ── NOTIFICATIONS ─────────────────────────────────────
                    Text("NOTIFICATIONS", style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 2.sp, fontWeight = FontWeight.Bold), color = primaryColor, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Control which push notifications you receive", color = textColorTertiary, style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(16.dp))
                    SettingToggle(
                        title = "Push Notifications",
                        subtitle = "Master switch for all notifications",
                        icon = Icons.Default.Notifications,
                        checked = notifEnabled,
                        onCheckedChange = onNotifEnabledChange,
                        primaryColor = primaryColor,
                        textSizeMultiplier = textSizeMultiplier,
                        lightMode = lightMode
                    )
                    if (notifEnabled) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Column(
                            modifier = Modifier.fillMaxWidth()
                                .background(
                                    if (lightMode) Color(0xFFF0F4FF) else Color.White.copy(alpha = 0.04f),
                                    RoundedCornerShape(16.dp)
                                )
                                .padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp)
                        ) {
                            SettingToggle(
                                title = "Direct Messages",
                                subtitle = "New messages from connections",
                                icon = Icons.Default.Chat,
                                checked = notifMessages,
                                onCheckedChange = onNotifMessagesChange,
                                primaryColor = primaryColor,
                                textSizeMultiplier = textSizeMultiplier,
                                lightMode = lightMode
                            )
                            HorizontalDivider(color = textColor.copy(alpha = 0.06f))
                            SettingToggle(
                                title = "Group Messages",
                                subtitle = "Activity in your group chats",
                                icon = Icons.Default.Group,
                                checked = notifGroupMessages,
                                onCheckedChange = onNotifGroupMessagesChange,
                                primaryColor = primaryColor,
                                textSizeMultiplier = textSizeMultiplier,
                                lightMode = lightMode
                            )
                            HorizontalDivider(color = textColor.copy(alpha = 0.06f))
                            SettingToggle(
                                title = "Connection Requests",
                                subtitle = "When someone wants to connect",
                                icon = Icons.Default.PersonAdd,
                                checked = notifConnectionRequests,
                                onCheckedChange = onNotifConnectionRequestsChange,
                                primaryColor = primaryColor,
                                textSizeMultiplier = textSizeMultiplier,
                                lightMode = lightMode
                            )
                            HorizontalDivider(color = textColor.copy(alpha = 0.06f))
                            SettingToggle(
                                title = "Connection Accepted",
                                subtitle = "When someone accepts your request",
                                icon = Icons.Default.People,
                                checked = notifConnectionAccepted,
                                onCheckedChange = onNotifConnectionAcceptedChange,
                                primaryColor = primaryColor,
                                textSizeMultiplier = textSizeMultiplier,
                                lightMode = lightMode
                            )
                            HorizontalDivider(color = textColor.copy(alpha = 0.06f))
                            SettingToggle(
                                title = "Event Reminders",
                                subtitle = "Alerts before your competitions",
                                icon = Icons.Default.Event,
                                checked = notifEventReminders,
                                onCheckedChange = onNotifEventRemindersChange,
                                primaryColor = primaryColor,
                                textSizeMultiplier = textSizeMultiplier,
                                lightMode = lightMode
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                    HorizontalDivider(color = textColor.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(24.dp))

// ── PRIVACY ───────────────────────────────────────────
                    Text("PRIVACY", style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 2.sp, fontWeight = FontWeight.Bold), color = primaryColor, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Control what others can see about you", color = textColorTertiary, style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(16.dp))
                    SettingToggle(
                        title = "Hide My Connections",
                        subtitle = "Others won't see your connections list",
                        icon = Icons.Default.VisibilityOff,
                        checked = hideConnections,
                        onCheckedChange = onHideConnectionsChange,
                        primaryColor = primaryColor,
                        textSizeMultiplier = textSizeMultiplier,
                        lightMode = lightMode
                    )
                    HorizontalDivider(color = textColor.copy(alpha = 0.06f))
                    SettingToggle(
                        title = "Allow Peers to Share My Profile",
                        subtitle = "Connections can share your card & profile",
                        icon = Icons.Default.Share,
                        checked = allowPeerSharing,
                        onCheckedChange = onAllowPeerSharingChange,
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
                        Text("About FBLA Hub v1.3", color = textColorTertiary, style = MaterialTheme.typography.bodySmall)
                    }
                    TextButton(
                        enabled = !isSendingDeletionCode,
                        onClick = {
                            val user = FirebaseAuth.getInstance().currentUser
                            val targetEmail = user?.email?.trim()?.lowercase().orEmpty()
                            if (targetEmail.isBlank()) {
                                Toast.makeText(context, "No email on file for this account", Toast.LENGTH_LONG).show()
                                return@TextButton
                            }
                            isSendingDeletionCode = true
                            scope.launch {
                                try {
                                    val name = user?.displayName.orEmpty()
                                    sendAccountDeletionCode(targetEmail, name)
                                    withContext(Dispatchers.Main) {
                                        isSendingDeletionCode = false
                                        Toast.makeText(context, "We emailed a confirmation code to $targetEmail", Toast.LENGTH_LONG).show()
                                        deletionEmail = targetEmail
                                        showDeletionVerify = true
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        isSendingDeletionCode = false
                                        Toast.makeText(context, "Couldn't send code: ${e.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isSendingDeletionCode) "Sending code…" else "Delete Account", color = Color.Red, style = MaterialTheme.typography.bodySmall)
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
                        modifier = Modifier.fillMaxWidth().height(50.dp).pressScale()
                    ) {
                        Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("TUTORIAL", fontWeight = FontWeight.Black, fontSize = 13.sp, letterSpacing = 1.sp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = {
                            val userEmail = auth.currentUser?.email.orEmpty()
                            val userName = auth.currentUser?.displayName.orEmpty()
                            if (userEmail.isNotBlank()) {
                                scope.launch {
                                    try {
                                        sendContactUsAutoReply(userEmail, userName)
                                    } catch (e: Exception) {
                                        android.util.Log.e("EmailJS", "Contact auto-reply failed: ${e.message}", e)
                                    }
                                }
                            }
                            sendSupportEmail(
                                context,
                                subject = "FBLA Hub - Support / Contact",
                                body = "Hi FBLA Hub team,\n\nDetails:\n\n"
                            )
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = primaryColor),
                        border = BorderStroke(1.5.dp, primaryColor.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(50.dp).pressScale()
                    ) {
                        Icon(Icons.Default.Email, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("CONTACT US", fontWeight = FontWeight.Black, fontSize = 13.sp, letterSpacing = 1.sp)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Questions or feedback? Email us at $SUPPORT_EMAIL", color = textColorTertiary, style = MaterialTheme.typography.bodySmall, modifier = Modifier.fillMaxWidth())

                    Spacer(modifier = Modifier.height(48.dp))
                }
            }
        }
    }

// Color Picker Dialog
    if (showColorPicker) {
        LaunchedEffect(showColorPicker) {
            val hsv = FloatArray(3)
            android.graphics.Color.colorToHSV(primaryColor.toArgb(), hsv)
            pickerHue = hsv[0]; pickerSaturation = hsv[1]; pickerValue = hsv[2]
        }
        val presetColors = remember {
            listOf(
                0xFFFFB800, 0xFFFF6B00, 0xFFE53935, 0xFFD81B60,
                0xFF8E24AA, 0xFF3949AB, 0xFF1E88E5, 0xFF00ACC1,
                0xFF00897B, 0xFF43A047, 0xFF7CB342, 0xFFF4511E,
                0xFF6D4C41, 0xFF546E7A, 0xFFFFD600, 0xFFFFFFFF
            ).map { it.toInt() }
        }
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showColorPicker = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
                Card(
                    modifier = Modifier.fillMaxWidth(0.92f),
                    colors = CardDefaults.cardColors(containerColor = if (lightMode) Color.White else Color(0xFF1A2740)),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Choose Color", color = textColor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(16.dp))

// Preset grid
                        Text("Presets", color = textColorSecondary, style = MaterialTheme.typography.labelSmall, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(8.dp))
                        val presetRows = presetColors.chunked(4)
                        presetRows.forEach { row ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                row.forEach { colorInt ->
                                    val presetColor = Color(colorInt)
                                    val curArgb = android.graphics.Color.HSVToColor(floatArrayOf(pickerHue, pickerSaturation, pickerValue))
                                    val isActive = curArgb == colorInt
                                    Box(
                                        modifier = Modifier.weight(1f).aspectRatio(1f)
                                            .shadow(if (isActive) 6.dp else 1.dp, RoundedCornerShape(8.dp))
                                            .background(presetColor, RoundedCornerShape(8.dp))
                                            .border(if (isActive) 2.5.dp else 0.dp, Color.White, RoundedCornerShape(8.dp))
                                            .clickable {
                                                val hsv = FloatArray(3)
                                                android.graphics.Color.colorToHSV(colorInt, hsv)
                                                pickerHue = hsv[0]; pickerSaturation = hsv[1]; pickerValue = hsv[2]
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isActive) {
                                            val lum = 0.299f * presetColor.red + 0.587f * presetColor.green + 0.114f * presetColor.blue
                                            Icon(Icons.Default.Check, null, tint = if (lum > 0.6f) Color.Black else Color.White, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                        }

                        HorizontalDivider(color = textColor.copy(alpha = 0.1f))
                        Spacer(Modifier.height(12.dp))

// Custom HSV wheel
                        Text("Custom", color = textColorSecondary, style = MaterialTheme.typography.labelSmall, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(8.dp))
                        Box(modifier = Modifier.size(220.dp), contentAlignment = Alignment.Center) {
                            Canvas(modifier = Modifier.size(220.dp).pointerInput(Unit) {
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
                                for (angle in 0 until 360) { val hc=android.graphics.Color.HSVToColor(floatArrayOf(angle.toFloat(),1f,1f)); drawArc(color=Color(hc),startAngle=angle.toFloat(),sweepAngle=1.5f,useCenter=false,topLeft=androidx.compose.ui.geometry.Offset(cx-outerR,cy-outerR),size=androidx.compose.ui.geometry.Size(outerR*2,outerR*2),style=androidx.compose.ui.graphics.drawscope.Stroke(width=ring)) }
                                val sqSz=innerR*1.4f; val sqL=cx-sqSz/2f; val sqT=cy-sqSz/2f
                                for (x in 0 until sqSz.toInt() step 3) for (y in 0 until sqSz.toInt() step 3) { val c=android.graphics.Color.HSVToColor(floatArrayOf(pickerHue,x/sqSz,1f-(y/sqSz))); drawCircle(color=Color(c),radius=3f,center=androidx.compose.ui.geometry.Offset(sqL+x,sqT+y)) }
                                val hRad=pickerHue*Math.PI.toFloat()/180f; val hR=(innerR+outerR)/2f; val hPos=androidx.compose.ui.geometry.Offset(cx+hR*kotlin.math.cos(hRad),cy+hR*kotlin.math.sin(hRad))
                                drawCircle(color=Color.White,radius=10f,center=hPos); drawCircle(color=Color.Black.copy(alpha=0.3f),radius=10f,center=hPos,style=androidx.compose.ui.graphics.drawscope.Stroke(width=2f))
                                val sPos=androidx.compose.ui.geometry.Offset(sqL+pickerSaturation*sqSz,sqT+(1f-pickerValue)*sqSz)
                                drawCircle(color=Color.White,radius=8f,center=sPos); drawCircle(color=Color.Black.copy(alpha=0.3f),radius=8f,center=sPos,style=androidx.compose.ui.graphics.drawscope.Stroke(width=2f))
                            }
                        }

                        Spacer(Modifier.height(12.dp))
// Preview bar
                        val previewArgb = android.graphics.Color.HSVToColor(floatArrayOf(pickerHue, pickerSaturation, pickerValue))
                        Box(modifier = Modifier.fillMaxWidth().height(44.dp).background(Color(previewArgb), RoundedCornerShape(10.dp)).border(1.5.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(10.dp)))

                        Spacer(Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { showColorPicker = false },
                                modifier = Modifier.weight(1f).pressScale(),
                                border = BorderStroke(1.dp, textColor.copy(alpha = 0.3f)),
                                shape = RoundedCornerShape(10.dp)
                            ) { Text("Cancel", color = textColorSecondary) }
                            Button(
                                onClick = {
                                    val newArgb = android.graphics.Color.HSVToColor(floatArrayOf(pickerHue, pickerSaturation, pickerValue))
                                    val alreadySaved = savedColors.any { it == newArgb }
                                    if (!alreadySaved && savedColors.size < 5) onSavedColorsChange(savedColors + newArgb)
                                    onPrimaryColorChange(Color(newArgb))
                                    showColorPicker = false
                                },
                                modifier = Modifier.weight(1f).pressScale(),
                                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                                shape = RoundedCornerShape(10.dp)
                            ) { Text("Apply", color = Color.Black, fontWeight = FontWeight.Bold) }
                        }
                    }
                }
            }
        }
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
                    Text("FBLA Hub v1.3", color = primaryColor, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), modifier = Modifier.padding(bottom = 16.dp))
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
fun SpotlightTutorialOverlay(
    onDismiss: () -> Unit,
    primaryColor: Color,
    lightMode: Boolean,
    navBarBounds: Rect
) {
    data class TutorialStep(
        val title: String,
        val body: String,
        val tabIndex: Int? = null // 0=Profile,1=Events,2=Home,3=News,4=Resources; null=no tab
    )

    val steps = listOf(
        TutorialStep("Welcome to FBLA Hub", "This quick tour covers every feature. Tap Next to walk through it, or Skip to jump straight in."),
        TutorialStep("Home", "Your daily dashboard — upcoming competition reminders with live countdowns and a daily business tip. Tap the Ask AI button up top to chat with FBLA Hub AI anytime.", tabIndex = 2),
        TutorialStep("Networking Hub", "Right on Home: open Messages, scan a QR code to instantly connect with members, view connection requests, and join Classroom group chats.", tabIndex = 2),
        TutorialStep("Events & Competitions", "Browse FBLA competitions by state on a full calendar. Expand any card to set a reminder, share it to your Instagram story, or get AI prep tips.", tabIndex = 1),
        TutorialStep("Your Profile", "Set your name, chapter, grad year, state, and bio. Link your Instagram, X, Facebook, and LinkedIn, then connect and message other members.", tabIndex = 0),
        TutorialStep("News Feed", "Live articles from FBLA.org, auto-tagged by category. Tap to read in full or share in one tap.", tabIndex = 3),
        TutorialStep("Resources", "Official guidelines, competitive events, dress code, NLC info, and scholarships — plus the Study Hub with AI-generated practice tests & Q&A prep, the AI Dress Code Checker, and the national & chapter Instagram feeds.", tabIndex = 4),
        TutorialStep("You're all set!", "Reopen this tour anytime from Settings. Good luck at your competitions.")
    )

    val navTabs = listOf(
        Pair(Icons.Filled.Person, "Profile"),
        Pair(Icons.Filled.Event, "Events"),
        Pair(Icons.Filled.Home, "Home"),
        Pair(Icons.AutoMirrored.Filled.Article, "News"),
        Pair(Icons.AutoMirrored.Filled.MenuBook, "Resources")
    )

    var currentStep by remember { mutableIntStateOf(0) }
    val step = steps[currentStep]
    val isEndStep = step.tabIndex == null
    val cardColor = if (lightMode) Color.White else Color(0xFF0F1F38)
    val textColor = if (lightMode) Color(0xFF1A1A2E) else Color.White

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.80f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Progress dots (skip on welcome/done)
            if (!isEndStep) {
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    (1..steps.size - 2).forEach { i ->
                        Box(
                            modifier = Modifier
                                .size(if (i == currentStep) 20.dp else 7.dp, 7.dp)
                                .background(
                                    if (i == currentStep) primaryColor else Color.White.copy(alpha = 0.3f),
                                    RoundedCornerShape(4.dp)
                                )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Main card
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = cardColor),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isEndStep) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(primaryColor.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (currentStep == 0) Icons.Default.AutoAwesome else Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = primaryColor,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(18.dp))
                    }
                    Text(
                        step.title,
                        color = textColor,
                        fontSize = if (isEndStep) 26.sp else 22.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        step.body,
                        color = textColor.copy(alpha = 0.75f),
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (currentStep > 0) {
                            OutlinedButton(
                                onClick = { currentStep-- },
                                modifier = Modifier.weight(1f).pressScale(),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, textColor.copy(alpha = 0.25f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = textColor)
                            ) { Text("Back", fontWeight = FontWeight.Bold) }
                        }
                        Button(
                            onClick = { if (currentStep < steps.size - 1) currentStep++ else onDismiss() },
                            modifier = Modifier.weight(1f).pressScale(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                        ) {
                            Text(
                                if (currentStep == 0) "Start Tour" else if (currentStep == steps.size - 1) "Let's Go" else "Next",
                                color = Color(0xFF0A1628), fontWeight = FontWeight.Black
                            )
                        }
                    }
                    if (currentStep < steps.size - 1) {
                        Spacer(modifier = Modifier.height(6.dp))
                        TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                            Text("Skip tour", color = textColor.copy(alpha = 0.4f), fontSize = 13.sp)
                        }
                    }
                }
            }

            // Mini nav bar indicator — shows which tab this step is about
            if (!isEndStep && step.tabIndex != null) {
                Spacer(modifier = Modifier.height(24.dp))
                Text("▼", color = primaryColor, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (lightMode) Color(0xFFD8E4F8) else Color(0xFF000D1A), RoundedCornerShape(16.dp))
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    navTabs.forEachIndexed { i, (icon, label) ->
                        val isActive = i == step.tabIndex
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .background(
                                    if (isActive) primaryColor.copy(alpha = 0.15f) else Color.Transparent,
                                    RoundedCornerShape(10.dp)
                                )
                                .border(
                                    if (isActive) 1.5.dp else 0.dp,
                                    if (isActive) primaryColor else Color.Transparent,
                                    RoundedCornerShape(10.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                icon, null,
                                tint = if (isActive) primaryColor else Color.White.copy(alpha = 0.35f),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                label,
                                color = if (isActive) primaryColor else Color.White.copy(alpha = 0.35f),
                                fontSize = 10.sp,
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TutorialScreen(onDismiss: () -> Unit, primaryColor: Color = Color(0xFFFFB800), lightMode: Boolean = false) {
    var currentPage by remember { mutableIntStateOf(0) }

    data class TutorialPage(
        val title: String,
        val description: String,
        val mockup: @Composable () -> Unit
    )

    val pages = listOf(
        TutorialPage(
            title = "Your Profile",
            description = "Set up your name, chapter, graduation year, state, and bio. Add a photo and link your social media so connections can find you.",
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
            description = "Search for FBLA members by name, or scan a QR code to connect instantly. View their chapter, competitions, and bio — then send a connection request to link up.",
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
            title = "Messages & Classrooms",
            description = "Chat in real time with your connections — messages are instant. Join Classroom group chats to collaborate with your whole chapter in one place.",
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
                            Text("See you there.", color = Color.Black, fontSize = 11.sp)
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
            title = "Ask FBLA Hub AI",
            description = "Tap the Ask AI button on Home to chat with FBLA Hub AI. Get instant help with competition prep, app questions, conferences, and career tips.",
            mockup = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Question bubble
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Box(modifier = Modifier.background(primaryColor, RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 12.dp)).padding(horizontal = 12.dp, vertical = 8.dp)) {
                            Text("How do I prep for Business Law?", color = Color.Black, fontSize = 11.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    // AI reply bubble
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start, verticalAlignment = Alignment.Top) {
                        Box(modifier = Modifier.size(28.dp).background(primaryColor.copy(alpha = 0.25f), CircleShape), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.AutoAwesome, null, tint = primaryColor, modifier = Modifier.size(15.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomEnd = 12.dp)).padding(horizontal = 12.dp, vertical = 9.dp)) {
                            Box(modifier = Modifier.width(150.dp).height(7.dp).background(Color.White.copy(alpha = 0.65f), RoundedCornerShape(4.dp)))
                            Spacer(modifier = Modifier.height(5.dp))
                            Box(modifier = Modifier.width(120.dp).height(7.dp).background(Color.White.copy(alpha = 0.65f), RoundedCornerShape(4.dp)))
                            Spacer(modifier = Modifier.height(5.dp))
                            Box(modifier = Modifier.width(90.dp).height(7.dp).background(Color.White.copy(alpha = 0.65f), RoundedCornerShape(4.dp)))
                        }
                    }
                }
            }
        ),
        TutorialPage(
            title = "Events & Calendar",
            description = "Browse FBLA competitions by state. Tap any date on the calendar to filter events, and set reminders for the ones you're entering. Expand a card to share it to your Instagram story.",
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
            description = "Official FBLA guidelines, competitive events, toolkits, brand assets, dress code, NLC info, and scholarships — plus the FBLA Study Hub and the national & chapter Instagram feeds.",
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
                    listOf("Instagram", "X (Twitter)").forEach { name ->
                        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp).background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(8.dp)).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                            Icon(Icons.Default.OpenInNew, null, tint = primaryColor, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        ),
        TutorialPage(
            title = "Practice & Test Hub",
            description = "Inside Resources, open the Study Hub to sharpen your skills. Choose Practice Mode, a Full Test, or AI-powered Q&A prep — questions are generated by AI for any FBLA event.",
            mockup = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    listOf(
                        Triple("Practice Mode", "Feedback after each answer", Icons.Default.AutoStories),
                        Triple("Full Test", "Answer all, then see your score", Icons.Default.Assignment),
                        Triple("Q&A Prep", "AI-generated judge questions", Icons.Default.QuestionAnswer)
                    ).forEach { (label, sub, icon) ->
                        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(10.dp)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(34.dp).background(primaryColor.copy(alpha = 0.18f), RoundedCornerShape(9.dp)), contentAlignment = Alignment.Center) {
                                Icon(icon, null, tint = primaryColor, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text(sub, color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
                            }
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = primaryColor.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        ),
        TutorialPage(
            title = "AI Dress Code Checker",
            description = "Not sure if your outfit meets FBLA's business-professional dress code? Snap a photo in Resources and AI will check it against the official standards and give you tips.",
            mockup = {
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(modifier = Modifier.fillMaxWidth().height(70.dp).background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.CameraAlt, null, tint = primaryColor, modifier = Modifier.size(26.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Upload outfit photo", color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth().background(Color(0xFF1B5E20).copy(alpha = 0.4f), RoundedCornerShape(8.dp)).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF66BB6A), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Looks competition-ready!", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Box(modifier = Modifier.padding(top = 4.dp).fillMaxWidth(0.8f).height(6.dp).background(Color.White.copy(alpha = 0.5f), RoundedCornerShape(3.dp)))
                        }
                    }
                }
            }
        ),
        TutorialPage(
            title = "Customize",
            description = "Make FBLA Hub yours. Change the accent color with a full color picker, adjust background darkness, scale text size, or enable simplified UI.",
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
                        modifier = Modifier.height(48.dp).pressScale()
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
                    modifier = Modifier.height(48.dp).pressScale()
                ) {
                    Text(
                        if (pagerState.currentPage == pages.size - 1) "Let's Go" else "Next",
                        color = Color.Black,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

/** The email address members use to reach the FBLA Hub team. */
const val SUPPORT_EMAIL = "FBLAHub@gmail.com"

/**
 * Opens the phone's default email app with a message pre-addressed to the FBLA Hub team.
 * Falls back to a chooser so the user can pick whichever mail app they have installed.
 */
fun sendSupportEmail(context: Context, subject: String, body: String) {
    // Encode the subject and body straight into the mailto URI so the recipient,
    // subject, and message are all reliably pre-filled when the email app opens.
    // (Some mail apps ignore intent extras for ACTION_SENDTO, but always honor the URI.)
    val encodedSubject = Uri.encode(subject)
    val encodedBody = Uri.encode(body)
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:$SUPPORT_EMAIL?subject=$encodedSubject&body=$encodedBody")
        // Keep the extras too, as a belt-and-suspenders fallback for apps that prefer them.
        putExtra(Intent.EXTRA_EMAIL, arrayOf(SUPPORT_EMAIL))
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, body)
    }
    try {
        context.startActivity(Intent.createChooser(intent, "Send email"))
    } catch (e: Exception) {
        Toast.makeText(context, "No email app found. Reach us at $SUPPORT_EMAIL", Toast.LENGTH_LONG).show()
    }
}

val TERMS_OF_SERVICE_TEXT = """
Welcome to FBLA Hub! Before you create an account, here's the agreement between you and us. We've tried to keep it short and readable.

1. Who can use FBLA Hub
FBLA Hub is built for FBLA members, advisers, and students interested in FBLA. By signing up, you confirm that the information you give us — your name, chapter, graduation year, and state — is accurate to the best of your knowledge.

2. Your account
You're responsible for what happens on your account, so keep your password private. Don't impersonate other people, create accounts for someone else without their permission, or share login details.

3. Being respectful
FBLA Hub is a community. Don't post, send, or upload anything hateful, harassing, threatening, sexually explicit, or otherwise inappropriate. This includes your profile photo, username, bio, and messages. We use automated checks and member reports to help keep things clean, and we may remove content or accounts that break these rules.

4. Your content
You keep ownership of the photos and information you add. By posting them, you give us permission to display them inside the app so the app can actually work (for example, showing your profile to people you connect with).

5. Reporting and moderation
If you see an account or message that doesn't belong here, please report it. Reports go straight to our team and we review them. We may suspend or remove accounts that violate these terms.

6. Privacy
We store your profile and activity to run the app, using Firebase. We don't sell your personal information. Be thoughtful about what you choose to share publicly.

7. The app is provided "as is"
We work hard to keep FBLA Hub running and accurate, but we can't promise it will always be perfect or available. FBLA Hub is a student-built project and is not officially endorsed by FBLA-PBL, Inc.

8. Changes
We may update these terms as the app grows. If we make a big change, we'll let you know in the app.

9. Contact
Questions? Reach out anytime at $SUPPORT_EMAIL.

By checking the box, you're telling us you've read and agree to these terms. Thanks for being part of the community!
""".trimIndent()

@Composable
fun AuthScreen(onLoginSuccess: () -> Unit, onBack: () -> Unit, lightMode: Boolean = false, primaryColor: Color = Color(0xFFFFB800), bgOpacity: Float = 0.9f) {
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
    var username by remember { mutableStateOf("") }
    var usernameAvailable by remember { mutableStateOf<Boolean?>(null) }
    var isCheckingUsername by remember { mutableStateOf(false) }
    var showGoogleSetup by remember { mutableStateOf(false) }
    var pendingGoogleUid by remember { mutableStateOf("") }
    var pendingGoogleName by remember { mutableStateOf("") }
    var pendingGoogleFirstName by remember { mutableStateOf("") }
    var pendingGoogleLastName by remember { mutableStateOf("") }
    var pendingGoogleEmail by remember { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(false) }
    var show2FAVerification by remember { mutableStateOf(false) }
    var generated2FACode by remember { mutableStateOf("") }
    var userEmailFor2FA by remember { mutableStateOf("") }
    var pendingWelcomeName by remember { mutableStateOf("") }
    var pendingWelcomeEmail by remember { mutableStateOf("") }

    var agreedToTerms by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }

    var showForgotPassword by remember { mutableStateOf(false) }
    var forgotEmail by remember { mutableStateOf("") }
    var isSendingReset by remember { mutableStateOf(false) }
    var showPasswordReset by remember { mutableStateOf(false) }
    var resetEmail by remember { mutableStateOf("") }

    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val scope = rememberCoroutineScope()

    LaunchedEffect(username) {
        if (username.length >= 3) {
            val regex = Regex("^[a-z0-9_]{3,20}$")
            if (regex.matches(username) && !containsProfanity(username)) {
                isCheckingUsername = true
                usernameAvailable = null
                delay(600)
                try {
                    val doc = db.collection("usernames").document(username).get().await()
                    usernameAvailable = !doc.exists()
                } catch (e: Exception) {
                    usernameAvailable = null
                } finally {
                    isCheckingUsername = false
                }
            }
        } else {
            usernameAvailable = null
            isCheckingUsername = false
        }
    }

    val googleSignInClient = remember {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("183041371315-pl8tg9fb2clpfekfrhjtdg82cder8lji.apps.googleusercontent.com")
            .requestEmail()
            .build()
        GoogleSignIn.getClient(context, gso)
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account.idToken ?: return@rememberLauncherForActivityResult
            val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
            isLoading = true
            scope.launch {
                try {
                    val authResult = auth.signInWithCredential(firebaseCredential).await()
                    val uid = authResult.user!!.uid
                    val userDoc = db.collection("users").document(uid).get().await()
                    if (!userDoc.exists()) {
                        pendingGoogleUid = uid
                        val displayName = authResult.user?.displayName ?: ""
                        pendingGoogleName = displayName
                        val nameParts = displayName.trim().split(" ", limit = 2)
                        pendingGoogleFirstName = nameParts.getOrElse(0) { "" }
                        pendingGoogleLastName = nameParts.getOrElse(1) { "" }
                        pendingGoogleEmail = authResult.user?.email ?: ""
                        isLoading = false
                        showGoogleSetup = true
                    } else {
                        isLoading = false
                        onLoginSuccess()
                    }
                } catch (e: Exception) {
                    isLoading = false
                    Toast.makeText(context, "Sign-in failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: ApiException) {
            isLoading = false
        }
    }

    if (showGoogleSetup) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize().background(if (lightMode) Color(0xFFF0F4FF) else Color(0xFF0A1628)))
            if (!lightMode) Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = bgOpacity)))
            Column(modifier = Modifier.fillMaxSize()) {
                Row(modifier = Modifier.fillMaxWidth().padding(top = 40.dp, end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    BackButton(onClick = { auth.signOut(); showGoogleSetup = false }, tint = backTint)
                }
                LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    item {
                        Text("Complete Your Profile", style = MaterialTheme.typography.headlineMedium, color = titleColor, fontWeight = FontWeight.Black)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Just a few more details to set up your account.", color = mutedLine, fontSize = 14.sp, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(modifier = Modifier.weight(1f)) {
                                AuthTextField(value = pendingGoogleFirstName, onValueChange = { if (it.length <= 30) pendingGoogleFirstName = it }, label = "First Name", lightMode = lightMode, accentColor = primaryColor)
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                AuthTextField(value = pendingGoogleLastName, onValueChange = { if (it.length <= 30) pendingGoogleLastName = it }, label = "Last Name", lightMode = lightMode, accentColor = primaryColor)
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            listOf("student" to "Student", "teacher" to "Teacher").forEach { (value, label) ->
                                val selected = accountRole == value
                                Button(onClick = { accountRole = value }, modifier = Modifier.weight(1f).height(48.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = if (selected) primaryColor else if (lightMode) Color(0xFFE8EDF5) else Color.White.copy(alpha = 0.12f), contentColor = if (selected) Color.Black else if (lightMode) Color(0xFF1A1A2E) else Color.White),
                                    shape = RoundedCornerShape(12.dp)) { Text(label, fontWeight = FontWeight.Bold) }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        if (accountRole != "teacher") {
                            YearDropdown(selectedYear = year, onYearSelected = { year = it }, lightMode = lightMode, accentColor = primaryColor)
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                        StateDropdown(selectedState = statePref, onStateSelected = { statePref = it }, lightMode = lightMode, accentColor = primaryColor)
                        Spacer(modifier = Modifier.height(12.dp))
                        val gufg = if (lightMode) Color(0xFF1A1A2E) else Color.White
                        val guborder = if (lightMode) gufg.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.7f)
                        val guHasError = username.isNotEmpty() && (username.length < 3 || containsProfanity(username) || usernameAvailable == false)
                        val guIsValid = usernameAvailable == true
                        val guActiveBorder = when { guHasError -> Color(0xFFF44336); guIsValid -> Color(0xFF4CAF50); else -> primaryColor }
                        OutlinedTextField(
                            value = username,
                            onValueChange = { v -> if (v.length <= 20 && v.all { c -> c.isLowerCase() || c.isDigit() || c == '_' }) { username = v; usernameAvailable = null } },
                            label = { Text("Username") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                            trailingIcon = {
                                when {
                                    isCheckingUsername -> CircularProgressIndicator(modifier = Modifier.size(20.dp), color = primaryColor, strokeWidth = 2.dp)
                                    guIsValid -> Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(22.dp))
                                    guHasError -> Icon(Icons.Default.Cancel, null, tint = Color(0xFFF44336), modifier = Modifier.size(22.dp))
                                    else -> {}
                                }
                            },
                            supportingText = {
                                val r = when {
                                    username.isNotEmpty() && username.length < 3 -> "Must be at least 3 characters"
                                    username.isNotEmpty() && containsProfanity(username) -> "Username contains inappropriate words"
                                    usernameAvailable == false -> "Username already taken"
                                    usernameAvailable == true -> "Username available!"
                                    else -> null
                                }
                                if (r != null) Text(r, color = if (guIsValid) Color(0xFF4CAF50) else Color(0xFFF44336), fontSize = 11.sp)
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedTextColor = gufg, focusedTextColor = gufg,
                                unfocusedBorderColor = if (guHasError) Color(0xFFF44336).copy(alpha = 0.7f) else if (guIsValid) Color(0xFF4CAF50).copy(alpha = 0.7f) else guborder,
                                focusedBorderColor = guActiveBorder, cursorColor = primaryColor,
                                unfocusedLabelColor = gufg.copy(alpha = 0.8f), focusedLabelColor = guActiveBorder
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { agreedToTerms = !agreedToTerms },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = agreedToTerms,
                                onCheckedChange = { agreedToTerms = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = primaryColor,
                                    uncheckedColor = mutedLine.copy(alpha = 0.5f),
                                    checkmarkColor = Color.Black
                                )
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Row {
                                Text("I agree to the ", color = mutedLine, fontSize = 13.sp)
                                Text(
                                    "Terms of Service",
                                    color = primaryColor,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable { showTermsDialog = true }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = {
                                when {
                                    pendingGoogleFirstName.isBlank() -> Toast.makeText(context, "Please enter your first name", Toast.LENGTH_SHORT).show()
                                    pendingGoogleLastName.isBlank() -> Toast.makeText(context, "Please enter your last name", Toast.LENGTH_SHORT).show()
                                    accountRole != "teacher" && year.isBlank() -> Toast.makeText(context, "Please select your graduation year", Toast.LENGTH_SHORT).show()
                                    statePref.isBlank() -> Toast.makeText(context, "Please select your state", Toast.LENGTH_SHORT).show()
                                    username.isBlank() -> Toast.makeText(context, "Please choose a username", Toast.LENGTH_SHORT).show()
                                    username.length < 3 -> Toast.makeText(context, "Username must be at least 3 characters", Toast.LENGTH_SHORT).show()
                                    !Regex("^[a-z0-9_]{3,20}$").matches(username) -> Toast.makeText(context, "Username: lowercase letters, numbers, underscores only", Toast.LENGTH_SHORT).show()
                                    containsProfanity(username) -> Toast.makeText(context, "Username contains inappropriate words", Toast.LENGTH_SHORT).show()
                                    usernameAvailable != true -> Toast.makeText(context, "Please choose an available username", Toast.LENGTH_SHORT).show()
                                    !agreedToTerms -> Toast.makeText(context, "Please agree to the Terms of Service to continue", Toast.LENGTH_SHORT).show()
                                    else -> {
                                        isLoading = true
                                        scope.launch {
                                            try {
                                                val chosenUsername = username.trim()
                                                val fullName = "${pendingGoogleFirstName.trim()} ${pendingGoogleLastName.trim()}".trim()
                                                val profile = UserProfile(uid = pendingGoogleUid, name = fullName, email = pendingGoogleEmail, school = "", year = year, state = statePref, role = accountRole, username = chosenUsername, usernameLastChanged = System.currentTimeMillis())
                                                db.runTransaction { transaction ->
                                                    val usernameDoc = transaction.get(db.collection("usernames").document(chosenUsername))
                                                    if (usernameDoc.exists()) throw Exception("Username already taken")
                                                    transaction.set(db.collection("usernames").document(chosenUsername), mapOf("uid" to pendingGoogleUid))
                                                    transaction.set(db.collection("users").document(pendingGoogleUid), profile)
                                                }.await()
                                                launch {
                                                    try {
                                                        sendWelcomeEmail(pendingGoogleEmail, fullName)
                                                    } catch (e: Exception) {
                                                        android.util.Log.e("EmailJS", "Welcome email failed: ${e.message}", e)
                                                    }
                                                }
                                                isLoading = false
                                                onLoginSuccess()
                                            } catch (e: Exception) {
                                                isLoading = false
                                                val msg = if (e.message?.contains("already taken") == true) "Username already taken. Please choose another." else "Error: ${e.message}"
                                                withContext(Dispatchers.Main) { Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() }
                                            }
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp).shadow(4.dp, RoundedCornerShape(12.dp)).pressScale(),
                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                            shape = RoundedCornerShape(12.dp), enabled = !isLoading
                        ) {
                            if (isLoading) CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp))
                            else Text("CONTINUE", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (showTermsDialog) {
                AlertDialog(
                    onDismissRequest = { showTermsDialog = false },
                    containerColor = if (lightMode) Color.White else Color(0xFF1A2740),
                    title = { Text("Terms of Service", color = titleColor, fontWeight = FontWeight.Bold) },
                    text = {
                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                            Text(TERMS_OF_SERVICE_TEXT, color = mutedLine, style = MaterialTheme.typography.bodyMedium)
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { agreedToTerms = true; showTermsDialog = false }) {
                            Text("I AGREE", color = primaryColor, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showTermsDialog = false }) {
                            Text("CLOSE", color = mutedLine)
                        }
                    }
                )
            }
        }
        return
    }

    if (showPasswordReset) {
        PasswordResetScreen(
            email = resetEmail,
            lightMode = lightMode,
            bgOpacity = bgOpacity,
            primaryColor = primaryColor,
            onComplete = {
                showPasswordReset = false
                Toast.makeText(context, "Password updated! Please sign in.", Toast.LENGTH_LONG).show()
            },
            onBack = { showPasswordReset = false }
        )
        return
    }

    if (show2FAVerification) {
        TwoFactorVerificationScreen(
            email = userEmailFor2FA,
            expectedCode = generated2FACode,
            lightMode = lightMode,
            bgOpacity = bgOpacity,
            primaryColor = primaryColor,
            onVerified = {
                show2FAVerification = false
                if (pendingWelcomeEmail.isNotBlank()) {
                    val welcomeEmail = pendingWelcomeEmail
                    val welcomeName = pendingWelcomeName
                    pendingWelcomeEmail = ""
                    pendingWelcomeName = ""
                    scope.launch {
                        try {
                            sendWelcomeEmail(welcomeEmail, welcomeName)
                        } catch (e: Exception) {
                            android.util.Log.e("EmailJS", "Welcome email failed: ${e.message}", e)
                        }
                    }
                }
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
        Box(modifier = Modifier.fillMaxSize().background(if (lightMode) Color(0xFFF0F4FF) else Color(0xFF0A1628)))
        if (!lightMode) Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = bgOpacity)))

        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar with back button — sits above scroll, always tappable
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp, start = 0.dp, end = 16.dp, bottom = 0.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BackButton(onClick = onBack, tint = backTint)
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            listOf("student" to "Student", "teacher" to "Teacher").forEach { (value, label) ->
                                val selected = accountRole == value
                                Button(
                                    onClick = { accountRole = value },
                                    modifier = Modifier.weight(1f).height(48.dp).pressScale(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (selected) primaryColor else if (lightMode) Color(0xFFE8EDF5) else Color.White.copy(alpha = 0.12f),
                                        contentColor = if (selected) Color.Black else if (lightMode) Color(0xFF1A1A2E) else Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) { Text(label, fontWeight = FontWeight.Bold) }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        AuthTextField(value = firstName, onValueChange = { if (it.length <= 30) firstName = it }, label = "First Name", lightMode = lightMode, accentColor = primaryColor)
                        Spacer(modifier = Modifier.height(12.dp))
                        AuthTextField(value = lastName, onValueChange = { if (it.length <= 30) lastName = it }, label = "Last Name", lightMode = lightMode, accentColor = primaryColor)
                        Spacer(modifier = Modifier.height(12.dp))
                        if (accountRole != "teacher") {
                            YearDropdown(selectedYear = year, onYearSelected = { year = it }, lightMode = lightMode, accentColor = primaryColor)
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                        StateDropdown(selectedState = statePref, onStateSelected = { statePref = it }, lightMode = lightMode, accentColor = primaryColor)
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    AuthTextField(value = email, onValueChange = { email = it }, label = "Email Address", lightMode = lightMode, accentColor = primaryColor)
                    Spacer(modifier = Modifier.height(12.dp))
                    AuthTextField(value = password, onValueChange = { password = it }, label = "Password", isPassword = true, lightMode = lightMode, accentColor = primaryColor)
                    if (isLogin) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { forgotEmail = email; showForgotPassword = true }) {
                                Text("Forgot password?", color = primaryColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                    if (!isLogin) {
                        Spacer(modifier = Modifier.height(12.dp))
                        AuthTextField(value = confirmPassword, onValueChange = { confirmPassword = it }, label = "Confirm Password", isPassword = true, lightMode = lightMode, accentColor = primaryColor)
                        Spacer(modifier = Modifier.height(12.dp))
                        val ufg = if (lightMode) Color(0xFF1A1A2E) else Color.White
                        val uborder = if (lightMode) ufg.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.7f)
                        val uHasError = username.isNotEmpty() && (username.length < 3 || containsProfanity(username) || usernameAvailable == false)
                        val uIsValid = usernameAvailable == true
                        val uActiveBorder = when { uHasError -> Color(0xFFF44336); uIsValid -> Color(0xFF4CAF50); else -> primaryColor }
                        OutlinedTextField(
                            value = username,
                            onValueChange = { v ->
                                if (v.length <= 20 && v.all { c -> c.isLowerCase() || c.isDigit() || c == '_' }) {
                                    username = v
                                    usernameAvailable = null
                                }
                            },
                            label = { Text("Username") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            trailingIcon = {
                                when {
                                    isCheckingUsername -> CircularProgressIndicator(modifier = Modifier.size(20.dp), color = primaryColor, strokeWidth = 2.dp)
                                    uIsValid -> Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(22.dp))
                                    uHasError -> Icon(Icons.Default.Cancel, contentDescription = null, tint = Color(0xFFF44336), modifier = Modifier.size(22.dp))
                                    else -> {}
                                }
                            },
                            supportingText = {
                                val reason = when {
                                    username.isNotEmpty() && username.length < 3 -> "Must be at least 3 characters"
                                    username.isNotEmpty() && containsProfanity(username) -> "Username contains inappropriate words"
                                    usernameAvailable == false -> "Username already taken"
                                    usernameAvailable == true -> "Username available!"
                                    else -> null
                                }
                                if (reason != null) {
                                    Text(
                                        text = reason,
                                        color = if (uIsValid) Color(0xFF4CAF50) else Color(0xFFF44336),
                                        fontSize = 11.sp
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedTextColor = ufg,
                                focusedTextColor = ufg,
                                unfocusedBorderColor = if (uHasError) Color(0xFFF44336).copy(alpha = 0.7f) else if (uIsValid) Color(0xFF4CAF50).copy(alpha = 0.7f) else uborder,
                                focusedBorderColor = uActiveBorder,
                                cursorColor = primaryColor,
                                unfocusedLabelColor = ufg.copy(alpha = 0.8f),
                                focusedLabelColor = uActiveBorder
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    if (!isLogin) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { agreedToTerms = !agreedToTerms },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = agreedToTerms,
                                onCheckedChange = { agreedToTerms = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = primaryColor,
                                    uncheckedColor = mutedLine.copy(alpha = 0.5f),
                                    checkmarkColor = Color.Black
                                )
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Row {
                                Text("I agree to the ", color = mutedLine, fontSize = 13.sp)
                                Text(
                                    "Terms of Service",
                                    color = primaryColor,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable { showTermsDialog = true }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            if (!isLogin) {
                                when {
                                    firstName.isBlank() -> { Toast.makeText(context, "Please enter your first name", Toast.LENGTH_SHORT).show(); return@Button }
                                    lastName.isBlank() -> { Toast.makeText(context, "Please enter your last name", Toast.LENGTH_SHORT).show(); return@Button }
                                    accountRole != "teacher" && year.isBlank() -> { Toast.makeText(context, "Please select your graduation year", Toast.LENGTH_SHORT).show(); return@Button }
                                    statePref.isBlank() -> { Toast.makeText(context, "Please select your state", Toast.LENGTH_SHORT).show(); return@Button }
                                    email.isBlank() -> { Toast.makeText(context, "Please enter your email", Toast.LENGTH_SHORT).show(); return@Button }
                                    password.isEmpty() -> { Toast.makeText(context, "Please enter a password", Toast.LENGTH_SHORT).show(); return@Button }
                                    confirmPassword.isEmpty() -> { Toast.makeText(context, "Please confirm your password", Toast.LENGTH_SHORT).show(); return@Button }
                                    password != confirmPassword -> { Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show(); return@Button }
                                    username.isBlank() -> { Toast.makeText(context, "Please choose a username", Toast.LENGTH_SHORT).show(); return@Button }
                                    username.length < 3 -> { Toast.makeText(context, "Username must be at least 3 characters", Toast.LENGTH_SHORT).show(); return@Button }
                                    !Regex("^[a-z0-9_]{3,20}$").matches(username) -> { Toast.makeText(context, "Username: lowercase letters, numbers, underscores only", Toast.LENGTH_SHORT).show(); return@Button }
                                    containsProfanity(username) -> { Toast.makeText(context, "Username contains inappropriate words", Toast.LENGTH_SHORT).show(); return@Button }
                                    usernameAvailable != true -> { Toast.makeText(context, "Please choose an available username", Toast.LENGTH_SHORT).show(); return@Button }
                                    !agreedToTerms -> { Toast.makeText(context, "Please agree to the Terms of Service to continue", Toast.LENGTH_SHORT).show(); return@Button }
                                }
                            } else if (email.isBlank() || password.isEmpty()) {
                                Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
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
                                        pendingWelcomeEmail = ""
                                        pendingWelcomeName = ""

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
                                                    try {
                                                        sendVerificationEmail(email, code)
                                                    } catch (e: Exception) {
                                                        android.util.Log.e("EmailJS", "Failed: ${e.message}", e)
                                                        withContext(Dispatchers.Main) {
                                                            Toast.makeText(
                                                                context,
                                                                "Could not send verification email. Check spam or try again.",
                                                                Toast.LENGTH_LONG
                                                            ).show()
                                                        }
                                                    }
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
                                        val chosenUsername = username.trim()
                                        val uid = auth.currentUser!!.uid
                                        val profile = UserProfile(uid = uid, name = displayName, email = email, school = "", year = year, state = statePref, role = accountRole, username = chosenUsername, usernameLastChanged = System.currentTimeMillis())
                                        scope.launch {
                                            try {
                                                db.runTransaction { transaction ->
                                                    val usernameDoc = transaction.get(db.collection("usernames").document(chosenUsername))
                                                    if (usernameDoc.exists()) throw Exception("Username already taken")
                                                    transaction.set(db.collection("usernames").document(chosenUsername), mapOf("uid" to uid))
                                                    transaction.set(db.collection("users").document(uid), profile)
                                                }.await()
                                                val code = (100000..999999).random().toString()
                                                generated2FACode = code
                                                userEmailFor2FA = email
                                                val twoFactorCode = TwoFactorCode(
                                                    code = code,
                                                    email = email,
                                                    expiresAt = System.currentTimeMillis() + (5 * 60 * 1000),
                                                    verified = false
                                                )
                                                db.collection("twoFactorCodes").document(email).set(twoFactorCode).await()
                                                pendingWelcomeName = displayName
                                                pendingWelcomeEmail = email
                                                launch {
                                                    try {
                                                        sendVerificationEmail(email, code)
                                                    } catch (e: Exception) {
                                                        android.util.Log.e("EmailJS", "Failed: ${e.message}", e)
                                                        withContext(Dispatchers.Main) {
                                                            Toast.makeText(
                                                                context,
                                                                "Could not send verification email. Check spam or try again.",
                                                                Toast.LENGTH_LONG
                                                            ).show()
                                                        }
                                                    }
                                                }
                                                isLoading = false
                                                show2FAVerification = true
                                            } catch (e: Exception) {
                                                auth.currentUser?.delete()?.await()
                                                isLoading = false
                                                val msg = if (e.message?.contains("already taken") == true)
                                                    "Username already taken. Please choose another."
                                                else "Registration error: ${e.message}"
                                                withContext(Dispatchers.Main) {
                                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
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

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = mutedLine.copy(alpha = 0.3f))
                        Text("  or  ", color = mutedLine.copy(alpha = 0.6f), fontSize = 13.sp)
                        HorizontalDivider(modifier = Modifier.weight(1f), color = mutedLine.copy(alpha = 0.3f))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            googleSignInClient.signOut().addOnCompleteListener {
                                googleSignInLauncher.launch(googleSignInClient.signInIntent)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp).pressScale(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (lightMode) Color.White else Color.White.copy(alpha = 0.07f),
                            contentColor = if (lightMode) Color(0xFF1A1A2E) else Color.White
                        ),
                        border = BorderStroke(1.dp, mutedLine.copy(alpha = 0.25f)),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isLoading
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_google),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = Color.Unspecified
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Continue with Google", fontWeight = FontWeight.SemiBold)
                    }

                    TextButton(onClick = { isLogin = !isLogin }) {
                        Text(if (isLogin) "New here? Create an account" else "Have an account? Sign in", color = primaryColor, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (showTermsDialog) {
                AlertDialog(
                    onDismissRequest = { showTermsDialog = false },
                    containerColor = if (lightMode) Color.White else Color(0xFF1A2740),
                    title = { Text("Terms of Service", color = titleColor, fontWeight = FontWeight.Bold) },
                    text = {
                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                            Text(TERMS_OF_SERVICE_TEXT, color = mutedLine, style = MaterialTheme.typography.bodyMedium)
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { agreedToTerms = true; showTermsDialog = false }) {
                            Text("I AGREE", color = primaryColor, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showTermsDialog = false }) {
                            Text("CLOSE", color = mutedLine)
                        }
                    }
                )
            }

            if (showForgotPassword) {
                AlertDialog(
                    onDismissRequest = { if (!isSendingReset) showForgotPassword = false },
                    containerColor = if (lightMode) Color.White else Color(0xFF1A2740),
                    icon = { Icon(Icons.Default.LockReset, null, tint = primaryColor) },
                    title = { Text("Reset Password", color = titleColor, fontWeight = FontWeight.Bold) },
                    text = {
                        Column {
                            Text(
                                "Enter your account email and we'll send you an 8-digit code to reset your password.",
                                color = mutedLine,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            AuthTextField(value = forgotEmail, onValueChange = { forgotEmail = it }, label = "Email Address", lightMode = lightMode, accentColor = primaryColor)
                        }
                    },
                    confirmButton = {
                        TextButton(
                            enabled = !isSendingReset,
                            onClick = {
                                val target = forgotEmail.trim()
                                if (target.isBlank()) {
                                    Toast.makeText(context, "Please enter your email", Toast.LENGTH_SHORT).show()
                                    return@TextButton
                                }
                                isSendingReset = true
                                scope.launch {
                                    try {
                                        sendPasswordResetEmail(target)
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "If an account exists, a reset code is on the way.", Toast.LENGTH_LONG).show()
                                            isSendingReset = false
                                            showForgotPassword = false
                                            resetEmail = target
                                            showPasswordReset = true
                                        }
                                    } catch (e: Exception) {
                                        withContext(Dispatchers.Main) {
                                            isSendingReset = false
                                            Toast.makeText(context, "Couldn't send reset email: ${e.message}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            }
                        ) {
                            if (isSendingReset) CircularProgressIndicator(color = primaryColor, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            else Text("SEND CODE", color = primaryColor, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(enabled = !isSendingReset, onClick = { showForgotPassword = false }) {
                            Text("CANCEL", color = mutedLine)
                        }
                    }
                )
            }
        } // end Column
    }
}

/** Client-side mirror of the server strength check, for live feedback. */
fun passwordStrengthError(pw: String): String? = when {
    pw.length < 8 -> "Password must be at least 8 characters."
    !pw.any { it.isUpperCase() } -> "Password must include an uppercase letter."
    !pw.any { it.isLowerCase() } -> "Password must include a lowercase letter."
    !pw.any { it.isDigit() } -> "Password must include a number."
    else -> null
}

@Composable
private fun PasswordRequirementRow(label: String, met: Boolean, mutedLine: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
        Icon(
            if (met) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            null,
            tint = if (met) Color(0xFF4CAF50) else mutedLine.copy(alpha = 0.5f),
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(label, color = if (met) mutedLine else mutedLine.copy(alpha = 0.7f), fontSize = 13.sp)
    }
}

@Composable
fun PasswordResetScreen(
    email: String,
    lightMode: Boolean,
    bgOpacity: Float,
    primaryColor: Color,
    onComplete: () -> Unit,
    onBack: () -> Unit
) {
    val titleColor = if (lightMode) Color(0xFF1A1A2E) else Color.White
    val mutedLine = if (lightMode) titleColor.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.85f)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var step by remember { mutableStateOf(1) } // 1 = enter code, 2 = set new password
    var code by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isBusy by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize().background(if (lightMode) Color(0xFFF0F4FF) else Color(0xFF0A1628)))
        if (!lightMode) Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = bgOpacity)))
        Column(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxWidth().padding(top = 40.dp, end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                BackButton(onClick = onBack, tint = titleColor)
            }
            LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 24.dp)) {
                item {
                    Spacer(Modifier.height(8.dp))
                    Icon(Icons.Default.LockReset, null, tint = primaryColor, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("Reset Password", color = titleColor, fontWeight = FontWeight.Black, style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(6.dp))

                    if (step == 1) {
                        Text("Enter the 8-digit code we emailed to $email.", color = mutedLine, fontSize = 14.sp)
                        Spacer(Modifier.height(20.dp))
                        AuthTextField(value = code, onValueChange = { if (it.length <= 8 && it.all { c -> c.isDigit() }) { code = it; errorMsg = "" } }, label = "8-digit code", lightMode = lightMode, accentColor = primaryColor)
                        if (errorMsg.isNotEmpty()) { Spacer(Modifier.height(8.dp)); Text(errorMsg, color = Color(0xFFF44336), fontSize = 13.sp) }
                        Spacer(Modifier.height(20.dp))
                        Button(
                            onClick = {
                                if (code.length != 8) { errorMsg = "Please enter the 8-digit code from your email"; return@Button }
                                isBusy = true; errorMsg = ""
                                scope.launch {
                                    try {
                                        val (valid, reason) = verifyPasswordResetCode(email, code.trim())
                                        withContext(Dispatchers.Main) {
                                            isBusy = false
                                            if (valid) step = 2 else errorMsg = reason.ifEmpty { "Invalid code" }
                                        }
                                    } catch (e: Exception) {
                                        withContext(Dispatchers.Main) { isBusy = false; errorMsg = "Couldn't verify: ${e.message}" }
                                    }
                                }
                            },
                            enabled = !isBusy,
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isBusy) CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(22.dp))
                            else Text("VERIFY", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(4.dp))
                        TextButton(onClick = {
                            scope.launch {
                                try {
                                    sendPasswordResetEmail(email)
                                    withContext(Dispatchers.Main) { Toast.makeText(context, "A new code is on the way", Toast.LENGTH_SHORT).show() }
                                } catch (_: Exception) {}
                            }
                        }) { Text("Resend code", color = primaryColor, fontWeight = FontWeight.Bold) }
                    } else {
                        Text("Choose a new password. It must be different from your old one.", color = mutedLine, fontSize = 14.sp)
                        Spacer(Modifier.height(20.dp))
                        AuthTextField(value = newPassword, onValueChange = { newPassword = it; errorMsg = "" }, label = "New password", isPassword = true, lightMode = lightMode, accentColor = primaryColor)
                        Spacer(Modifier.height(12.dp))
                        AuthTextField(value = confirmPassword, onValueChange = { confirmPassword = it; errorMsg = "" }, label = "Confirm new password", isPassword = true, lightMode = lightMode, accentColor = primaryColor)
                        Spacer(Modifier.height(16.dp))
                        PasswordRequirementRow("At least 8 characters", newPassword.length >= 8, mutedLine)
                        PasswordRequirementRow("An uppercase letter", newPassword.any { it.isUpperCase() }, mutedLine)
                        PasswordRequirementRow("A lowercase letter", newPassword.any { it.isLowerCase() }, mutedLine)
                        PasswordRequirementRow("A number", newPassword.any { it.isDigit() }, mutedLine)
                        if (errorMsg.isNotEmpty()) { Spacer(Modifier.height(8.dp)); Text(errorMsg, color = Color(0xFFF44336), fontSize = 13.sp) }
                        Spacer(Modifier.height(20.dp))
                        Button(
                            onClick = {
                                val strengthErr = passwordStrengthError(newPassword)
                                when {
                                    strengthErr != null -> { errorMsg = strengthErr; return@Button }
                                    newPassword != confirmPassword -> { errorMsg = "Passwords do not match"; return@Button }
                                }
                                isBusy = true; errorMsg = ""
                                scope.launch {
                                    try {
                                        confirmPasswordReset(email, code.trim(), newPassword)
                                        withContext(Dispatchers.Main) { isBusy = false; onComplete() }
                                    } catch (e: Exception) {
                                        withContext(Dispatchers.Main) { isBusy = false; errorMsg = e.message ?: "Could not reset password" }
                                    }
                                }
                            },
                            enabled = !isBusy,
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isBusy) CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(22.dp))
                            else Text("UPDATE PASSWORD", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AccountDeletionScreen(
    email: String,
    lightMode: Boolean,
    bgOpacity: Float,
    primaryColor: Color,
    onConfirmed: () -> Unit,
    onBack: () -> Unit
) {
    val titleColor = if (lightMode) Color(0xFF1A1A2E) else Color.White
    val mutedLine = if (lightMode) titleColor.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.85f)
    val danger = Color(0xFFDC143C)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var code by remember { mutableStateOf("") }
    var isBusy by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize().background(if (lightMode) Color(0xFFF0F4FF) else Color(0xFF0A1628)))
        if (!lightMode) Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = bgOpacity)))
        Column(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxWidth().padding(top = 40.dp, end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                BackButton(onClick = onBack, tint = titleColor)
            }
            LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 24.dp)) {
                item {
                    Spacer(Modifier.height(8.dp))
                    Icon(Icons.Default.Warning, null, tint = danger, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("Delete Account", color = danger, fontWeight = FontWeight.Black, style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "This is permanent — all of your data will be deleted. Enter the confirmation code we emailed to $email to continue.",
                        color = mutedLine,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(20.dp))
                    AuthTextField(value = code, onValueChange = { code = it; errorMsg = "" }, label = "Confirmation code", lightMode = lightMode, accentColor = danger)
                    if (errorMsg.isNotEmpty()) { Spacer(Modifier.height(8.dp)); Text(errorMsg, color = danger, fontSize = 13.sp) }
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = {
                            if (code.isBlank()) { errorMsg = "Please enter the code from your email"; return@Button }
                            isBusy = true; errorMsg = ""
                            scope.launch {
                                try {
                                    val (valid, reason) = verifyAccountDeletionCode(email, code.trim())
                                    withContext(Dispatchers.Main) {
                                        isBusy = false
                                        if (valid) onConfirmed() else errorMsg = reason.ifEmpty { "Invalid code" }
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) { isBusy = false; errorMsg = "Couldn't verify: ${e.message}" }
                                }
                            }
                        },
                        enabled = !isBusy,
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = danger),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isBusy) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp))
                        else Text("DELETE MY ACCOUNT", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(4.dp))
                    TextButton(onClick = {
                        scope.launch {
                            try {
                                sendAccountDeletionCode(email, FirebaseAuth.getInstance().currentUser?.displayName.orEmpty())
                                withContext(Dispatchers.Main) { Toast.makeText(context, "A new code is on the way", Toast.LENGTH_SHORT).show() }
                            } catch (_: Exception) {}
                        }
                    }) { Text("Resend code", color = primaryColor, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

@Composable
fun TwoFactorVerificationScreen(email: String, expectedCode: String, onVerified: () -> Unit, onBack: () -> Unit, lightMode: Boolean = false, bgOpacity: Float = 0.9f, primaryColor: Color = Color(0xFFFFB800)) {
    var enteredCode by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
// Background replaced with solid FBLA dark blue
        Box(modifier = Modifier.fillMaxSize().background(if (lightMode) Color(0xFFF0F4FF) else Color(0xFF0A1628)))
        if (!lightMode) Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = bgOpacity)))

        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar with back button — always above scroll content
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp, start = 0.dp, end = 16.dp, bottom = 0.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BackButton(onClick = onBack, tint = Color.White)
            }

            Column(
                modifier = Modifier.weight(1f).padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.Lock, null, modifier = Modifier.size(80.dp), tint = primaryColor)
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
                    color = primaryColor,
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
                        focusedBorderColor = primaryColor
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
                    modifier = Modifier.fillMaxWidth().height(56.dp).shadow(4.dp, RoundedCornerShape(12.dp)).pressScale(),
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
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
            }
        } // end inner Column
    } // end outer Column
}

@Composable
fun AuthTextField(value: String, onValueChange: (String) -> Unit, label: String, isPassword: Boolean = false, lightMode: Boolean = false, accentColor: Color = Color(0xFFFFB800)) {
    val fg = if (lightMode) Color(0xFF1A1A2E) else Color.White
    val border = if (lightMode) fg.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.7f)
    var passwordVisible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        trailingIcon = if (isPassword) ({
            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                Icon(
                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = if (passwordVisible) "Hide password" else "Show password",
                    tint = fg.copy(alpha = 0.7f)
                )
            }
        }) else null,
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

fun shareMemberProfile(context: Context, name: String, chapter: String, year: String, state: String, bio: String = "") {
    val bioLine = if (bio.isNotBlank()) "\n\n$bio" else ""
    val body = "$name on FBLA Hub\n\nChapter: ${chapter.ifBlank { "—" }}\nClass of: ${year.ifBlank { "—" }} • ${state.ifBlank { "—" }}$bioLine\n\n#FBLA #FBLAHub"
    sharePlainText(context, "$name — FBLA Hub", body, "Share profile")
}

@Composable
fun BusinessCardComposable(
    name: String,
    username: String,
    chapter: String,
    year: String,
    state: String,
    bio: String,
    primaryColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.background(Color(0xFF0A1628))
    ) {
        // Decorative circles
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(color = primaryColor.copy(alpha = 0.07f), radius = size.width * 0.35f, center = androidx.compose.ui.geometry.Offset(size.width, 0f))
            drawCircle(color = primaryColor.copy(alpha = 0.07f), radius = size.width * 0.24f, center = androidx.compose.ui.geometry.Offset(0f, size.height))
            // Left accent bar
            drawRect(color = primaryColor, topLeft = androidx.compose.ui.geometry.Offset(0f, 0f), size = androidx.compose.ui.geometry.Size(6f, size.height))
            // Bottom accent strip
            drawRect(color = primaryColor.copy(alpha = 0.14f), topLeft = androidx.compose.ui.geometry.Offset(0f, size.height - size.height * 0.1f), size = androidx.compose.ui.geometry.Size(size.width, size.height * 0.1f))
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top row: FBLA HUB brand
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Text(
                    "FBLA HUB",
                    color = primaryColor,
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp,
                    letterSpacing = 2.sp
                )
            }

            // Main content
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = name,
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 26.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (username.isNotEmpty()) {
                    Text(
                        text = "@$username",
                        color = primaryColor,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.12f), thickness = 1.dp)
                Spacer(modifier = Modifier.height(6.dp))
                if (chapter.isNotEmpty()) {
                    Text(chapter, color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
                val classLine = buildString {
                    if (year.isNotEmpty()) append("Class of $year")
                    if (year.isNotEmpty() && state.isNotEmpty()) append("  ·  ")
                    if (state.isNotEmpty()) append(state)
                }
                if (classLine.isNotEmpty()) {
                    Text(classLine, color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                }
                if (bio.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = bio,
                        color = Color.White.copy(alpha = 0.45f),
                        fontSize = 10.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Footer
            Text(
                "Connect on FBLA Hub",
                color = primaryColor.copy(alpha = 0.75f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
    }
}

fun generateBusinessCardBitmap(
    name: String, username: String, chapter: String,
    year: String, state: String, bio: String, primaryColorInt: Int
): Bitmap {
    val W = 1050; val H = 600
    val bm = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bm)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    paint.color = 0xFF0A1628.toInt()
    canvas.drawRect(0f, 0f, W.toFloat(), H.toFloat(), paint)

    paint.color = primaryColorInt; paint.alpha = 18
    canvas.drawCircle(W.toFloat(), 0f, 320f, paint)
    canvas.drawCircle(0f, H.toFloat(), 220f, paint)
    paint.alpha = 255

    paint.color = primaryColorInt
    canvas.drawRect(0f, 0f, 10f, H.toFloat(), paint)
    paint.alpha = 35
    canvas.drawRect(0f, H - 60f, W.toFloat(), H.toFloat(), paint)
    paint.alpha = 255

    paint.color = primaryColorInt
    paint.textSize = 24f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    paint.letterSpacing = 0.18f
    val brandText = "FBLA HUB"
    canvas.drawText(brandText, W - paint.measureText(brandText) - 40f, 48f, paint)

    paint.color = 0xFFFFFFFF.toInt()
    paint.textSize = 68f
    paint.letterSpacing = 0f
    val displayName = if (paint.measureText(name) > W - 100f) name.take(22) + "…" else name
    canvas.drawText(displayName, 44f, 158f, paint)

    var y = 202f
    if (username.isNotEmpty()) {
        paint.color = primaryColorInt; paint.textSize = 34f
        canvas.drawText("@$username", 44f, y, paint); y += 44f
    }

    paint.color = 0xFFFFFFFF.toInt(); paint.alpha = 22
    canvas.drawRect(44f, y + 6f, W - 44f, y + 9f, paint)
    paint.alpha = 255; y += 36f

    paint.textSize = 30f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    if (chapter.isNotEmpty()) {
        paint.color = 0xFFFFFFFF.toInt(); paint.alpha = 210
        canvas.drawText(chapter, 44f, y, paint); y += 44f
    }

    val classLine = buildString {
        if (year.isNotEmpty()) append("Class of $year")
        if (year.isNotEmpty() && state.isNotEmpty()) append("  ·  ")
        if (state.isNotEmpty()) append(state)
    }
    if (classLine.isNotEmpty()) {
        paint.color = 0xFFFFFFFF.toInt(); paint.alpha = 160
        canvas.drawText(classLine, 44f, y, paint); y += 44f
    }

    if (bio.isNotEmpty()) {
        y += 6f; paint.textSize = 26f
        paint.color = 0xFFFFFFFF.toInt(); paint.alpha = 120
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        val maxW = W - 88f; var line = ""; var drawn = 0
        for (word in bio.split(" ")) {
            val test = if (line.isEmpty()) word else "$line $word"
            if (paint.measureText(test) <= maxW) line = test
            else {
                if (drawn < 2) { canvas.drawText(line, 44f, y, paint); y += 36f; drawn++ }
                line = word
            }
        }
        if (line.isNotEmpty() && drawn < 2) canvas.drawText(if (drawn == 1 && bio.length > 70) line.take(38) + "…" else line, 44f, y, paint)
    }

    paint.color = primaryColorInt; paint.alpha = 200
    paint.textSize = 22f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    paint.letterSpacing = 0.06f
    canvas.drawText("Connect on FBLA Hub", 44f, H - 20f, paint)

    return bm
}

fun shareBusinessCard(context: Context, bitmap: Bitmap, name: String) {
    try {
        val file = File(context.cacheDir, "fbla_card_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { fos -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos) }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "$name — FBLA Hub #FBLA #FBLAHub")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Business Card"))
    } catch (e: Exception) {
        Toast.makeText(context, "Could not generate card: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
