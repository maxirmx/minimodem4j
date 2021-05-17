package minimodem.databits;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;


/**
 * Unit tests for minimodem.databits
 */

public class CallerIdTest {
    @Test
    public void CallerIdSDMFTest() throws IOException {
        IEncodeDecode cid = new DataBitsCallerId();

        File bytesFile = new File(this.getClass().getResource("/cidTests/testdata-callerid-sdmf.bytes").getFile());
        byte[] msg = Files.readAllBytes(bytesFile.toPath());
        File textFile = new File(this.getClass().getResource("/cidTests/testdata-callerid-sdmf.txt").getFile());
        String decoded = Files.readString(textFile.toPath());
        byte[] msgDecoded = new byte[256];

        for (byte b : msg) {
            cid.decode(msgDecoded, msgDecoded.length, b, 8 );
        }
        assert(decoded.trim().equals(new String(msgDecoded).trim()));

        assert(cid.encode(new int[2], (byte) 'a')==0);   // Not implemented
        assert(cid.decode(null, 2, msg[0],8)==0);

    }

    @Test
    public void CallerIdMDMFTest() throws IOException {
        IEncodeDecode cid = new DataBitsCallerId();
        File bytesFile = new File(this.getClass().getResource("/cidTests/testdata-callerid-mdmf.bytes").getFile());
        byte[] msg = Files.readAllBytes(bytesFile.toPath());
        File textFile = new File(this.getClass().getResource("/cidTests/testdata-callerid-mdmf.txt").getFile());
        String decoded = Files.readString(textFile.toPath());
        byte[] msgDecoded = new byte[256];

        for (byte b : msg) {
            cid.decode(msgDecoded, msgDecoded.length, Byte.toUnsignedLong(b), 8 );
        }

        assert(decoded.trim().equals(new String(msgDecoded).trim()));
    }

}
