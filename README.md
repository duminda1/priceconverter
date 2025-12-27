# PocketCurrency  
**Instant currency conversion for travellers**

PocketCurrency is a traveller-friendly Android app that helps you quickly understand what prices really cost in your own currency — online or offline.

Designed for non-technical users, PocketCurrency works anywhere in the world, supports saved and manual rates, and includes camera-based price scanning for fast conversions while travelling.

---

## ✨ Features

- Quick currency conversion
- Automatic price updates (optional)
- Offline support using saved or manual rates
- Camera-based price scanning
- Clear rate source indicators (Live / Saved / Manual)
- Simple, travel-friendly user interface

---

## 🛠 Technical Setup Guide

### Requirements

- Android Studio (latest stable version)
- Android SDK 26 or higher
- Kotlin
- Android Emulator or a physical Android device

---

### Clone the Repository

```bash
git clone https://github.com/duminda1/PocketCurrency.git
cd PocketCurrency
```

## Open in Android Studio

1. Open Android Studio
2. Select **Open**
3. Choose the `PocketCurrency` project directory
4. Wait for the Gradle sync to complete

---

## Run on an Emulator

1. Open **Device Manager** in Android Studio
2. Create or start an Android emulator
3. Select the emulator as the target device
4. Click **Run ▶**

---

## Run on a Real Device

1. Enable **Developer Options** on your Android phone
2. Enable **USB Debugging**
3. Connect your phone to the computer using a USB cable
4. Select your device in Android Studio
5. Click **Run ▶**

---

## Price Updates (Optional)

PocketCurrency works without internet access using saved or manual rates.

To enable automatic price updates:

1. Open **Settings**
2. Go to **Price updates**
3. Use the built-in service (recommended), or
4. Add your own API key (advanced option)

---

## 📱 How to Use PocketCurrency

### Convert a Price

1. Enter a price amount
2. Select the source currency
3. Select the target currency
4. Tap **Convert**
5. View the converted amount and rate information

---

### Scan a Price Using the Camera

1. Enable **Scan**
2. Point your camera at a price
3. PocketCurrency automatically detects and converts the value

---

### Offline Usage

PocketCurrency is designed for travel and works without internet access:

- Saved rates are used automatically
- Manual rates can be entered by the user
- The app shows when a rate was last updated

---

## ❓ Help & Support

### Live vs Saved vs Manual Rates

- **Live**: Updated automatically when internet is available
- **Saved**: Last known rate stored on the device
- **Manual**: A rate entered by the user

The current rate source and last update time are always visible in the app.

---

### About Price Updates and API Keys

PocketCurrency can automatically update exchange rates using an online price update service.

- This is optional
- The app works fully without an API key
- Advanced users may provide their own API key

To use your own service:

1. Visit https://exchangerate.host
2. Create a free account
3. Copy your API key
4. Paste it into **Settings → Price updates**

If the service is unavailable or the key is invalid, PocketCurrency continues using saved or manual rates.

---

## ♿ Accessibility

PocketCurrency is built with accessibility in mind:

- Large, readable text
- High-contrast colors
- Screen reader support
- Clear and simple language

---

## 🔐 Privacy & Security

- No API keys are stored in source code
- No personal user data is collected
- All data is stored locally on the device
