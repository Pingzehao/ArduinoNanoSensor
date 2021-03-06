/*
*	NanoStandAlone.cpp - Sketch for the stand alone Arduino based system. 
*						 Handles control of the device reading of data, parsing, 
*						 displaying and saving it.
*	Copyright(c) 2011 University of California, Riverside
*	Author: Albert Chen
*
*/

#include <GPS.h>
#include <NewSoftSerial.h>
#include <Oled.h>
#include <SPI.h>
#include <OneWire.h>
#include <DallasTemperature.h>

const int SWITCH_PIN = 2;
const int RX_PIN = 6;
const int TX_PIN = 5;
const int LED_PIN = 13;
const int SLAVE_SELECT_PIN = 10;
const int ONE_WIRE_BUS = 8;

//Battery power
const int SWITCH_UP = 1021;
const int SWITCH_DOWN = 405;
const int SWITCH_IN = 509;


/*USB powered
const int SWITCH_UP = 687;
const int SWITCH_DOWN = 217;
const int SWITCH_IN	= 343;*/


const int KEY_NONE = 0;
const int KEY_IN = 1;
const int KEY_DOWN = 2;
const int KEY_UP = 3;

const int STOPPED = 0;
const int CHIP = 1;
const int TEMP_HUMIDITY = 2;
const int GPS_VIEW = 3;

const int HOLD_DELAY = 2000;
const int SWITCH_VOLTAGE_GAP = 50;
const int CHIP_PINS = 5;
const double REFERENCE_VOLTAGE = 5.0;
const double INPUT_VOLTAGE = 5.0;
const int TEMP_SCALE_CONST = 39;

const int BAR_MID = 64 / 2;
const int PERCENT_SCALE = 2;
const int RECT_WIDTH = 96 / CHIP_PINS;

NewSoftSerial mySerial = NewSoftSerial(RX_PIN, TX_PIN);
byte pinState = 0;
char NMEA[100];
int counter = 0;
long dividerResistance[CHIP_PINS] = {0};
long initialResistance[CHIP_PINS] = {0};
long previousResistance[CHIP_PINS] = {0};

OLED myOLED(7, 8, 57600, 2500);

const int GREEN = myOLED.get16bitRGB(0, 255, 0);
const int BLACK = myOLED.get16bitRGB(0, 0, 0);
const int WHITE = myOLED.get16bitRGB(255, 255, 255);

GPS myGPS;

int deviceStatus = STOPPED;
long holdStart = 0;
long holdDuration = 0;
int lastKey = 0;
bool haveGPSData = false;

OneWire digitalTemp(ONE_WIRE_BUS);
DallasTemperature temperatureSensor(&digitalTemp);

void setup()
{ 
	//Initialize digital temperature sensor
	temperatureSensor.setWaitForConversion(true);
	temperatureSensor.begin();

	//Set switch pin to output and high to power the switch
	pinMode(SWITCH_PIN, OUTPUT);
	digitalWrite(SWITCH_PIN, HIGH);
	
	//Initialize SoftwareSerial
	mySerial.begin(4800);
	delay(100);
	
	//Initialize Digital Potentiometer
	pinMode(SLAVE_SELECT_PIN, OUTPUT);
	digitalWrite(SLAVE_SELECT_PIN, HIGH);
	SPI.begin(); 
	digitalPotWrite(5, 5);
	delay(100);
	
	//Initialize OLED
	Serial.begin(57600);
	myOLED.Init();
	myOLED.Clear();
	delay(100);

	//Initialize SD Card
	myOLED.SetSDAddress(0x00, 0x01, 0x00, 0x00);
	myOLED.writeSDString("\nDate\tTime\tLongitude\tLatitude\tR0\tR1\tR2\tR3\tR4\
		tTemp\tRH%\n");	
}

void loop()
{
	//Read the voltage input for the switch
	int selectorSwitch = analogRead(5);
	checkInput(selectorSwitch);

	if(deviceStatus == STOPPED)
	{
		myOLED.DrawText(1, 2, 0x02, "HOLD BUTTON", WHITE);
		myOLED.DrawText(3, 3, 0x02, "TO START", WHITE);
	}
	else
	{
		readGPS();
		readChip();
		readTempHumid();
	}
}

