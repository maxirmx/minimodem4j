/**
 * minimodem4j
 * SimpleAudio.java
 * Transmitter implementation
 * Created from minimodem.c @ https://github.com/kamalmostafa/minimodem
 */
package minimodem;

import minimodem.databits.IEncodeDecode;
import minimodem.fsk.Fsk;
import minimodem.simpleaudio.SimpleAudio;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import static java.lang.Math.ceil;
import static java.nio.ByteOrder.nativeOrder;
import static minimodem.databits.BitOps.bitReverse;
import static minimodem.databits.BitOps.bitWindow;

public class Receiver {
    private static final Logger fLogger = LogManager.getFormatterLogger("Receiver");
    public final static int SAMPLE_BUF_DIVISOR = 12;
    /*
     * FSK_ANALYZE_NSTEPS Try 3 frame positions across the try_max_nsamples
     * range.  Using a larger nsteps allows for more accurate tracking of
     * fast/slow signals (at decreased performance).  Note also
     * FSK_ANALYZE_NSTEPS_FINE below, which refines the frame
     * position upon first acquiring carrier, or if confidence falls.
     */
    public final static int FSK_ANALYZE_NSTEPS = 3;
    /*
     * FSK_ANALYZE_NSTEPS_FINE:
     *  Scan again, but try harder to find the best frame.
     *  Since we found a valid confidence frame in the "sloppy"
     *  fsk_find_frame() call already, we're sure to find one at
     *  least as good this time.
     */
    public final static int FSK_MAX_NOCONFIDENCE_BITS = 20;
    public final static int FSK_ANALYZE_NSTEPS_FINE = 8;

    /*
     * Fraction of nsamples_per_bit that we will "overscan"; range (0.0 .. 1.0)
     * should be != 0.0 (only the nyquist edge cases actually require this?)
     * for handling of slightly faster-than-us rates:
     *  should be >> 0.0 to allow us to lag back for faster-than-us rates
     *  should be << 1.0 or we may lag backwards over whole bits
     * for optimal analysis:
     *  should be >= 0.5 (half a bit width) or we may not find the optimal bit
     *  should be <  1.0 (a full bit width) or we may skip over whole bits
     * for encodings without start/stop bits:
     *  MUST be <= 0.5 or we may accidentally skip a bit
     */
     public final static float FRAME_OVERSCAN = 0.5f;

     private final SimpleAudio rxSaIn;
     private final int sampleRate;
     private final float bfskDataRate;
     private final int bfskNStartBits;
     private final float bfskNStopBits;
     private final int bfskNDataBits;
     private final int bfskFrameNBits;
     private final boolean invertStartStop;
     private final boolean bfskDoRxSync;
     private final int bfskSyncByte;
     private final float bfskMarkF;
     private final float bfskSpaceF;
     private final float bandWidth;
     private final float carrierAutodetectThreshold;
     private final int autodetectShift;
     private final boolean bfskInvertedFreqs;
     private final float fskConfidenceSearchLimit;
     private final float fskConfidenceThreshold;
     private final boolean bfskMsbFirst;

     private float nSamplesPerBit;
     private int expectNBits;
     protected int nSamplesOverscan;
     private int frameNSamples;
     protected byte[] expectDataString;
     private byte[] expectSyncString;
     private Fsk fskp;


   /*
    *    Sample buffer
    *        samplebufSize samples of type float  (viewed as sampleBuf)
    *                              or ByteBuffer  (viewed as sampleBufRaw)
    *        advance - current first scanning position in sampleBuf
    *        sampleNValid - remaining number of valid, non scanned float samples
    */
    private int samplebufSize;
    protected int advance;
    protected int samplesNValid;
    private ByteBuffer sampleBufRaw;
    protected FloatBuffer sampleBuf;


    /**
     *
     * @param saIn      Input device
     * @param modem     Minimodem instance to inherit configuration from
     */
     public Receiver(SimpleAudio saIn,
                     Minimodem modem) {
        rxSaIn = saIn;
        sampleRate = saIn.getRate();
        bfskDataRate = modem.getBfskDataRate();
        bfskNStartBits = modem.getBfskNStartBits();
        bfskNStopBits = modem.getBfskNStopBits();
        bfskNDataBits = modem.getBfskNDataBits();
        bfskFrameNBits = modem.getBfskFrameNBits();
        invertStartStop = modem.isInvertStartStop();
        bfskDoRxSync = modem.isBfskDoRxSync();
        bfskSyncByte = modem.getBfskSyncByte();
        bfskMarkF =  modem.getBfskMarkF();
        bfskSpaceF = modem.getBfskSpaceF();
        bandWidth = modem.getBandWidth();
        carrierAutodetectThreshold = modem.getCarrierAutodetectThreshold();
        autodetectShift = modem.getAutodetectShift();
        bfskInvertedFreqs = modem.isBfskInvertedFreqs();
        fskConfidenceSearchLimit = modem.getFskConfidenceSearchLimit();
        fskConfidenceThreshold = modem.getFskConfidenceThreshold();
        bfskMsbFirst = modem.isBfskMsbFirst();
     }

    /**
     * Configure receiver class
     *   Prepare input sample buffer
     *   Build Data/Sync patterns
     *   Create Fsk object (aka demod)
     * @param expctDataString   expected data string pattern
     */
    public void configure(byte[] expctDataString) {
        nSamplesPerBit = sampleRate / bfskDataRate;  // The input sample chunk rate
        fskp = new Fsk(sampleRate, bfskMarkF, bfskSpaceF, bandWidth);

         /*
         * Prepare the input sample buffer.  For 8-bit frames with prev/start/stop
         * we need 11 data-bits worth of samples, and we will scan through one bits
         * worth at a time, hence we need a minimum total input buffer size of 12
         * data-bits.
         */
        int nBits = 0;
        nBits += 1;                 // prev stop bit (last whole stop bit)
        nBits += bfskNStartBits;    // start bits
        nBits += bfskNDataBits;
        nBits += 1;                 // stop bit (first whole stop bit)

        samplebufSize = (int)(ceil(nSamplesPerBit) * (nBits + 1));  //  +1 goes with extra bit when scanning
        samplebufSize *= 2; // account for the half-buf filling method

        // For performance, use a larger samplebuf_size than necessary
        if(samplebufSize < sampleRate / SAMPLE_BUF_DIVISOR) {
            samplebufSize = sampleRate / SAMPLE_BUF_DIVISOR;
        }
        fLogger.debug("Creating sample buffer with samplebufSize=%d", samplebufSize);
        sampleBufRaw = ByteBuffer.allocate(samplebufSize * Float.BYTES);
        sampleBufRaw.order(nativeOrder());           // Here it shall be native order. Lsb/Msb is handled in the code.
        sampleBuf = sampleBufRaw.asFloatBuffer();

        // Ensure that we overscan at least a single sample
        nSamplesOverscan = (int)(nSamplesPerBit * FRAME_OVERSCAN + 0.5f);
        if(FRAME_OVERSCAN > 0.0f && nSamplesOverscan == 0) {
            nSamplesOverscan = 1;
        }
        fLogger.debug("FRAME_OVERSCAN=%.2f nSamplesOverscan=%d", FRAME_OVERSCAN, nSamplesOverscan);

        frameNSamples = (int)(nSamplesPerBit * bfskFrameNBits + 0.5f);

        if(expctDataString == null) {
            expectDataString = new byte[64];
            expectNBits = buildExpectBitsString(expectDataString, 0, 0);
        } else {
            expectDataString = expctDataString;
            expectNBits = expctDataString.length;
        }
        String dbgOut = byteArray2String(expectDataString);
        fLogger.debug("expectDataString = '%s' (%d)", dbgOut, dbgOut.length()-2);

        expectSyncString = expectDataString;
        if(bfskDoRxSync && bfskSyncByte >= 0) {
            expectSyncString = new byte[64];
            buildExpectBitsString(expectSyncString, 1, bfskSyncByte);
        }
        dbgOut = byteArray2String(expectDataString);
        fLogger.debug("expectSyncString = '%s' (%d)", dbgOut, dbgOut.length()-2);

    }

