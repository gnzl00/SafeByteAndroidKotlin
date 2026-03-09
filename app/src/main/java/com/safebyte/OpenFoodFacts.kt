package com.safebyte

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.IOException
import java.net.Inet4Address
import java.net.InetAddress
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import okhttp3.Dns
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.HttpException
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface OpenFoodFactsApi {
    @GET("api/v2/product/{barcode}.json")
    suspend fun getProduct(
        @Path("barcode") barcode: String,
        @Query("fields") fields: String = OpenFoodFacts.REQUEST_FIELDS
    ): ProductResponse
}

@Serializable
data class ProductResponse(
    val status: Int = 0,
    @SerialName("status_verbose") val statusVerbose: String? = null,
    val product: Product? = null
)

@Serializable
data class Product(
    @SerialName("product_name") val productName: String? = null,
    @SerialName("ingredients_text") val ingredientsText: String? = null,
    val allergens: String? = null,
    @SerialName("allergens_tags") val allergensTags: List<String>? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    val nutriments: Map<String, JsonElement>? = null
)

sealed interface OpenFoodFactsLookupResult {
    data class Found(val product: Product) : OpenFoodFactsLookupResult
    data object NotFound : OpenFoodFactsLookupResult    
    data class NetworkError(val error: IOException) : OpenFoodFactsLookupResult
    data class HttpError(val code: Int) : OpenFoodFactsLookupResult
    data class UnknownError(val error: Throwable) : OpenFoodFactsLookupResult
}

object OpenFoodFacts {
    const val REQUEST_FIELDS =
        "product_name,ingredients_text,allergens,allergens_tags,image_url,nutriments"

    private val json = Json { ignoreUnknownKeys = true }
    private val hosts = listOf(
        "https://world.openfoodfacts.org/",
        "https://es.openfoodfacts.org/",
        "https://openfoodfacts.org/"
    )
    private val apiCache = ConcurrentHashMap<String, OpenFoodFactsApi>()
    private const val RETRIES_PER_HOST = 2

    private val client by lazy {
        val logger = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
        }
        val ipv4FirstDns = object : Dns {
            override fun lookup(hostname: String): List<InetAddress> =
                Dns.SYSTEM.lookup(hostname)
                    .sortedBy { address -> if (address is Inet4Address) 0 else 1 }
        }

        OkHttpClient.Builder()
            .dns(ipv4FirstDns)
            .retryOnConnectionFailure(true)
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .callTimeout(25, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "SafeByteAndroid/1.0 (contact: team@safebyte.app)")
                    .header("Accept", "application/json")
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(logger)
            .build()
    }

    private fun buildApi(baseUrl: String): OpenFoodFactsApi {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(OpenFoodFactsApi::class.java)
    }

    private fun apiFor(baseUrl: String): OpenFoodFactsApi =
        apiCache.getOrPut(baseUrl) { buildApi(baseUrl) }

    suspend fun lookupProduct(barcode: String): OpenFoodFactsLookupResult {
        var lastIoError: IOException? = null
        var lastHttpError: HttpException? = null
        var lastUnknown: Throwable? = null

        for (baseUrl in hosts) {
            val apiForHost = apiFor(baseUrl)
            repeat(RETRIES_PER_HOST) { attempt ->
                try {
                    val response = apiForHost.getProduct(barcode)
                    val product = response.product
                    return if (response.status == 1 && product != null) {
                        OpenFoodFactsLookupResult.Found(product)
                    } else {
                        OpenFoodFactsLookupResult.NotFound
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: IOException) {
                    lastIoError = e
                    if (attempt == 0) delay(250L)
                } catch (e: HttpException) {
                    lastHttpError = e
                    if (e.code() == 400 || e.code() == 404) {
                        return OpenFoodFactsLookupResult.NotFound
                    }
                    if (attempt == 0) delay(250L)
                } catch (e: Throwable) {
                    lastUnknown = e
                    if (attempt == 0) delay(250L)
                }
            }
        }

        lastIoError?.let { return OpenFoodFactsLookupResult.NetworkError(it) }
        lastHttpError?.let { return OpenFoodFactsLookupResult.HttpError(it.code()) }
        lastUnknown?.let { return OpenFoodFactsLookupResult.UnknownError(it) }
        return OpenFoodFactsLookupResult.UnknownError(
            IOException("No se pudo consultar Open Food Facts en este momento.")
        )
    }
}

