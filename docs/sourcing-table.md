# Sourcing table — the texts the app is allowed to show

**Guardrail (research-agenda §Guardrail):** no religious text ships unless its
source is verified. A council seat verified each narration by full-text scan of
the canonical corpus (Bukhari, Muslim, Tirmidhi, Nasa'i, Abu Dawud, Ibn Majah,
Musnad Ahmad). A wrong number is worse than a missing one, so unnumbered
citations stay in this table and never reach the UI.

Verified by the council on 2026-11-10 (`minimax-m3.1-flash-free`).

## Shipped in the UI (verified)

| # | Text | Source | Where |
|---|------|--------|-------|
| 1 | «يَنْزِلُ رَبُّنَا تَبَارَكَ وَتَعَالَى كُلَّ لَيْلَةٍ إِلَى السَّمَاءِ الدُّنْيَا حِينَ يَبْقَى ثُلُثُ اللَّيْلِ الآخِرُ…» | صحيح البخاري ١١٤٥ · صحيح مسلم ٧٥٨ | مواقيت السنة → وقت النوم |
| 2 | «تَسَحَّرُوا فَإِنَّ فِي السُّحُورِ بَرَكَةً» | صحيح البخاري ١٩٢٣ · صحيح مسلم ١٠٩٥ | مواقيت السنة → وقت السحور |
| 3 | «كَانَتْ أُمُّ سُلَيْمٍ تَبْسُطُ لِلنَّبِيِّ ﷺ نِطَعًا فَيَقِيلُ عِنْدَهَا» · «كُنَّا نَقِيلُ وَنَتَغَدَّى بَعْدَ الْجُمُعَةِ» | صحيح البخاري ٦٢٨١ · ٦٢٧٩ | مواقيت السنة → وقت القيلولة |
| 4 | «اقْرَءُوا الْقُرْآنَ فَإِنَّهُ يَأْتِي يَوْمَ الْقِيَامَةِ شَفِيعًا لِأَصْحَابِهِ» | صحيح مسلم ٨٠٤ | مواقيت السنة → أوقات قراءة القرآن |
| 5 | «اللَّهُ الْوَاحِدُ الصَّمَدُ ثُلُثُ الْقُرْآنِ» | صحيح البخاري ٥٠١٥ · صحيح مسلم ٨١١–٨١٢ · الترمذي ٢٨٩٦ | مواقيت السنة → أوقات قراءة القرآن |
| 6 | «وَبِٱلْأَسْحَارِ هُمْ يَسْتَغْفِرُونَ» | سورة الذاريات ١٨ | بطاقة وقت الأسحار |

## The donor's reminder texts (prayer_events) — verified one by one

The donor ships 21 reminder texts with their time windows. The council checked
each against the corpus; the app prints only the ones with a verified source,
and prints the collection-only line for the rest.

