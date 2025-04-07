package com.guicarneirodev.ltascore.data.datasource.static

/**
 * Mapeamento de IDs de times da API para IDs internos no banco estático
 */
object TeamIdMapping {
    private val apiToInternalMap = mapOf(
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

    fun getInternalTeamId(apiTeamId: String?): String {
        return apiTeamId?.let { apiToInternalMap[it] } ?: "unknown-team"
    }
}