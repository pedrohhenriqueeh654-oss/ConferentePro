package com.conferentepro.app.data.local

import android.content.Context
import androidx.room.*
import com.conferentepro.app.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DemandaDao {
    @Query("SELECT * FROM demandas ORDER BY dataPrazoFinal ASC")
    fun getAllDemandas(): Flow<List<DemandaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDemanda(demanda: DemandaEntity): Long

    @Update
    suspend fun updateDemanda(demanda: DemandaEntity)
}

@Dao
interface HistoricoDao {
    @Query("SELECT * FROM historico_timeline WHERE demandaId = :demandaId ORDER BY dataHoraRegistro DESC")
    fun getHistorico(demandaId: Long): Flow<List<HistoricoTimelineEntity>>

    @Insert
    suspend fun insertHistorico(item: HistoricoTimelineEntity): Long
}

@Database(entities = [DemandaEntity::class, HistoricoTimelineEntity::class], version = 1, exportSchema = false)\n@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun demandaDao(): DemandaDao
    abstract fun historicoDao(): HistoricoDao

    companion object {
        @Volatile private var instance: AppDatabase? = null
        fun getInstance(ctx: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(ctx.applicationContext, AppDatabase::class.java, "conferente_pro.db")
                .fallbackToDestructiveMigration().build().also { instance = it }
        }
    }
}
