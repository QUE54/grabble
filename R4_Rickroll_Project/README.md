# R4 Rickroll Key

โปรเจกต์ 2 ฝั่งที่เชื่อมกันผ่าน BLE:

```
arduino/UNO_R4_Rickroll/   → โค้ดอัปโหลดลงบอร์ด UNO R4 WiFi
android/                   → โปรเจกต์ Android Studio (Kotlin) เต็มรูปแบบ
```

## แนวคิดการทำงาน

R4 **ประกาศตัว (advertise)** เป็น BLE peripheral ตลอดเวลา ส่วนแอป Android
**สแกนหา R4** อยู่เบื้องหลัง (foreground service) — สลับด้านจากที่ร่างไว้ตอนแรก
เพราะมือถือสมัยใหม่ไม่ยอม advertise ตัวเองให้ถูกสแกนเจอ (สุ่ม MAC เพื่อความเป็นส่วนตัว)
ให้บอร์ดเป็นฝ่ายประกาศตัวแทนจะเสถียรกว่ามาก

```
UNO R4 (advertise BLE) --------> แอป Android (สแกนหาเบื้องหลัง)
                                        │
                              RSSI แรงพอ (ใกล้ ~30cm)
                                        │
                                        ▼
                          เปิดเบราว์เซอร์ไปที่ youtube.com/watch?v=...
                                        │
                                        ▼
                    (ไม่บังคับ) เชื่อมต่อ GATT กลับไปยัง R4 → ไฟ LED ติด
```

## ⚠️ สิ่งที่ผมทำให้ไม่ได้ในแซนด์บ็อกซ์นี้

ผมเขียนซอร์สโค้ดให้ครบและคอมไพล์ได้จริง แต่**ไม่สามารถสร้างไฟล์ .apk ที่คอมไพล์แล้วให้ได้**
เพราะสภาพแวดล้อมนี้ไม่มี Android SDK และเข้าถึงอินเทอร์เน็ตได้เฉพาะบางโดเมน (ไม่รวม
`dl.google.com` / `maven.google.com` ที่ Gradle ต้องใช้ดาวน์โหลด Android Gradle Plugin)

## ⭐ วิธีที่ง่ายที่สุด: ให้ GitHub build ให้ (ไม่ต้องลง Android Studio เลย)

โปรเจกต์นี้มีไฟล์ `.github/workflows/build-apk.yml` ติดมาให้แล้ว — เป็นสูตรสำเร็จที่สั่ง
GitHub ให้คอมไพล์ APK บนเซิร์ฟเวอร์ของเขาเอง (มี Android SDK ครบ ไม่มีปัญหา path
มีช่องว่างแบบ Windows) แล้วให้เราดาวน์โหลดไฟล์ผลลัพธ์กลับมา

**ขั้นตอน (ทำผ่านเว็บเบราว์เซอร์ล้วนๆ ไม่ต้องเขียนโค้ดเพิ่ม):**

