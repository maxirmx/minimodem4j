package minimodem;

import org.junit.jupiter.api.Test;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioSystem;

/**
 * Unit tests for minimodem.databits
 */

public class DatabitsTest {
    @Test
    public void ATest(){
        AudioFileFormat.Type t[] = AudioSystem.getAudioFileTypes();
        for (int i = 0; i<t.length; i++)
            System.out.print(t[i].toString());
    }
}
