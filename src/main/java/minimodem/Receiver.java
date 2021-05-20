package minimodem;

import minimodem.simpleaudio.SimpleAudio;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static java.lang.Math.ceil;

public class Receiver {
    private static final Logger fLogger = LogManager.getFormatterLogger("Receiver");
    public final static int SAMPLE_BUF_DIVISOR = 12;

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

     private float nSamplesPerBit;
     private int expectNBits;
     private int samplebufSize;
     private float[] sampleBuf;
     private byte[] expectDataString;
     private byte[] expectSyncString;
     private Fsk fskp;

    /**
     *
     * @param saIn
     */
     public Receiver(SimpleAudio saIn,
                     float dataRate,
                     int nStartBits,
                     float nStopBits,
                     int nDataBits,
                     int frameNBits,
                     boolean iStartStop,
                     boolean doRxSync,
                     int syncByte,
                     float spaceF,
                     float markF,
                     float bWidth,
                     float cAutodetectThreshold) {
        rxSaIn = saIn;
        sampleRate = saIn.getRate();
        bfskDataRate = dataRate;
        bfskNStartBits = nStartBits;
        bfskNStopBits = nStopBits;
        bfskNDataBits = nDataBits;
        bfskFrameNBits = frameNBits;
        invertStartStop = iStartStop;
        bfskDoRxSync = doRxSync;
        bfskSyncByte = syncByte;
        bfskMarkF =  spaceF;
        bfskSpaceF = markF;
        bandWidth = bWidth;
        carrierAutodetectThreshold = cAutodetectThreshold;
     }

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

        // FIXME EXPLAIN +1 goes with extra bit when scanning
        samplebufSize = (int)(ceil(nSamplesPerBit) * Integer.toUnsignedLong(nBits + 1));
        samplebufSize *= 2; // account for the half-buf filling method

        // For performance, use a larger samplebuf_size than necessary
        if(samplebufSize < sampleRate / SAMPLE_BUF_DIVISOR) {
            samplebufSize = sampleRate / SAMPLE_BUF_DIVISOR;
        }
        fLogger.debug("Created sample buffer with samplebufSize=%i", samplebufSize);
        sampleBuf = new float[samplebufSize];

        // Ensure that we overscan at least a single sample
        int nSamplesOverscan = (int)(nSamplesPerBit * FRAME_OVERSCAN + 0.5f);
        if(FRAME_OVERSCAN > 0.0f && nSamplesOverscan == 0) {
            nSamplesOverscan = 1;
        }
        fLogger.debug(("FRAME_OVERSCAN=%f nSamplesOverscan=%i"), FRAME_OVERSCAN, nSamplesOverscan);

        float frameNBits = bfskFrameNBits;
        int frameNSamples = (int)(nSamplesPerBit * frameNBits + 0.5f);

        if(expctDataString == null) {
            expectDataString = new byte[64];
            expectNBits = buildExpectBitsString(expectDataString, 0, 0);
        } else {
            expectDataString = expctDataString;
            expectNBits = expctDataString.length;
        }
        fLogger.debug("expectDataString = '%s' (%d)", expectDataString.toString());

