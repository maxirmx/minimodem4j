package minimodem;

import minimodem.databits.IEncodeDecode;
import minimodem.simpleaudio.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

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

    private SaToneGenerator toneGenerator;

    public Transmitter(SimpleAudio saOut) {
        txSaOut = saOut;
        toneGenerator = new SaToneGenerator();
    }

    /**
     * rudimentary BFSK transmitter
     */
    private void fskTransmitFrame(int bits, int nDataBits, int bitNsamples, float bfskMarkF, float bfskSpaceF,
                                  float bfskNstartbits, float bfskNstopbits, boolean invertStartStop, boolean bfskMsbFirst) {
        int i;
        if(bfskNstartbits > 0) {
            toneGenerator.Tone(txSaOut, invertStartStop ? bfskMarkF : bfskSpaceF, (int) (bitNsamples * bfskNstartbits));
        }
        for(i = 0; i<nDataBits; i++) {
            // data
            int bit_U;
            if(bfskMsbFirst) {
                bit_U = bits >>> nDataBits - i - 1 & 1;
            } else {
                bit_U = bits >>> i & 1;
            }

            float toneFreq = bit_U == 1 ? bfskMarkF : bfskSpaceF;
            toneGenerator.Tone(txSaOut, toneFreq, bitNsamples);
        }
        if(bfskNstopbits > 0) {
            toneGenerator.Tone(txSaOut, invertStartStop ? bfskSpaceF : bfskMarkF, (int) (bitNsamples * bfskNstopbits)); // stop
        }
    }

    public void fskTransmitStdin(boolean txInteractive,
                                 float dataRate,
                                 float bfskMarkF,
                                 float bfskSpaceF,
                                 int nDataBits,
                                 float bfskNstartbits,
                                 float bfskNstopbits,
                                 boolean invertStartStop,
                                 boolean bfskMsbFirst,
                                 int bfskDoTxSyncBytes,
                                 int bfskSyncByte,
                                 IEncodeDecode encode,
                                 boolean txcarrier)
    {
        txBfskMarkF = bfskMarkF;
        int bitNsamples = (int) (txSaOut.getRate() / dataRate + 0.5f);
        if ( txInteractive )
            txFlushNsamples = txSaOut.getRate()/2; // 0.5 sec of zero samples to flush
        else
            txFlushNsamples = 0;


        boolean endOfFile = false;
        while (!endOfFile)
        {   int nextByte;
            try {
                 nextByte = System.in.read();
                 if (nextByte != -1) {
                     int[] bits = new int[2];
                     int j;
                     int nwords = encode.encode(bits,(byte)(nextByte&0xFF));
                     if(txTransmitting == 0) {
                         txTransmitting = 1;
                         /* emit leader tone (mark) */
                         for(j = 0; j<txLeaderBitsLen; j++) {
                             toneGenerator.Tone(txSaOut, invertStartStop ? bfskSpaceF : bfskMarkF, bitNsamples);
                         }
                         if(txTransmitting < 2) {
                             txTransmitting = 2;
                             /* emit "preamble" of sync bytes */
                             for(j = 0; j<bfskDoTxSyncBytes; j++) {
                                 fskTransmitFrame(bfskSyncByte, nDataBits, bitNsamples, bfskMarkF, bfskSpaceF,
                                                  bfskNstartbits, bfskNstopbits, invertStartStop, bfskMsbFirst);
                             }
                             /* emit data bits */
                             for(j = 0; j<nwords; j++) {
                                 fskTransmitFrame(bits[j], nDataBits, bitNsamples, bfskMarkF, bfskSpaceF, bfskNstartbits,
                                         bfskNstopbits, invertStartStop, bfskMsbFirst);
                             }

                         }
                     }
                 }
                 else {
                     endOfFile = true;
                 }
            } catch (IOException e) {
                endOfFile = true;
            }
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
