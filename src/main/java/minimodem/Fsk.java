package minimodem;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;

public class Fsk {
    private static final Logger fLogger = LogManager.getFormatterLogger("Fsk");

    public final static double FSK_MIN_MAGNITUDE = 0.0f;
    public final static double FSK_MIN_BIT_SNR = 1.0f;



    private final float bandWidth;
    private final int  nBands;

    private float sampleRate;
    private float fMark;
    private float fSpace;
    private float filterBw;

    private int	  fftSize;
    private int   bMark;
    private int   bSpace;
    private float[] fftin = null;
    private float[] fftout = null;
///	fftwf_plan	fftplan; https://www.nayuki.io/res/free-small-fft-in-multiple-languages/Fft.java

    public Fsk(float sampleRate, float fMark, float fSpace, float filterBw) {
        this.sampleRate = sampleRate;
        this.fMark = fMark;
        this.fSpace = fSpace;
        this.filterBw = filterBw;
        
        this.bandWidth = filterBw;

        float fftHalfBw = bandWidth / 2.0f;
        fftSize = (int)((sampleRate + fftHalfBw) / bandWidth);
        nBands = fftSize;

        bMark = (int)((fMark + fftHalfBw) / bandWidth);
        bSpace = (int)((fSpace + fftHalfBw) / bandWidth);
        if(bMark > nBands || bSpace > nBands) {
            fLogger.error("b_mark=%d or b_space=%d  is invalid (nbands=%d)", bMark, bSpace, nBands);
            return;
        }
        fLogger.debug("### b_mark=%u b_space=%u fftsize=%d", bMark, bSpace, fftSize);
    }

    public void fskSetTonesByBandshift(int bMark, int bShift) {
        assert bShift != 0;
        assert bMark < nBands;

        int bSpace = bMark + bShift;
        assert bSpace >= 0;
        assert bSpace < nBands;

        this.bMark = bMark;
        this.bSpace = bSpace;
        this.fMark = bMark * this.bandWidth;
        this.fSpace = bSpace * this.bandWidth;
    }

    public int fskDetectCarrier(float[] samples, int pSamples, int nSamples, float minMagThreshold) {
        assert nSamples<=fftSize;
        int paNChannels = 1; // FIXME
        float[] fftin = Arrays.copyOfRange(samples, pSamples, pSamples+nSamples);
//        fftwfExecute(fskp[0].getFftplan());
        float[][] fftout = new float[nSamples][2];
        float magScalar = 1.0f / nSamples / 2.0f;
        float maxMag = 0.0f;
        int maxMagBand = -1;
        int i = 1; /* start detection at the first non-DC band */
        for(; i < nBands; i++) {
            float mag = bandMag(fftout, i, magScalar);
            if(mag < minMagThreshold) {
                continue;
            }
            if(maxMag < mag) {
                maxMag = mag;
                maxMagBand = i;
            }
        }
        if(maxMagBand < 0) {
            return -1;
        }
        return maxMagBand;
    }

    private static void fskBitAnalyze(FskPlan[] fskp, float[] samples, int bitNsamples_U, int[] bitOutp_U, float[] bitSignalMagOutp, float[] bitNoiseMagOutp) {
        // FIXME: Fast and loose ... don't bzero fftin, just assume its only ever
        // been used for bit_nsamples so the remainder is still zeroed.  Sketchy.
        //
        // unsigned int pa_nchannels = 1;	// FIXME
        // bzero(fskp->fftin, (fskp->fftsize * sizeof(float) * pa_nchannels));

        nnc(fskp[0].getFftin()).copyFrom(samples, bitNsamples_U);

        float magscalar = 2.0f / Integer.toUnsignedLong(bitNsamples_U);
//        fftwf_execute(fskp->fftplan);
        float magMark = bandMag(fskp[0].getFftout(), fskp[0].getBMark_U(), magscalar);
        float magSpace = bandMag(fskp[0].getFftout(), fskp[0].getBSpace_U(), magscalar);
        // mark==1, space==0
        if(magMark > magSpace) {
            bitOutp_U[0] = 1;
            bitSignalMagOutp[0] = magMark;
            bitNoiseMagOutp[0] = magSpace;
        } else {
            bitOutp_U[0] = 0;
            bitSignalMagOutp[0] = magSpace;
            bitNoiseMagOutp[0] = magMark;
        }
        float magSpace = bandMag(fskp[0].getFftout(), fskp[0].getBSpace_U(), magscalar);
        debugLog(cs8("\t%.2f  %.2f  %s  bit=%u sig=%.2f noise=%.2f\n"), magMark, magSpace, magMark > magSpace ? cs8("mark      ") : cs8("     space"), bitOutp_U[0], bitSignalMagOutp[0], bitNoiseMagOutp[0]);


    }

