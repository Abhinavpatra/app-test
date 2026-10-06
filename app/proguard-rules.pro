# Bloom release rules. SQLCipher ships its own consumer rules as of 4.14.0; these cover ours.

-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes *Annotation*

# Room: entities, DAOs and the generated database impl are reached reflectively.
-keep class com.bloomcycle.app.data.local.** { *; }

# Firebase and Play services: reached through generated code and reflection.
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# SQLCipher native bridge.
-keep class net.zetetic.** { *; }
-dontwarn net.zetetic.**

# Crash readability: keep file/line info without keeping the original names.
-keepattributes SourceFile, LineNumberTable
-renamesourcefileattribute SourceFile
