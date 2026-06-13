# Rozakos Home

Rozakos Home is a local-first Android device hub for Rozakos ESP32 / IoT projects. The first supported device type is the ESP32-2432S028R CYD Stock Ticker, with the existing firmware WebUI embedded as the working v1 control surface.

## What Works In V1

- Dashboard for saved devices.
- Add-device flow for Stock Tickers.
- Manual IP entry for CYD Stock Ticker devices.
- Local persistence with DataStore.
- Device detail page with status check, room/group, IP, and last-seen state.
- Embedded WebView fallback for `http://<device-ip>/settings`.
- Setup-mode helper for `CYD-Setup-XXXX` and `http://192.168.4.1`.
- Extensible device category/type registry for future Rozakos devices.

## Build And Run

1. Open this folder in Android Studio.
2. Let Android Studio sync Gradle.
3. Select an emulator or physical Android device.
4. Run the `app` configuration.

Command-line build:

```powershell
.\gradlew.bat :app:assembleDebug
```

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
    device/        HTTP status checks, discovery scaffold, CYD API TODO interface
    local/         DataStore persistence
  domain/
    model/         Device, category, type, status models
    repository/    Repository interfaces
    usecase/       Add device use case
  ui/
    adddevice/     Add-device flow
    device/        Device detail screen
    home/          Dashboard
    theme/         Material 3 theme
    webview/       Embedded WebUI screen
```

## Future Integration TODOs

- Add subnet scanning.
- Add mDNS discovery.
- Add firmware endpoint support:

```http
GET /api/device-info
```

Suggested response:

```json
{
  "id": "cyd-stock-abc123",
  "name": "Office Ticker",
  "type": "cyd_stock_ticker",
  "firmwareVersion": "1.0.0",
  "capabilities": ["web_ui", "settings"],
  "webUiPath": "/settings"
}
```

- Add native CYD settings endpoints for symbols, refresh interval, and API bearer token.
- Add more categories such as Sensors, Lighting, Household Security, Weather Stations, and Cleaning Appliances.
- Add optional cloud sync/account support later while keeping local-first behavior.
