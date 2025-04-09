package com.guicarneirodev.ltascore.data.datasource.static

object TeamIdMapping {
    // Mapeamento de IDs numéricos (quando disponíveis)
    private val apiIdToInternalMap = mapOf(
        // Mapeamento dos times da LTA Sul
        "100205576309502431" to "furia",
        "107598699275015260" to "leviatan",
        "113606449173273162" to "isurus-estral",
        "99566408217955692" to "pain-gaming",
        "109480056092207899" to "fluxo-w7m",
        "99566408219409348" to "keyd",
        "105397404796640412" to "loud",
        "99566408221961358" to "red-kalunga",
        // Adicione aqui mapeamentos para times da LTA Norte
    )

    // Novo mapeamento baseado em códigos de times
    private val teamCodeToInternalMap = mapOf(
        // LTA Sul
        "LOUD" to "loud",
        "PAIN" to "pain-gaming",
        "IE" to "isurus-estral",
        "LEV" to "leviatan",
        "FUR" to "furia",
        "VKS" to "keyd",
        "RED" to "red",
        "FXW7" to "fxw7", // Fluxo W7M
        // LTA Norte - Adicionar quando necessário
        "TL" to "team-liquid",
        "C9" to "cloud9",
        "EG" to "eg"
    )

    /**
     * Tenta obter o ID interno de um time a partir do ID da API
     * Se o ID da API não estiver disponível, tenta usar o código do time
     */
    fun getInternalTeamId(apiTeamId: String?, teamCode: String?): String {
        // Primeiro tenta pelo ID da API
        apiTeamId?.let { id ->
            apiIdToInternalMap[id]?.let { return it }
        }

        // Se não encontrar pelo ID, tenta pelo código
        teamCode?.let { code ->
            teamCodeToInternalMap[code]?.let { return it }
        }

        // Se nada funcionar, registra o erro e retorna um valor padrão
        println("Alerta: Não foi possível mapear o time. ID: $apiTeamId, Código: $teamCode")
        return "unknown-team"
    }
}