package com.conferentepro.app.domain.model

enum class CategoriaDemanda(val label: String) {
    RECEBIMENTO("Recebimento"),
    CONFERENCIA("Conferência"),
    ESTOQUE("Estoque"),
    CD("Centro de Distribuição"),
    FORNECEDOR("Fornecedor"),
    TROCAS("Trocas"),
    AVARIAS("Avarias"),
    NOTAS_FISCAIS("Notas Fiscais"),
    DIVERGENCIAS("Divergências"),
    DEVOLUCOES("Devoluções"),
    FALTAS_SOBRAS("Faltas e Sobras"),
    OUTROS("Outros")
}

enum class Prioridade(val label: String) {
    BAIXA("Baixa"),
    MEDIA("Média"),
    ALTA("Alta"),
    URGENTE("Urgente")
}

enum class StatusDemanda(val label: String) {
    PENDENTE("Pendente"),
    EM_ANDAMENTO("Em andamento"),
    AGUARDANDO_CD("Aguardando CD"),
    AGUARDANDO_FORNECEDOR("Aguardando Fornecedor"),
    AGUARDANDO_TERCEIROS("Aguardando Terceiros"),
    CONCLUIDA("Concluída"),
    CANCELADA("Cancelada")
}

enum class TipoResponsabilidade(val label: String) {
    MINHA_RESPONSABILIDADE("Usuário"),
    TERCEIROS("Terceiros"),
    AGUARDANDO_RETORNO("Aguardando Retorno")
}

enum class TipoAcaoHistorico {
    CRIACAO,
    MUDANCA_STATUS,
    ENCAMINHAMENTO,
    COBRANCA_FEITA,
    CONCLUSAO_CONFIRMADA
}
