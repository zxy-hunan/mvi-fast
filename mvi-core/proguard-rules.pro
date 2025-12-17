# aFramework ProGuard Rules

# ==================== MVI Core ====================
-keep class com.mvi.core.** { *; }

# Keep all MviIntent implementations
-keep interface com.mvi.core.base.MviIntent
-keep class * implements com.mvi.core.base.MviIntent { *; }

# Keep ViewModel classes
-keepclassmembers class * extends com.mvi.core.base.MviViewModel {
    public <methods>;
}

# Keep UiState and UiEvent
-keep class com.mvi.core.base.UiState { *; }
-keep class com.mvi.core.base.UiState$* { *; }
-keep class com.mvi.core.base.UiEvent { *; }
-keep class com.mvi.core.base.UiEvent$* { *; }

# ==================== Network ====================
-keep class com.mvi.core.network.ApiResponse { *; }
-keep class com.mvi.core.network.ApiException { *; }

# Retrofit
-keepattributes Signature
-keepattributes Exceptions
-keepattributes *Annotation*

-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Keep API response models
-keep class * extends com.mvi.core.network.ApiResponse { *; }

# ==================== Coroutines ====================
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# ==================== ViewBinding ====================
-keep class * implements androidx.viewbinding.ViewBinding {
    public static *** inflate(android.view.LayoutInflater);
    public static *** bind(android.view.View);
}

# ==================== MMKV ====================
-keep class com.tencent.mmkv.** { *; }
-keep class com.mvi.core.storage.MmkvStorage { *; }

# ==================== Glide ====================
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule {
    <init>(...);
}
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
    **[] $VALUES;
    public *;
}
-keep class com.bumptech.glide.load.data.ParcelFileDescriptorRewinder$InternalRewinder {
    *** rewind();
}

# ==================== General ====================
# Keep ViewModels
-keep class * extends androidx.lifecycle.ViewModel {
    <init>();
}

# Keep Parcelable
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Keep Serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ==================== Kotlin ====================
-keep class kotlin.Metadata { *; }
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}

# Keep sealed classes
-keep class * extends kotlin.coroutines.Continuation

# ==================== Debug ====================
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
