-dontobfuscate

# Strip debug-level logging from release (minify runs only there): removes every Log.v/d/i body,
# including the cast proxy's per-segment REQ/RESP/HDR/UP traces and the media URLs they carry.
# Log.w/Log.e stay — real errors (receiver-reported cast errors, service failures) remain visible.
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

-keep,allowoptimization class com.sf.tadami.**

# Keep common dependencies used in extensions
-keep,allowoptimization class androidx.datastore.** { public protected *; }
-keep,allowoptimization class kotlin.** { public protected *; }
-keep,allowoptimization class kotlinx.coroutines.** { public protected *; }
-keep,allowoptimization class kotlinx.serialization.** { public protected *; }
-keep,allowoptimization class kotlin.time.** { public protected *; }
-keep,allowoptimization class okhttp3.** { public protected *; }
-keep,allowoptimization class okio.** { public protected *; }
-keep,allowoptimization class org.jsoup.** { public protected *; }
-keep,allowoptimization class io.reactivex.rxjava3.** { public protected *; }
-keep,allowoptimization class app.cash.quickjs.** { public protected *; }
-keep,allowoptimization class uy.kohesive.injekt.** { public protected *; }

# The classes of extensions-lib, with their public and protected members, are kept by the rules
# :app:generateExtensionsApiKeepRules writes (build/generated/proguard/extensions-api.pro)

# Methods JavaScript calls by name, like those of WebViewResolver's script bridge
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

##---------------Begin: proguard configuration for kotlinx.serialization  ----------
-keepattributes *Annotation*, InnerClasses, EnclosingMethod, Signature
-dontnote kotlinx.serialization.** # core serialization annotations

# kotlinx-serialization-json specific. Add this if you have java.lang.NoClassDefFoundError kotlinx.serialization.json.JsonObjectSerializer
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keepclassmembers class com.sf.** {
    *** Companion;
}
-keepclasseswithmembers class com.sf.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep class kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.** {
    <methods>;
}

##---------------End: proguard configuration for kotlinx.serialization  ----------





