#include <GPS.h>
#include <NewSoftSerial.h>
#include <Serial_LCD.h>
#include <SPI.h>
#include <OneWire.h>
#include <DallasTemperature.h>


//NewSoftSerial on Mega2560 can only recieve on the following:
//{10, 11, 12, 13, 50, 51, 52, 53, 62, 63, 64, 65, 66, 67, 68, 69}
Serial_LCD LCD(11, 12);

const int ONE_WIRE_BUS = 6;

const int PUMP_POWER1 = 7;
const int PUMP_POWER2 = 8;
const int PUMP_POWER3 = 9;

const int PUMP_CONTROL = 7;

const int VALVE_CONTROL1 = 22;
const int VALVE_CONTROL2 = 23;

const int MUX_SELECT1 = 26;
const int MUX_SELECT2 = 27;
const int MUX_SELECT3 = 28;
const int MUX_SELECT4 = 29;

const int TOUCH_NONE = 0;
const int TOUCH_DOWN = 1;
const int TOUCH_UP = 2;
const int TOUCH_MOVING = 3;
const int LANDSCAPE = 2;
const int TEXT_HEIGHT_LARGEST = 16;
const int TEXT_WIDTH_LARGEST = 12;
const int WHITE = 65535;
const int BLACK = 0;

const int CHIP_PINS = 16;

const int SLAVE_SELECT_PIN = 53;

const int HIDE_OPTIONS = -1;
const int STOPPED = 0;
const int CHIP_VIEW = 1;
const int GPS_VIEW = 2;
const int TEMP_VIEW = 3;

const int BAR_MID = 120;
const int WIDTH = 320;
const int HEIGHT = 240;
const int RECT_WIDTH = WIDTH / CHIP_PINS;
const double PERCENT_SCALE = 2;
const double MIN_DELTA = 0.05;

const double INPUT_VOLTAGE = 5.0;
const double REFERENCE_VOLTAGE = 5.0;

const int BUTTON_HEIGHT = HEIGHT / 3;
const int BUTTON_WIDTH = 80;

bool pumpOn = false;

unsigned long pumpEndMillis = 0;

int x = 0;
int y = 0;
int deviceStatus = STOPPED;
int counter = 0; 
char NMEA[100];
bool viewOptions = false;
bool haveGPSData = false;

bool writeToSD = false;

int dividerBitResistance[CHIP_PINS] = {0};
long dividerResistance[CHIP_PINS] = {0};
long initialResistance[CHIP_PINS] = {0};
double previousDelta[CHIP_PINS] = {0};

OneWire digitalTemp(ONE_WIRE_BUS);
DallasTemperature temperatureSensor(&digitalTemp);

class Button
{
private:
	int mX;
	int mY;
	int mHeight;
	int mWidth;
	int mColor;
	String mText;
	
public:
	Button()
	{
		mX = -1;
		mY = -1;
		mHeight = -1;
		mWidth = -1;
		mColor = -1;
	}
	
	Button(int x, int y, int width, int height, unsigned int color, String text)
	{
		mX = x;
		mY = y;
		mWidth = width;
		mHeight = height;
		mColor = color;
		mText = text;
	}
	
	bool isClicked(int x, int y)
	{
		if(x < mX + mWidth && x > mX)
			if(y < mY + mHeight && y > mY)
				return true;
		return false;
	}
	
	void draw()
	{
		LCD.rectangle(mX, mY, mX + mWidth, mY + mHeight, mColor);
		int length = mText.length();
		int textX = mX + (mWidth - length * TEXT_WIDTH_LARGEST) / 2;
		int textY = mY + (mHeight - TEXT_HEIGHT_LARGEST) / 2;
		LCD.gText(textX, textY, 0, mText);
	}
	
	void setColor(int color)
	{
		mColor = color;
	}
	
	void setPos(int x, int y)
	{
		mX = x;
		mY = y;
	}
};

