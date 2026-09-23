# SQLi Auto-Hunter

An automated reconnaissance Android application that accepts user-provided keywords, generates search-engine dork queries, scrapes search results, extracts candidate URLs, deduplicates via hashing, tests each URL for SQL injection vulnerabilities using a Chromium-based rendering engine (via Chrome DevTools Protocol), classifies results by confidence, stores everything in a local Room database, exports results to CSV, and persists execution in the background via a foreground service.

## Features

### Core Capabilities
- **Keyword & Dork Engine**: Expands keywords into SQLi-specific Google/Bing dork syntax variations
- **Search Scraper Engine**: Scrapes results from Bing, DuckDuckGo, and SearX with rotating User-Agent bundles
- **URL Normalizer + Hash Deduplicator**: Normalizes URLs and uses SHA-256 hashing for deduplication
- **Domain Blacklist Filter**: Filters against configurable blacklist (default: .gov, .mil, bank, etc.)
- **Vulnerability Scanner Engine**: Tests URLs using three independent techniques:
  - Error-based: Injects `'`, `"`, checks for SQL error signatures
  - Boolean-based: Compares responses for `AND 1=1` vs `AND 1=2` payloads
  - Time-based: Injects `SLEEP(5)` equivalent, measures response delay
- **Result Classifier**: Assigns confidence score (High/Medium/Low) based on technique agreement
- **Proxy Rotation Manager**: Round-robin rotation through user-provided proxy list

### Persistence
- **Foreground Service**: Returns START_STICKY, shows persistent notification
- **WorkManager**: Acts as watchdog, restarts service if killed by OS
- **Room Database**: Stores queue, tested URLs, vulnerability results, crash logs
- **CSV Export**: Scoped-storage compliant with buffered writing

### UI (Jetpack Compose)
- **Keyword Input**: Multi-keyword text field, dork template dropdown, Start/Stop button
- **Manual URL Test**: Single URL input, direct "Test Now" button
- **Live Progress Dashboard**: Real-time counters, live-scrolling list with status icons
- **Results Page**: Filterable list (by confidence), tap for detail view
- **Settings**: Thread count slider, delay between requests, proxy list input, blacklist editor
- **Logs & Diagnostics**: Crash log list, filter by module/severity, export logs

## Architecture

### Tech Stack
- **Language**: Kotlin (100%)
- **UI Framework**: Jetpack Compose (Material 3)
- **Local Database**: Room with KSP
- **Networking**: OkHttp + Retrofit
- **HTML Parsing**: Jsoup
- **Browser Engine**: Android System WebView + Chrome DevTools Protocol
- **Background Execution**: Foreground Service + WorkManager
- **Concurrency**: Kotlin Coroutines + Flow + Mutex
- **Dependency Injection**: Hilt
- **Build System**: Gradle with Kotlin DSL

### Modules (16)
1. Keyword & Dork Engine
2. Browser Engine Controller (Chromium WebView + CDP)
3. Search Scraper Engine
4. URL Normalizer + Hash Deduplicator
5. Domain Blacklist Filter
6. URL Queue Manager
7. Vulnerability Scanner Engine
8. Result Classifier
9. Proxy Rotation Manager
10. Foreground Service + WorkManager
11. Notification Controller
12. Crash Log Manager
13. Database Storage (Room)
14. CSV Export Manager
15. Dynamic Concurrency Controller
16. UI Dashboard (6 Screens)

## Getting Started

### Prerequisites
- Android Studio (latest version)
- Java 17+
- Android SDK 34
- Android NDK

### Installation
1. Clone the repository
2. Open in Android Studio
3. Sync Gradle
4. Build the project

### Running
- Connect an Android device (API 26+)
- Click "Run" in Android Studio
- Or build APK: `./gradlew assembleDebug`

## CI/CD

### GitHub Actions Workflows
- **build.yml**: Main CI pipeline with build, test, lint, and release jobs
- **nightly-build.yml**: Nightly automated build

### Build Types
- **Debug**: `./gradlew assembleDebug`
- **Release**: `./gradlew assembleRelease`

### Testing
- Unit tests: `./gradlew testDebugUnitTest`
- Instrumentation tests: `./gradlew connectedDebugAndroidTest`

### Lint
- Run lint: `./gradlew lintDebug`

## Configuration

### Settings
All settings are configurable via the Settings screen:
- Thread count (1-8, auto-detected based on RAM)
- Delay between requests (ms)
- Bulk mode (enable proxy rotation)
- Proxy list
- Auto export to CSV
- Domain blacklist

### Default Blacklist
- .gov
- .mil
- .gob
- bank
- gov
- mil
- government
- military
- admin
- login
- secure

### Dork Templates
- inurl:php?id=
- inurl:index.php?id=
- inurl:product.php?catid=
- inurl:news.php?id=
- inurl:page.php?id=
- inurl:item.php?id=
- inurl:view.php?id=
- inurl:details.php?id=
- inurl:category.php?id=
- inurl:search.php?q=

## SQL Error Signatures
- mysql_fetch
- SQL syntax
- ODBC
- Warning: mysql
- syntax error
- unclosed quotation mark
- quoted string not properly terminated
- SQL Server
- ORA-
- PostgreSQL
- syntax error at or near
- SQL command not properly ended
- MySQL
- error in your SQL syntax
- Fatal error: Call to undefined function
- ADODB
- DB2
- Oracle error

## License
This project is licensed under the MIT License.

## Contributing
Contributions are welcome! Please open an issue or submit a pull request.

## Support
For support, please open an issue on GitHub.
