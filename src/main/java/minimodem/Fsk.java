package minimodem;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Fsk {
    private static final Logger fLogger = LogManager.getFormatterLogger("Fsk");

    private float sampleRate;
    private float fMark;
    private float fSpace;
    private float filterBw;

    private int	  fftSize;
    private int   nBands;
    private float bandWidth;
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
        
        bandWidth = filterBw;

        float fftHalfBw = bandWidth / 2.0f;
        fftSize = (int)((sampleRate + fftHalfBw) / bandWidth);
        nBands = fftSize;

        bMark = (int)((fMark + fftHalfBw) / bandWidth);
        bSpace = ((int)((fSpace + fftHalfBw) / bandWidth);
        if(bMark > nBands || bSpace > nBands) {
            fLogger.error("b_mark=%d or b_space=%d  is invalid (nbands=%d)", bMark, bSpace, nBands);
            return;
        }
        fLogger.debug("### b_mark=%u b_space=%u fftsize=%d"), bMark, bSpace, fftSize);
    }

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
}
