package minimodem.simpleaudio;


import static javax.sound.sampled.AudioFormat.Encoding.PCM_FLOAT;
import static minimodem.simpleaudio.SaDirection.*;
import java.io.File;
import java.nio.ByteBuffer;

import org.junit.jupiter.api.Test;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioSystem;

/**
 * Unit tests for minimodem.simpleaudio
 */

public class SimpleAudioTest {
    @Test
    public void AfOpenTstWrongType(){
        AudioFile f = new AudioFile();
        assert (!f.open(new File("some.file"), PCM_FLOAT, SA_STREAM_RECORD, 48000, 1, false));
    }

    @Test
    public void AfOpenCleanTst(){
        AudioFile f = new AudioFile();
        assert (f.open(new File("some.wav"),PCM_FLOAT, SA_STREAM_RECORD,48000, 1, false));
        assert (f.fTmpOut != null);
        assert (f.fTmpChannel != null);
        assert (f.clean());
        assert (f.fTmpOut == null);
        assert (f.fTmpChannel == null);
    }

    @Test
    public void AfNaiveWriteTst() {
        AudioFile f = new AudioFile();
        f.open(new File("some.wav"), PCM_FLOAT, SA_STREAM_RECORD, 48000, 1, false);
        ByteBuffer buf = ByteBuffer.allocateDirect(5);
        buf.put((byte) '1').put((byte) '2').put((byte) '3').put((byte) '4');
        assert(f.write(buf, 0) == 4);
        f.close();
    }
}
