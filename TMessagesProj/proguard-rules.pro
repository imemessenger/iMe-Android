-keep public class com.google.android.gms.* { public *; }
-keepnames @com.google.android.gms.common.annotation.KeepName class *
-keepclassmembernames class * {
    @com.google.android.gms.common.annotation.KeepName *;
}
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public class * extends com.bumptech.glide.module.AppGlideModule
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}
-keep class org.telegram.** { *; }
-keep class com.google.android.exoplayer2.ext.** { *; }
-keep class com.google.android.exoplayer2.util.** { *; }
-dontwarn com.coremedia.**
-dontwarn org.telegram.**
-dontwarn com.google.android.exoplayer2.ext.**
-dontwarn com.google.android.exoplayer2.util.**
-dontwarn com.google.android.gms.**
-dontwarn com.google.common.cache.**
-dontwarn com.google.common.primitives.**
-dontwarn com.googlecode.mp4parser.**
-dontwarn com.opencsv.bean.**
-dontwarn org.apache.**
-keep,includedescriptorclasses class com.smedialink.responses.** { *; }
-keep,includedescriptorclasses class com.smedialink.** { *; }
-keep,includedescriptorclasses class com.google.android.gms.** { *; }
-keep,includedescriptorclasses class com.stripe.android.** { *; }
-keep,includedescriptorclasses class com.google.android.exoplayer2.** { *; }
-dontwarn
-dontwarn retrofit2.Platform$Java8
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-dontnote retrofit2.Platform
-dontwarn retrofit2.Platform$Java8
-dontwarn org.conscrypt.**
-dontwarn org.codehaus.mojo.animal_sniffer.**
-keepattributes Exceptions
# Use -keep to explicitly keep any other classes shrinking would remove
-dontoptimize
-dontobfuscate