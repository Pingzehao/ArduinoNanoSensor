/*!\mainpage uoled_library Documentation
*
*  uOLED - Library to communicate and control micro-OLED 4D Displays.
*  Created by Juan Manuel Castro R. (October 2009)
*  Released into the public domain.
*  Extended with some help from Jeroen alias Yot at arduino forum.
*
*  \section Mainpage_whatandhow Whan can you do with this library.
*
*  What?
*
*  \section Mainpage_comphardware Compatible hardware.
*
*  Some brabble on wich hardware is compatible with this library.
*
*  \section Mainpage_Howtohardware How to connect the wires.
*
*  Supply voltage, ground, Tx, Rx.
*  Optional sound output and/or button input.
*
*/
//!Inclusion of arduino commands/types. 
#include "WProgram.h"

#ifndef uOLED_h
#define uOLED_h

#ifndef DEBUG
#define DEBUG 1               // 0 for no debugging
#define debugPort Serial      // on mega Serial,Serial1,2,3       
#endif

//!Definition of small font. (default 5x7 pixels)
#define SMALL_FONT 	0x00

//!Definition of medium font. (default 8x8 pixels)
#define MEDIUM_FONT	0x01

//!Definition of large font. (default 8x12 pixels) 
#define LARGE_FONT	0x02

//!Filled objects
#define FULL		0x00

//!Wire-frame objects
#define EMPTY		0x01

//!Sound definitions
#define NOTE_B0  1//31
#define NOTE_C1  2//33
#define NOTE_CS1 3//35
#define NOTE_D1  4//37
#define NOTE_DS1 5//39
#define NOTE_E1  6//41
#define NOTE_F1  7//44
#define NOTE_FS1 8//46
#define NOTE_G1  9//49
#define NOTE_GS1 10//52
#define NOTE_A1  11//55
#define NOTE_AS1 12//58
#define NOTE_B1  13//62
#define NOTE_C2  14//65
#define NOTE_CS2 15//69
#define NOTE_D2  16//73
#define NOTE_DS2 17//78
#define NOTE_E2  18//82
#define NOTE_F2  19//87
#define NOTE_FS2 20//93
#define NOTE_G2  21//98
#define NOTE_GS2 104
#define NOTE_A2  110
#define NOTE_AS2 117
#define NOTE_B2  123
#define NOTE_C3  131
#define NOTE_CS3 139
#define NOTE_D3  147
#define NOTE_DS3 156
#define NOTE_E3  165
#define NOTE_F3  175
#define NOTE_FS3 185
#define NOTE_G3  196
#define NOTE_GS3 208
#define NOTE_A3  220
#define NOTE_AS3 233
#define NOTE_B3  247
#define NOTE_C4  262
#define NOTE_CS4 277
#define NOTE_D4  294
#define NOTE_DS4 311
#define NOTE_E4  330
#define NOTE_F4  349
#define NOTE_FS4 370
#define NOTE_G4  392
#define NOTE_GS4 415
#define NOTE_A4  440
#define NOTE_AS4 466
#define NOTE_B4  494
#define NOTE_C5  523
#define NOTE_CS5 554
#define NOTE_D5  587
#define NOTE_DS5 622
#define NOTE_E5  659
#define NOTE_F5  698
#define NOTE_FS5 740
#define NOTE_G5  784
#define NOTE_GS5 831
#define NOTE_A5  880
#define NOTE_AS5 932
#define NOTE_B5  988
#define NOTE_C6  1047
#define NOTE_CS6 1109
#define NOTE_D6  1175
#define NOTE_DS6 1245
#define NOTE_E6  1319
#define NOTE_F6  1397
#define NOTE_FS6 1480
#define NOTE_G6  1568
#define NOTE_GS6 1661
#define NOTE_A6  1760
#define NOTE_AS6 1865
#define NOTE_B6  1976
#define NOTE_C7  2093
#define NOTE_CS7 2217
#define NOTE_D7  2349
#define NOTE_DS7 2489
#define NOTE_E7  2637
#define NOTE_F7  2794
#define NOTE_FS7 2960
#define NOTE_G7  3136
#define NOTE_GS7 3322
#define NOTE_A7  3520
#define NOTE_AS7 3729
#define NOTE_B7  3951
#define NOTE_C8  4186
#define NOTE_CS8 4435
#define NOTE_D8  4699
#define NOTE_DS8 4978
#define OCTAVE_OFFSET 0

