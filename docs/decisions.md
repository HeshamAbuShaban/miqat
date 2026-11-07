# Decisions log — Miqat (working)

> One line per decision; newest first. Names/brands here are cheap to change until
> first release — applicationId `app.miqat` must freeze at release.

## 2026-11-07 — Product name: الميقات / Miqat (working brand)

- Hesham researched "ميقات" (Gemini pass, 2026-11-07): sharia-sound word for the fixed
  time of worship — Quranic basis {الصلاة كانت على المؤمنين كتاباً موقوتاً}; concern:
  bare "الميقات" may first evoke Hajj/Umrah ihram miqats.
- **Applied now:** display name الميقات / Miqat, package + applicationId `app.miqat`
  (folder stays `SalatukPlus` = donor working name; brand ≠ folder, precedent
  BigProjectBank/NexaBank).
- **Final variant still Hesham's pick** (one-string change until release):
  1. الميقات (current default — his researched candidate)
  2. ميقات الصلاة / مواقيت — Gemini's precision picks, kills the Hajj ambiguity
  3. موقوتاً / في وقتها — deep spiritual picks (from the Ayah / the Hadith)
  4. المُعلِم / حافظوا — notifier & preservation angles
- Decision rule agreed: whichever he lands on is a `app_name` string + store listing
  change; nothing structural depends on it.
