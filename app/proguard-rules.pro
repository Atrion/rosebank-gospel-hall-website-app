# Keep line numbers so crash reports in Google Play Console stay readable.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Methods the in-app video page calls from JavaScript.
-keepattributes JavascriptInterface
-keepclassmembers class ca.rosebankgospelhall.app.VideoBridge {
    @android.webkit.JavascriptInterface <methods>;
}

# Classes created by name at runtime.
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
-keepclassmembers class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# Optional libraries that are referenced but not shipped.
-dontwarn com.google.re2j.**
-dontwarn javax.annotation.**
-dontwarn org.jspecify.annotations.**
