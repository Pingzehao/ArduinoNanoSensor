// μLCD-32PT(SGC) 3.2” Serial LCD Display Module
// Arduino Library
//
// 2011-05-10 release 1
//   initial release
// 2011-06-15 release 2
//	 features added and bugs fixed
// 2011-06-29 release 3
//	 setBackGroundColour added
//       SD added
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

#include "WProgram.h"
#include "Serial_LCD.h"
#include "NewSoftSerial.h"


// Constructor
Serial_LCD::Serial_LCD(uint8_t receivePin, uint8_t transmitPin) : 
_lcd_nss(receivePin, transmitPin) {
}


// Interface
// 2.1 General Commands
// AutoBaud – 55hex 
void Serial_LCD::begin() {
  // LCD 500 ms power-up
  // SD card 3000 ms power-up
  delay(500);

  _lcd_nss.begin(9600);

  _lcd_nss.print('U', BYTE);    // connect

  while (_lcd_nss.read()!=6)  {     
    delay(100);  
  }


  _lcd_nss.print('Q', BYTE);
  _lcd_nss.print(0x08, BYTE);
  _lcd_nss.begin(19200);

	  while (_lcd_nss.read()!=6)  {     
    delay(100);  
  }

  _lcd_nss.print('o', BYTE);    // clear touch 
  _lcd_nss.print(0x04, BYTE);   // touch state
  _lcd_nss.flush();
  while(_lcd_nss.available()) _lcd_nss.read();

  setBacklight(true);  // backlight on
  setDisplay(true);  // display on
  setOrientation(3);
  clear();
  setFont(1);
}


String Serial_LCD::WhoAmI() {  
  String s="Serial uLCD-32PT ";
  _lcd_nss.print('V', BYTE);
  _lcd_nss.print(0x00, BYTE);
  void nacAck();

  while(_lcd_nss.available()!=0) {
    s += String(_lcd_nss.read(), HEX);
    s += " ";
  }
  return s;
}


byte Serial_LCD::clear() {
  _lcd_nss.print('E', BYTE);
  return nacAck();
}

void Serial_LCD::off() {
  clear();

  _lcd_nss.print('o', BYTE);
  _lcd_nss.print(0x04, BYTE);   // state
  _lcd_nss.flush();

  setBacklight(false);  // backlight off
  clear();
  setDisplay(false);  // display off
}


byte Serial_LCD::setBacklight(boolean b) {
  _lcd_nss.print('Y', BYTE);
  _lcd_nss.print(0x00, BYTE);
  _lcd_nss.print(b ? 0x01 : 0x00, BYTE);
  return nacAck();
}   

byte Serial_LCD::setDisplay(boolean b) {
  _lcd_nss.print('Y', BYTE);
  _lcd_nss.print(0x01, BYTE);
  _lcd_nss.print(b ? 0x01 : 0x00, BYTE);
  return nacAck();
}

byte Serial_LCD::setContrast(byte b) {
  if (b<=0x0f) {
    _lcd_nss.print('Y', BYTE);
    _lcd_nss.print(0x02, BYTE);
    _lcd_nss.print(b, BYTE);

    return nacAck();
  } 
  else {
    return 0x15; 
  }
}


byte Serial_LCD::setOrientation(byte b) {   // Display Control Functions – 59hex
  _orientation=b;
  _lcd_nss.print('Y', BYTE);
  _lcd_nss.print(0x04, BYTE);
  _lcd_nss.print(b, BYTE);  // 
  return nacAck();
}


byte Serial_LCD::setTouch(boolean b) {
  if (b) {
    _lcd_nss.print('Y', BYTE);
    _lcd_nss.print(0x05, BYTE);
    _lcd_nss.print(0x00, BYTE);  // enable touch
    char c=nacAck();
	delay(25);

    if (c=0x06) {
      _lcd_nss.print('Y', BYTE);
      _lcd_nss.print(0x05, BYTE);  // full screen active
      _lcd_nss.print(0x02, BYTE);
    }
  } 
  else {
    _lcd_nss.print('Y', BYTE);
    _lcd_nss.print(0x05, BYTE);  // disable touch
    _lcd_nss.print(0x01, BYTE);
  }
  return nacAck();
}

byte Serial_LCD::setVolume(byte percent) { // Set Volume - 76hex 
  byte b=0x08 + percent;
  if (b>0x7f) b=0x7f;
  _lcd_nss.print('v', BYTE);
  _lcd_nss.print(b, BYTE);

  return nacAck();
}

