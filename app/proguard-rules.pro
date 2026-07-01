# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep line numbers for easier crash debugging in Play Store Console
-keepattributes SourceFile,LineNumberTable

# Gson rules
-keepattributes *Annotation*,Signature
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Keep local database entities, models, and DAOs
-keep class com.example.rasaushadhies.data.local.** { *; }

# Keep data models parsed from JSON
-keep class com.example.rasaushadhies.ui.data.** { *; }

# Keep UI state models used in views
-keep class com.example.rasaushadhies.ui.screens.Medicine { *; }

# Keep Firebase models and Firestore reflections
-keep class com.google.firebase.** { *; }