    /**
     * returns confidence value [0.0 to INFINITY]
     */
    private static float fskFrameAnalyze(FskPlan[] fskp, float[] samples, float samplesPerBit, int nBits, byte[] expectBitsString, long[] bitsOutp_U, float[] amplOutp) {
        int bitNsamples_U = (int)(samplesPerBit + 0.5f);

        int[] bitValues_U = new int[64];
        float[] bitSigMags = new float[64];
        float[] bitNoiseMags = new float[64];
        int bitBeginSample_U;
        int bitnum;
        byte[] expectBits = expectBitsString;

        /* pass #1 - process and check only the "required" (1/0) expect_bits */
        for(int bitnum = 0; bitnum < nBits; bitnum++) {
            if(expectBits[bitnum] == 'd') {
                continue;
            }
            assert expectBits[bitnum] == '1' || expectBits[bitnum] == '0';

            bitBeginSample_U = (int)(samplesPerBit * bitnum + 0.5f);
            debugLog(cs8(" bit# %2d @ %7u: "), bitnum, bitBeginSample_U);

            fskBitAnalyze(fskp, nnc(samples).shift(bitBeginSample_U), bitNsamples_U, bitValues_U.shift(bitnum), bitSigMags.shift(bitnum), bitNoiseMags.shift(bitnum));
            if(expectBits[bitnum] - '0' != bitValues_U[bitnum]) {
                return 0.0f; /* does not match expected; abort frame analysis. */
            }
            #ifdef
            float bitSnr = bitSigMags[bitnum] / bitNoiseMags[bitnum];
            if(bitSnr < FSK_MIN_BIT_SNR) {
                return 0.0f;
            }
            #endif

            // Performance hack: reject frame early if sig mag isn't even half
            // of FSK_MIN_MAGNITUDE
            #ifdef
            if(bitSigMags[bitnum] < FSK_MIN_MAGNITUDE / 2.0) {
                return 0.0f; // too weak; abort frame analysis
            }
            #endif
            /* pass #2 - process only the dontcare ('d') expect_bits */
            for(int bitnum = 0; bitnum < nBits; bitnum++) {
                if(expectBits[bitnum] != 'd') {
                    continue;
                }
                bitBeginSample_U = (int)(samplesPerBit * bitnum + 0.5f);
                debugLog(cs8(" bit# %2d @ %7u: "), bitnum, bitBeginSample_U);
                fskBitAnalyze(fskp, nnc(samples).shift(bitBeginSample_U), bitNsamples_U, bitValues_U.shift(bitnum), bitSigMags.shift(bitnum), bitNoiseMags.shift(bitnum));
            #ifdef
                float bitSnr = bitSigMags[bitnum] / bitNoiseMags[bitnum];
                if(bitSnr < FSK_MIN_BIT_SNR) {
                    return 0.0f;
                }
            #endif
            }

            float confidence;
            float totalBitSig = 0.0f, totalBitNoise = 0.0f;
            float avgMarkSig = 0.0f, avgSpaceSig = 0.0f;
            int nMark_U = 0, nSpace_U = 0;

            for(int bitnum = 0; bitnum < nBits; bitnum++) {
                // Deal with floating point data type quantization noise...
                // If total_bit_noise <= FLT_EPSILON, then assume it to be 0.0,
                // so that we end up with snr==inf.
                totalBitSig += bitSigMags[bitnum];
                if(bitNoiseMags[bitnum] > FLT_EPSILON) {
                    totalBitNoise += bitNoiseMags[bitnum];
                }

                if(bitValues_U[bitnum] == 1) {
                    avgMarkSig += bitSigMags[bitnum];
                    nMark_U++;
                } else {
                    avgSpaceSig += bitSigMags[bitnum];
                    nSpace_U++;
                }
            }

            // Compute the "frame SNR"
            float snr = totalBitSig / totalBitNoise;

            // Compute avg bit sig and noise magnitudes
            float avgBitSig = totalBitSig / nBits;

            // Compute separate avg bit sig for mark and space
            if(nMark_U != 0) {
                avgMarkSig /= Integer.toUnsignedLong(nMark_U);
            }
            if(nSpace_U != 0) {
                avgSpaceSig /= Integer.toUnsignedLong(nSpace_U);
            }
            // Compute average "divergence": bit_mag_divergence / other_bits_mag
            float divergence = 0.0f;
            for(bitnum = 0; bitnum < nBits; bitnum++) {
                float avgBitSigOther;
                avgBitSigOther = bitValues_U[bitnum] != 0 ? avgMarkSig : avgSpaceSig;
                divergence += Math.abs(bitSigMags[bitnum] - avgBitSigOther) / avgBitSigOther;
            }
            divergence *= 2;
            divergence /= nBits;

            debugLog(cs8("    divg=%.3f snr=%.3f avg{bit_sig=%.3f bit_noise=%.3f(%s)}\n"), (MethodRef0<Integer>)ImplicitDeclarations::divergence, (MethodRef0<Integer>)ImplicitDeclarations::snr, (MethodRef0<Integer>)ImplicitDeclarations::avgBitSig, avgBitNoise, avgBitNoise == 0.0 ? cs8("zero") : cs8("non-zero"));

            #ifdef
            if(avgBitSig < FSK_MIN_MAGNITUDE) {
                return 0.0f; // too weak; reject frame
            }
            #endif

            // Frame confidence is the frame ( SNR * consistency )
            confidence = snr * (1.0f - divergence);
            amplOutp[0] = avgBitSig;

            // least significant bit first ... reverse the bits as we place them
            // into the bits_outp word.
            bitsOutp_U[0] = 0;
            for(int bitnum = 0; bitnum < nBits; bitnum++) {
                bitsOutp_U[0] = bitsOutp_U[0] | Integer.toUnsignedLong(bitValues_U[bitnum]) << bitnum;
            }

            debugLog(cs8("    frame algo=%d confidence=%f ampl=%f\n"), (MethodRef0<Integer>)ImplicitDeclarations::confidenceAlgo, confidence, amplOutp[0]);
            return confidence;


        }

        return 0;
    }


