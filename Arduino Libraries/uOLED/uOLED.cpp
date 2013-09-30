/*
*  uOLED - Library to communicate and control micro-OLED 4D Displays.
*  Created by Juan Manuel Castro R. (October 2009)
*  Released into the public domain.
*  Extended with some help from Jeroen alias Yot at arduino forum
*  Testing and small updates by Matthew Garten
*/

#include "uOLED.h"
#include "WProgram.h"

  uOLED::uOLED(void) 
  {
  }

void uOLED::begin(int pin, long baud, HardwareSerial *serialToUse)
{
  pSerial = serialToUse;
  PinReset= pin;
  pinMode(PinReset, OUTPUT);
    
  //Reset display
  digitalWrite(PinReset,LOW);
  delay(30);
  digitalWrite(PinReset,HIGH);
  delay(1000);                     // If you want to see the 4d systems startup screen change this to something well above 5000
  begin(baud);
  write(0x55);                     // Send 0x55 to stablish baud rate for serial communication

  res=RBack();
  
  DevInfoInVar();

  Cls();
}

void uOLED::write(byte pData)
{
     pSerial->write(pData);
} 

void uOLED::begin(long BaudRate)
{
     pSerial->begin(BaudRate);
} 

boolean uOLED::available()
{
     return pSerial->available();
} 

int uOLED::read()
{
     return pSerial->read();
} 

//Erase Screen
void uOLED::Cls()
{
  write(0x45);
  res=RBack();  
}

/*!
*   Obtain result for every command that send Arduino to uOLED
*/
char uOLED::RBack()
{ long starttime = millis();
  while (!available()) 
    { 
	  if (millis()-starttime > 4000) {       // timeout of 4 seconds
	  #if DEBUG
      debugPort.println("Serial timeout");
      #endif
	  return 0;                              // 0x15;
	  }
	  
	  // Wait for serial data 
	}
  return read();
}

/*!
*   Place a pixel with color on width x and hight y. 
*/ 
void uOLED::PutPixel(char x, char y, int color)   //Put a color pixel into specific coordinates
{
  write(0x50);
  write(x);
  write(y);
  write(color >> 8);		
  write(color & 0xFF);
  res=RBack();
}

//Draw a line
void uOLED::Line (char x1, char y1, char x2, char y2, int color)
{
  write(0x4C);
  write(x1);
  write(y1);
  write(x2);
  write(y2);
  write(color >> 8);		
  write(color & 0xFF);
  res=RBack();
}

//define how is going to be filled figures of Circle, Rectangle, Triangle, Polygon
void uOLED::PenSize(char size)
{
  write(0x70);
  write(byte(!size));
  res=RBack();
}

void uOLED::Rectangle (char x1, char y1, char x2, char y2, int color, char filled)
{
  PenSize(filled);

  write(0x72);
  write(x1);
  write(y1);
  write(x2);
  write(y2);
  write(color >> 8);		
  write(color & 0xFF);
  res=RBack();
}

void uOLED::Circle (char x, char y, char radius, int color, char filled)
{
  PenSize(filled);

  write(0x43);
  write(x);
  write(y);
  write(radius);
  write(color >> 8);		
  write(color & 0xFF);
  res=RBack();
}

//Define BackGround color 
void uOLED::SetBackColor (int color)
{
  write(0x42);
  write(color >> 8);		
  write(color & 0xFF);
  res=RBack();
}

void uOLED::Triangle (char x1, char y1, char x2, char y2, char x3, char y3, int color, char filled)
{
  PenSize(filled);

  write(0x47);
  write(x1);
  write(y1);
  write(x2);
  write(y2);
  write(x3);
  write(y3);
  write(color >> 8);		
  write(color & 0xFF);
  res=RBack();
}

void uOLED::CopyPaste (char xCopy,char yCopy,char xPaste, char yPaste, char Width, char Height)
{
  write(0x63);
  write(xCopy);
  write(yCopy);
  write(xPaste);
  write(yPaste);
  write(Width);
  write(Height);
  res=RBack();
}

void uOLED::SetFontSize(char font)
{
  write(0x46);
  write(font);
  res=RBack();
}

void uOLED::TextMode (char mode)
{
  write(0x4F);
  write(mode);
  res=RBack();
}

void uOLED::Character (char Character, char font, char col, char row, int color, char transparent)
{
  SetFontSize(font);
  
  TextMode(transparent);

  write(0x54);
  write(Character);
  write(col);
  write(row);
  write(color >> 8);		
  write(color & 0xFF);
  res=RBack();
}    

