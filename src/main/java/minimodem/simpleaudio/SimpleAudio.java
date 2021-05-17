package minimodem.simpleaudio;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.sound.sampled.AudioFormat;
import java.nio.ByteBuffer;

import static javax.sound.sampled.AudioFormat.Encoding.PCM_FLOAT;
import static javax.sound.sampled.AudioFormat.Encoding.PCM_SIGNED;
import static minimodem.simpleaudio.SaDirection.SA_STREAM_RECORD;

public abstract class SimpleAudio {
    private static final Logger fLogger = LogManager.getFormatterLogger(SimpleAudio.class);

    protected SaDirection direction;
    protected AudioFormat aFormat = null;

    /**
     * only for the sndfile backend
     */
    private float rxnoise;


    public int getRate() {
        if (aFormat == null) {
            return 0;
        } else {
            return (int)aFormat.getSampleRate();
        }
    }

    public int getBackendFramesize() {
        if (aFormat == null) {
            return 0;
        } else {
            return aFormat.getFrameSize();
        }
    }

    public AudioFormat.Encoding getEncoding() {
        if (aFormat == null) {
            return null;
        } else {
            return aFormat.getEncoding();
        }
    }

    public boolean open(AudioFormat.Encoding encoding, SaDirection dir, int sampleRate, int nChannels, boolean bfskMsbFirst) {
        direction = dir;
        if (nChannels != 1) {
            fLogger.error("%d channels are not supported", nChannels);
            return false;
        }
        /*
                https://docs.oracle.com/en/java/javase/15/docs/api/java.desktop/javax/sound/sampled/AudioFormat.Encoding.html
                Two encodings are supported by minimodem:
                - 	PCM_SIGNED	Specifies signed, linear PCM data (samples of type "short")
                - 	PCM_FLOAT	Specifies floating-point PCM data (samples of type "float")

         */
        if (!encoding.equals(PCM_SIGNED) && !encoding.equals(PCM_FLOAT)) {
            fLogger.error("'%s' encoding is not supported", encoding.toString());
            return false;
        }

        aFormat = new AudioFormat(encoding,     /* The audio encoding technique: PCM_SIGNED or PCM_FLOAT */
                sampleRate,                     /* The number of samples per second */
                encoding == PCM_SIGNED ? 16 /* size of short in bits */ :
                                         32 /* size of float in bits */,    /* The number of bits in each sample */
                nChannels,                     /* The number of channels */
                nChannels * (encoding == PCM_SIGNED ? 2: 4), /* The number of bytes in each frame */
                sampleRate/nChannels, /* The number of frames per second */
                bfskMsbFirst);


        return true;
    }
    abstract public int read(ByteBuffer byteBuf, int nframes );

    abstract public int write (ByteBuffer byteBuf, int nframes );
}
