/*
Digital Pot Control

This example controls an Analog Devices AD5206 digital potentiometer.
The AD5206 has 6 potentiometer channels. Each channel's pins are labeled
A - connect this to voltage
W - this is the pot's wiper, which changes when you set it
B - connect this to ground.

The AD5206 is SPI-compatible,and to command it, you send two bytes, 
one with the channel number (0 - 5) and one with the resistance value for the
channel (0 - 255).  

The circuit:
* All A pins  of AD5206 connected to +5V
* All B pins of AD5206 connected to ground
* An LED and a 220-ohm resisor in series connected from each W pin to ground
* CS - to digital pin 10  (SS pin)
* SDI - to digital pin 11 (MOSI pin)
* CLK - to digital pin 13 (SCK pin)

created 10 Aug 2010 
by Tom Igoe

Thanks to Heather Dewey-Hagborg for the original tutorial, 2005

*/


// inslude the SPI library:
#include <SPI.h>

// set pin 10 as the slave select for the digital pot:
const int slaveSelectPin = 10;

void setup() 
{
	Serial.begin(9600);
	pinMode(slaveSelectPin, OUTPUT);
	SPI.begin(); 
}

void loop() 
{
	/*for(int i = 0; i < 256; i++)
	{
		digitalPotWrite(0, i);
		double voltage = analogRead(1) / 1023.0 * 5.0;
		Serial.print(voltage);
		Serial.print("\t");
		double expectedVoltage = 235000 / ((static_cast<double>(i) * 100000 / 255) + 47000);
		Serial.println(expectedVoltage);
		//double measuredResistance = (100000 * voltage) / (5.0 - voltage);
		//Serial.println(measuredResistance);
		delay(50);
	}*/
	double voltage;
	int bitOhms = matchResistance(0, 255);
	double ohms = bitOhms / 255.0 * 100000;
	Serial.println(ohms);
	delay(1000);
	/*for(int i = 0; i < 255; ++i)
	{
		for(int j = 0; j < 10; ++j)
		{
			digitalPotWrite(0, i);
			voltage = analogRead(0) / 1023.0 * 5.0;
			//The Digital Pot is the second one in the series
			//ohms = (2800 - 560 * voltage) / voltage;
			//The Digital Pot is the first one in the series
			ohms = (22000 * voltage) / (5.0 - voltage);
			Serial.print(i);
			Serial.print("\t");
			Serial.print(voltage);
			Serial.print("\t");
			Serial.println(ohms);
			delay(50);
		}
	}*/
}

int matchResistance(int low, int high)
{
	delay(50);
	int mid = (low + high) / 2;
	digitalPotWrite(0, mid);
	double voltage = analogRead(0) / 1023.0 * 5.0;
	if(low > high)
	{
		Serial.print(voltage);
		Serial.print("\t");
		return mid;
	}
	if(voltage < 2.5)
	return matchResistance(low, mid - 1);
	else if(voltage > 2.5)
	return matchResistance(mid + 1, high);
}

int digitalPotWrite(int address, int value) {
	// take the SS pin low to select the chip:
	digitalWrite(slaveSelectPin,LOW);
	//  send in the address and value via SPI:
	SPI.transfer(address);
	SPI.transfer(value);
	// take the SS pin high to de-select the chip:
	digitalWrite(slaveSelectPin,HIGH); 
}