Button stopButton(0, 0, BUTTON_WIDTH, BUTTON_HEIGHT, WHITE, "Stop");
Button gpsButton(0, 80, BUTTON_WIDTH, BUTTON_HEIGHT, WHITE, "GPS");
Button tempButton(0, 160, BUTTON_WIDTH, BUTTON_HEIGHT, WHITE, "Temp");
Button chipButton(0, 160, BUTTON_WIDTH, BUTTON_HEIGHT, WHITE, "Chip");
Button calibrateButton(245, 0, BUTTON_WIDTH, BUTTON_HEIGHT, WHITE, "Match");
Button oneMinuteButton(20, BUTTON_HEIGHT, BUTTON_WIDTH, BUTTON_HEIGHT, WHITE, "1 min");
Button fiveMinuteButton(BUTTON_WIDTH + 40, BUTTON_HEIGHT, BUTTON_WIDTH, BUTTON_HEIGHT, WHITE, "5 min");
Button tenMinuteButton(BUTTON_WIDTH * 2 + 60, BUTTON_HEIGHT, BUTTON_WIDTH, BUTTON_HEIGHT, WHITE, "10 min");


void setup()
{
	Serial.begin(9600);
	
	LCD.begin();
	LCD.setTouch(true);
	LCD.setPenSolid(true);
	LCD.setFontSolid(false);
	LCD.setOrientation(LANDSCAPE);
	LCD.setFont(0x03);	
	
	//Initialize Digital Potentiometer
	pinMode(SLAVE_SELECT_PIN, OUTPUT);
	digitalWrite(SLAVE_SELECT_PIN, HIGH);
	SPI.begin(); 
	
	// Initialize temperature sensor
	temperatureSensor.begin();

	pinMode(PUMP_CONTROL, OUTPUT);
	setPumpOn(false);
	
	//TODO: Make it bootable even w/o SD in
	//pinMode(PUMP_POWER1, OUTPUT);
	//pinMode(PUMP_POWER2, OUTPUT);
	//pinMode(PUMP_POWER3, OUTPUT);
	//setPumpOn(false);
	
	pinMode(VALVE_CONTROL1, OUTPUT);
	pinMode(VALVE_CONTROL2, OUTPUT);
	digitalWrite(VALVE_CONTROL1, HIGH);
	digitalWrite(VALVE_CONTROL2, HIGH);	
	oneMinuteButton.draw();
	fiveMinuteButton.draw();
	tenMinuteButton.draw();
	LCD.tText(1, 2, WHITE, "Pick a calibration period");
}

void loop()
{
	if(deviceStatus == STOPPED)
	{
		if(LCD.getTouchActivity() == TOUCH_UP)
		{
			LCD.getTouchXY(x, y);
			if(oneMinuteButton.isClicked(x, y))
			{
				pumpEndMillis = millis() + 300;
				digitalWrite(VALVE_CONTROL1, LOW);
				digitalWrite(VALVE_CONTROL2, HIGH);
				initializePolling();
			}
			else if(fiveMinuteButton.isClicked(x, y))
			{
				pumpEndMillis = millis() + 300000;
				digitalWrite(VALVE_CONTROL1, LOW);
				digitalWrite(VALVE_CONTROL2, HIGH)
				initializePolling();
			}
			else if(tenMinuteButton.isClicked(x, y))
			{
				pumpEndMillis = millis() + 600000;
				digitalWrite(VALVE_CONTROL1, LOW);
				digitalWrite(VALVE_CONTROL2, HIGH);
				initializePolling();
			}
		}
	}
	else if(deviceStatus != STOPPED)
	{
		checkInput();
		readGPS();
		checkInput();
		readChip();
		checkInput();
		readTempHumid();
		checkInput();

		if(viewOptions)
		{
			drawOptions();
		}
		
		checkTime();
	}
}

