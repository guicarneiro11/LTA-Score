package com.guicarneirodev.ltascore.data.datasource.static

import com.guicarneirodev.ltascore.domain.models.Player
import com.guicarneirodev.ltascore.domain.models.PlayerPosition
import kotlinx.datetime.Instant

class PlayersDataSource {

    private val playersByTeamId: Map<String, List<Player>> by lazy {
        allPlayers.groupBy { it.teamId }
    }

    fun getAllPlayers(): List<Player> = allPlayers

    fun getPlayerById(playerId: String): Player? {
        return allPlayers.find { it.id == playerId }
    }

    fun getPlayersByTeamIdAndDate(teamId: String, matchDate: Instant, blockName: String): List<Player> {
        val allTeamPlayers = playersByTeamId[teamId] ?: emptyList()

        if (teamId == "isurus-estral") {
            val isWeek1 = blockName.contains("Semana 1", ignoreCase = true) ||
                    blockName.contains("Week 1", ignoreCase = true)

            val isWeek2OrLater = blockName.contains("Semana 2", ignoreCase = true) ||
                    blockName.contains("Week 2", ignoreCase = true) ||
                    blockName.contains("Semana 3", ignoreCase = true) ||
                    blockName.contains("Week 3", ignoreCase = true) ||
                    blockName.contains("Semana 4", ignoreCase = true) ||
                    blockName.contains("Week 4", ignoreCase = true)

            val week2StartDate = Instant.parse("2025-04-08T00:00:00Z")

            val isSemana1 = isWeek1 || (!isWeek2OrLater && matchDate < week2StartDate)

            println("Partida de ${matchDate}, blockName: $blockName, isSemana1: $isSemana1")

            return if (isSemana1) {
                println("Retornando time IE com Burdol (Semana 1)")
                allTeamPlayers.filter { it.id != "player_ie_summit" }
            } else {
                println("Retornando time IE com Summit (Semana 2+)")
                allTeamPlayers.filter { it.id != "player_ie_burdol" }
            }
        }

        if (teamId == "red") {
            val aegisStartDate = Instant.parse("2025-04-21T00:00:00Z")
            val grevtharStartDate = Instant.parse("2025-05-10T00:00:00Z")

            val useAegis = matchDate >= aegisStartDate
            val useGrevthar = matchDate >= grevtharStartDate

            println("Partida RED de ${matchDate}, useAegis: $useAegis, useGrevthar: $useGrevthar")

            val filteredPlayers = allTeamPlayers.filter { player ->
                val keepPlayer = when (player.id) {
                    "player_red_aegis" -> useAegis
                    "player_red_doom" -> !useAegis
                    "player_red_grevthar" -> useGrevthar
                    "player_red_mago" -> !useGrevthar
                    else -> true
                }
                keepPlayer
            }

            return filteredPlayers
        }

        if (teamId == "red-kalunga-academy") {
            val aegisStartDate = Instant.parse("2025-04-21T00:00:00Z")
            val kazeStartDate = Instant.parse("2025-05-12T00:00:00Z")
            val grevtharStartDate = Instant.parse("2025-05-10T00:00:00Z")

            val useAegis = matchDate >= aegisStartDate
            val useKaze = matchDate >= kazeStartDate
            val useGrevthar = matchDate >= grevtharStartDate

            println("Partida RED Academy de ${matchDate}, useAegis: $useAegis, useKaze: $useKaze")

            val filteredPlayers = allTeamPlayers.filter { player ->
                val keepPlayer = when (player.id) {
                    "player_red_academy_aegis" -> !useAegis
                    "player_red_academy_doom" -> useAegis
                    "player_red_academy_kaze" -> useKaze
                    "player_red_academy_grevthar" -> !useKaze && !useGrevthar
                    "player_red_academy_mago" -> !useKaze && useGrevthar
                    else -> true
                }
                keepPlayer
            }

            return filteredPlayers
        }

        if (teamId == "corinthians") {
            val rosterChangeDate = Instant.parse("2025-03-24T00:00:00Z")

            println("Partida Corinthians de ${matchDate}, verificando elenco")

            if (matchDate < rosterChangeDate) {
                println("Retornando time Corinthians com Xico (mid) e Telas (suporte)")
                return allTeamPlayers.filter {
                    it.id != "player_corinthians_leleko" && it.id != "player_corinthians_manel"
                }
            }
            else {
                println("Retornando time Corinthians com Leleko (mid) e Manel (suporte)")
                return allTeamPlayers.filter {
                    it.id != "player_corinthians_xico" && it.id != "player_corinthians_telas"
                }
            }
        }

        if (teamId == "kabum-idl") {
            val rangerStartDate = Instant.parse("2025-04-08T00:00:00Z")
            val wizStartDate = Instant.parse("2025-04-22T00:00:00Z")

            println("Partida KaBuM! IDL de ${matchDate}, verificando elenco")

            if (matchDate < rangerStartDate) {
                println("Retornando time KaBuM! IDL com Seize na jungle (até 07/04)")
                return allTeamPlayers.filter {
                    it.id != "player_kabum_ranger" && it.id != "player_kabum_wiz"
                }
            }
            else if (matchDate < wizStartDate) {
                println("Retornando time KaBuM! IDL com Ranger na jungle (de 08/04 até 21/04)")
                return allTeamPlayers.filter {
                    it.id != "player_kabum_seize" && it.id != "player_kabum_wiz"
                }
            }
            else {
                println("Retornando time KaBuM! IDL com Wiz na jungle (a partir de 22/04)")
                return allTeamPlayers.filter {
                    it.id != "player_kabum_seize" && it.id != "player_kabum_ranger"
                }
            }
        }

        if (teamId == "dopamina") {
            val forlinStartDate = Instant.parse("2025-04-08T00:00:00Z")

            println("Partida Dopamina de ${matchDate}, verificando elenco")

            if (matchDate < forlinStartDate) {
                println("Retornando time Dopamina com Ayel no top (até 31/03/2025)")
                return allTeamPlayers.filter { it.id != "player_dopamina_forlin" }
            }
            else {
                println("Retornando time Dopamina com Forlin no top (a partir de 08/04/2025)")
                return allTeamPlayers.filter { it.id != "player_dopamina_ayel" }
            }
        }

        if (teamId == "ratz") {
            val drakeHeroStartDate = Instant.parse("2025-03-24T00:00:00Z")
            val beenieStartDate = Instant.parse("2025-04-01T00:00:00Z")

            println("Partida RATZ de ${matchDate}, verificando elenco")

            if (matchDate < drakeHeroStartDate) {
                println("Retornando time RATZ com Soweto na jungle e Buero no ADC (até 23/03)")
                return allTeamPlayers.filter { it.id != "player_ratz_drakehero" && it.id != "player_ratz_beenie" }
            }
            else if (matchDate < beenieStartDate) {
                println("Retornando time RATZ com DrakeHero na jungle e Soweto no ADC (de 24/03 até 31/03)")
                return allTeamPlayers.filter { it.id != "player_ratz_beenie" && it.id != "player_ratz_buero" }
            }
            else {
                println("Retornando time RATZ com DrakeHero na jungle e Beenie no ADC (a partir de 01/04)")
                return allTeamPlayers.filter { it.id != "player_ratz_buero" && it.id != "player_ratz_soweto" }
            }
        }

        if (teamId == "rise") {
            val neroStartDate = Instant.parse("2025-04-15T00:00:00Z")

            println("Partida Rise Gaming de ${matchDate}, verificando elenco")

            if (matchDate < neroStartDate) {
                println("Retornando time Rise Gaming com ThayT na jungle (até 08/04)")
                return allTeamPlayers.filter { it.id != "player_rise_nero" }
            }
            else {
                println("Retornando time Rise Gaming com Nero na jungle (a partir de 15/04)")
                return allTeamPlayers.filter { it.id != "player_rise_thayt" }
            }
        }

        return allTeamPlayers
    }

