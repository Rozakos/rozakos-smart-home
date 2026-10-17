# Rozakos Home UI/UX review — 17 October 2026

The highest-value changes are protecting edits, making setup easier to understand,
and keeping everyday controls ahead of technical information. Keep the
rozakos.com palette, device illustrations and local company-name search.
The changes below are included in Home 0.3.4.

## Findings and changes

| Priority | Finding in 0.3.3 | Change |
| --- | --- | --- |
| High | Remove immediately deleted a saved device. | A named confirmation explains that the physical device keeps its settings. Cancel leaves the device untouched. |
| High | Back could discard watchlist edits without warning. | Both toolbar and system Back ask before discarding. Back during a save explains that saving must finish. |
| High | Save was below a potentially long watchlist, and repeated taps could start overlapping writes. | A persistent Save/Discard bar appears for drafts. Saves are single-flight; edits freeze during saving, and failures retain the draft for retry. |
| High | The refresh slider only reached 600 seconds although settings support 3,600, and a scroll gesture could change its value. | A precise interval dialog accepts 15–3,600 seconds, with quick presets and inline validation. |
| Medium | IP, firmware and device ID appeared ahead of useful controls; long values could overrun their row. | Connection details are collapsed below the watchlist. Values wrap in bounded columns. Quotes and controls are reached sooner. |
| Medium | Saved watchlist entries only showed symbols. | Known symbols show their company/fund/crypto names. Find a company is placed before the list so a long watchlist does not bury it. |
| Medium | Discovery, manual entry, device types, Bluetooth and AP setup occupied one long page. | Nearby, Manual and New device are separate tabs. Only supported categories appear. Wi-Fi settings and discovery have direct actions. |
| Medium | A scan with all devices already saved looked like nothing had been discovered. | The screen explains how many discovered devices are already in the home. Scanning can be stopped, and failures point to Wi-Fi checks and Manual. |
| Medium | Manual/discovered saves had no busy state, duplicate check or persistence-error recovery. | Busy states prevent repeat taps; UID/IP duplicates are rejected; field errors appear beside the relevant input; failed saves can be retried. |
| Medium | Old or absent quote data was insufficiently distinguished from current data. | Detail and dashboard rows explicitly label stale or missing quotes. Stale changes are muted, and empty data has an explanation. |
| Medium | Initial empty UI states could briefly show “Device not found” or an empty home during loading. | Loading states are distinct from genuinely missing/empty states. Refresh failures have recovery messages. |
| Medium | Bluetooth denial had no explanation; returning from settings did not refresh readiness; scanning could continue indefinitely. | Permission recovery links to app settings, readiness refreshes on resume, scans finish after a bounded window, and no-result messages explain the next step. |
| Medium | Provisioning could be abandoned accidentally, and a disconnect could be mislabeled as a timeout. | Leaving during provisioning asks to stop; actual failure reasons survive. Wi-Fi credentials remain available for retry. Password visibility and password-appropriate keyboard settings help prevent typing errors. |
| Medium | Web errors appeared over the browser’s own error page; Android Back ignored web history. | A solid recovery screen replaces the failed page, handles HTTP errors, and offers Retry. Back traverses web history first; WebViews release resources when closed. |
| Medium | Selection relied on color alone and dense quote rows became cramped with larger text. | Device type selection also has a checkmark and accessibility state; larger-text quote layouts stack values; actions have descriptive labels. |
| Low | Last-seen text was a long absolute timestamp; the empty home repeated Add actions. | Recent timestamps are relative, and the empty state has one primary Add action. Background polling pauses when the screen is not started. |

## Review coverage

- Dashboard, room grouping, empty/loading states, device menus, rename and removal.
- Device detail hierarchy, live prices/followers, missing/stale data, offline and
  partial-data recovery, room assignment, native watchlists and connection details.
- Search, no results, already-added results, manual fallback, draft selection,
  interval entry, save failures and leaving with changes.
- LAN discovery, already-known devices, manual validation, duplicate prevention
  and first-time device setup.
- Bluetooth scan/permissions, credentials, progress, cancellation, failure and
  successful device registration.
- Embedded web settings, history, loading, errors and resource cleanup.
- Brand consistency, text contrast, readable labels, keyboard choices, touch
  targets, selected-state semantics and larger text.

## Validation

The Android build and lint passed: zero lint errors and 34 existing warnings.
All 34 enabled unit tests passed; one opt-in hardware test was skipped. Tests exercise
search and draft races, save failures, duplicate prevention, invalid fields,
storage failures, scan errors, Bluetooth timeout/permission/failure recovery and
relative timestamps. The opt-in test that writes to physical ticker settings is
excluded from this review run.

Home 0.3.4 is installed on the Samsung SM-F711B. Phone checks confirmed removal
confirmation (cancelled), collapsed connection details, a 3,600-second interval
draft, the Back warning, and the Nearby/Manual/New device tabs. The review found
and corrected navigation-bar overlap in the persistent save controls, and added
an explicit accessibility label to the floating Add device action.
The corrected Save/Discard bar was checked above Android's navigation buttons;
Discard cleared the test draft. Empty manual IP entry produced its inline
validation message without adding a device.
Dashboard and device details were inspected at 1.5× system text size: titles
wrap and quote values stack. The original 1.0× font setting was restored.
The ticker's complete settings response matches the pre-review snapshot.
Screenshots are saved in `Documents/backups/stock-ticker-2026-10-17/home-review-*.png`.

Actual factory Bluetooth provisioning requires a setup-ready CYD board and is not verified on
hardware in this review. Empty-home and network-outage behavior are assessed
through source review and mocked failures rather than deleting the user's data
or disconnecting the home network. This is not a formal screen-reader audit.

## Further product improvements

These are optional follow-up work, not blockers for the reviewed update:

- Native follower-account controls would make the Follower Tracker as convenient
  as the stock tickers; account editing still uses the device web page.
- A system-aware dark theme needs its own contrast and illustration treatment.
- With a larger device collection, add room/status filters and device search.
  The current room grouping is sufficient for the small collection reviewed.
- Test TalkBack end-to-end and the folded/landscape layouts on additional phones.
  The current review does not establish device-wide accessibility certification.