void initializePolling()
{
	if(writeToSD)
		LCD.appendString2File("Data", "\nDate\tTime\tLongitude\tLatitude\tR0\tR1\tR2\tR3\tR4\tR5\tR6\tR7\tR8\tR9\tR10\tR11\tR12\tR13\tR14\tR15\tTemp\tRH%\n");
	setPumpOn(true);
	deviceStatus = CHIP_VIEW;
	LCD.clear();
	x = -1;
	y = -1;
}

void checkTime()
{
	if(millis() > pumpEndMillis)
	{
		calibratePot();
		digitalWrite(VALVE_CONTROL1, HIGH);
		digitalWrite(VALVE_CONTROL2, LOW);
	}
}

void checkInput()
{
	if(LCD.getTouchActivity() == 1)
	{
		LCD.getTouchXY(x, y);
		if(viewOptions && deviceStatus == CHIP_VIEW)
		{			
			if(stopButton.isClicked(x, y))
				setView(STOPPED);
			else if(gpsButton.isClicked(x, y))
				setView(GPS_VIEW);
			else if(tempButton.isClicked(x, y))
				setView(TEMP_VIEW);
			else if(calibrateButton.isClicked(x, y))
			{
				calibratePot();
				setView(HIDE_OPTIONS);
			}
			else
				setView(HIDE_OPTIONS);
		}
		else if(viewOptions && deviceStatus == GPS_VIEW)
		{
			if(stopButton.isClicked(x, y))
				setView(STOPPED);
			else if(chipButton.isClicked(x, y))
				setView(CHIP_VIEW);
			else if(tempButton.isClicked(x, y))
				setView(TEMP_VIEW);
			else
				setView(HIDE_OPTIONS);
		}
		else if(viewOptions && deviceStatus == TEMP_VIEW)
		{
			if(stopButton.isClicked(x, y))
				setView(STOPPED);
			else if(gpsButton.isClicked(x, y))
				setView(GPS_VIEW);
			else if(chipButton.isClicked(x, y))
				setView(CHIP_VIEW);
			else
				setView(HIDE_OPTIONS);
		}
		else if(!viewOptions)
			viewOptions = true;
		x = -1;
		y = -1;
	}
}

void setView(int view)
{
	if(view != -1)
		deviceStatus = view;
	viewOptions = false;
	LCD.clear();
	if(view == STOPPED)
	{
		setPumpOn(false);
		digitalWrite(VALVE_CONTROL1, HIGH);
		digitalWrite(VALVE_CONTROL2, HIGH);
		oneMinuteButton.draw();
		fiveMinuteButton.draw();
		tenMinuteButton.draw();
		LCD.tText(1, 2, WHITE, "Pick a calibration period");
	}
}

