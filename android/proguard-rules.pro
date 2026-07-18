# Keep ProGuard rules for encrypted database
-keep class net.sqlcipher.** { *; }
-dontwarn net.sqlcipher.**

# Kotlin
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class kotlinx.serialization.** { *; }
