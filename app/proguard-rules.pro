# Feature-specific keep rules belong with the feature that requires them.
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keepattributes JavascriptInterface
-keepattributes *Annotation*
-dontwarn com.razorpay.**
-keep class com.razorpay.** { *; }
-optimizations !method/inlining/*
-keepclasseswithmembers class * {
    public void onPayment*(...);
}

# Places SDK 5.1.1 instantiates internal service providers through reflection.
# Required by Google's release notes when R8 full mode is enabled.
-keepclassmembers class com.google.android.libraries.places.internal.** {
    <init>();
}
