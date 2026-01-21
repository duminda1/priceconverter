# PocketCurrency Help

Welcome to **PocketCurrency** 👋
PocketCurrency helps you quickly understand what prices really cost in your own currency while travelling.

It's designed to be simple, reliable, and easy to use even without internet access.

---

## 🌍 What does PocketCurrency do?

PocketCurrency converts prices from one currency to another so you can:

- Shop with confidence
- Avoid overpaying
- Understand prices instantly while travelling

Just enter a price (or scan one), and PocketCurrency does the rest.

---

## 🔢 How do I convert a price?

1. Enter the price you see
2. Choose the local currency (From)
3. Choose your home currency (To)
4. Tap **Convert**

Tip: Use the swap button between the currency fields to flip them quickly.

If you see "No rates available", open **Settings** and save a rate or add an offline rate.

---

## 🏠 Home & Destination Currencies

PocketCurrency uses two defaults to save time:

- **Home currency** is the default **To** currency
- **Destination currency** is the default **From** currency

Home currency is set the first time based on your device region, and you can change it anytime in **Settings**.
Destination currency can auto-detect from your device region, or you can override it manually.

To change auto-detect, open **Settings -> Home & destination** and toggle **Auto-detect destination**.
When it is on, the destination field updates from your device country and cannot be edited.
Turn it off to type your own destination currency.

---

## ✅ Service status

After you convert, PocketCurrency shows a status line so you always know what is being used:

- **Live updates** (realtime)
- **Saved for offline use**
- **Works offline** (manual)

It also shows when the rate was last updated and whether it is stale.
Tap the info (i) icon next to the status line to learn more about the current rate source.

---

## 📷 How does price scanning work?

PocketCurrency can scan prices using your camera.

1. Make sure **Scan** is enabled on the main screen (or in **Settings -> Scan prices with camera**).
2. Tap **Start live scan**. The camera stays off until you start it.
3. Allow camera permission when prompted.
4. If OCR is not ready, tap **Download model** and wait (requires internet and Google Play services: https://support.google.com/googleplay/answer/9037938).
5. Point your camera at a price.
6. If no price is detected, tap **Scan again** or **Enter manually** to type the amount.

If a currency symbol or code is detected with high confidence, PocketCurrency updates the **From** currency and shows a small "Detected EUR" hint.
If it shows "Detected EUR (inferred)", you can override it manually.

Tip: Make sure the price is clear and well-lit for best results.

Live scan uses on-device OCR. The first time you enable it, the OCR model may need to download (internet required) and Google Play services must be available.

---

## 📡 Live, Saved, and Manual Rates

PocketCurrency uses different types of rates depending on what is available.

### Live updates (realtime)
- Available with **Advanced: Custom API (exchangerate.host)**
- Requires a valid API key and the **Realtime** toggle on the main screen
- Most accurate option when realtime updates are enabled

### Saved rates
- Saved from the rate service and stored on your phone
- Used when you are offline or realtime is disabled
- Can be refreshed in **Settings -> Saved for offline use**
- Works both directions (a saved USD->AUD rate can be used for AUD->USD)

### Manual rates (offline)
- Rates you enter yourself in **Settings -> Offline rates**
- Useful if you want full control
- Works both directions (a manual USD->AUD rate can be used for AUD->USD)

If realtime is enabled and supported, PocketCurrency tries **Live -> Saved -> Manual** in that order.
If realtime is off or unavailable, it uses **Saved -> Manual**.
The status line always shows the source and last update time. A "Stale" tag means the rate looks outdated.

---

## ✈️ Can I use PocketCurrency without internet?

Yes. PocketCurrency works offline.

- Saved rates are used automatically
- Manual rates always work
- Live scan works after the OCR model is downloaded
- The app shows when a rate was last updated

---

## 🔄 What are "Price updates"?

Price updates let PocketCurrency refresh rates when you are online.

### Recommended option (easy)
- Use the built-in Frankfurter service (default): https://www.frankfurter.app/
- No API key required
- Daily updates around 16:00 CET
- Best for keeping saved rate pairs up to date

### Advanced option (optional)
Some users prefer to use their own service for realtime updates.

To do this:
1. Open **Settings -> Rate service** and select **Advanced: Custom API (exchangerate.host)**
2. Visit https://exchangerate.host/signup/free
3. Create a free account
4. Copy your API key
5. Paste it into **Settings -> API access**
6. Tap **Verify & Save** to confirm the key
7. Enable **Realtime** on the main screen

When using the Custom API, PocketCurrency shows a usage meter and optional alerts for the free plan (100 requests/month).
You can review usage and toggle alerts in **Settings -> Rate service -> Plan & usage**.

If anything goes wrong, PocketCurrency safely falls back to saved or manual rates.

---

## ⚠️ What if something isn't working?

Here are some common situations:

### No internet connection
- PocketCurrency switches to saved or manual rates automatically

### Realtime toggle is disabled
- The selected rate service does not support realtime, or
- A valid API key has not been saved

### Live scan is off
- Enable **Scan** on the main screen or in **Settings -> Scan prices with camera**

### No price detected
- Hold steady, improve lighting, or tap **Scan again**
- You can always tap **Enter manually** to type the amount

### Invalid API key
- Only applies if you switch to Advanced: Custom API (exchangerate.host)
- The app continues using saved rates
- You'll see a helpful message explaining what happened

### Monthly API limit reached
- Applies to exchangerate.host API keys and free plan usage
- You may need to upgrade your plan or wait for the next billing cycle

### OCR not ready
- Connect to the internet to download the OCR model
- Make sure Google Play services are enabled and up to date: https://support.google.com/googleplay/answer/9037938

### Camera not working
- Check camera permissions
- Make sure Scan is enabled
- If permission is blocked, allow it in your system settings

---

## ♿ Accessibility

PocketCurrency is built to be accessible for everyone:

- Large, readable text
- High-contrast colors
- Screen reader support
- Simple, clear language

---

## 🔐 Privacy & Safety

- PocketCurrency does not collect personal data
- No tracking or analytics
- API keys are never stored in source code
- All data stays on your device
- Camera input is used only for live scanning and is never saved or uploaded

---

## ❤️ Tips for Travellers

- Save rates before flying
- Use Scan while shopping
- Check the "last updated" time for peace of mind
- Use manual rates if you prefer full control

---

## 📬 Need more help?

PocketCurrency is designed to be simple - but if you are ever unsure:

- Check this Help section
- Review the rate source shown on screen
- You're always in control

Safe travels ✈️
