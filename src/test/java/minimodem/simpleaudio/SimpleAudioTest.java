package minimodem.simpleaudio;


import static javax.sound.sampled.AudioFormat.Encoding.PCM_FLOAT;
import static minimodem.simpleaudio.SaDirection.*;
import java.io.File;
import java.nio.ByteBuffer;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for minimodem.simpleaudio
 */

public class SimpleAudioTest {
    @Test
    public void AfOpenTstWrongType(){
        SaAudioFile f = new SaAudioFile();
        assert (!f.open(new File("some.file"), PCM_FLOAT, SA_TRANSMIT, 48000, 1, false));
    }

    @Test
    public void AfOpenCleanTst(){
        SaAudioFile f = new SaAudioFile();
        assert (f.open(new File("some.wav"),PCM_FLOAT, SA_TRANSMIT,48000, 1, false));
        assert (f.fTmpOut != null);
        assert (f.fTmpChannel != null);
        assert (f.clean());
        assert (f.fTmpOut == null);
        assert (f.fTmpChannel == null);
    }

    @Test
    public void AfNaiveWriteTst() {
        SaAudioFile f = new SaAudioFile();
        File fOut = new File("some.wav");
        fOut.deleteOnExit();
        assert (f.open(fOut, PCM_FLOAT, SA_TRANSMIT, 48000, 1, false));
        ByteBuffer buf = ByteBuffer.allocateDirect(4);
        buf.put((byte) '1').put((byte) '2').put((byte) '3').put((byte) '4');
        assert (f.write(buf, 0) == 4);
        f.close();
    }
}
