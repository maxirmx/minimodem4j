package minimodem.simpleaudio;

import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import static minimodem.simpleaudio.SaDirection.*;


public class SaAudioFile extends SimpleAudio {
    private static final Logger fLogger = LogManager.getFormatterLogger(SaAudioFile.class);

    protected SaDirection direction;
    protected AudioFileFormat.Type type = null;
    protected File fTmpOut = null;
    protected File fOut = null;
    protected FileChannel fTmpChannel = null;

    public boolean open(File f, AudioFormat.Encoding enc, SaDirection dir, int sampleRate, int nChannels, boolean bfskMsbFirst) {
        clean();
        direction = dir;
        fOut = f;
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

        if (direction == SA_STREAM_RECORD) {
            try {
                fTmpOut = File.createTempFile("minimodem-", ".tmp");
                fTmpChannel = new FileOutputStream(fTmpOut, false).getChannel();
            } catch (IOException ex) {
                fLogger.error("Failed to create temporary buffer file '%s'", fTmpOut.getPath());
                clean();
                return false;
            }
        }

        return super.open(enc, dir, sampleRate, nChannels, bfskMsbFirst);
    }

    public boolean close()
    {
        boolean res = false;
        try {
            fTmpChannel.close();
            FileInputStream fInStream = new FileInputStream(fTmpOut);
            AudioInputStream aStream = new AudioInputStream(fInStream, aFormat, fTmpOut.length());
            AudioSystem.write(aStream, type, fOut);
            fInStream.close();
        } catch (IOException ex) {
            fLogger.error("Failed to write output file '%s' [%s]", fOut.getPath(), ex.getLocalizedMessage());
        } catch (IllegalArgumentException ex) {
            fLogger.error("Failed to write output file '%s [%s]", fOut.getPath(), ex.getLocalizedMessage());
        }
        clean();
        return res;
    }

    public int write(ByteBuffer byteBuf, int nframes) {
        if (direction == SA_STREAM_PLAYBACK) {
            fLogger.error("FCannot read from file '%s' which is open for playback", fOut.getPath());
            return 0;
        }
        int ret = 0;
        byteBuf.flip();
        try {
            ret = fTmpChannel.write(byteBuf);
        } catch (IOException ex) {
            fLogger.error("Failed to write to temporary buffer file '%s'", fTmpOut.getPath());
        }
        return ret;
    }

    public int read(ByteBuffer byteBuf, int nframes) {
        if (direction == SA_STREAM_RECORD) {
            fLogger.error("FCannot read from file '%s' which is open for recording", fOut.getPath());
        }
        return 0;
    }

        protected boolean clean() {
        boolean ret = true;

        type = null;
        if (fTmpChannel != null) {
            try {
                fTmpChannel.close();
                fTmpChannel = null;
            } catch (IOException ex) {
                fLogger.error("Failed to close temporary buffer file '%s'", fTmpOut.getPath());
                ret = false;
            }
        }
        if (fTmpOut != null) {
            fTmpOut.delete();
            fTmpOut = null;
        }
        return ret;

    }

}
