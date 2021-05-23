/*
 * minimodem4j
 * SimpleAudio.java
 * Analog for simpleaudio.c, simpleaudio.h @ https://github.com/kamalmostafa/minimodem
 */
package minimodem.simpleaudio;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.sound.sampled.AudioFormat;
import java.nio.ByteBuffer;

import static javax.sound.sampled.AudioFormat.Encoding.PCM_FLOAT;
import static javax.sound.sampled.AudioFormat.Encoding.PCM_SIGNED;

/**
 *  Abstraction of audio device
 */
public abstract class SimpleAudio {
    private static final Logger fLogger = LogManager.getFormatterLogger(SimpleAudio.class);

    protected SaDirection direction;
    protected AudioFormat aFormat = null;
    private float rxNoise;

    /**
     * rxNoise setter
     * @param rxNoise new value
     */
    public void setRxNoise(float rxNoise) {
        this.rxNoise = rxNoise;
    }

    /**
     * Gets backend sample rate
     * @return Backend sample rate as specified by audio format or 0 if audio format os not set up
     */
    public int getRate() {
        if (aFormat == null) {
            return 0;
        } else {
            return (int)aFormat.getSampleRate();
        }
    }

    /**
     * Gets backend frame size
     * @return Backend frame size as specified by audio format or 0 if audio format os not set up
     */
    public int getFrameSize() {
        if (aFormat == null) {
            return 0;
        } else {
            return aFormat.getFrameSize();
        }
    }

    /**
     * Gets backend encoding
     * @return Backend encoding as specified by audio format or 0 if audio format os not set up
     */
    public AudioFormat.Encoding getEncoding() {
        if (aFormat == null) {
            return null;
        } else {
            return aFormat.getEncoding();
        }
    }

    /**
     * Opens audio
     * @param encoding encoding (Java sampled audio AudioFormat.Encoding)
     * @param dir operation direction (PLAYBACK or RECORD)
     * @param sampleRate  sample rate
     * @param nChannels  the number of channels (actually only 1 is supported)
     * @param bfskMsbFirst  Big endian flag. This shall match audio file format specification
     * @return true on success, false on error
     */

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

    /**
     * Reads given number of frames into buffer
     * @param byteBuf  ByteBuffer to store samples
     * @param pFrames  The first frame to read
     * @param nFrames  Maximum number of frames to read
     * @return  >0   OK, number of samples red
     *          ==0  OK, EOF reached or line closed
     *          -1   Error
     */
    abstract public int read(ByteBuffer byteBuf, int pFrames, int nFrames);

    /**
     * Writes audio samples to file (actually, to temp. buffer)
     * @param byteBuf   ByteBuffer to write
     * @param nFrames   Number of frames to write
     * @return number of frames written, -1 on error
     */
    abstract public int write (ByteBuffer byteBuf, int nFrames);

    /**
     * Close file, cleans associated resources
     * For SA_TRANSMIT mode it also means repackaging of raw sound buffer into appropriate file format
     */
    abstract public void close();

}
