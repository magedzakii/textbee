# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Gson serializes these classes by field name. Keep fields stable when R8 is
# enabled for release builds.
-keep class com.vernu.sms.dtos.** { *; }
-keep class com.vernu.sms.models.** { *; }
-keepattributes Signature,*Annotation*,SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
-renamesourcefileattribute SourceFile
