# Keep all classes in the extensions and scenecore packages since they can be used externally
-keep public class androidx.xr.extensions.** { *; }
-keep public interface androidx.xr.extensions.** { *; }
-keep @interface androidx.xr.extensions.**

-keep class androidx.xr.scenecore.** { *; }
-keep class androidx.xr.scenecore.**$* { *; }
-keep class androidx.xr.scenecore.impl.** { *; }
-keep class androidx.xr.scenecore.impl.**$* { *; }

-keep class * extends androidx.xr.scenecore.** { *; }
-keep class * extends androidx.xr.scenecore.**$* { *; }
-keep class * extends androidx.xr.scenecore.impl.** { *; }
-keep class * extends androidx.xr.scenecore.impl.**$* { *; }
