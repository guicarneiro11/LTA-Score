package com.guicarneirodev.ltascore.util

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.guicarneirodev.ltascore.domain.models.*

object DataExporter {
    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun exportMatchesToJson(matches: List<Match>): String {
        return json.encodeToString(matches)
    }

    fun exportTeamsToJson(teams: List<Team>): String {
        return json.encodeToString(teams)
    }

    fun exportPlayersToJson(players: List<Player>): String {
        return json.encodeToString(players)
    }
}