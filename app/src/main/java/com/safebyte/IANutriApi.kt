package com.safebyte

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

interface IANutriApi {
    @POST("api/IANutri/Reformulate")
    suspend fun reformulate(
        @Body request: IANutriReformulateRequest
    ): IANutriReformulateResponse

    @POST("api/IANutri/GenerateSuggestions")
    suspend fun generateSuggestions(
        @Body request: IANutriGenerateSuggestionsRequest
    ): IANutriGenerateSuggestionsResponse

    @POST("api/IANutri/CookingAssistant")
    suspend fun cookingAssistant(
        @Body request: IANutriCookingAssistantRequest
    ): IANutriCookingAssistantResponse

    @GET("api/IANutri/History")
    suspend fun getHistory(
        @Query("email") email: String
    ): IANutriHistoryEnvelope

    @DELETE("api/IANutri/History")
    suspend fun deleteHistory(
        @Query("email") email: String
    ): IANutriDeleteHistoryResponse
}

@Serializable
data class IANutriReformulateRequest(
    val email: String = "",
    val userInput: String = "",
    val option: String = "",
    val allergens: List<String> = emptyList()
)

@Serializable
data class IANutriGenerateSuggestionsRequest(
    val email: String = "",
    val userInput: String = "",
    val option: String = "",
    val reformulatedPrompt: String = "",
    val allergens: List<String> = emptyList()
)

@Serializable
data class IANutriCookingAssistantRequest(
    val email: String = "",
    val allergens: List<String> = emptyList(),
    val historyId: String = "",
    val recipe: IANutriRecipeSuggestion? = null
)

@Serializable
data class IANutriReformulateResponse(
    val optionLabel: String = "",
    val reformulatedPrompt: String = "",
    val allergens: List<String> = emptyList(),
    val notes: List<String> = emptyList()
)

@Serializable
data class IANutriGenerateSuggestionsResponse(
    val historyId: String = "",
    val summary: String = "",
    val reformulatedPrompt: String = "",
    val allergens: List<String> = emptyList(),
    val globalWarnings: List<String> = emptyList(),
    val generalSubstitutions: List<String> = emptyList(),
    val suggestions: List<IANutriRecipeSuggestion> = emptyList()
)

@Serializable
data class IANutriRecipeSuggestion(
    val title: String = "",
    val description: String = "",
    val estimatedTime: String = "",
    val difficulty: String = "",
    val ingredients: List<String> = emptyList(),
    val steps: List<String> = emptyList(),
    val allergensDetected: List<String> = emptyList(),
    val safeSubstitutions: List<String> = emptyList(),
    val allergyWarning: String = ""
)

@Serializable
data class IANutriCookingAssistantResponse(
    val recipeTitle: String = "",
    val intro: String = "",
    val requiredItems: List<String> = emptyList(),
    val stepByStep: List<String> = emptyList(),
    val safetyNotes: List<String> = emptyList()
)

@Serializable
data class IANutriHistoryEnvelope(
    val history: List<IANutriHistoryItem> = emptyList(),
    @SerialName("History")
    val historyPascal: List<IANutriHistoryItem> = emptyList()
) {
    val resolvedHistory: List<IANutriHistoryItem>
        get() = if (history.isNotEmpty()) history else historyPascal
}

@Serializable
data class IANutriHistoryItem(
    val id: String = "",
    val email: String = "",
    val userInput: String = "",
    val option: String = "",
    val reformulatedPrompt: String = "",
    val summary: String = "",
    val allergens: List<String> = emptyList(),
    val globalWarnings: List<String> = emptyList(),
    val generalSubstitutions: List<String> = emptyList(),
    val suggestions: List<IANutriRecipeSuggestion> = emptyList(),
    val createdAtUtc: String? = null,
    val updatedAtUtc: String? = null
)

@Serializable
data class IANutriDeleteHistoryResponse(
    val message: String = "",
    val deleted: Int = 0,
    @SerialName("Message")
    val messagePascal: String = "",
    @SerialName("Deleted")
    val deletedPascal: Int = 0
) {
    val resolvedMessage: String
        get() = message.ifBlank { messagePascal }
    val resolvedDeleted: Int
        get() = if (deleted != 0) deleted else deletedPascal
}

object IANutriNetwork {
    private val json = Json { ignoreUnknownKeys = true }
    private val cache = ConcurrentHashMap<String, IANutriApi>()

    private val client: OkHttpClient by lazy {
        val logger = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
        }
        OkHttpClient.Builder()
            .addInterceptor(logger)
            .readTimeout(40, TimeUnit.SECONDS)
            .writeTimeout(40, TimeUnit.SECONDS)
            .connectTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    private fun buildApi(baseUrl: String): IANutriApi {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(IANutriApi::class.java)
    }

    fun apiFor(baseUrl: String): IANutriApi {
        val normalized = normalizeBaseUrl(baseUrl)
        return cache.getOrPut(normalized) { buildApi(normalized) }
    }

    private fun normalizeBaseUrl(value: String): String {
        val trimmed = value.trim().ifBlank { BuildConfig.SAFEBYTE_API_BASE_URL }.trim()
        val noTrailingSlash = trimmed.trimEnd('/')
        val cleaned = if (noTrailingSlash.lowercase().endsWith("/api")) {
            noTrailingSlash.dropLast(4)
        } else {
            noTrailingSlash
        }
        val finalBase = cleaned.ifBlank { BuildConfig.SAFEBYTE_API_BASE_URL.trimEnd('/') }
        return if (finalBase.endsWith("/")) finalBase else "$finalBase/"
    }
}

