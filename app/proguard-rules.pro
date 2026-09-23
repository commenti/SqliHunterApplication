# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
#
# For more details, see
#   https://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# Preserve Hilt annotations
-keep class ** implements com.google.dagger.hilt.android.internal.managers.ManagerHolder
-keep class ** implements com.google.dagger.hilt.android.internal.builders.ModuleAdapter

# Preserve Room database classes
-keep class androidx.room.** { *; }
-keep class * extends androidx.room.Database { *; }
-keep class * extends androidx.room.Entity { *; }
-keep class * extends androidx.room.Dao { *; }

# Preserve Retrofit service interfaces
-keep class com.yourapp.sqliautohunter.data.remote.** { *; }
-keepattributes Signature
-keepattributes Exceptions

# Preserve OkHttp
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# Preserve Coroutines
-keep class kotlinx.coroutines.** { *; }

# Preserve WebView
-keep class androidx.webkit.** { *; }

# Preserve Compose
-keep class androidx.compose.** { *; }

# Preserve Jsoup
-keep class org.jsoup.** { *; }

# Preserve all serializable classes
-keepclassmembers class * implements java.io.Serializable {
    static <fields>;
    private <fields>;
    private <methods>;
}

# Preserve R (resources) classes
-keep class **.R$* { *; }

# Preserve all native method names and the names of their classes
-keepclassmembers class * {
    public private *;
    static *;
}
