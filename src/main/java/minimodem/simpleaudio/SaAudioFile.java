/*
 * minimodem4j
 * SaAudioFile.java
 */
package minimodem.simpleaudio;

import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.sound.sampled.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import static minimodem.simpleaudio.SaDirection.*;

/**
 * Audio file abstraction
 */
public class SaAudioFile extends SimpleAudio {
    private static final Logger fLogger = LogManager.getFormatterLogger(SaAudioFile.class);

    protected SaDirection direction;
    protected AudioFileFormat.Type type = null;
    protected File fTmpOut = null;
    protected File file = null;
    protected FileChannel fTmpChannel = null;
    protected AudioInputStream sIn = null;
    protected int bytesPerFrame = 1;

    /**
     * Opens file
     * @param f File object to open
     * @param enc encoding (Java sampled audio AudioFormat.Encoding)
     * @param dir operation direction (PLAYBACK or RECORD)
     * @param sampleRate  sample rate
     * @param nChannels  the number of channels (actually only 1 is supported)
     * @param bfskMsbFirst  Big endian flag. This shall match audio file format specification
     * @return true on success, false on error
     */
    public boolean open(File f, AudioFormat.Encoding enc, SaDirection dir,
                        int sampleRate, int nChannels, boolean bfskMsbFirst)  {
        if (f==null) {
            fLogger.error("No file specified.");
            return false;
        }
        clean();
        direction = dir;
        file = f;

        if (!super.open(enc, dir, sampleRate, nChannels, bfskMsbFirst)) {
            return false;
        }

        String ext = FilenameUtils.getExtension(f.getName());
        AudioFileFormat.Type[] types = AudioSystem.getAudioFileTypes();
        for (AudioFileFormat.Type value : types) {
            if (ext.equalsIgnoreCase(value.getExtension())) {
                type = value;
                break;
            }
        }
        if (type == null) {
            fLogger.error("Audio file extension '%s' is not supported", ext);
            return false;
        }

        if (direction == SA_TRANSMIT) {
            try {
                fTmpOut = File.createTempFile("minimodem-", ".tmp");
                fTmpChannel = new FileOutputStream(fTmpOut, false).getChannel();
            } catch (IOException e) {
                fLogger.error("Failed to create temporary buffer file '%s': [%s]", fTmpOut.getPath(), e.getMessage());
                clean();
                return false;
            }
        } else {
            try {
                sIn = AudioSystem.getAudioInputStream(f);
 /*               if (!sIn.getFormat().getEncoding().equals(enc)) {
                    fLogger.error("Failed to open audio file '%s' due to encoding mismatch: actual '%s', requested: '%s'",
                            f.getPath(), sIn.getFormat().getEncoding().toString(), enc.toString());
                    clean();
                    return false;
                } */
                bytesPerFrame = sIn.getFormat().getFrameSize();
                if (bytesPerFrame == AudioSystem.NOT_SPECIFIED) {
                    // some audio formats may have unspecified frame size
                    // in that case we may read any amount of bytes
                    bytesPerFrame = 1;
                }
            } catch (Exception e) {
                fLogger.error("Failed to open audio file '%s': [%s]", f.getPath(), e.getMessage());
                clean();
                return false;
            }
        }
        return true;
    }

    /**
     * Close file, cleans associated resources
     * For SA_TRANSMIT mode it also means repackaging of raw sound buffer into appropriate file format
     */
    public void close()
    {
        if (direction == SA_TRANSMIT) {
            try {
                fTmpChannel.close();
                FileInputStream fInStream = new FileInputStream(fTmpOut);
                AudioInputStream aStream = new AudioInputStream(fInStream, aFormat, fTmpOut.length() / getFramesize());
                AudioSystem.write(aStream, type, file);
                fInStream.close();
            } catch (Exception e) {
                fLogger.error("Failed to write output file '%s': [%s]", file.getPath(), e.getMessage());
            }
        }
        clean();
    }

    /**
     * Writes audio samples to file (actually, to temp. buffer)
     * @param byteBuf   ByteBuffer to write
     * @param nFrames   Number of frames to write
     * @return number of frames written, -1 on error
     */
    public int write(ByteBuffer byteBuf, int nFrames) {
        int res = -1;
        if (direction == SA_RECEIVE) {
            fLogger.error("Cannot read from file '%s' which is open for playback", file.getPath());
        } else {
            byteBuf.rewind();
            try {
                res = fTmpChannel.write(byteBuf);
                if (res>0) {
                    res /= getFramesize();
                }
            } catch (IOException e) {
                fLogger.error("Failed to write to temporary buffer file '%s': [%s]", fTmpOut.getPath(), e.getMessage());
            }
        }
        return res;
    }

    /**
     * Reads audio samples from file
     * @param byteBuf  ByteBuffer to store samples
     * @param pFrames  The first frame to read
     * @param nFrames  Maximum number of frames to read
     * @return  >0   OK, number of samples red
     *          ==0  OK, EOF reached
     *          -1   Error
     */
    public int read(ByteBuffer byteBuf, int pFrames, int nFrames) {
        int res=-1;
        if (direction == SA_TRANSMIT) {
            fLogger.error("Cannot read from file '%s' which is open for recording", file.getPath());
        } else {
            try {
                res = sIn.read(byteBuf.array(), pFrames*bytesPerFrame, nFrames*bytesPerFrame)/bytesPerFrame;
            } catch (IOException e) {
                fLogger.error("Cannot read from file '%s': [%s] ", file.getPath(), e.getMessage());
                res = -1;
            }
        }
        return res;
    }

    /**
     * Cleans resource associated with a file as smoothly as possible
     * @return false if error have arised, true otherwise
     */
    protected boolean clean() {
        boolean ret = true;

        type = null;
        if (fTmpChannel != null) {
            try {
                fTmpChannel.close();
                fTmpChannel = null;
            } catch (IOException e) {
                fLogger.error("Failed to close temporary buffer file '%s': [%s]", fTmpOut.getPath(), e.getMessage());
                ret = false;
            }
        }
        if (fTmpOut != null) {
            ret = fTmpOut.delete();
            fTmpOut = null;
        }

        if (sIn != null) {
            try {
                sIn.close();
            } catch (IOException e) {
                fLogger.error("Failed to close input audio stream: [%s]", e.getMessage());
                ret = false;
            }
            sIn = null;
        }
        return ret;
    }
}
