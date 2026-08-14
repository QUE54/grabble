/*
 * UNO R4 WiFi — BLE "Rickroll Key"
 * ---------------------------------
 * บอร์ดนี้ทำหน้าที่เป็น BLE Peripheral (ตัวประกาศตัว)
 * แอป Android จะเป็นฝ่ายสแกนหาบอร์ดนี้เอง (ดูโฟลเดอร์ android/)
 *
 * เหตุผลที่ออกแบบแบบนี้ (บอร์ด advertise / มือถือสแกน แทนที่จะกลับด้าน):
 * มือถือสมัยใหม่ไม่ยอม advertise BLE ให้ตัวเองถูกสแกนเจอโดย default
 * (สุ่ม MAC address เพื่อความเป็นส่วนตัว) เลยให้บอร์ดเป็นฝ่ายประกาศตัวแทน
 * ซึ่งเสถียรกว่ามาก และยังคุมระยะ ~30cm ได้ด้วยค่า RSSI ฝั่งแอป
 *
 * ต้องติดตั้งไลบรารี "ArduinoBLE" ผ่าน Library Manager ก่อน
 * (Tools > Manage Libraries... > ค้นหา ArduinoBLE > Install)
 */

#include <ArduinoBLE.h>

// ===================== ตั้งค่า (ต้องตรงกับฝั่งแอป Android) =====================
const char* DEVICE_NAME = "R4_KEY";                                  // ชื่อที่แอปค้นหา
const char* SERVICE_UUID = "19b10000-e8f2-537e-4f6c-d104768a1214";   // ต้องตรงกับ SERVICE_UUID ในแอป
const char* CHAR_UUID    = "19b10001-e8f2-537e-4f6c-d104768a1214";   // ใช้ยืนยันการเชื่อมต่อ (ไม่บังคับ)
// ================================================================================

BLEService rickrollService(SERVICE_UUID);
BLEByteCharacteristic statusChar(CHAR_UUID, BLERead | BLEWrite | BLENotify);

const int LED_PIN = LED_BUILTIN;

void setup() {
  Serial.begin(9600);
  // ไม่ while(!Serial) เพราะบอร์ดต้องทำงานได้แม้ไม่ได้เสียบคอมดู Serial Monitor

  pinMode(LED_PIN, OUTPUT);
  digitalWrite(LED_PIN, LOW);

  if (!BLE.begin()) {
    Serial.println("เริ่มโมดูล BLE ไม่สำเร็จ! ตรวจสอบบอร์ด/สาย");
    while (1) {
      digitalWrite(LED_PIN, !digitalRead(LED_PIN));
      delay(200); // ไฟกะพริบเร็ว = error
    }
  }

  BLE.setLocalName(DEVICE_NAME);
  BLE.setAdvertisedService(rickrollService);
  rickrollService.addCharacteristic(statusChar);
  BLE.addService(rickrollService);
  statusChar.writeValue(0);

  BLE.advertise();

  Serial.print("R4 พร้อมแล้ว กำลังประกาศตัวเป็น: ");
  Serial.println(DEVICE_NAME);
}

void loop() {
  // รอมือถือ (ที่รันแอปอยู่) เข้ามาเชื่อมต่อยืนยันตัวตน
  BLEDevice central = BLE.central();

  if (central) {
    Serial.print("พบเป้าหมาย! เชื่อมต่อกับ: ");
    Serial.println(central.address());
    digitalWrite(LED_PIN, HIGH); // ไฟติดค้าง = กำลังเชื่อมต่ออยู่

    while (central.connected()) {
      if (statusChar.written()) {
        Serial.print("แอปส่งค่ามา: ");
        Serial.println(statusChar.value());
      }
    }

    digitalWrite(LED_PIN, LOW);
    Serial.println("มือถือหลุดการเชื่อมต่อ กลับไปรอเป้าหมายใหม่...");
  }
}
