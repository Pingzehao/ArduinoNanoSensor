#include "SDBuffer.h"
#include "WProgram.h"

static int power(int base, int power)
{
	int result = 1;
	for(int i = 0; i < power; ++i)
		result *= base;
	return result;
}

SDBuffer::SDBuffer()
{
	for(int i = 0; i < 512; ++i)
	{
		mBuf[i] = '\0';
	}
	mSize = 0;
	mSectorHigh = 0x00;
	mSectorMid = 0x00;
	mSectorLow = 0x00;
}

void SDBuffer::add(char c)
{
	mBuf[mSize++] = c;
	if(mSize == 512)
	{
		Serial.write(OLED_EXT_CMD);
		Serial.write(OLED_WRITE_SECTOR);
		Serial.print((byte) mSectorHigh);
		Serial.print((byte) mSectorMid);
		Serial.print((byte) mSectorLow);
/*		Serial.write((byte) 0x00);
		Serial.write((byte) 0x00);
		Serial.write((byte) 0x00);*/
		print();
		mSectorLow += 0x01;
		reset();
	}
}

void SDBuffer::add(int n)
{
	int value = n;
	int digits = log10(value) + 1;
	for(int i = digits; i > 0; --i)
	{
		char toAdd = value / power(10, i - 1) + 48;
		value = value % power(10, i - 1);
		add(toAdd);
	}
}

void SDBuffer::add(long l)
{
	int value = l;
	int digits = log10(value) + 1;
	for(int i = digits; i > 0; --i)
	{
		char toAdd = value / power(10, i - 1) + 48;
		value = value % power(10, i - 1);
		add(toAdd);
	}
}

void SDBuffer::add(double d)
{
	int wholeNumber = d;
	int decimals = (d - (double) wholeNumber) * 1000;
	add(wholeNumber);
	add('.');
	add(decimals);
}

void SDBuffer::add(char* buf)
{
	for(int i = 0; buf[i] != '\0'; ++i)
	{
		add(buf[i]);
	}
}

void SDBuffer::print()
{
	for(int i = 0; i < 512; ++i)
	{
		Serial.print(mBuf[i]);
	}
}

void SDBuffer::reset()
{
	for(int i  = 0; i < 512; ++i)
	{
		mBuf[i] = '\0';
	}
	mSize = 0;
}

int SDBuffer::getSize()
{
	return mSize;
}

