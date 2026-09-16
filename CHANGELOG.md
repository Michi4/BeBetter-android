# Changelog

## v1.0.0 — 2026-09-16 (rebuild 6, phone versionCode 7 / wear versionCode 3)

Dedicated-emulator audit of every screen (uianalyzer geometry pass: overlaps/clipping/touch targets) plus functional end-to-end tests on the demo account.

### Fixed
- **Bottom-nav Home** — from Habits/Profile/Assistant, tapping Home sometimes landed on Profile or did nothing (`restoreState` resurrected stale back-stack state). Home now pops back to a fresh Dashboard.
- **Habits FAB** — `FabPosition.Center` was only set on the dashboard; the Habits screen FAB sat on the right overlapping the history calendar. Now centered on both.
- **Assistant quick prompts** — the three suggestion chips overflowed the 360dp row, the third chip was clipped under the Send button. Now a wrapping `FlowRow`.
- **Task ⋮ menu (web parity)** — the web TaskCard has Edit / Convert to Habit / Delete; Android had none of them:
  - Edit opens an inline dialog (title/description/due date/time → `PUT /tasks/{id}`), verified live on demo (`PUT` returns 200).
  - Delete with confirmation dialog (verified the full flow; the demo server answers 403 "Not available in the demo account…" by design).
  - Convert to Habit deletes the task then opens the create sheet prefilled in habit mode.
  - All task mutations now surface the server error message inline under the row instead of failing silently (previously `runCatching` swallowed the 403).
- **"Add another time" dead in create sheet** — the HH:MM input only rendered in non-anytime mode while adding required a valid typed time, so the button was a no-op in the default state. The input is now always visible (auto-syncs the Anytime chip), duplicates are rejected, removing all times with ✕ restores Anytime. Add + ✕-delete verified.
- **Dashboard grid day click (web parity)** — tapping a contribution-grid cell opens the day-detail dialog (`GET /grid/day`: weekday title, scheduled habits ✅/⬜, logs, tasks, vacation note) like the web DayDetail modal.
- **Scroll clearance** — Dashboard/Habits list padding raised to 320dp so max-scroll audits clear the centered FAB.

### Verified (no changes needed)
- Full habit-mode create sheet (schedule presets, weekday pills, reminders, preset toggle, buddies, challenges), edit-habit sheet, habit detail, preset detail, login/register/forgot-password forms (registered `qaaudit10` end-to-end via UI, empty-state screens clean, account deleted via API afterwards), Notifications/Profile/Settings including Danger Zone. Task-row edit and habit-row navigation both work (earlier tap failures were stale dump coordinates, not app bugs).

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
