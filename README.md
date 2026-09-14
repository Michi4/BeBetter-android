# BeBetter for Android (+ Wear OS)

Native Android app for [BeBetter](https://github.com/Michi4/BeBetter) — habit tracking, tasks, social battles, year grid. Fully API-compatible with `https://app.bebetter.websters.at`.

Designed to look exactly like the web app (emerald `#34d399` on `#0b0c0f`, same cards/inputs/nav), tidied up for mobile.

## Phone app (`:app`)
- Auth: login, register, demo, forgot/reset, session persist, server URL switch
- Today dashboard: stats row, year grid, quick create, Overdue/Now/Upcoming buckets (30s live clock)
- Habits: CRUD, schedules, reminders, honor/photo/BeBetter-Cam proof (gallery upload), pause/resume, finish, delete
- Tasks: quick-add, complete/uncomplete, due filtering
- Year grid: GitHub-style heatmap with month labels + today ring + legend
- Social: friends search/add/requests, invite links, head-to-head battles, leaderboard
- Presets: browse, like, fork, use
- Notifications: inbox + preferences (local reminders via WorkManager replace web-push)
- Assistant: AI chat (when enabled) + sessions info
- Profile: bio, public flag, avatar upload, vacation mode
- Admin: stats, user search, announcements, reports (role-gated)
- Extras: light/dark/system theme (like web), **keep-screen-on** (phone never sleeps in app, toggle in Settings), home-screen widget, deep links (`https://bebetter.websters.at/*`, `https://app.bebetter.websters.at/*`, `bebetter://*`)

## Watch app (`:wear`) — Pixel Watch 4 / Wear OS
- Today list with one-tap complete, stats, assistant chat
- Tile: due/total + streak, refreshes every 10 min
- Complication: `🔥 streak` (SHORT_TEXT + RANGED_VALUE) for any watch face
- Same backend, own login; keep-screen-on while open

## Build
```bash
export ANDROID_HOME=~/Android/Sdk
./gradlew :app:assembleDebug :wear:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Release APKs (phone + watch) are published under GitHub Releases.

## Config
Default server: `https://app.bebetter.websters.at` (change in Login → Server or Settings).
