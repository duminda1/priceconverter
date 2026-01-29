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
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# --- Release shrinker guardrails ---
# Retrofit reflects on HTTP method/parameter annotations; keep those methods intact.
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations, Signature
-keepclassmembers,allowshrinking,allowobfuscation interface com.pockettoolsstudio.pocketcurrency.data.api.** {
    @retrofit2.http.* <methods>;
}

# Gson reflects on fields; keep @SerializedName members so JSON parsing survives obfuscation.
-keepclassmembers class com.pockettoolsstudio.pocketcurrency.data.model.** {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Hilt generates entry points/base classes used by bytecode transformation.
-keep @dagger.hilt.internal.GeneratedEntryPoint class * { *; }
-keep class * implements dagger.hilt.internal.GeneratedComponentManagerHolder { *; }