void uOLED::CharacterGraphic(char Character, char x, char y, int color, char Width, char Height, char transparent)
{
  TextMode(transparent);

  write(0x74);
  write(Character);
  write(x);
  write(y);
  write(color >> 8);		
  write(color & 0xFF);
  write(Width);
  write(Height);
  res=RBack();
}

void uOLED::Text( char col, char row, char font, int color, char *Text, char transparent)
{
  TextMode(transparent);

  write(0x73);
  write(col); 
  write(row); 
  write(font);
  write(color >> 8);
  write(color & 0xFF);
  for (int i=0 ; i<strlen(Text) ; i++)
  {
    write(Text[i]);
  }
  write(byte(0x00));       // to 'fool' the compiler. (without typecasting the compiler complains about the 0 (zero) as being ambiguous)
  res=RBack();
}

void uOLED::TextGraphic(char col, char row, char font, int color, char Width, char Height, char *text, char transparent)
{
  TextMode(transparent);

  write(0x53);
  write(col); 
  write(row); 
  write(font);
  write(color >> 8);
  write(color & 0xFF);
  write(Width);
  write(Height);
  for (int i=0 ; i<strlen(text) ; i++)
  {
    write(text[i]);
  }
  write(byte(0x00));        // to 'fool' the compiler. (without typecasting the compiler complains about the 0 (zero) as being ambiguous)
  res=RBack();
}

void uOLED::TextButton (char state, char x, char y, int ButtonColor, char font, int TextColor, char TextWidth, char TextHeight, char *Text)
{
  TextMode(byte(0x00));

  write(0x62);
  write(state); 
  write(x); 
  write(y); 
  write(ButtonColor >> 8);
  write(ButtonColor & 0xFF);
  write(font);
  write(TextColor >> 8);
  write(TextColor & 0xFF);
  write(TextWidth);
  write(TextHeight);
  for (int i=0 ; i<strlen(Text) ; i++)
  {
    write(Text[i]);
  }
  write(byte(0x00));
  res=RBack();
}

void uOLED::AddBMPChar(char reference, char data1,char data2,char data3,char data4,char data5,char data6,char data7,char data8)
{
  write(0x41);
  write(reference); 
  write(data1); 
  write(data2); 
  write(data3); 
  write(data4); 
  write(data5); 
  write(data6); 
  write(data7); 
  write(data8); 
  res=RBack();
}

void uOLED::PutBMPChar(char reference, char x, char y, int color)
{
  write(0x44);
  write(reference);
  write(x); 
  write(y); 
  write(color >> 8);
  write(color & 0xFF);
  res=RBack();
}

void uOLED::DisplayControl(char Mode, char Value)
{
  write(0x59);
  write(Mode);
  write(Value); 
  res=RBack();
}

// Return the color of a pixel at x, y
unsigned int uOLED::ReadPixel(char x, char y)
{
  byte highByte;
  byte lowByte;
  unsigned int data;
  write(0x52);            // Send the readpixel command
  write(x);               // Send the x coordinate
  write(y);               // Send the y coordinate
  highByte = RBack();                     // Read the MSB of the color
  lowByte = RBack();                      // Read the LSB of the color
  data = word(highByte, lowByte);         // Combine the two in an int
  return data;                            // Return the color 
}

// Set the contrast. 0dec to 15dec : Contrast range (default = 8dec)
void uOLED::SetContrast(char contrastValue) 
{ 
  write(0x59);
  write(0x02);
  write(contrastValue);
  res=RBack();
}

// Set the powerstate; 00hex = Power-Down / 01hex = Power-Up  (After powerup my display does not set contrast to default of 8dec, have to do that manualy)
void uOLED::SetPowerState (char state)
{
  write(0x59);
  write(0x03);
  write(state); 
  res=RBack();
}

// Set the displaystate; 00hex = off / 01hex = on 
void uOLED::SetDisplayState (char state)
{
  write(0x59);
  write(0x01);
  write(state);
  res=RBack();
}


void uOLED::DeviceInfo ()
{
  write(0x56);       // send DeviceInfoRequest command
  write(0x01);       // 0 = info to serial port only. 1 = info to serial port and screen.
  for ( byte i=0; i<5; i++)
	{
	RBack();                         // dump 5 times the serial input from screen
	}
}

