# ── General ───────────────────────────────────────
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
-keepattributes *Annotation*

# ── Kotlin Serialization ─────────────────────────
-keepattributes RuntimeVisibleAnnotations
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class * {
    @kotlinx.serialization.Serializable *;
}
-keep @kotlinx.serialization.Serializable class * { *; }
-keepclassmembers @kotlinx.serialization.Serializable class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

# ── DTO / Entity models ─────────────────────────
-keep class com.example.mobile_tracker.data.remote.dto.** { *; }
-keep class com.example.mobile_tracker.data.local.db.entity.** { *; }

# ── Ktor ─────────────────────────────────────────
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# ── OkHttp ───────────────────────────────────────
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# ── Room ─────────────────────────────────────────
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# ── Koin ─────────────────────────────────────────
-keep class org.koin.** { *; }
-dontwarn org.koin.**

# ── Timber ───────────────────────────────────────
-dontwarn timber.log.**

# ── Coroutines ───────────────────────────────────
-dontwarn kotlinx.coroutines.**

# ── EncryptedSharedPreferences ───────────────────
-keep class androidx.security.crypto.** { *; }