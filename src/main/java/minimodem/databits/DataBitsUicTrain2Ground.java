/**
 * minimodem4j
 * Binary.java
 * Created from databits_binary.c, databits.h @ https://github.com/kamalmostafa/minimodem
 */

package minimodem.databits;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.charset.StandardCharsets;

import static minimodem.databits.UicCodes.TYPE_TRAINGROUND;
import static minimodem.databits.UicCodes.databitsDecodeUic;

/**
 * http://ec.europa.eu/transport/rail/interoperability/doc/ccs-tsi-en-annex.pdf
 * (no encoder)
 */
public class DataBitsUicTrain2Ground implements IEncodeDecode {
    private static final Logger logger = LogManager.getLogger(DataBitsUicTrain2Ground.class);
    /**
     * encode  -- placeholder only
     * @param databitsOutp  the buffer for encoded data
     * @param charOut  a byte to encode
     * @return the number of data words stuffed into databitsOutp  (Always 0)
     */
    public int encode(int[] databitsOutp, byte charOut) {
        logger.error("A call to encode which is not implemented for DataBitsUicGround");
        return 0;
    }

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
        if (dataoutP == null) {  /* databits processor reset: noop */
            return 0;
        }
        String rs = databitsDecodeUic(bits, TYPE_TRAINGROUND);
        System.arraycopy(rs.getBytes(StandardCharsets.UTF_8), 0, dataoutP, 0, rs.length());
        return rs.length();
    }

}
