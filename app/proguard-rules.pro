# ProGuard rules for JARVIS
-keep class com.jarvis.assistant.data.model.** { *; }
-keepclassmembers class * {
    @androidx.room.* *;
}
