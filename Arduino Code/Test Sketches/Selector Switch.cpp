void setup()
{
	pinMode(2, OUTPUT);
	digitalWrite(2, HIGH);
  Serial.begin(9600);
}

void loop()
{
  Serial.println(analogRead(5));
}
