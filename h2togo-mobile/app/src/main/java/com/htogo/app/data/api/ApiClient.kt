package com.htogo.app.data.api

import android.content.Context
import com.htogo.app.data.local.SessionManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Cliente HTTP centralizado que inicializa Retrofit y expone las APIs.
 */
class ApiClient private constructor(context: Context) {

    private val sessionManager = SessionManager.getInstance(context)

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val authInterceptor = AuthInterceptor(sessionManager)

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(ApiConstants.BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val authApi: AuthApi = retrofit.create(AuthApi::class.java)
    val pedidosApi: PedidosApi = retrofit.create(PedidosApi::class.java)
    val direccionesApi: DireccionesApi = retrofit.create(DireccionesApi::class.java)
    val negociosApi: NegociosApi = retrofit.create(NegociosApi::class.java)
    val repartidorApi: RepartidorApi = retrofit.create(RepartidorApi::class.java)
    val inventarioApi: InventarioApi = retrofit.create(InventarioApi::class.java)

    companion object {
        @Volatile
        private var instance: ApiClient? = null

        fun getInstance(context: Context): ApiClient {
            return instance ?: synchronized(this) {
                instance ?: ApiClient(context.applicationContext).also { instance = it }
            }
        }
    }
}
