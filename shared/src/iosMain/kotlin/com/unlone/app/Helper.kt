package com.unlone.app


import com.unlone.app.data.auth.AuthRepository
import com.unlone.app.data.story.TopicRepository
import com.unlone.app.di.appModule
import com.unlone.app.domain.useCases.stories.FetchStoryDetailUseCase
import com.unlone.app.domain.useCases.stories.FetchStoryItemsUseCase
import com.unlone.app.domain.useCases.write.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.startKoin

class GreetingHelper : KoinComponent {
    private val greeting: Greeting by inject()
    fun greet(): String = greeting.greeting()
}

class AuthRepositoryHelper : KoinComponent {
    private val authRepo: AuthRepository by inject()

    //    private val authRepo: AuthRepository by inject(named("iosAuthRepo")){ parametersOf(KMMContext)}
    fun authRepo(): AuthRepository = authRepo
    /*
    suspend fun signUpEmail(email: String) = authRepo.signUpEmail(email)
    suspend fun signUp(email: String, password: String) = authRepo.signUp(email, password)
    suspend fun signInEmail(email: String) = authRepo.signInEmail(email)
    suspend fun signIn(email: String, password: String) = authRepo.signIn(email, password)
    suspend fun authenticate() = authRepo.authenticate()
    fun signOut() = authRepo.signOut()*/
}

class TopicRepositoryHelper : KoinComponent {
    private val topicRepo: TopicRepository by inject()

    fun topicRepo(): TopicRepository = topicRepo
}

class UseCasesHelper : KoinComponent {
	val fetchStoryItemsUseCase: FetchStoryItemsUseCase by inject()
    val fetchStoryDetailUseCase: FetchStoryDetailUseCase by inject()

    val getAllDraftsTitleUseCase: GetAllDraftsTitleUseCase by inject()
    val getLastOpenedUseCase: GetLastOpenedDraftUseCase by inject()
    val saveDraftUseCase: SaveDraftUseCase by inject()
    val createNewDraftUseCase: CreateNewDraftUseCase by inject()
    val queryDraftUseCase: QueryDraftUseCase by inject()
    val postStoryUseCase: PostStoryUseCase by inject()

}


fun initKoin() = startKoin {
    modules(appModule())
}
