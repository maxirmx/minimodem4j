/**
 * minimodem4j
 * Ascii8.java
 * Created from databits_ascii.c, databits.h @ https://github.com/kamalmostafa/minimodem
 */
package minimodem.databits;

/**
 * ASCII 8-bit data bits decoder/encoder (passthrough)
 */
public class DataBitsAscii8 implements IEncodeDecode {
    /**
     * encode
     * @param databitsOutp  the buffer for encoded data
     * @param charOut  a byte to encode
     * @return the number of data words stuffed into databitsOutp  (==1)
     */
    public int encode(int[] databitsOutp, byte charOut) {
        databitsOutp[0] = charOut;
        return 1;
    }
    /**
     * decode
     * @param dataoutP the buffer for encoded data
     *                 null value means processor reset, noop for this decoder
     * @param dataoutSize the size of the buffer encoded data
     * @param bits  the data to decode
     * @param nDatabits  the number of bits to decode
     * @return returns the number of bytes decoded (==1)
     */
    public int decode(byte[] dataoutP, int dataoutSize, long bits, int nDatabits) {

        if (dataoutP == null) { return 0; } /* databits processor reset: noop */

        bits &= 0xFF;
        dataoutP[0] = (byte)bits;
        return 1;
    }
}