/**
*	readGPS reads in the GPS data, parses it, writes it to the SD card and draws
*	it to the LCD depending on if the user is currently in GPS mode or not.
*
*/
void readGPS()
{
	while(mySerial.available())
	{
		char inChar = mySerial.read();
		if(inChar == '$')
			counter = 0;
		NMEA[counter++] = inChar;
		
		//Have the complete string
		if(NMEA[5] == 'C' && NMEA[counter - 3] == '*')
		{
			//Create GPS object that parses data
			GPS gpsData(NMEA);
			if(gpsData.isConnected())
			{
				//Draws info to the LCD
				if(deviceStatus == GPS_VIEW)
				{
					myOLED.Clear();
					//Draw Date
					if(gpsData.getMonth() < 10)
					{
						myOLED.DrawText(0, 0, 0x02, "0", WHITE);
						myOLED.DrawText(1, 0, 0x02, gpsData.getMonth(), WHITE);
					}
					else
						myOLED.DrawText(0, 0, 0x02, gpsData.getMonth(), WHITE);
					myOLED.DrawText(2, 0, 0x02, "/", WHITE);
					if(gpsData.getDay() < 10)
					{
						myOLED.DrawText(3, 0, 0x02, "0", WHITE);
						myOLED.DrawText(4, 0, 0x02, gpsData.getMonth(), WHITE);
					}
					else
						myOLED.DrawText(3, 0, 0x02, gpsData.getMonth(), WHITE);
					
					myOLED.DrawText(5, 0, 0x02, "/", WHITE);
					myOLED.DrawText(6, 0, 0x02, 20, WHITE);
					myOLED.DrawText(8, 0, 0x02, gpsData.getYear(), WHITE);
				
					//Draw Time
					if(gpsData.getHour() < 10)
					{
						myOLED.DrawText(0, 1, 0x02, "0", WHITE);
						myOLED.DrawText(1, 1, 0x02, gpsData.getHour(), WHITE);
					}
					else
						myOLED.DrawText(0, 1, 0x02, gpsData.getHour(), WHITE);
					myOLED.DrawText(2, 1, 0x02, ":", WHITE);
				
					if(gpsData.getMinute() < 10)
					{
						myOLED.DrawText(3, 1, 0x02, "0", WHITE);
						myOLED.DrawText(4, 1, 0x02, gpsData.getMinute(), WHITE);
					}
					else
						myOLED.DrawText(3, 1, 0x02, gpsData.getMinute(), WHITE);
					myOLED.DrawText(5, 1, 0x02, ":", WHITE);
				
					if(gpsData.getSecond() < 10)
					{
						myOLED.DrawText(6, 1, 0x02, "0", WHITE);
						myOLED.DrawText(7, 1, 0x02, gpsData.getSecond(), WHITE);
					}
					else
						myOLED.DrawText(6, 1, 0x02, gpsData.getSecond(), WHITE);
				
					//Draw Latitude
					if(gpsData.getLatDegrees() < 10)
					{
						myOLED.DrawText(0, 2, 0x02, "0", WHITE);
						myOLED.DrawText(0, 2, 0x02, gpsData.getLatDegrees(), 
									WHITE);
					}
					else
						myOLED.DrawText(0, 2, 0x02, gpsData.getLatDegrees(), 
									WHITE);
					myOLED.DrawText(2, 2, 0x02, "*", WHITE);
					myOLED.DrawText(5, 2, 0x02, gpsData.getLatMinutes(), WHITE);
					myOLED.DrawText(10, 2, 0x02, "'", WHITE);
					myOLED.DrawText(11, 2, 0x02, gpsData.getLatHeading(), 
								WHITE);
				
					//Draw Longitude
					if(gpsData.getLongDegrees() < 100)
					{
						if(gpsData.getLongDegrees() < 10)
						{
							myOLED.DrawText(0, 3, 0x02, "00", WHITE);
							myOLED.DrawText(2, 3, 0x02, 
										gpsData.getLongDegrees(), WHITE);
						}
						else
						{
							myOLED.DrawText(0, 3, 0x02, "0", WHITE);
							myOLED.DrawText(1, 3, 0x02, 
										gpsData.getLongDegrees(), WHITE);
						}
					}
					else
						myOLED.DrawText(0, 3, 0x02, gpsData.getLongDegrees(), 
									WHITE);
					myOLED.DrawText(3, 3, 0x02, "*", WHITE);
					myOLED.DrawText(5, 3, 0x02, gpsData.getLongMinutes(), 
								WHITE);
					myOLED.DrawText(10, 3, 0x02, "'", WHITE);
					myOLED.DrawText(11, 3, 0x02, gpsData.getLongHeading(), 
								WHITE);
					haveGPSData = true;
				}
				
				//Write data to SD card
				String toWrite(gpsData.getMonth());
				toWrite += "/";
				toWrite += gpsData.getDay();
				toWrite += "/";
				toWrite += gpsData.getYear();
				toWrite += "\t";
				myOLED.writeSDString(toWrite);
				
				toWrite = gpsData.getHour();
				toWrite += ":";
				toWrite += gpsData.getMinute();
				toWrite += ":";
				toWrite += gpsData.getSecond();
				toWrite += "\t";
				myOLED.writeSDString(toWrite);
				
				toWrite = gpsData.getLongDegrees();
				toWrite += ".";
				toWrite += (long) (gpsData.getLongMinutes() / 60 * 1000000);
				toWrite += gpsData.getLongHeading();
				toWrite += "\t";
				myOLED.writeSDString(toWrite);
				
				toWrite = gpsData.getLatDegrees();
				toWrite += ".";
				toWrite += (long) (gpsData.getLatMinutes() / 60 * 1000000);
				toWrite += gpsData.getLatHeading();
				toWrite += "\t";
				myOLED.writeSDString(toWrite);
			}
			else if(deviceStatus == GPS_VIEW)
			{
				myOLED.DrawText(3, 2, 0x02, "NO GPS", WHITE);
				myOLED.DrawText(3, 3, 0x02, "SIGNAL", WHITE);
				haveGPSData = false;
			}
			else
				haveGPSData = false;
		}
		if(counter >= 75)
			counter = 0;
	}
}

