package com.conferentepro.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.conferentepro.app.domain.model.*

@Entity(tableName = "demandas")
data class DemandaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val titulo: String,
    val descricao: String = "",
    val categoria: CategoriaDemanda,
    val prioridade: Prioridade,
    val status: StatusDemanda = StatusDemanda.PENDENTE,
    val responsabilidade: TipoResponsabilidade = TipoResponsabilidade.MINHA_RESPONSABILIDADE,
    val dataCriacao: Long = System.currentTimeMillis(),
    val dataPrazoFinal: Long,
    val setorRelacionado: String = "",
    val responsavelExterno: String? = null,
    val observacoes: String = "",
    val ultimaAtualizacao: Long = System.currentTimeMillis(),
    val concluida: Boolean = false
)

@Entity(tableName = "historico_timeline")
data class HistoricoTimelineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val demandaId: Long,
    val dataHoraRegistro: Long = System.currentTimeMillis(),
    val tipoAcao: TipoAcaoHistorico,
    val responsavelAcao: String,
    val descricao: String,
    val respostaOuRetorno: String? = null
)

@Entity(tableName = "produtos")
data class ProdutoEntity(
    @PrimaryKey val ean: String,
    val nome: String,
    val sku: String = "",
    val descricao: String = "",
    val dataCadastro: Long = System.currentTimeMillis(),
    val ultimaAtualizacao: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "ocorrencias_produto",
    foreignKeys = [ForeignKey(entity = ProdutoEntity::class, parentColumns = ["ean"], childColumns = ["produtoEan"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("produtoEan"), Index("dataRegistro")]
)
data class OcorrenciaProdutoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val produtoEan: String,
    val tipo: String,
    val destino: String,
    val quantidade: Double = 1.0,
    val unidade: String = "unidade(s)",
    val lote: String = "",
    val validade: Long? = null,
    val protocolo: String = "",
    val observacoes: String = "",
    val status: String = "PENDENTE",
    val dataRegistro: Long = System.currentTimeMillis(),
    val dataResolucao: Long? = null
)
