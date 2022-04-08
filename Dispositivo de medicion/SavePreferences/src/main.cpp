#include <Arduino.h>
#include <Preferences.h>
Preferences preferences;

void setup() {
  //save mqtt credentials 
  preferences.begin("mqtt_cred", false);
  preferences.putString("mqtt_server","ipV4_server");
  preferences.putInt("mqtt_port",1883);
  preferences.putString("mqtt_user","user");
  preferences.putString("mqtt_pass","pass");  
  preferences.end();
  //save info device
  preferences.begin("info_device", false);
  preferences.putString("device","ENER-ESP32-001");
  preferences.end();
}

void loop() {
  
}