#include <Oled.h>

#define WHITE 65535

OLED myOLED(7, 8, 2500, 4800);

void setup()
{
	Serial.begin(4800);
	myOLED.Init();
	myOLED.Clear();
	myOLED.SetSDAddress(0x00, 0x3B, 0x40, 0x00);
	myOLED.WriteSDData();
}

void loop()
{
	myOLED.DrawText(0, 0, 0x02, "Done Writing", WHITE);
}