/**
*	Reads the resistances of the chip using a simple voltage divider circuit. 
*	Knowing input voltage, the resistance of the potentiometer and the voltage 
*	at the node between the resistors using analogRead() the resistance of the 
*	chip can be calculated using V_out * R_pot / (V_in - V_out). It also writes 
*	the data to the SD card and displays it if the device is in chip mode
*
*/
void readChip()
{
	if(!haveGPSData)
	{
		String toWrite = "NO GPS DATA\t\t\t\t";
		myOLED.writeSDString(toWrite);
	}
	else if(haveGPSData)
	{
		haveGPSData = false;
	}
	for(int i = 0; i < CHIP_PINS; ++i)
	{
		int bitVoltage = analogRead(i);
		double voltageOut = (double) bitVoltage / 1023.0 * REFERENCE_VOLTAGE;
		long currentResistance = (voltageOut * dividerResistance[i]) / 
					(INPUT_VOLTAGE - voltageOut);
		
		
		double percentDelta = ((double) currentResistance / 
					(double) initialResistance[i]);
		if(percentDelta < 1)
		{
			percentDelta = 1.0 / percentDelta;
			percentDelta--;
			percentDelta = -percentDelta;
		}
		else
			percentDelta--;
		
		
		String toWrite = doubleToString(percentDelta, 2);
		toWrite += "\t";
		myOLED.writeSDString(toWrite);
		
		if(deviceStatus == CHIP)
		{
			int rectLeft = RECT_WIDTH * i;
			int rectRight = rectLeft + RECT_WIDTH;
			int rectTop = BAR_MID;
			int rectBot = BAR_MID;
			long rectDelta = BAR_MID * percentDelta / PERCENT_SCALE;
			
			if(percentDelta < 0)
				rectTop += absVal(rectDelta);
			else
				rectBot -= absVal(rectDelta);
				
			if(previousResistance[i] != currentResistance)
			{
				myOLED.DrawRectangle(rectLeft, 0, rectRight, 96, BLACK);
				previousResistance[i] = currentResistance;
			}
			myOLED.DrawRectangle(rectLeft, rectTop, rectRight, rectBot, GREEN);
		}
	}
}

