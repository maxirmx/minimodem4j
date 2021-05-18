package minimodem.databits;

import org.junit.jupiter.api.Test;

import static minimodem.databits.BitOps.bitReverse;
import static minimodem.databits.BitOps.bitWindow;

public class BitOpsTest {
    @Test
    public void BitReverseTest() {
        long res = bitReverse(0x0F, 8);
        assert(res == 0xF0);

        res = bitReverse(0x0F, 32);
        assert (Long.compareUnsigned(res,0xF0000000l) == 0);

        res = bitReverse(0x0E, 64);
        assert (Long.compareUnsigned(res,0x7000000000000000l) == 0);
    }

    @Test
    public void BitWindowTest() {
        long res = bitWindow(0xABCDF0l, 4, 4);
        assert (Long.compareUnsigned(res,0xF)==0);

        res = bitWindow(0xFFED_CBA9_8765_4321l,0,64);
        assert(Long.compareUnsigned(res,0xFFED_CBA9_8765_4321l)==0);
    }
}
