void setup()
{
	Serial.begin(9600);
	for(int i = 6; i < 10; ++i)
	{
		pinMode(i, OUTPUT);
		digitalWrite(i, LOW);
	}
}

void loop()
{
	for(int i = 0; i < 16; ++i)
	{
		setMUX(i);
		double voltage = (double) analogRead(0) / 1023.0 * 5.0;
		double resistance = (voltage / 5.0 * 3.9) / (1 - voltage / 5.0);
		Serial.print("Resistor ");
		Serial.print(i);
		Serial.print(": ");
		Serial.println(resistance);
	}
	Serial.println();
}

void setMUX(int i)
{
	if(i & 0x0001)
		digitalWrite(6, HIGH);
	else
		digitalWrite(6, LOW);
	if(i & 0x0002)
		digitalWrite(7, HIGH);
	else
		digitalWrite(7, LOW);
	if(i & 0x0004)
		digitalWrite(8, HIGH);
	else
		digitalWrite(8, LOW);
	if(i & 0x0008)
		digitalWrite(9, HIGH);
	else
		digitalWrite(9, LOW);
}