        expectSyncString = expectDataString;
        if(bfskDoRxSync && bfskSyncByte >= 0) {
            expectSyncString = new byte[64];
            buildExpectBitsString(expectSyncString, 1, bfskSyncByte);
        }
        fLogger.debug("expectSyncString = '%s'", expectSyncString);

    }

    public void receive() {

        int samplesNValid = 0;
        int ret = 0;

        int carrier = 0;
        float confidenceTotal = 0;
        float amplitudeTotal = 0;
        int nframesDecoded_U = 0;
        long carrierNsamples_U = 0;

        int noconfidence_U = 0;
        int advance = 0;

        int expectNSamples = (int) (nSamplesPerBit * expectNBits);
        float trackAmplitude = 0.0f;
        float peakConfidence = 0.0f;

        boolean rxStop = false;
        while (!rxStop) {
            fLogger.debug("advance = %d", advance);
            /* Shift the samples in sampleBuf by 'advance' samples */
            assert Integer.compareUnsigned(advance, samplebufSize) <= 0;
            if (advance == samplebufSize) {
                samplesNValid = 0;
                advance = 0;
            }
            if (advance != 0) {
                if (advance > samplesNValid) {
                    break;
                }
                System.arraycopy(sampleBuf,advance,sampleBuf,0, sampleBuf.length-advance);
                samplesNValid -= advance;
            }
            if (samplesNValid < samplebufSize / 2) {
                float[] samplesReadptr = sampleBuf;
                int samplesReadptrIndex = (int) samplesNValid;
                long readNsamples_U = samplebufSize / 2;
                /* Read more samples into samplebuf (fill it) */
                assert Long.compareUnsigned(readNsamples_U, 0) > 0;
                assert Long.compareUnsigned(samplesNValid + readNsamples_U, samplebufSize) <= 0;
                long r=0;
               // >>>>> r = sa.read()
              //  r = simpleaudioRead((MethodRef0<Integer>) ImplicitDeclarations::sa, (MethodRef0<Integer>) ImplicitDeclarations::samplesReadptr, readNsamples_U);
              //  debugLog(cs8("simpleaudio_read(samplebuf+%td, n=%zu) returns %zd\n"), nnc(sampleBuf).shift((int) dataAddress(-(MethodRef0<Integer>) ImplicitDeclarations::samplesReadptr)), readNsamples_U, r);
                if (r < 0) {
                    fLogger.error("Simpleaudio read error");
                    ret = -1;
                    break;
                }
                samplesNValid += r;
            }

            if (samplesNValid == 0) {
                break;
            }

            int carrierBand = -1;
            if (carrierAutodetectThreshold > 0.0f && carrierBand < 0) {
                int i;
                float nsamplesPerScan = nSamplesPerBit;
				/*	    if ( nsamples_per_scan > fskp->fftsize )
		nsamples_per_scan = fskp->fftsize;*/
                for (i = 0; i + nsamplesPerScan <= samplesNValid; i = (int) (i + nsamplesPerScan)) {
					/*		carrier_band = fsk_detect_carrier(fskp,
				    samplebuf+i, nsamples_per_scan,
				    carrier_autodetect_threshold);*/
                    if (carrierBand >= 0) {
                        break;
                    }
                }
                advance = (int) (i + nsamplesPerScan);
                    if (advance > samplesNValid) {
                        advance = samplesNValid;
                    }
                    if (carrierBand < 0) {
                        fLogger.debug("autodetected carrier band was not found");
                        continue;

                    }
                // default negative shift -- reasonable?
			/*	    int b_shift = - (float)(autodetect_shift + fskp->band_width/2.0f)
						/ fskp->band_width;*/
                if (bfskInvertedFreqs) {
                    bShift *= -1;
                }
                /* only accept a carrier as b_mark if it will not result
                 * in a b_space band which is "too low". */
                int bSpace = carrierBand + bShift;
                if (bSpace < 1 /*|| b_space >= fskp->nbands*/) {
                    debugLog(cs8("autodetected space band out of range\n"));
                    carrierBand = -1;
                    continue;
                }
                debug_log("### TONE freq=%.1f ###\n",
                        carrier_band * fskp -> band_width);

                fskp.fskSetTonesByBandshift(/*bMark*/ carrierBand, bShift);
            }
        }
    }

    // example expect_bits_string
    //	  0123456789A
    //	  isddddddddp	i == idle bit (a.k.a. prev_stop bit)
    //			s == start bit  d == data bits  p == stop bit
    // ebs = "10dddddddd1"  <-- expected mark/space framing pattern
    //
    // NOTE! expect_n_bits ends up being (frame_n_bits+1), because
    // we expect the prev_stop bit in addition to this frame's own
    // (start + n_data_bits + stop) bits.  But for each decoded frame,
    // we will advance just frame_n_bits worth of samples, leaving us
    // pointing at our stop bit -- it becomes the next frame's prev_stop.
    //
    //                  prev_stop--v
    //                       start--v        v--stop
    // char *expect_bits_string = "10dddddddd1";
    //

    protected int buildExpectBitsString(byte[] expectBitsString,
                                        int useExpectBits,
                                        long expectBits_U) {
        byte startBitValue = (byte)(invertStartStop ? '1' : '0');
        byte stopBitValue = (byte)(invertStartStop ? '0' : '1');
        int j = 0;
        if(bfskNStopBits != 0.0f) {
            expectBitsString[j++] = stopBitValue;
        }
        // Nb. only integer number of start bits works (for rx)
        for(int i = 0; i < bfskNStartBits; i++) {
            expectBitsString[j++] = startBitValue;
        }
        for(int i = 0; i < bfskNDataBits; i++, j++) {
            if(useExpectBits != 0) {
                expectBitsString[j] = (byte)((expectBits_U >>> i & 1) + '0');
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

}