/**
*	Reads the temperature from the sensor using the oneWire library. It then 
*	reads the humidity sensor using analogRead() and calculates the relative 
*	humidity %. Also saves the data to the SD card and draws it to the screen if
*	it's on Temp/Humidity mode.
*
*/
void readTempHumid()
{
	temperatureSensor.requestTemperatures();
	double tempCelcius = temperatureSensor.getTempCByIndex(0);
	
	double humidityVoltage = (double) analogRead(6) / 1023.0 * 
				REFERENCE_VOLTAGE;
	double humidityPercentage = (humidityVoltage / REFERENCE_VOLTAGE - 0.16) / 
				0.0062;
	double relativeHumidity = humidityPercentage / (1.0546 - 0.00216 * 
				tempCelcius);
	
	if(relativeHumidity > 100)
		relativeHumidity = 100;
	else if(relativeHumidity < 0)
		relativeHumidity = 0;
	
	String toWrite = doubleToString(tempCelcius, 2);
	toWrite += "\t";
	toWrite += doubleToString(relativeHumidity, 2);
	toWrite += "\n";
	myOLED.writeSDString(toWrite);
	
	if(deviceStatus == TEMP_HUMIDITY)
	{
		myOLED.Clear();
		myOLED.DrawText(0, 1, 0x02, "TEMP(C): ", WHITE);
		myOLED.DrawText(0, 2, 0x02, tempCelcius, WHITE);
		myOLED.DrawText(0, 3, 0x02, "RH%: ", WHITE);
		myOLED.DrawText(0, 4, 0X02, relativeHumidity, WHITE);
	}
	
}

/**
*	The switch controls the system by having three resistors creating two 
*	different voltage dividers and one direct input into the analogRead of the 
*	Arduino. With this there are three different input voltages each 
*	representing pushed in, up, or down.
*	
*	keyPressed returns which key is pressed as represented by a constant which 
*	allows for a small tolerance between the ideal voltage and the actual 
*	voltage
*
*	@param selectorSwitch
*		The bit voltage as read from the Arduino (0 - 1023)
*
*	@return
*		Returns an int representing the status of the switch as represented by 
*		the constants
*
*/
int keyPressed(int selectorSwitch)
{
	if(selectorSwitch < SWITCH_IN + SWITCH_VOLTAGE_GAP && selectorSwitch > SWITCH_IN - SWITCH_VOLTAGE_GAP)
		return KEY_IN;
	else if(selectorSwitch < SWITCH_DOWN + SWITCH_VOLTAGE_GAP && selectorSwitch > SWITCH_DOWN - SWITCH_VOLTAGE_GAP)
		return KEY_DOWN;
	else if(selectorSwitch < SWITCH_UP + SWITCH_VOLTAGE_GAP && selectorSwitch > SWITCH_UP - SWITCH_VOLTAGE_GAP)
		return KEY_UP;
	else if(selectorSwitch < SWITCH_VOLTAGE_GAP)
		return KEY_NONE;
}

/**
*	checkHold checks the button and starts a counter to see how long the button
*	has been pushed in for and controls the starting/stopping of the device
*
*/
void checkHold()
{
	if(holdDuration > HOLD_DELAY)
	{
		myOLED.Clear();
		if(deviceStatus == STOPPED)
		{
			//Matches the resistance on each channel of the potentiometer to the 
			//chip as closely as possible
			matchAllResistances();
			deviceStatus = CHIP;
		}
		else if(deviceStatus != STOPPED)
		{
			deviceStatus = STOPPED;
		}
		holdDuration = 0;
	}
	else if(holdDuration == 0)
	{
		holdStart = millis();
		holdDuration++;
	}
	else
		holdDuration = millis() - holdStart;
}

