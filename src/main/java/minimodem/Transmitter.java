package minimodem;

import minimodem.databits.IEncodeDecode;
import minimodem.simpleaudio.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public class Transmitter {
    private static final Logger fLogger = LogManager.getFormatterLogger("Transmitter");

    private final SimpleAudio txSaOut;
    private final SaToneGenerator txToneGenerator;
    private final boolean txPrintEot;
    private final float bfskDataRate;
    private final float bfskMarkF;
    private final float bfskSpaceF;
    private final int nDataBits;
    private final float bfskNStartBits;
    private final float bfskNStopBits;
    private final boolean invertStartStop;
    private final boolean bfskMsbFirst;
    private final int bfskDoTxSyncBytes;
    private final int bfskSyncByte;

    private int txTransmitting = 0;
    public static int txLeaderBitsLen = 2;
    public static int txTrailerBitsLen = 2;
    public static float txBfskMarkF;
    private int txBitNsamples;


    public Transmitter(SimpleAudio saOut,
                       SaToneGenerator saTone,
                       float dataRate,
                       float markF,
                       float spaceF,
                       int nDBits,
                       float nStartBits,
                       float nStopBits,
                       boolean iStartStop,
                       boolean msbFirst,
                       int doTxSyncBytes,
                       int syncByte,
                       boolean printEot
                       ) {
        txSaOut = saOut;
        txToneGenerator = saTone;
        txPrintEot = printEot;
        bfskDataRate = dataRate;
        bfskMarkF = markF;
        bfskSpaceF = spaceF;
        nDataBits = nDBits;
        bfskNStartBits = nStartBits;
        bfskNStopBits = nStopBits;
        invertStartStop = iStartStop;
        bfskMsbFirst = msbFirst;
        bfskDoTxSyncBytes = doTxSyncBytes;
        bfskSyncByte = syncByte;
    }

    /**
     * rudimentary BFSK transmitter
     */
    private void fskTransmitFrame(int bits, int nDataBits, int bitNsamples, float bfskMarkF, float bfskSpaceF,
                                  float bfskNstartbits, float bfskNstopbits, boolean invertStartStop, boolean bfskMsbFirst) {
        int i;
        if(bfskNstartbits > 0) {
            txToneGenerator.Tone(txSaOut, invertStartStop ? bfskMarkF : bfskSpaceF, (int) (bitNsamples * bfskNstartbits));
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
            txToneGenerator.Tone(txSaOut, toneFreq, bitNsamples);
        }
        if(bfskNstopbits > 0) {
            txToneGenerator.Tone(txSaOut, invertStartStop ? bfskSpaceF : bfskMarkF, (int) (bitNsamples * bfskNstopbits)); // stop
        }
    }

    public void fskTransmitStdin(IEncodeDecode encoder)
    {
        txBfskMarkF = bfskMarkF;
        txBitNsamples = (int) (txSaOut.getRate() / bfskDataRate + 0.5f);

        boolean endOfFile = false;
        while (!endOfFile) {
            int nextByte;
            try {
                 nextByte = System.in.read();
                 if (nextByte != -1) {
                     int[] bits = new int[2];
                     int j;
                     int nwords = encoder.encode(bits,(byte)(nextByte&0xFF));
                     if(txTransmitting == 0) {
                         txTransmitting = 1;
                         /* emit leader tone (mark) */
                         for (j = 0; j < txLeaderBitsLen; j++) {
                             txToneGenerator.Tone(txSaOut, invertStartStop ? bfskSpaceF : bfskMarkF, txBitNsamples);
                         }
                     }
                     if(txTransmitting < 2) {
                         txTransmitting = 2;
                         /* emit "preamble" of sync bytes */
                         for (j = 0; j < bfskDoTxSyncBytes; j++) {
                             fskTransmitFrame(bfskSyncByte, nDataBits, txBitNsamples, bfskMarkF, bfskSpaceF,
                                     bfskNStartBits, bfskNStopBits, invertStartStop, bfskMsbFirst);
                         }
                     }
                     /* emit data bits */
                     for(j = 0; j<nwords; j++) {
                         fskTransmitFrame(bits[j], nDataBits, txBitNsamples, bfskMarkF, bfskSpaceF, bfskNStartBits,
                                 bfskNStopBits, invertStartStop, bfskMsbFirst);
                     }
                 }
                 else {
                     endOfFile = true;
                 }
            } catch (IOException e) {
                endOfFile = true;
            }
        }
        if (txTransmitting !=0) {
            txStopTransmitHandler();
        }
    }

    private void txStopTransmitHandler() {
        for(int j = 0; j < txTrailerBitsLen; j++) {
            txToneGenerator.Tone(txSaOut, txBfskMarkF, txBitNsamples);
        }

        txTransmitting = 0;
        if(txPrintEot) {
            fLogger.info("### EOT");
        }
    }
}
