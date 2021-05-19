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

     private float[] sampleBuf;
     private byte[] expectDataString;
     private byte[] expectSyncString;

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
                     int syncByte) {
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
     }

    public void configure(byte[] expctDataString) {

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
            buildExpectBitsString(expectDataString, 0, 0);
        } else {
            expectDataString = expctDataString;
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

        long samplesNvalid_U = 0;
        int ret = 0;

        int carrier = 0;
        float confidenceTotal = 0;
        float amplitudeTotal = 0;
        int nframesDecoded_U = 0;
        long carrierNsamples_U = 0;

        int noconfidence_U = 0;
        int advance_U = 0;

Signal.handle()

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
