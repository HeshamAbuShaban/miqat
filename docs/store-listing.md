# The store listing — المِيقات / Miqat

The copy for F-Droid (and, if it ever goes to a store, for the listing
page). It is written to be **true** — every claim below was checked against
the built APK, not wished for.

---

## The name

**الميقات / Miqat**

*The name is deliberately wide*: ميقات is not only the prayer — it is every
appointed time in a Muslim's day and year.

## The tagline (one line)

**مواقيت الصلاة والعبادات، بلا إنترنت — وكم بقي، ولماذا.**

> Prayer and worship times, offline — how long is left, and why.

## The short description (F-Droid, ≤ 250 characters)

```
تطبيق مواقيت للصلاة والعبادات: الموعد القادم وكم بقي عليه، وتقويمٌ
هجريٌّ بدفترِ عباداتٍ كامل، وأوقاتُ الليل والصيام والحج — بلا إنترنت
ولا إعلانات ولا حسابات.
```

## The long description (Arabic)

```
الميقات — كل ما في يوم المسلم وسنته له ميقات، والتطبيق يُعطيك إياه بصدق.

• الموعد القادم، والعدّادُ الخاصّ به. كلُّ شيءٍ في سطره: وقتُ الصلاة
  القادمة، ومنذ متى دخل وقتُ الصلاة، والإقامةُ بعددّادها.
• تقويمٌ هجريٌّ الأول، ودفترُ عباداتٍ لكل يوم: الصلوات، ونوافذُ
  الضحى والأسحار والثلث الأخير من الليل، والسحور والقيلولة، وأوقاتُ
  قراءة القرآن، وحدودُ الصيام، ومناسباتُ الحجّ والعيد.
• لكلّ وقتٍ شرحُه: ما هو، ولماذا يهمّ، وكيف يُحسب، ومصدرُه — ولا
  اجتهادَ ولا فتوى في التطبيق.
• أوقانُ الصلاة محسوبةٌ من جدول مدينتك ومن طريقةٍ معلنة، مع ذكرِ
  درجةِ الثقة في كلّ وقت: مؤكد، أو محسوب، أو تقديري.
• تنبيهاتٌ في وقتها، وأذانٌ عند دخول الوقت، وعدّادٌ دائم على شاشة
  القفل، وودجتٌ على شاشة البيت.
• يعملُ بلا إنترنتٍ إطلاقاً: لا إعلانات، ولا حسابات، ولا تتبّع. بل
  إذنُ الإنترنت غيرُ موجودٍ في التطبيق أصلاً.
```

## The long description (English)

```
Miqat — everything in a Muslim's day and year has an appointed time, and
this app gives it to you honestly.

• The next prayer and its own countdown. Every feature gets its own line:
  the next prayer, how long the current prayer's time has been open, and
  the iqama (the second call) with its own timer.
• A Hijri-first calendar and a worship ledger for every day: the prayers,
  the worship windows (Duha, Sahar, the last third of the night), Suhor,
  the qaylula, the times for reading the Qur'an, the boundaries of the
  fast, and the rites of Hajj and Eid.
• Every time explains itself: what it is, why it matters, how it is
  computed, and where it is from. The app states times and cites sources;
  it does not issue rulings.
• The times are computed from your city's table and a declared method,
  and every time carries its confidence: confirmed, computed or estimated.
• Notifications on time, the adhan when the time enters, an always-on
  countdown on the lock screen, and a home-screen widget.
• Offline, always: no ads, no accounts, no tracking. The app does not even
  hold the internet permission.
```

## The privacy statement (a short one, and a true one)

```
لا يجمع التطبيق أي بيانات. لا إعلانات، ولا تحليلات، ولا حسابات.
لا يوجد في التطبيق إذن الإنترنت (android.permission.INTERNET) — لا يمكنه
الاتصال بأي خادم. الموقع الجغرافي اختياريّ: إن اخترت مدينة، فلا حاجة
لإذن الموقع أصلاً.
```

```
The app collects no data. No ads, no analytics, no accounts. It does not
hold the INTERNET permission at all — it cannot reach a server. Location is
optional: pick a city and no location permission is needed.
```

**How to check it yourself (the claim is verifiable):**

```sh
aapt2 dump permissions app/build/outputs/apk/debug/app-debug.apk
# → no android.permission.INTERNET
```

## The feature bullets for the listing (short)

| | |
|---|---|
| ⏱ | الموعد القادم والعدّاد |
| 📅 | تقويم هجري + دفتر عبادات |
| 🌙 | أوقات الليل والسحور والقيلولة |
| 🍽 | حدود الصيام ومناسبات الحج والعيد |
| 🕌 | أذانٌ وإقامةٌ وتنبيهاتٌ في وقتها |
| 📲 | ودجت على شاشة البيت |
| 🔒 | بلا إنترنت، بلا إعلانات، بلا تتبّع |

*(The store listing may use emoji in the bullets; the APP itself never does —
the no-emoji rule is a rule of the interface, not of the store copy.)*

## The screenshots to take

1. **الرئيسية** — the hero over the day's horizon, the timer lines beneath.
2. **التقويم** — the Hijri-first grid with the sigil strip.
3. **بطاقة اليوم** — the spine: the prayers as points, the windows as bands.
4. **دليل الطاعات** — the acts index, each with its own clock.
5. **الودجت** — the same horizon on the home screen.

(Each one is taken from the emulator, in the app's own palette — the listing
must show the product, not a mock-up of it.)

## The release checklist

- [x] `versionName 0.1.0`, `versionCode 1`, minSdk 24, targetSdk 35.
- [x] No INTERNET permission (verified with `aapt2 dump permissions`).
- [x] No new dependency in the whole history of the project — Compose + the
      Android framework only (see `docs/ui-patterns.md`).
- [x] The release build is unsigned-ready; the F-Droid build needs the
      standard metadata files (this document is the copy).
- [ ] The signing key (the founder's; never committed).
- [ ] The F-Droid metadata pull request (`metadata/app.miqat.yml`).
