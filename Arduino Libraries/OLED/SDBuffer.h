#ifndef SDBUFFER_H
#define SDBUFFER_H

#define OLED_EXT_CMD			0x40
#define OLED_READ_BYTE 			0x72
#define OLED_WRITE_FILE			0x74
#define OLED_WRITE_SECTOR		0X57

class SDBuffer
{
	private:
		char mBuf[512];
		int mSize;
		int mSectorHigh;
		int mSectorMid;
		int mSectorLow;
	public:
		SDBuffer();
		void add(char c);
		void add(int i);
		void add(long l);
		void add(double d);
		void add(char* buf);
		void print();
		void writeSD();
		void reset();
		int getSize();
};

#endif
