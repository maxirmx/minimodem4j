/**
 * minimodem4j
 * BitOpes.java
 * Created from databits.h @ https://github.com/kamalmostafa/minimodem
 */

package minimodem.databits;
/**
 *
 */

public class BitOps {
    /**
     * Reverses the ordering of the bits on a long integer
     * @param value integer to reverse
     * @param bits number of meaningful bits
     * @return reversed value
     */
    public static long bitReverse(long value, int bits) {
        long out = 0;
        while(bits-- != 0) {
            out = out << 1 | value & 1;
            value >>>= 1;
        }
        return out;
    }

     /**
     * Gets "bits" bits from "value" starting "offset" bits from the start
     * @param value original value
     * @param offset offset from the start
     * @param bits number of bits to take
     * @return
     */
    public static long bitWindow(long value, int offset, int bits) {
        long mask = (1l << bits) - 1;
        if(mask == 0) {        /* handle bits==64 */
            return value >>> offset;
        }
        value = value >>> offset & mask;
        return value;
    }
}

