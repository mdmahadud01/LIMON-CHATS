package com.example.ui.navigation

import kotlinx.serialization.Serializable

sealed interface Route {
    @Serializable data object Splash : Route
    @Serializable data object Onboarding : Route
    @Serializable data object Auth : Route
    @Serializable data object Home : Route
    @Serializable data object LockedChats : Route
    @Serializable data object ChatLockSettings : Route
    @Serializable data object ChatLockOverview : Route
    @Serializable data class Chat(val chatId: String, val peerUid: String) : Route
    @Serializable data class UserProfile(val uid: String) : Route
    @Serializable data object EditProfile : Route
    @Serializable data class ImageViewer(
        val url: String,
        val title: String = "Photo",
        val chatId: String? = null,
        val messageId: String? = null,
        val secureMode: Boolean = false
    ) : Route
    @Serializable data class VideoPlayer(
        val url: String,
        val title: String = "Video",
        val chatId: String? = null,
        val messageId: String? = null,
        val secureMode: Boolean = false
    ) : Route
    @Serializable data class CameraCapture(val chatId: String, val peerUid: String) : Route
    @Serializable data class ActiveCall(val callId: String) : Route
    @Serializable data object BlockedUsers : Route
    @Serializable data object PrivacySettings : Route
    @Serializable data object AppearanceSettings : Route
    @Serializable data object StorageSettings : Route
    @Serializable data object SecuritySettings : Route
    @Serializable data object AccountSettings : Route
    @Serializable data object About : Route
}