    public int receive(IEncodeDecode decoder, boolean quietMode, boolean outputPrintFilter, boolean rxOne) {

        int ret = 0;

        boolean carrier = false;
        float confidenceTotal = 0.0f;
        float amplitudeTotal = 0.0f;
        int nFramesDecoded = 0;
        int carrierNSamples = 0;

        int noconfidence = 0;

        advance = 0;
        samplesNValid = 0;

        int expectNSamples = (int) (nSamplesPerBit * expectNBits);
        float trackAmplitude = 0.0f;
        float peakConfidence = 0.0f;

        while ((ret = refillBuf()) > 0 || samplesNValid > 0) {
            int carrierBand = -1;
            if (carrierAutodetectThreshold > 0.0f && carrierBand < 0) {
                int i;
                float nSamplesPerScan = nSamplesPerBit;
				if (nSamplesPerScan > fskp.getFftSize()) {
                    nSamplesPerScan = fskp.getFftSize();
                }
                for (i = 0; i + nSamplesPerScan <= samplesNValid; i = (int) (i + nSamplesPerScan)) {
					carrierBand = fskp.fskDetectCarrier(sampleBuf, i, (int) nSamplesPerScan, carrierAutodetectThreshold);
                    if (carrierBand >= 0) {
                        break;
                    }
                }
                advance = (int) (i + nSamplesPerScan);
                if (advance > samplesNValid) {
                    advance = samplesNValid;
                }
                if (carrierBand < 0) {
                    fLogger.debug("autodetected carrier band was not found");
                    continue;
                }
                // default negative shift -- reasonable?
			    int bShift = (int) (- (float)(autodetectShift + bandWidth/2.0f)/bandWidth);
                if (bfskInvertedFreqs) {
                    bShift *= -1;
                }
                /* only accept a carrier as b_mark if it will not result
                 * in a bSpace band which is "too low".
                 */
                int bSpace = carrierBand + bShift;
                if (bSpace < 1 || bSpace >= fskp.getnBands()) {
                    fLogger.debug("autodetected space band out of range");
                    continue;
                }
                fLogger.debug("### TONE freq=%.1f ###", carrierBand*bandWidth);
                fskp.fskSetTonesByBandshift(/*bMark*/ carrierBand, bShift);
            }
            /*
             * The main processing algorithm: scan samplesbuf for FSK frames,
             * looking at an entire frame at once.
             */
            fLogger.debug( "--------------------------");

            if ( samplesNValid < expectNSamples ) {
                break;
            }

           /* try_max_nsamples
            * serves two purposes:
            * avoids finding a non-optimal first frame
            * allows us to track slightly slow signals
            */
            int tryMaxNSamples;
            if(carrier) {
                tryMaxNSamples = (int)(nSamplesPerBit * 0.75f + 0.5f);
            } else {
                tryMaxNSamples = (int)nSamplesPerBit;
            }
            tryMaxNSamples += nSamplesOverscan;
            int tryStepNsamples = tryMaxNSamples/FSK_ANALYZE_NSTEPS;
            if(tryStepNsamples == 0) {
                tryStepNsamples = 1;
            }
            float confidence, amplitude;
            long bits_U = 0;
            /* Note: frameStartSample is actually the sample where the
             * prev_stop bit begins (since the "frame" includes the prev_stop). */
            int frameStartSample = 0;

            int tryFirstSample_U;
            float tryConfidenceSearchLimit;

            tryConfidenceSearchLimit = fskConfidenceSearchLimit;
            tryFirstSample_U = carrier ? nSamplesOverscan : 0;

            boolean doRefineFrame = false;

            Number[] resFF = fskp.fskFindFrame(sampleBuf,
                    expectNSamples,
                    tryFirstSample_U,
                    tryMaxNSamples,
                    tryStepNsamples,
                    tryConfidenceSearchLimit,
                    carrier ? expectDataString : expectSyncString);

            confidence = (float) resFF[0];
            bits_U = (long) resFF[1];
            amplitude = (float) resFF[2];
            frameStartSample = (int) resFF[3];

            if(confidence < peakConfidence * 0.75f) {
                doRefineFrame = true;
                fLogger.debug(" ... do_refine_frame rescan (confidence %f << %f peak)", confidence, peakConfidence);
                peakConfidence = 0;
            }
            // no-confidence if amplitude drops abruptly to < 25% of the
            // track_amplitude, which follows amplitude with hysteresis
            if(amplitude < trackAmplitude * 0.25f) {
                confidence = 0;
            }

            if(confidence <= fskConfidenceThreshold) {
                if (++noconfidence > FSK_MAX_NOCONFIDENCE_BITS) {
                    if (carrier) {
                        if (!quietMode) {
                            reportNoCarrier(nFramesDecoded,
                                    carrierNSamples,
                                    confidenceTotal,
                                    amplitudeTotal);
                        }
                        carrier = false;
                        carrierNSamples = 0;
                        confidenceTotal = 0.0f;
                        amplitudeTotal = 0.0f;
                        nFramesDecoded = 0;
                        trackAmplitude = 0.0f;

                        if (rxOne) {
                            break;
                        }
                    }
                }
                /* Advance the sample stream forward by try_max_nsamples so the
                 * next time around the loop we continue searching from where
                 * we left off this time.		*/
                advance = tryMaxNSamples;
                fLogger.debug("@ NOCONFIDENCE=%d advance=%d", noconfidence, advance);
                continue;
            }
            // Add a frame's worth of samples to the sample count
            carrierNSamples += frameNSamples;
            if(carrier) {
                // If we already had carrier, adjust sample count +start -overscan
                carrierNSamples += frameStartSample;
                carrierNSamples -= nSamplesOverscan;
            } else { // We just acquired carrier.

                if (!quietMode) {
                    if (bfskDataRate >= 100) {
                        fLogger.info("### CARRIER %.2f @ %.1f Hz ", bfskDataRate + 0.5f, fskp.getbMark() * bandWidth);
                    } else {
                        fLogger.info("### CARRIER %.2f @ %.1f Hz ", bfskDataRate, fskp.getbMark() * bandWidth);
                    }
                    fLogger.info("###");
                }

                carrier = true;
                decoder.decode(null, 0, 0, 0); // reset the frame processor

                doRefineFrame = true;
                fLogger.debug(" ... do_refine_frame rescan (acquired carrier)");
            }

            if(doRefineFrame) {
                if(confidence < Float.POSITIVE_INFINITY && tryStepNsamples > 1) {
                    tryStepNsamples = Integer.divideUnsigned(tryMaxNSamples, FSK_ANALYZE_NSTEPS_FINE);
                    if(tryStepNsamples == 0) {
                        tryStepNsamples = 1;
                    }
                    tryConfidenceSearchLimit = Float.POSITIVE_INFINITY;
                }

                resFF = fskp.fskFindFrame(sampleBuf,
                        expectNSamples,
                        tryFirstSample_U,
                        tryMaxNSamples,
                        tryStepNsamples,
                        tryConfidenceSearchLimit,
                        carrier ? expectDataString : expectSyncString);
                if((float)resFF[0] > confidence) {
                    bits_U = (long) resFF[1];
                    amplitude = (float) resFF[2];
                    frameStartSample = (int) resFF[3];
                }

            }
            trackAmplitude = (trackAmplitude + amplitude) / 2;
            if(peakConfidence < confidence) {
                peakConfidence = confidence;
            }
            fLogger.debug("@ confidence=%.3f peak_conf=%.3f amplitude=%.3f track_amplitude=%.3f",
                    confidence,
                    peakConfidence,
                    amplitude,
                    trackAmplitude);

            confidenceTotal += confidence;
            amplitudeTotal += amplitude;
            nFramesDecoded++;
            noconfidence = 0;

            // Advance the sample stream forward past the junk before the
            // frame starts (frame_start_sample), and then past decoded frame
            // (see also NOTE about frame_n_bits and expect_n_bits)...
            // But actually advance just a bit less than that to allow
            // for tracking slightly fast signals, hence - nsamples_overscan.
            advance = frameStartSample + frameNSamples - nSamplesOverscan;

            fLogger.debug("@ nsamples_per_bit=%.3f n_data_bits=%d  frame_start=%d advance=%d",
                    nSamplesPerBit,
                    bfskNDataBits,
                    frameStartSample,
                    advance);

            // chop off the prev_stop bit
            if(bfskNStopBits != 0.0f) {
                bits_U = bits_U >>> 1;
            }
            /*
             * Send the raw data frame bits to the backend frame processor
             * for final conversion to output data bytes.
             */
            // chop off framing bits
            bits_U = bitWindow(bits_U, bfskNStartBits, bfskNDataBits);
            if(bfskMsbFirst) {
                bits_U = bitReverse(bits_U, bfskNDataBits);
            }
            fLogger.debug("Input: %08x%08x - Databits: %d - Shift: %d",
                    (int)(bits_U >>> 32),
                    (int)bits_U,
                    bfskNDataBits,
                    bfskNStartBits);

            int dataoutSize_U = 4_096;
            byte[] dataoutbuf = new byte[4_096];
            int dataoutNbytes_U = 0;

            // suppress printing of bfsk_sync_byte bytes
            if(bfskDoRxSync) {
                if(dataoutNbytes_U == 0 && bits_U == bfskSyncByte) {
                    continue;
                }
            }
            dataoutNbytes_U = decoder.decode(dataoutbuf, dataoutSize_U , bits_U, bfskNDataBits);

            if(dataoutNbytes_U == 0) {
                continue;
            }
            /*
             * Print the output buffer to stdout
             */
            for(int p=0; dataoutNbytes_U != 0; p++, dataoutNbytes_U--) {
                char printable;
                if(!outputPrintFilter) {
                    printable = (char) dataoutbuf[p];
                } else {
                    printable = (Character.isISOControl(dataoutbuf[p]) || Character.isSpaceChar(dataoutbuf[p])) ?  '.' : (char) dataoutbuf[p];
                }
                System.out.print(printable);
            }
            if(carrier) {
                if(!quietMode) {
                    reportNoCarrier(nFramesDecoded,
                            carrierNSamples,
                            confidenceTotal,
                            amplitudeTotal);
                }
            }
        }
        return ret;
    }