void readGPS()
{
	while(Serial.available() > 0)
	{
		char inChar = Serial.read();
		if(inChar == '$')
			counter = 0;
		NMEA[counter++] = inChar;
		//TODO: Calculate the checksum by using XOR and matching it?
		if(NMEA[5] == 'C' && NMEA[counter - 3] == '*')
		{
			GPS gpsData(NMEA);
			//print(gpsData);
			if(gpsData.isConnected())
			{
				if(deviceStatus == GPS_VIEW)
				{
					LCD.clear();
					//Draw Date
					if(gpsData.getMonth() < 10)
					{
						LCD.tText(0, 0, WHITE, "0");
						LCD.tText(1, 0, WHITE, gpsData.getMonth());
					}
					else
						LCD.tText(0, 0, WHITE, gpsData.getMonth());
					LCD.tText(2, 0, WHITE, "/");
					if(gpsData.getDay() < 10)
					{
						LCD.tText(3, 0, WHITE, "0");
						LCD.tText(4, 0, WHITE, gpsData.getDay());
					}
					else
						LCD.tText(3, 0, WHITE, gpsData.getDay());
					
					LCD.tText(5, 0, WHITE, "/");
					LCD.tText(6, 0, WHITE, 20);
					LCD.tText(8, 0, WHITE, gpsData.getYear());
					
					//Draw Time
					if(gpsData.getHour() < 10)
					{
						LCD.tText(0, 1, WHITE, "0");
						LCD.tText(1, 1, WHITE, gpsData.getHour());
					}
					else
						LCD.tText(0, 1, WHITE, gpsData.getHour());
					LCD.tText(2, 1, WHITE, ":");
					
					if(gpsData.getMinute() < 10)
					{
						LCD.tText(3, 1, WHITE, "0");
						LCD.tText(4, 1, WHITE, gpsData.getMinute());
					}
					else
						LCD.tText(3, 1, WHITE, gpsData.getMinute());
					LCD.tText(5, 1, WHITE, ":");
					
					if(gpsData.getSecond() < 10)
					{
						LCD.tText(6, 1, WHITE, "0");
						LCD.tText(7, 1, WHITE, gpsData.getSecond());
					}
					else
						LCD.tText(6, 1, WHITE, gpsData.getSecond());
					
					//Draw Latitude
					if(gpsData.getLatDegrees() < 10)
					{
						LCD.tText(0, 2, WHITE, "0");
						LCD.tText(0, 2, WHITE, gpsData.getLatDegrees());
					}
					else
						LCD.tText(0, 2, WHITE, gpsData.getLatDegrees());
					LCD.tText(2, 2, WHITE, "*");
					LCD.tText(5, 2, WHITE, gpsData.getLatMinutes());
					LCD.tText(10, 2, WHITE, "'");
					LCD.tText(11, 2, WHITE, gpsData.getLatHeading());
					
					//Draw Longitude
					if(gpsData.getLongDegrees() < 100)
					{
						if(gpsData.getLongDegrees() < 10)
						{
							LCD.tText(0, 3, WHITE, "00");
							LCD.tText(2, 3, WHITE, gpsData.getLongDegrees());
						}
						else
						{
							LCD.tText(0, 3, WHITE, "0");
							LCD.tText(1, 3, WHITE, gpsData.getLongDegrees());
						}
					}
					else
						LCD.tText(0, 3, WHITE, gpsData.getLongDegrees());
					LCD.tText(3, 3, WHITE, "*");
					LCD.tText(5, 3, WHITE, gpsData.getLongMinutes());
					LCD.tText(10, 3, WHITE, "'");
					LCD.tText(11, 3, WHITE, gpsData.getLongHeading());
				}
				
				if(writeToSD)
				{
					haveGPSData = true;
					String toWrite = gpsData.getMonth();
					LCD.appendString2File("Data", toWrite);
					toWrite = "/";
					LCD.appendString2File("Data", toWrite);
					toWrite = gpsData.getDay();
					LCD.appendString2File("Data", toWrite);
					toWrite = "/";
					LCD.appendString2File("Data", toWrite);
					toWrite = gpsData.getYear();
					LCD.appendString2File("Data", toWrite);
					toWrite = "\t";
					
					LCD.appendString2File("Data", toWrite);
					
					toWrite = gpsData.getHour();
					LCD.appendString2File("Data", toWrite);
					toWrite = ":";
					LCD.appendString2File("Data", toWrite);
					toWrite = gpsData.getMinute();
					LCD.appendString2File("Data", toWrite);
					toWrite = ":";
					LCD.appendString2File("Data", toWrite);
					toWrite = gpsData.getSecond();
					LCD.appendString2File("Data", toWrite);
					toWrite = "\t";
					LCD.appendString2File("Data", toWrite);
					
					toWrite = gpsData.getLongDegrees();
					LCD.appendString2File("Data", toWrite);
					toWrite = ".";
					LCD.appendString2File("Data", toWrite);
					toWrite = (long) (gpsData.getLongMinutes() / 60 * 1000000);
					LCD.appendString2File("Data", toWrite);
					toWrite = gpsData.getLongHeading();
					LCD.appendString2File("Data", toWrite);
					toWrite = "\t";
					LCD.appendString2File("Data", toWrite);
					
					toWrite = gpsData.getLatDegrees();
					LCD.appendString2File("Data", toWrite);
					toWrite = ".";
					LCD.appendString2File("Data", toWrite);
					toWrite = (long) (gpsData.getLatMinutes() / 60 * 1000000);
					LCD.appendString2File("Data", toWrite);
					toWrite = gpsData.getLatHeading();
					LCD.appendString2File("Data", toWrite);
					toWrite = "\t";
					LCD.appendString2File("Data", toWrite);
				}
			}
			else if(deviceStatus == GPS_VIEW)
			{
				LCD.tText(7, 7, WHITE, "NO GPS SIGNAL");
				haveGPSData = false;
			}
			else
				haveGPSData = false;
		}
		else if(counter >= 75)
			counter = 0;
	}
}

