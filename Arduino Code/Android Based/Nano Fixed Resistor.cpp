#define TYPE 1
#define COMMAND 2
#define ARG1 3
#define ARG2 4
#define EXECUTE 5
#define REPEATED_SAMPLING 6
#define RATE 7
#define EOA '.'
#define MAXVAL 10


#define OUT 0
#define IN 1

#define HIGHVAL 1
#define LOWVAL 0


#define DIGITAL 10
#define ANALOG 11
#define REPEATED 12

#define MODE 20
#define PULL 21
#define READ 22
#define WRITE 23
#define SAMPLING 24

#define ANALOG_PINS 8
#define USB 9600
#define BLUETOOTH 115200

int char2num(char *str, int len);
int main(void);
void execute(void);

int inByte = 0;
int digitalMode[14];
int sigType = 0;
int operation = 0;
int command = 0;
char val[MAXVAL];
int valIndex = 0;
int val1 = 0;
int val2 = 0;
int state = TYPE;
int rsValues[ANALOG_PINS];
boolean rsState[ANALOG_PINS];
int rsRate;

void setup() {
	Serial.begin(BLUETOOTH);
	//Serial.begin(USB);
	//Serial.print("Enter a command:");
	//Sets the resistor for the Temperature Sensor to 7.84kOhms
	int mm;
	for (mm=0;mm<14;mm++) {
		digitalMode[mm] = OUT;
		pinMode(mm, OUTPUT);
	}
}


void loop() 
{
	if(Serial.available() > 0) 
	{
		inByte = Serial.read();
		
		if(inByte != ' ') 
		{
			switch(state) 
			{
			case TYPE:	
				valIndex = 0;
				state = COMMAND;
				if(inByte == 'A') 
				{
					sigType = ANALOG;
				} 
				else if(inByte == 'D') 
				{
					sigType = DIGITAL;
					
				} 
				else if(inByte == 'R')
				{
					for(int i = 0; i < ANALOG_PINS; i++)
					{
						rsValues[i] = 0;
					}
					state = REPEATED_SAMPLING;
				}
				else
				{
					Serial.print("E: Invalid signal type");
					Serial.println(inByte);
					state = TYPE;
				}
				break;
			case REPEATED_SAMPLING:
				if(inByte == 'a')
					rsState[0] = true;
				else if(inByte == 'b')
					rsState[1] = true;
				else if(inByte == 'c')
					rsState[2] = true;
				else if(inByte == 'd')
					rsState[3] = true;
				else if(inByte == 'e')
					rsState[4] = true;
				else if(inByte == 'f')
					rsState[5] = true;
				else if(inByte == 'g')
					rsState[6] = true;
				else if(inByte == 'h')
					rsState[7] = true;
				else if(inByte == EOA)
					state = RATE;
				else
					state = TYPE;
				break;
			case RATE:
				if(inByte >= '0' && inByte <= '9')
				{
					if(rsRate > 0)
					{
						rsRate *= 10;
						rsRate += inByte - '0';
					}
					else
						rsRate += inByte - '0';
					break;
				}
				else if(inByte == EOA)
				{
					while(true)
					{
						delay(rsRate);
						Serial.print(';');
						for(int i = 0; i < ANALOG_PINS; i++)
						{
							if(rsState[i])
							{
								Serial.print(analogRead(i));
								Serial.print('.');
							}
							else
							{
								Serial.print("0.");
							}
						}
						Serial.print(';');
						if(Serial.available() > 0)
						{
							if(Serial.read() == EOA)
							{
								for(int i = 0; i < ANALOG_PINS; i++)
								{
									rsState[i] = false;
								}
								state = TYPE;
								rsRate = 0;
								inByte = 0;
								break;
							}
						}
					}
				}
				while(Serial.available() > 0)
				{
					Serial.read();
				}
				state = TYPE;
				break;
			case COMMAND:
				state = ARG1;
				if(inByte == 'M') 
				{
					operation = MODE;
				} 
				else if(inByte == 'P') 
				{
					operation = PULL;
				} 
				else if(inByte == 'R') 
				{
					operation = READ;
				} 
				else if(inByte == 'W') 
				{
					operation = WRITE;
				} 
				else 
				{
					Serial.println("E: Invalid operation");
					state = TYPE;
				}
				break;

			case ARG1:	
				if(valIndex > MAXVAL) 
				{
					Serial.println("Runaway argument");
					state = TYPE;
				} 
				else if(inByte != EOA) 
				{
					val[valIndex] = inByte;
					valIndex++;
				} 
				else 
				{  // got the argument
					if(valIndex > 0) 
					{
						val1 = char2num(val, valIndex);
						valIndex = 0;
						if( (sigType == ANALOG && operation == MODE) || (operation == READ) ) 
						{
							state = EXECUTE;
						} 
						else 
						{
							state = ARG2;
						}
					} 
					else 
					{
						Serial.println("Empty argument");
						state = TYPE;
					}
				}
				break;


			case ARG2:
				if(valIndex > MAXVAL) 
				{
					Serial.println("Runaway argument");
					state = TYPE;
				} 
				else if(inByte != EOA) 
				{
					val[valIndex] = inByte;
					valIndex++;
				}
				else 
				{  // got the argument
					if(valIndex > 0)
					{
						val2 = char2num(val, valIndex);
						state = EXECUTE;
					} 
					else 
					{
						Serial.println("Empty argument");
						state = TYPE;
					}
				}
				break;
			} // end switch

			if(state == EXECUTE) 
			{
				execute();
				state = TYPE;
			}
		} // end if not whitespace
		//  } // end if serial available
	} //end loop
} //end main

