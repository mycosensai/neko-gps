# ═══════════════════════════════════════════════════════════════════════════
#  Neko GPS — R8 / ProGuard rules
# ═══════════════════════════════════════════════════════════════════════════
#
#  Most libraries (Room, AndroidX, Play Services, Gson) ship their own consumer
#  rules, which R8 applies automatically. Only app-specific reflection needs
#  rules here.
#
#  ── Release-only failure modes these rules guard against ────────────────────
#  * Missing `Signature` attribute breaks Gson `TypeToken<List<Foo>>`, silently
#    deserialising into List<LinkedTreeMap> instead of the expected type.
#  * R8 renaming/removing Gson model fields breaks JSON binding at runtime even
#    though the debug build works perfectly.
# ═══════════════════════════════════════════════════════════════════════════

# ── Attributes Gson and reflection depend on ─────────────────────────────────
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault

# ── Gson model classes (bound by field NAME via reflection) ──────────────────
# Speed cameras, fuel prices, cached route points and emergency data are all
# (de)serialised with Gson + TypeToken, so their field names must survive.
-keep class com.nekogps.app.features.speedcamera.SpeedCamera { *; }
-keep class com.nekogps.app.features.stats.FuelStation { *; }
-keep class com.nekogps.app.features.stats.RoutePoint { *; }
-keep class com.nekogps.app.features.safety.EmergencySOSManager$EmergencyContact { *; }
-keep class com.nekogps.app.features.safety.OfflineEmergencyManager$ICEContact { *; }
-keep class com.nekogps.app.features.safety.OfflineEmergencyManager$MedicalInfo { *; }
-keep class com.nekogps.app.features.safety.OfflineEmergencyManager$Hospital { *; }

# Gson internals — referenced reflectively by the TypeToken machinery.
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-dontwarn com.google.gson.**

# ── Room ─────────────────────────────────────────────────────────────────────
# Room ships consumer rules; only the generated database implementations are
# resolved by name at runtime, so pin that suffix explicitly.
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# ── osmdroid ─────────────────────────────────────────────────────────────────
# Tile sources and overlays are instantiated reflectively from configuration.
-keep class org.osmdroid.** { *; }
-keepclassmembers class org.osmdroid.** { *; }
-dontwarn org.osmdroid.**

# ── Play Services ────────────────────────────────────────────────────────────
-keep class com.google.android.gms.location.** { *; }
-dontwarn com.google.android.gms.**

# ── Kotlin plumbing ──────────────────────────────────────────────────────────
# Metadata is required for Room's suspend-function detection and kotlin-reflect.
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**

# ── Keep line numbers so release crash reports stay readable ─────────────────
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