// Graphics
byte Serial_LCD::circle(int x1, int y1, int radius, unsigned int colour) {
  _lcd_nss.print('C', BYTE);

  _uIntPrint(x1);
  _uIntPrint(y1);
  _uIntPrint(radius);
  _uIntPrint(colour);

  return nacAck();
}


byte Serial_LCD::rectangle(int x1, int y1, int x2, int y2, unsigned int colour) {
  _lcd_nss.print('r', BYTE);

  _uIntPrint(x1);
  _uIntPrint(y1);
  _uIntPrint(x2);
  _uIntPrint(y2);
  _uIntPrint(colour);

  return nacAck();
}  


byte Serial_LCD::line(int x1, int y1, int x2, int y2, unsigned int colour) {
  _lcd_nss.print('L', BYTE);

  _uIntPrint(x1);
  _uIntPrint(y1);
  _uIntPrint(x2);
  _uIntPrint(y2);
  _uIntPrint(colour);

  return nacAck();
}  

// 2011-06-24 release 3
//	 setBackGroundColour added
byte Serial_LCD::setBackGroundColour(unsigned int colour) { 
  _lcd_nss.print('K', BYTE);

  _uIntPrint(colour);

  return nacAck();
}

byte Serial_LCD::point(int x1, int y1, unsigned int colour) {
  _lcd_nss.print('L', BYTE);

  _uIntPrint(x1);
  _uIntPrint(y1);
  _uIntPrint(colour);

  return nacAck();
}  


byte Serial_LCD::triangle(int x1, int y1, int x2, int y2, int x3, int y3, unsigned int colour) {
  boolean b=true;

  // Graham Scan + Andrew's Monotone Chain Algorithm
  // 1. Sort by ascending x

  while (b) {  // required x2 < x1 : x3 > x2 : y2 > y1 : y3 > y1
    b=false;
    if (!b && (x1>x2)) { 
      _swap(x1, x2);
      _swap(y1, y2);
      b=true; 
    }
    if (!b && (x2>x3)) { 
      _swap(x3, x2);
      _swap(y3, y2);
      b=true; 
    }
  }

  // Graham Scan + Andrew's Monotone Chain Algorithm
  // 2. Sort by ascending y
  while (b) {  // required x2 < x1 : x3 > x2 : y2 > y1 : y3 > y1
    if (!b && (y1>y2)) { 
      _swap(x1, x2);
      _swap(y1, y2);
      b=true; 
    }
    if (!b && (y3>y2)) { 
      _swap(x3, x2);
      _swap(y3, y2);
      b=true; 
    }
  }

  // Graham Scan + Andrew's Monotone Chain Algorithm
  // 3. check counter-clockwise, clockwise, collinear
  long l= (x2 - x1)*(y3 - y1) - (y2 - y1)*(x3 - x1);

  if (l==0)   return line(x1, y1, x3, y3, colour);

  if (l>0) {
    _swap(x1, x2);
    _swap(y1, y2);
  }
  l= (x2 - x1)*(y3 - y1) - (y2 - y1)*(x3 - x1);

  _lcd_nss.print('G', BYTE);

  _uIntPrint(x1);
  _uIntPrint(y1);
  _uIntPrint(x2);
  _uIntPrint(y2);
  _uIntPrint(x3);
  _uIntPrint(y3);
  _uIntPrint(colour);

  return nacAck();
}  



byte Serial_LCD::setPenSolid(boolean b) {
  // 00hex : All graphics objects are drawn solid 
  // 01hex : All graphics objects are drawn wire-frame
  _lcd_nss.print('p', BYTE);
  _lcd_nss.print(b ? 0x00 : 0x01, BYTE);
  return nacAck();
}  


// Text
// 2011-06-15 release 2
//   bug fixed, break added!
byte Serial_LCD::setFont(byte b) {
  // 00hex : 6x8 (5x7 false) small size font set 
  // 01hex : 8x8 medium size font set 
  // 02hex : 8x12 large size font set
  // 03hex : 12x16 largest size font set
  _lcd_nss.print('F', BYTE);
  _lcd_nss.print(b, BYTE);
  _font=b;
  switch (b) {
  case 0:
    _fontX=6; 
    _fontY=8;
    break;
  case 1:
    _fontX=8; 
    _fontY=8;
    break;
  case 2:
    _fontX=8; 
    _fontY=12;
    break;
  case 3:
    _fontX=12; 
    _fontY=16;
  } 
  return nacAck();
}  