    companion object {
        private val allPlayers = listOf(
            // LTA Sul
            // paiN Gaming
            Player(
                id = "player_pain_wizer",
                name = "Choi Eui-seok",
                nickname = "Wizer",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/a/ad/PAIN_Wizer_LTA_2025_Split_1.png/revision/latest?cb=20250213190024",
                position = PlayerPosition.TOP,
                teamId = "pain-gaming"
            ),
            Player(
                id = "player_pain_cariok",
                name = "Marcos Santos de Oliveira Junior",
                nickname = "Cariok",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/6/66/PAIN_CarioK_LTA_2025_Split_1.png/revision/latest?cb=20250213190114",
                position = PlayerPosition.JUNGLE,
                teamId = "pain-gaming"
            ),
            Player(
                id = "player_pain_roamer",
                name = "Jo Woo-jin",
                nickname = "Roamer",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/6/6e/PAIN_Roamer_2025_Split_2.png/revision/latest?cb=20250405203909",
                position = PlayerPosition.MID,
                teamId = "pain-gaming"
            ),
            Player(
                id = "player_pain_titan",
                name = "Alexandre Lima dos Santos",
                nickname = "TitaN",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/3/32/PAIN_TitaN_LTA_2025_Split_1.png/revision/latest?cb=20250213190619",
                position = PlayerPosition.ADC,
                teamId = "pain-gaming"
            ),
            Player(
                id = "player_pain_kuri",
                name = "Choi Won-yeong",
                nickname = "Kuri",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/a/a6/PAIN_Kuri_LTA_2025_Split_1.png/revision/latest?cb=20250213190700",
                position = PlayerPosition.SUPPORT,
                teamId = "pain-gaming"
            ),

            // LOUD
            Player(
                id = "player_loud_robo",
                name = "Leonardo Souza",
                nickname = "Robo",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/b/b8/LOUD_Robo_LTA_2025_Split_1.png/revision/latest?cb=20250213192537",
                position = PlayerPosition.TOP,
                teamId = "loud"
            ),
            Player(
                id = "player_loud_shini",
                name = "Diogo Rogê Moreira",
                nickname = "Shini",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/5/5a/LOUD_Shini_2025_Split_2.png/revision/latest?cb=20250405204056",
                position = PlayerPosition.JUNGLE,
                teamId = "loud"
            ),
            Player(
                id = "player_loud_tinowns",
                name = "Thiago Sartori",
                nickname = "Tinowns",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/7/73/LOUD_tinowns_LTA_2025_Split_1.png/revision/latest?cb=20250213192613",
                position = PlayerPosition.MID,
                teamId = "loud"
            ),
            Player(
                id = "player_loud_route",
                name = "Moon Geom-su",
                nickname = "Route",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/3/3d/LOUD_Route_LTA_2025_Split_1.png/revision/latest?cb=20250213192640",
                position = PlayerPosition.ADC,
                teamId = "loud"
            ),
            Player(
                id = "player_loud_redbert",
                name = "Ygor Freitas",
                nickname = "RedBert",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/5/51/LOUD_Redbert_2025_Split_2.png/revision/latest?cb=20250405204135",
                position = PlayerPosition.SUPPORT,
                teamId = "loud"
            ),

            // Isurus Estral
            Player(
                id = "player_ie_summit",
                name = "Park Woo-tae",
                nickname = "Summit",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/3/35/R7_Summit_2024_Closing.png/revision/latest?cb=20240613054346",
                position = PlayerPosition.TOP,
                teamId = "isurus-estral"
            ),
            Player(
                id = "player_ie_burdol",
                name = "Noh Tae-yoon",
                nickname = "Burdol",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/b/b5/IE_Burdol_LTA_2025_Split_1.png/revision/latest?cb=20250213182149",
                position = PlayerPosition.TOP,
                teamId = "isurus-estral"
            ),
            Player(
                id = "player_ie_josedeodo",
                name = "Brandon Joel Villegas",
                nickname = "Josedeodo",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/3/38/IE_Josedeodo_LTA_2025_Split_1.png/revision/latest?cb=20250213182247",
                position = PlayerPosition.JUNGLE,
                teamId = "isurus-estral"
            ),
            Player(
                id = "player_ie_mireu",
                name = "Jeong Jo-bin",
                nickname = "Mireu",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/1/15/IE_Mireu_LTA_2025_Split_1.png/revision/latest?cb=20250213182317",
                position = PlayerPosition.MID,
                teamId = "isurus-estral"
            ),
            Player(
                id = "player_ie_snaker",
                name = "Brian Alejo Distefano",
                nickname = "Snaker",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/d/d4/IE_Snaker_LTA_2025_Split_1.png/revision/latest?cb=20250213182431",
                position = PlayerPosition.ADC,
                teamId = "isurus-estral"
            ),
            Player(
                id = "player_ie_ackerman",
                name = "Gabriel Aparicio",
                nickname = "Ackerman",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/9/92/IE_Ackerman_LTA_2025_Split_1.png/revision/latest?cb=20250213182530",
                position = PlayerPosition.SUPPORT,
                teamId = "isurus-estral"
            ),

            // LEVIATÁN
            Player(
                id = "player_lev_zothve",
                name = "Cristóbal Arróspide",
                nickname = "Zothve",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/8/80/LEV_Zothve_LTA_2025_Split_1.png/revision/latest?cb=20250213184019",
                position = PlayerPosition.TOP,
                teamId = "leviatan"
            ),
            Player(
                id = "player_lev_scary",
                name = "Artur Queiroz Scalabrini",
                nickname = "SCARY",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/a/ad/LEV_SCARY_LTA_2025_Split_1.png/revision/latest?cb=20250213184041",
                position = PlayerPosition.JUNGLE,
                teamId = "leviatan"
            ),
            Player(
                id = "player_lev_cody",
                name = "Cristian Sebastián Quispe Yampara",
                nickname = "cody",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/d/d7/LEV_cody_LTA_2025_Split_1.png/revision/latest?cb=20250213184105",
                position = PlayerPosition.MID,
                teamId = "leviatan"
            ),
            Player(
                id = "player_lev_ceo",
                name = "Lorenzo Tévez",
                nickname = "ceo",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/4/4f/LEV_ceo_LTA_2025_Split_1.png/revision/latest?cb=20250213184127",
                position = PlayerPosition.ADC,
                teamId = "leviatan"
            ),
            Player(
                id = "player_lev_prodelta",
                name = "Fábio Luis Bezerra Marques",
                nickname = "ProDelta",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/2/27/LEV_ProDelta_LTA_2025_Split_1.png/revision/latest?cb=20250213184417",
                position = PlayerPosition.SUPPORT,
                teamId = "leviatan"
            ),

            // Furia
            Player(
                id = "player_furia_guigo",
                name = "Guilherme Araújo Ruiz",
                nickname = "Guigo",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/0/08/FURIA_Guigo_LTA_2025_Split_1.png/revision/latest?cb=20250125192106",
                position = PlayerPosition.TOP,
                teamId = "furia"
            ),
            Player(
                id = "player_furia_tatu",
                name = "Pedro Seixas",
                nickname = "Tatu",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/3/3d/FURIA_Tatu_LTA_2025_Split_1.png/revision/latest?cb=20250125192158",
                position = PlayerPosition.JUNGLE,
                teamId = "furia"
            ),
            Player(
                id = "player_furia_tutsz",
                name = "Arthur Peixoto Machado",
                nickname = "Tutsz",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/5/50/FURIA_Tutsz_LTA_2025_Split_1.png/revision/latest?cb=20250125192110",
                position = PlayerPosition.MID,
                teamId = "furia"
            ),
            Player(
                id = "player_furia_ayu",
                name = "Andrey Saraiva",
                nickname = "Ayu",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/7/73/FURIA_Ayu_LTA_2025_Split_1.png/revision/latest?cb=20250125192126",
                position = PlayerPosition.ADC,
                teamId = "furia"
            ),
            Player(
                id = "player_furia_jojo",
                name = "Gabriel Dzelme de Oliveira",
                nickname = "JoJo",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/1/1a/FURIA_JoJo_LTA_2025_Split_1.png/revision/latest?cb=20250125192131",
                position = PlayerPosition.SUPPORT,
                teamId = "furia"
            ),

            // Vivo Keyd Stars
            Player(
                id = "player_keyd_boal",
                name = "Felipe Boal",
                nickname = "Boal",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/d/d9/VKS_Boal_LTA_2025_Split_1.png/revision/latest?cb=20250125191644",
                position = PlayerPosition.TOP,
                teamId = "keyd"
            ),
            Player(
                id = "player_keyd_disamis",
                name = "Pedro Arthur Gonçalves",
                nickname = "Disamis",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/a/aa/VKS_Disamis_LTA_2025_Split_1.png/revision/latest?cb=20250125194439",
                position = PlayerPosition.JUNGLE,
                teamId = "keyd"
            ),
            Player(
                id = "player_keyd_kisee",
                name = "Ronald Van Bao Vo",
                nickname = "Kisee",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/9/96/VKS_Kisee_LTA_2025_Split_1.png/revision/latest?cb=20250125191719",
                position = PlayerPosition.MID,
                teamId = "keyd"
            ),
            Player(
                id = "player_keyd_morttheus",
                name = "Matheus Motta",
                nickname = "Morttheus",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/3/36/VKS_Morttheus_LTA_2025_Split_1.png/revision/latest?cb=20250125191710",
                position = PlayerPosition.ADC,
                teamId = "keyd"
            ),
            Player(
                id = "player_keyd_Trymbi",
                name = "Adrian Trybus",
                nickname = "Trymbi",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/4/42/VKS_Trymbi_LTA_2025_Split_1.png/revision/latest?cb=20250125191229",
                position = PlayerPosition.SUPPORT,
                teamId = "keyd"
            ),

            // Red Cannids
            Player(
                id = "player_red_fnb",
                name = "Francisco Natanael Braz do Espirito Santo Miranda",
                nickname = "fNb",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/6/6a/RED_fNb_LTA_2025_Split_1.png/revision/latest?cb=20250125194031",
                position = PlayerPosition.TOP,
                teamId = "red"
            ),
            Player(
                id = "player_red_aegis",
                name = "Gabriel Vinicius Saes de Lemos",
                nickname = "Aegis",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/8/85/RED_Aegis_2025_Split_2.png/revision/latest?cb=20250408221920",
                position = PlayerPosition.JUNGLE,
                teamId = "red"
            ),
            Player(
                id = "player_red_doom",
                name = "Raí Yamada",
                nickname = "DOOM",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/2/20/RED_DOOM_2025_Split_1.png/revision/latest/?cb=20250319183419",
                position = PlayerPosition.JUNGLE,
                teamId = "red"
            ),
            Player(
                id = "player_red_mago",
                name = "Jean Carl Dias",
                nickname = "Mago",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/2/29/RED_Mago_2025_Split_2.png/revision/latest?cb=20250405203827",
                position = PlayerPosition.MID,
                teamId = "red"
            ),
            Player(
                id = "player_red_grevthar",
                name = "Daniel Xavier Ferreira",
                nickname = "Grevthar",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/9/99/RED_Grevthar_2025_Split_2.png/revision/latest/scale-to-width-down/220?cb=20250319183537",
                position = PlayerPosition.MID,
                teamId = "red"
            ),
            Player(
                id = "player_red_brance",
                name = "Diego Amaral",
                nickname = "Brance",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/8/8c/RED_Brance_LTA_2025_Split_1.png/revision/latest?cb=20250125194043",
                position = PlayerPosition.ADC,
                teamId = "red"
            ),
            Player(
                id = "player_red_frosty",
                name = "José Eduardo Leal Pacheco",
                nickname = "frosty",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/6/64/RED_Frosty_LTA_2025_Split_1.png/revision/latest?cb=20250125194048",
                position = PlayerPosition.SUPPORT,
                teamId = "red"
            ),

            // Fluxo W7M
            Player(
                id = "player_fxw7_hidan",
                name = "Leonardo Borré dos Santos",
                nickname = "Hidan",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/4/4d/FX7M_Hidan_LTA_2025_Split_1.png/revision/latest?cb=20250219022942",
                position = PlayerPosition.TOP,
                teamId = "fxw7"
            ),
            Player(
                id = "player_fxw7_ganks",
                name = "Franco Sánchez Juaregui",
                nickname = "Ganks",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/4/4d/FX7M_Ganks_2025_Split_2.png/revision/latest?cb=20250405203950",
                position = PlayerPosition.JUNGLE,
                teamId = "fxw7"
            ),
            Player(
                id = "player_fxw7_fuuu",
                name = "Gabriel Furuuti Toshio",
                nickname = "Fuuu",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/1/14/FX7M_Fuuu_LTA_2025_Split_1.png/revision/latest?cb=20250219023014",
                position = PlayerPosition.MID,
                teamId = "fxw7"
            ),
            Player(
                id = "player_fxw7_marvin",
                name = "Vinicius de Souza",
                nickname = "Marvin",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/a/a6/FX7M_Marvin_LTA_2025_Split_1.png/revision/latest?cb=20250219022813",
                position = PlayerPosition.ADC,
                teamId = "fxw7"
            ),
            Player(
                id = "player_fxw7_guigs",
                name = "Guilherme Soares",
                nickname = "Guigs",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/8/8b/FX7M_Guigs_LTA_2025_Split_1.png/revision/latest?cb=20250219022740",
                position = PlayerPosition.SUPPORT,
                teamId = "fxw7"
            ),

            // LTA Norte
            // Cloud9
            Player(
                id = "player_cloud9_thanatos",
                name = "Park Seung-gyu",
                nickname = "Thanatos",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/8/8d/C9_Thanatos_2025_Split_1.png/revision/latest?cb=20250121144454",
                position = PlayerPosition.TOP,
                teamId = "cloud9-kia"
            ),
            Player(
                id = "player_cloud9_blaber",
                name = "Robert Huang",
                nickname = "Blaber",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/d/de/C9_Blaber_2025_Split_1.png/revision/latest?cb=20250121144455",
                position = PlayerPosition.JUNGLE,
                teamId = "cloud9-kia"
            ),
            Player(
                id = "player_cloud9_loki",
                name = "Lee Sang-min",
                nickname = "Loki",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/9/98/C9_Loki_2025_Split_1.png/revision/latest?cb=20250121144457",
                position = PlayerPosition.MID,
                teamId = "cloud9-kia"
            ),
            Player(
                id = "player_cloud9_zven",
                name = "Jesper Svenningsen",
                nickname = "Zven",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/8/86/C9_Zven_2025_Split_1.png/revision/latest/scale-to-width-down/220?cb=20250121144458",
                position = PlayerPosition.ADC,
                teamId = "cloud9-kia"
            ),
            Player(
                id = "player_cloud9_vulcan",
                name = "Philippe Laflamme",
                nickname = "Vulcan",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/d/dc/C9_Vulcan_2025_Split_1.png/revision/latest?cb=20250121144459",
                position = PlayerPosition.SUPPORT,
                teamId = "cloud9-kia"
            ),

            //100 Thieves
            Player(
                id = "player_100thieves_sniper",
                name = "Rayan Shoura",
                nickname = "Sniper",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/f/fc/100_Sniper_2024_Split_2.png/revision/latest/scale-to-width-down/220?cb=20240923220459",
                position = PlayerPosition.TOP,
                teamId = "100-thieves"
            ),
            Player(
                id = "player_100thieves_river",
                name = "Kim Dong-woo",
                nickname = "River",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/f/f0/100_River_2024_Split_2.png/revision/latest?cb=20240923220400",
                position = PlayerPosition.JUNGLE,
                teamId = "100-thieves"
            ),
            Player(
                id = "player_100thieves_quid",
                name = "Lim Hyeon-seung",
                nickname = "Quid",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/2/20/100_Quid_2024_Split_2.png/revision/latest/scale-to-width-down/220?cb=20240923220321",
                position = PlayerPosition.MID,
                teamId = "100-thieves"
            ),
            Player(
                id = "player_100thieves_fbi",
                name = "Ian Victor Huang",
                nickname = "FBI",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/9/9e/NRG_FBI_2023_Split_2.png/revision/latest/scale-to-width-down/220?cb=20230624094632",
                position = PlayerPosition.ADC,
                teamId = "100-thieves"
            ),
            Player(
                id = "player_100thieves_eyla",
                name = "Bill Nguyen",
                nickname = "Eyla",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/2/2b/100_Eyla_2024_Split_2.png/revision/latest?cb=20240923220426",
                position = PlayerPosition.SUPPORT,
                teamId = "100-thieves"
            ),

            //Fly Quest
            Player(
                id = "player_flyquest_bwipo",
                name = "Gabriël Rau",
                nickname = "Bwipo",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/0/05/FLY_Bwipo_2024_Split_2.png/revision/latest?cb=20240923220159",
                position = PlayerPosition.TOP,
                teamId = "flyquest"
            ),
            Player(
                id = "player_flyquest_inspired",
                name = "Kacper Słoma",
                nickname = "Inspired",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/c/ca/FLY_Inspired_2024_Split_2.png/revision/latest?cb=20240923220235",
                position = PlayerPosition.JUNGLE,
                teamId = "flyquest"
            ),
            Player(
                id = "player_flyquest_quad",
                name = "Song Su-hyeong",
                nickname = "Quad",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/1/1c/FLY_Quad_2024_Split_2.png/revision/latest?cb=20240923220132",
                position = PlayerPosition.MID,
                teamId = "flyquest"
            ),
            Player(
                id = "player_flyquest_massu",
                name = "Fahad Abdulmalek",
                nickname = "Massu",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/0/0b/FLY_Massu_2024_Split_2.png/revision/latest?cb=20240923220111",
                position = PlayerPosition.ADC,
                teamId = "flyquest"
            ),
            Player(
                id = "player_flyquest_busio",
                name = "Alan Cwalina",
                nickname = "Busio",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/3/3c/FLY_Busio_2024_Split_2.png/revision/latest?cb=20240923220252",
                position = PlayerPosition.SUPPORT,
                teamId = "flyquest"
            ),

            //Team Liquid
            Player(
                id = "player_teamliquid_impact",
                name = "Jeong Eon-young",
                nickname = "Impact",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/0/0b/TL_Impact_2025_Split_1.png/revision/latest?cb=20250311032414",
                position = PlayerPosition.TOP,
                teamId = "team-liquid-honda"
            ),
            Player(
                id = "player_teamliquid_umti",
                name = "Um Sung-hyeon",
                nickname = "UmTi",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/e/e6/TL_UmTi_2025_Split_1.png/revision/latest?cb=20250311032359",
                position = PlayerPosition.JUNGLE,
                teamId = "team-liquid-honda"
            ),
            Player(
                id = "player_teamliquid_apa",
                name = "Eain Stearns",
                nickname = "APA",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/1/11/TL_APA_2025_Split_1.png/revision/latest/scale-to-width-down/220?cb=20250311032347",
                position = PlayerPosition.MID,
                teamId = "team-liquid-honda"
            ),
            Player(
                id = "player_teamliquid_yeon",
                name = "Sean Sung",
                nickname = "Yeon",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/4/48/TL_Yeon_2025_Split_1.png/revision/latest/scale-to-width-down/220?cb=20250311032323",
                position = PlayerPosition.ADC,
                teamId = "team-liquid-honda"
            ),
            Player(
                id = "player_teamliquid_corejj",
                name = "Jo Yong-in",
                nickname = "CoreJJ",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/1/10/TL_CoreJJ_2025_Split_1.png/revision/latest?cb=20250311032305",
                position = PlayerPosition.SUPPORT,
                teamId = "team-liquid-honda"
            ),

            //Shopify Rebellion
            Player(
                id = "player_shopify_fudge",
                name = "Ibrahim Allami",
                nickname = "Fudge",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/c/c2/SR_Fudge_2025_Split_1.png/revision/latest?cb=20250221144256",
                position = PlayerPosition.TOP,
                teamId = "shopify-rebellion"
            ),
            Player(
                id = "player_shopify_contractz",
                name = "Juan Arturo Garcia",
                nickname = "Contractz",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/b/b2/SR_Contractz_2025_Split_1.png/revision/latest/scale-to-width-down/220?cb=20250221144259",
                position = PlayerPosition.JUNGLE,
                teamId = "shopify-rebellion"
            ),
            Player(
                id = "player_shopify_palafox",
                name = "Cristian Palafox",
                nickname = "Palafox",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/d/de/SR_Palafox_2025_Split_1.png/revision/latest?cb=20250221144300",
                position = PlayerPosition.TOP,
                teamId = "shopify-rebellion"
            ),
            Player(
                id = "player_shopify_bvoy",
                name = "Ju Yeong-hoon",
                nickname = "Bvoy",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/b/bd/SR_Bvoy_2025_Split_1.png/revision/latest/scale-to-width-down/220?cb=20250221144255",
                position = PlayerPosition.ADC,
                teamId = "shopify-rebellion"
            ),
            Player(
                id = "player_shopify_ceos",
                name = "Denilson Oliveira Gonçalves",
                nickname = "Ceos",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/2/26/SR_Ceos_2025_Split_1.png/revision/latest?cb=20250221144258",
                position = PlayerPosition.SUPPORT,
                teamId = "shopify-rebellion"
            ),

            //Dignitas
            Player(
                id = "player_dignitas_srtty",
                name = "Jett Michael Joye",
                nickname = "Srtty",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/9/91/DIG_Srtty_2025_Split_1.png/revision/latest/scale-to-width-down/220?cb=20250221144311",
                position = PlayerPosition.TOP,
                teamId = "dignitas"
            ),
            Player(
                id = "player_dignitas_sheiden",
                name = "Jade Libut",
                nickname = "Sheiden",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/8/88/EG_Sheiden_2023_Split_2.png/revision/latest/scale-to-width-down/220?cb=20230729193651",
                position = PlayerPosition.JUNGLE,
                teamId = "dignitas"
            ),
            Player(
                id = "player_dignitas_keine",
                name = "Kim Joon-cheol",
                nickname = "Keine",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/2/2f/DIG_Keine_2025_Split_1.png/revision/latest/scale-to-width-down/220?cb=20250221144307",
                position = PlayerPosition.MID,
                teamId = "dignitas"
            ),
            Player(
                id = "player_dignitas_tomo",
                name = "Frank Lam",
                nickname = "Tomo",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/6/65/DIG_Tomo_2025_Split_1.png/revision/latest/scale-to-width-down/220?cb=20250221144309",
                position = PlayerPosition.ADC,
                teamId = "dignitas"
            ),
            Player(
                id = "player_dignitas_isles",
                name = "Jonah Rosario",
                nickname = "Isles",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/9/94/DIG_Isles_2025_Split_1.png/revision/latest?cb=20250221144308",
                position = PlayerPosition.SUPPORT,
                teamId = "dignitas"
            ),

            //LYON
            Player(
                id = "player_lyon_licorice",
                name = "Eric Ritchie",
                nickname = "Licorice",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/d/de/LYON_Licorice_2025_Split_1.png/revision/latest/scale-to-width-down/220?cb=20250221144304",
                position = PlayerPosition.TOP,
                teamId = "lyon"
            ),
            Player(
                id = "player_lyon_oddielan",
                name = "Sebastián Alonso Niño Zavaleta",
                nickname = "Oddielan",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/c/cb/LYON_Oddielan_2025_Split_1.png/revision/latest/scale-to-width-down/220?cb=20250221144302",
                position = PlayerPosition.JUNGLE,
                teamId = "lyon"
            ),
            Player(
                id = "player_lyon_saint",
                name = "Kang Sung-in",
                nickname = "Saint",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/0/0c/LYON_Saint_2025_Split_1.png/revision/latest?cb=20250221144306",
                position = PlayerPosition.MID,
                teamId = "lyon"
            ),
            Player(
                id = "player_lyon_hena",
                name = "Park Jeung-hwan",
                nickname = "Hena",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/f/fa/FOX_Hena_2024_Split_2.png/revision/latest/scale-to-width-down/220?cb=20240618144641",
                position = PlayerPosition.ADC,
                teamId = "lyon"
            ),
            Player(
                id = "player_lyon_lyonz",
                name = "Pedro Luis Peralta",
                nickname = "Lyonz",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/5/5b/LYON_Lyonz_2025_Split_1.png/revision/latest/scale-to-width-down/220?cb=20250221144303",
                position = PlayerPosition.SUPPORT,
                teamId = "lyon"
            ),

            //Disguised
            Player(
                id = "player_disguised_castle",
                name = "Cho Hyeon-seong",
                nickname = "Castle",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/0/0b/DSG_Castle_2025_Split_1.png/revision/latest/scale-to-width-down/220?cb=20250221144314",
                position = PlayerPosition.TOP,
                teamId = "disguised"
            ),
            Player(
                id = "player_disguised_exyu",
                name = "Lawrence Lin Xu",
                nickname = "eXyu",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/2/28/DSG_eXyu_2025_Split_1.png/revision/latest/scale-to-width-down/220?cb=20250221144316",
                position = PlayerPosition.JUNGLE,
                teamId = "disguised"
            ),
            Player(
                id = "player_disguised_abbedagge",
                name = "Felix Braun",
                nickname = "Abbedagge",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/4/4c/DSG_Abbedagge_2025_Split_1.png/revision/latest?cb=20250221144317",
                position = PlayerPosition.MID,
                teamId = "disguised"
            ),
            Player(
                id = "player_disguised_scaryjerry",
                name = "Jeremiah Leathe",
                nickname = "ScaryJerry",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/3/30/DSG_ScaryJerry_2025_Split_1.png/revision/latest/scale-to-width-down/220?cb=20250221144315",
                position = PlayerPosition.ADC,
                teamId = "disguised"
            ),
            Player(
                id = "player_disguised_huhi",
                name = "Choi Jae-hyun",
                nickname = "huhi",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/6/63/DSG_huhi_2025_Split_1.png/revision/latest?cb=20250221144319",
                position = PlayerPosition.SUPPORT,
                teamId = "disguised"
            ),

            // Circuito Desafiante
            // Corinthians
            Player(
                id = "player_corinthians_tyrin",
                name = "William Portugal",
                nickname = "tyrin",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/5/5b/SCCP_tyrin_2025_Split_1.png/revision/latest/scale-to-width-down/1024?cb=20250401151747",
                position = PlayerPosition.TOP,
                teamId = "corinthians"
            ),
            Player(
                id = "player_corinthians_sting",
                name = "Luís Gustavo Dirami Martins",
                nickname = "stiNg",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/1/1a/SCCP_stiNg_2025_Split_1.png/revision/latest/scale-to-width-down/220?cb=20250401151633",
                position = PlayerPosition.JUNGLE,
                teamId = "corinthians"
            ),
            Player(
                id = "player_corinthians_xico",
                name = "Francisco Costa",
                nickname = "Xico",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/f/f7/GSNS_Xico_2024_Split_2.png/revision/latest?cb=20240816123903",
                position = PlayerPosition.MID,
                teamId = "corinthians"
            ),
            Player(
                id = "player_corinthians_leleko",
                name = "Leandro Hideki Aihara",
                nickname = "Leleko",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/9/95/SCCP_Leleko_2025_Split_1.png/revision/latest/scale-to-width-down/220?cb=20250401151411",
                position = PlayerPosition.MID,
                teamId = "corinthians"
            ),
            Player(
                id = "player_corinthians_trigo",
                name = "Matheus Trigo Nobrega",
                nickname = "Trigo",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/1/16/SCCP_Trigo_2025_Split_1.png/revision/latest/scale-to-width-down/220?cb=20250401151531",
                position = PlayerPosition.ADC,
                teamId = "corinthians"
            ),
            Player(
                id = "player_corinthians_telas",
                name = "Telas",
                nickname = "Telas",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/a/a9/VKS_Telas_AC_2024.png/revision/latest/scale-to-width-down/1024?cb=20240916201655",
                position = PlayerPosition.SUPPORT,
                teamId = "corinthians"
            ),
            Player(
                id = "player_corinthians_manel",
                name = "Manuel",
                nickname = "Manel",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/3/30/SCCP_Manel_2025_Split_1.png/revision/latest/scale-to-width-down/220?cb=20250401151500",
                position = PlayerPosition.SUPPORT,
                teamId = "corinthians"
            ),

            // Dopamina
            Player(
                id = "player_dopamina_ayel",
                name = "Marcelo Mello Zanini",
                nickname = "Ayel",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/4/45/Ayel_CBOL%C3%83O_2024.jpg/revision/latest/scale-to-width-down/220?cb=20241209004301",
                position = PlayerPosition.TOP,
                teamId = "dopamina"
            ),
            Player(
                id = "player_dopamina_forlin",
                name = "Leonardo Pereira",
                nickname = "Forlin",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/c/c7/FX_Forlin_2024_Split_2.png/revision/latest/scale-to-width-down/1000?cb=20240604202230",
                position = PlayerPosition.TOP,
                teamId = "dopamina"
            ),
            Player(
                id = "player_dopamina_dizin",
                name = "Ronald Silva de Paula",
                nickname = "Dizin",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/a/a7/INTZ_Dizin_2024_Split_2.png/revision/latest?cb=20240604014145",
                position = PlayerPosition.JUNGLE,
                teamId = "dopamina"
            ),
            Player(
                id = "player_dopamina_envy",
                name = "Bruno Farias",
                nickname = "Envy",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/6/6d/DPM_Envy_2025_Split_1.png/revision/latest/scale-to-width-down/1000?cb=20250319170036",
                position = PlayerPosition.MID,
                teamId = "dopamina"
            ),
            Player(
                id = "player_dopamina_kojima",
                name = "Caio Yuiti Kojima",
                nickname = "Kojima",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/d/d9/DPM_Kojima_2025_Split_1.png/revision/latest/scale-to-width-down/1000?cb=20250319165954",
                position = PlayerPosition.ADC,
                teamId = "dopamina"
            ),
            Player(
                id = "player_dopamina_bulecha",
                name = "Lucas Adriel",
                nickname = "Bulas",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/e/ed/LOUD_Bulecha_2024_Split_2.png/revision/latest/scale-to-width-down/220?cb=20240604204635",
                position = PlayerPosition.SUPPORT,
                teamId = "dopamina"
            ),

            // Flamengo
            Player(
                id = "player_flamengo_yupps",
                name = "Yuri Gabriel Petermann",
                nickname = "Yupps",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/e/e1/FLA_Yupps_2025_Split_1.png/revision/latest/scale-to-width-down/220?cb=20250319171008",
                position = PlayerPosition.TOP,
                teamId = "flamengo"
            ),
            Player(
                id = "player_flamengo_yampi",
                name = "Yan Christian Petermann",
                nickname = "Yampi",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/5/56/FLA_Yampi_2025_Split_1.png/revision/latest/scale-to-width-down/220?cb=20250319171113",
                position = PlayerPosition.JUNGLE,
                teamId = "flamengo"
            ),
            Player(
                id = "player_flamengo_piloto",
                name = "Elvis Vergara",
                nickname = "Piloto",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/1/11/FLA_Pilot_2025_Split_1.png/revision/latest/scale-to-width-down/1024?cb=20250319171342",
                position = PlayerPosition.MID,
                teamId = "flamengo"
            ),
            Player(
                id = "player_flamengo_ninjakiwi",
                name = "Yudi Leonardo Miyashiro",
                nickname = "Ninjakiwi",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/5/57/FLA_NinjaKiwi_2025_Split_1.png/revision/latest/scale-to-width-down/220?cb=20250319171505",
                position = PlayerPosition.ADC,
                teamId = "flamengo"
            ),
            Player(
                id = "player_flamengo_momochi",
                name = "Gabriel Sousa",
                nickname = "Momochi",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/5/53/FLA_Momochi_2025_Split_1.png/revision/latest/scale-to-width-down/220?cb=20250319171544",
                position = PlayerPosition.SUPPORT,
                teamId = "flamengo"
            ),

            // Kabum IDL
            Player(
                id = "player_kabum_hirit",
                name = "Shin Tae-min",
                nickname = "HiRit",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/d/db/KBM_IDL_HiRit_2025_Split_1.png/revision/latest/scale-to-width-down/1000?cb=20250319174148",
                position = PlayerPosition.TOP,
                teamId = "kabum-idl"
            ),
            Player(
                id = "player_kabum_seize",
                name = "Kim Chan-hee",
                nickname = "Seize",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/b/b2/KBM_IDL_Seize_2025_Split_1.png/revision/latest/scale-to-width-down/220?cb=20250319174958",
                position = PlayerPosition.JUNGLE,
                teamId = "kabum-idl"
            ),
            Player(
                id = "player_kabum_ranger",
                name = "Filipe Brombilla de Bairros",
                nickname = "Ranger",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/7/7c/KBM_IDL_Ranger_2025_Split_1.png/revision/latest/scale-to-width-down/220?cb=20250319174302",
                position = PlayerPosition.JUNGLE,
                teamId = "kabum-idl"
            ),
            Player(
                id = "player_kabum_wiz",
                name = "Na Yoo-joon",
                nickname = "Wiz",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/5/53/LOUD_Wiz_LTA_2025_Split_1.png/revision/latest/scale-to-width-down/220?cb=20250213192557",
                position = PlayerPosition.JUNGLE,
                teamId = "kabum-idl"
            ),
            Player(
                id = "player_kabum_hauz",
                name = "Bruno Augusto Felberge Ferreira",
                nickname = "Hauz",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/f/f2/KBM_IDL_Hauz_2025_Split_1.png/revision/latest/scale-to-width-down/1000?cb=20250319174010",
                position = PlayerPosition.MID,
                teamId = "kabum-idl"
            ),
            Player(
                id = "player_kabum_scuro",
                name = "Gabriel Scuro",
                nickname = "scuro",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/8/84/KBM_IDL_Scuro_2025_Split_1.png/revision/latest/scale-to-width-down/220?cb=20250319174736",
                position = PlayerPosition.ADC,
                teamId = "kabum-idl"
            ),
            Player(
                id = "player_kabum_damage",
                name = "Yan Sales Neves",
                nickname = "Damage",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/3/3f/KBM_IDL_Damage_2025_Split_1.png/revision/latest/scale-to-width-down/220?cb=20250319174057",
                position = PlayerPosition.SUPPORT,
                teamId = "kabum-idl"
            ),
            Player(
                id = "player_kabum_reaper",
                name = "Matheus Silva Pessoa",
                nickname = "Reaper",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/1/16/KBM_IDL_Reaper_2025_Split_1.png/revision/latest/scale-to-width-down/220?cb=20250319174640",
                position = PlayerPosition.SUPPORT,
                teamId = "kabum-idl"
            ),

            // LOS
            Player(
                id = "player_los_supercleber",
                name = "Cleber Nantes",
                nickname = "SuperCleber",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/1/17/LOS_SuperCleber_2025_Split_1.png/revision/latest/scale-to-width-down/220?cb=20250319180358",
                position = PlayerPosition.TOP,
                teamId = "los"
            ),
            Player(
                id = "player_los_stiner",
                name = "Vinicius Dias",
                nickname = "StineR",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/3/31/LOS_Stiner_2025_Split_1.png/revision/latest/scale-to-width-down/220?cb=20250319180324",
                position = PlayerPosition.JUNGLE,
                teamId = "los"
            ),
            Player(
                id = "player_los_mg",
                name = "Lee Ji-hoon",
                nickname = "M G",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/b/b5/LOS_MG_2025_Split_1.png/revision/latest/scale-to-width-down/220?cb=20250319180057",
                position = PlayerPosition.MID,
                teamId = "los"
            ),
            Player(
                id = "player_los_netuno",
                name = "Lucas Flores Fensterseifer",
                nickname = "Netuno",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/8/82/LOS_Netuno_2025_Split_1.png/revision/latest/scale-to-width-down/220?cb=20250319180242",
                position = PlayerPosition.ADC,
                teamId = "los"
            ),
            Player(
                id = "player_los_sanghyeon",
                name = "Jeong Sang-hyeon",
                nickname = "sanghyeon",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/9/99/LOS_Sanghyeon_2025_Split_1.png/revision/latest/scale-to-width-down/220?cb=20250319180135",
                position = PlayerPosition.SUPPORT,
                teamId = "los"
            ),

            // RATZ
            Player(
                id = "player_ratz_kiari",
                name = "Thiago Luiz Soares",
                nickname = "Kiari",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/d/d4/RATZ_Kiari_2025_Split_1.png/revision/latest/scale-to-width-down/1000?cb=20250319181952",
                position = PlayerPosition.TOP,
                teamId = "ratz"
            ),
            Player(
                id = "player_ratz_soweto",
                name = "Leonardo Alencar",
                nickname = "Soweto",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/d/df/RATZ_Soweto_2025_Split_1.png/revision/latest/scale-to-width-down/1000?cb=20250319181235",
                position = PlayerPosition.JUNGLE,
                teamId = "ratz"
            ),
            Player(
                id = "player_ratz_drakehero",
                name = "Luciano Junior Paes",
                nickname = "Drakehero",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/8/81/RATZ_Drakehero_2025_Split_2.png/revision/latest/scale-to-width-down/1000?cb=20250319181611",
                position = PlayerPosition.JUNGLE,
                teamId = "ratz"
            ),
            Player(
                id = "player_ratz_lynkez",
                name = "Leonardo Cassuci",
                nickname = "Lynkez",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/4/48/RISE_Lynkez_2024_Split_2.png/revision/latest?cb=20240723025120",
                position = PlayerPosition.MID,
                teamId = "ratz"
            ),
            Player(
                id = "player_ratz_buero",
                name = "Daniel Nunes de Jesus",
                nickname = "Buero",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/0/01/RATZ_Buero_2025_Split_1.png/revision/latest/scale-to-width-down/220?cb=20250319181833",
                position = PlayerPosition.ADC,
                teamId = "ratz"
            ),
            Player(
                id = "player_ratz_beenie",
                name = "Pedro Lucas Rodrigues",
                nickname = "Beenie",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/0/0b/LEV_Beenie_2024_Closing.png/revision/latest?cb=20240613052705",
                position = PlayerPosition.ADC,
                teamId = "ratz"
            ),
            Player(
                id = "player_ratz_krastyel",
                name = "Marcos Henrique Ferraz",
                nickname = "Krastyel",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/b/b4/KBM_Krastyel_2024_Split_2.png/revision/latest/scale-to-width-down/220?cb=20240604193952",
                position = PlayerPosition.SUPPORT,
                teamId = "ratz"
            ),

            // RED Academy
            Player(
                id = "player_red_academy_zynts",
                name = "Matheus Emanuel",
                nickname = "zynts",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/7/78/RED_Zynts_2025_Split_1.png/revision/latest/scale-to-width-down/220?cb=20250319184040",
                position = PlayerPosition.TOP,
                teamId = "red-kalunga-academy"
            ),
            Player(
                id = "player_red_academy_doom",
                name = "Raí Yamada",
                nickname = "DOOM",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/2/2b/RED_DOOM_LTA_2025_Split_2.png/revision/latest/scale-to-width-down/220?cb=20250408222002",
                position = PlayerPosition.JUNGLE,
                teamId = "red-kalunga-academy"
            ),
            Player(
                id = "player_red_academy_aegis",
                name = "Gabriel Vinicius Saes de Lemos",
                nickname = "Aegis",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/8/85/RED_Aegis_2025_Split_2.png/revision/latest/scale-to-width-down/220?cb=20250408221920",
                position = PlayerPosition.JUNGLE,
                teamId = "red-kalunga-academy"
            ),
            Player(
                id = "player_red_academy_grevthar",
                name = "Daniel Xavier Ferreira",
                nickname = "Grevthar",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/9/99/RED_Grevthar_2025_Split_2.png/revision/latest/scale-to-width-down/220?cb=20250319183537",
                position = PlayerPosition.MID,
                teamId = "red-kalunga-academy"
            ),
            Player(
                id = "player_red_academy_mago",
                name = "Jean Carl Dias",
                nickname = "Mago",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/2/29/RED_Mago_2025_Split_2.png/revision/latest?cb=20250405203827",
                position = PlayerPosition.MID,
                teamId = "red-kalunga-academy"
            ),
            Player(
                id = "player_red_academy_kaze",
                name = "Lucas Fe",
                nickname = "Kaze",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/b/bb/ISG_Kaze_2024_Closing.png/revision/latest?cb=20240613041201",
                position = PlayerPosition.MID,
                teamId = "red-kalunga-academy"
            ),
            Player(
                id = "player_red_academy_rabelo",
                name = "Guilherme Rabelo Muniz",
                nickname = "Rabelo",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/6/63/RED_Rabelo_2025_Split_1.png/revision/latest/scale-to-width-down/220?cb=20250319183825",
                position = PlayerPosition.ADC,
                teamId = "red-kalunga-academy"
            ),

            Player(
                id = "player_red_academy_uzent",
                name = "Matheus Ferreira",
                nickname = "uZent",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/4/43/RED_uZent_2025_Split_1.png/revision/latest/scale-to-width-down/220?cb=20250319183952",
                position = PlayerPosition.SUPPORT,
                teamId = "red-kalunga-academy"
            ),

            // Rise Gaming
            Player(
                id = "player_rise_makes",
                name = "Gabriel Nemeth Ramos",
                nickname = "Makes",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/f/f2/RISE_Makes_2025_Split_1.png/revision/latest/scale-to-width-down/1000?cb=20250319191431",
                position = PlayerPosition.TOP,
                teamId = "rise"
            ),
            Player(
                id = "player_rise_thayt",
                name = "Erick da Rosa Silva",
                nickname = "ThayT",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/0/08/RISE_ThayT_2025_Split_1.png/revision/latest/scale-to-width-down/220?cb=20250319191511",
                position = PlayerPosition.JUNGLE,
                teamId = "rise"
            ),
            Player(
                id = "player_rise_nero",
                name = "Otávio Augusto Moura Ribeiro",
                nickname = "Nero",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/9/92/LOS_Nero_2024_Split_2.png/revision/latest/scale-to-width-down/220?cb=20240604022705",
                position = PlayerPosition.JUNGLE,
                teamId = "rise"
            ),
            Player(
                id = "player_rise_anyyy",
                name = "Ruan Silva",
                nickname = "Anyyy",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/9/98/LOUD_Anyyy.png/revision/latest?cb=20240409232320",
                position = PlayerPosition.MID,
                teamId = "rise"
            ),
            Player(
                id = "player_rise_aikawa",
                name = "Kalil Hoyer Kayatt",
                nickname = "Aikawa",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/7/7a/FUR_Aikawa_2024_Split_2.png/revision/latest/scale-to-width-down/220?cb=20240723201650",
                position = PlayerPosition.ADC,
                teamId = "rise"
            ),
            Player(
                id = "player_rise_zay",
                name = "Vinicius Argolo Viana",
                nickname = "Zay",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/5/5f/RISE_Zay_2025_Split_1.png/revision/latest/scale-to-width-down/1024?cb=20250319191348",
                position = PlayerPosition.SUPPORT,
                teamId = "rise"
            ),

            // Stellae Gaming
            Player(
                id = "player_stellae_zekas",
                name = "César Berteli França",
                nickname = "ZekaS",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/7/7c/STE_Zekas_2025_Split_1.png/revision/latest/scale-to-width-down/1024?cb=20250319193549",
                position = PlayerPosition.TOP,
                teamId = "stellae"
            ),
            Player(
                id = "player_stellae_mewkyo",
                name = "Mateus Henrique Ferraz Muniz",
                nickname = "Mewkyo",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/0/03/STE_Mewkyo_2025_Split_1.png/revision/latest/scale-to-width-down/220?cb=20250319202228",
                position = PlayerPosition.JUNGLE,
                teamId = "stellae"
            ),
            Player(
                id = "player_stellae_aithusa",
                name = "Lucas Mantese",
                nickname = "Aithusa",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/1/1a/STE_Aithusa_2025_Split_1.png/revision/latest/scale-to-width-down/220?cb=20250319193655",
                position = PlayerPosition.MID,
                teamId = "stellae"
            ),
            Player(
                id = "player_stellae_celo",
                name = "Marcelo Leite",
                nickname = "Celo",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/8/8a/STE_Celo_2025_Split_1.png/revision/latest/scale-to-width-down/220?cb=20250319193837",
                position = PlayerPosition.ADC,
                teamId = "stellae"
            ),
            Player(
                id = "player_stellae_cavalo",
                name = "Alexandre Fernandes",
                nickname = "Cavalo",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/6/65/STE_Cavalo_2025_Split_1.png/revision/latest/scale-to-width-down/220?cb=20250319193759",
                position = PlayerPosition.SUPPORT,
                teamId = "stellae"
            ),

            // Vivo Keyd Stars Academy
            Player(
                id = "player_keyd_academy_xyno",
                name = "Carlos Felipe Ferreira",
                nickname = "xyno",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/f/f4/VKS_Xyno_2025_Split_1.png/revision/latest/scale-to-width-down/220?cb=20250319200722",
                position = PlayerPosition.TOP,
                teamId = "keyd-academy"
            ),
            Player(
                id = "player_keyd_academy_sarolu",
                name = "Victor Satoru Noguchi",
                nickname = "sarolu",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/3/35/VKS_Sarolu_2025_Split_1.png/revision/latest/scale-to-width-down/1024?cb=20250319200118",
                position = PlayerPosition.JUNGLE,
                teamId = "keyd-academy"
            ),
            Player(
                id = "player_keyd_academy_qats",
                name = "Thiago Augusto Fernandes Barros de Freitas",
                nickname = "Qats",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/c/c2/VKS_Qats_2025_Split_1.png/revision/latest?cb=20250319200040",
                position = PlayerPosition.MID,
                teamId = "keyd-academy"
            ),
            Player(
                id = "player_keyd_academy_smiley",
                name = "Ludvig Erik Hugo Granquist",
                nickname = "Smiley",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/d/dd/VKS_Smiley_2025_Split_1.png/revision/latest/scale-to-width-down/220?cb=20250319200218",
                position = PlayerPosition.ADC,
                teamId = "keyd-academy"
            ),
            Player(
                id = "player_keyd_academy_scamber",
                name = "Pedro Lemos Soares Maximiniano",
                nickname = "scamber",
                imageUrl = "https://static.wikia.nocookie.net/lolesports_gamepedia_en/images/1/14/VKS_Scamber_2025_Split_1.png/revision/latest/scale-to-width-down/220?cb=20250319200604",
                position = PlayerPosition.SUPPORT,
                teamId = "keyd-academy"
            )
        )
    }
}