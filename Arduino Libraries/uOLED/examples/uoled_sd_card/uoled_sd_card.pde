#include <uOLED.h>
#include <colors.h>

uOLED uoled;         // create an instance of the uOLED class with the 

void setup() {
  Serial.begin(115200);
  uoled.begin(8,4800, &Serial1);      //reset line connected to pin 8 and a serial speed of 4800, hardware serial 1
 
}

void loop() {
  uoled.DeviceInfo(); 
  //uoled.Text(0, 5, LARGE_FONT, 0xFFFF, "Dit doet het iig.", 0);
  uoled.scrollSpeed(0x7);  
  uoled.scrollToRight(0);
  uoled.scrollEnable(1);
  
  uoled.sdInit();
  Serial.print("sdInit = "); 
  Serial.println(uoled.res, HEX);
  
  uoled.sdSetMemAdrr(0, 0, 0, 0);
  Serial.print("sdSetMemAdrr = ");
  Serial.println(uoled.res, HEX);
  
  for (byte i=0; i<6; i++) 
  {
    uoled.sdWriteByte(i);
    Serial.print("sdWriteByte ");
    Serial.print(i, DEC);
    Serial.print(" = ");
    Serial.println(uoled.res, HEX);
  }
  
  uoled.sdSetMemAdrr(0, 0, 0, 0);
  Serial.print("sdSetMemAdrr = ");
  Serial.println(uoled.res, HEX);
  for (byte i=0; i<6; i++) 
  {
    Serial.print("sdReadByte ");
    Serial.print(i, DEC);
    Serial.print(" = ");
    Serial.println(uoled.sdReadByte(), DEC);
  }
  
  byte oneBlockOfData[512];
  Serial.println("filling oneBlockOfData:");  
  for (int i=0; i<256; i++) {
    oneBlockOfData[i] = i;
    Serial.print("filling location= "); 
    Serial.print(i, DEC);
    Serial.print(" filling data = ");
    Serial.println(i, DEC);
  }
  for (int i=0; i<256; i++) {
    oneBlockOfData[i+256] = i;
    Serial.print("filling location= "); 
    Serial.print(i+256, DEC);
    Serial.print(" filling data = ");
    Serial.println(i, DEC);
  }
  
  uoled.sdWriteBlock(0L, oneBlockOfData);
  Serial.print("sdWriteBlock = ");
  Serial.println(uoled.res, HEX);
  
  Serial.println("one-ing oneBlockOfData:");
  for (int i=0; i<512; i++) {
    oneBlockOfData[i] = 1;
  }
  for (int i=0; i<512; i++) {
    Serial.print("read location= "); 
    Serial.print(i, DEC);
    Serial.print(" read data = ");
    Serial.println(i, DEC);
    Serial.println(oneBlockOfData[i], DEC);
  }
  
  uoled.sdReadBlock(0L, oneBlockOfData);
  Serial.println("sdReadBlock done ");  
  
  for (int i=0; i<512; i++) {
    Serial.print("read location= "); 
    Serial.print(i, DEC);
    Serial.print(" read data = ");
    Serial.println(i, DEC);
    Serial.println(oneBlockOfData[i], DEC);
  }
  
  uoled.sdScreenCopy(0, 0, 71, 70, 1L);
  Serial.print("sdScreenCopy left side = ");
  Serial.println(uoled.res, HEX);
  
  uoled.sdScreenCopy(71, 0, 70, 70, 100L);
  Serial.print("sdScreenCopy right side= ");
  Serial.println(uoled.res, HEX);
  uoled.Cls();
  delay(1000);  
    
  uoled.sdDisplayImage(0, 0, 71, 70, 1, 1L);
  Serial.print("sdDisplayImage = ");
  Serial.println(uoled.res, HEX);
  delay(3000);
  uoled.Cls();
  uoled.sdDisplayImage(71, 0, 70, 70, 1, 100L);
  Serial.print("sdDisplayImage = ");
  Serial.println(uoled.res, HEX);
  delay(3000);
  uoled.Cls();
  
  uoled.sdDisplayImage(0, 0, 71, 70, 1, 1L);
  Serial.print("sdDisplayImage = ");
  Serial.println(uoled.res, HEX);
  uoled.sdDisplayImage(71, 0, 70, 70, 1, 100L);
  Serial.print("sdDisplayImage = ");
  Serial.println(uoled.res, HEX);
  delay(3000);
  uoled.Cls();
  
//  uoled.sdDisplayVideo(0, 0, 160, 128, 1, 0x0, 0xB7, 0x1000);
//  Serial.print("sdDisplayVideo = ");
//  Serial.println(uoled.res, HEX);
//  
//  uoled.Cls();
  

}
