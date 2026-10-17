# Rozakos Home

Rozakos Home is a local-first Android hub for Rozakos ESP32 / IoT devices, with
discovery, live data, native ticker controls and access to each device's web UI.
Current version: **0.3.4**. See [release notes](CHANGELOG.md).

## What It Does

- **Device artwork**: distinct illustrations for OLED, touch, STM32, LED matrix
  and follower displays, shared across the dashboard, discovery and details.
  The dashboard uses the [rozakos.com](https://www.rozakos.com/) palette
  (charcoal navy, crimson actions, teal success and light gray surfaces), compact connection badges and
  separate live quote rows with readable price changes.
  Brand colors live in `ui/theme/BrandPalette.kt`; small success text uses a
  darker teal for contrast on light surfaces.
- **Finds your devices by itself**: Add device lists every Rozakos device on the
  phone's WiFi (mDNS `_rozakos._tcp` + a subnet probe of `/api/device-info`).
  One tap adds it. A saved device that moved to a new IP is found again
  automatically.
- **Live dashboard**: device cards grouped by room show online/offline and live
  values (quotes on the tickers, follower counts on the tracker), refreshed every
  30 s.
- **Native controls**: on the CYD Stock Ticker you edit the watchlist (add,
  remove, reorder) and refresh interval in the app; the device's own web page is
  still one tap away for everything else.
- **Company search**: tap **Find a company or symbol** in the watchlist, type
  a name such as Nvidia, Nokia or Bitcoin, and choose the matching symbol.
  Results show the exchange and instrument type; existing entries are marked
  **Already added**. Selection edits your draft; **Save to device** applies it.
  Manual symbol entry remains available in the search picker.
  The bundled directory includes U.S. listings from Nasdaq Trader and a
  snapshot of up to 250 crypto USD pairs from CoinGecko. Search works offline
  on first use; the app attempts a background directory refresh on startup,
  at most once daily,
  keeping its previous directory if sources are unavailable. Searches run
  locally and require no stock API key in the app.
- **Bluetooth setup**: a factory-fresh CYD Stock Ticker gets your WiFi details
  over BLE, no network switching needed.
- **Safer everyday controls (0.3.4)**: drafts have persistent Save/Discard
  actions and a warning before leaving. Removal asks for confirmation;
  refresh intervals accept exact values from 15 to 3,600 seconds.
  Nearby, Manual and New device tabs separate the setup paths, with inline
  validation and recovery actions. See the [full UI/UX review](docs/ui-ux-review-2026-10-17.md).
- **Supported devices**: CYD Stock Ticker, STM32 (NUCLEO) Stock Ticker, CYD
  Follower Tracker, OLED Stock Ticker and Dot-Matrix Stock Ticker. Both new
  tickers support discovery, live prices and native watchlist/refresh editing.
  Their WiFi setup network is `Ticker-Setup-XXXX`. The protocol is in
  [docs/rozakos-local-protocol.md](docs/rozakos-local-protocol.md); a new
  device type needs a registry entry and that contract, nothing else.

## Build And Run

1. Open this folder in Android Studio.
2. Let Android Studio sync Gradle.
3. Select an emulator or physical Android device.
4. Run the `app` configuration.

Command-line build and tests:

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest
```

The unit tests cover the protocol parsing (with the exact JSON the firmware
serves), discovery merging, subnet enumeration and the HTTP client against a
local fake device.

If Java is not on your shell `PATH`, Android Studio can still build the project. On this machine, the project was verified with Android Studio's bundled JDK.

## Using The App

1. Tap **Add device**.
2. Use **Nearby** to discover and add a device on the same Wi-Fi.
3. Alternatively, choose **Manual**, select the device type and enter its IP
   address, for example `192.168.1.50`, then tap **Add to home**.
4. Open the device card to see live data and edit its watchlist.
5. Use **Open device settings** for additional firmware controls.

The app loads the existing firmware admin UI at:

```text
http://<device-ip>/settings
```

The firmware WebUI is not removed or replaced.

## Setup Mode

Bluetooth support depends on the firmware; Android pairing is not required.

| Device | Initial network setup |
| --- | --- |
| CYD Stock Ticker | In-app Bluetooth provisioning, or `CYD-Setup-XXXX` Wi-Fi portal |
| CYD Follower Tracker | `CYD-Setup-XXXX` Wi-Fi portal |
| OLED / Dot-Matrix Stock Ticker | `Ticker-Setup-XXXX` Wi-Fi portal; current firmware has no BLE provisioning |
| STM32 Stock Ticker | Ethernet DHCP, or configure its ESP-01 Wi-Fi separately |

For the OLED or matrix ticker:

1. Power on the device. With no saved Wi-Fi, or after failing to connect to its
   saved network at boot, it opens `Ticker-Setup-XXXX`.
2. Choose **Add device > New device > OLED Stock Ticker** (or matrix).
3. Tap **Open Wi-Fi settings**, join `Ticker-Setup-XXXX`, and stay connected
   even though Android says the network has no internet.
4. Return to Home and tap **Open setup portal** (`http://192.168.4.1`).
5. Enter a 2.4 GHz home Wi-Fi network and password. After connecting the device,
   reconnect the phone to home Wi-Fi.
6. Use **Nearby** to find and add it, or **Manual** with its local IP address.
7. If an API key has not already been provisioned, enter a per-device stock API
   key in **Device web settings**. The key is write-only.

For CYD Wi-Fi portal setup:

1. Power on the CYD Stock Ticker.
2. Join the `CYD-Setup-XXXX` WiFi network from Android WiFi settings.
3. Return to Rozakos Home.
4. Open **Add device > New device**, select its type and open the setup portal.
5. Configure home WiFi on the device.
6. Add the device by its new local IP address.

For CYD Bluetooth setup, choose **Start Bluetooth setup** inside **New device**.
Allow the requested Bluetooth permissions and select the setup-ready board in
the app. Home connects directly over BLE and sends the Wi-Fi credentials using
the [provisioning protocol](docs/ble-provisioning-protocol.md). Do not pair it in
Android's Bluetooth settings. Once it joins Wi-Fi, discovery and normal controls
use the local network.

## Project Structure

```text
app/src/main/java/com/rozakos/home/
  data/
    ble/           BLE WiFi provisioning (central side)
    device/        LAN discovery (mDNS + subnet probe), status checks, repository
    local/         DataStore persistence
    protocol/      Rozakos local device protocol: models, parsers, HTTP client
  domain/
    model/         Device, category, type registry, status models
    repository/    Repository interfaces
    usecase/       Add device use case
  ui/
    adddevice/     Nearby devices + manual add
    components/    Live state rows (quotes, followers)
    device/        Device detail, live data, watchlist editor
    home/          Dashboard grouped by room
    onboarding/    Bluetooth setup flow
    theme/         Material 3 theme
    webview/       Embedded device web page
```

## Next Ideas

- Native settings for the Follower Tracker (accounts) and the STM32 ticker
  (needs a JSON settings endpoint in their firmware first).
- mDNS on the STM32 ticker (LwIP's mdns app) so it shows up without the subnet probe.
- Home-screen widget with the live quotes.
