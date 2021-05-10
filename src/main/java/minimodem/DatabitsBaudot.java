/**
  * DatabitsBaudot.java
  * Created from databits_baudot.c, databits.h @ https://github.com/kamalmostafa/minimodem
  * Baudot 5-bit data databits decoder/encoder
  */

package minimodem;

public class DatabitsBaudot extends Baudot implements IEncodeDecode {
    public int decode(byte[] dataoutP, int dataoutSize, long bits, int nDatabits) {
        if(dataoutP == null) {
            // databits processor reset: reset Baudot state
            reset();
            return 0;
        }
        bits &= 0x1F;
        return decode(dataoutP, (byte) bits);
    }
}