    /**
     * A helper function to report "no carrier"
     * @param nFramesDecoded        total number of frames decoded
     * @param carrierNSamples
     * @param confidenceTotal
     * @param amplitudeTotal
     */
    private void reportNoCarrier(int nFramesDecoded,
                                 long carrierNSamples,
                                 float confidenceTotal,
                                 float amplitudeTotal) {
        float nBitsDecoded = nFramesDecoded * bfskFrameNBits;
        float throughputRate = nBitsDecoded * sampleRate / carrierNSamples;
        fLogger.info("\n### NOCARRIER ndata=%d confidence=%.3f ampl=%.3f bps=%.2f",
                nFramesDecoded,
                confidenceTotal/ nFramesDecoded,
                amplitudeTotal / nFramesDecoded,
                throughputRate);
        if((long)(nBitsDecoded * sampleRate + 0.5f) == (long)(bfskDataRate * carrierNSamples)) {
            fLogger.info(" (rate perfect) ###");
        } else {
            float throughputSkew = (throughputRate - bfskDataRate) / bfskDataRate;
            fLogger.info(" (%.1f%% %s) ###",
                    (float)(Math.abs(throughputSkew) * 100.0f), Math.signum(throughputSkew) < 0 ? "slow" : "fast");
        }
    }


    /**
     * Builds expected bits string
     * Example expect_bits_string
     *	  0123456789A
     *	  isddddddddp	i == idle bit (a.k.a. prev_stop bit)
     *			s == start bit  d == data bits  p == stop bit
     * ebs = "10dddddddd1"  <-- expected mark/space framing pattern
     *
     * NOTE! expect_n_bits ends up being (frame_n_bits+1), because
     * we expect the prev_stop bit in addition to this frame's own
     * (start + n_data_bits + stop) bits.  But for each decoded frame,
     * we will advance just frame_n_bits worth of samples, leaving us
     * pointing at our stop bit -- it becomes the next frame's prev_stop.
     *
     *                  prev_stop--v
     *                       start--v        v--stop
     * char *expect_bits_string = "10dddddddd1";
     *
     * @param expectBitsString
     * @param useExpectBits
     * @param expectBits
     * @return a mask for expected string
     */
    protected int buildExpectBitsString(byte[] expectBitsString,
                                        int useExpectBits,
                                        long expectBits) {
        byte startBitValue = (byte)(invertStartStop ? '1' : '0');
        byte stopBitValue = (byte)(invertStartStop ? '0' : '1');
        int i, j = 0;
        if(bfskNStopBits != 0.0f) {
            expectBitsString[j++] = stopBitValue;
        }
        // Nb. only integer number of start bits works (for rx)
        for(i = 0; i < bfskNStartBits; i++) {
            expectBitsString[j++] = startBitValue;
        }
        for(i = 0; i < bfskNDataBits; i++, j++) {
            if(useExpectBits != 0) {
                expectBitsString[j] = (byte)((expectBits >>> i & 1) + '0');
            } else {
                expectBitsString[j] = 'd';
            }
        }
        if(bfskNStopBits != 0.0f) {
            expectBitsString[j++] = stopBitValue;
        }
        expectBitsString[j] = (byte)0;
        return j;
    }

