# Retrofit / Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.ran.crm.data.remote.model.** { *; }
-keep class com.ran.crm.data.local.entity.** { *; }

# Gson
-keepattributes SerializedName
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Room
-keep class * extends androidx.room.RoomDatabase

# Keep @Keep annotated classes
-keep @androidx.annotation.Keep class * { *; }

# Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# libphonenumber
-keep class com.google.i18n.phonenumbers.** { *; }
