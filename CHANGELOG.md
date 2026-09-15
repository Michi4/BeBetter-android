# Changelog

## v1.0.0 — 2026-09-15 (rebuild 5, phone versionCode 6 / wear versionCode 3)

### Fixed
- **Phone top bar** — logout removed from the action row (it lives in Profile), icons sized to 40dp with 48dp touch targets, no visual overlaps.
- **Phone FAB** — floats fully above the nav bar (`bottom 48dp`), dashboard scroll clearance raised to 220dp.
- **Wear app fully rebuilt** (was a broken 391-line skeleton): wear-native login (no more overlapping M3 `TextField`s), Today screen with **tap-to-toggle habit completion** (real `POST /logs` / `DELETE /logs/habit/{id}`), **live streaming assistant** over SSE (verified end-to-end on Pixel Watch), stats screen, working Protolayout tiles (Today + Assistant with `LaunchAction` deep links via `?screen=` extras), streak complication kept.
- **Wear assistant off-by-one** — history payload now includes the current user message (empty `messages` caused server 500 → "Something went wrong").

## v1.0.0 — 2026-09-14 (rebuild 4, versionCode 5)

Pixel-perfect 1:1 parity with the web PWA (`app.bebetter.websters.at`).

### Fixed
- **Nav bar no longer cut off** — proper edge-to-edge insets; system nav bar area is padded, never overlaps content.
- **Tasks are movable** — up/down buttons on every task, live reorder via `POST /tasks/reorder` (same behavior as the web app on mobile).
- **Checkboxes match web** — unchecked habits/tasks are now empty outline circles/boxes; the check mark only appears when done. Completed task titles get the web strike-through.
- **Year grid is 1:1 with web ContributionGrid** — Monday-start weeks, 10px cells, 3px gaps, absolute-positioned month labels, Wed/Sat day labels, today ring, auto-scroll to today, Less/More legend with the exact web emerald/gray/vacation colors.
- **Assistant actually works** — talks to the real streaming SSE endpoint (`/api/assistant/chat`) instead of the broken JSON call; live streaming replies, chat history/`sessionId` continuation, friendly 403/429/5xx errors. Demo accounts get the web "sign up to use this" message (by design).
- **History screen is real** — replaced the fake mock calendar with the actual server-backed month grid (`GET /grid`, `GET /grid/day`): per-day habit completion, prev/next month, Today button, progress bar — exactly the web color scale.
- **Settings now reachable and 1:1 with web Profile.vue** — avatar in the top bar → Profile → Settings hub: theme chips, keep-screen-on, full notification prefs (master switch + morning/evening/habit reminders/announcements with web pill switches), AI assistant settings, vacation mode, change password, server URL, Danger Zone (delete account), logout.
- **FAB** — centered above the nav bar (web `bottom-[calc(4rem+…)]`), 56dp rounded-2xl emerald-600.
- **"Show more (n remaining) / Show less"** task collapse like the web dashboard.

### Changed
- `versionCode` 5.
- Gradle wrapper (`gradlew`, wrapper jar) committed — repo builds out of the box.

## Earlier builds
- v1.0.0 rebuild 1–3: initial 1:1 mobile restyle (cards, inputs, sections), premium create sheet, history + grid first pass — superseded by the fixes above.
- Initial release: full API-compatible phone + Wear OS client for BeBetter.
