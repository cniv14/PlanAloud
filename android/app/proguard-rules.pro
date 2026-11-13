# Signed modules resolve these classes against the host at runtime.
-keep interface app.planaloud.HotModule { *; }
-keep interface app.planaloud.assistant.ConversationAppearance { *; }
-keep class app.planaloud.**.R { *; }
-keep class app.planaloud.**.R$* { *; }
-keep class app.planaloud.voice.NativeSpeech { *; }
-keep class androidx.appcompat.** { *; }
-keep class androidx.core.** { *; }
-keep class androidx.recyclerview.** { *; }
-keep class com.google.android.material.** { *; }
-keep class com.nlf.calendar.** { *; }
-keep class io.noties.markwon.** { *; }
# Markwon supplies these optional decoder classes; Plan Aloud does not enable them.
-dontwarn com.caverock.androidsvg.SVG
-dontwarn com.caverock.androidsvg.SVGParseException
-dontwarn pl.droidsonroids.gif.GifDrawable
-keep class org.commonmark.** { *; }
-keep class org.scilab.forge.jlatexmath.** { *; }
-keep class io.github.yahiaangelo.** { *; }

# WebMessage and Android JNI entry points are discovered by the platform.
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
    native <methods>;
}