void uOLED::DevInfoInVar()
{
  write(0x56);       // send DeviceInfoRequest command
  write(byte(0x00));       // 0 = info to serial port only. 1 = info to serial port and screen.
  for ( byte i=0; i<3; i++)
	{
	RBack();                         // dump 3 times the serial input from screen
	}
  switch (RBack())                   // fill x_res variable
  {
    case 0x22:
      x_res=219;
      break;
    case 0x28:
      x_res=127;
      break;
    case 0x32:
      x_res=319;
      break;
    case 0x60:
      x_res=159;
      break;
    case 0x64:
      x_res=63;
      break;
    case 0x76:
      x_res=175;
      break;
    case 0x96:
      x_res=95;
      break;
  }
  switch (RBack())                   // fill y_res variable
  {
    case 0x22:
      y_res=219;
      break;
    case 0x28:
      y_res=127;
      break;
    case 0x32:
      y_res=319;
      break;
    case 0x60:
      y_res=159;
      break;
    case 0x64:
      y_res=63;
      break;
    case 0x76:
      y_res=175;
      break;
    case 0x96:
      y_res=95;
      break;
  }                   
}

void uOLED::sdInit()
{
   write(0x40);
   write(0x69);
   res=RBack();
}


void uOLED::sdSetMemAdrr(char Umsb, char Ulsb, char Lmsb, char Llsb)
{
   write(0x40);
   write(0x41);
   write(Umsb);
   write(Ulsb);
   write(Lmsb);
   write(Llsb);
   res=RBack();
}
   
void uOLED::sdWriteByte(byte data)
{
   write(0x40);
   write(0x77);
   write(data);
   res=RBack();
}  

byte uOLED::sdReadByte(void)
{
   write(0x40);
   write(0x72);
   return RBack();;
}
   
void uOLED::sdWriteBlock(long sector, byte data[])
{
   write(0x40);
   write(0x57);
   write(sector >> 16);
   write(sector >> 8);
   write(sector & 0xFF);
   for (int i=0; i<512; i++) {
      write(data[i]);
   }
   res=RBack();
}   
  
void uOLED::sdReadBlock(long sector, byte data[])
{  write(0x40);
   write(0x52);
   write(sector >> 16);
   write(sector >> 8);
   write(sector & 0xFF);
   for (int j=0; j<512; j++) {
      data[j]=RBack();
   }
} 
   
void uOLED::sdScreenCopy(byte x, byte y, byte width, byte height, long sector)   
{
   write(0x40);
   write(0x43);
   write(x);
   write(y);
   write(width);
   write(height);
   write(sector >> 16);
   write(sector >> 8);
   write(sector & 0xFF);
   res=RBack();
}

void uOLED::sdDisplayImage(byte x, byte y, byte width, byte height, boolean twoBytesPP, long sector)
{
   write(0x40);
   write(0x49);
   write(x);
   write(y);
   write(width);
   write(height);
   if (twoBytesPP == true) {
	  write(0x10);
   } else {
      write(0x08);
   }
   write(sector >> 16);         // TODO!! is this the correct way??
   write(sector >> 8);
   write(sector & 0xFF);
   res=RBack();
}

void uOLED::sdDisplayVideo(byte x, byte y, byte width, byte height, boolean twoBytesPP, byte fdelay, int frames, long sector)
{
   write(0x40);
   write(0x56);
   write(x);
   write(y);
   write(width);
   write(height);
   if (twoBytesPP == true) {
	  write(0x10);
   } else {
      write(0x08);
   }
   write(fdelay);
   write(frames >> 8);
   write(frames & 0xFF);
   write(sector >> 16);        // TODO!! is this the correct way??
   write(sector >> 8);
   write(sector & 0xFF);
   res=RBack();
} 

void uOLED::scrollEnable(boolean enable)
{
   write(0x24);
   write(0x53);
   write(0x0);
   write(enable);
   res=RBack();
}

void uOLED::scrollToRight(boolean toRight)
{
   write(0x24);
   write(0x53);
   write(0x01);
   write(toRight);
   res=RBack();
}

void uOLED::scrollSpeed(byte scrollSpeed)
{
   write(0x24);
   write(0x53);
   write(0x02);
   if (scrollSpeed > 7)             // Limit scrollspeed to max 7
   {            
      write(0x07);
   }
   else if (scrollSpeed < 0)         // Limit scrollspeed to min 0
   {      
      write(byte(0));
   } else 
   {
      write(scrollSpeed);
   }	  
   res=RBack();
}

void uOLED::playTone(int tone, int duration)
{
   write(0x4e);
   write(tone >> 8);
   write(tone & 0xFF);
   write(duration >> 8);
   write(duration & 0xFF);
   res=RBack();
}

