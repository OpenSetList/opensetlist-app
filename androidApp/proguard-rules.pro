# OpenSetlist - ProGuard/R8 rules for Android release builds

# Entry point activity (referenced from the AndroidManifest).
-keep public class com.opensetlist.app.MainActivity { *; }

# SQLDelight generated schema/queries - reached through generated API but kept intact
# to avoid stripping classes referenced by the query interface implementation.
-keep class com.opensetlist.app.data.db.** { *; }

# Runtime attributes required by Compose, coroutines and metadata-based lookups.
-keepattributes *Annotation*, Signature, ExceptionTable, InnerClasses, EnclosingMethod

# Coroutines: volatile fields updated via AtomicReferenceFieldUpdater must not be renamed.
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-keepclassmembers class kotlin.coroutines.SafeContinuation {
    volatile <fields>;
}
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Compose desktop/unused JVM-only classes that R8 cannot resolve on Android.
-dontnote androidx.**
-dontnote kotlinx.**
-dontwarn kotlinx.coroutines.debug.**
-dontwarn sun.misc.Signal
-dontwarn sun.misc.SignalHandler
-dontwarn java.lang.instrument.ClassFileTransformer
-dontwarn java.lang.instrument.Instrumentation
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement

# Ktor (HTTP): slf4j binding e classes JVM ausentes no Android.
-dontwarn org.slf4j.**
-dontwarn io.ktor.client.engine.okhttp.**
-dontwarn io.ktor.utils.io.**
-dontwarn okhttp3.internal.platform.**
-dontwarn kotlinx.io.**
