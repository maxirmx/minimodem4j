package minimodem.simpleaudio;

import java.nio.ByteBuffer;

public class SimpleAudio {
    private SaStreamFormat format;
    private int rate;
    private int channels;
    //    private final Container<AbstractData> backendHandle = new Container<AbstractData>(this, 1){};
    private int samplesize;
    private int backendFramesize;
    /**
     * only for the sndfile backend
     */
    private float rxnoise;

    public SimpleAudio() {
    }

    public SaStreamFormat getFormat() {
        return format;
    }
    public int getRate() {
        return rate;
    }
    public int getBackendFramesize() {
        return backendFramesize;
    }

    int read(ByteBuffer byteBuf, int nframes ) {
        return 0;
    }

    int write (ByteBuffer byteBuf, int nframes ) {
        return 0;
    }
}
