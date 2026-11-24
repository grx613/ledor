# Маршруты навигации и модели с @Serializable: сериализаторы ищутся рефлексией по Companion.
-keep @kotlinx.serialization.Serializable class org.ledor.** { *; }
-keepclassmembers class org.ledor.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keepattributes *Annotation*, InnerClasses, Signature

# KosherJava: используем только JewishDate/AstronomicalCalendar; калькулятор выбирается по умолчанию.
-keep class com.kosherjava.zmanim.util.NOAACalculator { *; }
-dontwarn com.kosherjava.**
