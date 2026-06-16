# FastPix Android Data Core SDK

[![License](https://img.shields.io/badge/License-Proprietary-blue.svg)](LICENSE)
[![Version](https://img.shields.io/badge/version-1.3.1-green.svg)](CHANGELOG.md)
[![Min SDK](https://img.shields.io/badge/minSdk-24-orange.svg)](build.gradle.kts)

The FastPix Android Data Core SDK serves as the foundational layer for integrating video playback
analytics within Android applications. As a core SDK, it does not function as a standalone player
but instead enhances any video player by capturing essential playback metrics such as playback
events, buffering patterns, and user engagement data. This data is automatically collected and made
available on the FastPix dashboard for real-time monitoring and analysis. Designed for efficiency,
the SDK ensures minimal impact on playback performance. While currently optimized for Java-based
Android projects, future updates will introduce extended support for Kotlin and enhanced
customization options for analytics tracking.

# Key Features:

- **Automatic analytics tracking** – Seamlessly captures playback metrics without additional
  configuration.
- **Real-time insights** – Provides instant access to video performance data on the FastPix
  dashboard.
- **Optimized for performance** – Ensures minimal impact on playback and system resources.
- **Future enhancements** – Planned support for Kotlin and expanded analytics customization options.

## Requirements

- **Minimum Android SDK**: 24 (Android 7.0)
- **Target/Compile SDK**: 35
- **Kotlin**: 2.0.21+
- **Java**: 11

## Installation

### Gradle (Kotlin DSL)

Add the GitHub Packages repository to your project's `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://maven.pkg.github.com/FastPix/android-core-data-sdk")
            credentials {
                username = project.findProperty("lpr.user") as String? ?: "github-username"
                password =
                    project.findProperty("lpr.token") as String? ?: "github-personal-access-token"
            }
        }
    }
}
```

Add the dependency to your app's `build.gradle.kts`:

```kotlin
dependencies {
    implementation("io.fastpix.data:core:1.3.1")
}
```

### Authentication

Create a `local.properties` file in your project root and add your GitHub credentials:

```properties
lpr.user=YOUR_GITHUB_USERNAME
lpr.key=YOUR_GITHUB_PERSONAL_ACCESS_TOKEN
```

> **Note**: Make sure to add `local.properties` to your `.gitignore` to keep credentials secure.

## Quick Start

### 1. Initialize the SDK

```kotlin
import io.fastpix.data.FastPixDataSDK
import io.fastpix.data.domain.SDKConfiguration
import io.fastpix.data.domain.model.VideoDataDetails
import io.fastpix.data.domain.model.PlayerDataDetails
import io.fastpix.data.domain.model.CustomDataDetails
import io.fastpix.data.domain.listeners.PlayerListener

class MainActivity : AppCompatActivity() {

    private val fastPixSDK = FastPixDataSDK()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Create configuration
        val config = SDKConfiguration(
            workspaceId = "your-workspace-id",
            beaconUrl = "custom.beacon.url", // Optional
            // Optional
            videoData = VideoDataDetails(
                videoId = "video-123",
                videoTitle = "My Awesome Video",
                videoDuration = "300000", // in milliseconds
                videoThumbnail = "https://example.com/thumbnail.jpg",
                fpPlaybackId = "playback-id" // Optional FastPix playback ID
                //.. etc
            ),
            // Optional
            playerData = PlayerDataDetails(
                playerName = "ExoPlayer",
                playerVersion = "2.19.0"
            ),
            playerListener = MyPlayerListener(),
            enableLogging = true, // Enable for debugging
            // Optional
            customData = CustomDataDetails(
                customField1 = "custom-value-1",
                customField2 = "custom-value-2"
                //.. etc
            )
        )

        // Initialize SDK
        fastPixSDK.initialize(config, applicationContext)
    }
  
    override fun onDestroy() {
        super.onDestroy()
        // Clear the SDK instance
        fastPixSDK.release()
    }
}
```

### 2. Implement PlayerListener

The `PlayerListener` interface provides the SDK with real-time player state information:

```kotlin
import io.fastpix.data.domain.listeners.PlayerListener
import io.fastpix.data.domain.model.BandwidthModel
import io.fastpix.data.domain.model.ErrorModel

class MyPlayerListener : PlayerListener {

    override fun playerHeight(): Int? = player.videoSize.height

    override fun playerWidth(): Int? = player.videoSize.width

    override fun videoSourceWidth(): Int? = player.videoFormat?.width

    override fun videoSourceHeight(): Int? = player.videoFormat?.height

    override fun playHeadTime(): Int? = player.currentPosition.toInt()

    override fun mimeType(): String? = player.videoFormat?.sampleMimeType

    override fun sourceFps(): String? = player.videoFormat?.frameRate?.toString()

    override fun sourceAdvertisedBitrate(): String? = player.videoFormat?.bitrate?.toString()

    override fun sourceAdvertiseFrameRate(): Int? = player.videoFormat?.frameRate

    override fun sourceDuration(): Int? = player.duration.toInt()

    override fun isPause(): Boolean? = !player.isPlaying

    override fun isAutoPlay(): Boolean? = player.isAutoPlay()

    override fun preLoad(): Boolean? = player.preLoad

    override fun isBuffering(): Boolean? = player.playbackState == Player.STATE_BUFFERING

    override fun playerCodec(): String? = player.videoFormat?.codecs

    override fun sourceHostName(): String? =
        Uri.parse(player.currentMediaItem?.localConfiguration?.uri.toString()).host

    override fun isLive(): Boolean? = player.isCurrentMediaItemLive

    override fun sourceUrl(): String? = player.currentMediaItem?.localConfiguration?.uri.toString()

    override fun isFullScreen(): Boolean? = isPlayerFullScreen

    override fun getBandWidthData(): BandwidthModel {
        return BandwidthModel(
            bandwidth = player.currentBandwidthEstimate.toString(),
            // Add other bandwidth metrics
        )
    }

    override fun getPlayerError(): ErrorModel {
        return ErrorModel(
            errorCode = player.playerError?.errorCode?.toString(),
            errorMessage = player.playerError?.message
        )
    }

    override fun getVideoCodec(): String? = player.videoFormat?.codecs
    override fun getSoftwareName(): String? = "software-name"
    override fun getSoftwareVersion(): String? = "software-version"
}
```

### 3. Dispatch Events

Track player events throughout the video lifecycle:

```kotlin
import io.fastpix.data.domain.enums.PlayerEventType

// When player is ready
fastPixSDK.dispatchEvent(PlayerEventType.playerReady)

// When video begins
fastPixSDK.dispatchEvent(PlayerEventType.viewBegin)

// When playback starts
fastPixSDK.dispatchEvent(PlayerEventType.play)
fastPixSDK.dispatchEvent(PlayerEventType.playing)

// When user pauses
fastPixSDK.dispatchEvent(PlayerEventType.pause)

// When seeking
fastPixSDK.dispatchEvent(PlayerEventType.seeking)
fastPixSDK.dispatchEvent(PlayerEventType.seeked)

// When buffering
fastPixSDK.dispatchEvent(PlayerEventType.buffering)
fastPixSDK.dispatchEvent(PlayerEventType.buffered)

// When quality changes
fastPixSDK.dispatchEvent(PlayerEventType.variantChanged)

// When video ends
fastPixSDK.dispatchEvent(PlayerEventType.ended)
fastPixSDK.dispatchEvent(PlayerEventType.viewCompleted)

// On errors
fastPixSDK.dispatchEvent(PlayerEventType.error)
```

### 4. Clean Up

Release SDK resources when done:

```kotlin
override fun onDestroy() {
    super.onDestroy()
    fastPixSDK.release()
}
```

## Configuration

### SDKConfiguration

| Parameter        | Type              | Required | Description                               |
|------------------|-------------------|----------|-------------------------------------------|
| `workspaceId`    | String            | ✅        | Your FastPix workspace identifier         |
| `playerListener` | PlayerListener    | ✅        | Interface implementation for player state |
| `videoData`      | VideoDataDetails  | ❌        | Video metadata (see below)                |
| `beaconUrl`      | String            | ❌        | Custom beacon URL (default: anlytix.io)   |
| `playerData`     | PlayerDataDetails | ❌        | Player information                        |
| `enableLogging`  | Boolean           | ❌        | Enable debug logs (default: true)         |
| `customData`     | CustomDataDetails | ❌        | Custom metadata fields                    |

### VideoDataDetails

| Field              | Type   | Required | Description              |
|--------------------|--------|----------|--------------------------|
| `videoId`          | String | ✅        | Unique video identifier  |
| `videoTitle`       | String | ✅        | Video title              |
| `videoDuration`    | String | ❌        | Duration in milliseconds |
| `videoThumbnail`   | String | ❌        | Thumbnail URL            |
| `videoSeries`      | String | ❌        | Series name              |
| `videoProducer`    | String | ❌        | Producer name            |
| `videoDrmType`     | String | ❌        | Producer name            |
| `videoContentType` | String | ❌        | Content type             |
| `videoVariant`     | String | ❌        | Variant information      |
| `videoLanguage`    | String | ❌        | Language code            |
| `fpPlaybackId`     | String | ❌        | FastPix playback ID      |
| `foMediaId`        | String | ❌        | Media ID                 |
| `fpLiveStreamId`   | String | ❌        | Live stream ID           |

### PlayerDataDetails

| Field           | Type   | Required | Description                     |
|-----------------|--------|----------|---------------------------------|
| `playerName`    | String | ❌        | Player name (e.g., "ExoPlayer") |
| `playerVersion` | String | ❌        | Player version                  |

### CustomDataDetails

Supports up to 10 custom fields (`customField1` through `customField10`) for additional metadata.

## Event Types

The SDK tracks the following player events:

- **`playerReady`** - Player is initialized and ready
- **`viewBegin`** - Video view has started
- **`play`** - Playback initiated
- **`playing`** - Video is actively playing
- **`pause`** - Playback paused
- **`seeking`** - Seek operation started
- **`seeked`** - Seek operation completed
- **`buffering`** - Buffering started
- **`buffered`** - Buffering completed
- **`variantChanged`** - Quality/variant changed
- **`ended`** - Playback ended
- **`viewCompleted`** - View session completed
- **`error`** - Playback error occurred
- **`requestCanceled`** - Network request canceled
- **`requestFailed`** - Network request failed
- **`requestCompleted`** - Network request completed

## Dependencies

The SDK uses the following libraries:

- **Kotlin Coroutines** - Async operations
- **OkHttp** - Network requests
- **Retrofit** - API communication
- **Gson** - JSON serialization
- **Kotlinx Serialization** - Data serialization

## Changelog

See [CHANGELOG.md](CHANGELOG.md) for release history and version updates.

## Support

For questions, issues, or feature requests:

- **Email**: support@fastpix.com
- **Documentation**: [FastPix Documentation](https://fastpix.com/docs)
- **GitHub Issues**: [Report an issue](https://github.com/FastPix/android-core-data-sdk/issues)

## License

Copyright © 2025 FastPix. All rights reserved.

This SDK is proprietary software. Unauthorized copying, modification, distribution, or use of this
software is strictly prohibited.

