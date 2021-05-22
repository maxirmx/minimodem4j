/**
 * minimodem4j
 * SaToneGenerator.java
 * Created from simple-tone-generator.c, simpleaudio.h @ https://github.com/kamalmostafa/minimodem
 */
package minimodem.simpleaudio;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import static java.nio.ByteOrder.nativeOrder;
import static javax.sound.sampled.AudioFormat.Encoding.PCM_FLOAT;
import static javax.sound.sampled.AudioFormat.Encoding.PCM_SIGNED;

public class SaToneGenerator {
    private static final Logger fLogger = LogManager.getFormatterLogger(SaToneGenerator.class);
    /**
     * Precompiled sine table(s)
     * sinTableFloat  -- as floats (-1.0 to 1.0)
     * sinTableShort  -- as shorts (-32767 to +32767)
     * sinTableLen    -- table length
     */
    private int sinTableLen;
    private short[] sinTableShort;
    private float[] sinTableFloat;

    private float toneMagFloat = 1.0f;
    private short toneMagShort = (short) (0.5f);
    private float saToneCphase = 0.0f;

    /**
     * lroundf Equivalent of C99 function - Halfway values are rounded away from zero
     * @param v - a value to round
     * @return rounded value
     */
    private static short lroundf(float v) {
        if (v > 0) {
            return (short) Math.round(v);
        } else {
            return (short) -Math.round(-v);
        }
    }

    /**
     * Table value (approximation) of sine(turns) as signed short
     * @param turns  (0.0 to 1.0)
     * @return sine (-32767 to +32767)
     */
    private short sinLuShort(float turns) {
        int t = (int) (sinTableLen * turns + 0.5f);
        t = Integer.remainderUnsigned(t, sinTableLen);
        return sinTableShort[t];
    }

    /**
     * Table value (approximation) of sine(turns) as float
     * @param turns (0.0 to 1.0)
     * @return -1.0 to +1.0
     */
    private float sinLuFloat(float turns) {
        int t = (int) (sinTableLen * turns + 0.5f);
        t = Integer.remainderUnsigned(t, sinTableLen);
        return sinTableFloat[t];
    }
    /**
     * Convert turns to radians
     * @param turns (0.0 to 1.0)
     * @return radians
     */
    public static float turnsToRadians(float turns) {
        return (float) Math.PI * 2 * turns;
    }

    /**
     * Initialize or drop sine lookup table
     * @param newSinTableLen table size
     * @param mag signal magnitude
     */
    public void toneInit(int newSinTableLen, float mag) {
        sinTableLen = newSinTableLen;
        toneMagFloat = mag;

        if (sinTableLen != 0) {
            sinTableShort = new short[sinTableLen];
            sinTableFloat = new float[sinTableLen];

            toneMagShort = (short) (32767.0f * toneMagFloat + 0.5f);
            if (toneMagFloat > 1.0f) {  /* clamp to 1.0 to avoid overflow */
                toneMagShort = 32767;
            }
            if (Short.toUnsignedInt(toneMagShort) < 1) { /* "short epsilon" */
                toneMagShort = 1;
            }
            for (int i = 0; i < sinTableLen; i++) {
                sinTableShort[i] = lroundf(toneMagShort * (float) Math.sin((float) Math.PI * 2 * i / sinTableLen));
            }
            for (int i = 0; i < sinTableLen; i++) {
                sinTableFloat[i] = toneMagFloat * (float) Math.sin((float) Math.PI * 2 * i / sinTableLen);
            }
        } else {
            if (sinTableShort != null) {
                sinTableShort = null;
            }
            if (sinTableFloat != null) {
                sinTableFloat = null;
            }
        }
    }

    /**
     * Emit tone
     * @param saOut         Audio device
     * @param toneFreq      frequency
     * @param nsamplesDur   duration (the number of samples)
     */
    public void Tone(SimpleAudio saOut, float toneFreq, int nsamplesDur) {
        int framesize = saOut.getFramesize();
        ByteBuffer byteBuf = ByteBuffer.allocateDirect(nsamplesDur * framesize);
        byteBuf.order(nativeOrder());           // Here it shall be native order. Lsb/Msb is handled in the code.
        FloatBuffer floatBuf = byteBuf.asFloatBuffer();
        ShortBuffer shortBuf = byteBuf.asShortBuffer();
        int i;

        if (toneFreq != 0) {
            float waveNsamples = saOut.getRate() / toneFreq;
            if (saOut.getEncoding().equals(PCM_FLOAT)) {
                if (sinTableFloat != null) {
                    for (i = 0; i < nsamplesDur; i++) {
                        floatBuf.put(sinLuFloat(sinePhaseTurns(i, waveNsamples)));
                    }
                } else {
                    for (i = 0; i < nsamplesDur; i++) {
                        floatBuf.put((float) (toneMagFloat * Math.sin(sinePhaseRadians(i, waveNsamples))));
                    }
                }
            } else if (saOut.getEncoding().equals(PCM_SIGNED)) {
                    if (sinTableShort != null) {
                        for (i = 0; i < nsamplesDur; i++) {
                            shortBuf.put(sinLuShort(sinePhaseTurns(i, waveNsamples)));
                        }
                    } else {
                        for (i = 0; i < nsamplesDur; i++) {
                            shortBuf.put(lroundf((float) (toneMagShort * Math.sin(sinePhaseRadians(i, waveNsamples)))));
                        }
                    }
            } else {
                    fLogger.error("Invalid stream format [%s] in processing.", saOut.getEncoding().toString());
            }
            saToneCphase = (saToneCphase + nsamplesDur / waveNsamples) % 1.0f;
        } else {
            for (i = 0; i < nsamplesDur * framesize; i++) {
                byteBuf.put((byte) 0);
            }
            saToneCphase = 0.0f;
        }

        saOut.write(byteBuf, nsamplesDur);
    }

    /**
     *
     * @param i
     * @param waveNsamples
     * @return
     */
    public float sinePhaseTurns(int i, float waveNsamples) {
        return (float) i / waveNsamples + saToneCphase;
    }

    /**
     *
     * @param i
     * @param waveNsamples
     * @return
     */
    public float sinePhaseRadians(int i, float waveNsamples) {
        return turnsToRadians(sinePhaseTurns(i, waveNsamples));
    }

}




