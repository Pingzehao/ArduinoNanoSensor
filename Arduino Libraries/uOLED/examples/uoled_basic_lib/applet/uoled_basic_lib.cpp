/* color is a 16 bit value.
 binary representation       1111111111111111        (this will give white) same as 0xffff in hexadecimal or 65535 decimal
 //                                     |||||-> value for blue
 //                               ||||||-> value for green
 //                          |||||-> value for red
 */

#include <uOLED.h>              
#include <colors.h>

#include "WProgram.h"
void setup();
void loop();
uOLED uoled;                    // create an instance of the uOLED class

byte i,j;                       // some variables for looping
int color=0;                    // a variable named color with an initial value of 0 (black)

void setup()
{
//  Serial.begin(115200);         
  uoled.begin(8,256000, &Serial);// with the reset line connected to pin 8 and a serial speed of 256000 
                                  // using hardware serial port1 (options are &Serial, &Serial1, &Serial2 and &serial3 on a Arduino Mega)
                                  // reset and initialize the display
  uoled.SetContrast(5);           // set the contrast of the display to 5 (0 to 15 default (not always) is 8)
  uoled.DeviceInfo();             // display some hardware/software info of the screen
  delay(2000);                    // info will be on screen for 2 seconds
  uoled.Cls();                    // clears the screen of everything on it. It uses the background color (at this moment it's at default black)
}

