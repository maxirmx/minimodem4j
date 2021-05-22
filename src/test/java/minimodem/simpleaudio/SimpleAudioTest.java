package minimodem.simpleaudio;


import static javax.sound.sampled.AudioFormat.Encoding.PCM_FLOAT;
import static javax.sound.sampled.AudioFormat.Encoding.PCM_SIGNED;
import static minimodem.simpleaudio.SaDirection.*;
import java.io.File;
import java.nio.ByteBuffer;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for minimodem.simpleaudio
 */

public class SimpleAudioTest {
    @Test
    public void AfOpenTstWrongTypeW(){
        SaAudioFile f = new SaAudioFile();
        assert (!f.open(new File("some.file"), PCM_FLOAT, SA_TRANSMIT, 48000, 1, false));
    }

    @Test
    public void AfOpenTstNoFileR(){
        SaAudioFile f = new SaAudioFile();
        assert (!f.open(new File("some.file"), PCM_FLOAT, SA_RECEIVE, 48000, 1, false));
    }

    @Test
    public void AfOpenCleanTstW(){
        SaAudioFile f = new SaAudioFile();
        assert (f.open(new File("some.wav"),PCM_FLOAT, SA_TRANSMIT,48000, 1, false));
        assert (f.fTmpOut != null);
        assert (f.fTmpChannel != null);
        assert (f.clean());
        assert (f.fTmpOut == null);
        assert (f.fTmpChannel == null);
    }

    @Test
    public void AfOpenCleanTstR(){
        SaAudioFile f = new SaAudioFile();
        assert (f.open(new File(this.getClass().getResource("/Test3/test_input.wav").getFile()),
                PCM_SIGNED, SA_RECEIVE,48000, 1, false));
        assert (f.sIn != null);
        assert (f.clean());
        assert (f.sIn == null);
        f.close();
    }

    @Test
    public void AfNaiveWRTst() {
        SaAudioFile f = new SaAudioFile();
        File fOut = new File("some.wav");
        fOut.deleteOnExit();
        assert (f.open(fOut, PCM_FLOAT, SA_TRANSMIT, 48000, 1, false));
        ByteBuffer buf = ByteBuffer.allocate(4);
        buf.put((byte) '1').put((byte) '2').put((byte) '3').put((byte) '4');
        assert (f.write(buf, 0) == 4);
        f.close();
        assert (f.open(fOut, PCM_FLOAT, SA_RECEIVE, 48000, 1, false));
        buf.rewind();
        assert (f.read(buf, 1) == 1);
        assert (f.read(buf, 1) == 0);
        f.close();
        String rs = new String(buf.array());
        assert rs.equals("1234");
    }
}
