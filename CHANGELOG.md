# Changelog

All notable changes to this project will be documented in this file.

## [1.3.1]
### Changed
- Refactored `BaseEvent` from an abstract class to an interface; all event models updated accordingly.
- Centralized event serialization and deserialization in `EventJsonCodec` using a binding-based lookup table, replacing verbose `when` blocks in `EventPersistenceManager` and `EventStore`.
- Refactored `FastPixDataSDK.dispatchEvent` to use a data-driven `eventDispatchTable` for event building and side-effect management.
- Streamlined session recreation logic in `FastPixDataSDK`.
- `DependencyContainer` now uses lazy initialization and safer null handling for service instances.
- Improved `FastPixDataSDK` lifecycle management and event dispatching flow.
- `DeviceInfoUtility.getDeviceName()` now returns `Build.DEVICE` instead of a manufacturer and model string.

### Improved
- Enhanced null safety in `EventDataCalculator` during view metric calculations.
- Structured logging with standardized `TAG` constants in `EventPersistenceManager`, `EventStore`, and related modules.
- Replaced hardcoded strings with private constants for logging levels and shared preference keys.
- Cleaned up internal state management in `NetworkTracker` and `SessionService`.
- Simplified `DeviceInfoUtility` with property-based OS and device type constants.
- Reduced code size and complexity across persistence, storage, and upload pipeline components.

## [1.3.0]
### Added
- Added unit test coverage for session lifecycle and SDK state transitions (`SessionServiceTest` and `SDKStateServiceTest`).
- Added regression tests for duplicate `viewBegin` dispatch prevention and session recovery behavior in `FastPixDataSDKTest`.
- Added test dependencies for MockK, AndroidX core-testing, and coroutine test utilities.

## [1.2.9]
- Introduced `IMMEDIATE_UPLOAD_EVENTS` set containing "viewBegin" and "playerReady".
- Updated `EventDispatcher` to trigger an immediate upload if a critical event is detected in the drained event batch and network is available.

### Fixed
- Prevented duplicate `viewBegin` events from being dispatched for the same active view.
- Fixed session recovery flow to create a fresh view context, re-emit bootstrap events (`playerReady`, `viewBegin`), and re-dispatch the original pending event.
- Improved `SessionService` thread safety for concurrent access by synchronizing session lifecycle operations.

## [1.2.8]
### Added
- Periodic in-process event upload in `EventDispatcher` that pushes pending events to the server every 10 seconds, reducing reliance on WorkManager for timely delivery.
- `getAllSessions` query in `SessionDao` and `EventStore` to retrieve all sessions regardless of status.
- Debug logging for dispatched events in `FastPixDataSDK`.

### Fixed
- Fixed `viewTimeToFirstFrame` calculation in `PlayingEvent` — value is now computed before the sent-flag is updated, ensuring the correct timestamp is captured.
- Sessions are now only deleted after upload when their status is `COMPLETED`, preventing premature removal of active sessions in both `EventUploadWorker` and periodic upload.

### Changed
- `EventUploadWorker` now processes all sessions (not just completed ones), uploading their events while still respecting session lifecycle for deletion.
- Refined pulse event scheduling in `FastPixBaseMedia3Player`: pulse timers are now started on `viewBegin`, `play`, `seeked` (when playing), and `buffering`, and cancelled only when the player is no longer playing after a seek.
- Improved `EventDispatcher` shutdown sequence — cancels upload/pipeline jobs, flushes remaining events, and attempts a final upload before scheduling WorkManager fallback.

## [1.2.7]
## Changed
- Replaced the manual event cleanup logic with a Room-based persistence layer and WorkManager-driven upload pipeline.
- Added `AnalyticsDatabase`, `EventStore`, and associated DAOs/Entities to handle persistent event storage.
- Introduced `EventQueue` for thread-safe in-memory event buffering.
- Implemented `EventUploadWorker` and `EventUploadScheduler` to manage reliable background data synchronization.
- Updated `FastPixAnalytics` and `FastPixDataSDK` to use a new lifecycle-aware state machine (`SdkLifecycleState`).
- Refactored `EventDispatcher` to orchestrate the flow from the in-memory queue to the persistent store and network.
- Updated the default beacon domain from `metrix.ws` to `anlytix.io`.
- Enhanced the `Logger` utility with structured formatting, session/trace tracking, and standardized event labels.
- Updated dependencies for Room, KSP, and Kotlin serialization.

## [1.2.6]
### Improved
- **SDK Version Update**:
- Crash Fix
- Bug Fix

## [1.2.5]
### Improved
- **SDK Version Update**:
- SDK Version Update
- Bug Fix

## [1.2.4]
### Improved
- **SDK Version Update**:
- SDK Version Update

## [1.2.3]
### Improved
- **SDK Version Update**:
- SDK Version Update
- Improve Analytics
- Improved Data

## [1.2.2]
### Improved
- **SDK Version Update**:
- SDK Version Update

## [1.2.1]
### Improved
- **Major Code Optimization and Refactoring**:
- Data Syncing to server by keeping work manager
- Handles app killing scenario also
- Keeps the data in local and removes it after synced to server

## [1.2.0]
### Improved
- **Major Code Optimization and Refactoring**:
- Resolved crash in network monitoring caused by repeated registerNetworkCallback calls
- Ensured network callback lifecycle is safely handled
- Improved stability when tracking connectivity events

## [1.1.0]
### Changed
- **Major Code Optimization and Refactoring**:
- Comprehensive code refactoring for improved maintainability and performance.
- Optimized internal components and dependencies for better efficiency.
- Enhanced code structure and organization across the SDK.
- Improved overall stability and reduced technical debt.

## [1.0.2]
### Added
- **Enhanced Playback Experience and Stability Improvements**:
- Fullscreen Detection: Tracked views only after fullscreen activation.
- Request Event Logging: Enhanced cancellation details and error tracking.
- Playback Stability: Fixed event transitions, seek handling, and playhead updates.
- SDK Version Tracking: Included FastPix SDK name and version in metadata.

## [1.0.1]
### Added
- **Update core files**
  -logic and code updated

## [1.0.0]
### Added
- **Integration with ExoPlayer**:
  - Enabled video performance tracking using FastPix Data SDK, supporting ExoPlayer streams with user engagement metrics, playback quality monitoring, and real-time diagnostics.
  - Provides robust error management and reporting capabilities for seamless ExoPlayer video performance tracking.
  - Allows customizable behavior, including options to disable data collection, respect Do Not Track settings, and configure advanced error tracking with automatic error handling.
  - Includes support for custom metadata, enabling users to pass optional fields such as video_id, video_title, video_duration, and more.
  - Introduced event tracking for onPlayerStateChanged and onTracksChanged to handle seamless metadata updates during playback transitions.
    