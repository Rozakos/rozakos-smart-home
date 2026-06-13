# BLE WiFi Provisioning Protocol

Shared contract between the Rozakos Home Android app and the CYD Stock Ticker firmware.
The app implements the **central** (client); the firmware implements the **peripheral**
(GATT server). Both sides must use the exact UUIDs and JSON shapes below.

App side of record: `app/src/main/java/com/rozakos/home/data/ble/BleProvisioningProtocol.kt`.

## Advertising

- Advertise as connectable BLE peripheral.
- Local name: `CYD-Ticker-XXXX` (suffix = last 2 bytes of MAC, hex).
- Advertise the 128-bit **service UUID** below so the app can filter scans (the app scans
  with a `ServiceUuid` filter and ignores everything else).
- Advertise while unprovisioned, and for ~3 min after boot / on a "setup" button so a user
  can re-provision. Stop advertising once WiFi is connected (optional but recommended).

## GATT service

Service UUID: `5f1d0001-7b3a-4c2e-9d8f-1a2b3c4d5e6f`

| Characteristic | UUID | Properties | Payload |
|---|---|---|---|
| Credentials | `5f1d0002-7b3a-4c2e-9d8f-1a2b3c4d5e6f` | Write, Write No Response | UTF-8 JSON `{"ssid":"..","pass":".."}` |
| Status | `5f1d0003-7b3a-4c2e-9d8f-1a2b3c4d5e6f` | Read, Notify (+ CCCD) | UTF-8 JSON `{"state":"..","ip":"..","reason":".."}` |
| Device info | `5f1d0004-7b3a-4c2e-9d8f-1a2b3c4d5e6f` | Read | UTF-8 JSON `{"name":"..","type":"..","fw":"..","mac":".."}` |

CCCD (notifications) descriptor: standard `0x2902`.

### Status states

- `idle` — booted, no attempt yet.
- `connecting` — credentials received, joining WiFi.
- `connected` — joined; `ip` MUST be the device's DHCP IPv4 address (e.g. `192.168.1.50`).
- `failed` — could not join; `reason` is a short human-readable message.

`ip` is only required for `connected`. `reason` is only required for `failed`.

### Device info fields

- `name` — default display name, e.g. `CYD Stock Ticker` (app pre-fills this).
- `type` — must be `cyd_stock_ticker` (matches the app's `DeviceTypeId`).
- `fw` — firmware version string.
- `mac` — station MAC.

## Flow (app → device)

1. App connects, negotiates MTU (requests 247).
2. App reads **Device info** (best-effort).
3. App subscribes to **Status** notifications (writes CCCD).
4. App writes **Credentials**.
5. Device attempts WiFi, notifying `connecting` → then `connected` (with `ip`) or `failed`.
6. On `connected`, the app saves the device by its reported IP and disconnects.

## Notes

- Support an ATT MTU of at least 247 so a full credentials JSON arrives in one write.
- Persist credentials (NVS / Preferences) and auto-reconnect on boot.
- Treat the write as authoritative: always emit a terminal `connected`/`failed` so the app
  doesn't hang (the app also times out after 90 s).
