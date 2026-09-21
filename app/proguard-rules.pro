# Proguard rules for Velo

# Keep Nordic BLE Library
-keep class no.nordicsemi.android.ble.** { *; }

# Keep DataStore & Kotlin serialization
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}