int char2num(char *str, int len) 
{
	int res = 0;
	int ii;
	int v;
	int place = 1;
	for(ii=len-1; ii>=0; ii--) 
	{
		v = str[ii] - '0';
		res += place * v;
		place = place * 10;
	}
	return res;
}

int readres;

void execute(void)
{
	if(sigType == DIGITAL) 
	{
		if(operation == MODE) 
		{
			if(((val1 > 1) && (val1 < 13)) || ((val1 == 13) && (val2 == OUT))) 
			{
				if((val2 == OUT) || (val2 == IN)) 
				{
					if(val2 == OUT) 
					{
						Serial.print("Digital PinMode: ");
						Serial.print(val1);
						Serial.println(" OUT");
						pinMode(val1, OUTPUT);
						digitalMode[val1] = OUT;
					} 
					else 
					{
						Serial.print("Digital PinMode: ");
						Serial.print(val1);
						Serial.println(" IN");
						pinMode(val1, INPUT);
						digitalMode[val1] = IN;
					}
					digitalMode[val1] = val2; 
				} 
				else 
				{
					Serial.println("Digital PinMode: Invalid mode");
				}
			}
			else if((val1 == 13) && (val2 == IN))
			{
				Serial.println("Digital PinMode: Pin 13 output only");
			} 
			else 
			{
				Serial.println("Digital PinMode: pin value out of range");
			}

		} 
		else if(operation == PULL) 
		{
			if((val1 > 1) && (val1 < 14)) 
			{
				if(digitalMode[val1] == IN) 
				{
					if((val2 == HIGHVAL) || (val2 == LOWVAL))
					{
						if(val2 == HIGHVAL) 
						{
							Serial.print("Digital Pull: ");
							Serial.print(val1);
							Serial.println(" HIGH");
							digitalWrite(val1, HIGH);       
						} 
						else 
						{
							Serial.print("Digital Pull: ");
							Serial.print(val1);
							Serial.println(" LOW");
							digitalWrite(val1, LOW);       
						}
					} 
					else 
					{
						Serial.println("Digital Pull: invalid pull value");
					}
				} 
				else 
				{
					Serial.print("Digital Pull: pin ");
					Serial.print(val1);
					Serial.println(" not input mode");
				}
			} 
			else 
			{
				Serial.println("Digital Pull: pin value out of range");
			}
		} 
		else if(operation == READ) 
		{
			if((val1 > 1) && (val1 < 14))
			{
				if(digitalMode[val1] == IN) 
				{
					Serial.print("Digital Read: ");
					Serial.println(val1);	
					readres = digitalRead(val1);
					Serial.print("-> ");
					Serial.println(readres);
				} 
				else 
				{
					Serial.print("Digital Read: pin ");
					Serial.print(val1);	
					Serial.println(" not input mode");
				}
			}
			else 
			{
				Serial.println("Digital Read: pin value out of range");
			}


		}
		else if(operation == WRITE) 
		{
			if((val1 > 1) && (val1 < 14)) 
			{
				if(digitalMode[val1] == OUT) 
				{
					if((val2 == 0) || (val2 == 1)) 
					{
						Serial.print("Digital Write: pin ");
						Serial.print(val1);	
						Serial.print(" ");
						Serial.println(val2);	
						digitalWrite(val1, val2);
					} 
					else 
					{
						Serial.println("Digital Write: invalid write value");
					}
				} 
				else 
				{
					Serial.print("Digital Write: pin ");
					Serial.print(val1);	
					Serial.println(" not output mode");
				}
			} 
			else 
			{
				Serial.println("Digital Write: pin value out of range");
			}
		}
	} 
	else	//Analog
	{
		if(operation == MODE)
		{
			if(val1 == 0)
			{
				Serial.println("Analog Reference: Default");
			}
			else if(val1 == 1)
			{
				Serial.println("Analog Reference: External Reference not Supported");
			} 
			else 
			{
				Serial.println("Analog Reference: Invalid reference");
			}
		} 
		else if(operation == READ) 
		{
			if((val1 >= 0) && (val1 < ANALOG_PINS))
			{
				//Serial.print("Analog Read: pin ");
				//Serial.println(val1);
				readres = analogRead(val1);
				//Serial.print("-> ");
				Serial.print(".");
				Serial.print(readres);
				Serial.print(".");
			} 
			else 
			{
				Serial.println("Analog Read: pin value out of range");
			}
		} 
		else if(operation == WRITE) 
		{
			if((val1 == 3) || (val1 == 5) || (val1 == 6) || (val1 == 9) || (val1 == 10) || (val1 == 11))
			{
				if(digitalMode[val1] == OUT) 
				{
					if((val2 >= 0) && (val2 <= 255)) 
					{
						Serial.print("Analog Write: pin ");
						Serial.print(val1);	
						Serial.print(" ");
						Serial.println(val2);	
						analogWrite(val1,val2);
					} 
					else 
					{
						Serial.println("Analog Write: invalid write value");
					}
				} 
				else 
				{
					Serial.print("Analog Write: pin ");
					Serial.print(val1);	
					Serial.println(" not output mode");
				}
			} 
			else 
			{
				Serial.println("Analog Write: pin value out of range");
			}
		}
	}
	//Serial.println("Enter a command:");
}