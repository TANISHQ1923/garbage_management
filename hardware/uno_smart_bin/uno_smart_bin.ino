#include <Arduino.h>

// Arduino Uno + HC-SR04: VCC=5V, GND=GND, TRIG=9, ECHO=10.
// USB serial only. No credentials, Wi-Fi, or backend logic live on the Uno.
const uint8_t TRIG_PIN = 9;
const uint8_t ECHO_PIN = 10;
const unsigned long ECHO_TIMEOUT_US = 25000UL;
const unsigned long SAMPLE_INTERVAL_MS = 2000UL;
unsigned long sequenceNumber = 0;
unsigned long lastSampleAt = 0;

void setup() {
  pinMode(TRIG_PIN, OUTPUT);
  pinMode(ECHO_PIN, INPUT);
  digitalWrite(TRIG_PIN, LOW);
  Serial.begin(9600);
}

void loop() {
  const unsigned long now = millis();
  if ((unsigned long)(now - lastSampleAt) < SAMPLE_INTERVAL_MS) return;
  lastSampleAt = now;
  digitalWrite(TRIG_PIN, LOW);
  delayMicroseconds(2);
  digitalWrite(TRIG_PIN, HIGH);
  delayMicroseconds(10);
  digitalWrite(TRIG_PIN, LOW);
  const unsigned long duration = pulseIn(ECHO_PIN, HIGH, ECHO_TIMEOUT_US);
  const float distanceCm = duration * 0.0343f / 2.0f;
  const bool valid = duration != 0 && distanceCm >= 2.0f && distanceCm <= 400.0f;

  Serial.print(F("{\"sequence\":")); Serial.print(++sequenceNumber);
  Serial.print(F(",\"uptimeMs\":")); Serial.print(now);
  Serial.print(F(",\"distanceCm\":"));
  if (valid) Serial.print(distanceCm, 2); else Serial.print(F("null"));
  Serial.print(F(",\"valid\":")); Serial.print(valid ? F("true") : F("false"));
  Serial.print(F(",\"reason\":\""));
  Serial.print(valid ? F("OK") : (duration == 0 ? F("NO_ECHO") : F("OUT_OF_RANGE")));
  Serial.println(F("\"}"));
}
