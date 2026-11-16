package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.AccentColorOption
import com.example.model.AppSettings
import com.example.model.AppearanceMode
import com.example.model.BubbleStyleOption
import com.example.model.ChatWallpaperPreset
import com.example.model.GlassStyleOption
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import java.io.File

private val Context.limonDataStore: DataStore<Preferences> by preferencesDataStore(name = "limon_settings")

class SettingsDataStore(private val context: Context) {

    private val dataStore = context.applicationContext.limonDataStore

    val settingsFlow: Flow<AppSettings> = dataStore.data.map { prefs ->
        AppSettings(
            onboardingCompleted = prefs[KEY_ONBOARDING_DONE] ?: false,
            appearanceMode = prefs[KEY_APPEARANCE_MODE]?.let { runCatching { AppearanceMode.valueOf(it) }.getOrNull() }
                ?: AppearanceMode.LIQUID_GLASS,
            glassStyle = prefs[KEY_GLASS_STYLE]?.let { runCatching { GlassStyleOption.valueOf(it) }.getOrNull() }
                ?: GlassStyleOption.IOS_27_DEFAULT,
            accentColor = prefs[KEY_ACCENT_COLOR]?.let { runCatching { AccentColorOption.valueOf(it) }.getOrNull() }
                ?: AccentColorOption.CITRUS_EMERALD,
            bubbleStyle = prefs[KEY_BUBBLE_STYLE]?.let { runCatching { BubbleStyleOption.valueOf(it) }.getOrNull() }
                ?: BubbleStyleOption.LIQUID_PILL,
            chatWallpaper = ChatWallpaperPreset.from(prefs[KEY_CHAT_WALLPAPER]),
            customWallpaperUri = prefs[KEY_CUSTOM_WALLPAPER_URI],
            wallpaperBlur = prefs[KEY_WALLPAPER_BLUR] ?: false,
            fontScale = prefs[KEY_FONT_SCALE] ?: 1.0f,
            animationsEnabled = prefs[KEY_ANIMATIONS_ENABLED] ?: true,
            messageNotifications = prefs[KEY_MSG_NOTIF] ?: true,
            callNotifications = prefs[KEY_CALL_NOTIF] ?: true,
            notificationSound = prefs[KEY_NOTIF_SOUND] ?: true,
            notificationVibration = prefs[KEY_NOTIF_VIB] ?: true,
            notificationPreview = prefs[KEY_NOTIF_PREVIEW] ?: true,
            autoDownloadMedia = prefs[KEY_AUTO_DOWNLOAD] ?: true,
            highQualityMedia = prefs[KEY_HQ_MEDIA] ?: false,
            enterToSend = prefs[KEY_ENTER_TO_SEND] ?: false,
            twoStepEnabled = prefs[KEY_TWO_STEP] ?: false,
            chatLockSecretConfigured = prefs[KEY_CHAT_LOCK_SECRET_SET] ?: false,
            loginAlertsEnabled = prefs[KEY_LOGIN_ALERTS] ?: true,
            recentSearches = decodeList(prefs[KEY_RECENT_SEARCHES])
        )
    }

    suspend fun currentSettings(): AppSettings = runCatching { settingsFlow.first() }.getOrDefault(AppSettings())

    suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { it[KEY_ONBOARDING_DONE] = completed }
    }

    suspend fun setAppearanceMode(mode: AppearanceMode) {
        dataStore.edit { it[KEY_APPEARANCE_MODE] = mode.name }
    }

    suspend fun setGlassStyle(style: GlassStyleOption) {
        dataStore.edit { it[KEY_GLASS_STYLE] = style.name }
    }

    suspend fun setAccentColor(accent: AccentColorOption) {
        dataStore.edit { it[KEY_ACCENT_COLOR] = accent.name }
    }

    suspend fun setBubbleStyle(style: BubbleStyleOption) {
        dataStore.edit { it[KEY_BUBBLE_STYLE] = style.name }
    }

    suspend fun setChatWallpaper(preset: ChatWallpaperPreset) {
        dataStore.edit { it[KEY_CHAT_WALLPAPER] = preset.name }
    }

    suspend fun setCustomWallpaperUri(uri: String?) {
        dataStore.edit {
            if (uri.isNullOrBlank()) {
                it.remove(KEY_CUSTOM_WALLPAPER_URI)
            } else {
                it[KEY_CUSTOM_WALLPAPER_URI] = uri
                it[KEY_CHAT_WALLPAPER] = ChatWallpaperPreset.CUSTOM.name
            }
        }
    }

    suspend fun setWallpaperBlur(enabled: Boolean) {
        dataStore.edit { it[KEY_WALLPAPER_BLUR] = enabled }
    }

    suspend fun setFontScale(scale: Float) {
        dataStore.edit { it[KEY_FONT_SCALE] = scale.coerceIn(0.85f, 1.35f) }
    }

    suspend fun setAnimationsEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_ANIMATIONS_ENABLED] = enabled }
    }

    suspend fun setMessageNotifications(enabled: Boolean) {
        dataStore.edit { it[KEY_MSG_NOTIF] = enabled }
    }

    suspend fun setCallNotifications(enabled: Boolean) {
        dataStore.edit { it[KEY_CALL_NOTIF] = enabled }
    }

    suspend fun setNotificationSound(enabled: Boolean) {
        dataStore.edit { it[KEY_NOTIF_SOUND] = enabled }
    }

    suspend fun setNotificationVibration(enabled: Boolean) {
        dataStore.edit { it[KEY_NOTIF_VIB] = enabled }
    }

    suspend fun setNotificationPreview(enabled: Boolean) {
        dataStore.edit { it[KEY_NOTIF_PREVIEW] = enabled }
    }

    suspend fun setAutoDownloadMedia(enabled: Boolean) {
        dataStore.edit { it[KEY_AUTO_DOWNLOAD] = enabled }
    }

    suspend fun setHighQualityMedia(enabled: Boolean) {
        dataStore.edit { it[KEY_HQ_MEDIA] = enabled }
    }

    suspend fun setEnterToSend(enabled: Boolean) {
        dataStore.edit { it[KEY_ENTER_TO_SEND] = enabled }
    }

    suspend fun setTwoStepEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_TWO_STEP] = enabled }
    }

    suspend fun setChatLockSecretConfigured(configured: Boolean) {
        dataStore.edit { it[KEY_CHAT_LOCK_SECRET_SET] = configured }
    }

    suspend fun setLoginAlertsEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_LOGIN_ALERTS] = enabled }
    }

    suspend fun addRecentSearch(query: String) {
        val cleaned = query.trim()
        if (cleaned.isEmpty()) return
        dataStore.edit { prefs ->
            val current = decodeList(prefs[KEY_RECENT_SEARCHES]).toMutableList()
            current.removeAll { it.equals(cleaned, ignoreCase = true) }
            current.add(0, cleaned)
            prefs[KEY_RECENT_SEARCHES] = encodeList(current.take(10))
        }
    }

    suspend fun clearRecentSearches() {
        dataStore.edit { it[KEY_RECENT_SEARCHES] = "[]" }
    }

    fun calculateCacheSizeBytes(): Long {
        fun dirSize(file: File?): Long {
            if (file == null || !file.exists()) return 0L
            if (file.isFile) return file.length()
            return file.listFiles()?.sumOf { dirSize(it) } ?: 0L
        }
        return dirSize(context.cacheDir)
    }

    fun clearLocalCache(): Long {
        fun deleteChildren(dir: File?) {
            dir?.listFiles()?.forEach { child ->
                if (child.isDirectory) deleteChildren(child)
                child.delete()
            }
        }
        deleteChildren(context.cacheDir)
        return calculateCacheSizeBytes()
    }

    private fun decodeList(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            List(arr.length()) { idx -> arr.optString(idx) }.filter { it.isNotBlank() }
        }.getOrDefault(emptyList())
    }

    private fun encodeList(items: List<String>): String {
        val arr = JSONArray()
        items.forEach { arr.put(it) }
        return arr.toString()
    }

    private companion object {
        val KEY_ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val KEY_APPEARANCE_MODE = stringPreferencesKey("appearance_mode")
        val KEY_ACCENT_COLOR = stringPreferencesKey("accent_color")
        val KEY_BUBBLE_STYLE = stringPreferencesKey("bubble_style")
        val KEY_FONT_SCALE = floatPreferencesKey("font_scale")
        val KEY_ANIMATIONS_ENABLED = booleanPreferencesKey("animations_enabled")
        val KEY_MSG_NOTIF = booleanPreferencesKey("msg_notif")
        val KEY_CALL_NOTIF = booleanPreferencesKey("call_notif")
        val KEY_NOTIF_SOUND = booleanPreferencesKey("notif_sound")
        val KEY_NOTIF_VIB = booleanPreferencesKey("notif_vib")
        val KEY_NOTIF_PREVIEW = booleanPreferencesKey("notif_preview")
        val KEY_AUTO_DOWNLOAD = booleanPreferencesKey("auto_download")
        val KEY_HQ_MEDIA = booleanPreferencesKey("hq_media")
        val KEY_ENTER_TO_SEND = booleanPreferencesKey("enter_to_send")
        val KEY_TWO_STEP = booleanPreferencesKey("two_step")
        val KEY_CHAT_LOCK_SECRET_SET = booleanPreferencesKey("chat_lock_secret_set")
        val KEY_LOGIN_ALERTS = booleanPreferencesKey("login_alerts")
        val KEY_RECENT_SEARCHES = stringPreferencesKey("recent_searches")
        val KEY_CHAT_WALLPAPER = stringPreferencesKey("chat_wallpaper")
        val KEY_CUSTOM_WALLPAPER_URI = stringPreferencesKey("custom_wallpaper_uri")
        val KEY_WALLPAPER_BLUR = booleanPreferencesKey("wallpaper_blur")
        val KEY_GLASS_STYLE = stringPreferencesKey("glass_style")
    }
}
