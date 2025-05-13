package com.guicarneirodev.ltascore.android.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.guicarneirodev.ltascore.domain.repository.AdminRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await

class FirebaseAdminRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : AdminRepository {
    override fun isUserAdmin(userId: String): Flow<Boolean> = flow {
        try {
            android.util.Log.d("AdminRepository", "Verificando se usuário $userId é administrador")

            val adminDoc = firestore.collection("admins")
                .document(userId)
                .get()
                .await()

            val isAdmin = adminDoc.exists()
            android.util.Log.d("AdminRepository", "Resultado para $userId: documento existe: $isAdmin")

            emit(isAdmin)
        } catch (e: Exception) {
            android.util.Log.e("AdminRepository", "Erro ao verificar administrador: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }.catch { exception ->
        android.util.Log.e("AdminRepository", "Tratando exceção no flow: ${exception.message}")
        emit(false)
    }
}
