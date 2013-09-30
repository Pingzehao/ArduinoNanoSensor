#include <GPS.h>
#include <NewSoftSerial.h>
#include <Serial_LCD.h>
#include <SPI.h>

Serial_LCD LCD(6, 5);

const int TOUCH_NONE = 0;
const int TOUCH_DOWN = 1;
const int TOUCH_UP = 2;
const int TOUCH_MOVING = 3;
const int LANDSCAPE = 2;
const int TEXT_HEIGHT_LARGEST = 16;
const int TEXT_WIDTH_LARGEST = 12;
const int WHITE = 65535;
const int BLACK = 0;

const int CHIP_PINS = 5;

const int SLAVE_SELECT_PIN = 10;

const int STOPPED = 0;
const int CHIP_VIEW = 1;
const int GPS_VIEW = 2;

const int BAR_MID = 120;
const int WIDTH = 320;
const int HEIGHT = 240;
const int RECT_WIDTH = WIDTH / CHIP_PINS;
const double PERCENT_SCALE = 2;
const double MIN_DELTA = 0.05;

const double INPUT_VOLTAGE = 5.0;
const double REFERENCE_VOLTAGE = 5.0;

int x = 0;
int y = 0;
int deviceStatus = STOPPED;
int counter = 0; 
char NMEA[75];
bool viewOptions = false;
bool haveGPSData = false;

long dividerResistance[CHIP_PINS] = {0};
long initialResistance[CHIP_PINS] = {0};
double previousDelta[CHIP_PINS] = {0};

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
};

Button startButton(0, 0, 320, 240, WHITE, "Touch to Start");
Button stopButton(0, 0, 75, 75, WHITE, "Stop");
Button gpsButton(0, 75, 75, 75, WHITE, "GPS");
Button chipButton(0, 75, 75, 75, WHITE, "Chip");
Button calibrateButton(0, 150, 75, 75, WHITE, "Match");

void setup()
{
	Serial.begin(4800);

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

	//TODO: Make it bootable even w/o SD in
	startButton.draw();
}

void loop()
{
	if(deviceStatus == STOPPED)
	{
		if(LCD.getTouchActivity() == 2)
		{
			LCD.getTouchXY(x, y);
			LCD.tText(3, 0, WHITE, x);
			LCD.tText(3, 1, WHITE, y);
			if(startButton.isClicked(x, y))
			{
				deviceStatus = CHIP_VIEW;
				calibratePot();
				LCD.clear();
				LCD.appendString2File("Data", "\nDate\tTime\tLongitude\tLatitude\tR0\tR1\tR2\tR3\tR4\n");
				x = -1;
				y = -1;
			}
		}
	}
	else if(deviceStatus != STOPPED)
	{
		checkInput();
		readGPS();
		readChip();
		if(viewOptions)
		{
			drawOptions();
		}
	}
}

void checkInput()
{
	if(LCD.getTouchActivity() == 2)
	{
		LCD.getTouchXY(x, y);
		if(viewOptions && deviceStatus == CHIP_VIEW)
		{			
			if(gpsButton.isClicked(x, y))
			{
				deviceStatus = GPS_VIEW;
				viewOptions = false;
				LCD.clear();
			}
			else if(calibrateButton.isClicked(x, y))
				calibratePot();
			else if(stopButton.isClicked(x, y))
			{
				deviceStatus = STOPPED;
				viewOptions = false;
				startButton.draw();
			}
			else
			{
				viewOptions = false;
				LCD.clear();
			}
		}
		else if(viewOptions && deviceStatus == GPS_VIEW)
		{
			if(chipButton.isClicked(x, y))
			{
				deviceStatus = CHIP_VIEW;
				viewOptions = false;
				LCD.clear();
			}
			else if(stopButton.isClicked(x, y))
			{
				deviceStatus = STOPPED;
				viewOptions = false;
				startButton.draw();
			}
			else
			{
				viewOptions = false;
				LCD.clear();
			}
		}
		else if(!viewOptions)
			viewOptions = true;
		x = -1;
		y = -1;
	}
}

void readChip()
{
	String toWrite = "";
	if(!haveGPSData)
	{
		toWrite = "NO GPS DATA\t\t\t\t";
		LCD.appendString2File("Data", toWrite);
	}
	else if(haveGPSData)
		haveGPSData = false;
	
	for(int i = 0; i < CHIP_PINS; ++i)
	{
		//Read the voltage and calculate the resistance and percent delta
		int bitVoltage = analogRead(i);
		double voltageOut = (double) bitVoltage / 1023.0 * REFERENCE_VOLTAGE;
		long currentResistance = (voltageOut * dividerResistance[i]) / (INPUT_VOLTAGE - voltageOut);
		
		double percentDelta =  (double) currentResistance / initialResistance[i];
		if(percentDelta < 1)
		{
			percentDelta = 1.0 / percentDelta;
			percentDelta--;
			percentDelta = -percentDelta;
		}
		else
			percentDelta--;
		
		
		String toWrite = (int) (percentDelta * 100);
		toWrite += ".";
		int decimalValue = ((int) absVal(percentDelta * 10000)) % 100;
		toWrite += decimalValue;
		
		LCD.appendString2File("Data", toWrite + "\t");
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
				LCD.rectangle(rectLeft, 0, rectRight, HEIGHT, BLACK);
				previousDelta[i] = percentDelta;
			}
			LCD.rectangle(rectLeft, rectTop, rectRight, rectBot, WHITE);
		}
	}
	LCD.appendString2File("Data", "\n");
}

void readGPS()
{
	
	while(Serial.available() > 0)
	{
		char inChar = Serial.read();
		if(inChar == '$')
			counter = 0;
		NMEA[counter++] = inChar;
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
				toWrite = (long) (gpsData.getLongMinutes() / 60 * 100000);
				LCD.appendString2File("Data", toWrite);
				toWrite = gpsData.getLongHeading();
				LCD.appendString2File("Data", toWrite);
				toWrite = "\t";
				LCD.appendString2File("Data", toWrite);
				
				toWrite = gpsData.getLatDegrees();
				LCD.appendString2File("Data", toWrite);
				toWrite = ".";
				LCD.appendString2File("Data", toWrite);
				toWrite = (long) (gpsData.getLatMinutes() / 60 * 100000);
				LCD.appendString2File("Data", toWrite);
				toWrite = gpsData.getLatHeading();
				LCD.appendString2File("Data", toWrite);
				toWrite = "\t";
				LCD.appendString2File("Data", toWrite);
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
void drawOptions()
{
	if(deviceStatus == CHIP_VIEW)
	{
		stopButton.draw();
		gpsButton.draw();
		calibrateButton.draw();
	}
	else if(deviceStatus == GPS_VIEW)
	{
		stopButton.draw();
		chipButton.draw();
	}
}

void calibratePot()
{
	for(int i = 0; i < CHIP_PINS; ++i)
	{
		dividerResistance[i] = (long) ((double) matchResistance(0, 255, i) * 100000.0 / 255.0);
		double voltageOut = analogRead(i) * 5.0 / 1023;
		initialResistance[i] = (voltageOut * dividerResistance[i]) / (INPUT_VOLTAGE - voltageOut);
	}
}

int matchResistance(int low, int high, int pin)
{
	int mid = (low + high) / 2;
	digitalPotWrite(pin, mid);
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