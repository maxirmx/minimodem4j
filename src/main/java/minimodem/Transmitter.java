package minimodem;

import minimodem.databits.IEncodeDecode;
import minimodem.simpleaudio.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Transmitter {
    private static final Logger fLogger = LogManager.getFormatterLogger("Transmitter");

    private SimpleAudio txSaOut;                // Output stream
    private static int txFlushNsamples = 0;

    public int txTransmitting = 0;
    public static boolean txPrintEot = false;
    public static int txLeaderBitsLen = 2;
    public static int txTrailerBitsLen = 2;
    public static float txBfskMarkF;
    public static int txBitNsamples_U;

    private SimpleToneGenerator toneGenerator = new SimpleToneGenerator();

    public Transmitter(SimpleAudio saOut) {
        txSaOut = saOut;
    }

    /**
     * rudimentary BFSK transmitter
     */
    private void fskTransmitFrame(int bits, int nDataBits, int bitNsamples, float bfskMarkF, float bfskSpaceF, float bfskNstartbits, float bfskNstopbits, int invertStartStop, int bfskMsbFirst) {
        int i;
        if(bfskNstartbits > 0) {
            toneGenerator.Tone(txSaOut, invertStartStop != 0 ? bfskMarkF : bfskSpaceF, (int) (bitNsamples * bfskNstartbits));
        }
        for(i = 0; i<nDataBits; i++) {
            // data
            int bit_U;
            if(bfskMsbFirst != 0) {
                bit_U = bits >>> nDataBits - i - 1 & 1;
            } else {
                bit_U = bits >>> i & 1;
            }

            float toneFreq = bit_U == 1 ? bfskMarkF : bfskSpaceF;
            toneGenerator.Tone(txSaOut, toneFreq, bitNsamples);
        }
        if(bfskNstopbits > 0) {
            toneGenerator.Tone(txSaOut, invertStartStop != 0 ? bfskSpaceF : bfskMarkF, (int) (bitNsamples * bfskNstopbits)); // stop
        }

    }

    private void fskTransmitStdin(boolean txInteractive, float dataRate, float bfskMarkF, float bfskSpaceF,
                                         int nDataBits, float bfskNstartbits, float bfskNstopbits, int invertStartStop, int bfskMsbFirst,
                                         int bfskDoTxSyncBytes_U, int bfskSyncByte_U, IEncodeDecode encode, int txcarrier) {
        txBfskMarkF = bfskMarkF;
        int bitNsamples = (int) (txSaOut.getRate() / dataRate + 0.5f);
        if ( txInteractive )
            txFlushNsamples = txSaOut.getRate()/2; // 0.5 sec of zero samples to flush
        else
            txFlushNsamples = 0;


        boolean idle = false;
        if(!idle) {
            int nwords_U;
            int[] bits_U = new int[2];

            if(txTransmitting == 0) {
                txTransmitting = 1;
                /* emit leader tone (mark) */
                for(int j_U = 0; Integer.compareUnsigned(j_U, txLeaderBitsLen) < 0; j_U++) {
                    toneGenerator.Tone(txSaOut, invertStartStop != 0 ? bfskSpaceF : bfskMarkF, bitNsamples);
                }
            }

            if(txTransmitting < 2) {
                txTransmitting = 2;
                /* emit "preamble" of sync bytes */
                for(int j_U = 0; Integer.compareUnsigned(j_U, bfskDoTxSyncBytes_U) < 0; j_U++) {
                    fskTransmitFrame(bfskSyncByte_U, nDataBits, bitNsamples, bfskMarkF, bfskSpaceF, bfskNstartbits, bfskNstopbits, invertStartStop, 0);
                }

            }

            /* emit data bits */
//            for(int j_U = 0; Integer.compareUnsigned(j_U, nwords_U) < 0; j_U++) {
//                fskTransmitFrame(bits_U[j_U], nDataBits, bitNsamples, bfskMarkF, bfskSpaceF, bfskNstartbits, bfskNstopbits, invertStartStop, bfskMsbFirst);
//            }
//        } else {
        txTransmitting = 1;
        /* emit idle tone (mark) */
   //         toneGenerator.Tone(txSaOut, invertStartStop != 0 ? bfskSpaceF : bfskMarkF, Long.divideUnsigned(Integer.toUnsignedLong(idleCarrierUsec) * sampleRate, 1_000_000));
    }
}

    public void txStopTransmitSighandler(int sig) {
        for(int j = 0; j < txTrailerBitsLen; j++) {
            toneGenerator.Tone(txSaOut, txBfskMarkF, txBitNsamples_U);
        }

        if(txFlushNsamples != 0) {
            toneGenerator.Tone(txSaOut, 0, txFlushNsamples);
        }

        txTransmitting = 0;
        if(txPrintEot) {
            fLogger.info("### EOT");
        }
    }
}