    /**
     *
     * @param a byte array presumably padded with 0s (output of buildExpectBitsString)
     * @return string representation including trailing '\0'
     *          if there is no trailing '\0'  '>>' will be added (looks abnormal)
     */
    protected static String byteArray2String(byte[] a) {
        StringBuilder sb = new StringBuilder("");
        int i;
        for (i=0; i<a.length && a[i]!=0; i++) {
            sb.append((char)a[i]);
        }
        sb.append(i<a.length?"\\0":">>");
        return sb.toString();
    }

    /**
     * Shifts sample buffer for p positions left (sampleBuf[0] <-- sampleBuf[p])
     * @param p
     */
    protected void shiftSampleBuf(int p) {
        int i,j;
        for (i=p, j=0; i<sampleBuf.capacity(); i++, j++){
            sampleBuf.put(j, sampleBuf.get(i));
        }
        sampleBuf.position(0);
    }

    /**
     * Helper function to (re)fill samples buffer
     * @return  >0   OK, number of samples red
     *          ==0  EOF reached
     *          <0   Error
     */
    protected int refillBuf() {
        int r = -1;
        fLogger.debug("Refilling buffer: advance = %d, samplesNValid = %d", advance, samplesNValid);
        /*
         * (not really required for Java)
         *     assert advance <= samplebufSize;
         *     assert advance <= samplesNValid;
         */

        if (advance == samplebufSize) {
            samplesNValid = 0;
            advance = 0;
        }
        /* Shift the samples in sampleBuf by 'advance' samples */
        if (advance != 0) {
            shiftSampleBuf(advance);
            samplesNValid -= advance;
            advance = 0;
        }
        if (samplesNValid < samplebufSize / 2) {
            int readNSamples = samplebufSize / 2;
        /*
         *  (not really required for Java)
         *     assert readNSamples > 0;
         *     assert samplesNValid + readNSamples <= samplebufSize;
         */

        /* Read more samples into samplebuf (fill it) */
            r = rxSaIn.read(sampleBufRaw, readNSamples);
            fLogger.debug("Reading audio (%d samples) returns %d", readNSamples, r);
            if (r < 0) {
                fLogger.error("Audio file read error");
            } else {
                samplesNValid += r;
            }
        }
        return r;
    }
}
