package io.fastpix.data.di

import android.annotation.SuppressLint
import android.content.Context
import androidx.room.Room
import io.fastpix.data.domain.calculator.EventDataCalculator
import io.fastpix.data.domain.repository.EventApiService
import io.fastpix.data.domain.repository.EventDispatcher
import io.fastpix.data.domain.state.SDKStateService
import io.fastpix.data.domain.state.SessionService
import io.fastpix.data.domain.wallclock.ViewWatchCounter
import io.fastpix.data.utils.DeviceInfoUtility
import io.fastpix.data.utils.Logger
import io.fastpix.data.utils.NetworkTracker
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import okhttp3.MediaType.Companion.toMediaType
import io.fastpix.data.utils.JsonSerializer
import java.util.concurrent.TimeUnit
import android.os.Build
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import io.fastpix.data.pref.EventPersistenceManager
import io.fastpix.data.pref.ViewerPrefs
import io.fastpix.data.scalingTracker
import io.fastpix.data.sdkBuild.SDKBuildConfig
import io.fastpix.data.storage.AnalyticsDatabase
import io.fastpix.data.storage.EventStore
import okhttp3.Interceptor

/**
 * Manual dependency injection container
 * Replaces Hilt/Dagger for dependency management
 */
@SuppressLint("StaticFieldLeak")
object DependencyContainer {

    private const val TAG = "DependencyContainer"

    private var context: Context? = null
    private var isInitialized = false

    // Singleton instances
    private var _okHttpClient: OkHttpClient? = null
    private var _retrofit: Retrofit? = null
    private var _eventApiService: EventApiService? = null
    private var _networkTracker: NetworkTracker? = null
    private var _eventDispatcher: EventDispatcher? = null
    private var _sdkStateService: SDKStateService? = null
    private var _viewerPrefs: ViewerPrefs? = null
    private var _eventPersistenceManager: EventPersistenceManager? = null
    private var _eventDataCalculator: EventDataCalculator? = null
    private var _deviceInfoUtility: DeviceInfoUtility? = null
    private var _analyticsDatabase: AnalyticsDatabase? = null
    private var _eventStore: EventStore? = null

    /**
     * Initialize the dependency container with application context
     */
    fun initialize(context: Context) {
        if (isInitialized) {
            Logger.logWarning(TAG, "Already initialized")
            return
        }

        this.context = context.applicationContext
        isInitialized = true
        Logger.log(TAG, "Initialized successfully")
    }

    /**
     * Get application context
     */
    fun getContext(): Context {
        return context
            ?: throw IllegalStateException("DependencyContainer not initialized. Call initialize() first.")
    }

