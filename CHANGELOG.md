# Changelog


## 0.3.5 — 2026-10-18

- A different device that DHCP gave a stale card's old IP is no longer hidden from Nearby or
  rejected as a duplicate; stable device ids decide when both sides report one.
- Switching devices while a refresh is in flight can no longer show the previous device's data.

## 0.3.4 — 2026-10-17

- Recognize OLED and dot-matrix stock tickers for discovery, live data and native
  watchlist/refresh editing.
- Add distinct device illustrations and the rozakos.com navy, crimson and teal
  palette, including readable success text and matching launcher styling.
- Search companies, funds and crypto by name with a bundled offline directory;
  show names in watchlists and mark results already added. Keep manual entry.
- Protect drafts with a persistent Save/Discard bar and a warning before leaving;
  prevent overlapping saves and retain edits after failures.
- Confirm device removal and move technical metadata into Connection details.
- Replace the refresh slider with exact 15–3,600-second entry and presets.
- Separate Nearby, Manual and New device setup, with field validation, duplicate
  prevention, bounded Bluetooth scans and recovery actions.
- Clarify stale/missing quotes, loading, offline and partial-data states; improve
  web settings history/error recovery and larger-text layouts.

Validation: Android build, lint (zero errors; 34 existing warnings), and 34 enabled
unit tests pass. One opt-in hardware mutation test is skipped. Installed and
checked on Samsung SM-F711B, including 1.5× text. Saved device/ticker settings
were preserved. Factory BLE provisioning and formal TalkBack coverage remain
unverified on hardware. See [the complete review](docs/ui-ux-review-2026-10-17.md).
