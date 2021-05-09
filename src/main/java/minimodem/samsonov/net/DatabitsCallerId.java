package minimodem.samsonov.net;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DatabitsCallerId implements EncoderDecoder{
    private static final Logger fLogger = LogManager.getFormatterLogger("DatabitsCallerId");

    private final static int _MSG_MDMF = 0x80;
    private final static int _MSG_SDMF = 0x04;

    private final static int _DATA_DATETIME = 0x01;
    private final static int _DATA_PHONE    = 0x02;
    private final static int _DATA_PHONE_NA = 0x04;
    private final static int _DATA_NAME     = 0x07;
    private final static int _DATA_NAME_NA  = 0x08;

    private final static String[] datatypeNames = {
            "unknown0:",
            "Time:",
            "Phone:",
            "unknown3:",
            "Phone:",
            "unknown5:",
            "unknown6:",
            "Name:",
            "Name:"
    };

    private int msgType     = 0;
    private int nData       = 0;
    private byte[] buffer   = new byte[256];


    private int decodeMdmfCallerid_U(byte[] dataoutP, int dataoutSize_U) {
        int dataoutN_U = 0;
        int cidI_U = 0;
        int cidMsglen_U = Byte.toUnsignedInt(buffer.get(1));

        String8 m_U = buffer.shift(2);
        while (Integer.compareUnsigned(cidI_U, cidMsglen_U) < 0) {
            int cidDatatype_U = Byte.toUnsignedInt((m_U = nnc(m_U).shift(1)).get(-1));
            if (Integer.compareUnsigned(cidDatatype_U, _DATA_NAME_NA) > 0) {
                // FIXME: bad datastream -- print something here
                return 0;
            }

            int cidDatalen_U = Byte.toUnsignedInt((m_U = nnc(m_U).shift(1)).get(-1));
            if (dataAddress(nnc(m_U).shift(2 + cidDatalen_U)) >= dataAddress(buffer.shift(buffer.size()))) {
                // FIXME: bad datastream -- print something here
                return 0;
            }


            dataoutN_U += sprintf(nnc(dataoutP).shift(dataoutN_U), cs8("%-6s "), datatypeNames[cidDatatype_U]);

            int prlen = 0;
            byte[] prdata = null;
            switch(cidDatatype_U) {
                case _DATA_DATETIME:
                    dataoutN_U += sprintf(nnc(dataoutP).shift(dataoutN_U), cs8("%.2s/%.2s %.2s:%.2s\n"), m_U, nnc(m_U).shift(2), nnc(m_U).shift(4), nnc(m_U).shift(6));
                    break;
                case _DATA_PHONE:
                    if(cidDatalen_U == 10) {
                        dataoutN_U += sprintf(nnc(dataoutP).shift(dataoutN_U), cs8("%.3s-%.3s-%.4s\n"), m_U, nnc(m_U).shift(3), nnc(m_U).shift(6));
                        break;
                    } else {
                        // fallthrough
                    }
                case _DATA_NAME:
                    prdata = m_U;
                    prdataIndex = mIndex;
                    prlen = cidDatalen_U;
                    break;
                case _DATA_PHONE_NA:
                case _DATA_NAME_NA:
                    if(cidDatalen_U == 1 && m_U[mIndex] == 'O') {
                        prdata = "[N/A]\0".getBytes();
                        prdataIndex = 0;
                        prlen = 5;
                    } else if(cidDatalen_U == 1 && m_U[mIndex] == 'P') {
                        prdata = "[blocked]\0".getBytes();
                        prdataIndex = 0;
                        prlen = 9;
                    }
                    break;
                default:
                    // FIXME: warning here?
                    break;
            }
            if(prdata != null) {
                dataoutN_U += sprintf(nnc(dataoutP).shift(dataoutN_U), cs8("%.*s\n"), prlen, prdata);
            }

            mIndex += cidDatalen_U;
            cidI_U += cidDatalen_U + 2;
        }

        return dataoutN_U;
    }

    private int decodeSdmfCallerid_U(String8 dataoutP, int dataoutSize_U) {
        int dataoutN_U = 0;
        int cidMsglen_U = Byte.toUnsignedInt(buffer.get(1));

        String8 m_U = buffer.shift(2);

        dataoutN_U += sprintf(nnc(dataoutP).shift(dataoutN_U), cs8("%-6s "), datatypeNames[_DATA_DATETIME]);
        dataoutN_U += sprintf(nnc(dataoutP).shift(dataoutN_U), cs8("%.2s/%.2s %.2s:%.2s\n"), m_U, nnc(m_U).shift(2), nnc(m_U).shift(4), nnc(m_U).shift(6));
        m_U = nnc(m_U).shift(8);

        dataoutN_U += sprintf(nnc(dataoutP).shift(dataoutN_U), cs8("%-6s "), datatypeNames[_DATA_PHONE]);
        int cidDatalen_U = cidMsglen_U - 8;
        if(cidDatalen_U == 10) {
            dataoutN_U += sprintf(nnc(dataoutP).shift(dataoutN_U), cs8("%.3s-%.3s-%.4s\n"), m_U, nnc(m_U).shift(3), nnc(m_U).shift(6));
        } else {
            dataoutN_U += sprintf(nnc(dataoutP).shift(dataoutN_U), cs8("%.*s\n"), cidDatalen_U, m_U);
        }

        return dataoutN_U;
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

        if (Long.compareUnsigned(nData, (long) buffer.length) >= 0) {
            // FIXME? buffer overflow; do what here?
            return decodeCidReset();
        }

        buffer[nData++] = (byte) bits;

        // Collect input bytes until we've collected as many as the message
        // length byte says there will be, plus two (the message type byte
        // and the checksum byte)
        long cidMsglen_U = Byte.toUnsignedLong(buffer[1]);
        if (Long.compareUnsigned(nData, cidMsglen_U + 2) < 0) {
            return 0;
        }
        // Now we have a whole CID message in cid_buf[] -- decode it

        // FIXME: check the checksum

        int dataoutN_U = 0;

        dataoutN_U += sprintf(nnc(dataoutP).shift(dataoutN_U), cs8("CALLER-ID\n"));

        if(msgType == _MSG_MDMF) {
            dataoutN_U += decodeMdmfCallerid(nnc(dataoutP).shift(dataoutN_U), dataoutSize - dataoutN_U);
        } else {
            dataoutN_U += decodeSdmfCallerid(nnc(dataoutP).shift(dataoutN_U), dataoutSize - dataoutN_U);
        }

        // All done; reset for the next one
        decodeCidReset();

        return dataoutN_U;

    }

    public int encode(int[] databitsOutp, byte charOut) {
        fLogger.error("A call to encode which is not implemented for DatabitsCallerId");
        return 0;
    }

}
