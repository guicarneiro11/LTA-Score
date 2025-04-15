package com.guicarneirodev.ltascore.data.datasource.static

import com.guicarneirodev.ltascore.domain.models.Player
import com.guicarneirodev.ltascore.domain.models.PlayerPosition
import kotlinx.datetime.Instant

class PlayersStaticDataSource {

    // Cache de jogadores por ID do time
    private val playersByTeamId: Map<String, List<Player>> by lazy {
        allPlayers.groupBy { it.teamId }
    }

    fun getAllPlayers(): List<Player> = allPlayers

    fun getPlayersByTeamId(teamId: String): List<Player> {
        return playersByTeamId[teamId] ?: emptyList()
    }

    fun getPlayerById(playerId: String): Player? {
        return allPlayers.find { it.id == playerId }
    }

    fun getPlayersByTeamIdAndDate(teamId: String, matchDate: Instant, blockName: String): List<Player> {
        // Obter todos os jogadores do time
        val allTeamPlayers = playersByTeamId[teamId] ?: emptyList()

        // Caso especial para Isurus Estral - troca entre Burdol e Summit
        if (teamId == "isurus-estral") {
            // Verificar pelo nome do bloco se é Semana 1
            val isWeek1 = blockName.contains("Semana 1", ignoreCase = true) ||
                    blockName.contains("Week 1", ignoreCase = true)

            // Verificar se é Semana 2 ou posterior
            val isWeek2OrLater = blockName.contains("Semana 2", ignoreCase = true) ||
                    blockName.contains("Week 2", ignoreCase = true) ||
                    blockName.contains("Semana 3", ignoreCase = true) ||
                    blockName.contains("Week 3", ignoreCase = true) ||
                    blockName.contains("Semana 4", ignoreCase = true) ||
                    blockName.contains("Week 4", ignoreCase = true)

            // Data de corte estimada para a Semana 2
            val week2StartDate = Instant.parse("2025-04-08T00:00:00Z")

            // Determinar se estamos na Semana 1 ou posterior
            val isSemana1 = isWeek1 || (!isWeek2OrLater && matchDate < week2StartDate)

            println("Partida de ${matchDate}, blockName: $blockName, isSemana1: $isSemana1")

            return if (isSemana1) {
                // Semana 1: incluir Burdol, excluir Summit
                println("Retornando time IE com Burdol (Semana 1)")
                allTeamPlayers.filter { it.id != "player_ie_summit" }
            } else {
                // Semana 2 ou posterior: incluir Summit, excluir Burdol
                println("Retornando time IE com Summit (Semana 2+)")
                allTeamPlayers.filter { it.id != "player_ie_burdol" }
            }
        }

        // Para outros times, retorna todos os jogadores normalmente
        return allTeamPlayers
    }

    companion object {
        /**
         * Lista de todos os jogadores da LTA Sul e LTA Norte
         * IDs são gerados para garantir unicidade
         */
        private val allPlayers = listOf(
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

            // E depois os da LTA Norte
            // ...
        )
    }
}