/**
*	checkInput takes the input of the switch and controls the status of the 
*	device
*
*	@param selectorSwitch
*		The bit voltage value as read from the Arduino (0 - 1023)
*		
*/
void checkInput(int selectorSwitch)
{
	//Get the current status of the switch
	int keyDown = keyPressed(selectorSwitch);
	
	//Prevent the switch input from being read multiple times until it's reset 
	//to neutral position
	if(lastKey != 0 && keyDown != 0 && keyDown != KEY_IN)
		return;
	
	lastKey = keyDown;
	
	switch(deviceStatus)
	{
		case STOPPED:
			//The switch is pushed in and its been held for more than HOLD_DELAY
			//milliseconds
			if(keyDown == KEY_IN)
				checkHold();
			else
				holdDuration = 0;
			break;
		case CHIP:
			//Push in switch to match resistance, then set first value to be the 
			//baseline
			if(keyDown == KEY_UP)
			{
				deviceStatus = GPS_VIEW;
				myOLED.Clear();
			}
			else if(keyDown == KEY_DOWN)
			{
				deviceStatus = TEMP_HUMIDITY;
				myOLED.Clear();
			}
			else if(keyDown == KEY_IN)
			{
				if(holdDuration == 1)
					matchAllResistances();
				checkHold();
			}
			else
				holdDuration = 0;
			break;
		case TEMP_HUMIDITY:
			if(keyDown == KEY_UP)
			{
				deviceStatus = CHIP;
				myOLED.Clear();
			}
			else if(keyDown == KEY_DOWN)
			{
				deviceStatus = GPS_VIEW;
				myOLED.Clear();
			}
			else if(keyDown == KEY_IN)
			{
				checkHold();
			}
			else
				holdDuration = 0;
			break;
		case GPS_VIEW:
			if(keyDown == KEY_UP)
			{
				deviceStatus = TEMP_HUMIDITY;
				myOLED.Clear();
			}
			else if(keyDown == KEY_DOWN)
			{
				deviceStatus = CHIP;
				myOLED.Clear();
			}
			else if(keyDown == KEY_IN)
			{
				checkHold();
			}
			else
				holdDuration = 0;
			break;
	}
}

/**
*	Matches the resistance on all the channels of the chip/potentiometer
*
*/
void matchAllResistances()
{
	for(int i = 0; i < CHIP_PINS; ++i)
	{
		dividerResistance[i] = (long) ((double) matchResistance(0, 255, i) * 
					100000.0 / 255.0);
		double voltageOut = analogRead(i) * 5.0 / 1023;
		initialResistance[i] = (voltageOut * dividerResistance[i]) / 
					(INPUT_VOLTAGE - voltageOut);
	}
}

/**
*	Matches the resistance of potentiometer to the chip on the given channel
*
*	@param low
*		The low end of the resistance to check
*	@param high
*		The highest resistance to check
*	@param pin
*		The pin to read on the Arduino
*	
*	@return
*		The digital value to set the potentiometer to
*/
int matchResistance(int low, int high, int pin)
{
	int mid = (low + high) / 2;
	digitalPotWrite(pin, mid);
	delay(10);
	int voltage = analogRead(pin);
	if(low > high)
	{
		return mid;
	}
	if(voltage < 512)
		return matchResistance(low, mid - 1, pin);
	else if(voltage > 512)
		return matchResistance(mid + 1, high, pin);
	else
		return mid;
}

/**
*	digitalPotWrite writes the bit representation of the resistance to the the 
*	given channel of the potentiometer
*
*	@param address
*		The channel of the potentiometer to write to
*	@param value
*		The bit value to set the resistance to (0 - 255 representing 32Ohms - 
*		100kOhms)
*/
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

/**
*	Converts a double to a string
*
*	@param d
*		Value to be converted
*	@param precision
*		The number of decimal places to get
*
*	@return
*		A String representation of the double
*/
String doubleToString(double d, long precision)
{
	long decimalPlaces = pow(10, precision);
	long wholeNumber = (int) d;
	long decimalNumber = (long)(d * precision) % precision;
	String toReturn(wholeNumber);
	toReturn += ".";
	toReturn += abs(decimalNumber);
	return toReturn;
}


/**
*	Returns the absolute value of a double
*	
*	@param d
*		Value to get the absolute value of
*	
*	@return
*		The absolute value of the parameter
*/
double absVal(double d)
{
	if(d < 0)
		return -d;
	else
		return d;
}