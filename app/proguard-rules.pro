# Add project specific ProGuard rules here.
-keep class com.pulsenet.app.data.local.entity.** { *; }
-keep class com.pulsenet.app.data.remote.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**
