package com.conferentepro.app.data.local

import android.content.Context
import androidx.room.*
import com.conferentepro.app.data.local.entity.*
import kotlinx.coroutines.flow.Flow
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

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

@Dao
interface ProdutoDao {
    @Query("SELECT * FROM produtos ORDER BY nome COLLATE NOCASE ASC")
    fun getProdutos(): Flow<List<ProdutoEntity>>
    @Query("SELECT * FROM produtos WHERE ean = :ean LIMIT 1")
    suspend fun getProduto(ean: String): ProdutoEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun salvarProduto(produto: ProdutoEntity)
    @Query("SELECT * FROM ocorrencias_produto WHERE produtoEan = :ean ORDER BY dataRegistro DESC")
    fun getOcorrencias(ean: String): Flow<List<OcorrenciaProdutoEntity>>
    @Query("SELECT * FROM ocorrencias_produto ORDER BY dataRegistro DESC")
    fun getTodasOcorrencias(): Flow<List<OcorrenciaProdutoEntity>>
    @Insert
    suspend fun inserirOcorrencia(ocorrencia: OcorrenciaProdutoEntity): Long
}

@Dao
interface CobrancaDao {
    @Query("SELECT * FROM cobrancas ORDER BY COALESCE(proximaCobranca, dataContato) ASC")
    fun getTodas(): Flow<List<CobrancaEntity>>
    @Insert
    suspend fun inserir(item: CobrancaEntity): Long
    @Query("UPDATE cobrancas SET status = 'RESOLVIDA', proximaCobranca = NULL WHERE id = :id")
    suspend fun concluir(id: Long)
}

@Dao
interface RemanejoDao {
    @Query("SELECT * FROM remanejos ORDER BY dataRegistro DESC")
    fun getTodos(): Flow<List<RemanejoEntity>>
    @Insert
    suspend fun inserir(item: RemanejoEntity): Long
    @Query("UPDATE remanejos SET assinaturaBase64 = :assinatura WHERE id = :id")
    suspend fun atualizarAssinatura(id: Long, assinatura: String)
}

@Database(
    entities = [DemandaEntity::class, HistoricoTimelineEntity::class, ProdutoEntity::class, OcorrenciaProdutoEntity::class, CobrancaEntity::class, RemanejoEntity::class],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun demandaDao(): DemandaDao
    abstract fun historicoDao(): HistoricoDao
    abstract fun produtoDao(): ProdutoDao
    abstract fun cobrancaDao(): CobrancaDao
    abstract fun remanejoDao(): RemanejoDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS produtos (ean TEXT NOT NULL PRIMARY KEY, nome TEXT NOT NULL, sku TEXT NOT NULL DEFAULT '', descricao TEXT NOT NULL DEFAULT '', dataCadastro INTEGER NOT NULL, ultimaAtualizacao INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS ocorrencias_produto (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, produtoEan TEXT NOT NULL, tipo TEXT NOT NULL, destino TEXT NOT NULL, quantidade REAL NOT NULL, unidade TEXT NOT NULL, lote TEXT NOT NULL, validade INTEGER, protocolo TEXT NOT NULL, observacoes TEXT NOT NULL, status TEXT NOT NULL, dataRegistro INTEGER NOT NULL, dataResolucao INTEGER, FOREIGN KEY(produtoEan) REFERENCES produtos(ean) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_ocorrencias_produto_produtoEan ON ocorrencias_produto(produtoEan)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_ocorrencias_produto_dataRegistro ON ocorrencias_produto(dataRegistro)")
            }
        }
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS cobrancas (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, destino TEXT NOT NULL, referencia TEXT NOT NULL, pessoaContatada TEXT NOT NULL, dataContato INTEGER NOT NULL, resposta TEXT NOT NULL, proximaCobranca INTEGER, status TEXT NOT NULL, observacoes TEXT NOT NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_cobrancas_proximaCobranca ON cobrancas(proximaCobranca)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_cobrancas_dataContato ON cobrancas(dataContato)")
            }
        }
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS remanejos (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, numeroNota TEXT NOT NULL, filialSaida TEXT NOT NULL, filialDestino TEXT NOT NULL, retiradoPor TEXT NOT NULL, assinaturaBase64 TEXT NOT NULL, observacoes TEXT NOT NULL, dataRegistro INTEGER NOT NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_remanejos_numeroNota ON remanejos(numeroNota)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_remanejos_dataRegistro ON remanejos(dataRegistro)")
            }
        }
        @Volatile private var instance: AppDatabase? = null
        fun getInstance(ctx: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(ctx.applicationContext, AppDatabase::class.java, "conferente_pro.db")
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build().also { instance = it }
            }
    }
}
