package minimodem.simpleaudio;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;


public class AudioFile extends SimpleAudio {
    public AudioFile(SaStreamFormat fmt, String fileName) {
        super(fmt);
    }
}
