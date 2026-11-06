# Salatuk teardown — `com.masarat.salati` v3.7.17 (2026-11-06)

Donor for the **SalatukPlus** rebuild (`~/WorkingOn/SalatukPlus/`). Everything below is
static observation from the shipped APK; runtime behavior stays unverified. Observations
vs inferences kept separate; unverified claims marked (VERIFY).

## 0. Identity & provenance (REA Evidence)

- Label **Salatuk**, package `com.masarat.salati`, developer Masarat (masaratapp.com;
  social: facebook.com/salatukapp, twitter/salatukapp; iOS twin on the App Store,
  id686077611).
- **v3.7.17 (versionCode 383)**, minSdk 21, targetSdk 34, platform build 14.
- Pulled read-only from POCO vayu (`35d5637e`) on 2026-11-06 (installed 2023-05-02,
  updated 2025-07-11).
- Artifact: `~/WorkingOn/SalatukPlus/donor/com.masarat.salati-3.7.17.apk` — 33,957,007 B,
  sha256 `14e92319044cd657377a5b93417a13c449503e931e2ac4278e36323c32c10397`.
- **REA Evidence ID `ev_ea7cc79a942f20962eef69c1dd1f8799520c85f3afe4ac428efdc29888e898eb`**
  (inspect_android_package, complete Evidence at
  `SalatukPlus/donor/rea-evidence/inspect-evidence.json`; summary projection
  `ev_37a68b3e…4763c43`). Engine: jadx-headless-mcp **0.7.1** (sha256
  `6e5eacf5…7feb573ff75` = REA's audited release). Coverage: manifest + component
  inventory **complete**; **3,515 classes**, **1,311 resources**; signature verification
  not performed; APK never executed.
- Decompiles (jadx 1.5.6, 4 minor class errors — negligible):
  `SalatukPlus/donor/extracted-code/` (sources) + `extracted-res/` (resources).
- Obfuscation: R8 on — helper classes renamed to `a`, `b0`, `x4.a`…; the main package
  `com.masarat.salati.*` kept its real names (131 files), which is why we can map
  everything below.

## 1. Architecture shape

Classic Android **Views** app (no Compose), Java with embedded Kotlin modules (v5/x5/t5
engine is Kotlin), `j$` desugared `java.time` for minSdk 21. RTL-first: custom
`TextViewAR`/`RadioButtonAR`/`CheckedTextViewAR`, own `RtlViewPager`. App class
`SalatiApplication` force-sets locale + clamps fontScale at 1.2, uses legacy
`MODE_MULTI_PROCESS` (mode 4) shared prefs, `largeHeap=true`,
`defaultToDeviceProtectedStorage=true`, `allowBackup=false`.

Main UI = 3 tabs (`HomeFragment` / `PrayersFragment` / `RemindersFragment`) in
`MainActivity` + a fleet of single-purpose activities (Qibla, Calendar, Location,
SoundPicker, theme/dark-mode/transparency, settings tree).

## 2. The prayer-time engine (the crown jewel)

- Config class `t5.a`: default method **`"makkah"`**, fields for coords (`w5.b`),
  `PrayerPreference` (`v5.d`), asr method (`x5.a`), high-lat rule (`x5.c`), format
  (`x5.e`).
- `PrayerPreference(fajrAdjust, sunriseAdjust, dhuhrAdjust, asrAdjust, maghrebAdjust,
  ishaAdjust, isDSTOffset, isRamadan, calcMethod, asrMethod, highLats, prayerFormat,
  showDebugLogs)` — per-prayer minute adjustment + DST + Ramadan-mode switches.
- Entry `t5.a.a(String method, double[] coords, PrayerPreference)` → `double[]` times.
- **Method table (18+):** makkah, mwl, isna, egypt, karachi, kuwait, qatar, dubai,
  emirates, jordan, syria, libya, tunisia, algeria, turkey, malaysia, tehran… (strings
  in `t5.a` + `q5.n`). PrayTimes-style: julian-day solar math (`Math.asin/acos`, julian
  term present).
- The engine is **fully offline** — prayer times are computed on-device; no API call
  anywhere in the app (see §6).

## 3. The "ministry-correct" data layer — `assets/databases/SalatukDB_v7.zip`

26.5 MB zip → `SalatukDB.db` (SQLite, user_version 151), copied to
`filesDir/.salatuk/SalatukDB_v7` (MainActivity.java:126). Tables:

| table | content |
|---|---|
| `cities` | **218,032 cities**: name EN + Arabic, lat, lon, country_code, city id, timezone, …, default `calc_method` per city (e.g. `mwl`) |
| `countries` | country_code/name/continent/languages + **Arabic + French names + default calc_method** |
| `timezone` + `zone` | full historical tz table (zone_id, abbreviation, time_start, gmt_offset, dst) — app carries its **own tz database**, doesn't trust ICU (good for old Androids) |
| `staticprayertimes` | precomputed times per (method, city, date) — e.g. `emirates/abudhabi/20140101 = 05:44 07:03 12:28 15:28 17:49 19:07`. Legacy offline fallback + **verification corpus** |
| `qibla` | per-method **seasonal minute-correction table**: rows keyed (m=method id, mn=method name, c=city?, b/e=MMDD window like `401`→`430` = Apr 1–30, `601`→`605` = Jun 1–5), per-prayer correction columns fa/ms/mv/is/iv/fm/sm/dm/am/mm/im (fm=−3.0, dm=−1.0 on the UAE row) — this is how Salatuk matches official ministry timetables that shift seasonally (VERIFY: exact column semantics not fully decoded) |
| `day_events` | Islamic-calendar events (ar/en/fr) |
| `prayer_events` | per-prayer contextual duas/azkar, indexed by prayer + day, with priority (e.g. waking dua "الحمد لله الذي أحيانا…") |
| `prayer_times_compare_libs` | cross-library comparison data (VERIFY usage) |

This DB is the moat: an algorithm alone does not match ministries; **algorithm +
seasonal-correction table + per-city defaults** does.

## 4. Adhan / alarm machinery

- **Bundled audio packs** `assets/adhans/`: `standard/`, `haramayn/` (Mecca+Medina
  muezzins), `maghrebi/` (each has two takes `01/02`), plus `reminders/`: shahada clips
  (default/Makkah/Madina variants), "حَيّ على الصلاة" Fajr-variant clips, iqama clips
  (haram full/short, nabawi full/short), bird sounds, ringtones.
- `AdhanService` (foreground dataSync): MediaPlayer + MediaSession + audio focus;
  **pauses adhan when a call starts** (`TelephonyCallback`/legacy `PhoneStateListener`
  pair); **SensorEventListener** (shake = stop/snooze (VERIFY which)); `MediaSession`
  callbacks for hardware buttons; `MediaBroadcastReceiver` listens to
  `MEDIA_BUTTON` + **Bluetooth ACL connected/disconnected** (that's why `BLUETOOTH`
  permission — adhan keeps routing correctly when headsets appear/vanish).
- `AdhanAlarmActivity`: full-screen over lock screen (`showOnLockScreen`,
  `taskAffinity=""`, `excludeFromRecents`), custom `SwipeButtonView` slide-to-dismiss.
- `SilentService`: flips `NotificationManager` interruption filter (auto-DND during
  prayer, restores after); per-prayer silent prefs split across 4 preference screens.
- `PersistentNotificationService` + `ContinuousNotificationReceiver`: always-on
  next-prayer countdown notification.
- `SalatiRefreshService` runs in its **own process** (`Salati.refresh`).
- Scheduling core `SalatiService` + `SalatiReceiver` re-arms on
  BOOT/CONNECTIVITY_CHANGE/TIME_SET/TIMEZONE_CHANGED/DATE_CHANGED/PROVIDERS_CHANGED +
  custom `com.masarat.salatuk.PRAYERTIMES_REQUEST` / `START_ACTIVITY` actions
  (signature-protected → internal/widget IPC surface).

## 5. Qibla & sensors

`QiblaActivity`: accelerometer (type 1) + magnetometer (type 2), low-pass α=0.09
(`q5.k.a`), `SensorManager.getRotationMatrix` → `getOrientation`, then
`GeomagneticField.getDeclination()` correction, needle = `360 − (deg − qiblaBearing)`,
**±3° alignment lock** fires an "aligned" state. `HIGH_SAMPLING_RATE_SENSORS` requested
for this compass path. (RECORD_AUDIO remains unexplained — VERIFY: possibly for a
"whistle/shout to find qibla"? Nothing obvious; treat as dead permission.)

## 6. Network & telemetry posture (surprisingly clean)

- **No app-owned API endpoints at all.** All hardcoded URLs are social/privacy/Play/iOS
  store links. The app is 100% offline for its core job.
- Facebook SDK (app id `399888690106811`, client token, login scheme `fb399888690106811`,
  CustomTab + content provider) — but `AutoLogAppEventsEnabled=false` and
  `AdvertiserIDCollectionEnabled=false` in manifest; FacebookActivity present (sharing).
- Google: `com.google.android.geo.API_KEY` = `AIzaSyCKGAkN9ttmAdYpQS0jJU68eo5txAQveDs`
  (hardcoded, Maps), optional `com.google.android.maps` uses-library, Play Core
  in-app review, install referrer. **Embedded API keys/IDs are evidence, not ours to
  reuse.**
- `network_security_config`: single entry, cleartext blocked for `google.com` only —
  effectively decorative (no pinning, no custom trust).

## 7. Manifest curiosities (vendor-compat archaeology)

- `com.huawei.permission.external_app_settings.USE_COMPONENT` +
  `oppo.permission.OPPO_COMPONENT_SAFE` — EMUI/ColorOS alarm-autostart compat hacks
  (the app requests permission to start its own components on Chinese OEM ROMs).
- Signature-level permission `com.masaratapp.salati.SALATUK_PERMISSION` guards all
  widgets/receivers.
- `queries` provider `com.facebook.katana.provider.PlatformProvider` (FB app presence).
- 3 widgets (`SalatukWidgetAll/V2/V3`; V3 listens to `TIME_TICK` for a live minute
  hand) + own analog clock view (`SalatukAnalogClock`).
- `AdsActivity`: transparent, `excludeFromRecents`, extends obfuscated `b0`; **no
  AdMob/FAN app id anywhere in manifest** — mechanism unresolved (VERIFY; possibly a
  self-promo/MoreApps overlay, cf. `MoreAppsPreferences` + Play "details" links).
- `READ_/WRITE_CALENDAR` — prayer times → calendar export (CalendarPreferences).
- `DISABLE_KEYGUARD`, `USE_FULL_SCREEN_INTENT`, `WAKE_LOCK` — the alarm ladder.

## 8. Leverage points → what SalatukPlus takes from this

The rebuild lives at `~/WorkingOn/SalatukPlus/` (own README + ROADMAP = lead ladder).
Direct takeaways, ranked:

1. **Engine is replicable, data is the moat** — implement the solar math (small, known),
   ship the method table as data, and keep a `staticprayertimes`-style verification
   corpus for CI parity tests.
2. **Own tz table** (their `timezone`/`zone` pair) is the right call for
   minSdk-broad apps; steal the design.
3. **Seasonal correction table** keyed by method + MMDD window = the feature that makes
   times match official posters; model it explicitly in the rebuild.
4. Offline-first with zero telemetry is achievable *and* they still ship rich UX
   (lockscreen alarm, shake-stop, DND, widgets) — no network needed anywhere.
5. Their weak spots = our openings: Views-era UI (Compose rewrite), no theme system
   beyond colors, mono-locale logic hacked via `MODE_MULTI_PROCESS` prefs, no backup
   (`allowBackup=false`), no accessibility pass, no tests shipped, ads surface
   unresolved. See `SalatukPlus/ROADMAP.md` for the full ladder.