byte Serial_LCD::setFontSolid(byte b) {
  // 00hex : Transparent, objects behind text are visible. 
  // 01hex : Opaque, objects behind text blocked by background.
  _lcd_nss.print('O', BYTE);
  _lcd_nss.print(b, BYTE);
  return nacAck();
}  


byte Serial_LCD::tText(byte x, byte y, unsigned int colour, String s) {
  _lcd_nss.print('s', BYTE);
  _lcd_nss.print(x, BYTE);     // in character units
  _lcd_nss.print(y, BYTE);
  _lcd_nss.print(_font, BYTE);
  _uIntPrint(colour);
  _lcd_nss.print(s);
  _lcd_nss.print(0x00, BYTE);
  return nacAck();
}

byte Serial_LCD::tText(byte x, byte y, unsigned int colour, double d) {
  _lcd_nss.print('s', BYTE);
  _lcd_nss.print(x, BYTE);     // in character units
  _lcd_nss.print(y, BYTE);
  _lcd_nss.print(_font, BYTE);
  _uIntPrint(colour);
  _lcd_nss.print(d);
  _lcd_nss.print(0x00, BYTE);
  return nacAck();
}

byte Serial_LCD::tText(byte x, byte y, unsigned int colour, int i) {
  _lcd_nss.print('s', BYTE);
  _lcd_nss.print(x, BYTE);     // in character units
  _lcd_nss.print(y, BYTE);
  _lcd_nss.print(_font, BYTE);
  _uIntPrint(colour);
  _lcd_nss.print(i);
  _lcd_nss.print(0x00, BYTE);
  return nacAck();
}

byte Serial_LCD::tText(byte x, byte y, unsigned int colour, char c) {
  _lcd_nss.print('s', BYTE);
  _lcd_nss.print(x, BYTE);     // in character units
  _lcd_nss.print(y, BYTE);
  _lcd_nss.print(_font, BYTE);
  _uIntPrint(colour);
  _lcd_nss.print(c);
  _lcd_nss.print(0x00, BYTE);
  return nacAck();
}


byte Serial_LCD::gText(int x, int y, unsigned int colour, String s) {
  _lcd_nss.print('S', BYTE);
  _uIntPrint(x);    // in graphic units
  _uIntPrint(y);
  _lcd_nss.print(_font, BYTE);
  _uIntPrint(colour);
  _lcd_nss.print(0x01, BYTE);   // multiplier
  _lcd_nss.print(0x01, BYTE);
  _lcd_nss.print(s);
  _lcd_nss.print(0x00, BYTE);
  return nacAck();
}


// Touch
// 2011-06-15 release 2
//   +2 features
//   case 1: touch down added
//   return value 
//     0 : No Touch Activity 
//     1 : Touch Press 
//     2 : Touch Release 
//     3 : Touch Moving
byte Serial_LCD::getTouchActivity() {
  _lcd_nss.print('o', BYTE);
  _lcd_nss.print(0x04, BYTE);   // state

  int i=0; 
  while (_lcd_nss.available() && (i<4)) {
    _touch_buffer[i]=_lcd_nss.read();
    i++;
  }

  switch (_touch_buffer[1]) {
  case 1:
  case 2:
  case 3:
    return _touch_buffer[1];
    //		  return true;
  default:
    return false;
  }
}

byte Serial_LCD::getTouchXY(int &x, int &y) {
  _lcd_nss.print('o', BYTE);
  _lcd_nss.print(0x05, BYTE);   // coordinates

  int i=0; 
  while (_lcd_nss.available() && (i<4)) {
    _touch_buffer[i]=_lcd_nss.read();
    i++;
  }

  if (_touch_buffer[0]!=0x15) {
    x = (_touch_buffer[0] << 8) | _touch_buffer[1];
    y = (_touch_buffer[2] << 8) | _touch_buffer[3];
    return 0x06;
  } 
  else {
    return 0x15;
  }
}

// 2011-06-29 release 3
// 2.6 SD Memory Card Commands (FAT16-Level/DOS)
// Initialise Memory Card - @69hex 
byte Serial_LCD::initSD() {
  // SD card 3000 ms power-up
  delay(3000);
  _lcd_nss.print('@', BYTE);
  _lcd_nss.print('i', BYTE);   

  // answer = 210 ms
  return nacAck();
}


