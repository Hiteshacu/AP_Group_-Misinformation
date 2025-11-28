# Add project specific ProGuard rules here.
# Keep Retrofit and Gson classes
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class retrofit2.** { *; }
