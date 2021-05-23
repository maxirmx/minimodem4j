/*
 * minimodem4j
 * CallerId.java
 * Created from databits_callerid.c, databits.h @ https://github.com/kamalmostafa/minimodem
 */

package minimodem.databits;

import java.nio.charset.StandardCharsets;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * Caller-ID (USA SDMF/MDMF) databits decoder
 * Reference: http://melabs.com/resources/callerid.htm
 */

public class DataBitsCallerId implements IEncodeDecode {
    private static final Logger fLogger = LogManager.getFormatterLogger(DataBitsCallerId.class);

    private final static int MSG_MDMF = 0x80;
    private final static int MSG_SDMF = 0x04;

    private final static int DATA_DATETIME = 0x01;
    private final static int DATA_PHONE = 0x02;
    private final static int DATA_PHONE_NA = 0x04;
    private final static int DATA_NAME = 0x07;
    private final static int DATA_NAME_NA = 0x08;

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
    private final byte[] buffer   = new byte[256];

    /**
     * encode  -- placeholder only
     * @param databitsOutp  the buffer for encoded data
     * @param charOut  a byte to encode
     * @return the number of data words stuffed into databitsOutp  (Always 0)
     */
    public int encode(int[] databitsOutp, byte charOut) {
        fLogger.error("A call to encode which is not implemented for " + DataBitsCallerId.class);
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
        if (dataoutP == null) { /* databits processor reset */
            return decodeCidReset();
        }

        if (msgType == 0) {
            if (bits == MSG_MDMF)      { msgType = MSG_MDMF; }
            else if (bits == MSG_SDMF) { msgType = MSG_SDMF; }
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
        int cidMsglen = Byte.toUnsignedInt(buffer[1]);
        if (nData < cidMsglen + 2) {
            return 0;
        }
        // Now we have a whole CID message in cid_buf[] -- decode it

        // FIXME: check the checksum

        String rs = "CALLER-ID\n";

        if(msgType == MSG_MDMF) {  rs += decodeMdmfCallerid(); }
        else                     {  rs += decodeSdmfCallerid(); }

        // All done; reset for the next one
        decodeCidReset();

        System.arraycopy(rs.getBytes(StandardCharsets.UTF_8), 0, dataoutP, 0, rs.length());
        return rs.length();
    }

    /**
     * Converts given number of bytes from the current buffer position to string
     * @param p - buffer position
     * @param n - number of bytes to convert
     * @return string
     */
    private String nbToStr (int p, int n) {
        StringBuilder rs = new StringBuilder();
        for (int i=0; i<n; i++) {
            rs.append((char) buffer[p+i]);
        }
        return rs.toString();
    }

    /**
     * Decodes multi-data
     * @return  Caller id string
     */
    private String decodeMdmfCallerid() {
        StringBuilder rs = new StringBuilder();
        int cidI = 0;
        int m = 1;
        int cidMsglen = Byte.toUnsignedInt(buffer[m++]);

        while (cidI < cidMsglen) {
            int cidDatatype = Byte.toUnsignedInt(buffer[m++]);
            if (cidDatatype > DATA_NAME_NA) {
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
            rs.append(datatypeNames[cidDatatype]);

            switch (cidDatatype) {
                case DATA_DATETIME:
            // From dataout_n += sprintf(dataout_p+dataout_n, "%.2s/%.2s %.2s:%.2s\n", m+0, m+2, m+4, m+6);
                    rs.append(nbToStr(m, 2)).append("/").append(nbToStr(m + 2, 2)).append(" ").
                            append(nbToStr(m + 4, 2)).append(":").append(nbToStr(m + 6, 2)).append("\n");
                    break;
                case DATA_PHONE:
                    if (cidDatalen == 10) {
            // From dataout_n += sprintf(dataout_p+dataout_n, "%.3s-%.3s-%.4s\n", m+0, m+3, m+6);
                        rs.append(nbToStr(m, 3)).append("-").append(nbToStr(m + 3, 3)).append("-").append(nbToStr(m + 6, 4)).append("\n");
                        break;
                    } else {
                        // fallthrough
                        fLogger.warn("Skipping cidDatatype==DATA_PHONE with cidDatalen==%d", cidDatalen);
                    }
                case DATA_NAME:
                    rs.append(nbToStr(m, cidDatalen)).append("\n");;
                    break;
                case DATA_PHONE_NA:
                case DATA_NAME_NA:
                    if (cidDatalen == 1 && buffer[m] == 'O') {
                        rs.append("[N/A]").append("\n");;
                    } else if (cidDatalen == 1 && buffer[m] == 'P') {
                        rs.append("[blocked]").append("\n");;
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

        return rs.toString();
    }

    /**
     * Decodes single-data
     * @return  Caller id string
     */
    private String decodeSdmfCallerid() {
        StringBuilder rs = new StringBuilder();
        int m = 1;
        int cidMsglen = Byte.toUnsignedInt(buffer[m++]);

        rs.append(datatypeNames[DATA_DATETIME]).append(nbToStr(m,2)).append("/").append(nbToStr(m+2,2)).append(" ").
                append(nbToStr(m+4,2)).append(":").append(nbToStr(m+6,2) ).append("\n");
        m += 8;

        rs.append(datatypeNames[DATA_PHONE]);
        int cidDatalen = cidMsglen - 8;
        if(cidDatalen == 10) {
            rs.append(nbToStr(m,3)).append("-").append(nbToStr(m+3,3)).append("-").append(nbToStr(m+6,4)).append("\n");
        } else {
            rs.append(nbToStr(m, cidDatalen)).append("\n");
        }

        return rs.toString();
    }

    /**
     * CallerId decoder reset
     * @return 0
     */
    private int decodeCidReset() {
        msgType = 0;
        nData = 0;
        return 0;
    }

}
