# Android Frontend (Jetpack Compose)

## What this app does

- Simple chat UI that talks to the FastAPI backend.
- Streams model output and updates the last “AI” bubble as chunks arrive.
- “Info” screen lets you toggle **English/Spanish** for the app language (sent to the backend with each question).

---

## Requirements

- Android Studio
- JDK 17
- SDK 24 to SDK 35
- Backend running at:
  - HTTP: `http://10.0.2.2:3000/ask` (Android emulator loopback to host)
  - HTTPS (with certs): if the backend is started with TLS (e.g. `10.0.2.2+1.pem` / `10.0.2.2+1-key.pem` via mkcert):
    - Trust your mkcert CA in the emulator with a [network security config](https://developer.android.com/training/articles/security-config)
> If your backend isn’t on the same machine or runs on a different port, update the URL in `fetchLLMStream`.

---

## Project structure (high-level)

```
app/
  src/main/java/com/example/civilrightshelper/
    MainActivity.kt            # App entry + navigation state (chat <-> info), ChatScreen, InfoScreen,fetchLLMStream
  src/androidTest/...          # Instrumentation/UI tests (Compose tests)
```

---

## Key components

### ChatScreen
- Top bar with title and **Info** icon.
- Scrollable message list with “user” (right) and “ai” (left) bubbles.
- TextField + **Send** button.
- On send:
  1. Adds a user bubble,
  2. Adds an empty ai bubble,
  3. Launches `fetchLLMStream(...)` which appends text as it arrives,
  4. Replaces last ai bubble text on each chunk.

### InfoScreen
- Shows a short disclaimer.
- Lets you switch language (**English** / **Spanish**) using radio buttons.
- Back arrow returns to chat.

### `fetchLLMStream(userMessage, language, client, onChunk)`
- Builds a JSON request:
  ```json
  {"query":"...", "language":"..."}
  ```
- POSTs to `http://10.0.2.2:3000/ask` with OkHttp.
- Reads the response stream and calls `onChunk(chunk)` for each piece.
- Stops when it sees the terminator `[[END_OF_STREAM]]`, which the backend sends as the last chunk.

Default OkHttp client:
```kotlin
OkHttpClient.Builder()
  .connectTimeout(60, SECONDS)
  .readTimeout(0, SECONDS)   // no timeout for streaming
  .writeTimeout(60, SECONDS)
  .build()
```

---

## Running the app

1. Start the backend locally ([see backend docs](https://github.com/Osbaldo-Arellano/CivilRightsHelperBackend)).
2. In Android Studio, select an emulator.
3. Run the app.  
   Type a question, press 'Send', you should see an AI bubble grow as text streams in.

> `10.0.2.2` is Android’s special host endpoint. If you’re on a physical device or remote, update the URL.

---

## Local tests

### UI tests

Included tests (examples):
- `MainActivityTest.chatScreen_displaysTitle()`  
- `MainActivityTest.clickingInfoIcon_navigatesToInfoScreen()`  
- `MainActivityTest.infoScreen_selectsSpanishLanguage()`  
- `MainActivityTest.sendingMessage_addsUserBubble()`  
- `MainActivityTest.backFromInfo_returnsToChat()`  

An test for streaming:
- `FetchLLMStreamInstrumentedTest.fetchLLMStream_streams_chunks_until_end()`  
  (mocks OkHttp `Call` to return a fake streaming body and verifies collected chunks)

---

## GitHub Actions (CI)

Minimal workflow using **emulator.wtf** (cloud emulators):

```yaml
name: Android tests

on:
  push:
    branches: [ main ]
  pull_request:

jobs:
  tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: "17"

      - name: Make gradlew executable
        run: chmod +x ./gradlew

      # Build app + androidTest APKs
      - name: Build app & androidTest APKs
        run: ./gradlew :app:assembleDebug :app:assembleAndroidTest --stacktrace

      # Run instrumentation tests in emulator.wtf
      - name: Run instrumentation tests (emulator.wtf)
        uses: emulator-wtf/run-tests@v0
        with:
          api-token: ${{ secrets.EW_API_TOKEN }}  # add in GitHub repo Settings > Secrets
          app: app/build/outputs/apk/debug/app-debug.apk
          test: app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
          outputs-dir: build/ewtf-results

      # Always publish artifacts (videos/logs/junit)
      - name: Upload test outputs
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: android-test-outputs
          path: build/ewtf-results
```

Create the secret `EW_API_TOKEN` in your repo settings from your emulator.wtf account.

---

## Dependencies

Dependencies :
```kotlin
    // Main app OkHttp
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation(libs.androidx.ui.test.junit4.android)

    // Local JVM unit tests
    testImplementation(libs.junit)
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.11.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.1.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")

    // Android instrumented tests
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation("org.mockito:mockito-android:5.11.0")
    androidTestImplementation("org.mockito.kotlin:mockito-kotlin:5.1.0")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")

    // Frontend test
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Debug tools
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
```

---
