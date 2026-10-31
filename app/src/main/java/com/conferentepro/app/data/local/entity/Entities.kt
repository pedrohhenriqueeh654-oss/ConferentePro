package com.conferentepro.app.data.local.entity

import androidx.room.Entity
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
