# Rozakos local device protocol (v1)

How Rozakos Home finds and talks to Rozakos devices on the home network. Plain
HTTP on port 80, JSON bodies, no cloud and no account. Read-only endpoints need
no auth (they expose only what is already on the device's screen).

Implemented by:

| Device | `type` | Discovery | Endpoints |
|---|---|---|---|
| CYD Stock Ticker (`Rozakos/CYD-Stock-Ticker`) | `cyd_stock_ticker` | mDNS + subnet probe | device-info, state, settings (GET/POST) |
| CYD Follower Tracker (`Rozakos/CYD-Following-Tracker`) | `cyd_follower_tracker` | mDNS + subnet probe | device-info, state |
| STM32 Stock Ticker (`Rozakos/NUCLEO-STOCK-TICKER`) | `nucleo_stock_ticker` | subnet probe (no mDNS on LwIP build) | device-info, state |

BLE WiFi provisioning (unprovisioned CYD Stock Ticker) is a separate contract:
[ble-provisioning-protocol.md](ble-provisioning-protocol.md).

## Discovery

1. **mDNS / DNS-SD**: ESP32 devices advertise `_rozakos._tcp` on port 80 with TXT
   records `id`, `type`, `name`, `fw`, `mac`, `path`, and the hostname
   `<id>.local` (e.g. `cyd-ticker-ab12.local`).
2. **Subnet probe**: the app also sends `GET /api/device-info` to every address
   of the phone's /24 (48 in parallel, 600 ms connect timeout) for devices
   without mDNS and for networks that drop multicast.

Every candidate is confirmed with `/api/device-info` before it is listed. The
app stores the device's `id`; when a saved device stops answering it looks the
`id` up over mDNS again, so a DHCP address change heals by itself.

## `GET /api/device-info`

```json
{
  "id": "cyd-ticker-ab12",
  "name": "CYD Stock Ticker",
  "type": "cyd_stock_ticker",
  "fw": "1.0.0",
  "mac": "24:6F:28:AA:AB:12",
  "webUiPath": "/settings",
  "capabilities": ["web_ui", "settings", "settings_api", "state"]
}
```

- `id`: stable, unique per board (derived from the MAC / MCU UID), lowercase.
- `type`: one of the types above. The app ignores devices with unknown types.
- `mac`: optional. `webUiPath`: the device's own settings page.
- `capabilities`: `web_ui` (has a web page), `state` (`/api/state`),
  `settings_api` (`/api/settings`).

## `GET /api/state`

Stock tickers:

```json
{
  "quotes": [
    {"symbol": "AAPL", "price": 231.40, "change_pct": -1.25, "fresh": true, "ext_price": 232.10},
    {"symbol": "NVDA", "price": null, "change_pct": null, "fresh": false}
  ],
  "market_state": "POST",
  "updated": 1791580000
}
```

`price`/`change_pct` are regular-session values (`null` until the first quote
arrives); `ext_price`/`ext_change_pct` appear during pre/after-hours.
`market_state` is `PRE`, `REGULAR`, `POST` or `CLOSED` (absent when unknown).

Follower tracker:

```json
{
  "accounts": [
    {"platform": "youtube", "handle": "@mkbhd", "display_name": "Marques Brownlee",
     "followers": 19200000, "delta": 250000}
  ],
  "range": "1w",
  "updated": 1791580000
}
```

`delta` is the change over the device's list window (`range`); `followers` is
`null` until the first successful fetch.

## `GET /api/settings` / `POST /api/settings` (stock ticker)

```json
{"symbols": ["AAPL", "NVDA"], "refresh_s": 60, "max_symbols": 16, "has_api_key": true}
```

POST takes any subset of `{"symbols": [...], "refresh_s": N, "api_key": "..."}`.
`symbols` replaces the watchlist in the given order (1..`max_symbols`, each
1-12 chars of `A-Z 0-9 . - ^`); `refresh_s` is 15..3600. The API key is
write-only and never returned. Errors are `400 {"error": "message"}`, and the
app shows the message as is.
