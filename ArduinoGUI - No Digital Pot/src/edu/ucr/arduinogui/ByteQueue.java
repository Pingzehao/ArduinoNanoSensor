package edu.ucr.arduinogui;

import android.util.Log;

public class ByteQueue {
    private static final String LOG_TAG = "ArduinoGUI";
	private static final int ANALOG_PINS = 8;
	private byte[] mBuffer;
    private int mHead;
    private int mStoredBytes;
    private int[] rPollingData = new int[ANALOG_PINS];
    
    public ByteQueue(int size) {
        mBuffer = new byte[size];
    }

    public int getBytesAvailable() {
        synchronized(this) {
            return mStoredBytes;
        }
    }

    public int read(byte[] buffer, int offset, int length)
        throws InterruptedException {
        if (length + offset > buffer.length) {
            throw
                new IllegalArgumentException("length + offset > buffer.length");
        }
        if (length < 0) {
            throw
            new IllegalArgumentException("length < 0");

        }
        if (length == 0) {
            return 0;
        }
        synchronized(this) {
            while (mStoredBytes == 0) {
                wait();
            }
            Log.v(LOG_TAG, "" + mBuffer.length);
            int totalRead = 0;
            int bufferLength = mBuffer.length;
            boolean wasFull = bufferLength == mStoredBytes;
            while (length > 0 && mStoredBytes > 0) {
                int oneRun = Math.min(bufferLength - mHead, mStoredBytes);
                int bytesToCopy = Math.min(length, oneRun);
                System.arraycopy(mBuffer, mHead, buffer, offset, bytesToCopy);
                mHead += bytesToCopy;
                if (mHead >= bufferLength) {
                    mHead = 0;
                }
                mStoredBytes -= bytesToCopy;
                length -= bytesToCopy;
                offset += bytesToCopy;
                totalRead += bytesToCopy;
            }
            if (wasFull) {
                notify();
            }
            return totalRead;
        }
    }

    public int[] writePolling(byte[] buffer, int offset, int length)
    throws InterruptedException {
        if (length + offset > buffer.length) {
            throw
                new IllegalArgumentException("length + offset > buffer.length");
        }
        if (length < 0) {
            throw
            new IllegalArgumentException("length < 0");

        }
        if (length == 0) {
            return null;
        }
        synchronized(this) {
        	int bufferLength = mBuffer.length;
            boolean wasEmpty = mStoredBytes == 0;
            while (length > 0) {
                while(bufferLength == mStoredBytes) {
                    wait();
                }
                int tail = mHead + mStoredBytes;
                int oneRun;
                if (tail >= bufferLength) {
                    tail = tail - bufferLength;
                    oneRun = mHead - tail;
                } else {
                    oneRun = bufferLength - tail;
                }
                int bytesToCopy = Math.min(oneRun, length);

                System.arraycopy(buffer, offset, mBuffer, tail, bytesToCopy);
	            String s = new String(mBuffer);
	            if(bytesToCopy > 1)
	            {
	            	//The index of the last char we're copying in
		            int i = tail + bytesToCopy - 1; 
		            if(s.charAt(i) == ';')
		            {
		            	--i; //Reduce the index by one so that it's not a ';'.
		            	//Subtracts from i until it reaches the starting ';'
			            for(; i > 0 && s.charAt(i) != ';'; --i)
			            {
			            }
			            ++i; 
			            //Increases i so that it's now at the first digit of the 
			            //number
		            	for(int j = 0; j < ANALOG_PINS; ++j)
		            	{
		            		String voltage = "";
		            		for(; s.charAt(i) != '.' && i < 50/*&& i < s.length() && i > -1*/; ++i)
		            		{
		            			voltage += s.charAt(i);
		            		}
		            		//Increases i so that it's now past the first dot
		            		++i; 
		            		try
		            		{
		            			rPollingData[j] = Integer.parseInt(voltage);
		            		}
		            		catch(NumberFormatException e)
		            		{
		            			Log.e("ArduinoGUI", "ByteQueue::writePolling:" +
		            					"ErrorParsingStringToInt: " + voltage);
		            		}		
		            	}
		            	//tail = 0;
		            	return rPollingData;
		            }
	            }
                offset += bytesToCopy;
                mStoredBytes += bytesToCopy;
                length -= bytesToCopy;
            }
            
            if (wasEmpty) {
                notify();
            }
        }
		return null;
    }
    
    public byte[] write(byte[] buffer, int offset, int length)
    throws InterruptedException {
        if (length + offset > buffer.length) {
            throw
                new IllegalArgumentException("length + offset > buffer.length");
        }
        if (length < 0) {
            throw
            	new IllegalArgumentException("length < 0");

        }
        if (length == 0) {
        	//TODO Leave
        }
        synchronized(this) {
        	int bufferLength = mBuffer.length;
            boolean wasEmpty = mStoredBytes == 0;
            while (length > 0) {
                while(bufferLength == mStoredBytes) {
                    wait();
                }
                int tail = mHead + mStoredBytes;
                int oneRun;
                if (tail >= bufferLength) {
                    tail = tail - bufferLength;
                    oneRun = mHead - tail;
                } else {
                    oneRun = bufferLength - tail;
                }
                int bytesToCopy = Math.min(oneRun, length);
                System.arraycopy(buffer, offset, mBuffer, tail, bytesToCopy);
                offset += bytesToCopy;
                mStoredBytes += bytesToCopy;
                length -= bytesToCopy;
            }
            if (wasEmpty) {
                notify();
            }
            return mBuffer;
        }
    }
    
    public void clearBuffer()
    {
    	mBuffer = new byte[4096];
    	mStoredBytes = 0;
    }
    
    public int[] getData(int n)
    {
    	return rPollingData;
    }
}