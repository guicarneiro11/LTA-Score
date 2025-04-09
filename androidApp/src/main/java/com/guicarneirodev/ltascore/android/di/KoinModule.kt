package com.guicarneirodev.ltascore.android.di

import com.guicarneirodev.ltascore.android.viewmodels.MatchesViewModel
import com.guicarneirodev.ltascore.android.viewmodels.VotingViewModel
import com.guicarneirodev.ltascore.api.LoLEsportsApi
import com.guicarneirodev.ltascore.data.datasource.local.MatchLocalDataSource
import com.guicarneirodev.ltascore.data.datasource.static.PlayersStaticDataSource
import com.guicarneirodev.ltascore.data.repository.MatchRepositoryImpl
import com.guicarneirodev.ltascore.data.repository.VoteRepositoryImpl
import com.guicarneirodev.ltascore.domain.repository.MatchRepository
import com.guicarneirodev.ltascore.domain.repository.VoteRepository
import com.guicarneirodev.ltascore.domain.usecases.GetCompletedMatchesUseCase
import com.guicarneirodev.ltascore.domain.usecases.GetMatchByIdUseCase
import com.guicarneirodev.ltascore.domain.usecases.GetMatchesUseCase
import com.guicarneirodev.ltascore.domain.usecases.SubmitPlayerVoteUseCase
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    // API
    single { LoLEsportsApi() }

    // DataSources
    single { PlayersStaticDataSource() }
    single { MatchLocalDataSource() }

    // Repositories
    single<MatchRepository> { MatchRepositoryImpl(get(), get(), get()) }
    single<VoteRepository> { VoteRepositoryImpl() }

    // Use Cases
    single { GetMatchesUseCase(get()) }
    single { GetCompletedMatchesUseCase(get()) }
    single { GetMatchByIdUseCase(get()) }
    single { SubmitPlayerVoteUseCase(get()) }

    // ViewModels
    viewModel { MatchesViewModel(get()) }
    viewModel { VotingViewModel(get(), get()) }
}