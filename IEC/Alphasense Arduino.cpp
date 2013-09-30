double OP1 = 0.0;
double OP2 = 0.0;
long startTime = 0;
boolean started = false;

void setup()
{
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
		OP1 = analogRead(0) / 1023.0 * 5.0;
		OP2 = analogRead(1) / 1023.0 * 5.0;
		Serial.print(OP1);
		Serial.print("\t");
		Serial.println(OP2);
		delay(100);
	}
	else
	{
		startTime = millis();
	}
}

