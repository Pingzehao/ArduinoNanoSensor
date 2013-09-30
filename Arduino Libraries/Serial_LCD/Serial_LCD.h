//
// μLCD-32PT(SGC) 3.2” Serial LCD Display Module
// Arduino Library
//
// 2011-05-10 release 1
//   initial release
// 2011-06-15 release 2
//	 features added and bugs fixed
// 2011-06-29 release 3
//	 setBackGroundColour added
//       SD card
//
// CC = BY NC SA
// http://sites.google.com/site/vilorei/
//
// Based on
// 4D LABS PICASO-SGC Command Set
// Software Interface Specification
// Document Date: 1st March 2011 
// Document Revision: 6.0
// http://www.4d-Labs.com
//
//

#ifndef Serial_LCD_h
#define Serial_LCD_h

#include "WProgram.h"
#include <inttypes.h>
#include <NewSoftSerial.h>

// Units

class Serial_LCD
{
public:
  Serial_LCD(uint8_t receivePin, uint8_t transmitPin); // constructor

  // 2.1 General Commands
  void begin(); // AutoBaud – 55hex 
  // Set new Baud-Rate - 51hex 
  String WhoAmI(); // Version-Device Info Request – 56hex 
  // Replace Background Colour – 42hex 
  byte clear(); // Clear Screen – 45hex
  byte setBacklight(boolean b);   // Display Control Functions – 59hex 
  byte setDisplay(boolean b);   // Display Control Functions – 59hex
  byte setContrast(byte b);   // Display Control Functions – 59hex
  byte setOrientation(byte b);   // Display Control Functions – 59hex
  byte setTouch(boolean b);   // Display Control Functions – 59hex
  byte setVolume(byte percent); // Set Volume - 76hex 
  // Sleep – 5Ahex 
  // Read GPIO Pin - 61hex
  // Write GPIO Pin - 69hex 
  // Read GPIO Bus - 79hex 
  // Write GPIO Bus – 57hex  
  void off(); // Highly recommended at the end

  // 2.2 Graphics Commands
  // Add User Bitmap Character – 41hex 
  // Draw User Bitmap Character – 44hex 
  byte circle(int x1, int y1, int radius, unsigned int colour);  // Draw Circle – 43hex 
  byte triangle(int x1, int y1, int x2, int y2, int x3, int y3, unsigned int colour);  // Draw Triangle – 47hex 
  // Draw Image-Icon – 49hex 
  byte setBackGroundColour(unsigned int colour);   // Set Background colour – 4Bhex 
  byte line(int x1, int y1, int x2, int y2, unsigned int colour);  // Draw Line – 4Chex 
  // Draw Polygon – 67hex 
  byte rectangle(int x1, int y1, int x2, int y2, unsigned int colour);  // Draw Rectangle – 72hex 
  // Draw Ellipse – 65hex 
  byte point(int x1, int y1, unsigned int colour);   // Draw Pixel – 50hex 
  // Read Pixel – 52hex 
  // Screen Copy-Paste – 63hex 
  // Replace colour – 6Bhex 
  byte setPenSolid(boolean b);    // Set Pen Size 1=solid; 0=wire frame – 70hex

  // 2.3 Text Commands
  byte setFont(byte b);  // Set Font – 46hex 
  byte setFontSolid(byte b);  // Set 0=Transparent-1=Opaque Text – 4Fhex 
  // Draw ASCII Character (text format) – 54hex 
  // Draw ASCII Character (graphics format) – 74hex 
  byte tText(byte x, byte y, unsigned int colour, String s);  // Draw “String” of ASCII Text (text format) – 73hex 
  byte tText(byte x, byte y, unsigned int colour, int i);  // Draw “String” of ASCII Text (text format) – 73hex 
  byte tText(byte x, byte y, unsigned int colour, double d);  // Draw “String” of ASCII Text (text format) – 73hex 
  byte tText(byte x, byte y, unsigned int colour, char c);
  byte gText(int x, int y, unsigned int colour, String s);    // Draw “String” of ASCII Text (graphics format) – 53hex 
  // Draw Text Button – 62hex

  // 2.4 Touch Screen Commands
  // Touch screen must be enabled to be able to use the touch commands. 
  byte getTouchActivity();   // Get Touch Coordinates - 6Fhex - 0 : No Touch Activity 1 : Touch Press 2 : Touch Release 3 : Touch Moving
  byte getTouchXY(int &x, int &y);   // Get Touch Coordinates - 6Fhex 
  // Wait Until Touch - 77hex 
  // Detect Touch Region - 75hex

  // 2.5 SD Memory Card Commands (Low-Level/RAW)
  // Initialise Memory Card - @69hex 
  // Set Address Pointer of Card (RAW) - @41hex 
  // Read Byte Data from Card (RAW) - @72hex 
  // Write Byte Data to Card (RAW) - @77hex 
  // Read Sector Block Data from Card (RAW) - @52hex 
  // Write Sector Block Data to Card (RAW) - @57hex 
  // Screen Copy-Save to Card (RAW) - @43hex 
  // Display Image-Icon from Card (RAW) - @49hex 
  // Display Object from Card (RAW) - @4Fhex 
  // Display Video-Animation Clip from Card (RAW) - @56hex 
  // Run Script (4DSL) Program from Card (RAW) - @50hex

  // 2011-06-29 release 3
  // 2.6 SD Memory Card Commands (FAT16-Level/DOS)
  byte initSD();   // Initialise Memory Card - @69hex 
  // Read File from Card (FAT) - @61hex 
  // Write File to Card (FAT) - @74hex 
  byte writeString2File(String filename, String text, byte option=0x00);   
  byte appendString2File(String filename, String text);
  byte eraseFile(String filename);   // Erase file from Card (FAT) - @65hex 
  // List Directory from Card (FAT) - @64hex 
  byte findFile(String filename);
  byte saveScreenSD(String filename);   // Screen Copy-Save to Card (FAT) - @63hex 
  byte readScreenSD(String filename);   // Display Image-Icon from Card (FAT) - @6Dhex 
  // Play Audio WAV file from Card (FAT) - @6Chex 
  // Run Script (4DSL) Program from Card (FAT) - @70hex
  byte setBaud();

  // Utilities
  unsigned int rgb16(byte red, byte green, byte blue);
  byte nacAck(); // 0x06=success
  byte fontX() { 
    return _fontX; 
  };	
  byte fontY() { 
    return _fontY; 
  };	


private:
  NewSoftSerial _lcd_nss;
  byte _font, _fontX, _fontY;
  byte _touch_buffer[4];
  byte _orientation;

  void _uIntPrint(unsigned int ui);
  void _swap(int &a, int &b);

};


#endif
















