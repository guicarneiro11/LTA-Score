package com.guicarneirodev.ltascore.data.datasource.static

import com.guicarneirodev.ltascore.domain.models.TeamFilterItem

object TeamLogoMapper {
    private val teamIdToLogoMap = mapOf(
        // LTA Sul
        "isurus-estral" to "https://static.lolesports.com/teams/1733496976364_badge-Color-svg.png",
        "pain-gaming" to "https://static.lolesports.com/teams/1674657011011_pain_logo_white.png",
        "furia" to "https://static.lolesports.com/teams/FURIA---black.png",
        "leviatan" to "https://static.lolesports.com/teams/1643795049372_LEV-CLAROWhite.png",
        "keyd" to "https://static.lolesports.com/teams/1670542079678_vks.png",
        "fxw7" to "https://static.lolesports.com/teams/1738138907381_OFFICIALFluxo_W7M_Logo.PNG",
        "loud" to "https://static.lolesports.com/teams/Logo-LOUD-Esports_Original.png",
        "red" to "https://static.lolesports.com/teams/1631820575924_red-2021-worlds.png",

        // LTA Norte
        "team-liquid-honda" to "https://static.lolesports.com/teams/1631820014208_tl-2021-worlds.png",
        "flyquest" to "https://static.lolesports.com/teams/flyquest-new-on-dark.png",
        "shopify-rebellion" to "https://static.lolesports.com/teams/1701424227458_Teams204_Shopify_1632869404072.png",
        "dignitas" to "https://static.lolesports.com/teams/DIG-FullonDark.png",
        "cloud9-kia" to "https://static.lolesports.com/teams/1736924120254_C9Kia_IconBlue_Transparent_2000x2000.png",
        "100-thieves" to "https://static.lolesports.com/teams/1631819887423_100t-2021-worlds.png",
        "disguised" to "https://static.lolesports.com/teams/1731496922454_Disguised-Wordmark-Yellow-Main.png",
        "lyon" to "https://static.lolesports.com/teams/1743717443673_isotypelyon-03.png",

        // Circuito Desafiante
        "red-kalunga-academy" to "https://static.lolesports.com/teams/1642969030123_REDlogo_REDCanids-RGB-linhabranca1.png",
        "keyd-academy" to "https://static.lolesports.com/teams/1670613637640_NAMINGRIGHTS-BRANCO.png",
        "los" to "https://static.lolesports.com/teams/1686087430642_logo_los_white_newest.png",
        "flamengo" to "https://static.lolesports.com/teams/1741770712987_LOGOFLAESPORTSCRFVERMELHA.png",
        "ratz" to "https://static.lolesports.com/teams/1741771391013_logoratz.png",
        "dopamina" to "https://static.lolesports.com/teams/1741770527047_dopamina5.png",
        "stellae" to "https://static.lolesports.com/teams/1741771669890_Logo-Stellae-PNG.png",
        "rise" to "https://static.lolesports.com/teams/1741771240015_2-IYVhiot.png",
        "kabum-idl" to "https://static.lolesports.com/teams/1741770829365_COLORFUL_WHITE.png",
        "corinthians" to "https://static.lolesports.com/teams/1741769767384_LogoFundoEscuro.png"
    )

    private const val LTA_CROSS_LOGO = "https://static.lolesports.com/leagues/1731566966819_LTA-LOGO-LightGold_RGB2000px.png"

    private const val FALLBACK_LOGO_URL = "https://static.lolesports.com/teams/1592591395339_IconPlaceholder.png"

    fun getTeamLogoUrl(teamId: String?): String {
        return if (teamId != null) {
            teamIdToLogoMap[teamId] ?: FALLBACK_LOGO_URL
        } else {
            FALLBACK_LOGO_URL
        }
    }

    fun getLtaCrossLogo(): String {
        return LTA_CROSS_LOGO
    }

    fun updateTeamLogoUrl(team: TeamFilterItem): TeamFilterItem {
        return team.copy(imageUrl = getTeamLogoUrl(team.id))
    }
}