#define isdigit(n) (n >= '0' && n <= '9')
void uOLED::playRtttl(char *song)
{
const int notes[] = { 0,
NOTE_C4, NOTE_CS4, NOTE_D4, NOTE_DS4, NOTE_E4, NOTE_F4, NOTE_FS4, NOTE_G4, NOTE_GS4, NOTE_A4, NOTE_AS4, NOTE_B4,
NOTE_C5, NOTE_CS5, NOTE_D5, NOTE_DS5, NOTE_E5, NOTE_F5, NOTE_FS5, NOTE_G5, NOTE_GS5, NOTE_A5, NOTE_AS5, NOTE_B5,
NOTE_C6, NOTE_CS6, NOTE_D6, NOTE_DS6, NOTE_E6, NOTE_F6, NOTE_FS6, NOTE_G6, NOTE_GS6, NOTE_A6, NOTE_AS6, NOTE_B6,
NOTE_C7, NOTE_CS7, NOTE_D7, NOTE_DS7, NOTE_E7, NOTE_F7, NOTE_FS7, NOTE_G7, NOTE_GS7, NOTE_A7, NOTE_AS7, NOTE_B7
};
  
  // Absolutely no error checking in here

  byte default_duration = 4;
  byte default_octave = 6;
  int bpm = 63;
  int num;
  long wholenote;
  long duration;
  byte note;
  byte scale;

  // format: d=N,o=N,b=NNN:
  // find the start (skip name, etc)

  while(*song != ':') song++;    // ignore name
  song++;                     // skip ':'

  // get default duration
  if(*song == 'd')
  {
    song++; song++;              // skip "d="
    num = 0;
    while(isdigit(*song))
    {
      num = (num * 10) + (*song++ - '0');
    }
    if(num > 0) default_duration = num;
    song++;                   // skip comma
  }

  Serial.print("duration: "); Serial.println(default_duration, 10);

  // get default octave
  if(*song == 'o')
  {
    song++; song++;              // skip "o="
    num = *song++ - '0';
    if(num >= 3 && num <=7) default_octave = num;
    song++;                   // skip comma
  }

  Serial.print("default_octave "); Serial.println(default_octave, 10);

  // get BPM
  if(*song == 'b')
  {
    song++; song++;              // skip "b="
    num = 0;
    while(isdigit(*song))
    {
      num = (num * 10) + (*song++ - '0');
    }
    bpm = num;
    song++;                   // skip colon
  }

  Serial.print("bpm: "); Serial.println(bpm, 10);

  // BPM usually expresses the number of quarter notes per minute
  wholenote = (60 * 1000L / bpm) * 4;  // this is the time for whole note (in milliseconds)

  Serial.print("wholenote "); Serial.print(wholenote, 10);Serial.println(" ms");


  // now begin note loop
  while(*song)
  {
    // first, get note duration, if available
    num = 0;
    while(isdigit(*song))
    {
      num = (num * 10) + (*song++ - '0');
    }
    
    if(num) duration = wholenote / num;
    else duration = wholenote / default_duration;  // we will need to check if we are a dotted note after

    // now get the note
    note = 0;

    switch(*song)
    {
      case 'c':
        note = 1;
        break;
      case 'd':
        note = 3;
        break;
      case 'e':
        note = 5;
        break;
      case 'f':
        note = 6;
        break;
      case 'g':
        note = 8;
        break;
      case 'a':
        note = 10;
        break;
      case 'b':
        note = 12;
        break;
      case 'p':
      default:
        note = 0;
    }
    song++;

    // now, get optional '#' sharp
    if(*song == '#')
    {
      note++;
      song++;
    }

    // now, get optional '.' dotted note
    if(*song == '.')
    {
      duration += duration/2;
      song++;
    }
  
    // now, get scale
    if(isdigit(*song))
    {
      scale = *song - '0';
      song++;
    }
    else
    {
      scale = default_octave;
    }

    scale += OCTAVE_OFFSET;

    if(*song == ',')
      song++;       // skip comma for next note (or we may be at the end)

    // now play the note

    if(note)
    {
      Serial.print("Playing: ");
      Serial.print(scale, 10); Serial.print(' ');
      Serial.print(note, 10); Serial.print(" (");
      Serial.print(notes[(scale - 4) * 12 + note], 10);
      Serial.print(") ");
      Serial.println(duration, 10);
      playTone(notes[(scale - 4) * 12 + note], duration);
    }
    else
    {
      Serial.print("Pausing: ");
      Serial.println(duration, 10);
      delay(duration);
    }
  }
}