# Crashlytics: readable stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.codingwithtashi.dailyprayer.**$$serializer { *; }
-keepclassmembers class com.codingwithtashi.dailyprayer.** {
    *** Companion;
}
-keepclasseswithmembers class com.codingwithtashi.dailyprayer.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Type-safe navigation routes (serialized by class name)
-keep class com.codingwithtashi.dailyprayer.navigation.** { *; }

# Room, Hilt, Media3 and Firebase ship consumer rules.
