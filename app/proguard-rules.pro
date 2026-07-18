# Keep kotlinx-serialization generated serializers.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class com.vnstudio.vnpad.** {
    kotlinx.serialization.KSerializer serializer(...);
}
