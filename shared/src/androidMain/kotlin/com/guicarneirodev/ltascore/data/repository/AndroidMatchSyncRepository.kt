package com.guicarneirodev.ltascore.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.guicarneirodev.ltascore.domain.repository.MatchRepository
import com.guicarneirodev.ltascore.domain.repository.MatchSyncRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await

class AndroidMatchSyncRepository(
    private val matchRepository: MatchRepository,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : MatchSyncRepository {

    override suspend fun syncMatchesToFirestore() {
        val leagues = listOf("lta_s", "lta_n", "cd")

        for (leagueSlug in leagues) {
            try {
                val matches = matchRepository.getMatches(leagueSlug).first()

                for (match in matches) {
                    val matchData = mapOf(
                        "id" to match.id,
                        "startTime" to match.startTime.toString(),
                        "state" to match.state.name,
                        "blockName" to match.blockName,
                        "leagueName" to match.leagueName,
                        "leagueSlug" to match.leagueSlug,
                        "bestOf" to match.bestOf,
                        "hasVod" to match.hasVod,
                        "vodUrl" to match.vodUrl,
                        "teams" to match.teams.map { team ->
                            mapOf(
                                "id" to team.id,
                                "name" to team.name,
                                "code" to team.code,
                                "imageUrl" to team.imageUrl,
                                "result" to mapOf(
                                    "outcome" to team.result.outcome?.name,
                                    "gameWins" to team.result.gameWins,
                                    "wins" to team.result.wins,
                                    "losses" to team.result.losses
                                )
                            )
                        }
                    )

                    firestore
                        .collection("matches")
                        .document(match.id)
                        .set(matchData)
                        .await()
                }

                println("Synced matches for league: $leagueSlug")
            } catch (e: Exception) {
                println("Failed to sync matches for league $leagueSlug: ${e.message}")
            }
        }
    }
}