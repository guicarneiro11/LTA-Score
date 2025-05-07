package com.guicarneirodev.ltascore.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.guicarneirodev.ltascore.api.LoLEsportsApi
import com.guicarneirodev.ltascore.domain.models.MatchState
import com.guicarneirodev.ltascore.domain.repository.MatchRepository
import com.guicarneirodev.ltascore.domain.repository.MatchSyncRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.minus

class AndroidMatchSyncRepository(
    private val matchRepository: MatchRepository,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val api: LoLEsportsApi = LoLEsportsApi()
) : MatchSyncRepository {

    override suspend fun syncMatchesToFirestore() {
        val leagues = listOf("lta_s", "lta_n", "cd")

        for (leagueSlug in leagues) {
            try {
                val matches = matchRepository.getMatches(leagueSlug).first()

                val liveMatches = matches.filter { it.state == MatchState.INPROGRESS }
                if (liveMatches.isNotEmpty()) {
                    Log.d("MatchSync", "Encontradas ${liveMatches.size} partidas em andamento na liga $leagueSlug")

                    for (liveMatch in liveMatches) {
                        try {
                            updateLiveMatch(liveMatch.id)
                        } catch (e: Exception) {
                            Log.e("MatchSync", "Erro ao atualizar partida em andamento ${liveMatch.id}: ${e.message}")
                        }
                    }
                }

                for (match in matches) {
                    val existingDoc = try {
                        firestore.collection("matches")
                            .document(match.id)
                            .get()
                            .await()
                    } catch (_: Exception) {
                        null
                    }

                    val currentState = existingDoc?.getString("state")
                    val newState = match.state.name

                    if (currentState != null && currentState != newState) {
                        Log.d("MatchSync", "Atualizando estado da partida ${match.id}: $currentState -> $newState")
                    }

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

                Log.d("MatchSync", "Sincronizadas partidas para liga: $leagueSlug")
            } catch (e: Exception) {
                Log.e("MatchSync", "Falha ao sincronizar partidas para liga $leagueSlug: ${e.message}")
            }
        }
    }

    override suspend fun syncLiveMatches() {
        try {
            val liveMatchesSnapshot = firestore.collection("matches")
                .whereEqualTo("state", "INPROGRESS")
                .get()
                .await()

            if (liveMatchesSnapshot.isEmpty) {
                Log.d("MatchSync", "Nenhuma partida em andamento encontrada")
                return
            }

            Log.d("MatchSync", "Encontradas ${liveMatchesSnapshot.size()} partidas em andamento para atualização")

            for (doc in liveMatchesSnapshot.documents) {
                try {
                    updateLiveMatch(doc.id)
                } catch (e: Exception) {
                    Log.e("MatchSync", "Erro ao atualizar partida em andamento ${doc.id}: ${e.message}")
                }
            }

            val today = Clock.System.now()
            val recentMatchesSnapshot = firestore.collection("matches")
                .whereEqualTo("state", "UNSTARTED")
                .get()
                .await()

            val recentMatches = recentMatchesSnapshot.documents.filter {
                try {
                    val startTimeStr = it.getString("startTime") ?: return@filter false
                    val startTime = kotlinx.datetime.Instant.parse(startTimeStr)

                    startTime <= today && startTime >= today.minus(1, DateTimeUnit.HOUR)
                } catch (_: Exception) {
                    false
                }
            }

            if (recentMatches.isNotEmpty()) {
                Log.d("MatchSync", "Verificando ${recentMatches.size} partidas recentes que podem ter começado")

                for (doc in recentMatches) {
                    try {
                        updateLiveMatch(doc.id)
                    } catch (e: Exception) {
                        Log.e("MatchSync", "Erro ao verificar partida recente ${doc.id}: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("MatchSync", "Erro na sincronização de partidas em andamento: ${e.message}")
        }
    }

    private suspend fun updateLiveMatch(matchId: String) {
        Log.d("MatchSync", "Obtendo dados atualizados para partida: $matchId")

        val matchResponse = api.getMatch(matchId)
        val eventDetail = matchResponse.data?.event

        if (eventDetail != null) {
            val matchState = determineMatchState(eventDetail.match.games)
            val firebaseState = mapApiStateToFirebaseState(matchState)

            val currentDoc = firestore.collection("matches")
                .document(matchId)
                .get()
                .await()

            val currentState = currentDoc.getString("state")

            if (currentState != firebaseState) {
                Log.d("MatchSync", "Atualizando estado da partida $matchId: $currentState -> $firebaseState")

                firestore.collection("matches")
                    .document(matchId)
                    .update("state", firebaseState)
                    .await()

                if (firebaseState == "COMPLETED" && eventDetail.match.teams.isNotEmpty()) {
                    updateMatchResults(matchId, eventDetail.match.teams)
                }
            } else {
                Log.d("MatchSync", "Partida $matchId continua no mesmo estado: $currentState")
            }
        } else {
            Log.d("MatchSync", "Sem dados atualizados para partida $matchId")
        }
    }

    private fun determineMatchState(games: List<com.guicarneirodev.ltascore.api.models.GameDTO>): String {
        if (games.isEmpty()) return "unstarted"

        if (games.any { it.state.lowercase() == "inprogress" }) {
            return "inprogress"
        }

        if (games.all { it.state.lowercase() == "completed" }) {
            return "completed"
        }

        return "unstarted"
    }

    private suspend fun updateMatchResults(matchId: String, teams: List<com.guicarneirodev.ltascore.api.models.TeamDTO>) {
        try {
            val matchDoc = firestore.collection("matches")
                .document(matchId)
                .get()
                .await()

            if (!matchDoc.exists()) {
                Log.e("MatchSync", "Partida $matchId não encontrada para atualizar resultados")
                return
            }

            @Suppress("UNCHECKED_CAST")
            val currentTeams = matchDoc.get("teams") as? List<Map<String, Any>> ?: return

            val updatedTeams = currentTeams.map { currentTeam ->
                val teamId = currentTeam["id"] as? String ?: ""
                val apiTeam = teams.find { it.id == teamId || it.code == currentTeam["code"] }

                if (apiTeam != null) {
                    val updatedResult = mapOf(
                        "outcome" to apiTeam.result.outcome,
                        "gameWins" to apiTeam.result.gameWins,
                        "wins" to (apiTeam.record?.wins ?: 0),
                        "losses" to (apiTeam.record?.losses ?: 0)
                    )

                    currentTeam + ("result" to updatedResult)
                } else {
                    currentTeam
                }
            }

            firestore.collection("matches")
                .document(matchId)
                .update("teams", updatedTeams)
                .await()

            Log.d("MatchSync", "Resultados da partida $matchId atualizados com sucesso")
        } catch (e: Exception) {
            Log.e("MatchSync", "Erro ao atualizar resultados da partida $matchId: ${e.message}")
        }
    }

    private fun mapApiStateToFirebaseState(apiState: String): String {
        return when (apiState.lowercase()) {
            "unstarted" -> "UNSTARTED"
            "inprogress" -> "INPROGRESS"
            "completed" -> "COMPLETED"
            else -> "UNSTARTED"
        }
    }

    override suspend fun forceUpdateMatchState(matchId: String, newState: String) {
        try {
            firestore.collection("matches")
                .document(matchId)
                .update("state", newState)
                .await()

            Log.d("MatchSync", "Estado da partida $matchId atualizado manualmente para $newState")
        } catch (e: Exception) {
            Log.e("MatchSync", "Erro ao atualizar estado: ${e.message}")
        }
    }
}