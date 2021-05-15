/**
 * minimodem4j
 * Binary.java
 * Created from databits_binary.c, databits.h @ https://github.com/kamalmostafa/minimodem
 */

package minimodem.databits;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Rawbits N-bit binary data decoder
 * (no encoder)
 */
public class DataBitsBinary implements IEncodeDecode {
    /**
     * encode  -- placeholder only
     * @param databitsOutp  the buffer for encoded data
     * @param charOut  a byte to encode
     * @return the number of data words stuffed into databitsOutp  (Always 0)
     */
    public int encode(int[] databitsOutp, byte charOut) {
        fLogger.error("A call to encode which is not implemented for DataBitsBinary");
        return 0;
    }

    private static final Logger fLogger = LogManager.getFormatterLogger(DataBitsBinary.class);
    /**
     * decode
     * @param dataoutP the buffer for encoded data
     *                 null value means processor reset, noop for this decoder
     * @param dataoutSize the size of the buffer encoded data
     * @param bits  the data to decode
     * @param nDatabits  the number of bits to decode
     * @return returns the number of bytes decoded
     */
    public int decode(byte[] dataoutP, int dataoutSize, long bits, int nDatabits) {
        if (dataoutP == null) { return 0; } /* databits processor reset: noop */

        if (dataoutSize < nDatabits+1) {
            fLogger.error("dataoutSize (%d) is less then nDatabits+1 (%d)", dataoutSize, nDatabits + 1);
            return 0;
        }
        int j;
        for (j = 0; j < nDatabits; j++) {
            dataoutP[j] = (byte)((bits >>> j & 1) + '0');
        }
        dataoutP[j] = '\n';
        return nDatabits + 1;
    }

}