void loop()
{
  // uoled.x_res is the horizontal resolution of the display u are using.
  // uoled.y_res is the vertical resolution of the display u are using.

  // sample for PutPixel
  // void PutPixel (char x, char y, int color);  
  for (i=0;i<100;i++)                  // loop for 100 pixels
  {
    uoled.PutPixel(random(uoled.x_res), random(uoled.y_res), random(65536));  // put a pixel at a random x coordinate, random y coordinate, with a random color.
    delay(10);                                                                // delay 10 ms between every PutPixel.
  }
  delay(1000);                         // after 100 pixels on display wait a second to watch them.
  uoled.Cls();                         // clear screen. (still default black)

  // Lines sample
  // void Line (char x1, char y1, char x2, char y2, int color);
  uoled.Line (0, 0, uoled.x_res, uoled.y_res, 65535);            // draw a line from top left to bottom right with color white
  uoled.Line (uoled.x_res, 0, 0, uoled.y_res, 65535);            // draw a line from top right to bottom left with color white
  delay(1000);

  // This example draws lines until you see a kind of * in shades of green and blue (star with crossing of lines in center of screen)
  color=0; 
  for (i=0;i<uoled.y_res;i+=2)                       
  {
    uoled.Line(0,uoled.y_res-i,uoled.x_res,i,color);           
    color++;
  }  
  for (i=0;i<uoled.x_res;i+=4)
  {
    uoled.Line(uoled.x_res-i,0,i,uoled.y_res,color);         
    color=color+32;
  }  
  delay(1000);
  uoled.Cls();                         // clear screen. (yep, still default black)            

  // rectangles sample
  // void Rectangle (char x1, char y1, char x2, char y2, int color, char filled);
  for (i=0;i<5;i++)                      // loop for 5 rectangles                   
  { 
    uoled.Rectangle(random(uoled.x_res), random(uoled.y_res), random(uoled.x_res), random(uoled.y_res), random(65536), random(2));
    delay(100);
  }
  delay(1000);
  uoled.Cls();

  // Circles sample
  // void Circle (char x, char y, char radius, int color, char filled);
  for (i=2;i<uoled.y_res/2;i+=4)
  {
    uoled.Circle(uoled.x_res/2, uoled.y_res/2, (uoled.y_res/2)-i, random(65536), FULL);   // will draw circles with center in midle of screen 
  }
  delay(1000);

  // CopyPaste sample
  // void CopyPaste (char xCopy, char yCopy, char xPaste, char yPaste, char Width, char Height);
  uoled.CopyPaste(0,0,uoled.x_res/2,uoled.y_res/2,uoled.x_res/2,uoled.y_res/2);   // will copy the top left quadrant to bottom right quadrant
  delay(1000);

  // ReadPixel sample
  // unsigned int ReadPixel(char x, char y);     // gives the hexadecimal value of the color at x, y.
  for (i=uoled.x_res/3;i<(uoled.x_res/3)*2; i++) // loop through 1/3 of width(x) of screen upto 2/3 of width(x) of screen.
  {
    for (j=0;j<uoled.y_res/3;j++)                // loop through height(y) 0 upto 1/3 of height(y) of screen
    {
      unsigned int color = ~uoled.ReadPixel(i, j); // flip every bit in the color read at (x)i, (y)j
      uoled.PutPixel(i, j, color);                 // and put a pixel at (x)i, (y)j with the 'flipped' color
    }
  }

  // Contrast sample
  // void SetContrast(char contrastValue);
  for (i=16; i> 0; i--)                      // loop for 15 contrast values
  {
    uoled.SetContrast(i-1);                  // set contrast to a value of 15 to 0
    delay(100);                              
  }
  for (i=0; i< 16; i++)                      // loop for 15 contrast values
  {
    uoled.SetContrast(i);                    // set contrast to a value of 0 to 15
    delay(100);
  }

  uoled.Cls();                                
  uoled.SetContrast(5);                      // set contrast to a sensable value                       

  // triangles sample
  // void Triangle(char x1, char y1, char x2, char y2, char x3, char y3, int color, char filled);
  for (i=0;i<5;i++)                          // loop for 5 triangles. This example could give wrong commands to the display as the display expects the coordinates
  {                                                                // to be in a anti clockwise fashion. (x2 < x1, x3 > x2, y2 > y1, y3 > y1)
    uoled.Triangle(random(uoled.x_res), random(uoled.y_res), random(uoled.x_res), random(uoled.y_res), random(uoled.x_res), random(uoled.y_res), random(65536), random(2));
    delay(100);
  }
  delay(1000);

  //Background color sample
  for (i=0;i<5;i++)                     // loop for 5 background colors
  {
    uoled.SetBackColor(random(65536));  // set the color to something random
    delay(100);
  }
  delay(1000);
  uoled.SetBackColor(0);               // set the background color back to 0 again
  uoled.SetPowerState(0);              // Switch power of display off
  delay(2000);
  uoled.SetPowerState(1);              // Switch power of display on
  uoled.SetContrast(5);                // on poweron the display doesn't go back to the default(8) or setted(5) contrast value. So set it manualy to something you like
  delay(1000);
  uoled.Cls();


  uoled.Rectangle(3,3,uoled.x_res/2,uoled.y_res/2,0xf800,FULL);         // draw a rectangle with color red
  
  // place character example
  // void Character (char Character, char font, char col, char row, int color, char transparent);
  // SMALL_FONT = 5x7 pixels, MEDIUM_FONT = 8x8 pixels, LARGE_FONT = 8x12 pixels
  uoled.Character('A', SMALL_FONT, 0,0,0xffff,0);  // draws 'A' at column 0 row 0 in white, transparent (objects behind text are visible) with small font. 
  delay(500);                                                                                         
  uoled.Character('A', LARGE_FONT, 3,0,0xffff,1);  // draws 'A' character at column 3 (fourth), row 0 in white, opaque (objects behind text blocked by background) with large font
  delay(500);  
  
  // place character graphical example
  // void CharacterGraphic(char Character, char font, char x, char y, int color, char Width, char Height, char transparent);
  uoled.CharacterGraphic('J', SMALL_FONT, 0,13,0xffff,2,2,0);  // draws a 'J' , small font, at row 0, column 13, in white, twice the font-width, twice the font-height transparently
  delay(500);
  uoled.CharacterGraphic('J', SMALL_FONT, 30,13,0xffff,2,1,1); // draws a 'J' , small font, at row 30, column 13, in white, twice the font width, normal font height and opaque
  delay(1500);
  uoled.Cls();


  uoled.Rectangle(3,3,uoled.x_res/2,uoled.y_res/2,0x001f,FULL);       // draws a rectangle in blue
  
  // text (string of text) example
  // void Text(char col, char row, char font, int color, char *Text, char transparent);
  uoled.Text(0,0,SMALL_FONT,0xffff,"String 1",0);  // draws at row 0, column 0, small font, color white, text "String 1" transparently
  delay(500);
  
  // text (string of text) graphical example
  // void TextGraphic(char x, char y, char font, int color, char Width, char Height, char *text, char transparent);
  uoled.TextGraphic(0,15,SMALL_FONT,0xffff,1,2,"TEST",1);    // draws at row 0, column 15, with small font, in color white, normal font width, twice font height, text "TEST", opaque
  delay(1500);
  uoled.Cls();
  
  // textbutton example
  // void TextButton (char state, char x, char y, int ButtonColor, char font, int TextColor, char TextWidth, char TextHeight, char *Text);
  uoled.TextButton(1,10,5,0xf800,SMALL_FONT,0xffff,1,1,"unpressed"); // draws a unpressed button with top left corner at x10,y5, in red, with small font, text in white,
  //                                                                 //   standard font sizes with the text "unpressed"                                                                   
  uoled.TextButton(0,10,25,0x07e0,SMALL_FONT,0x0000,1,2,"pressed");  // draws a pressed button with top left corner at x10,y25, in green, with small font, text in black,
  //                                                                 //   standard font width, double the font heighth with the text "pressed"
  delay(1500);
  uoled.Cls();


  
  // add bitmap character
  // void AddBMPChar(char reference, char data1, char data2, char data3, char data4, char data5, char data6, char data7, char data8);
  uoled.AddBMPChar(0x00,0x24,0x24,0x24,0x24,0x00,0x81,0x42,0x3c);  
  uoled.AddBMPChar(0x01,0x24,0x24,0x24,0x24,0x00,0x3c,0x42,0x81);
  
  // put/draw bitmap example
  // void PutBMPChar(char reference, char x, char y, int color);
  uoled.PutBMPChar(0x00,0,0,0xf800);
  uoled.PutBMPChar(0x01,8,8,0x07e0);
  delay(1500);
  uoled.Cls();

}


int main(void)
{
	init();

	setup();
    
	for (;;)
		loop();
        
	return 0;
}

