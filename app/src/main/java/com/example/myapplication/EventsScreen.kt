package com.example.myapplication

import android.content.ContentValues
import android.provider.CalendarContract
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
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
import androidx.camera.core.Preview


@Composable
fun CalendarView(
    selectedDate: Calendar?,
    onDateSelected: (Calendar) -> Unit,
    eventDates: Set<String>,
    primaryColor: Color,
    textSizeMultiplier: Float,
    lightMode: Boolean = false
) {
    val textColor = if (lightMode) Color(0xFF1A1A2E) else Color.White
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

    val reduceMotion = LocalReduceMotion.current
    val textColor = if (lightMode) Color(0xFF1A1A2E) else Color.White
    val textColorSecondary = if (lightMode) Color(0xFF1A1A2E).copy(alpha = 0.8f) else Color.White.copy(alpha = 0.9f)
    val textColorTertiary = if (lightMode) Color(0xFF1A1A2E).copy(alpha = 0.75f) else Color.White.copy(alpha = 0.85f)

    var selectedState by remember { mutableStateOf(userState) }
    var expanded by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf<Calendar?>(null) }
    var showListView by remember { mutableStateOf(false) }
    var userReminders by remember { mutableStateOf<List<CompetitionReminder>>(emptyList()) }
    var showStoryBuilderForEvent by remember { mutableStateOf<FblaEvent?>(null) }
    var storyBuilderCompetingInEvents by remember { mutableStateOf("") }
    var userPfpUrl by remember { mutableStateOf("") }
    var showReminderOptionsEvent by remember { mutableStateOf<FblaEvent?>(null) }
    var showShareOptionsEvent by remember { mutableStateOf<FblaEvent?>(null) }

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
                userPfpUrl = data?.pfpUrl ?: ""
            }
        }
    }

    val nationalConferences = listOf(
        FblaEvent("National Leadership Conference (NLC) 2026", "June 29 – July 2, 2026", "San Antonio, TX", 1782691200000L),
        FblaEvent("National Leadership Conference (NLC) 2027", "June 23–26, 2027", "Columbus, OH", 1813708800000L),
        FblaEvent("National Leadership Conference (NLC) 2028", "June 23–26, 2028", "Anaheim, CA", 1845331200000L),
        FblaEvent("National Leadership Conference (NLC) 2029", "June 25–28, 2029", "Indianapolis, IN", 1877040000000L)
    )
    val eventsByState = remember {
        val statesList = listOf("Alabama", "Alaska", "Arizona", "Arkansas", "California", "Colorado", "Connecticut", "Delaware", "Florida", "Georgia", "Hawaii", "Idaho", "Illinois", "Indiana", "Iowa", "Kansas", "Kentucky", "Louisiana", "Maine", "Maryland", "Massachusetts", "Michigan", "Minnesota", "Mississippi", "Missouri", "Montana", "Nebraska", "Nevada", "New Hampshire", "New Jersey", "New Mexico", "New York", "North Carolina", "North Dakota", "Ohio", "Oklahoma", "Oregon", "Pennsylvania", "Rhode Island", "South Carolina", "South Dakota", "Tennessee", "Texas", "Utah", "Vermont", "Virginia", "Washington", "West Virginia", "Wisconsin", "Wyoming")
        val baseEvents = mapOf(
            "Alabama" to listOf(FblaEvent("District Workshops", "Jan-Feb 2026", "Various Locations"), FblaEvent("State Leadership Conference", "April 9–10, 2026", "Mobile", 1775710800000L)),
            "California" to listOf(FblaEvent("Section Leadership Conferences", "Feb-March 2026", "Various Sections"), FblaEvent("State Leadership Conference", "April 23–26, 2026", "Sacramento", 1776920400000L)),
            "Texas" to listOf(FblaEvent("Area Leadership Conferences", "Jan-Feb 2026", "Areas 1-9"), FblaEvent("State Leadership Conference", "March 25–27, 2026", "Galveston", 1774414800000L)),
            "Wisconsin" to listOf(FblaEvent("Region 1 Leadership Conference", "Feb 7, 2026", "Shell Lake", 1770444000000L), FblaEvent("State Leadership Conference", "April 13–14, 2026", "Green Bay", 1776056400000L))
        )
        statesList.associateWith { (baseEvents[it] ?: listOf(FblaEvent("Regional Leadership Conference", "Jan-Feb 2026", "Various"), FblaEvent("State Leadership Conference", "March-April 2026", "TBD", 1774674000000L))) + nationalConferences }
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
            if (lightMode) Color(0xFFF0F4FF) else Color(0xFF0A1628)
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
                    text = if (selectedState.isEmpty()) "Events" else "$selectedState Events",
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
                                if (daysUntilNLC == 0L) {
                                    Text(
                                        text = "Today",
                                        color = textColor,
                                        style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Black, fontSize = (56 * textSizeMultiplier).sp),
                                        lineHeight = (56 * textSizeMultiplier).sp
                                    )
                                    Column(modifier = Modifier.padding(bottom = 6.dp)) {
                                        Text("NLC IS", color = primaryColor, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black, fontSize = (12 * textSizeMultiplier).sp, letterSpacing = 2.sp))
                                        Text("HERE", color = textColorTertiary, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = (10 * textSizeMultiplier).sp, letterSpacing = 1.sp))
                                    }
                                } else {
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
                                animationSpec = if (reduceMotion) snap() else spring(
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
                                    animationSpec = if (reduceMotion) snap() else spring(
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
                                HorizontalDivider(color = if (lightMode) Color(0xFF1A1A2E).copy(alpha = 0.08f) else Color.White.copy(alpha = 0.1f))
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
                            modifier = Modifier.fillMaxWidth().height(56.dp).pressScale(),
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
                            modifier = Modifier.weight(1f).height(64.dp).pressScale(),
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
                            modifier = Modifier.weight(1f).height(64.dp).pressScale(),
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
                                animationSpec = if (reduceMotion) snap() else spring(
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

                                Button(
                                    onClick = { showShareOptionsEvent = event },
                                    modifier = Modifier.fillMaxWidth().pressScale(),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                                ) {
                                    Icon(Icons.Default.Share, null, tint = Color(0xFF0A1628), modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Add to Story", fontWeight = FontWeight.Bold, style = TextStyle(fontSize = (14 * textSizeMultiplier).sp), color = Color(0xFF0A1628))
                                }

                                Spacer(modifier = Modifier.height(8.dp))

// Check if reminder already exists
                                val hasReminder = userReminders.any { it.name == event.name && it.date == event.date }

                                Button(
                                    onClick = {
                                        if (!hasReminder) showReminderOptionsEvent = event
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (hasReminder) Color.Gray else primaryColor,
                                        contentColor = if (hasReminder) Color.White.copy(alpha = 0.85f) else Color.Black
                                    ),
                                    modifier = Modifier.fillMaxWidth().pressScale(),
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

    // Story builder overlay (Instagram Story only)
    showStoryBuilderForEvent?.let { event ->
        StoryBuilderScreen(
            competitionName = event.name,
            initialCompetingIn = storyBuilderCompetingInEvents,
            onCompetingInChange = { storyBuilderCompetingInEvents = it },
            primaryColor = primaryColor,
            lightMode = lightMode,
            context = context,
            pfpUrl = userPfpUrl,
            onDismiss = { showStoryBuilderForEvent = null }
        )
    }

    // Add to Story dialog
    showShareOptionsEvent?.let { ev ->
        AlertDialog(
            onDismissRequest = { showShareOptionsEvent = null },
            containerColor = if (lightMode) Color.White else Color(0xFF1A2540),
            shape = RoundedCornerShape(20.dp),
            icon = {
                Box(
                    modifier = Modifier.size(56.dp).background(primaryColor.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Share, null, tint = primaryColor, modifier = Modifier.size(28.dp))
                }
            },
            title = {
                Text("Add to Story", fontWeight = FontWeight.Black, fontSize = 20.sp, color = if (lightMode) Color(0xFF1A1A2E) else Color.White)
            },
            text = {
                Text(
                    "Where do you want to share \"${ev.name}\"?",
                    textAlign = TextAlign.Center,
                    color = if (lightMode) Color(0xFF1A1A2E).copy(alpha = 0.75f) else Color.White.copy(alpha = 0.8f)
                )
            },
            confirmButton = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Instagram + LinkedIn side by side
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Instagram Story
                        Button(
                            onClick = {
                                storyBuilderCompetingInEvents = ""
                                showStoryBuilderForEvent = ev
                                showShareOptionsEvent = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDD2A7B)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).pressScale()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .background(
                                        brush = Brush.linearGradient(listOf(Color(0xFFF58529), Color(0xFFDD2A7B), Color(0xFF8134AF))),
                                        shape = RoundedCornerShape(4.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.CameraAlt, null, tint = Color.White, modifier = Modifier.size(10.dp))
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Instagram", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        // LinkedIn
                        Button(
                            onClick = {
                                scope.launch(Dispatchers.IO) {
                                    val pfpBitmap = if (userPfpUrl.isNotEmpty()) {
                                        try {
                                            val req = okhttp3.Request.Builder().url(userPfpUrl).build()
                                            val bytes = OkHttpClient().newCall(req).execute().body?.bytes()
                                            if (bytes != null) BitmapFactory.decodeByteArray(bytes, 0, bytes.size) else null
                                        } catch (e: Exception) { null }
                                    } else null
                                    withContext(Dispatchers.Main) {
                                        shareCompetitionToStory(
                                            context = context,
                                            attendingEvent = ev.name,
                                            competingIn = "",
                                            bgHex = "#0A1628",
                                            accentHex = "#FFB800",
                                            platform = "linkedin",
                                            pfpBitmap = pfpBitmap
                                        )
                                    }
                                }
                                showShareOptionsEvent = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0A66C2)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).pressScale()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .background(Color.White, RoundedCornerShape(3.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("in", color = Color(0xFF0A66C2), fontSize = 9.sp, fontWeight = FontWeight.Black)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("LinkedIn", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                    // Share as text
                    OutlinedButton(
                        onClick = {
                            val text = buildString {
                                append("🏆 ${ev.name}\n")
                                if (ev.date.isNotBlank()) append("📅 ${ev.date}\n")
                                if (ev.location.isNotBlank()) append("📍 ${ev.location}\n")
                                append("\n#FBLA #FBLAHub")
                            }
                            sharePlainText(context, ev.name, text, "Share competition")
                            showShareOptionsEvent = null
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.5.dp, primaryColor),
                        modifier = Modifier.fillMaxWidth().pressScale()
                    ) {
                        Icon(Icons.Default.Share, null, tint = primaryColor, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Share as Text", color = primaryColor, fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = { showShareOptionsEvent = null }, modifier = Modifier.fillMaxWidth()) {
                        Text("Cancel", color = if (lightMode) Color(0xFF1A1A2E).copy(alpha = 0.6f) else Color.White.copy(alpha = 0.6f))
                    }
                }
            },
            dismissButton = {}
        )
    }

    // Set Reminder options dialog
    showReminderOptionsEvent?.let { ev ->
        AlertDialog(
            onDismissRequest = { showReminderOptionsEvent = null },
            containerColor = if (lightMode) Color.White else Color(0xFF1A2540),
            shape = RoundedCornerShape(20.dp),
            icon = {
                Box(
                    modifier = Modifier.size(56.dp).background(primaryColor.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.NotificationsActive, null, tint = primaryColor, modifier = Modifier.size(28.dp))
                }
            },
            title = {
                Text(
                    "Save Reminder",
                    color = if (lightMode) Color(0xFF1A1A2E) else Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp
                )
            },
            text = {
                Text(
                    "Where would you like to save \"${ev.name}\"?",
                    color = if (lightMode) Color(0xFF1A1A2E).copy(alpha = 0.75f) else Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val currentUid = auth.currentUser?.uid
                            if (!currentUid.isNullOrEmpty()) {
                                val reminder = CompetitionReminder(ev.name, ev.date, ev.timestamp)
                                db.collection("users").document(currentUid)
                                    .update("reminders", FieldValue.arrayUnion(reminder))
                                    .addOnSuccessListener {
                                        Toast.makeText(context, "Reminder Set!", Toast.LENGTH_SHORT).show()
                                    }
                                val daysFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                                    timeZone = TimeZone.getTimeZone("UTC")
                                }
                                val eventDays = parseEventDates(ev.date)
                                val startMs = eventDays.firstOrNull()?.let { daysFmt.parse(it)?.time } ?: ev.timestamp
                                val endMs = eventDays.lastOrNull()?.let {
                                    val c = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                                    c.time = daysFmt.parse(it)!!
                                    c.add(Calendar.DAY_OF_MONTH, 1)
                                    c.timeInMillis
                                } ?: (startMs + 24 * 60 * 60 * 1000L)
                                context.startActivity(
                                    Intent(Intent.ACTION_INSERT, CalendarContract.Events.CONTENT_URI).apply {
                                        putExtra(CalendarContract.Events.TITLE, ev.name)
                                        putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMs)
                                        putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMs)
                                        putExtra(CalendarContract.Events.ALL_DAY, 1)
                                        putExtra(CalendarContract.Events.DESCRIPTION, "FBLA Competition — added via FBLA Hub")
                                        putExtra(CalendarContract.Events.EVENT_LOCATION, ev.location)
                                        putExtra(CalendarContract.Events.HAS_ALARM, 1)
                                    }
                                )
                            }
                            showReminderOptionsEvent = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().pressScale()
                    ) {
                        Icon(Icons.Default.CalendarMonth, null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Home Screen + Calendar", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = {
                            val currentUid = auth.currentUser?.uid
                            if (!currentUid.isNullOrEmpty()) {
                                val reminder = CompetitionReminder(ev.name, ev.date, ev.timestamp)
                                db.collection("users").document(currentUid)
                                    .update("reminders", FieldValue.arrayUnion(reminder))
                                    .addOnSuccessListener {
                                        Toast.makeText(context, "Reminder Set!", Toast.LENGTH_SHORT).show()
                                    }
                            }
                            showReminderOptionsEvent = null
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().pressScale(),
                        border = BorderStroke(1.dp, if (lightMode) Color(0xFF1A1A2E).copy(alpha = 0.3f) else Color.White.copy(alpha = 0.3f))
                    ) {
                        Icon(Icons.Default.Home, null, tint = if (lightMode) Color(0xFF1A1A2E) else Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Home Screen Only", color = if (lightMode) Color(0xFF1A1A2E) else Color.White, fontWeight = FontWeight.Bold)
                    }
                    TextButton(
                        onClick = { showReminderOptionsEvent = null },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cancel", color = if (lightMode) Color(0xFF1A1A2E).copy(alpha = 0.5f) else Color.White.copy(alpha = 0.5f))
                    }
                }
            },
            dismissButton = {}
        )
    }
}


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

// FBLA competitive events — used by the story builder so members select a real
// event instead of free-typing whatever they want.
val fblaCompetitiveEvents = listOf(
    "Accounting I", "Accounting II", "Agribusiness", "Banking & Financial Systems",
    "Business Calculations", "Business Communication", "Business Ethics", "Business Law",
    "Business Math", "Business Plan", "Client Service", "Computer Applications",
    "Computer Problem Solving", "Cybersecurity", "Database Design & Applications",
    "Digital Video Production", "E-business", "Economics", "Electronic Career Portfolio",
    "Emerging Business Issues", "Entrepreneurship", "Future Business Educator",
    "Global Business", "Graphic Design", "Healthcare Administration", "Help Desk",
    "Hospitality & Event Management", "Human Resource Management", "Impromptu Speaking",
    "Insurance & Risk Management", "Introduction to Business",
    "Introduction to Business Communication", "Introduction to Financial Math",
    "Introduction to Information Technology", "Introduction to Parliamentary Procedure",
    "Job Interview", "Management Decision Making", "Marketing",
    "Mobile Application Development", "Network Design", "Networking Concepts",
    "Parliamentary Procedure", "Personal Finance", "Political Science", "Public Speaking",
    "Sales Presentation", "Securities & Investments", "Social Media Strategies",
    "Sports & Entertainment Management", "Supply Chain Management", "UX Design",
    "Website Design", "Word Processing"
).sorted()

@Composable
fun StoryBuilderScreen(
    competitionName: String,
    initialCompetingIn: String,
    onCompetingInChange: (String) -> Unit,
    primaryColor: Color,
    lightMode: Boolean,
    context: android.content.Context,
    pfpUrl: String = "",
    onDismiss: () -> Unit
) {
    val storyDesigns = listOf(
        Triple("Navy Classic", "#0A1628", "#FFB800"),
        Triple("Ocean", "#061A40", "#60A5FA"),
        Triple("Forest", "#071A0E", "#4ADE80"),
        Triple("Purple", "#1A0A2E", "#C084FC"),
        Triple("Midnight", "#080808", "#F5F5F5")
    )

    var step by remember { mutableStateOf(1) }
    var competingIn by remember { mutableStateOf(initialCompetingIn) }
    var eventSearch by remember { mutableStateOf("") }
    var selectedDesign by remember { mutableStateOf(-1) }
    var sharingInProgress by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val bg = if (lightMode) Color(0xFFF0F4FF) else Color(0xFF0A1628)
    val textCol = if (lightMode) Color(0xFF1A1A2E) else Color.White
    val surfaceCol = if (lightMode) Color.White else Color(0xFF1A2540)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    if (step == 2) step = 1 else onDismiss()
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = textCol)
                }
                Text(
                    if (step == 1) "Build Your Story" else "Choose a Design",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = textCol,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            if (step == 1) {
                // ── Step 1: Enter competing-in event ──
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp)
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    // Competition chip (pre-filled, read-only)
                    if (competitionName.isNotBlank()) {
                        Text("Attending", fontSize = 12.sp, color = textCol.copy(alpha = 0.55f))
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(primaryColor.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                                .border(1.dp, primaryColor.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Text(competitionName, color = primaryColor, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    Text("What event are you competing in?", fontSize = 12.sp, color = textCol.copy(alpha = 0.55f))
                    Spacer(modifier = Modifier.height(6.dp))

                    if (competingIn.isBlank()) {
                        // ── Search & select a real FBLA event (no free typing) ──
                        OutlinedTextField(
                            value = eventSearch,
                            onValueChange = { eventSearch = it },
                            placeholder = { Text("Search FBLA events…") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = primaryColor) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryColor,
                                unfocusedBorderColor = textCol.copy(alpha = 0.25f),
                                focusedTextColor = textCol,
                                unfocusedTextColor = textCol
                            )
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        val matches = remember(eventSearch) {
                            val q = eventSearch.trim()
                            if (q.isEmpty()) fblaCompetitiveEvents
                            else fblaCompetitiveEvents.filter { it.contains(q, ignoreCase = true) }
                        }
                        if (matches.isEmpty()) {
                            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                                Text(
                                    "No FBLA events match \"${eventSearch.trim()}\"",
                                    color = textCol.copy(alpha = 0.5f),
                                    fontSize = 14.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth().weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                itemsIndexed(matches) { _, ev ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(surfaceCol)
                                            .clickable {
                                                competingIn = ev
                                                onCompetingInChange(ev)
                                                eventSearch = ""
                                            }
                                            .padding(horizontal = 16.dp, vertical = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = primaryColor, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(ev, color = textCol, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    } else {
                        // ── Selected event chip (tap X to pick a different one) ──
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(primaryColor.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                                .border(1.dp, primaryColor.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = primaryColor, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(competingIn, color = primaryColor, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, modifier = Modifier.weight(1f))
                            IconButton(onClick = { competingIn = ""; onCompetingInChange("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Change event", tint = primaryColor, modifier = Modifier.size(18.dp))
                            }
                        }

                        // Live preview of the story text
                        Spacer(modifier = Modifier.height(24.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(surfaceCol, RoundedCornerShape(14.dp))
                                .padding(16.dp)
                        ) {
                            Column {
                                Text("Preview", fontSize = 11.sp, color = textCol.copy(alpha = 0.4f))
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    buildString {
                                        append("I'm attending")
                                        if (competitionName.isNotBlank()) append(" $competitionName")
                                        append(", competing in $competingIn")
                                    },
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = textCol
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("via FBLA Hub", fontSize = 11.sp, color = textCol.copy(alpha = 0.35f))
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        Button(
                            onClick = { step = 2 },
                            modifier = Modifier.fillMaxWidth().height(52.dp).pressScale(),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                        ) {
                            Text("See Story Designs", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }

            } else {
                // ── Step 2: Design picker ──
                Text(
                    "Tap a design to preview, then share",
                    fontSize = 13.sp,
                    color = textCol.copy(alpha = 0.5f),
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))

                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    itemsIndexed(storyDesigns) { index, (name, bgHex, accentHex) ->
                        StoryDesignCard(
                            name = name,
                            bgHex = bgHex,
                            accentHex = accentHex,
                            competitionName = competitionName,
                            competingIn = competingIn,
                            selected = selectedDesign == index,
                            onClick = { selectedDesign = index }
                        )
                    }
                }

                // Share button
                Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                    if (selectedDesign < 0) {
                        Text(
                            "Select a design above to share",
                            fontSize = 13.sp,
                            color = if (lightMode) Color(0xFF1A1A2E).copy(alpha = 0.4f) else Color.White.copy(alpha = 0.4f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        )
                    }
                    Button(
                        onClick = {
                            if (!sharingInProgress) {
                                val (_, bgHex, accentHex) = storyDesigns[selectedDesign]
                                sharingInProgress = true
                                scope.launch(Dispatchers.IO) {
                                    val pfpBitmap = if (pfpUrl.isNotEmpty()) {
                                        try {
                                            val req = okhttp3.Request.Builder().url(pfpUrl).build()
                                            val bytes = OkHttpClient().newCall(req).execute().body?.bytes()
                                            if (bytes != null) BitmapFactory.decodeByteArray(bytes, 0, bytes.size) else null
                                        } catch (e: Exception) { null }
                                    } else null
                                    withContext(Dispatchers.Main) {
                                        shareCompetitionToStory(
                                            context = context,
                                            attendingEvent = competitionName.ifBlank { "FBLA Competition" },
                                            competingIn = competingIn.trim(),
                                            bgHex = bgHex,
                                            accentHex = accentHex,
                                            platform = "instagram",
                                            pfpBitmap = pfpBitmap
                                        )
                                        sharingInProgress = false
                                        onDismiss()
                                    }
                                }
                            }
                        },
                        enabled = selectedDesign >= 0 && !sharingInProgress,
                        modifier = Modifier.fillMaxWidth().height(52.dp).pressScale(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryColor,
                            disabledContainerColor = primaryColor.copy(alpha = 0.3f)
                        )
                    ) {
                        if (sharingInProgress) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color(0xFF0A1628), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Share, null, tint = Color(0xFF0A1628), modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add to Story", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0A1628))
                    }
                }
            }
        }
    }
}

@Composable
fun StoryDesignCard(
    name: String,
    bgHex: String,
    accentHex: String,
    competitionName: String,
    competingIn: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bg = Color(android.graphics.Color.parseColor(bgHex))
    val accent = Color(android.graphics.Color.parseColor(accentHex))
    val mutedWhite = Color.White.copy(alpha = 0.6f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) accent else accent.copy(alpha = 0.25f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.background(bg)) {
            // Top accent bar
            Box(modifier = Modifier.fillMaxWidth().height(6.dp).background(accent))

            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                // App label
                Text("FBLA HUB", color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp)
                Spacer(modifier = Modifier.height(14.dp))

                // Decorative ring placeholder
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .border(2.dp, accent.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("FBLA", color = accent, fontSize = 8.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("I'm attending", color = mutedWhite, fontSize = 11.sp)
                        Text(
                            competitionName.ifBlank { "Your Competition" },
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Divider(color = accent.copy(alpha = 0.4f), thickness = 1.dp)
                Spacer(modifier = Modifier.height(10.dp))

                if (competingIn.isNotBlank()) {
                    Text("Competing in", color = mutedWhite, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .background(accent.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .border(1.dp, accent.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(competingIn, color = accent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                Text("via FBLA Hub", color = Color.White.copy(alpha = 0.3f), fontSize = 10.sp)
            }

            // Bottom accent bar
            Box(modifier = Modifier.fillMaxWidth().height(6.dp).background(accent))
        }

        // Selected checkmark
        if (selected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 10.dp, end = 10.dp)
                    .size(24.dp)
                    .background(accent, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Check, contentDescription = null, tint = bg, modifier = Modifier.size(14.dp))
            }
        }

        // Design name chip bottom-right
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 12.dp, end = 16.dp)
                .background(accent.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Text(name, color = accent, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        }
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

fun shareCompetition(context: Context, name: String, date: String, location: String = "", eventName: String = "", pfpBitmap: android.graphics.Bitmap? = null) {
    val subtitle = buildString {
        append(date)
        if (location.isNotBlank()) append("  •  $location")
    }
    shareToInstagramStory(context, name, subtitle, eventName, pfpBitmap)
}

fun shareCompetitionToStory(
    context: android.content.Context,
    attendingEvent: String,
    competingIn: String,
    bgHex: String,
    accentHex: String,
    platform: String = "instagram",
    pfpBitmap: android.graphics.Bitmap? = null
) {
    try {
        val width = 1080
        val height = 1920
        val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val bgColor = android.graphics.Color.parseColor(bgHex)
        val accentColor = android.graphics.Color.parseColor(accentHex)
        val white = android.graphics.Color.WHITE
        val mutedWhite = android.graphics.Color.argb(160, 255, 255, 255)

        // Background
        canvas.drawColor(bgColor)

        // Top & bottom accent bars
        val accentPaint = Paint().apply { color = accentColor }
        canvas.drawRect(0f, 0f, width.toFloat(), 20f, accentPaint)
        canvas.drawRect(0f, height - 20f, width.toFloat(), height.toFloat(), accentPaint)

        // "FBLA HUB" top label
        val appLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            textSize = 50f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.3f
        }
        canvas.drawText("FBLA HUB", 80f, 150f, appLabelPaint)

        // Profile avatar circle in upper area
        val cx = 540f; val cy = 500f; val radius = 260f
        val ar = android.graphics.Color.red(accentColor)
        val ag = android.graphics.Color.green(accentColor)
        val ab = android.graphics.Color.blue(accentColor)
        if (pfpBitmap != null) {
            val clipPath = android.graphics.Path().apply { addCircle(cx, cy, radius, android.graphics.Path.Direction.CW) }
            canvas.save()
            canvas.clipPath(clipPath)
            val scaled = android.graphics.Bitmap.createScaledBitmap(pfpBitmap, (radius * 2).toInt(), (radius * 2).toInt(), true)
            canvas.drawBitmap(scaled, cx - radius, cy - radius, null)
            canvas.restore()
        } else {
            val ringFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.argb(20, ar, ag, ab); style = Paint.Style.FILL }
            canvas.drawCircle(cx, cy, radius, ringFillPaint)
            val fblaRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = accentColor; textSize = 80f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); textAlign = Paint.Align.CENTER; letterSpacing = 0.2f
            }
            canvas.drawText("FBLA", cx, cy - 15f, fblaRingPaint)
            val hubRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.argb(180, ar, ag, ab); textSize = 38f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL); textAlign = Paint.Align.CENTER; letterSpacing = 0.4f
            }
            canvas.drawText("FBLA HUB", cx, cy + 35f, hubRingPaint)
        }
        val ringBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.argb(80, ar, ag, ab); style = Paint.Style.STROKE; strokeWidth = 8f
        }
        canvas.drawCircle(cx, cy, radius, ringBorderPaint)

        // ── Main content block ──
        // "I'm attending" label
        val attendingLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = mutedWhite
            textSize = 52f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        canvas.drawText("I'm attending", 80f, 840f, attendingLabelPaint)

        // Event name — large bold, wrapping
        val eventPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = white
            textSize = 96f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val eventWords = attendingEvent.split(" ")
        var eventLine = ""
        var y = 960f
        for (word in eventWords) {
            val test = if (eventLine.isEmpty()) word else "$eventLine $word"
            if (eventPaint.measureText(test) > 920f) {
                canvas.drawText(eventLine, 80f, y, eventPaint)
                y += 115f
                eventLine = word
            } else eventLine = test
        }
        if (eventLine.isNotEmpty()) {
            canvas.drawText(eventLine, 80f, y, eventPaint)
            y += 115f
        }

        // Accent divider
        val divPaint = Paint().apply { color = accentColor; strokeWidth = 3f }
        canvas.drawLine(80f, y + 30f, 1000f, y + 30f, divPaint)
        y += 80f

        // "Competing in" section
        if (competingIn.isNotBlank()) {
            val compLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = mutedWhite
                textSize = 44f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }
            canvas.drawText("Competing in", 80f, y + 60f, compLabelPaint)

            val compEventPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = accentColor
                textSize = 64f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val badgeTextWidth = compEventPaint.measureText(competingIn)
            val padH = 40f; val padV = 24f
            val badgeLeft = 76f
            val badgeTop = y + 80f
            val badgeRight = badgeLeft + badgeTextWidth + padH * 2
            val badgeBottom = badgeTop + compEventPaint.textSize + padV * 2
            val badgeBgPaint = Paint().apply {
                color = android.graphics.Color.argb(30, android.graphics.Color.red(accentColor), android.graphics.Color.green(accentColor), android.graphics.Color.blue(accentColor))
            }
            val badgeBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = accentColor; style = Paint.Style.STROKE; strokeWidth = 3f
            }
            val badgeRect = android.graphics.RectF(badgeLeft, badgeTop, badgeRight, badgeBottom)
            canvas.drawRoundRect(badgeRect, 16f, 16f, badgeBgPaint)
            canvas.drawRoundRect(badgeRect, 16f, 16f, badgeBorderPaint)
            canvas.drawText(competingIn, badgeLeft + padH, badgeTop + padV + compEventPaint.textSize - 8f, compEventPaint)
        }

        // "via FBLA Hub" branding at the bottom
        val viaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.argb(140, 255, 255, 255)
            textSize = 40f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        canvas.drawText("via FBLA Hub", 80f, height - 65f, viaPaint)

        // Save and share
        val imagesDir = java.io.File(context.cacheDir, "images").also { it.mkdirs() }
        val imageFile = java.io.File(imagesDir, "competition_story.jpg")
        java.io.FileOutputStream(imageFile).use { bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 95, it) }

        val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", imageFile)

        when (platform) {
            "linkedin" -> {
                context.grantUriPermission("com.linkedin.android", uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                try {
                    val linkedInIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "image/jpeg"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        putExtra(Intent.EXTRA_TEXT, buildString {
                            append("I'm attending $attendingEvent")
                            if (competingIn.isNotBlank()) append(", competing in $competingIn")
                            append(" #FBLA #FBLAHub")
                        })
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        setPackage("com.linkedin.android")
                    }
                    context.startActivity(linkedInIntent)
                } catch (e: android.content.ActivityNotFoundException) {
                    val fallback = Intent(Intent.ACTION_SEND).apply {
                        type = "image/jpeg"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(fallback, "Share to LinkedIn…"))
                }
            }
            else -> {
                context.grantUriPermission("com.instagram.android", uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                try {
                    val instagramIntent = Intent("com.instagram.share.ADD_TO_STORY").apply {
                        setDataAndType(uri, "image/jpeg")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        setPackage("com.instagram.android")
                    }
                    context.startActivity(instagramIntent)
                } catch (e: android.content.ActivityNotFoundException) {
                    val fallback = Intent(Intent.ACTION_SEND).apply {
                        type = "image/jpeg"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(fallback, "Share story…"))
                }
            }
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Could not share: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun StoryBgCard(
    name: String,
    bgHex: String,
    accentHex: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = Color(android.graphics.Color.parseColor(bgHex))
    val accent = Color(android.graphics.Color.parseColor(accentHex))
    Box(
        modifier = modifier
            .height(120.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) accent else accent.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
    ) {
        // Accent bar top
        Box(modifier = Modifier.fillMaxWidth().height(6.dp).background(accent))
        // Name label centered
        Text(
            text = name,
            color = accent,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Center)
        )
        // Accent bar bottom
        Box(modifier = Modifier.fillMaxWidth().height(6.dp).background(accent).align(Alignment.BottomCenter))
        // Selected checkmark
        if (selected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(18.dp)
                    .background(accent, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Check, contentDescription = null, tint = bg, modifier = Modifier.size(12.dp))
            }
        }
    }
}
