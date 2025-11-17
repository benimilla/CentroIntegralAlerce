###############################################
#              FIREBASE / FIRESTORE
###############################################
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes *Annotation*

# Firebase core
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**

# Firestore & deserialización
-keepclassmembers class * {
    @com.google.firebase.firestore.PropertyName <fields>;
    @com.google.firebase.firestore.Exclude <fields>;
    @com.google.firebase.firestore.ServerTimestamp <fields>;
}
-keep class com.google.firestore.** { *; }

###############################################
#              KOTLIN / COROUTINES
###############################################
-keep class kotlin.** { *; }
-keep class kotlinx.** { *; }
-dontwarn kotlin.**

###############################################
#           TUS MODELOS (CRÍTICO)
###############################################
-keep class cl.alercelab.centrointegral.data.** { *; }
-keep class cl.alercelab.centrointegral.domain.** { *; }
-keep class cl.alercelab.centrointegral.admin.** { *; }
-keep class cl.alercelab.centrointegral.activities.** { *; }

###############################################
#            JSON / GSON (si lo usas)
###############################################
-keep class com.google.gson.** { *; }
-dontwarn com.google.gson.**

###############################################
#        ANDROIDX / VIEWMODEL / ROOM
###############################################
-keep class androidx.lifecycle.** { *; }
-keep class androidx.room.** { *; }
-dontwarn androidx.**
