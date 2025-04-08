package com.parawale.rateit.network

import android.util.Log
import com.google.firebase.crashlytics.buildtools.reloc.com.google.common.reflect.TypeToken
import com.google.firebase.crashlytics.buildtools.reloc.org.apache.http.conn.ConnectTimeoutException
import com.google.gson.Gson
import com.parawale.rateit.Models.Product
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.json.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json

object FakeStoreApi {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true }) // Handles unknown JSON fields
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 30000  // 30 seconds
            connectTimeoutMillis = 30000  // 30 seconds
            socketTimeoutMillis = 30000   // 30 seconds
        }
    }

    suspend fun fetchProducts(): List<Product> {
        return try {
            Log.d("FakeStoreApi", "Fetching products...") // Log request start
            val response: HttpResponse = client.get("https://fakestoreapi.com/products")
            Log.d("FakeStoreApi", "Response received: ${response.status}")

            if (response.status.value == 200) {
                val products: List<Product> = response.body()
                Log.d("FakeStoreApi", "Parsed ${products.size} products successfully")
                products
            } else {
                Log.e("FakeStoreApi", "Error: ${response.status}")
                emptyList()
            }
        } catch (e: ConnectTimeoutException) {
            Log.e("FakeStoreApi", "Connection Timeout: ${e.message}")
            emptyList()
        } catch (e: Exception) {
            Log.e("FakeStoreApi", "Network Error: ${e.message}")
            emptyList()
        }
    }

}
