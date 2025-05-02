@file:Suppress("DEPRECATION")

package com.guicarneirodev.ltascore.android.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.guicarneirodev.ltascore.android.data.repository.FirebaseFriendshipRepository
import com.guicarneirodev.ltascore.android.data.repository.FirebaseMatchPredictionRepository
import com.guicarneirodev.ltascore.android.data.repository.FirebaseRankingRepository
import com.guicarneirodev.ltascore.android.data.repository.FirebaseTeamFeedRepository
import com.guicarneirodev.ltascore.android.data.repository.FirebaseUserRepository
import com.guicarneirodev.ltascore.android.data.repository.FirebaseVoteRepository
import com.guicarneirodev.ltascore.android.data.repository.FirebaseVoteSocialRepository
import com.guicarneirodev.ltascore.android.data.repository.NotificationTokenRepository
import com.guicarneirodev.ltascore.android.data.repository.UserPreferencesRepository
import com.guicarneirodev.ltascore.android.viewmodels.AuthViewModel
import com.guicarneirodev.ltascore.android.viewmodels.EditProfileViewModel
import com.guicarneirodev.ltascore.android.viewmodels.FriendsFeedViewModel
import com.guicarneirodev.ltascore.android.viewmodels.FriendsViewModel
import com.guicarneirodev.ltascore.android.viewmodels.MatchSummaryViewModel
import com.guicarneirodev.ltascore.android.viewmodels.MatchesViewModel
import com.guicarneirodev.ltascore.android.viewmodels.NotificationSettingsViewModel
import com.guicarneirodev.ltascore.android.viewmodels.RankingViewModel
import com.guicarneirodev.ltascore.android.viewmodels.VoteHistoryViewModel
import com.guicarneirodev.ltascore.android.viewmodels.VotingViewModel
import com.guicarneirodev.ltascore.api.LoLEsportsApi
import com.guicarneirodev.ltascore.data.datasource.local.MatchLocalDataSource
import com.guicarneirodev.ltascore.data.datasource.static.PlayersStaticDataSource
import com.guicarneirodev.ltascore.data.repository.AndroidMatchSyncRepository
import com.guicarneirodev.ltascore.data.repository.MatchRepositoryImpl
import com.guicarneirodev.ltascore.domain.repository.FriendshipRepository
import com.guicarneirodev.ltascore.domain.repository.MatchPredictionRepository
import com.guicarneirodev.ltascore.domain.repository.MatchRepository
import com.guicarneirodev.ltascore.domain.repository.MatchSyncRepository
import com.guicarneirodev.ltascore.domain.repository.RankingRepository
import com.guicarneirodev.ltascore.domain.repository.TeamFeedRepository
import com.guicarneirodev.ltascore.domain.repository.UserRepository
import com.guicarneirodev.ltascore.domain.repository.VoteRepository
import com.guicarneirodev.ltascore.domain.repository.VoteSocialRepository
import com.guicarneirodev.ltascore.domain.usecases.GetCompletedMatchesUseCase
import com.guicarneirodev.ltascore.domain.usecases.GetFriendsFeedUseCase
import com.guicarneirodev.ltascore.domain.usecases.GetMatchByIdUseCase
import com.guicarneirodev.ltascore.domain.usecases.GetMatchesUseCase
import com.guicarneirodev.ltascore.domain.usecases.GetPlayerRankingUseCase
import com.guicarneirodev.ltascore.domain.usecases.GetUserVoteHistoryUseCase
import com.guicarneirodev.ltascore.domain.usecases.ManageFriendshipsUseCase
import com.guicarneirodev.ltascore.domain.usecases.ManageMatchPredictionsUseCase
import com.guicarneirodev.ltascore.domain.usecases.ShareVoteToTeamFeedUseCase
import com.guicarneirodev.ltascore.domain.usecases.SubmitPlayerVoteUseCase
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { FirebaseAuth.getInstance() }
    single { FirebaseFirestore.getInstance() }

    single { LoLEsportsApi() }

    single { PlayersStaticDataSource() }
    single { MatchLocalDataSource() }

    single<MatchRepository> { MatchRepositoryImpl(get(), get(), get()) }
    single<UserRepository> { FirebaseUserRepository(get(), get()) }
    single<VoteRepository> { FirebaseVoteRepository(get(), get()) }
    single<RankingRepository> { FirebaseRankingRepository(get(), get(), get()) }
    single<FriendshipRepository> { FirebaseFriendshipRepository(get(), get()) }
    single<VoteSocialRepository> { FirebaseVoteSocialRepository(get(), get()) }
    single<TeamFeedRepository> { FirebaseTeamFeedRepository(get(), get()) }
    single<MatchSyncRepository> { AndroidMatchSyncRepository(get(), get()) }
    single<MatchPredictionRepository> { FirebaseMatchPredictionRepository(get()) }

    single { NotificationTokenRepository() }
    single { UserPreferencesRepository(androidContext()) }

    single { GetMatchesUseCase(get()) }
    single { GetCompletedMatchesUseCase(get()) }
    single { GetMatchByIdUseCase(get()) }
    single { SubmitPlayerVoteUseCase(get(), get(), get()) }
    single { GetPlayerRankingUseCase(get()) }
    single { GetUserVoteHistoryUseCase(get(), get()) }
    single { ManageFriendshipsUseCase(get()) }
    single { GetFriendsFeedUseCase(get(), get(), get()) }
    single { ShareVoteToTeamFeedUseCase(get(), get(), get()) }
    single { ManageMatchPredictionsUseCase(get(), get()) }

    viewModel { MatchesViewModel(get(), get(), get()) }
    viewModel { VotingViewModel(get(), get(), get(), get()) }
    viewModel { MatchSummaryViewModel(get(), get(), get(), get()) }
    viewModel { AuthViewModel(get(), get(), get()) }
    viewModel { RankingViewModel(get(), get()) }
    viewModel { VoteHistoryViewModel(get(), get()) }
    viewModel { FriendsViewModel(get()) }
    viewModel { FriendsFeedViewModel(get(), get(), get(), get()) }
    viewModel { EditProfileViewModel(get(), get(), get()) }
    viewModel { NotificationSettingsViewModel(get(), get()) }
}