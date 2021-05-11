/**
 * DatabitsCallerId.java
 * Created from databits_callerid.c, databits.h @ https://github.com/kamalmostafa/minimodem
 * Decoder !!! ONLY !!!
 * This is high-level semantic decoder, so it first builds a string and then converts it to byte array
 */

package minimodem.databits;

import java.nio.charset.StandardCharsets;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class CallerId implements IEncodeDecode {
    private static final Logger fLogger = LogManager.getFormatterLogger("DatabitsCallerId");

    private final static int _MSG_MDMF = 0x80;
    private final static int _MSG_SDMF = 0x04;

    private final static int _DATA_DATETIME = 0x01;
    private final static int _DATA_PHONE    = 0x02;
    private final static int _DATA_PHONE_NA = 0x04;
    private final static int _DATA_NAME     = 0x07;
    private final static int _DATA_NAME_NA  = 0x08;

    // Datatype names have been adjusted to match original "%-6s " sprintf format specification
    private final static String[] datatypeNames = {
            "unkn0: ",
            "Time:  ",
            "Phone: ",
            "unkn3: ",
            "Phone: ",
            "unkn5: ",
            "unkn6: ",
            "Name:  ",
            "Name:  "
    };

    private int msgType     = 0;
    private int nData       = 0;
    private byte[] buffer   = new byte[256];

    private String nbToStr (int p, int n) {
        String rs = Byte.toString(buffer[p]);
        for (int i=1; i<n; i++) {
            rs += (char)buffer[p+i];
        }
        return rs;
    }

    private String decodeMdmfCallerid() {
        String rs = "";
        int cidI = 0;
        int m = 1;
        int cidMsglen = Byte.toUnsignedInt(buffer[m++]);

        while (cidI < cidMsglen) {
            int cidDatatype = Byte.toUnsignedInt(buffer[m++]);
            if (cidDatatype > _DATA_NAME_NA) {
                // Bad datastream -- print something here
                fLogger.error("Invalid datatype [%d] decoded.", cidDatatype);
                return "";
            }

            int cidDatalen = Byte.toUnsignedInt(buffer[m++]);
            if (m + cidDatalen + 2 > buffer.length) {
                // FIXME: bad datastream -- print something here
                fLogger.error("Data length too big: m=%d, cidDatalen=%d, buffer.length=%d", m, cidDatalen, buffer.length);
                return "";
            }


            // From dataout_n += sprintf(dataout_p+dataout_n, "%-6s ",  cid_datatype_names[cid_datatype]);
            rs += datatypeNames[cidDatatype];

            switch (cidDatatype) {
                case _DATA_DATETIME:
            // From dataout_n += sprintf(dataout_p+dataout_n, "%.2s/%.2s %.2s:%.2s\n", m+0, m+2, m+4, m+6);
                    rs += nbToStr(m,2) + "/" + nbToStr(m+2,2) + nbToStr(m+4,2) + ":" + nbToStr(m+6,2) + "\n";
                    break;
                case _DATA_PHONE:
                    if (cidDatalen == 10) {
            // From dataout_n += sprintf(dataout_p+dataout_n, "%.3s-%.3s-%.4s\n", m+0, m+3, m+6);
                        rs += nbToStr(m,3) + "-" + nbToStr(m+3,3) + nbToStr(m+6,4) + "\n";
                        break;
                    } else {
                        // fallthrough
                        fLogger.warn("Skipping cidDatatype==DATA_PHONE with cidDatalen==%d", cidDatalen);
                    }
                case _DATA_NAME:
                    rs += nbToStr(m,cidDatalen);
                    break;
                case _DATA_PHONE_NA:
                case _DATA_NAME_NA:
                    if (cidDatalen == 1 && buffer[m] == 'O') {
                        rs += "[N/A]";
                    } else if (cidDatalen == 1 && buffer[m] == 'P') {
                        rs += "[blocked]";
                    } else {
                        // fallthrough
                        fLogger.warn("Skipping cidDatatype==DATA_PHONE_NA/DATA_NAME_NA with cidDatalen==%d and buffer[m]==%x", cidDatalen, buffer[m]);
                    }
                    break;
                default:
                    fLogger.error("Invalid datatype [%d] in processing.", cidDatatype);
                    // FIXME: warning here?
                    break;
            }

            m += cidDatalen;
            cidI += cidDatalen + 2;
        }

        return rs;
    }

    private String decodeSdmfCallerid() {
        String rs = "";
        int m = 1;
        int cidMsglen = Byte.toUnsignedInt(buffer[m++]);

        rs += datatypeNames[_DATA_DATETIME];
        rs += nbToStr(m,2) + "/" + nbToStr(m+2,2) + nbToStr(m+4,2) + ":" + nbToStr(m+6,2) + "\n";
        m += 8;

        rs += datatypeNames[_DATA_PHONE];
        int cidDatalen = cidMsglen - 8;
        if(cidDatalen == 10) {
            rs += nbToStr(m,3) + "-" + nbToStr(m+3,3) + nbToStr(m+6,4) + "\n";
        } else {
            rs += nbToStr(m, cidDatalen) + "\n";
        }

        return rs;
    }

    private int decodeCidReset() {
        msgType = 0;
        nData = 0;
        return 0;
    }

    public int decode(byte[] dataoutP, int dataoutSize, long bits, int nDatabits) {
        if (dataoutP == null) { return decodeCidReset(); } /* databits processor reset */

        if (msgType == 0) {
            if (bits == _MSG_MDMF)      { msgType = _MSG_MDMF; }
            else if (bits == _MSG_SDMF) { msgType = _MSG_SDMF; }
            else                        { return 0;            }
            buffer[nData++] = (byte) bits;
            return 0;
        }

        if (nData > buffer.length) {
            // FIXME? buffer overflow; do what here?
            fLogger.error("Buffer overflow");
            return decodeCidReset();
        }

        buffer[nData++] = (byte) bits;

        // Collect input bytes until we've collected as many as the message
        // length byte says there will be, plus two (the message type byte
        // and the checksum byte)
        long cidMsglen = Byte.toUnsignedLong(buffer[1]);
        if (Long.compareUnsigned(nData, cidMsglen + 2) < 0) {
            return 0;
        }
        // Now we have a whole CID message in cid_buf[] -- decode it

        // FIXME: check the checksum

        String rs = "CALLER-ID\n";

        if(msgType == _MSG_MDMF) {  rs += decodeMdmfCallerid(); }
        else                     {  rs += decodeSdmfCallerid(); }

        // All done; reset for the next one
        decodeCidReset();

        rs.getBytes(StandardCharsets.UTF_8);
        System.arraycopy(rs.getBytes(StandardCharsets.UTF_8), 0, dataoutP, 0, rs.length());

        return rs.length();
    }

    public int encode(int[] databitsOutp, byte charOut) {
        fLogger.error("A call to encode which is not implemented for DatabitsCallerId");
        return 0;
    }

}
