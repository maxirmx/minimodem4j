/*
 * minimodem4j
 * Fsk.java
 * Created from fsk.c, fsk.h @ https://github.com/kamalmostafa/minimodem
 */
package minimodem.fsk;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jtransforms.fft.FloatFFT_1D;

import java.nio.FloatBuffer;

public class Fsk {
    private static final Logger fLogger = LogManager.getFormatterLogger("Fsk");

    // Negative values mean not using fast fallbacks
    // Used to be conditional compilation in original C code
    public final static double FSK_MIN_MAGNITUDE = -1.0f;
    public final static double FSK_MIN_BIT_SNR = -1.0f;

    public final static int CONFIDENCE_ALGO = 6;

    private final float     bandWidth;
    private final int       nBands;
    private final int	    fftSize;
    private final float sampleRate;

    private int   bMark;
    private int   bSpace;

    private FloatFFT_1D fft;



    public Fsk(float sampleRate, float fMark, float fSpace, float filterBw) {
        this.sampleRate = sampleRate;

        this.bandWidth = filterBw;

        float fftHalfBw = bandWidth / 2.0f;
        fftSize = (int)((sampleRate + fftHalfBw) / bandWidth);
        nBands = fftSize/2 + 1;

        bMark = (int)((fMark + fftHalfBw) / bandWidth);
        bSpace = (int)((fSpace + fftHalfBw) / bandWidth);
        if(bMark > nBands || bSpace > nBands) {
            fLogger.error("b_mark=%d or b_space=%d  is invalid (nbands=%d)", bMark, bSpace, nBands);
            return;
        }
        fft = new FloatFFT_1D(fftSize);

        fLogger.debug("### b_mark=%d b_space=%d fftsize=%d", bMark, bSpace, fftSize);

    }

    public void fskSetTonesByBandshift(int bMark, int bShift) {
        assert bShift != 0;
        assert bMark < nBands;

        int bSpace = bMark + bShift;
        assert bSpace >= 0;
        assert bSpace < nBands;

        this.bMark = bMark;
        this.bSpace = bSpace;
    }

