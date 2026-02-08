# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
-renamesourcefileattribute SourceFile

# This rule instructs ProGuard to keep all classes in the kotlinx.coroutines.flow package.
# The allowobfuscation and allowshrinking flags allow ProGuard to obfuscate the names of the classes
# and to remove unused classes or methods, respectively.
-keep,allowobfuscation,allowshrinking class kotlinx.coroutines.flow.*


# Retain generic signatures of TypeToken and its subclasses with R8 version 3.0 and higher.
-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class * extends com.google.gson.reflect.TypeToken
-keep public class * implements java.lang.reflect.Type

# Uncomment if needed
# Application classes that will be serialized/deserialized over Gson
#-keep class com.google.gson.examples.android.model.** { <fields>; }

# Prevent proguard from stripping interface information from TypeAdapter, TypeAdapterFactory,
# JsonSerializer, JsonDeserializer instances (so they can be used in @JsonAdapter)
# -keep class * extends com.google.gson.TypeAdapter
# -keep class * implements com.google.gson.TypeAdapterFactory
# -keep class * implements com.google.gson.JsonSerializer
# -keep class * implements com.google.gson.JsonDeserializer

# This ensures that the information about generic types and
# any annotations present in your code are preserved after obfuscation.
-keepattributes Signature
-keepattributes *Annotation*

# This rule tells ProGuard to assume that the Log methods have no side effects.
# It allows ProGuard to remove calls to these methods (like Log.e(), Log.d(), etc.) from the final build,
# which helps in reducing the app size and improving performance
# since logging can be a performance bottleneck in release builds.
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int e(...);
    public static int w(...);
    public static int wtf(...);
    public static int d(...);
    public static int v(...);
    public static int i(...);
}

-keep class com.gmail.vondenuelle.denuspend.data.remote.models.* { *; }

-keep class com.gmail.vondenuelle.denuspend.data.repositories.auth.response.* { *; }
-keep class com.gmail.vondenuelle.denuspend.data.repositories.auth.request.* { *; }

# Firebase Auth
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }