package com.guicarneirodev.ltascore.data.datasource.static

object TeamIdMapping {
    private val apiIdToInternalMap = mapOf(
        "100205576309502431" to "furia",
        "107598699275015260" to "leviatan",
        "113606449173273162" to "isurus-estral",
        "99566408217955692" to "pain-gaming",
        "109480056092207899" to "fluxo-w7m",
        "99566408219409348" to "keyd",
        "105397404796640412" to "loud",
        "99566408221961358" to "red-kalunga",
    )

    private val teamCodeToInternalMap = mapOf(
        // LTA Sul
        "LOUD" to "loud",
        "PAIN" to "pain-gaming",
        "IE" to "isurus-estral",
        "LEV" to "leviatan",
        "FUR" to "furia",
        "VKS" to "keyd",
        "RED" to "red",
        "FXW7" to "fxw7",

        // LTA Norte
        "TL" to "team-liquid",
        "C9" to "cloud9",
        "EG" to "eg"
    )

    fun getInternalTeamId(apiTeamId: String?, teamCode: String?): String {
        apiTeamId?.let { id ->
            apiIdToInternalMap[id]?.let { return it }
        }

        teamCode?.let { code ->
            teamCodeToInternalMap[code]?.let { return it }
        }

        println("Alerta: Não foi possível mapear o time. ID: $apiTeamId, Código: $teamCode")
        return "unknown-team"
    }
}