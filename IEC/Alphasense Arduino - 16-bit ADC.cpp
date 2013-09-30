#include <Wire.h>
#include <Adafruit_ADS1015.h>

Adafruit_ADS1115 adc(0x48);

long startTime = 0;
boolean started = false;

void setup()
{
	adc.begin();
	Serial.begin(9600);
}

void loop()
{
	if(Serial.available() > 0)
	{
		char inByte = Serial.read();
		if(inByte == 's')
		{
			started = !started;
			if(started)
			{
				Serial.print("Started at ");
				Serial.print(millis());
				Serial.println(" milliseconds since boot\n");
			}
			else
			{
				Serial.print("Stopped at ");
				Serial.print(millis());
				Serial.println(" milliseconds since boot\n");
			}
		}
	}
	if(started)
	{
		Serial.print(millis() - startTime);
		Serial.print("\t");
		double A0 = adc.readADC_SingleEnded(0);
		double A1 = adc.readADC_SingleEnded(1);
		if(A0 > 65525)
			A0 = 0;
		if(A1 > 65525)
			A1 = 1;
		double OP1 = A0 / 65536.0 * 2.048;
		double OP2 = A1 / 65536.0 * 2.048;
		Serial.print(OP1, 5);
		Serial.print("\t");
		Serial.println(OP2, 5);
		delay(100);
	}
	else
	{
		startTime = millis();
	}
}

