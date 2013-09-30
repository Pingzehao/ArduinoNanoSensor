#include <GPSShield.h>

#define rxPin 2
#define txPin 3
#define ledPin 13

byte pinState = 0;
char NMEA[75];
int counter = 0;


void setup()
{
	pinMode(rxPin, INPUT);
	pinMode(txPin, OUTPUT);
	pinMode(ledPin, OUTPUT);
	Serial.begin(4800);
}

void print(GPSShield gpsData)
{
	int day = gpsData.getDay();
	int month = gpsData.getMonth();
	int year = gpsData.getYear();
	int hour = gpsData.getHour();
	int minute = gpsData.getMinute();
	int second = gpsData.getSecond();
	int millisecond = gpsData.getMillis();
	int longDegrees = gpsData.getLongDegrees();
	int latDegrees = gpsData.getLatDegrees();
	double longMinutes = gpsData.getLongMinutes();
	double  latMinutes = gpsData.getLatMinutes();
	char longHeading = gpsData.getLongHeading();
	char latHeading = gpsData.getLatHeading();
	Serial.print("Time: ");
	if(hour < 10)
		Serial.print("0");
	Serial.print(hour);
	Serial.print(":");
	if(minute < 10)
		Serial.print("0");
	Serial.print(minute);
	Serial.print(":");
	Serial.print(second);
	if(second < 10)
		Serial.print("0");
	Serial.print(":");
	if(millisecond < 10)
		Serial.print("0");
	Serial.println(millisecond);
	
	Serial.print("Connection Status: ");
	if(gpsData.isConnected())
		Serial.println("Connected");
	else
		Serial.println("No GPS Signal");
	
	Serial.print("Latitude: ");
	Serial.print(latDegrees);
	Serial.print("* ");
	Serial.print(latMinutes);
	Serial.print("' ");
	Serial.println(latHeading);
	
	Serial.print("Longitude: ");
	Serial.print(longDegrees);
	Serial.print("* ");
	Serial.print(longMinutes);
	Serial.print("' ");
	Serial.println(longHeading);
	
	Serial.print("Date: ");
	if(month < 10)
		Serial.print("0");
	Serial.print(month);
	Serial.print("/");
	if(day < 10)
		Serial.print("0");
	Serial.print(day);
	Serial.print("/");
	Serial.print("20");
	if(year < 10)
		Serial.print("0");
	Serial.println(year);
}

void loop()
{
	while(Serial.available())
	{
		char inChar = Serial.read();
		if(inChar == '$')
			counter = 0;
		NMEA[counter++] = inChar;
	}
	
	// for(int i = 0; i < counter; ++i)
	// {
		// Serial.print(NMEA[i]);
	// }
	// Serial.println();

	if(NMEA[5] == 'C' && NMEA[counter - 3] == '*')
	{
		GPSShield gpsData(NMEA);
		Serial.println("\n");
		print(gpsData);
		Serial.println();
	}
	
	if(counter == 75 || NMEA[counter - 3] == '*')
	{
		counter = 0;
	}
	toggle(13);
}

void toggle(int pinNum)
{
	digitalWrite(pinNum, pinState);
	pinState = !pinState;
}