// Write File to Card (FAT) - @74hex 
// default option = 0
byte Serial_LCD::writeString2File(String filename, String text, byte option) { 
  String s;
  byte a;
  byte j;

  j=text.length() >>4;

  // 16-byte blocks
  if (j>0) {
    _lcd_nss.print('@', BYTE);
    _lcd_nss.print('t', BYTE);
    _lcd_nss.print(0x10 + option, BYTE); // hand-shaking
    _lcd_nss.print(filename);
    _lcd_nss.print(0x00, BYTE);
    _uIntPrint(0);
    _uIntPrint((unsigned long)(j <<4));
    a=nacAck();

    for (int i=0; i<j; i++) {
      s=text.substring(i <<4, (i+1) <<4);
      _lcd_nss.print(s);
      a=nacAck();
    }
  }

  // remaining bytes
  j=text.length() % 0x10;

  if   (j > 0) {
    _lcd_nss.print('@', BYTE);
    _lcd_nss.print('t', BYTE);
    _lcd_nss.print(0x00 + option, BYTE);   // no hand-shaking
    _lcd_nss.print(filename);
    _lcd_nss.print(0x00, BYTE);
    _uIntPrint(0);
    _uIntPrint((unsigned long)(j));
    a=nacAck();

    s=text.substring(text.length()-j, text.length());
    _lcd_nss.print(s);
    a=nacAck();
  }

  return a;
}

byte Serial_LCD::appendString2File(String filename, String text) { 
  return writeString2File(filename, text, 0x80);  // append option
}

// Erase file from Card (FAT) - @65hex 
byte Serial_LCD::eraseFile(String filename) {  
  _lcd_nss.print('@', BYTE);
  _lcd_nss.print('e', BYTE);
  _lcd_nss.print(filename);
  _lcd_nss.print(0x00, BYTE);

  return nacAck();
}

// List Directory from Card (FAT) - @64hex
byte Serial_LCD::findFile(String filename) {  
  _lcd_nss.print('@', BYTE);
  _lcd_nss.print('d', BYTE);
  _lcd_nss.print(filename);
  _lcd_nss.print(0x00, BYTE);

  String s="";  
  char c=0;

  do 
    if (_lcd_nss.available()) {
    c=_lcd_nss.read();  
    s=s+String(c);
  }
  while ((c != 0x06) && (c != 0x15) && (c != 0x0a));
  _lcd_nss.flush();

  if ((c==0x15) || (c==0x06)) return 0x15;
  if (s.length()==0) return 0x15;
  if (filename.equalsIgnoreCase(s.substring(0, (s.indexOf(c))))) return 0x06 ;
  return 0x15;
}


// Screen Copy-Save to Card (FAT) - @63hex 
byte Serial_LCD::saveScreenSD(String filename) {    
  _lcd_nss.print('@', BYTE);
  _lcd_nss.print('c', BYTE);
  _uIntPrint(0);
  _uIntPrint(0);
  _uIntPrint(319);
  _uIntPrint(239);
  _lcd_nss.print(filename);
  _lcd_nss.print(0x00, BYTE);

  return nacAck();
}

// Display Image-Icon from Card (FAT) - @6Dhex 
byte Serial_LCD::readScreenSD(String filename) {   
  _lcd_nss.print('@', BYTE);
  _lcd_nss.print('m', BYTE);
  _lcd_nss.print(filename);
  _lcd_nss.print(0x00, BYTE);
  _uIntPrint(0);
  _uIntPrint(0);
  _uIntPrint(0);
  _uIntPrint(0);

  return nacAck();
}


// Play Audio WAV file from Card (FAT) - @6Chex 
// Run Script (4DSL) Program from Card (FAT) - @70hex



// Utilities


unsigned int Serial_LCD::rgb16(byte red8, byte green8, byte blue8) {
  // rgb16 = red5 green6 blue5
  return (red8 >> 3) << 11 | (green8 >> 2) << 5 | (blue8 >> 3);
}


byte Serial_LCD::nacAck() {
  byte b=0x06;
  while (!_lcd_nss.available()) {     
    delay(1);   
  }
  b = _lcd_nss.read();
  return b;
}


void Serial_LCD::_uIntPrint(unsigned int ui) {
  _lcd_nss.print(ui >> 8, BYTE);
  _lcd_nss.print(ui & 0xff, BYTE);
}


void Serial_LCD::_swap(int &a, int &b) {
  int w=a;
  a=b;
  b=w;
}





















