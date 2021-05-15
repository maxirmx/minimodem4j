package minimodem.simpleaudio;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import static javax.sound.sampled.AudioFormat.Encoding.PCM_FLOAT;
import static javax.sound.sampled.AudioFormat.Encoding.PCM_SIGNED;

public class SimpleToneGenerator {
    private static final Logger fLogger = LogManager.getFormatterLogger(SimpleToneGenerator.class);

    private static float toneMag = 1.0f;
    private static int sinTableLen;
    private static short[] sinTableShort;
    private static float[] sinTableFloat;
    private float saToneCphase = 0.0f;

    /**
     * lroundf
     * ( Halfway values are rounded away from zero )
     *
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
     * in: turns (0.0 to 1.0)    out: (-32767 to +32767)
     */
    private static short sinLuShort(float turns) {
        int t = (int) (sinTableLen * turns + 0.5f);
        t = Integer.remainderUnsigned(t, sinTableLen);
        return sinTableShort[t];
    }

    /**
     * in: turns (0.0 to 1.0)    out: -1.0 to +1.0
     */
    private static float sinLuFloat(float turns) {
        int t = (int) (sinTableLen * turns + 0.5f);
        t = Integer.remainderUnsigned(t, sinTableLen);
        return sinTableFloat[t];
    }

    public static float turnsToRadians(float t) {
        return (float) Math.PI * 2 * t;
    }


    public void Init(int newSinTableLen, float mag) {
        sinTableLen = newSinTableLen;
        toneMag = mag;

        if (sinTableLen != 0) {
            sinTableShort = new short[sinTableLen];
            sinTableFloat = new float[sinTableLen];

            short magS = (short) (32767.0f * toneMag + 0.5f);
            if (toneMag > 1.0f) {  /* clamp to 1.0 to avoid overflow */
                magS = 32767;
            }
            if (Short.toUnsignedInt(magS) < 1) { /* "short epsilon" */
                magS = 1;
            }
            for (int i = 0; i < sinTableLen; i++) {
                sinTableShort[i] = lroundf(magS * (float) Math.sin((float) Math.PI * 2 * i / sinTableLen));
            }
            for (int i = 0; i < sinTableLen; i++) {
                sinTableFloat[i] = toneMag * (float) Math.sin((float) Math.PI * 2 * i / sinTableLen);
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

    public float sinePhaseTurns(int i, float waveNsamples) {
        return (float) i / waveNsamples + saToneCphase;
    }

    public float sinePhaseRadians(int i, float waveNsamples) {
        return turnsToRadians(sinePhaseTurns(i, waveNsamples));
    }

    public void Tone(SimpleAudio saOut, float toneFreq, int nsamplesDur) {
        int framesize = saOut.getBackendFramesize();
        ByteBuffer byteBuf = ByteBuffer.allocateDirect(nsamplesDur * framesize);
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
                        floatBuf.put((float) (toneMag * Math.sin(sinePhaseRadians(i, waveNsamples))));
                    }
                }
            } else if (saOut.getEncoding().equals(PCM_SIGNED)) {
                    if (sinTableShort != null) {
                        for (i = 0; i < nsamplesDur; i++) {
                            shortBuf.put(sinLuShort(sinePhaseTurns(i, waveNsamples)));
                        }
                    } else {
                        short magS = (short) (32767.0f * toneMag + 0.5f);
                        if (toneMag > 1.0f) { /* clamp to 1.0 to avoid overflow */
                            magS = 32767;
                        }
                        if (magS < 1) { /* "short epsilon" */
                            magS = 1;
                        }
                        for (i = 0; i < nsamplesDur; i++) {
                            shortBuf.put(lroundf((float) (magS * Math.sin(sinePhaseRadians(i, waveNsamples)))));
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
        assert ( saOut.write(byteBuf, nsamplesDur) > 0 );

    }
}




