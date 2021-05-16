package minimodem.databits;

import org.junit.jupiter.api.Test;

import static minimodem.databits.DataBitsBaudot.*;


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

    @Test
    public void BaudotTest() {
        IEncodeDecode db = new DataBitsBaudot(false);
        int[] data = new int[2];
        assert(db.encode(data, (byte)'A')==2);
        assert(data[1]==0x03);
        assert(data[0]==LTRS);

        assert(db.encode(data, (byte)';')==2);
        assert(data[1]==0x1e);
        assert(data[0]==FIGS);

        assert(db.encode(data, (byte)0x12)==0);

        byte[] dd = new byte[1];
        assert(db.decode(null,0,0,0)==0);
        assert(db.decode(dd,1,LTRS,8)==0);
        assert(db.decode(dd,1,0x03,8)==1);
        assert(dd[0]==(byte)'A');
        assert(db.decode(dd,1,FIGS,8)==0);
        assert(db.decode(dd,1,0x1e,8)==1);
        assert(dd[0]==(byte)';');
    }
    @Test
    public void USoSTest() {
        IEncodeDecode db = new DataBitsBaudot(true);
        byte[] dd = new byte[1];
        assert(db.decode(dd,1,FIGS,8)==0);
        assert(db.decode(dd,1,0x1e,8)==1);
        assert(dd[0]==(byte)';');
        assert(db.decode(dd,1,SPACE,8)==1);
        assert(dd[0]==(byte)' ');
        assert(db.decode(dd,1,0x19,8)==1);
        assert(dd[0]==(byte)'B');
        db = new DataBitsBaudot(false);
        assert(db.decode(dd,1,FIGS,8)==0);
        assert(db.decode(dd,1,0x1e,8)==1);
        assert(dd[0]==(byte)';');
        assert(db.decode(dd,1,SPACE,8)==1);
        assert(dd[0]==(byte)' ');
        assert(db.decode(dd,1,0x19,8)==1);
        assert(dd[0]==(byte)'?');
    }
}
