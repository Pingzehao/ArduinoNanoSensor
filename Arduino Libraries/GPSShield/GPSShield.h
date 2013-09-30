#ifndef GPSShield_h
#define GPSShield_h

class GPSShield
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
		GPSShield();
		GPSShield(char* NMEA);
		GPSShield(GPSShield& toCopy);
		void operator=(GPSShield toCopy);
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