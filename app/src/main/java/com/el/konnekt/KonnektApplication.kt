package com.el.konnekt

import android.app.Application
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.el.konnekt.utils.ForegroundNotificationHandler

class KonnektApplication : Application(), LifecycleObserver {

    companion object {
        var isAppInForeground = false
            private set

        var currentChatId: String? = null
            private set

        fun setCurrentChat(chatId: String?) {
            currentChatId = chatId
        }

        fun shouldShowNotification(chatId: String): Boolean {
            return isAppInForeground && currentChatId != chatId
        }
    }

    override fun onCreate() {
        super.onCreate()

        val imageLoader = ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.15) // Use only 15% of available memory
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(50 * 1024 * 1024) // 50MB max
                    .build()
            }
            .respectCacheHeaders(false)
            .build()

        // Set as default
        coil.Coil.setImageLoader(imageLoader)

        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        ForegroundNotificationHandler.createNotificationChannels(this)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onAppForegrounded() {
        isAppInForeground = true
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onAppBackgrounded() {
        isAppInForeground = false
        currentChatId = null
    }
}