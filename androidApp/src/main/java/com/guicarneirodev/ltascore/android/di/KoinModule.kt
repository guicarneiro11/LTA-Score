package com.guicarneirodev.ltascore.android.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.guicarneirodev.ltascore.android.data.repository.FirebaseUserRepository
import com.guicarneirodev.ltascore.android.data.repository.FirebaseVoteRepository
import com.guicarneirodev.ltascore.android.data.repository.UserPreferencesRepository
import com.guicarneirodev.ltascore.android.viewmodels.AuthViewModel
import com.guicarneirodev.ltascore.android.viewmodels.MatchSummaryViewModel
import com.guicarneirodev.ltascore.android.viewmodels.MatchesViewModel
import com.guicarneirodev.ltascore.android.viewmodels.VotingViewModel
import com.guicarneirodev.ltascore.api.LoLEsportsApi
import com.guicarneirodev.ltascore.data.datasource.local.MatchLocalDataSource
import com.guicarneirodev.ltascore.data.datasource.static.PlayersStaticDataSource
import com.guicarneirodev.ltascore.data.repository.MatchRepositoryImpl
import com.guicarneirodev.ltascore.domain.repository.MatchRepository
import com.guicarneirodev.ltascore.domain.repository.UserRepository
import com.guicarneirodev.ltascore.domain.repository.VoteRepository
import com.guicarneirodev.ltascore.domain.usecases.GetCompletedMatchesUseCase
import com.guicarneirodev.ltascore.domain.usecases.GetMatchByIdUseCase
import com.guicarneirodev.ltascore.domain.usecases.GetMatchesUseCase
import com.guicarneirodev.ltascore.domain.usecases.SubmitPlayerVoteUseCase
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    // Firebase
    single { FirebaseAuth.getInstance() }
    single { FirebaseFirestore.getInstance() }

    // API
    single { LoLEsportsApi() }

    // DataSources
    single { PlayersStaticDataSource() }
    single { MatchLocalDataSource() }

    // Repositories
    single<MatchRepository> { MatchRepositoryImpl(get(), get(), get()) }
    single<UserRepository> { FirebaseUserRepository(get(), get()) }
    single<VoteRepository> { FirebaseVoteRepository(get()) }

    // DataStore Repository
    single { UserPreferencesRepository(androidContext()) }

    // Use Cases
    single { GetMatchesUseCase(get()) }
    single { GetCompletedMatchesUseCase(get()) }
    single { GetMatchByIdUseCase(get()) }
    single { SubmitPlayerVoteUseCase(get()) }

    // ViewModels
    viewModel { MatchesViewModel(get()) }
    viewModel { VotingViewModel(get(), get(), get(), get()) } // Adicionamos UserPreferencesRepository
    viewModel { MatchSummaryViewModel(get(), get(), get(), get()) } // Adicionamos UserPreferencesRepository
    viewModel { AuthViewModel(get()) }
}