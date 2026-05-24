# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keep,includedescriptorclasses class **$$serializer { *; }
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

# Ktor + Coroutines + OkHttp
-dontwarn org.slf4j.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**
-dontwarn io.ktor.network.tls.**
-dontwarn java.lang.management.**
-keep class io.ktor.** { *; }
-keep class kotlinx.coroutines.** { *; }

# SQLDelight
-keep class app.cash.sqldelight.** { *; }
-keep class eu.eurostat.**.*Queries { *; }

# Decompose / Essenty (use reflection for serialization of configs)
-keep class com.arkivanov.decompose.** { *; }
-keep class com.arkivanov.essenty.** { *; }
-keep class eu.eurostat.core.navigation.ChildConfig** { *; }

# Keep all @Serializable data classes
-keep,includedescriptorclasses class eu.eurostat.** {
    *** Companion;
    <init>(...);
}
-keepclassmembers class eu.eurostat.** {
    *** Companion;
}