//!Base uOLED class.
/*!
   Base uOLED class with most functions from the manual
*/
class uOLED
{
  public:
  
//! Class constructor.
    uOLED(void);

//! Start and init screen
/*!
*   begin takes 3 arguments.
*   - int pin.\n 
*     Pinnumber connected to the reset line of the display 
*   - long baud.\n
*     Baudrate to use for the serial connection to the display 
*   - HardwareSerial *serialToUse.\n
*     Which hardware serial port to use. On a Arduino the options are\n
*     &Serial, &Serial1, &Serial2 and &serial3.\n 
*/
    void begin(int pin, long baud, HardwareSerial *serialToUse);

//! Pin that sends Reset signal 
    int PinReset;

//! Result for every command
/*!
*   Readable as a variable in your sketch
*/    
    char res;
	
//! width of display in pixels
/*!
*   Readable as a variable in your sketch
*/	                      
	int x_res;

//! Height of display in pixels
/*!
*   Readable as a variable in your sketch
*/                       
	int y_res;                       
	
//********************** Basic Graphics********************
//! Draw a pixel.
/*!
*   Place a pixel at x,y coordinates with 16bit color
*/
    void PutPixel (char x, char y, int color);
	
//! Draw a line.
/*!
*   Place a line starting at x1,y1 ending at x2,y2 with 16bit color
*/	
    void Line (char x1, char y1, char x2, char y2, int color);
	
//! Draw a rectangle.
/*!
*   Draw a rectangle with upper-left corner at x1,y1 and the bottom-right corner at x2,y2. Lines will have 16bit color.
*   filled=true -> fill the rectangle with the color given.\n
*   Note that the sides of the rectangle can not be diagonal on the screen.
*/	
    void Rectangle (char x1, char y1, char x2, char y2, int color, char filled);
	
//! Draw a circle.
/*!
*   Draw a circle with the center at x,y with a radius. 16bit color. Filled in that color or not.
*/	
    void Circle (char x, char y, char radius, int color, char filled);
	
//! Set background color
/*!
*   Set the 16bits background color. Using this command will fill the screen with the chosen color. All pixels on screen
*   that have the same color wil 'disappear'.
*/	
    void SetBackColor (int color);
	
//! Draw a triangle.
/*!
*   Draw a triangle with corners x1,y1, x2,y2 and x3,y3 with specified color, filled or not.\n
*   Note from datasheet; The vertices must be specified in an anti-clock wise manner,\n
*   i.e. x2 < x1 : x3 > x2 : y2 > y1 : y3 > y1
*/	
    void Triangle(char x1, char y1, char x2, char y2, char x3, char y3, int color, char filled);
	
//! Copy and paste a rectangle on screen.
/*!
*   Copies a rectangle with top-left corner at xCopy,yCopy with a width and heighth to (top-left corner) xPaste,yPaste.
*/	
    void CopyPaste (char xCopy, char yCopy, char xPaste, char yPaste, char Width, char Height);

//******************** Text functions*******************************************
//! Place a character on the screen.
/*!
*   Places one character on the screen with a fontsize (0x00, 0x01 or 0x02) using column and row as reference.
*   Color or is a 16bit color and argument 'transparant' takes;\n
*   either blocked(opaque)  (0x01) or transparent (0x00).\n
*   From datasheet for 160 by 128 pixels;\n\n
*
*   column : horizontal position of character:\n 
*   range : 0 - 20 for 5x7 font.\n 
*   range : 0 - 15 for 8x8 and 8x12 font.\n\n
*   
*   row : vertical position of character:\n 
*   range : 0 - 15 for 5x7 and 8x8 font.\n 
*   range : 0 - 9 for 8x12 font.\n
*/
    void Character (char Character, char font, char col, char row, int color, char transparent);
	
//! Place a scaled character on the screen.
/*!
*   Place one scaled character on the screen with a chosen font (0x00, 0x01 or 0x02) referenced by column/row.\n
*   From datasheet for 160 by 128 pixels;\n\n
*
*   column : horizontal position of character:\n 
*   range : 0 - 20 for 5x7 font.\n 
*   range : 0 - 15 for 8x8 and 8x12 font.\n\n
*   
*   row : vertical position of character:\n 
*   range : 0 - 15 for 5x7 and 8x8 font.\n 
*   range : 0 - 9 for 8x12 font.\n\n
*   
*   With a 16bit color.\n
*   Width and Height are multipliers.\n
*   Example;
*   - font used is 0x00 (5 by 7 pixels)\n
*   - width and height are 2\n
*   The result will be a character 10 by 14 pixels. Note that the column/row reference is still using 5 by 7 pixels.
*/	
    void CharacterGraphic(char Character, char x, char y, int color, char Width, char Height, char transparent);
	
//! Place a string of characters on the screen.
/*!
*   Places a string of characters on the screen with a fontsize (0x00, 0x01 or 0x02) using column and row as reference for 
*   the first character. Maximum string size is 256 characters and the display will wrap around to the next row. 
*   Color or is a 16bit color and argument 'transparant' takes;\n
*   either blocked (0x01) or transparent (0x00).\n
*   From datasheet for 160 by 128 pixels;\n\n
*
*   column : horizontal position of first character:\n 
*   range : 0 - 20 for 5x7 font.\n 
*   range : 0 - 15 for 8x8 and 8x12 font.\n\n
*   
*   row : vertical position of first character:\n 
*   range : 0 - 15 for 5x7 and 8x8 font.\n 
*   range : 0 - 9 for 8x12 font.\n
*/	
    void Text(char col, char row, char font, int color, char *Text, char transparent);
	
//! Place a scaled string of characters on the screen.
/*!
*   Place a scaled string of characters on the screen with a chosen font (0x00, 0x01 or 0x02).  
*   First character referenced by column/row.\n
*   From datasheet for 160 by 128 pixels;\n\n
*
*   column : horizontal position of character:\n 
*   range : 0 - 20 for 5x7 font.\n 
*   range : 0 - 15 for 8x8 and 8x12 font.\n\n
*   
*   row : vertical position of character:\n 
*   range : 0 - 15 for 5x7 and 8x8 font.\n 
*   range : 0 - 9 for 8x12 font.\n\n
*   
*   With a 16bit color.\n
*   Width and Height are multipliers.\n
*   Example;
*   - font used is 0x00 (5 by 7 pixels)\n
*   - width and height are 2\n
*   The result will be a character 10 by 14 pixels. Note that the column/row reference is still using 5 by 7 pixels.
*/
    void TextGraphic(char col, char row, char font, int color, char Width, char Height, char *text, char transparent);
	
//! To do.
/*!
*   Longer text to do.
*/	
    void TextButton (char state, char x, char y, int ButtonColor, char font, int TextColor, char TextWidth, char TextHeight, char *Text);
	
//! To do.
/*!
*   Longer text to do.
*/
    void AddBMPChar(char reference, char data1, char data2, char data3, char data4, char data5, char data6, char data7, char data8);
	
//! To do.
/*!
*   Longer text to do.
*/
    void PutBMPChar(char reference, char x, char y, int color);
	
//! To do.
/*!
*   Longer text to do.
*/
    unsigned int ReadPixel(char x, char y);
	
//**********************************Display Control******************************************
	
//! To do.
/*!
*   Longer text to do.
*/
    void Cls(void);
	
//! To do.
/*!
*   Longer text to do.
*/
	void DisplayControl(char Mode, char Value);
	
//! To do.
/*!
*   Longer text to do.
*/		
	void SetContrast(char contrastValue);
	
//! To do.
/*!
*   Longer text to do.
*/
	void SetPowerState(char state);
	
//! To do.
/*!
*   Longer text to do.
*/
	void SetDisplayState(char state);
	
//! To do.
/*!
*   Longer text to do.
*/
	void DeviceInfo();                           // Display device info on screen
	
//! To do.
/*!
*   Longer text to do.
*/ 
    void scrollEnable(boolean enable);
	
//! To do.
/*!
*   Longer text to do.
*/
    void scrollToRight(boolean toRight);
	
//! To do.
/*!
*   Longer text to do.
*/
	void scrollSpeed(byte scrollSpeed);
	
//********************************** sd card functions**********************************
//! To do.
/*!
*   Longer text to do.
*/
	void sdInit();
	
//! To do.
/*!
*   Longer text to do.
*/
	void sdSetMemAdrr(char Umsb, char Ulsb, char Lmsb, char Llsb);
	
//! To do.
/*!
*   Longer text to do.
*/
    void sdWriteByte(byte data);
	
//! To do.
/*!
*   Longer text to do.
*/
	byte sdReadByte(void);
	
//! To do.
/*!
*   Longer text to do.
*/
	void sdWriteBlock(long sector, byte data[]);
	
//! To do.
/*!
*   Longer text to do.
*/
    void sdReadBlock(long sector, byte data[]);
	
//! To do.
/*!
*   Longer text to do.
*/
	void sdScreenCopy(byte x, byte y, byte width, byte height, long sector);
	
//! To do.
/*!
*   Longer text to do.
*/
    void sdDisplayImage(byte x, byte y, byte width, byte height, boolean twoBytesPP, long sector);
	
//! To do.
/*!
*   Longer text to do.
*/
    void sdDisplayVideo(byte x, byte y, byte width, byte height, boolean twoBytesPP, byte fdelay, int frames, long sector);
	
//! Generate a note or frequency for a certain duration.
/*!  
*    0 : No sound, silence.\n
*    1-84 : 5 octaves piano range + 2 more.\n
*    100-20000 : Frequency in Hz.\n
*/
    void playTone(int tone, int duration);
	
//! To do.
/*!
*   Longer text to do.
*/
    void playRtttl(char *song);	
	