1. สมัคร/ล็อกอิน [github.com](https://github.com) (ฟรี)
2. กด **New repository** → ตั้งชื่ออะไรก็ได้ เช่น `r4-rickroll` → เลือก **Public** หรือ
   **Private** ก็ได้ → กด Create repository
3. ในหน้าเว็บ repo ที่สร้างใหม่ กด **"uploading an existing file"** (ลิงก์สีฟ้าตรงกลางหน้า)
4. **ลากทั้งโฟลเดอร์** `rickroll_project` (ที่แตกไฟล์ zip ออกมาแล้ว — ต้องลากทั้งโฟลเดอร์
   ทั้ง `android/`, `arduino/`, `.github/` ไปพร้อมกัน) ไปวางในกล่องอัปโหลด แล้วกด
   **Commit changes**
5. ไปที่แท็บ **Actions** ด้านบนของหน้า repo → จะเห็น workflow "Build APK" กำลังรันอยู่
   (วงกลมสีเหลืองหมุนๆ) รอประมาณ 3-5 นาที จนกลายเป็นเครื่องหมายถูกสีเขียว ✅
6. คลิกเข้าไปที่ workflow run นั้น → เลื่อนลงมาล่างสุดจะเจอส่วน **Artifacts** →
   ดาวน์โหลด `R4-Rickroll-app-debug` (เป็นไฟล์ .zip ข้างในมี `app-debug.apk`)
7. ส่งไฟล์ `app-debug.apk` เข้ามือถือ (ผ่าน LINE/Google Drive/สาย USB) แล้วติดตั้งได้เลย
   (ต้องเปิด "อนุญาตติดตั้งจากแหล่งที่ไม่รู้จัก" ในมือถือก่อน)

ข้อดี: ไม่ต้องลง Android Studio, ไม่มีปัญหา path ช่องว่างแบบที่เจอ, ทุกครั้งที่แก้โค้ด
แล้วอัปโหลดใหม่ (หรือแก้ไฟล์ตรงในเว็บ GitHub เลยก็ได้) มันจะ build ให้อัตโนมัติ

## วิธี build APK แบบ local (ถ้าอยากลองอีกครั้ง หรืออยาก build ผ่าน USB debug)

1. ดาวน์โหลด [Android Studio](https://developer.android.com/studio) (ฟรี)
2. เปิดโปรเจกต์: File > Open... > เลือกโฟลเดอร์ `android/`
3. รอ Gradle sync (ครั้งแรกจะช้าหน่อย ต้องโหลด dependency)
4. เสียบมือถือ (เปิด USB debugging) แล้วกด Run ▶️ — หรือจะเอา APK ไฟล์จริงก็ไปที่
   `Build > Build Bundle(s) / APK(s) > Build APK(s)` ได้ไฟล์ที่
   `android/app/build/outputs/apk/debug/app-debug.apk`

## วิธีอัปโหลดโค้ด Arduino

1. เปิด Arduino IDE > Tools > Manage Libraries... > ค้นหา `ArduinoBLE` แล้วติดตั้ง
2. เลือกบอร์ด "Arduino UNO R4 WiFi"
3. เปิดไฟล์ `arduino/UNO_R4_Rickroll/UNO_R4_Rickroll.ino` แล้ว Upload
4. เปิด Serial Monitor (9600 baud) จะเห็นข้อความ "R4 พร้อมแล้ว..."

## ค่าที่ปรับได้

| ค่า | อยู่ที่ไฟล์ | ผลลัพธ์ |
|---|---|---|
| `SERVICE_UUID` / `CHAR_UUID` | `.ino` และ `BleScanService.kt` | **ต้องตรงกันทั้งสองฝั่งเสมอ** ถ้าเปลี่ยนต้องเปลี่ยนพร้อมกัน |
| `DEVICE_NAME` | `.ino` | ชื่อบอร์ดที่โชว์ตอนสแกน (ไม่กระทบการทำงาน เปลี่ยนได้อิสระ) |
| `RSSI_THRESHOLD` | `BleScanService.kt` | ยิ่งใกล้ 0 = ต้องเข้าใกล้บอร์ดมากขึ้นถึงจะทริกเกอร์ (เริ่มที่ -50 แล้วค่อยจูนจากการทดสอบจริง) |
| `COOLDOWN_MS` | `BleScanService.kt` | กันเปิดซ้ำถี่ๆ ตอนยังอยู่ใกล้กัน (ค่าเริ่มต้น 30 วินาที) |
| `YOUTUBE_URL` | `BleScanService.kt` | ลิงก์ที่จะเปิด เปลี่ยนวิดีโอ/URL ได้ตามใจ |

## หมายเหตุเรื่องสิทธิ์ (permissions)

แอปจะขอสิทธิ์ Bluetooth (และ Location บน Android เก่ากว่า 12) และ Notification
ตอนกดปุ่ม "เริ่มสแกนหา R4" ครั้งแรก — ถ้าปฏิเสธ แอปจะสแกนไม่ได้ ต้องเข้าไปกดอนุญาต
เองใน Settings > Apps > R4 Key > Permissions

บนบางรุ่น (โดยเฉพาะ Xiaomi/Samsung ที่ตัดพลังงานแอปพื้นหลังแรง) อาจต้องปิด
"Battery optimization" ให้แอปนี้ด้วย ไม่งั้น service จะถูกระบบฆ่าทิ้งหลังใช้งานสักพัก
