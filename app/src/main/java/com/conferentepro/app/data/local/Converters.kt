package com.conferentepro.app.data.local

import androidx.room.TypeConverter
import com.conferentepro.app.domain.model.CategoriaDemanda
import com.conferentepro.app.domain.model.Prioridade
import com.conferentepro.app.domain.model.StatusDemanda
import com.conferentepro.app.domain.model.TipoAcaoHistorico
import com.conferentepro.app.domain.model.TipoResponsabilidade

class Converters {
    @TypeConverter fun categoriaToString(value: CategoriaDemanda): String = value.name
    @TypeConverter fun stringToCategoria(value: String): CategoriaDemanda = CategoriaDemanda.valueOf(value)

    @TypeConverter fun prioridadeToString(value: Prioridade): String = value.name
    @TypeConverter fun stringToPrioridade(value: String): Prioridade = Prioridade.valueOf(value)

    @TypeConverter fun statusToString(value: StatusDemanda): String = value.name
    @TypeConverter fun stringToStatus(value: String): StatusDemanda = StatusDemanda.valueOf(value)

    @TypeConverter fun responsabilidadeToString(value: TipoResponsabilidade): String = value.name
    @TypeConverter fun stringToResponsabilidade(value: String): TipoResponsabilidade = TipoResponsabilidade.valueOf(value)

    @TypeConverter fun acaoToString(value: TipoAcaoHistorico): String = value.name
    @TypeConverter fun stringToAcao(value: String): TipoAcaoHistorico = TipoAcaoHistorico.valueOf(value)
}