    /**
     * Carrier detector
     * @param sampleBuf          Sample buffer
     * @param nSamples           Position of the first sample to analyze
     * @param nSamples           Number of samples to analyze
     * @param minMagThreshold    Carrier threshold
     * @return    Carrier band or -1 if no carrier was detected
     */
    public int fskDetectCarrier(FloatBuffer sampleBuf, int pSamples, int nSamples, float minMagThreshold) {

        float[] fftbuf = new float[fftSize*2];
        sampleBuf.position(pSamples);
        sampleBuf.get(fftbuf, 0, nSamples);
        fft.realForwardFull(fftbuf);

        float magScalar = 1.0f / nSamples / 2.0f;
        float maxMag = 0.0f;
        int maxMagBand = -1;
        int i = 1; /* start detection at the first non-DC band */
        for(; i < nBands; i++) {
            float mag = bandMag(fftbuf, i, magScalar);
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

    /**
     * Bit analizer
     * @param sampleBuf     Sample buffer
     * @param pSamples      Starting position to analyze
     * @param nSamples      Number of samples to analyze
     * @return  Number[3]   [0] -->  Bit value (1/0): integer
     *                      [1] -->  Signal magnitude: float
     *                      [2] -->  Noise magnitude:  float
     */
    private Number[] fskBitAnalyze(FloatBuffer sampleBuf, int pSamples, int nSamples, String pre) {
        Number[] res = new Number[3];

        float[] fftbuf = new float[fftSize*2];
        sampleBuf.position(pSamples);
        sampleBuf.get(fftbuf, 0, nSamples);
        fft.realForwardFull(fftbuf);

        float magScalar = 2.0f / nSamples;
        float magMark = bandMag(fftbuf, bMark, magScalar);
        float magSpace = bandMag(fftbuf, bSpace, magScalar);
        // mark==1, space==0
        if(magMark > magSpace) {
            res[0] = 1;                 // bitOutp
            res[1] = magMark;           // bitSignalMagOutp
            res[2] = magSpace;          // bitNoiseMagOutp
        } else {
            res[0] = 0;                 // bitOutp
            res[1] = magSpace;          // bitNoiseMagOutp
            res[2] = magMark;           // bitSignalMagOutp
        }
        fLogger.debug("%s\t%.2f  %.2f  %s  bit=%d sig=%.2f noise=%.2f", pre, magMark, magSpace,
                magMark > magSpace ? "mark      ":"     space",
                res[0], res[1], res[2]);

        return res;
    }

    /**
     * returns confidence value [0.0 to INFINITY]
     */
    private Number[] fskFrameAnalyze(FloatBuffer sampleBuf,
                                     int pSamples,
                                     float samplesPerBit,
                                     int nBits,
                                     byte[] expectBitsString) {

        Number[] res = new Number[3];
        // Initialize with failure return values
        res[0] = 0.0f;              // confidence
        res[1] = 0L;                // bitsOutp
        res[2] = 0.0f;              // amplOutp;

        int bitNSamples = (int)(samplesPerBit + 0.5f);
        int[] bitValues = new int[64];
        float[] bitSigMags = new float[64];
        float[] bitNoiseMags = new float[64];
        int bitBeginSample;
        int bitnum;

        /* pass #1 - process and check only the "required" (1/0) expect_bits */
        for(bitnum = 0; bitnum < nBits; bitnum++) {
            if (expectBitsString[bitnum] == 'd') {
                continue;
            }
            assert expectBitsString[bitnum] == '1' || expectBitsString[bitnum] == '0';

            bitBeginSample = pSamples + (int) (samplesPerBit * bitnum + 0.5f);
            String pre = String.format(" bit# %2d @ %7d: ", bitnum, bitBeginSample);

            Number[] baRes = fskBitAnalyze(sampleBuf,bitBeginSample, bitNSamples, pre);
            bitValues[bitnum] = (int) baRes[0];
            bitSigMags[bitnum] = (float) baRes[1];
            bitNoiseMags[bitnum] = (float) baRes[2];

            if (expectBitsString[bitnum] - '0' != bitValues[bitnum]) {
                return res; /* does not match expected; abort frame analysis. */
            }

            if (bitSigMags[bitnum] / bitNoiseMags[bitnum] < FSK_MIN_BIT_SNR) {
                return res;
            }

            // Performance hack: reject frame early if sig mag isn't even half
            // of FSK_MIN_MAGNITUDE
            if (bitSigMags[bitnum] < FSK_MIN_MAGNITUDE / 2.0) {
                return res; // too weak; abort frame analysis
            }
        }
        /* pass #2 - process only the dontcare ('d') expect_bits */
        for(bitnum = 0; bitnum < nBits; bitnum++) {
            if(expectBitsString[bitnum] != 'd') {
                continue;
            }
            bitBeginSample = pSamples + (int)(samplesPerBit * bitnum + 0.5f);
            String pre = String.format(" bit# %2d @ %7d: ", bitnum, bitBeginSample);
            Number[] resBA = fskBitAnalyze(sampleBuf, bitBeginSample, bitNSamples, pre);
            bitValues[bitnum] = (int) resBA[0];
            bitSigMags[bitnum] = (float) resBA[1];
            bitNoiseMags[bitnum] = (float) resBA[2];

            if(bitSigMags[bitnum] / bitNoiseMags[bitnum] < FSK_MIN_BIT_SNR) {
                return res;
            }
        }

        float confidence;
        float totalBitSig = 0.0f, totalBitNoise = 0.0f;
        float avgMarkSig = 0.0f, avgSpaceSig = 0.0f;
        int nMark = 0, nSpace = 0;

        for(bitnum = 0; bitnum < nBits; bitnum++) {
            // Deal with floating point data type quantization noise...
            // If total_bit_noise <= FLT_EPSILON, then assume it to be 0.0,
            // so that we end up with snr==inf.
            totalBitSig += bitSigMags[bitnum];
            if(bitNoiseMags[bitnum] > Float.MIN_VALUE) {
                totalBitNoise += bitNoiseMags[bitnum];
            }

            if(bitValues[bitnum] == 1) {
                avgMarkSig += bitSigMags[bitnum];
                nMark++;
            } else {
                avgSpaceSig += bitSigMags[bitnum];
                nSpace++;
            }
        }

        // Compute the "frame SNR"
        float snr = totalBitSig / totalBitNoise;

        // Compute avg bit sig and noise magnitudes
        float avgBitSig = totalBitSig / nBits;

        // Compute separate avg bit sig for mark and space
        if(nMark != 0) {
            avgMarkSig /= Integer.toUnsignedLong(nMark);
        }
        if(nSpace != 0) {
            avgSpaceSig /= Integer.toUnsignedLong(nSpace);
        }
        // Compute average "divergence": bit_mag_divergence / other_bits_mag
        float divergence = 0.0f;
        for(bitnum = 0; bitnum < nBits; bitnum++) {
            float avgBitSigOther;
            avgBitSigOther = bitValues[bitnum] != 0 ? avgMarkSig : avgSpaceSig;
            divergence += Math.abs(bitSigMags[bitnum] - avgBitSigOther) / avgBitSigOther;
        }
        divergence *= 2;

        divergence /= nBits;
        float avgBitNoise = totalBitNoise / nBits;
        fLogger.debug("    divg=%.3f snr=%.3f avg{bit_sig=%.3f bit_noise=%.3f(%s)}",
                    divergence, snr, avgBitSig, avgBitNoise, avgBitNoise == 0.0 ? "zero" : "non-zero");

        if(avgBitSig < FSK_MIN_MAGNITUDE) {
            return res; // too weak; reject frame
        }

        // Frame confidence is the frame ( SNR * consistency )
        confidence = snr * (1.0f - divergence);

        // least significant bit first ... reverse the bits as we place them
        // into the bits_outp word.
        long bitsOutp = 0;
        for(bitnum = 0; bitnum < nBits; bitnum++) {
            bitsOutp = bitsOutp | Integer.toUnsignedLong(bitValues[bitnum]) << bitnum;
        }
            fLogger.debug("    frame algo=%d confidence=%f ampl=%f", CONFIDENCE_ALGO, confidence, avgBitSig);

        res[0] = confidence;
        res[1] = bitsOutp;
        res[2] = avgBitSig;
        return res;
    }

    public Number[] fskFindFrame(FloatBuffer sampleBuf,
                                 int frameNSamples,
                                 int tryFirstSample,
                                 int tryMaxNsamples,
                                 int tryStepNsamples,
                                 float tryConfidenceSearchLimit,
                                 byte[] expectBitsString) {

        Number[] res = new Number[4];
        // Initialize with failure return values
        res[0] = 0.0f;              // confidence
        res[1] = 0L;                // bitsOutp
        res[2] = 0.0f;              // amplOutp;
        res[3] = 0;                 // frameStartOutp;

        int expectNBits = 0;
        while (expectBitsString[expectNBits]!=0 && expectNBits<64) {
            expectNBits++;
        }
        assert expectNBits <= 64; // protect fsk_frame_analyze()
        float samplesPerBit = frameNSamples / expectNBits;
        // tryStepNsamples = 1;	// pedantic TEST
        int bestT = 0;
        float bestC = 0.0f, bestA = 0.0f;
        long bestBits = 0;
        // Scan the frame positions starting with the one try_first_sample,
        // alternating between a step above that, a step below that, above, below,
        // and so on, until we've scanned the whole try_max_nsamples range.
        for (int j = 0; ; j++) {
            int up = j % 2 != 0 ? 1 : -1;
            int t = tryFirstSample + up * ((j + 1) / 2) * tryStepNsamples;
            if (t >= tryMaxNsamples) {
                break;
            }
            if (t < 0) {
                continue;
            }

            fLogger.debug("try fsk_frame_analyze at t=%d", t);
            Number[] resFA = fskFrameAnalyze(sampleBuf, t, samplesPerBit, expectNBits, expectBitsString);
            float c = (float) resFA[0];
            long bitsOut = (long) resFA[1];
            float amplOut = (float) resFA[2];
            if (bestC < c) {
                bestT = t;
                bestC = c;
                bestA = amplOut;
                bestBits = bitsOut;
                // If we find a frame with confidence > try_confidence_search_limit
                // quit searching.
                if (bestC >= tryConfidenceSearchLimit) {
                    break;
                }
            }
        }
        /*
         * FIXME? hardcoded chop off framing bits for debug
         * Hmmm... we have now way to  distinguish between:
         *     8-bit data with no start/stopbits == 8 bits
         *     5-bit with prevstop+start+stop == 8 bits
         */
        byte byteChar = (expectNBits==11)?
                    (byte) (bestBits >>> 2 & 0xFF):
                    (byte) (bestBits & 0xFF);
        StringBuilder frm = new StringBuilder("FSK_FRAME bits='");
        for (int j = 0; j < expectNBits; j++) {
                frm.append((bestBits >>> j & 1) != 0 ? '1' : '0');
        }
        frm.append("' datum='").
                    append((Character.isISOControl(byteChar) || Character.isSpaceChar(byteChar)) ? '.' : byteChar).
                    append("'");
        fLogger.debug("%s (0x%02x)   c=%f  a=%f  t=%d", frm.toString(), byteChar, bestC, bestA, bestT);

        res[0] = bestC;
        res[1] = bestBits;
        res[2] = bestA;
        res[3] = bestT;

        return res;
    }

    static float bandMag(float[] cplr, int band, float scalar )
    {
        float re = cplr[2*band];
        float im = cplr[2*band+1];
        float mag = (float) (Math.sqrt(re*re+im*im) * scalar);
        return mag;
    }
    public int getFftSize() {
        return fftSize;
    }

    public int getnBands() {
        return nBands;
    }

    public int getbMark() {
        return bMark;
    }

}
