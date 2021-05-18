/**
 * minimodem4j
 * IEncodeDecode.java
 * Created from databits.h @ https://github.com/kamalmostafa/minimodem
 */

package minimodem.databits;

/**
 * Decoder/encoder interface
 */
public interface IEncodeDecode {
    /**
     * encode
     * @param databitsOutp  the buffer for encoded data
     * @param charOut  a byte to encode
     * @return the number of data words stuffed into databitsOutp
     * */
    int encode(int[] databitsOutp, byte charOut);
    /**
     * decode
     * @param dataoutP the buffer for encoded data
     *                 null value means processor reset, noop for this decoder
     * @param dataoutSize the size of the buffer encoded data
     * @param bits  the data to decode
     * @param nDatabits  the number of bits to decode
     * @return returns the number of bytes decoded (==1)
     */
    int decode(byte[] dataoutP, int dataoutSize, long bits, int nDatabits);
}
