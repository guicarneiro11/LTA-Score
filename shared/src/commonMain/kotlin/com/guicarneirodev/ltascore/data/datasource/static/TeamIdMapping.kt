package com.guicarneirodev.ltascore.data.datasource.static

object TeamIdMapping {
    private val apiIdToInternalMap = mapOf(
        // LTA South
        "100205576309502431" to "furia",
        "107598699275015260" to "leviatan",
        "113606449173273162" to "isurus-estral",
        "99566408217955692" to "pain-gaming",
        "109480056092207899" to "fluxo-w7m",
        "99566408219409348" to "keyd",
        "105397404796640412" to "loud",
        "99566408221961358" to "red-kalunga",

        // LTA North
        "98926509885559666" to "team-liquid-honda",
        "98926509892121852" to "flyquest",
        "111504538396430510" to "shopify-rebellion",
        "98926509883054987" to "dignitas",
        "98767991877340524" to "cloud9-kia",
        "99294153828264740" to "100-thieves",
        "110428362822825796" to "disguised",
        "99566405941863385" to "lyon",

        // Circuito Desafiante
        "105550001032913831" to "red-kalunga-academy",
        "109485335453835911" to "keyd-academy",
        "109480204628225868" to "los",
        "109546466354141671" to "flamengo",
        "114148730209204377" to "ratz",
        "114148673565223449" to "dopamina",
        "114148748659161269" to "stellae",
        "112489375097659332" to "rise",
        "105549995617170853" to "kabum-idl",
        "114148623959237019" to "corinthians"
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
        "TL" to "team-liquid-honda",
        "FLY" to "flyquest",
        "SR" to "shopify-rebellion",
        "DIG" to "dignitas",
        "C9" to "cloud9-kia",
        "100T" to "100-thieves",
        "DSG" to "disguised",
        "LYON" to "lyon",

        // Circuito Desafiante
        "RED" to "red-kalunga-academy",
        "VKS" to "keyd-academy",
        "LOS" to "los",
        "FLA" to "flamengo",
        "RATZ" to "ratz",
        "DPM" to "dopamina",
        "STE" to "stellae",
        "RISE" to "rise",
        "KBM" to "kabum-idl",
        "SCCP" to "corinthians"
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