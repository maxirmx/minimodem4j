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

import static javax.sound.sampled.AudioFormat.Encoding.PCM_SIGNED;
import static minimodem.simpleaudio.SaDirection.*;


public class AudioFile extends SimpleAudio {
    private static final Logger fLogger = LogManager.getFormatterLogger("AudioFile");

    protected AudioFileFormat.Type type = null;
    protected File fTmpOut = null;
    protected File fOut = null;
    protected FileChannel fTmpChannel = null;

    public AudioFile(SaStreamFormat fmt, SaDirection dir) {
        super(fmt, dir);
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
    public boolean close()
    {
        try {
            fTmpChannel.close();
            FileInputStream fStream = new FileInputStream(fTmpOut);
            AudioFormat format =
                    new AudioFormat(PCM_SIGNED, 44100, 8, 1, 1, 44100, false);
            AudioInputStream aStream = new AudioInputStream(fStream, format, 100);
            AudioSystem.write(aStream, type, fOut);
        } catch (IOException ex) {

        }


        return clean();
    }

    public boolean open(File f) {
        close();
        String ext = FilenameUtils.getExtension(f.getName());
        AudioFileFormat.Type types[] = AudioSystem.getAudioFileTypes();
        for (int i = 0; i<types.length; i++) {
            if (ext.equalsIgnoreCase(types[i].getExtension())) {
                type = types[i];
                break;
            }
        }
        if (type == null) {
            fLogger.fatal("Audio file extension '%s' is not supported", ext);
            return false;
        }
        if (direction == SA_STREAM_RECORD) {
            try {
                fTmpOut = File.createTempFile("minimodem-", ".tmp");
                fTmpChannel = new FileOutputStream(fTmpOut, false).getChannel();
            } catch (IOException ex) {
                fLogger.fatal("Failed to create temporary buffer file '%s'", fTmpOut.getPath());
                clean();
                return false;
            }
        }
        return true;
    }

    int write (ByteBuffer byteBuf, int nframes ) {
        int ret = 0;
        byteBuf.flip();
        try {
            ret = fTmpChannel.write(byteBuf);
        } catch (IOException ex) {
            fLogger.error("Failed to write to temporary buffer file '%s'", fTmpOut.getPath());
        }
        return ret;
    }

}
