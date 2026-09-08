-keep class org.osmdroid.** { *; }
-keep class com.google.android.gms.** { *; }
-keep class com.google.gson.** { *; }
-keep class androidx.room.** { *; }

# Keep location data classes
-keep class com.nekogps.app.data.** { *; }

# OSMDroid
-dontwarn org.osmdroid.**
-keepclassmembers class org.osmdroid.** { *; }
