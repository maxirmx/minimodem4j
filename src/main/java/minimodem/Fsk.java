package minimodem;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;

public class Fsk {
    private static final Logger fLogger = LogManager.getFormatterLogger("Fsk");
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
///	fftwf_plan	fftplan;

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

    static float bandMag(float[][] cplx, int band, float scalar )
    {
        float re = cplx[band][0];
        float im = cplx[band][1];
        float mag = (float) (Math.sqrt(re*re+im*im) * scalar);
        return mag;
    }


    /**
     *
     * @return fftSize
     */
    public int getFftSize() {
        return fftSize;
    }

    public int getnBands() {
        return nBands;
    }



}
