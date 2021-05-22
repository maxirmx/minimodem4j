/**
 * minimodem4j
 * SimpleAudio.java
 * Transmitter implementation
 * Created from minimodem.c @ https://github.com/kamalmostafa/minimodem
 */

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
    public int txLeaderBitsLen = 2;
    public  int txTrailerBitsLen = 2;
    private int txBitNsamples;

    /**
     * Transmitter
     * @param saOut     Output device
     * @param saTone    Tone generator (modulator)
     * @param modem     Minimodem instance to inherit configuration from
     */
    public Transmitter(SimpleAudio saOut,
                       SaToneGenerator saTone,
                       Minimodem modem) {

        txSaOut = saOut;
        txToneGenerator = saTone;
        txPrintEot = modem.isTxPrintEot();
        bfskDataRate = modem.getBfskDataRate();
        bfskMarkF = modem.getBfskMarkF();
        bfskSpaceF = modem.getBfskSpaceF();
        nDataBits = modem.getBfskNDataBits();
        bfskNStartBits = modem.getBfskNStartBits();
        bfskNStopBits = modem.getBfskNStopBits();
        invertStartStop = modem.isInvertStartStop();
        bfskMsbFirst = modem.isBfskMsbFirst();
        bfskDoTxSyncBytes = modem.getBfskDoTxSyncBytes();
        bfskSyncByte = modem.getBfskSyncByte();
    }

    /**
     * Transmits (bytes from) stdin using IEncodeDecode interface provided
     * @param encoder
     */
    public void fskTransmitStdin(IEncodeDecode encoder)
    {
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
                             fskTransmitFrame(bfskSyncByte);
                         }
                     }
                     /* emit data bits */
                     for(j = 0; j<nwords; j++) {
                         fskTransmitFrame(bits[j]);
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

    /**
     * Rudimentary BFSK transmitter
     * @param bits      data to transmit
     */
    private void fskTransmitFrame(int bits) {
        int i;
        if(bfskNStartBits > 0) {
            txToneGenerator.Tone(txSaOut,
                    invertStartStop ? bfskMarkF : bfskSpaceF,
                    (int) (txBitNsamples * bfskNStartBits));
        }
        for(i = 0; i<nDataBits; i++) {
            // data
            int bit;
            if(bfskMsbFirst) {
                bit = bits >>> nDataBits - i - 1 & 1;
            } else {
                bit = bits >>> i & 1;
            }

            float toneFreq = bit == 1 ? bfskMarkF : bfskSpaceF;
            txToneGenerator.Tone(txSaOut, toneFreq, txBitNsamples);
        }
        if(bfskNStopBits > 0) {
            txToneGenerator.Tone(txSaOut,
                    invertStartStop ? bfskSpaceF : bfskMarkF,
                    (int) (txBitNsamples * bfskNStopBits)); // stop
        }
    }

    /**
     * Stop Transmit Handler
     * Emits trailing bits if specified
     */
    private void txStopTransmitHandler() {
        for(int j = 0; j < txTrailerBitsLen; j++) {
            txToneGenerator.Tone(txSaOut, bfskMarkF, txBitNsamples);
        }
        txTransmitting = 0;
        if(txPrintEot) {
            fLogger.info("### EOT");
        }
    }
}
