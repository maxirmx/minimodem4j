/**
 * DatabitsAscii8.java
 * Created from databits_ascii.c, databits.h @ https://github.com/kamalmostafa/minimodem
 * ASCII 8-bit data databits decoder/encoder (passthrough)
 */
package minimodem;

public class DatabitsAscii8 implements IEncodeDecode {
     /**
       * returns the number of datawords stuffed into *databitsOutp  (==1)
	   */
    public int encode(int[] databitsOutp, byte charOut) {
        databitsOutp[0] = charOut;
        return 1;
    }

    /**
     * returns nbytes decoded
     */
    public int decode(byte[] dataoutP, int dataoutSize, long bits, int nDatabits) {
        if(dataoutP == null) { return 0; } /* databits processor reset: noop */

        bits &= 0xFF;
        dataoutP[0] = (byte)bits;
        return 1;
    }
}
