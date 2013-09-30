#include <SPI.h>

const int SLAVE_SELECT_PIN = 10;
const double VOLTAGE_DIVIDER_RESISTANCE = 22.0;
const double REFERENCE_VOLTAGE = 5.0;
const double CHIP_DIVIDER_RESISTANCE = 5.0;

void setup()
{
	//Initialize Digital Potentiometer
	pinMode(SLAVE_SELECT_PIN, OUTPUT);
	digitalWrite(SLAVE_SELECT_PIN, HIGH);
	SPI.begin(); 
	
	digitalPotWrite(0, 255);
	digitalPotWrite(1, 14);
	
	Serial.begin(9600);
	
}

void loop()
{
	for(int i = 52; i < 255; ++i)
	{
		digitalPotWrite(0, i);
		double inputVoltage = (double) analogRead(7) / 1023.0 * 5.0;
		double outputVoltage = (double) analogRead(0) / 1023.0 * inputVoltage;
		double chipResistance = CHIP_DIVIDER_RESISTANCE * (inputVoltage / outputVoltage - 1);
		double chipCurrentmA = (inputVoltage - outputVoltage) / chipResistance;
		Serial.print(chipCurrentmA);
		Serial.print("\t");
		Serial.println(inputVoltage);
	}
}

int digitalPotWrite(int address, int value) 
{
	// take the SS pin low to select the chip:
	digitalWrite(SLAVE_SELECT_PIN, LOW);
	//  send in the address and value via SPI:
	SPI.transfer(address);
	SPI.transfer(value);
	// take the SS pin high to de-select the chip:
	digitalWrite(SLAVE_SELECT_PIN, HIGH); 
}

int matchResistance(int low, int high, int potChannel, int analogPin, int targetBitVoltage) 
{
	int mid = (low + high) / 2;
	digitalPotWrite(potChannel, mid);
	int voltage = analogRead(analogPin);
	if(low > high)
	{
		return mid;
	}
	if(voltage < targetBitVoltage)
	return matchResistance(low, mid - 1, potChannel, analogPin, targetBitVoltage);
	else if(voltage > targetBitVoltage)
	return matchResistance(mid + 1, high, potChannel, analogPin, targetBitVoltage);
	else
	return mid;
}