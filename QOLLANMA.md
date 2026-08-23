# Maxfiy Xazina — APK yaratish qo'llanmasi (faqat smartfon bilan)

Bu qo'llanma sizga kompyutersiz, faqat telefon va brauzer orqali to'liq ishlaydigan Android APK yaratishga yordam beradi.

---

## Nima tayyor?

- ✅ To'liq veb-ilova (`www/index.html`) — AES-256, avtomatik qulflash, xavfsiz eksport/import
- ✅ Native Android kodi (Kotlin) — Autofill Service, Biometrika, Android Keystore
- ✅ GitHub Actions workflow — bulutda avtomatik APK yasaydi

## Umumiy jarayon (5 qadam)

1. GitHub'da bepul akkaunt oching (agar yo'q bo'lsa)
2. Yangi repository (loyiha) yarating
3. Bergan fayllarni shu repository'ga yuklaysiz
4. GitHub Actions avtomatik ishga tushib, APK yasaydi
5. Tayyor APK faylni yuklab olib, telefoningizga o'rnatasiz

---

## 1-QADAM: GitHub akkaunt va repository

1. Telefon brauzerida **github.com** ga kiring, agar akkaunt bo'lmasa **Sign up** orqali ro'yxatdan o'ting (bepul)
2. Kirgandan keyin yuqori o'ng burchakdagi **+** belgisini bosing → **New repository**
3. Repository nomi: `maxfiy-xazina` (yoki xohlagan nom)
4. **Public** yoki **Private** — ikkalasi ham bepul, lekin parol menejeri kodi uchun **Private**ni tanlashni tavsiya qilaman
5. **Create repository** tugmasini bosing

---

## 2-QADAM: Fayllarni yuklash

GitHub'ning veb-interfeysida telefon orqali papka yuklab bo'lmaydi (faqat alohida fayllar), shuning uchun eng oson yo'l — **zip fayl orqali**:

1. Yuqoridagi javobda men sizga zip fayl beraman (`vault-apk-project.zip`)
2. Uni telefoningizga yuklab oling
3. GitHub repository sahifangizda **"uploading an existing file"** havolasini bosing
4. Zip faylni **ochmasdan** turib, uni **avval telefoningizning fayl menejeri orqali papkaga ekstrakt qiling** (masalan, "Files" yoki "ZArchiver" ilovasi bilan)
5. Ekstrakt qilingan barcha fayl va papkalarni GitHub yuklash oynasiga birma-bir sudrab tashlang (yoki "choose your files" orqali tanlang)

**Muhim:** GitHub papka strukturasini saqlab qolishi uchun, fayllarni papkalar bilan birga (masalan, `.github/workflows/build.yml` yo'li saqlanган holda) yuklashingiz kerak. Agar telefon brauzeri buni qiyinlashtirsa, quyidagi muqobil usulni ishlating:

### Muqobil usul: GitHub mobil ilovasi + Termux (tavsiya etiladi)

Eng ishonchli yo'l — **Termux** (Android uchun terminal) orqali `git` buyruqlaridan foydalanish:

1. Play Store'dan **Termux** ilovasini o'rnating
2. Termux'ni oching va quyidagi buyruqlarni bittalab kiriting:

```bash
pkg update -y
pkg install git -y
```

3. GitHub'da **Settings → Developer settings → Personal access tokens → Generate new token** orqali token yarating (parol o'rniga ishlatiladi, "repo" ruxsati bilan)
4. Zip faylni Termux orqali yuklab, ekstrakt qiling va reponi push qiling:

```bash
cd ~
# Avval zip faylni Download papkasidan Termux'ga ko'chiring:
cp /sdcard/Download/vault-apk-project.zip .
unzip vault-apk-project.zip
cd vault-apk
git init
git add .
git commit -m "Birinchi versiya"
git branch -M main
git remote add origin https://github.com/FOYDALANUVCHI_NOMI/maxfiy-xazina.git
git push -u origin main
```

(`FOYDALANUVCHI_NOMI` o'rniga o'z GitHub username'ingizni yozing. Push paytida parol so'ralsa, oddiy parol emas, balki 3-qadamda yaratgan **token**ni kiriting.)

---

## 3-QADAM: Build jarayonini kuzatish

1. Fayllar yuklangandan keyin, repository sahifangizda **"Actions"** bo'limiga o'ting
2. "Build Android APK" nomli workflow avtomatik ishga tushganini ko'rasiz (sariq nuqta = ishlayapti, yashil belgi = muvaffaqiyatli, qizil = xato)
3. Bu jarayon odatda **3-7 daqiqa** davom etadi
4. Agar **qizil belgi (xato)** chiqsa — xavotir olmang, bu odatiy holat. Xato matnini nusxalab, menga yuboring, men tuzataman

---

## 4-QADAM: Tayyor APK'ni yuklab olish

1. Build muvaffaqiyatli tugagandan keyin (yashil belgi), o'sha workflow sahifasiga kiring
2. Pastda **"Artifacts"** bo'limida `maxfiy-xazina-apk` nomli fayl bo'ladi
3. Uni bosib yuklab oling (bu zip ichida APK bo'ladi, uni ekstrakt qiling)

---

## 5-QADAM: APK'ni o'rnatish

1. Yuklab olingan `app-debug.apk` faylini toping
2. Uni bosganingizda "Noma'lum manbalardan o'rnatishga ruxsat berilmagan" degan ogohlantirish chiqishi mumkin — **Sozlamalar**ga o'tib, shu brauzer/fayl menejeri uchun ruxsat bering
3. O'rnatilgandan keyin ilovani oching va bosh parolingizni yarating

### Autofill xizmatini yoqish (ixtiyoriy, lekin tavsiya etiladi)

1. Telefon **Sozlamalar** → **Tizim** (yoki **Til va kiritish**) → **Autofill xizmati**
2. **Maxfiy Xazina**ni tanlang
3. Endi boshqa ilovalarda login/parol maydoniga bosganingizda, xazinangizdagi mos yozuvlar taklif qilinadi

### Biometrikani yoqish

1. Ilova ichida **Sozlamalar** (⚙️ belgisi) → **"Barmoq izi bilan ochish"** ni yoqing
2. Keyingi safar ilovani ochganda barmoq izi tugmasi paydo bo'ladi

---

## Muhim eslatmalar

- **Bosh parolni unutmang** — uni tiklab bo'lmaydi, bu ataylab shunday qilingan (xavfsizlik uchun)
- **Debug APK** — bu birinchi versiya "debug" turida, ya'ni rasmiy raqamli imzo bilan emas. Bu shaxsiy foydalanish uchun butunlay yetarli va xavfsiz, lekin agar kelajakda Google Play'ga joylashtirmoqchi bo'lsangiz, "release" versiyasini alohida imzolash kerak bo'ladi — bu haqda alohida so'rasangiz yordam beraman
- **Zaxira nusxa** — ilova ichidagi Sozlamalar orqali muntazam ravishda shifrlangan zaxira fayl yarating va uni boshqa xavfsiz joyda (masalan, Google Drive) saqlang, chunki APK debug versiyasi ustidan qayta o'rnatishda ma'lumot yo'qolishi mumkin

---

## Agar biror narsa ishlamasa

Quyidagilarni menga yuboring, birga tuzatamiz:
1. Qaysi qadamda muammo chiqdi
2. Agar GitHub Actions xato bersa — "Actions" sahifasidagi qizil belgili workflow ichiga kirib, xato matnini (log) nusxalab yuboring
3. Agar APK o'rnatilmasa — telefon modelingiz va Android versiyangiz
