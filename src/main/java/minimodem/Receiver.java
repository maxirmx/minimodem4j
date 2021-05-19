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

     private float[] sampleBuf;

    /**
     *
     * @param saIn
     */
     public Receiver(SimpleAudio saIn) {
        rxSaIn = saIn;
        sampleRate = saIn.getRate();

     }

    /**
     *
     * @param bfskDataRate
     * @param bfskNStartBits
     * @param bfskNDataBits
     */
    public void configure(float bfskDataRate,
                          int bfskNStartBits,
                          int bfskNDataBits) {

        /*
         * Prepare the input sample chunk rate
         */
        float nSamplesPerBit = sampleRate / bfskDataRate;

        /*
         * Prepare the fsk plan


        AbstractData fskp;
        fskp = String8.from(fskPlanNew(sampleRate, bfskMarkF, bfskSpaceF, bandWidth));
        if(fskp == null) {
            System.err.println("fsk_plan_new() failed");
            return  1 ;
        }
*/
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
        int samplebufSize = (int)(ceil(nSamplesPerBit) * Integer.toUnsignedLong(nBits + 1));
        samplebufSize *= 2; // account for the half-buf filling method

        // For performance, use a larger samplebuf_size than necessary
        if(samplebufSize < sampleRate / SAMPLE_BUF_DIVISOR) {
            samplebufSize = sampleRate / SAMPLE_BUF_DIVISOR;
        }
        fLogger.debug("samplebufSize=%i", samplebufSize);
        sampleBuf = new float[samplebufSize];

        // Ensure that we overscan at least a single sample
        int nSamplesOverscan = (int)(nSamplesPerBit * FRAME_OVERSCAN + 0.5f);
        if(FRAME_OVERSCAN > 0.0f && nSamplesOverscan == 0) {
            nSamplesOverscan = 1;
        }
        fLogger.debug(("FRAME_OVERSCAN=%f nSamplesOverscan=%i"), FRAME_OVERSCAN, nSamplesOverscan);



        long samplesNvalid_U = 0;
        int ret = 0;

        int carrier = 0;
        float confidenceTotal = 0;
        float amplitudeTotal = 0;
        int nframesDecoded_U = 0;
        long carrierNsamples_U = 0;

        int noconfidence_U = 0;
        int advance_U = 0;


        float frameNBits = bfskFrameNBits;
        int frameNsamples_U = (int)(nsamplesPerBit * frameNBits + 0.5f);

        String8 expectDataStringBuffer = new String8(64);
        if(expectDataString == null) {
            expectDataString = expectDataStringBuffer;
            expectNBits_U = buildExpectBitsString(expectDataString, bfskNstartbits, bfskNDataBits_U, bfskNstopbits, (MethodRef0<Integer>)ImplicitDeclarations::invertStartStop, 0, 0);
        }
        debugLog(cs8("eds = '%s' (%lu)\n"), expectDataString, expectDataString.length());

        String8 expectSyncStringBuffer = new String8(64);
        if(expectSyncString == null && bfskDoRxSync_U && Integer.toUnsignedLong(bfskSyncByte_U) >= 0) {
            expectSyncString = expectSyncStringBuffer;
            buildExpectBitsString(expectSyncString, bfskNstartbits, (MethodRef0<Integer>)ImplicitDeclarations::bfskNDataBits, bfskNstopbits, (MethodRef0<Integer>)ImplicitDeclarations::invertStartStop, 1, bfskSyncByte_U);
        } else {
            expectSyncString = expectDataString;
        }
        debugLog(cs8("ess = '%s' (%lu)\n"), expectSyncString, expectSyncString.length());

    }

    public void receive() {

        int expectNsamples_U = nsamplesPerBit * expectNBits_U;
        float trackAmplitude = 0.0f;
        float peakConfidence = 0.0f;

        signal((MethodRef0<Integer>)ImplicitDeclarations::sigint, (MethodRef0<Integer>)ImplicitDeclarations::rxStopSighandler);

        while(true) {
            if(rxStop) {
                break;
            }
            debugLog(cs8("advance=%u\n"), advance_U);
            /* Shift the samples in samplebuf by 'advance' samples */
            assert Integer.compareUnsigned(advance_U, samplebufSize) <= 0;
            if(advance_U == samplebufSize) {
                samplesNvalid_U = false;
                advance_U = 0;
            }
            if(advance_U != 0) {
                if(Long.compareUnsigned(Integer.toUnsignedLong(advance_U), samplesNvalid_U) > 0) {
                    break;
                }
                nnc(String8.from(ImplicitDeclarations::samplebuf)).copyFrom(String8.from(dataAddress((MethodRef0<Integer>)ImplicitDeclarations::samplebuf) + advance_U), (int)(Integer.toUnsignedLong(samplebufSize - advance_U) * ((long)FLOAT_SIZE)));
                samplesNvalid_U -= Integer.toUnsignedLong(advance_U);
            }
            if(Long.compareUnsigned(samplesNvalid_U, samplebufSize / 2) < 0) {
                float[] samplesReadptr = sampleBuf;
                int samplesReadptrIndex = (int)samplesNvalid_U;
                long readNsamples_U = samplebufSize / 2;
                /* Read more samples into samplebuf (fill it) */
                assert Long.compareUnsigned(readNsamples_U, 0) > 0;
                assert Long.compareUnsigned(samplesNvalid_U + readNsamples_U, samplebufSize) <= 0;
                long r;
                r = simpleaudioRead((MethodRef0<Integer>)ImplicitDeclarations::sa, (MethodRef0<Integer>)ImplicitDeclarations::samplesReadptr, readNsamples_U);
                debugLog(cs8("simpleaudio_read(samplebuf+%td, n=%zu) returns %zd\n"), nnc(sampleBuf).shift((int)dataAddress(-(MethodRef0<Integer>)ImplicitDeclarations::samplesReadptr)), readNsamples_U, r);
                if(r < 0) {
                    System.err.println("simpleaudio_read: error");
                    ret = -1;
                    break;
                }
                samplesNvalid_U +=r;
            }
			if(samplesNvalid_U == 0) {
				break;
			}

			if(carrierAutodetectThreshold > 0.0f && carrierBand < 0) {
				float nsamplesPerScan = nsamplesPerBit;
				/*	    if ( nsamples_per_scan > fskp->fftsize )
		nsamples_per_scan = fskp->fftsize;*/
				for(int i_U = 0; Integer.toUnsignedLong(i_U) + nsamplesPerScan <= Float.parseFloat(Long.toUnsignedString(samplesNvalid_U)); i_U = (int)(Integer.toUnsignedLong(i_U) + nsamplesPerScan)) {
					/*		carrier_band = fsk_detect_carrier(fskp,
				    samplebuf+i, nsamples_per_scan,
				    carrier_autodetect_threshold);*/
					if(carrierBand >= 0) {
						break;
					}
                    			if(Long.compareUnsigned(Integer.toUnsignedLong(advance_U), samplesNvalid_U) > 0) {
				advance_U = (int)samplesNvalid_U;
			}
			if(carrierBand < 0) {
				debugLog(cs8("autodetected carrier band not found\n"));
				continue;
			
				}
             }   
			// default negative shift -- reasonable?
			/*	    int b_shift = - (float)(autodetect_shift + fskp->band_width/2.0f)
						/ fskp->band_width;*/
			if(bfskInvertedFreqs) {
				bShift *= -1;
			}
			/* only accept a carrier as b_mark if it will not result
			 * in a b_space band which is "too low". */
			int bSpace = carrierBand + bShift;
			if(bSpace < 1 /*|| b_space >= fskp->nbands*/) {
				debugLog(cs8("autodetected space band out of range\n"));
				carrierBand = -1;
				continue;
			}
	    debug_log("### TONE freq=%.1f ###\n",
		    carrier_band * fskp->band_width);

	    fsk_set_tones_by_bandshift(fskp, /*b_mark*/carrier_band, b_shift);
        }


    }

}
