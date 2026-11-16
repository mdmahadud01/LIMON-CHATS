package com.example.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class PrivacyScope(val value: String, val label: String) {
    EVERYONE("everyone", "Everyone"),
    CONTACTS("contacts", "My Contacts"),
    NOBODY("nobody", "Nobody");

    companion object {
        fun from(value: String?): PrivacyScope =
            entries.firstOrNull { it.value == value } ?: EVERYONE
    }
}

enum class MessageType(val value: String) {
    TEXT("text"),
    IMAGE("image"),
    VIDEO("video"),
    VOICE("voice"),
    DOCUMENT("document"),
    LOCATION("location"),
    CONTACT("contact");

    companion object {
        fun from(value: String?): MessageType =
            entries.firstOrNull { it.value == value } ?: TEXT
    }
}

enum class DeliveryStatus(val value: String) {
    SENT("sent"),
    DELIVERED("delivered"),
    READ("read");

    companion object {
        fun from(value: String?): DeliveryStatus =
            entries.firstOrNull { it.value == value } ?: SENT
    }
}

enum class CallType(val value: String) {
    VOICE("voice"),
    VIDEO("video");

    companion object {
        fun from(value: String?): CallType =
            entries.firstOrNull { it.value == value } ?: VOICE
    }
}

enum class CallStatus(val value: String) {
    RINGING("ringing"),
    ACCEPTED("accepted"),
    DECLINED("declined"),
    ENDED("ended"),
    MISSED("missed");

    companion object {
        fun from(value: String?): CallStatus =
            entries.firstOrNull { it.value == value } ?: ENDED
    }
}

enum class AppearanceMode(val label: String) {
    LIQUID_GLASS("Liquid Glass"),
    DARK("Dark Mode"),
    LIGHT("Light Mode"),
    SYSTEM("System Theme")
}

enum class GlassStyleOption(val label: String, val subtitle: String) {
    IOS_27_DEFAULT("iOS 27 Glass", "Signature layered frosted glass with crisp depth"),
    WATER_GLASS("Water Glass", "Ultra-clear transparent refractive glass")
}

enum class AccentColorOption(val label: String) {
    CITRUS_EMERALD("Citrus Emerald"),
    OCEAN_CYAN("Ocean Cyan"),
    INDIGO_FROST("Indigo Frost"),
    CORAL_SUNSET("Coral Sunset")
}

enum class BubbleStyleOption(val label: String) {
    LIQUID_PILL("Liquid Pill"),
    CLASSIC_IOS("Classic iOS"),
    COMPACT_GLASS("Compact Glass")
}

enum class ChatWallpaperPreset(val id: String, val label: String) {
    PASTEL_MINT("pastel_mint", "Pastel Mint"),
    LAVENDER_SKY("lavender_sky", "Lavender Sky"),
    WARM_PEACH("warm_peach", "Warm Peach"),
    MIDNIGHT_AURORA("midnight_aurora", "Midnight Aurora"),
    CUSTOM("custom", "Custom Photo");

    companion object {
        fun from(value: String?): ChatWallpaperPreset =
            entries.firstOrNull { it.name == value || it.id == value } ?: PASTEL_MINT
    }
}

