
package com.example.myapplication

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "fbla_hub_settings")

object SettingsStore {
    private val BG_OPACITY = floatPreferencesKey("bg_opacity")
    private val PRIMARY_COLOR = intPreferencesKey("primary_color")
    private val SAVED_COLORS = stringPreferencesKey("saved_colors")
    private val TEXT_SIZE = floatPreferencesKey("text_size_multiplier")
    private val SIMPLIFIED_UI = booleanPreferencesKey("simplified_ui")
    private val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
    private val NOTIF_MESSAGES = booleanPreferencesKey("notif_messages")
    private val NOTIF_GROUP_MESSAGES = booleanPreferencesKey("notif_group_messages")
    private val NOTIF_CONNECTION_REQUESTS = booleanPreferencesKey("notif_connection_requests")
    private val NOTIF_CONNECTION_ACCEPTED = booleanPreferencesKey("notif_connection_accepted")
    private val NOTIF_EVENT_REMINDERS = booleanPreferencesKey("notif_event_reminders")
    private val PUBLIC_PROFILE = booleanPreferencesKey("public_profile")
    private val LIGHT_MODE = booleanPreferencesKey("light_mode")
    private val BOLD_TEXT = booleanPreferencesKey("bold_text")
    private val HIGH_CONTRAST = booleanPreferencesKey("high_contrast")
    private val REDUCE_MOTION = booleanPreferencesKey("reduce_motion")

    private const val DEFAULT_BG = 0.9f
    private val DEFAULT_PRIMARY_INT = 0xFFFFB800.toInt()
    private const val DEFAULT_TEXT_SIZE = 0.95f
    private const val DEFAULT_SIMPLIFIED = false
    private const val DEFAULT_NOTIFICATIONS = true
    private const val DEFAULT_NOTIF_MESSAGES = true
    private const val DEFAULT_NOTIF_GROUP_MESSAGES = true
    private const val DEFAULT_NOTIF_CONNECTION_REQUESTS = true
    private const val DEFAULT_NOTIF_CONNECTION_ACCEPTED = true
    private const val DEFAULT_NOTIF_EVENT_REMINDERS = true
    private const val DEFAULT_PUBLIC = true
    private const val DEFAULT_LIGHT_MODE = false
    private const val DEFAULT_BOLD_TEXT = false
    private const val DEFAULT_HIGH_CONTRAST = false
    private const val DEFAULT_REDUCE_MOTION = false
    private val DEFAULT_SAVED_COLORS = listOf(
        0xFFFFB800.toInt(),
        0xFFFFFFFF.toInt()
    )

    fun bgOpacityFlow(context: Context): Flow<Float> =
        context.dataStore.data.map { it[BG_OPACITY] ?: DEFAULT_BG }

    suspend fun saveBgOpacity(context: Context, value: Float) {
        context.dataStore.edit { it[BG_OPACITY] = value }
    }

    fun primaryColorFlow(context: Context): Flow<Color> =
        context.dataStore.data.map { Color(it[PRIMARY_COLOR] ?: DEFAULT_PRIMARY_INT) }

    suspend fun savePrimaryColor(context: Context, color: Color) {
        context.dataStore.edit { it[PRIMARY_COLOR] = color.toArgb() }
    }

    fun savedColorsFlow(context: Context): Flow<List<Int>> =
        context.dataStore.data.map { prefs ->
            val savedStr = prefs[SAVED_COLORS]
            if (savedStr.isNullOrEmpty()) {
                DEFAULT_SAVED_COLORS
            } else {
                savedStr.split(",").mapNotNull { it.toLongOrNull()?.toInt() }
            }
        }

    suspend fun saveSavedColors(context: Context, colors: List<Int>) {
        val colorsStr = colors.joinToString(",") { it.toLong().toString() }
        context.dataStore.edit { it[SAVED_COLORS] = colorsStr }
    }

