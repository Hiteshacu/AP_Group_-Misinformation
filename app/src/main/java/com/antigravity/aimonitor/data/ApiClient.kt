package com.antigravity.aimonitor.data

import com.antigravity.aimonitor.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Singleton object providing configured Retrofit instance for Gemini AI API and Groq API
 */
object ApiClient {
    // API Keys
    const val GEMINI_API_KEY = BuildConfig.GEMINI_API_KEY
    const val GEMINI_DETAIL_API_KEY = BuildConfig.GEMINI_DETAIL_API_KEY
    const val GROQ_API_KEY = BuildConfig.GROQ_API_KEY
    const val TAVILY_API_KEY = BuildConfig.TAVILY_API_KEY
    
    // Base URLs
    private const val GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/"
    private const val GROQ_BASE_URL = "https://api.groq.com/"
    private const val TAVILY_BASE_URL = "https://api.tavily.com/"
    
    /**
     * OkHttp client with optimized settings for speed
     * Increased timeouts for web scanning operations
     */
    private val okHttpClient: OkHttpClient by lazy {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC // Reduced logging for speed
        }
        
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS) // Increased from 10s
            .readTimeout(45, TimeUnit.SECONDS) // Increased from 15s for web scanning
            .writeTimeout(30, TimeUnit.SECONDS) // Increased from 10s
            .retryOnConnectionFailure(false) // Fail fast, let racing handle it
            .connectionPool(okhttp3.ConnectionPool(5, 5, TimeUnit.MINUTES)) // Connection pooling
            .build()
    }
    
    /**
     * Lazy-initialized Retrofit instance for Gemini with Gson converter
     */
    private val geminiRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(GEMINI_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    /**
     * Lazy-initialized Retrofit instance for Groq with Gson converter
     */
    private val groqRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(GROQ_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    /**
     * Lazy-initialized Retrofit instance for Tavily with Gson converter
     */
    private val tavilyRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(TAVILY_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    /**
     * Lazy-initialized Gemini API interface
     */
    val geminiApi: GeminiApi by lazy {
        geminiRetrofit.create(GeminiApi::class.java)
    }
    
    /**
     * Lazy-initialized Groq API interface
     */
    val groqApi: GroqApi by lazy {
        groqRetrofit.create(GroqApi::class.java)
    }
    
    /**
     * Lazy-initialized Tavily API interface
     */
    val tavilyApi: TavilyApi by lazy {
        tavilyRetrofit.create(TavilyApi::class.java)
    }
    
    // Backward compatibility
    @Deprecated("Use geminiRetrofit instead", ReplaceWith("geminiRetrofit"))
    val retrofit: Retrofit
        get() = geminiRetrofit
}
