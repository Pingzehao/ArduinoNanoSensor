#include <SPI.h>

/** Arduino constants **/
const int BLUETOOTH = 115200;
const int SLAVE_SELECT_PIN = 10;
const int OUT = 0;
const int IN = 1;
const int SENSOR_PINS = 4;
const int ANALOG_PINS = 6;

/** Constants for SM states **/
const int STOPPED = 0;
const int PARSE = 1;
const int MATCH = 2;
const int READ = 3;
const int SEND = 4;

/** Constants for transmission **/
const char EOA = '.';
const char START = '^';
const char END = '$';

char commandString[20];
int commandIndex = 0;

int potResistance[SENSOR_PINS];
int voltageReadings[ANALOG_PINS];

int pollingRate = 0;

int state = STOPPED;
char inByte = '\0';

int char2num(char *str, int len);
int matchResistance(int low, int high);
void readValues();
void sendValues();


void setup()
{
	Serial.begin(9600);
	/** Setup SPI for controlling the digital potentiometer **/
	pinMode(SLAVE_SELECT_PIN, OUTPUT);
	SPI.begin();
	/** Set the resistor for the temperature sensor to 7.84kOhms **/
	digitalPotWrite(5, 5);
}

void loop()
{
	SM_Actions();
	SM_State();
	delay(pollingRate);
}

/** Writes the value to the digital potentiometer **/
int digitalPotWrite(int address, int value) 
{
	digitalWrite(SLAVE_SELECT_PIN,LOW);
	SPI.transfer(address);
	SPI.transfer(value);
	digitalWrite(SLAVE_SELECT_PIN, HIGH); 
}

/** Converts a char* into it's numerical representation e.g. "100" to 100 **/
int char2num(char* str, int length) 
{
	int value = 0;
	for(int i = 0; i < length; ++i)
	{
		if(i > 0)
			value *= 10;
		value += (int) (str[i] - '0');
	}
	return value;
}

/** Matches the potentiometer with the sensor so the output is 1V **/
int matchResistance(int low, int high, int pin)
{
	int mid = (low + high) / 2;
	digitalPotWrite(pin, mid);
	delay(10);
	double voltage = analogRead(pin);
	if(low > high || voltage == 205)
		return mid;
	if(voltage < 205)
		return matchResistance(low, mid - 1, pin);
	else if(voltage > 205)
		return matchResistance(mid + 1, high, pin);
}

/** Read the voltage values from the ADC **/
void readValues()
{
	for(int i = 0; i < ANALOG_PINS; ++i)
	{
		voltageReadings[i] = analogRead(i);
	}
}

/** Sends the values of the potentiometer resistances and then the voltage readings **/
void sendValues()
{
	Serial.print(START);
	for(int i = 0; i < SENSOR_PINS; ++i)
	{
		Serial.print(potResistance[i]);
		Serial.print(EOA);
	}
	for(int i = 0; i < ANALOG_PINS; ++i)
	{
		Serial.print(voltageReadings[i]);
		if(i < ANALOG_PINS - 1)
			Serial.print(EOA);
	}
	Serial.println(END);
}

void SM_Actions()
{
	if(Serial.available() > 0)
		inByte = Serial.read();

	switch(state)
	{
		case STOPPED:
			commandIndex = 0;
			break;
		case PARSE:
			if(inByte >= '0' && inByte <= '9')
				commandString[commandIndex++] = inByte;	
			pollingRate = char2num(commandString, commandIndex);
			break;
		case MATCH:
			for(int i = 0; i < SENSOR_PINS; ++i)
				potResistance[i] = matchResistance(0, 255, i);
			break;
		case READ:
			readValues();
			break;
		case SEND:
			sendValues();
			break;
	}
}

void SM_State()
{
	switch(state)
	{
		case STOPPED:
			if(inByte == START)
				state = PARSE;
			break;
		case PARSE:
			if(inByte == EOA)
			{
				state = MATCH;
				commandIndex = 0;
			}
			break;
		case MATCH:
			if(inByte == END)
				state = STOPPED; 
			else
				state = READ;
			break;
		case READ:
			if(inByte == END)
				state = STOPPED; 
			else
				state = SEND;
			break;
		case SEND:
			if(inByte == END)
				state = STOPPED; 
			else
				state = MATCH;
			break;
	}
}
