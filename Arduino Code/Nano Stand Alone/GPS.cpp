#include "GPS.h"

GPS::GPS()
{
	mDay = 0;
	mMonth = 12;
	mYear = 0;
	mHour = 0;
	mMinute = 0;
	mSecond = 0;
	mMillisecond = 0;
	mLongDegrees = 0;
	mLatDegrees = 0;
	mLongMinutes = 0;
	mLatMinutes = 0;
	mLongHeading = 0;
	mLatHeading = 0;
	connectionValid = false;
}

/**
*
* Takes a GPRMC string and parses it
* $GPRMC,hhmmss.ss,A,llll.ll,a,yyyyy.yy,a,x.x,x.x,ddmmyy,x.x,a*hh
* 1  = UTC of position fix
* 2  = Data status (V = navigation receiver warning, A = valid)
* 3  = Latitude of fix
* 4  = N or S
* 5  = Longitude of fix
* 6  = E or W
* 7  = Speed over ground in knots
* 8  = Track made good in degrees True
* 9  = UT date
* 10 = Magnetic variation degrees (Easterly var. subtracts from true course)
* 11 = E or W
* 12 = Checksum
*/
GPS::GPS(char* NMEA)
{
	int i = 0;
	//Get past the $GPRMC
	for(; NMEA[i] != ','; ++i) {}
	//Advance past the comma
	i++;
	
	//First digit of hour
	mHour = (NMEA[i] - 48) * 10;
	//Second digit of hour
	i++;
	mHour += NMEA[i] - 48;
	
	//First digit of minute
	i++;
	mMinute = (NMEA[i] - 48) * 10;
	//Second digit of minute
	i++;
	mMinute += NMEA[i] - 48;
	
	//First digit of second
	i++;
	mSecond = (NMEA[i] - 48) * 10;
	//Second digit of second
	i++;
	mSecond += NMEA[i] - 48;
	
	//First digit of millisecond
	i += 2;
	mMillisecond = (NMEA[i] - 48) * 10;
	//Second digit of millisecond
	i++;
	mMillisecond += NMEA[i] - 48;
	
	for(; NMEA[i] != ','; ++i) {}
	//Advance past the comma
	i++;
	
	if(NMEA[i] == 'A')
		connectionValid = true;
	else
		connectionValid = false;
	
	//First digit of degree		
	i += 2;
	mLatDegrees = (NMEA[i] - 48) * 10;
	//Second digit of degree
	i++;
	mLatDegrees += NMEA[i] - 48;
	
	//First digit of minute
	i++;
	mLatMinutes = (NMEA[i] - 48) * 10;
	//Second digit of degree
	i++;
	mLatMinutes += (NMEA[i] - 48);
	//Decimal digits of degree
	i += 2;
	mLatMinutes += (NMEA[i] - 48) / 10.0;
	i++;
	mLatMinutes += (NMEA[i] - 48) / 100.0;
	i++;
	mLatMinutes += (NMEA[i] - 48) / 1000.0;
	i++;
	mLatMinutes += (NMEA[i] - 48) / 10000.0;
	i++;
	mLatMinutes += (NMEA[i] - 48) / 100000.0;
	
	//Latitude Heading
	i ++;
	mLatHeading = NMEA[i];
	
	// Advance past comma
	i++;
	
	//First digit of degree		
	i++;
	mLongDegrees = (NMEA[i] - 48) * 100;
	//Second digit of degree
	i++;
	mLongDegrees += (NMEA[i] - 48) * 10;
	//Third digit of degree
	i++;
	mLongDegrees += NMEA[i] - 48;
	
	//First digit of minute
	i++;
	mLongMinutes = (NMEA[i] - 48) * 10;
	//Second digit of degree
	i++;
	mLongMinutes += (NMEA[i] - 48);
	//Decimal digits of degree
	i += 2;
	mLongMinutes += (NMEA[i] - 48) / 10.0;
	i++;
	mLongMinutes += (NMEA[i] - 48) / 100.0;
	i++;
	mLongMinutes += (NMEA[i] - 48) / 1000.0;
	i++;
	mLongMinutes += (NMEA[i] - 48) / 10000.0;
	i++;
	mLongMinutes += (NMEA[i] - 48) / 100000.0;
	
	//Longitude Heading
	i ++;
	mLongHeading = NMEA[i];
	
	for(; NMEA[i] != ','; ++i) {}
	//Advance past the comma
	i++;
	for(; NMEA[i] != ','; ++i) {}
	//Advance past the comma
	i++;
	for(; NMEA[i] != ','; ++i) {}
	
	//First digit of day
	i++;
	mDay = (NMEA[i] - 48) * 10;
	//Second digit of day
	i++;
	mDay += (NMEA[i] - 48);
	
	//First digit of month
	i++;
	mMonth = (NMEA[i] - 48) * 10;
	i++;
	//Second digit
	mMonth += (NMEA[i] - 48);
	
	//First digit of month
	i++;
	mYear = (NMEA[i] - 48) * 10;
	i++;
	//Second digit
	mYear += (NMEA[i] - 48);
}

GPS::GPS(GPS& toCopy)
{
	this->mDay = toCopy.getDay();
	this->mMonth = toCopy.getMonth();
	this->mYear = toCopy.getYear();
	this->mHour = toCopy.getHour();
	this->mMinute = toCopy.getMinute();
	this->mSecond = toCopy.getSecond();
	this->mMillisecond = toCopy.getMillis();
	this->mLongDegrees = toCopy.getLongDegrees();
	this->mLatDegrees = toCopy.getLatDegrees();
	this->mLongMinutes = toCopy.getLongMinutes();
	this->mLatMinutes = toCopy.getLatMinutes();
	this->mLongHeading = toCopy.getLongHeading();
	this->mLatHeading = toCopy.getLatHeading();
	this->connectionValid = toCopy.isConnected();
}

void GPS::operator=(GPS toCopy)
{
	this->mDay = toCopy.getDay();
	this->mMonth = toCopy.getMonth();
	this->mYear = toCopy.getYear();
	this->mHour = toCopy.getHour();
	this->mMinute = toCopy.getMinute();
	this->mSecond = toCopy.getSecond();
	this->mMillisecond = toCopy.getMillis();
	this->mLongDegrees = toCopy.getLongDegrees();
	this->mLatDegrees = toCopy.getLatDegrees();
	this->mLongMinutes = toCopy.getLongMinutes();
	this->mLatMinutes = toCopy.getLatMinutes();
	this->mLongHeading = toCopy.getLongHeading();
	this->mLatHeading = toCopy.getLatHeading();
	this->connectionValid = toCopy.isConnected();
}

int GPS::getDay()
{
	return mDay;
}

int GPS::getMonth()
{
	return mMonth;
}

int GPS::getYear()
{
	return mYear;
}

int GPS::getHour()
{
	return mHour;
}

int GPS::getMinute()
{
	return mMinute;
}

int GPS::getSecond()
{
	return mSecond;
}

int GPS::getMillis()
{
	return mMillisecond;
}

int GPS::getLatDegrees()
{
	return mLatDegrees;
}

double GPS::getLatMinutes()
{
	return mLatMinutes;
}

int GPS::getLongDegrees()
{
	return mLongDegrees;
}

double GPS::getLongMinutes()
{
	return mLongMinutes;
}

char GPS::getLongHeading()
{
	return mLongHeading;
}

char GPS::getLatHeading()
{
	return mLatHeading;
}

bool GPS::isConnected()
{
	return connectionValid;
}

