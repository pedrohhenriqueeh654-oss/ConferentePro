# Preserva metadados usados por Room, Kotlin e serializacao.
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
-keep class kotlin.Metadata { *; }

# Room: entidades, DAOs, conversores e implementacao gerada.
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep @androidx.room.TypeConverters class * { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
-keep class com.conferentepro.app.data.local.entity.** { *; }

# Componentes Android instanciados pelo framework.
-keep public class com.conferentepro.app.ConferenteProApp { *; }
-keep public class com.conferentepro.app.MainActivity { *; }
-keep public class com.conferentepro.app.worker.** extends android.content.BroadcastReceiver { *; }
-keep public class com.conferentepro.app.worker.** extends androidx.work.ListenableWorker { *; }

# Mantem nomes de membros anotados que podem ser acessados por reflexao.
-keepclassmembers class * {
    @androidx.room.* <fields>;
    @androidx.room.* <methods>;
}
