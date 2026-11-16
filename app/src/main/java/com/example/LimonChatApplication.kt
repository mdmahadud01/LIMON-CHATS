package com.example

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.example.data.AuthRepository
import com.example.data.CallRepository
import com.example.data.ChatRepository
import com.example.data.LimonRtdbProvider
import com.example.data.SettingsDataStore
import com.example.data.UserRepository
import com.example.media.LimonCoilFetcherFactory
import com.example.notifications.LimonBackgroundSyncWorker
import com.example.notifications.LimonRealtimeSyncService
import com.example.notifications.NotificationHelper
import com.example.security.KeystoreCryptoManager
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.messaging.messaging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class NetworkMonitor(context: Context) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _isOnline = MutableStateFlow(checkCurrentlyOnline())
    val isOnline = _isOnline.asStateFlow()

    private val appCtx = context.applicationContext

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            val wasOffline = !_isOnline.value
            _isOnline.value = true
            if (wasOffline) {
                LimonBackgroundSyncWorker.enqueueImmediateSync(appCtx)
            }
        }

        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            val validated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            _isOnline.value = validated || capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }

        override fun onLost(network: Network) {
            _isOnline.value = checkCurrentlyOnline()
        }
    }

    init {
        runCatching {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager.registerNetworkCallback(request, callback)
        }
    }

    private fun checkCurrentlyOnline(): Boolean = runCatching {
        val network = connectivityManager.activeNetwork ?: return@runCatching false
        val caps = connectivityManager.getNetworkCapabilities(network) ?: return@runCatching false
        caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }.getOrDefault(true)
}

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    val databaseId: String = appContext.getString(R.string.firestore_database_id)
    val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(databaseId).apply {
        runCatching {
            firestoreSettings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(
                    PersistentCacheSettings.newBuilder()
                        .setSizeBytes(100L * 1024L * 1024L)
                        .build()
                )
                .build()
        }
    }
    val networkMonitor = NetworkMonitor(appContext)
    val settingsDataStore = SettingsDataStore(appContext)
    val keystoreCryptoManager = KeystoreCryptoManager(appContext)
    val authRepository = AuthRepository()
    val userRepository = UserRepository(firestore)
    val chatRepository = ChatRepository(firestore, appContext)
    val callRepository = CallRepository(firestore)
}

class LimonChatApplication : Application(), ImageLoaderFactory {
    val container: AppContainer by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        runCatching { Firebase.messaging.isAutoInitEnabled = true }
        runCatching { LimonRtdbProvider.getDatabase(this) }
        NotificationHelper.ensureChannels(this)
        NotificationHelper.cancelNotification(this, NotificationHelper.REALTIME_SYNC_NOTIF_ID)
        LimonRealtimeSyncService.schedule(this)
        LimonBackgroundSyncWorker.schedulePeriodic(this)
        LimonBackgroundSyncWorker.enqueueImmediateSync(this)
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                add(LimonCoilFetcherFactory(applicationContext, container.firestore))
            }
            .crossfade(true)
            .build()
    }
}
