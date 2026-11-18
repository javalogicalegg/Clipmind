# Add project specific ProGuard rules here.
# Preserve Room entities, DAOs, and model enums for reflection/SQLite mapping
-keep class com.example.data.local.** { *; }
-keep class com.example.model.** { *; }
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