    /**
     * Get OkHttpClient instance
     */
    fun getOkHttpClient(): OkHttpClient {
        _okHttpClient?.let { return it }
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        // User-Agent interceptor to set proper Android user agent
        val userAgentInterceptor = Interceptor { chain ->
            val request = chain.request()
            val vmVersion = System.getProperty("java.vm.version")
            val userAgent =
                "Dalvik/${vmVersion} (Linux; U; Android ${Build.VERSION.RELEASE}; ${Build.MODEL} Build/${Build.ID})"

            val newRequest = request.newBuilder()
                .header("User-Agent", userAgent)
                .build()

            chain.proceed(newRequest)
        }

        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(userAgentInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
            .also { _okHttpClient = it }
    }

    fun getViewerPref(): ViewerPrefs? {
        if (_viewerPrefs == null) {
            _viewerPrefs = ViewerPrefs(getContext())
        }
        return _viewerPrefs
    }

    /**
     * Get EventPersistenceManager instance
     */
    fun getEventPersistenceManager(): EventPersistenceManager {
        return _eventPersistenceManager
            ?: EventPersistenceManager(getContext()).also { _eventPersistenceManager = it }
    }

    fun getAnalyticsDatabase(): AnalyticsDatabase {
        return _analyticsDatabase ?: Room.databaseBuilder(
            getContext(),
            AnalyticsDatabase::class.java,
            "fastpix_analytics.db"
        ).build().also { _analyticsDatabase = it }
    }

    fun getEventStore(): EventStore {
        return _eventStore
            ?: EventStore(getAnalyticsDatabase(), getEventPersistenceManager())
                .also { _eventStore = it }
    }

    /**
     * Get Retrofit instance
     */
    fun getRetrofit(): Retrofit {
        _retrofit?.let { return it }
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(SDKBuildConfig.SDK_URL) // Replace with your actual API base URL
            .client(getOkHttpClient())
            .addConverterFactory(JsonSerializer.json.asConverterFactory(contentType))
            .build()
            .also { _retrofit = it }
    }


    fun getNewRetrofit(): Retrofit {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(SDKBuildConfig.SDK_URL) // Replace with your actual API base URL
            .client(getOkHttpClient())
            .addConverterFactory(JsonSerializer.json.asConverterFactory(contentType))
            .build()
    }

    /**
     * Get EventApiService instance
     */
    fun getEventApiService(): EventApiService {
        return _eventApiService
            ?: getRetrofit().create(EventApiService::class.java)
                .also { _eventApiService = it }
    }

    /**
     * Get NetworkTracker instance
     */
    fun getNetworkTracker(): NetworkTracker {
        return _networkTracker ?: NetworkTracker(getContext()).also { _networkTracker = it }
    }

    /**
     * Get SDKStateService instance
     */
    fun getSDKStateService(): SDKStateService {
        return _sdkStateService ?: SDKStateService().also { _sdkStateService = it }
    }

    /**
     * Get EventDataCalculator instance
     */
    fun getEventDataCalculator(): EventDataCalculator {
        return _eventDataCalculator ?: EventDataCalculator().also { _eventDataCalculator = it }
    }

    /**
     * Get DeviceInfoUtility instance
     */
    fun getDeviceInfoUtility(): DeviceInfoUtility {
        return _deviceInfoUtility
            ?: DeviceInfoUtility(getContext()).also { _deviceInfoUtility = it }
    }

    /**
     * Get EventDispatcher instance
     */
    fun getEventDispatcher(): EventDispatcher {
        return _eventDispatcher ?: EventDispatcher(
            getEventApiService(),
            getNetworkTracker(),
            getContext()
        ).also { _eventDispatcher = it }
    }

    /**
     * Prepare dependencies for SDK release without dropping the stored context.
     * This clears and resets stateful singletons so that a subsequent initialize()
     * call can rebuild them from scratch while still allowing the cleanup process
     * to finish using any already created instances.
     */
    fun prepareForRelease() {
        if (!isInitialized) {
            Logger.logWarning(
                TAG,
                "prepareForRelease called before initialization"
            )
            return
        }
        ViewWatchCounter.reset()
        _eventDataCalculator?.reset()
        _eventDataCalculator = null
        _sdkStateService?.clearSdkState()
        _sdkStateService = null
        _eventDispatcher = null
        _eventStore = null
        SessionService.reset()
        scalingTracker.reset()
        Logger.log(TAG, "Prepared for release - instances cleared")
    }

    /**
     * Reset all instances (useful for testing)
     */
    fun reset() {
        _eventDataCalculator?.reset()
        _sdkStateService?.clearSdkState()
        ViewWatchCounter.destroy()
        SessionService.reset()
        _analyticsDatabase?.close()
        _okHttpClient = null
        _retrofit = null
        _eventApiService = null
        _networkTracker = null
        _eventDispatcher = null
        _sdkStateService = null
        _eventDataCalculator = null
        _deviceInfoUtility = null
        _eventPersistenceManager = null
        _analyticsDatabase = null
        _eventStore = null
        context = null
        isInitialized = false
        Logger.log(TAG, "Reset completed")
    }
}