| # | Text | Source |
|---|------|--------|
| 1 | الحمد لله الذي أحيانا بعد ما أماتنا وإليه النشور | سنن أبي داود ٥٠٨١ |
| 2 / 7 | أصبحنا وأصبح الملك لله … (the FULL wording; the donor's excerpt is not printed) | سنن الترمذي ٣٣٨٨ |
| 3 | اللهم رب هذه الدعوة التامة … | صحيح البخاري ٦١٤ |
| 4 | اللهم أنت السلام ومنك السلام | سنن الترمذي — رقم غير متحقق |
| 5 / 13 | بسم الله الذي لا يضر مع اسمه شيء | سورة الطلاق ٦٥:٣ |
| 6 | أعوذ بكلمات الله التامات من شر ما خلق | صحيح مسلم ٢٧٠٨ |
| 8 | رب أسألك علما نافعا ورزقا طيبا وعملا متقبلا | سنن ابن ماجه ٩٢٥ |
| 9 / 17 | سبحان الله وبحمده عدد خلقه (duplicates — one entry) | صحيح مسلم ٢٦٩١ |
| 10 | اللهم عافني في بدني | سنن أبي داود — رقم غير متحقق |
| 11 | اللهم إني أسألك العفو والعافية | سنن ابن ماجه — رقم غير متحقق |
| 12 | اللهم إني أعوذ بك من الهم والحزن | سنن أبي داود ١٥٥٨ |
| 14 | حسبي الله لا إله إلا هو | الأنفال ٨:٢٦ وطه ٦٥:٤ (بالمعنى) |
| 15 | اللهم إني أسألك الهدى والتقى والعفاف والرزق | سنن الترمذي ٣٥٢١ |
| 16 | رب اغفر لي وتب علي وارحمني | سنن أبي داود — رقم غير متحقق |
| 18 | لا إله إلا الله وحده لا شريك له | سنن الترمذي ٣٣٨٨ |
| 19 | اللهم عالم الغيب والشهادة | سنن أبي داود — رقم غير متحقق |
| 20 | أستغفر الله وأتوب إليه | سنن أبي داود — رقم غير متحقق |
| 21 | اللهم صل على محمد وعلى آل محمد … | صحيح مسلم ٤٠٦ وصحيح البخاري ٣٣٧٠ |

The collection-only line the app prints when the number is not verified:
**«رواه: <المصدر> — رقم الحديث غير متحقق منه»**.

## The daily reminder line (دليل اليوم)

The home screen shows one verse a day, chosen to fit the hour, with its
reference. The list is curated and verified; the wording is the Mushaf's.

| Verse | Reference | Hours |
|-------|-----------|-------|
| «وَمِنَ اللَّيْلِ فَتَهَجَّدْ بِهِ نَافِلَةً لَّكَ» | سورة مزمل ٧٣ | الليل |
| «أَلَا بِذِكْرِ اللَّهِ تَطْمَئِنُّ الْقُلُوبُ» | سورة الرعد ٢٨ | أي وقت |
| «وَقُل رَّبِّ زِدْنِي عِلْمًا» | سورة طه ١١٤ | الصباح |
| «إِنَّ مَعَ الْعُسْرِ يُسْرًا» | سورة الشرح ٦ | الظهيرة |
| «حَسْبُنَا اللَّهُ وَنِعْمَ الْوَكِيلُ» | سورة آل عمران ١٧٣ | المساء |

## The acts catalog (`assets/acts-v1.json`)

Eleven acts, each with: what it is, why it matters, how it is computed, and its
source (only where the council verified it). Acts without a verified source
print «تقديري بحسب الاصطلاح» instead of a reference — never a number we
cannot stand behind.

## Verified practice, but NOT quoted in the UI

| Narration | Source | Why it is not quoted |
|-----------|--------|----------------------|
| «يَقْرَأُ فِي صَلَاةِ الْغَدَاةِ مِنَ السِّتِّينَ إِلَى الْمِائَةِ» | صحيح مسلم ٤٦١ | The **number** of verses is what matters, and the app does not print it |
| «قَرَأَ فِي رَكْعَتَيِ الْفَجْرِ {قُلْ يَا أَيُّهَا الْكَافِرُونَ} وَ{قُلْ هُوَ اللَّهُ أَحَدٌ}» | سنن أبي داود ١٢٥٦ | Practice only; the UI states the practice, not the narration |
| «قَرَأَ آيتي البقرة آخر الليل» | صحيح البخاري ٥٠٠٩ | Same |
| «كان يقرأ {هَلْ أَتَاكَ حَدِيثُ الْغَاشِيَةِ} يوم الجمعة» | صحيح مسلم ٨٧٨ · أبو داود ١١٢٣ | Friday recitation — the app has no Friday screen yet |
| «من قرأ ما بين الستين إلى المئة في صلاة الغداة كان له نور...» / الفجر بعد الصبح يدعو له | صحيح مسلم ٧٤٧ | The Fajr→Dhuhr window as night recitation |

## Rejected — do NOT ship (the corpus has no such narration)

| Claim | Finding |
|-------|---------|
| «ينام أحدكم حتى لا يسهر» | **Zero hits** across Bukhari / Muslim / the Sunan collections — the wording does not exist |
| «إن في القيلولة شفاء» · «خذوا من القيلولة فإن فيها شفاء» | The word القيلولة does not occur in Abu Dawud / Bukhari / Muslim / Tirmidhi / Nasa'i; the phrase is not canonical |
| «كان يقرأ في الفجر ما بين ست وثلاثين آية» | The practice is verified (Muslim ٤٦١) but the number **31** appears in no narration — the verified range is ٦٠–١٠٠ |
| «من قرأ سورة الإخلاص والمفلقتين في يوم كأنما قرأ ثلث القرآن» | «المفلقتين» is absent from Bukhari / Muslim / Tirmidhi / Nasa'i / Ibn Majah — the verified wording is al-Ikhlas ALONE (Bukhari ٥٠١٥) |
| «كان النبي ﷺ يقرأ سورة الكهف يوم الجمعة» | Not in Bukhari / Muslim; the al-Kahf-on-Friday material is al-Baihaqi via Mishkat ٢١٧٥ — table only |
| «أطراف النهار» as a hadith phrase | The phrase is not a hadith; it only occurs inside isnāds. The UI may use it as a description, never as a quotation |

Weak material kept out of the UI: سنن ابن ماجه ١٦٩٣ and ٤٠٧٣–٤٠٧٤ (ضعيف),
سنن ابن ماجه ٨٣٣ (ضعيف), الترمذي ٣٤٠٤–٣٤٠٦ (حسن).

## Policy

1. A text ships only with a collection + number verified above.
2. The app displays and reminds; it never issues a ruling.
3. Differences between positions are named as positions, with sources.
4. When a narration cannot be verified, the FEATURE stays (the time window is a
   computation) but its text does not — the UI says «المرجع قيد التوثيق» instead
   of printing an unverified narration.
