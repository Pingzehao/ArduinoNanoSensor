#include "GPSShield.h"

GPSShield::GPSShield()
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

GPSShield::GPSShield(char* NMEA)
{
//TODO: Skip over/reject if it's not valid

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

GPSShield::GPSShield(GPSShield& toCopy)
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

void GPSShield::operator=(GPSShield toCopy)
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

int GPSShield::getDay()
{
	return mDay;
}

int GPSShield::getMonth()
{
	return mMonth;
}

int GPSShield::getYear()
{
	return mYear;
}

int GPSShield::getHour()
{
	return mHour;
}

int GPSShield::getMinute()
{
	return mMinute;
}

int GPSShield::getSecond()
{
	return mSecond;
}

int GPSShield::getMillis()
{
	return mMillisecond;
}

int GPSShield::getLatDegrees()
{
	return mLatDegrees;
}

double GPSShield::getLatMinutes()
{
	return mLatMinutes;
}

int GPSShield::getLongDegrees()
{
	return mLongDegrees;
}

double GPSShield::getLongMinutes()
{
	return mLongMinutes;
}

char GPSShield::getLongHeading()
{
	return mLongHeading;
}

char GPSShield::getLatHeading()
{
	return mLatHeading;
}

bool GPSShield::isConnected()
{
	return connectionValid;
}