@IgnoreExtraProperties
data class PublicProfile(
    @DocumentId val id: String = "",
    val uid: String = "",
    val username: String = "",
    val usernameLowercase: String = "",
    val displayName: String = "",
    val photoUrl: String? = null,
    val bio: String = "",
    val online: Boolean = false,
    @ServerTimestamp val lastSeen: Timestamp? = null,
    val activeDevice: String = "",
    val privacyShowLastSeen: String = PrivacyScope.EVERYONE.value,
    val privacyShowOnline: String = PrivacyScope.EVERYONE.value,
    val privacyShowPhoto: String = PrivacyScope.EVERYONE.value,
    val privacyReadReceipts: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class PrivateUserInfo(
    @DocumentId val id: String = "",
    val userId: String = "",
    val email: String = "",
    val emailVerified: Boolean = false,
    val phoneNumber: String = "",
    val fcmToken: String = "",
    val fcmTokens: Map<String, String> = emptyMap(),
    val activeSessions: List<String> = emptyList(),
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class BlockedUser(
    @DocumentId val id: String = "",
    val blockedUid: String = "",
    val blockedDisplayName: String = "",
    val blockedUsername: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ChatThread(
    @DocumentId val id: String = "",
    val memberUids: List<String> = emptyList(),
    val lastMessageText: String = "",
    val lastMessageType: String = MessageType.TEXT.value,
    val lastMessageSenderId: String = "",
    @ServerTimestamp val lastMessageAt: Timestamp? = null,
    val pinnedMessageId: String? = null,
    val pinnedMessageText: String? = null,
    val unreadBy: Map<String, Long> = emptyMap(),
    val sharedWallpaperPreset: String? = null,
    val sharedWallpaperUrl: String? = null,
    val sharedWallpaperBlur: Boolean? = null,
    val sharedWallpaperBy: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val memberUids: List<String> = emptyList(),
    val type: String = MessageType.TEXT.value,
    val text: String = "",
    val mediaUrl: String? = null,
    val mediaName: String? = null,
    val mediaSizeBytes: Long? = null,
    val mediaDurationMs: Long? = null,
    val waveform: List<Double>? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationName: String? = null,
    val contactName: String? = null,
    val contactUsername: String? = null,
    val contactUid: String? = null,
    val replyToId: String? = null,
    val replyToText: String? = null,
    val replyToSenderName: String? = null,
    val forwardedFrom: String? = null,
    val deliveryStatus: String = DeliveryStatus.SENT.value,
    val reactions: Map<String, String> = emptyMap(),
    val isViewOnce: Boolean = false,
    val viewOnceViewed: Boolean = false,
    val deletedForEveryone: Boolean = false,
    val deletedForUsers: List<String> = emptyList(),
    @ServerTimestamp val editedAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ChatPreference(
    @DocumentId val id: String = "",
    val chatId: String = "",
    val muted: Boolean = false,
    val archived: Boolean = false,
    val pinned: Boolean = false,
    val locked: Boolean = false,
    val lastReadAt: Timestamp? = null,
    val clearedAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class TypingStatus(
    @DocumentId val id: String = "",
    val uid: String = "",
    val memberUids: List<String> = emptyList(),
    val isTyping: Boolean = false,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CallSession(
    @DocumentId val id: String = "",
    val callerId: String = "",
    val receiverId: String = "",
    val memberUids: List<String> = emptyList(),
    val callerName: String = "",
    val callerPhotoUrl: String? = null,
    val receiverName: String = "",
    val receiverPhotoUrl: String? = null,
    val callType: String = CallType.VOICE.value,
    val status: String = CallStatus.RINGING.value,
    val offerSdp: String? = null,
    val answerSdp: String? = null,
    val durationSeconds: Long = 0L,
    val deletedForUsers: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    val endedAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class IceCandidateRecord(
    @DocumentId val id: String = "",
    val senderId: String = "",
    val memberUids: List<String> = emptyList(),
    val sdpMid: String = "",
    val sdpMLineIndex: Int = 0,
    val candidate: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class UserNotification(
    @DocumentId val id: String = "",
    val type: String = "activity",
    val senderId: String = "",
    val senderName: String = "",
    val title: String = "",
    val body: String = "",
    val chatId: String? = null,
    val read: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)

data class AppSettings(
    val onboardingCompleted: Boolean = false,
    val appearanceMode: AppearanceMode = AppearanceMode.LIQUID_GLASS,
    val glassStyle: GlassStyleOption = GlassStyleOption.IOS_27_DEFAULT,
    val accentColor: AccentColorOption = AccentColorOption.CITRUS_EMERALD,
    val bubbleStyle: BubbleStyleOption = BubbleStyleOption.LIQUID_PILL,
    val chatWallpaper: ChatWallpaperPreset = ChatWallpaperPreset.PASTEL_MINT,
    val customWallpaperUri: String? = null,
    val wallpaperBlur: Boolean = false,
    val fontScale: Float = 1.0f,
    val animationsEnabled: Boolean = true,
    val messageNotifications: Boolean = true,
    val callNotifications: Boolean = true,
    val notificationSound: Boolean = true,
    val notificationVibration: Boolean = true,
    val notificationPreview: Boolean = true,
    val autoDownloadMedia: Boolean = true,
    val highQualityMedia: Boolean = false,
    val enterToSend: Boolean = false,
    val twoStepEnabled: Boolean = false,
    val chatLockSecretConfigured: Boolean = false,
    val loginAlertsEnabled: Boolean = true,
    val recentSearches: List<String> = emptyList()
)

data class ChatListEntry(
    val chat: ChatThread,
    val peerProfile: PublicProfile?,
    val peerUid: String,
    val preference: ChatPreference,
    val unreadCount: Int,
    val isPeerTyping: Boolean = false
)

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}
