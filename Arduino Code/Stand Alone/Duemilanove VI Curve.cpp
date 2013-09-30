#include <SPI.h>
#include <NewSoftSerial.h>
#include <Serial_LCD.h>

const int SLAVE_SELECT_PIN = 10;
const double REFERENCE_VOLTAGE = 5.0;
const double CHIP_DIVIDER_RESISTANCE = 5.3;

const int WHITE = 65535;
const int BLACK = 0;
const int LANDSCAPE = 2;
const int TEXT_WIDTH = 8;

const int HEIGHT = 240;
const int Y_RANGE = 220;
const int Y_OFFSET = HEIGHT - Y_RANGE;
const int WIDTH = 320;
const int X_RANGE = 300;
const int X_OFFSET = WIDTH - X_RANGE;

//Max current in mA
const double MAX_CURRENT = 0.25;

const int POINTS_TO_STORE = 100;

double voltages[POINTS_TO_STORE];
double currents[POINTS_TO_STORE];
int arrayPtr = 0;
int dataCounter = 0;

Serial_LCD LCD(2, 3);

void setup()
{
	LCD.begin();
	LCD.setFontSolid(true);
	LCD.setOrientation(LANDSCAPE);
	LCD.setFont(0x02);	
	
	//Initialize Digital Potentiometer
	pinMode(SLAVE_SELECT_PIN, OUTPUT);
	digitalWrite(SLAVE_SELECT_PIN, HIGH);
	SPI.begin(); 
	
	digitalPotWrite(1, 13);
	
	Serial.begin(9600);
}

void loop()
{
	double slope = 0;
	double intercept = 0;

	LCD.rectangle(X_OFFSET, 0, WIDTH, Y_RANGE, BLACK);
	drawAxis();
	//Gather data
	for(int i = 55; i < 255; i++)
	{
		digitalPotWrite(0, i);
		double inputVoltage = ((double) analogRead(5)) / 1023.0 * 5.0;
		double outputVoltage = (double) analogRead(0) / 1023.0 * inputVoltage;
		double chipResistance = CHIP_DIVIDER_RESISTANCE * (inputVoltage / outputVoltage - 1);
		double chipCurrentmA = (inputVoltage - outputVoltage) / chipResistance;
		voltages[arrayPtr] = inputVoltage;
		currents[arrayPtr++] = chipCurrentmA;
		dataCounter++;
		if(arrayPtr >= POINTS_TO_STORE)
			arrayPtr = 0;
		if(dataCounter >= POINTS_TO_STORE)
			dataCounter = POINTS_TO_STORE;
			
		Serial.print(inputVoltage);
		Serial.print("\t");
		Serial.println(chipCurrentmA);
		int pointX = chipCurrentmA / MAX_CURRENT * X_RANGE + X_OFFSET;
		int pointY = (1 - inputVoltage / 5.0) * Y_RANGE;
		LCD.circle(pointX, pointY, 1, WHITE);
	}
	
	double sumOfProducts = 0;
	double sumOfX = 0;
	double sumOfY = 0;
	double sumOfXSquared = 0;		
	
	for(int i = 0; i < dataCounter; ++i)
	{
		sumOfProducts += voltages[i] * currents[i];
		sumOfX += currents[i];
		sumOfY += voltages[i];
		sumOfXSquared += currents[i] * currents[i];
	}
	
	slope = (dataCounter * sumOfProducts - sumOfX * sumOfY) / (dataCounter * sumOfXSquared - sumOfX * sumOfX);
	intercept = (sumOfY - slope * sumOfX) / dataCounter;
	
	
	int intSlope = (int) slope;
	int decSlope = (int) ((slope - intSlope) * 100);
	int intIntercept = (int) intercept;
	int decIntercept = (int) ((intercept - intIntercept) * 100);
	
	LCD.gText(75, 225, WHITE, "y = ");
	LCD.gText(75 + TEXT_WIDTH * 4, 225, WHITE, intSlope);
	LCD.gText(75 + TEXT_WIDTH * 6, 225, WHITE, ".");
	LCD.gText(75 + TEXT_WIDTH * 7, 225, WHITE, decSlope);
	LCD.gText(75 + TEXT_WIDTH * 9, 225, WHITE, "x + ");
	LCD.gText(75 + TEXT_WIDTH * 13, 225, WHITE, intIntercept);
	LCD.gText(75 + TEXT_WIDTH * 14, 225, WHITE, ".");
	LCD.gText(75 + TEXT_WIDTH * 15, 225, WHITE, decIntercept);
	int x1 = X_OFFSET;
	int y1 = (1 - intercept / 5.0) * Y_RANGE;
	int y2 = 0;
	int x2 = ((5.0 - intercept) / slope) / MAX_CURRENT * X_RANGE + X_OFFSET;
	LCD.line(x1, y1, x2, y2, LCD.rgb16(0, 0, 255));
	
	delay(5000);
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

void drawAxis()
{
	//y-axis
	LCD.line(Y_OFFSET, 0, Y_OFFSET, HEIGHT, WHITE);
	LCD.gText(0, 5, WHITE, "5v");
	LCD.gText(0, 205, WHITE, "0v");
	//x-axis
	LCD.line(0, Y_RANGE, WIDTH, Y_RANGE, WHITE);
	LCD.gText(25, 225, WHITE, "0mA");
	
	int maxCurrent = (int) (MAX_CURRENT * 100);
	String toPrint = "0.";
	toPrint += maxCurrent;
	toPrint += "mA";
	LCD.gText(265, 225, WHITE, toPrint);
}