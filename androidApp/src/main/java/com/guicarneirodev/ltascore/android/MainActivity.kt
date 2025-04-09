package com.guicarneirodev.ltascore.android

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.guicarneirodev.ltascore.api.LoLEsportsApi
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    // Injetar a API diretamente para teste
    private val api: LoLEsportsApi by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Chame este método para testar a API diretamente
        testApiDirectly()

        setContent {
            MyApplicationTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainNavigation()
                }
            }
        }
    }

    private fun testApiDirectly() {
        lifecycleScope.launch {
            try {
                // Tente fazer uma chamada direta para a API
                val response = api.getSchedule("lta_s")

                // Se chegar aqui, a chamada da API foi bem-sucedida
                Toast.makeText(
                    this@MainActivity,
                    "API chamada com sucesso!",
                    Toast.LENGTH_LONG
                ).show()

                // Log dos resultados
                val eventCount = response.data?.schedule?.events?.size ?: 0
                println("API Test: Recebidos $eventCount eventos")

            } catch (e: Exception) {
                // Se ocorrer um erro, exiba-o
                Toast.makeText(
                    this@MainActivity,
                    "Erro na API: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()

                println("API Test Error: ${e.message}")
                e.printStackTrace()
            }
        }
    }
}