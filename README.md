# Rozakos Home

Rozakos Home is a local-first Android device hub for Rozakos ESP32 / IoT projects. The first supported device type is the ESP32-2432S028R CYD Stock Ticker, with the existing firmware WebUI embedded as the working v1 control surface.

## What It Does

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
- **Bluetooth setup**: a factory-fresh CYD Stock Ticker gets your WiFi details
  over BLE, no network switching needed.
- **Supported devices**: CYD Stock Ticker, STM32 (NUCLEO) Stock Ticker, CYD
  Follower Tracker. The protocol is in
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
2. Keep **Stock Tickers** selected.
3. Enter the CYD Stock Ticker IP address, for example `192.168.1.50`.
4. Save the device.
5. Open the device card and tap **Open settings**.

The app loads the existing firmware admin UI at:

```text
http://<device-ip>/settings
```

The firmware WebUI is not removed or replaced.

## Setup Mode

If the ticker is not on the home network:

1. Power on the CYD Stock Ticker.
2. Join the `CYD-Setup-XXXX` WiFi network from Android WiFi settings.
3. Return to Rozakos Home.
4. Open the setup portal from the Add Device screen.
5. Configure home WiFi on the device.
6. Add the device by its new local IP address.

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