    fun textSizeFlow(context: Context): Flow<Float> =
        context.dataStore.data.map { it[TEXT_SIZE] ?: DEFAULT_TEXT_SIZE }

    suspend fun saveTextSize(context: Context, value: Float) {
        context.dataStore.edit { it[TEXT_SIZE] = value }
    }

    fun simplifiedUiFlow(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[SIMPLIFIED_UI] ?: DEFAULT_SIMPLIFIED }

    suspend fun saveSimplifiedUi(context: Context, value: Boolean) {
        context.dataStore.edit { it[SIMPLIFIED_UI] = value }
    }

    fun notificationsEnabledFlow(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[NOTIFICATIONS_ENABLED] ?: DEFAULT_NOTIFICATIONS }

    suspend fun saveNotificationsEnabled(context: Context, value: Boolean) {
        context.dataStore.edit { it[NOTIFICATIONS_ENABLED] = value }
    }

    fun notifMessagesFlow(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[NOTIF_MESSAGES] ?: DEFAULT_NOTIF_MESSAGES }
    suspend fun saveNotifMessages(context: Context, value: Boolean) {
        context.dataStore.edit { it[NOTIF_MESSAGES] = value }
    }

    fun notifGroupMessagesFlow(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[NOTIF_GROUP_MESSAGES] ?: DEFAULT_NOTIF_GROUP_MESSAGES }
    suspend fun saveNotifGroupMessages(context: Context, value: Boolean) {
        context.dataStore.edit { it[NOTIF_GROUP_MESSAGES] = value }
    }

    fun notifConnectionRequestsFlow(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[NOTIF_CONNECTION_REQUESTS] ?: DEFAULT_NOTIF_CONNECTION_REQUESTS }
    suspend fun saveNotifConnectionRequests(context: Context, value: Boolean) {
        context.dataStore.edit { it[NOTIF_CONNECTION_REQUESTS] = value }
    }

    fun notifConnectionAcceptedFlow(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[NOTIF_CONNECTION_ACCEPTED] ?: DEFAULT_NOTIF_CONNECTION_ACCEPTED }
    suspend fun saveNotifConnectionAccepted(context: Context, value: Boolean) {
        context.dataStore.edit { it[NOTIF_CONNECTION_ACCEPTED] = value }
    }

    fun notifEventRemindersFlow(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[NOTIF_EVENT_REMINDERS] ?: DEFAULT_NOTIF_EVENT_REMINDERS }
    suspend fun saveNotifEventReminders(context: Context, value: Boolean) {
        context.dataStore.edit { it[NOTIF_EVENT_REMINDERS] = value }
    }

    fun publicProfileFlow(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[PUBLIC_PROFILE] ?: DEFAULT_PUBLIC }

    suspend fun savePublicProfile(context: Context, value: Boolean) {
        context.dataStore.edit { it[PUBLIC_PROFILE] = value }
    }

    fun lightModeFlow(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[LIGHT_MODE] ?: DEFAULT_LIGHT_MODE }

    suspend fun saveLightMode(context: Context, value: Boolean) {
        context.dataStore.edit { it[LIGHT_MODE] = value }
    }

    fun boldTextFlow(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[BOLD_TEXT] ?: DEFAULT_BOLD_TEXT }

    suspend fun saveBoldText(context: Context, value: Boolean) {
        context.dataStore.edit { it[BOLD_TEXT] = value }
    }

    fun highContrastFlow(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[HIGH_CONTRAST] ?: DEFAULT_HIGH_CONTRAST }

    suspend fun saveHighContrast(context: Context, value: Boolean) {
        context.dataStore.edit { it[HIGH_CONTRAST] = value }
    }

    fun reduceMotionFlow(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[REDUCE_MOTION] ?: DEFAULT_REDUCE_MOTION }

    suspend fun saveReduceMotion(context: Context, value: Boolean) {
        context.dataStore.edit { it[REDUCE_MOTION] = value }
    }
}


