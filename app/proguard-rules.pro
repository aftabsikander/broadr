# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /home/seshachalam/android-studio/sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

-target 1.6
-dontobfuscate
-dontoptimize
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-dontpreverify
-verbose
-dump ../bin/class_files.txt
-printseeds ../bin/seeds.txt
-printusage ../bin/unused.txt
-printmapping ../bin/mapping.txt

# The -optimizations option disables some arithmetic simplifications that Dalvik 1.0 and 1.5 can't handle.
-optimizations !code/simplification/arithmetic

-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep class com.google.inject.Binder
-keepclassmembers class * {
    @com.google.inject.Inject <init>(...);
}
# There's no way to keep all @Observes methods, so use the On*Event convention to identify event handlers
-keepclassmembers class * {
    void *(**On*Event);
}
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
    public void set*(...);
}

-keep class * extends java.util.ListResourceBundle {
    protected Object[][] getContents();
}

-keep public class com.google.android.gms.common.internal.safeparcel.SafeParcelable {
    public static final *** NULL;
}

-keepnames @com.google.android.gms.common.annotation.KeepName class *
-keepclassmembernames class * {
    @com.google.android.gms.common.annotation.KeepName *;
}

-keepnames class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

-keepclassmembers class * extends de.greenrobot.dao.AbstractDao {
    public static java.lang.String TABLENAME;
}
-keep class **$Properties

-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
}

-keepattributes Signature
-keepattributes *Annotation*

-keep public class roboguice.**
-keep class com.google.inject.**
-keep class com.google.gson.** {*;}

-keep class com.google.inject.** { *; }
-keep class javax.inject.** { *; }
-keep class javax.annotation.** { *; }
-keep class roboguice.** { *; }

-keep class android.support.v4.app.** { *; }
-keep interface android.support.v4.app.** { *; }
-dontwarn android.support.v4.app.**

-keep class android.support.v7.** { *; }
-keep interface android.support.v7.** { *; }
-dontwarn android.support.v7.**

-keep class com.abbiya.broadr.** { *; }
-keep interface com.abbiya.broadr.** { *; }
-dontwarn com.abbiya.broadr.**

-keep class com.google.ads.AdRequest
-dontwarn com.google.ads.AdRequest

-keep class com.google.android.gms.** { *; }
-keep interface com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

-keep class com.google.gson.** { *; }
-keep interface com.google.gson.** { *; }
-dontwarn com.google.gson.**

-keep class com.google.inject.** { *; }
-keep interface com.google.inject.** { *; }
-dontwarn com.google.inject.**

-keep class com.ocpsoft.pretty.time.** { *; }
-keep interface com.ocpsoft.pretty.time.** { *; }
-dontwarn com.ocpsoft.pretty.time.**

-keep class com.path.android.jobqueue.** { *; }
-keep interface com.path.android.jobqueue.** { *; }
-dontwarn com.path.android.jobqueue.**

-keep class javax.inject.** { *; }
-keep interface javax.inject.** { *; }
-dontwarn javax.inject.**

-keep class com.squareup.okhttp.** { *; }
-keep interface com.squareup.okhttp.** { *; }
-dontwarn com.squareup.okhttp.**

-keep class com.squareup.picasso.** { *; }
-keep interface com.squareup.picasso.** { *; }
-dontwarn com.squareup.picasso.**

-keep class de.greenrobot.dao.** { *; }
-keep interface de.greenrobot.dao.** { *; }
-dontwarn de.greenrobot.dao.**

-keep class org.roboguice.shaded.goole.common.** { *; }
-keep interface org.roboguice.shaded.goole.common.** { *; }
-dontwarn org.roboguice.shaded.goole.common.**

-keep class roboguice.** { *; }
-keep interface roboguice.** { *; }
-dontwarn roboguice.**

-keep class retrofit.** { *; }
-keep interface retrofit.** { *; }
-dontwarn retrofit.**

-keep class roboguice.roboblender.** { *; }
-keep interface roboguice.roboblender.** { *; }
-dontwarn roboguice.roboblender.**

-keep class com.android.vending.licensing.ILicensingService

-dontwarn okio.**

-dontwarn
-ignorewarnings