	private:
//! Hardware serial port to use
/*! On a Arduino Mega the options are\n
*   &Serial, &Serial1, &Serial2 and &serial3.
*/  
    HardwareSerial *pSerial;

//! Wrapper for writing to serial port
    void write(byte pData);
	
//! Wrapper for Serial.begin(baudrate)	
    void begin(long BaudRate);
	
//! Wrapper for checking if there is something to read from the serial buffer.
    boolean available(void);
	
//! Wrapper for reading from the serial port buffer	
    int read(void);
	
//! Setting the fontsize to use.
/*! By default (no custom fonts created with font-tool) the options are;\n
*   SMALL_FONT 0x00, MEDIUM_FONT 0x01 and LARGE_FONT 0x02
*/
    void SetFontSize(char font);

//! Setting opaque or transparent for text.
/*! An object behind the text can either be blocked (0x01) or transparent (0x00).
*/
    void TextMode (char mode);
	
//! Draw an object solid or wire-frame
/*! 00hex : All graphics objects are drawn solid. 01hex : All graphics objects are drawn wire-frame
*/	
    void PenSize(char size);
	
//! Read back a character from the display
/*! Puts the readed character from the display in char res;
*/
    char RBack(void);
	
//! Asks the display for it's geometry.
/*! Puts the horizontal/vertical sizes of the display used in variables int x_res and int y_res.
*/
	void DevInfoInVar(void);
};

#endif

