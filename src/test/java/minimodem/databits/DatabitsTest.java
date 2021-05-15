package minimodem.databits;

import org.junit.jupiter.api.Test;


/**
 * Unit tests for minimodem.databits
 */

public class DatabitsTest {
    @Test
    public void Ascii8Test(){
        IEncodeDecode db = new DataBitsAscii8();
        int[] ed = new int[2];
        assert(db.encode(ed, (byte) 'a')==1);
        assert(ed[0]==0x61);

        byte[] dd = new byte[2];
        assert(db.decode(dd,2,ed[0],8)==1);
        assert(dd[0]=='a');

        assert(db.decode(null, 2, ed[0],8)==0);
    }

    @Test
    public void BinaryTest(){
        IEncodeDecode db = new DataBitsBinary();
        int[] ed = new int[2];
        assert(db.encode(ed, (byte) 'a')==0);   // Not implemented

        byte[] dd = new byte[9];
        assert(db.decode(dd,10,0b10110010,8)==9);
        String s = new String(dd);
        assert(s.equals("01001101\n"));         // Inverted ?

        assert(db.decode(null, 2, ed[0],8)==0);

        assert(db.decode(dd,3,0b10110010,8)==0);
    }

}