void readChip()
{
	if(writeToSD)
	{
		String toWrite = "";
		if(!haveGPSData)
		{
			toWrite = "NO GPS DATA\t\t\t\t";
			LCD.appendString2File("Data", toWrite);
		}
		else if(haveGPSData)
			haveGPSData = false;
	}
	for(int i = 0; i < CHIP_PINS; ++i)
	{
		setMUX(i);
		int resistance = matchResistance(0, 255);
		long divider = (long) ((double) resistance / 255.0 * 100000.0);
		int bitVoltage = analogRead(0);
		//Read the voltage and calculate the resistance and percent delta
		digitalPotWrite(0, dividerBitResistance[i]);
		int bitVoltage = analogRead(i);
		double voltageOut = (double) bitVoltage / 1023.0 * REFERENCE_VOLTAGE;
		long currentResistance = (voltageOut * dividerResistance[i]) / (INPUT_VOLTAGE - voltageOut);
		
		double percentDelta = (double) (currentResistance - initialResistance[i]) / initialResistance[i];
		
		if(writeToSD)
		{
			String toWrite = (int) (percentDelta * 100);
			toWrite += ".";
			int decimalValue = ((int) absVal(percentDelta * 10000)) % 100;
			toWrite += decimalValue;
		
			LCD.appendString2File("Data", toWrite + "\t");
		}
		//Draw the rectangles if currently viewing chip
		if(deviceStatus == CHIP_VIEW)
		{
			//Get the coordinates of the rectangles based on the delta
			int rectLeft = (int) (RECT_WIDTH * i);
			int rectRight = (int) (rectLeft + RECT_WIDTH);
			int rectTop = BAR_MID;
			int rectBot = BAR_MID;
			long rectDelta = BAR_MID * percentDelta / PERCENT_SCALE;
			//Swap the top and bottom depending on if it goes up or down.
			if(percentDelta < 0)
				rectTop += absVal(rectDelta);
			else
				rectBot -= absVal(rectDelta);
			if(percentDelta != previousDelta[i])
			{
				if(i < 4 && viewOptions);
				else if(i > 11 && viewOptions)	//Don't draw over the button (limit height)
					LCD.rectangle(rectLeft, BUTTON_HEIGHT, rectRight, HEIGHT, BLACK);
				else
					LCD.rectangle(rectLeft, 0, rectRight, HEIGHT, BLACK);
				previousDelta[i] = percentDelta;
			}
			
			if(i < 4 && viewOptions);		//Don't draw at all
			else if(i > 11 && viewOptions)	//Don't draw over the button (limit height)
			{
				if(rectTop < BUTTON_HEIGHT)
					rectTop = BUTTON_HEIGHT;
				LCD.rectangle(rectLeft, rectTop, rectRight, rectBot, WHITE);
			}
			else 
				LCD.rectangle(rectLeft, rectTop, rectRight, rectBot, WHITE);
		}
	}
}

