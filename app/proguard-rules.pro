# Add project specific ProGuard rules here.

# Keep Room Database entities, DAOs, and generated implementations
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keep class az.pdr.taqdiya2.data.local.** { *; }

# Keep Home Screen AppWidget BroadcastReceiver
-keep class az.pdr.taqdiya2.widget.** { *; }

# Kotlin & Coroutines safe rules
-dontwarn kotlinx.coroutines.**
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
