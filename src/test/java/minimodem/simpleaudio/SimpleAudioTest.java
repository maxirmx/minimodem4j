package minimodem.simpleaudio;


import static minimodem.simpleaudio.SaDirection.*;
import static minimodem.simpleaudio.SaStreamFormat.*;
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
        AudioFile f = new AudioFile(SA_SAMPLE_FORMAT_FLOAT, SA_STREAM_RECORD);
        assert (!f.open(new File("some.file")));
    }

    @Test
    public void AfOpenCleanTst(){
        AudioFile f = new AudioFile(SA_SAMPLE_FORMAT_FLOAT, SA_STREAM_RECORD);
        assert (f.open(new File("some.wav")));
        assert (f.fTmpOut != null);
        assert (f.fTmpChannel != null);
        assert (f.clean());
        assert (f.fTmpOut == null);
        assert (f.fTmpChannel == null);
    }

    @Test
    public void AfNaiveWriteTst() {
        AudioFile f = new AudioFile(SA_SAMPLE_FORMAT_FLOAT, SA_STREAM_RECORD);
        f.open(new File("some.wav"));
        ByteBuffer buf = ByteBuffer.allocateDirect(5);
        buf.put((byte) '1').put((byte) '2').put((byte) '3').put((byte) '4');
        assert(f.write(buf, 0) == 4);
        f.close();
    }
}
