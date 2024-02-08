-dontobfuscate

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

# From extensions-lib
-keep,allowoptimization class com.sf.tadami.network.NetworkHelper { public protected *; }
-keep,allowoptimization class com.sf.tadami.network.OkHttpExtensionsKt { public protected *; }
-keep,allowoptimization class com.sf.tadami.network.RequestsKt { public protected *; }
-keep,allowoptimization class com.sf.tadami.domain.anime.Anime { public protected *; }
-keep,allowoptimization class com.sf.tadami.preferences.model.SourcePreference$** { public protected *; }
-keep,allowoptimization class com.sf.tadami.ui.tabs.browse.tabs.sources.preferences.SourcesPreferencesContent { public protected *; }
-keep,allowoptimization class com.sf.tadami.source.** { public protected *; }
-keep,allowoptimization class com.sf.tadami.ui.components.data.Action { public protected *; }
-keep,allowoptimization class com.sf.tadami.ui.utils.** { public protected *; }

##---------------Begin: proguard configuration for kotlinx.serialization  ----------
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.** # core serialization annotations

# kotlinx-serialization-json specific. Add this if you have java.lang.NoClassDefFoundError kotlinx.serialization.json.JsonObjectSerializer
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class eu.kanade.**$$serializer { *; }
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

# XmlUtil
-keep public enum nl.adaptivity.xmlutil.EventType { *; }