void readTempHumid()
{
	temperatureSensor.requestTemperatures();
	double tempCelcius = temperatureSensor.getTempCByIndex(0);
	String toDrawTemp = doubleToString(tempCelcius, 2);
	
	
	double humidityVoltage = (double) analogRead(5) / 1024 * REFERENCE_VOLTAGE;
	double humidityPercentage = (humidityVoltage / REFERENCE_VOLTAGE - 0.16) / 0.0062;
	double relativeHumidity = humidityPercentage / (1.0546 - 0.00216 * tempCelcius);
	
	if(relativeHumidity > 100)
		relativeHumidity = 100;
	else if(relativeHumidity < 0)
		relativeHumidity = 0;
		
	String toDrawHumid = doubleToString(relativeHumidity, 2);
	if(deviceStatus == TEMP_VIEW)
	{
		LCD.rectangle(81, 0, 320, 240, BLACK);
		LCD.tText(7, 6, WHITE, "Temp(C): " + toDrawTemp + "C");
		LCD.tText(7, 7, WHITE, "RH%: " + toDrawHumid + "%");
	}
	
	if(writeToSD)
	{
		String toWrite = toDrawTemp + "\t" + toDrawHumid + "\n";
		LCD.appendString2File("Data", toWrite);
	}
}

void drawOptions()
{
	if(deviceStatus == CHIP_VIEW)
	{
		stopButton.draw();
		gpsButton.draw();
		tempButton.draw();
		calibrateButton.draw();
	}
	else if(deviceStatus == GPS_VIEW)
	{
		stopButton.draw();
		chipButton.setPos(0, 80);
		chipButton.draw();
		tempButton.draw();
	}
	else if(deviceStatus == TEMP_VIEW)
	{
		stopButton.draw();
		chipButton.setPos(0, 160);
		chipButton.draw();
		gpsButton.draw();
	}
}

void calibratePot()
{
	//Draw four buttons until one is clicked, 1 min, 5 min, 10 min, 15 min?
	//Turn on pump for that long while repeatedly taking resistances and stuff.
	for(int i = 0; i < CHIP_PINS; ++i)
	{
		setMUX(i);
		dividerBitResistance[i] = matchResistance(0, 255);
		dividerResistance[i] = (long) ((double) dividerBitResistance[i] * 100000.0 / 255.0);
		double voltageOut = (double) analogRead(0) * 5.0 / 1023.0;
		initialResistance[i] = (voltageOut * dividerResistance[i]) / (INPUT_VOLTAGE - voltageOut);
	}
}

int matchResistance(int low, int high, int pin)
{
	int mid = (low + high) / 2;
	digitalPotWrite(0, mid);
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

double absVal(double d)
{
	if(d < 0)
	return -d;
	else
	return d;
}	

String doubleToString(double d, int precision)
{
	long wholeNumber = (long) d;
	long decimalNumber = ((long) (d * 100)) % 100;
	String temp(wholeNumber);
	temp += ".";
	String decimal(decimalNumber);
	temp += decimal;
	return temp;
}

void setPumpOn(bool setPump)
{
	//TODO: Modify to using a transistor (HIGH for no flow)
	pumpOn = setPump;
	if(pumpOn)
	{
		digitalWrite(PUMP_CONTROL, LOW);
	}
	else
	{
		digitalWrite(PUMP_CONTROL, HIGH);
	}
}

void setMUX(int i)
{
	if(i & 0x0001)
		digitalWrite(MUX_SELECT1, HIGH);
	else
		digitalWrite(MUX_SELECT1, LOW);
	if(i & 0x0002)
		digitalWrite(MUX_SELECT2, HIGH);
	else
		digitalWrite(MUX_SELECT2, LOW);
	if(i & 0x0004)
		digitalWrite(MUX_SELECT3, HIGH);
	else
		digitalWrite(MUX_SELECT3, LOW);
	if(i & 0x0008)
		digitalWrite(MUX_SELECT4, HIGH);
	else
		digitalWrite(MUX_SELECT4, LOW);
}	
