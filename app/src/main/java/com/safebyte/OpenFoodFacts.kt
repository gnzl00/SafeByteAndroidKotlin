package com.safebyte

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Path
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType

interface OpenFoodFactsApi {
    @GET("api/v0/product/{barcode}.json")
    suspend fun getProduct(@Path("barcode") barcode: String): ProductResponse
}

@Serializable
data class ProductResponse(
    val status: Int = 0,
    val product: Product? = null
)

@Serializable
data class Product(
    @SerialName("product_name") val productName: String? = null,
    @SerialName("ingredients_text") val ingredientsText: String? = null,
    val allergens: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    val nutriments: Map<String, JsonElement>? = null
)

object OpenFoodFacts {
    private val json = Json { ignoreUnknownKeys = true }

    private val client: OkHttpClient by lazy {
        val logger = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
        OkHttpClient.Builder()
            .addInterceptor(logger)
            .build()
    }

    val api: OpenFoodFactsApi by lazy {
        val contentType = "application/json".toMediaType()
        Retrofit.Builder()
            .baseUrl("https://world.openfoodfacts.org/")
            .client(client)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(OpenFoodFactsApi::class.java)
    }
}