    public static float fskFindFrame(float[] samples, int frameNsamples_U, int tryFirstSample_U, int tryMaxNsamples_U, int tryStepNsamples_U, float tryConfidenceSearchLimit, String8 expectBitsString, long[] bitsOutp_U, float[] amplOutp, int[] frameStartOutp_U) {
        int expectNBits = expectBitsString.length();
        assert expectNBits <= 64; // protect fsk_frame_analyze()
        float samplesPerBit = Integer.toUnsignedLong(frameNsamples_U) / expectNBits;
        // try_step_nsamples = 1;	// pedantic TEST
        int bestT_U = 0;
        float bestC = 0.0f, bestA = 0.0f;
        long bestBits_U = 0;
// Scan the frame positions starting with the one try_first_sample,
        // alternating between a step above that, a step below that, above, below,
        // and so on, until we've scanned the whole try_max_nsamples range.
        for (int j = 0; ; j++) {
            int up = j % 2 != 0 ? 1 : -1;
            int t = tryFirstSample_U + up * ((j + 1) / 2) * tryStepNsamples_U;
            if (t >= tryMaxNsamples_U) {
                break;
            }
            if (t < 0) {
                continue;
            }
            float c;
            FloatContainer amplOut = FloatContainer.fromData(0.0f);
            LongContainer bitsOut_U = LongContainer.fromData(0);
            debugLog(cs8("try fsk_frame_analyze at t=%d\n"), t);
            c = fskFrameAnalyze(fskp, nnc(samples).shift(t), samplesPerBit, expectNBits, expectBitsString, bitsOut_U, amplOut);
            if (bestC < c) {
                bestT_U = t;
                bestC = c;
                bestA = amplOut;
                bestBits_U = bitsOut_U;
                // If we find a frame with confidence > try_confidence_search_limit
                // quit searching.
                if (bestC >= tryConfidenceSearchLimit) {
                    break;
                }
            }
            bitsOutp_U[0] = bestBits_U;
            amplOutp[0] = bestA;
            frameStartOutp_U[0] = bestT_U;

            float confidence = bestC;

            if (confidence == 0) {
                return 0;
            }
#ifdef
            byte bitchar_U;
            // FIXME? hardcoded chop off framing bits for debug
            // Hmmm... we have now way to  distinguish between:
            // 		8-bit data with no start/stopbits == 8 bits
            // 		5-bit with prevstop+start+stop == 8 bits
            switch (expectNBits) {
                case 11:
                    bitchar_U = (byte) (bitsOutp_U[0] >>> 2 & 0xFF);
                    break;
                case 8:
                default:
                    bitchar_U = (byte) (bitsOutp_U[0] & 0xFF);
                    break;
            }
            debugLog(cs8("FSK_FRAME bits='"));
            for (int j = 0; j < expectNBits; j++) {
                debugLog(cs8("%c"), (bitsOutp_U[0] >>> j & 1) != 0 ? '1' : '0');
            }
            debugLog(cs8("' datum='%c' (0x%02x)   c=%f  a=%f  t=%u\n"), isprint(bitchar_U) != 0 || isspace(bitchar_U) != 0 ? Byte.toUnsignedInt(bitchar_U) : '.', bitchar_U, (MethodRef0<Integer>) ImplicitDeclarations::confidence, (MethodRef0<Integer>) ImplicitDeclarations::bestA, bestT_U);
#endif
            return confidence;
        }
    }

    static float bandMag(float[][] cplx, int band, float scalar )
    {
        float re = cplx[band][0];
        float im = cplx[band][1];
        float mag = (float) (Math.sqrt(re*re+im*im) * scalar);
        return mag;
    }




}
