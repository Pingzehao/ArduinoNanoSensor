/*
*	GPS.h - GPS Library for parsing GPS NMEA GPRMC data
*	Copyright(c) 2011 University of California, Riverside
*	Author: Albert Chen
*
*/

#ifndef GPS_h
#define GPS_h

class GPS
{
	private:
		int mDay;
		int mMonth;
		int mYear;
		int mHour;
		int mMinute;
		int mSecond;
		int mMillisecond;
		int mLongDegrees;	
		int mLatDegrees;
		double mLongMinutes;
		double mLatMinutes;
		char mLongHeading;
		char mLatHeading;
		bool connectionValid;
	public:
		GPS();
		GPS(char* NMEA);
		GPS(GPS& toCopy);
		void operator=(GPS toCopy);
		int getDay();
		int getMonth();
		int getYear();
		int getHour();
		int getMinute();
		int getSecond();
		int getMillis();
		int getLatDegrees();
		double getLatMinutes();
		int getLongDegrees();
		double getLongMinutes();
		char getLongHeading();
		char getLatHeading();
		bool isConnected();
};

#endif