# kotlinx.serialization (backup DTOs)
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keep,includedescriptorclasses class com.carlens.data.**$$serializer { *; }
-keepclassmembers class com.carlens.data.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
