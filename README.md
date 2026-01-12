[![Android CI](https://github.com/duminda1/priceconverter/actions/workflows/android.yml/badge.svg)](
https://github.com/duminda1/priceconverter/actions/workflows/android.yml)
[![Coverage](https://codecov.io/gh/duminda1/priceconverter/branch/main/graph/badge.svg)](https://codecov.io/gh/duminda1/priceconverter)

# PocketCurrency  
**Instant currency conversion for travellers**

PocketCurrency is a traveller-friendly Android app that helps you quickly understand what prices really cost in your own currency, online or offline.

Designed for non-technical users, PocketCurrency works anywhere in the world with saved and manual rates, optional realtime updates, and camera-based price scanning for fast conversions while travelling.

---

## Features

- Manual price conversion with quick currency swap
- Saved rate pairs for offline use, with refresh controls
- Manual offline rates for full control
- Live scan with on-device OCR, detected currency hints, and scan-again/manual fallback
- Home and destination defaults with auto-detect and quick overrides
- Rate service selection: Frankfurter daily updates or Custom API (exchangerate.host)
- Realtime toggle, rate-source indicators, stale warnings, and usage alerts for Custom API

---

## Developer Setup Guide

### Requirements

- Android Studio (latest stable): https://developer.android.com/studio
- Android SDK Platform 35 (API 35): https://developer.android.com/studio/releases/platforms
- JDK 11: https://adoptium.net/temurin/releases/?version=11
- Android Emulator or a physical Android device

---

### Clone the Repository

```bash
git clone https://github.com/duminda1/priceconverter.git
cd priceconverter
```

### Open in Android Studio

1. Open Android Studio
2. Select **Open**
3. Choose the `priceconverter` project directory
4. Wait for the Gradle sync to complete

---

### Build from the Command Line (Optional)

Ensure `local.properties` has your Android SDK path (Android Studio creates this file automatically).

```bash
./gradlew :app:assembleDebug
```

---

### Run on an Emulator

1. Open **Device Manager** in Android Studio
2. Create or start an Android emulator
3. Select the emulator as the target device
4. Click **Run ▶**

---

### Run on a Real Device

1. Enable **Developer Options** on your Android phone
2. Enable **USB Debugging**
3. Connect your phone to the computer using a USB cable
4. Select your device in Android Studio
5. Click **Run ▶**

---

### Run Tests

Unit tests:

```bash
./gradlew testDebugUnitTest
```

Instrumentation tests (requires an emulator or physical device):

```bash
./gradlew connectedDebugAndroidTest
```

Coverage report:

```bash
./gradlew jacocoTestReport
```

Lint checks:

```bash
./gradlew lint
```

---

### Release Builds (Optional)

Release builds require TLS pin values for the rate services. Provide them in CI via environment
variables or `-P` Gradle properties:

- `EXCHANGE_RATE_API_PINS`
- `FRANKFURTER_API_PINS`

Firebase App Distribution tasks also require `FIREBASE_APP_ID`.

---

## Rate Services and Updates (Optional)

PocketCurrency works without internet access using saved or manual rates. To enable automatic updates:

1. Open **Settings -> Rate service**
2. Choose a provider:
   - Frankfurter (default): daily rates around 16:00 CET, no API key required. https://www.frankfurter.app/
   - Advanced: Custom API (exchangerate.host): realtime updates with your API key. https://exchangerate.host/
3. For realtime conversions, enable the **Realtime** toggle on the main screen.
4. Saved pairs can be fetched or refreshed in **Settings -> Saved for offline use**.
5. The Advanced provider shows usage, plan limits, and optional alerts in **Settings -> Rate service -> Plan & usage**.

---

## How to Use PocketCurrency

### Convert a Price

1. Enter a price amount
2. Select the source currency
3. Select the target currency
4. Tap **Convert**
5. View the converted amount and rate information

Tip: Use the swap button between the currency fields to flip them quickly.

---

### Scan a Price Using the Camera

1. Enable **Scan** on the main screen (or in **Settings -> Scan prices with camera**).
2. Tap **Start live scan**. The camera stays off until you start.
3. Allow camera permission when prompted.
4. If prompted, tap **Download model** to install OCR (requires internet and Google Play services: https://support.google.com/googleplay/answer/9037938).
5. Point your camera at a price.
6. If no price is detected, tap **Scan again** or **Enter manually** to type the amount.

If a currency symbol or code is detected with high confidence, PocketCurrency updates the **From** currency and shows a "Detected EUR" hint. If it shows "Detected EUR (inferred)", you can override it manually.

---

### Save a Rate for Offline Use

1. Open **Settings -> Saved for offline use**
2. Enter the **From** and **To** currencies
3. Tap **Fetch & Save**
4. Use **Refresh all** when you are online to update saved pairs

---

### Add a Manual Offline Rate

1. Open **Settings -> Offline rates**
2. Enter the **From** and **To** currencies and the rate
3. Tap **Save rate**

---

### Home & Destination Defaults

- Home currency is the default **To** currency
- Destination currency is the default **From** currency
- Auto-detect uses your device country when enabled and can be changed in **Settings -> Home & destination**

---

### Offline Usage

PocketCurrency is designed for travel and works without internet access:

- Saved rates are used automatically
- Manual rates can be entered by the user
- Live scan works after the OCR model is downloaded
- The app shows when a rate was last updated and if it looks stale

---

## Help & Support

### Live vs Saved vs Offline Rates

- **Live updates**: Realtime conversions from Custom API with **Realtime** enabled
- **Saved**: Rates you fetched and stored for offline use
- **Offline**: Manual rates you enter yourself

When realtime is enabled and supported, PocketCurrency tries **Live -> Saved -> Offline**.
Otherwise, it uses **Saved -> Offline**.
The current rate source and last update time are always visible in the app. Tap the info icon next to the status line to learn more about rate sources and stale warnings.

---

### About Price Updates and API Keys

PocketCurrency can automatically update exchange rates using an online price update service.

- The default service is Frankfurter (no API key required): https://www.frankfurter.app/
- Frankfurter updates once per day around 16:00 CET and refreshes saved pairs when you are online
- Advanced users can switch to Custom API (exchangerate.host) for realtime updates and usage tracking: https://exchangerate.host/

To use your own service (exchangerate.host):

1. Open **Settings -> Rate service** and select **Advanced: Custom API (exchangerate.host)**
2. Visit https://exchangerate.host/signup/free
3. Create a free account
4. Copy your API key
5. Paste it into **Settings -> API access**
6. Tap **Verify & Save** to confirm the key
7. Enable **Realtime** on the main screen

If the service is unavailable or the key is invalid, PocketCurrency continues using saved or manual rates.

---

## Accessibility

PocketCurrency is built with accessibility in mind:

- Large, readable text
- High-contrast colors
- Screen reader support
- Clear and simple language

---

## Privacy & Security

- No API keys are stored in source code
- No personal user data is collected
- All data is stored locally on the device
- Camera input is used only for live scanning and is never saved or uploaded

## Backup & Restore

PocketCurrency uses Android Auto Backup and device-to-device transfer to preserve non-sensitive data during device migration. https://developer.android.com/guide/topics/data/backup

What is backed up:
- `price_converter_prefs.xml` (manual rates, saved rates, and user settings)

What is not backed up:
- `price_converter_secure_prefs.xml` (encrypted API keys)

Privacy and UX trade-offs:
- Convenience: users keep manual rates and settings when moving to a new device.
- Privacy: data may be stored in the user's Google backup; users can disable system backups to opt out.
- Security: API keys must be re-entered after restore because encrypted prefs are excluded.

Backup rules are explicit in `app/src/main/res/xml/backup_rules.xml` and `app/src/main/res/xml/data_extraction_rules.xml`, and verified by unit tests.
