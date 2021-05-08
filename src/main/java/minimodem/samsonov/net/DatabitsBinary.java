/**
 * DatabitsBinary.java
 * Created from databits_binary.c, databits.h @ https://github.com/kamalmostafa/minimodem
 * Rawbits N-bit binary data decoder !!! ONLY !!!
 */

package minimodem.samsonov.net;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DatabitsBinary implements IEncodeDecode {

    private static final Logger fLogger = LogManager.getFormatterLogger("DatabitsBinary");
    /**
     * returns nbytes decoded
     */

    public int decode(byte[] dataoutP, int dataoutSize, long bits, int nDatabits) {
        if (dataoutP == null) { return 0; } /* databits processor reset: noop */

        if (dataoutSize < nDatabits+1) {
            fLogger.fatal("dataoutSize (%d) is less then nDatabits+1 (%d)", dataoutSize, nDatabits + 1);
            System.exit(-1);
        }
        int j;
        for (j = 0; Integer.compareUnsigned(j, nDatabits) < 0; j++) {
            dataoutP[j] = (byte)((bits >>> j & 1) + '0');
        }
        dataoutP[j] = '\n';
        return nDatabits + 1;
    }

    public int encode(int[] databitsOutp, byte charOut) {
        fLogger.error("A call to encode which is not implemented for DatabitsBinary");
        return 0;
    }
}
