# Sairen — R8 / ProGuard rules for release builds.
#
# Media3, AndroidX, Coil and OkHttp all ship consumer ProGuard rules, so only
# project-specific keep rules belong here.

# Media3 creates media items/session objects that are passed between processes
# (MediaSession -> MediaController). Keep the metadata model reachable.
-keep class androidx.media3.session.** { *; }
-dontwarn androidx.media3.**

# Guava (pulled in by Media3) references optional annotations we do not ship.
-dontwarn com.google.common.**
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**

# Kotlin metadata used by reflection-free serialization of enums in DataStore.
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep line numbers so Media3 playback exceptions stay debuggable in crash reports.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
