package minimodem.simpleaudio;

import java.nio.ByteBuffer;

public class SimpleAudio {
    private int rate;
    private int channels;
    //    private final Container<AbstractData> backendHandle = new Container<AbstractData>(this, 1){};
    private int samplesize;
    private int backendFramesize;
    /**
     * only for the sndfile backend
     */
    private float rxnoise;

    protected SaStreamFormat format;
    protected SaDirection direction;

    protected SimpleAudio(SaStreamFormat fmt, SaDirection dir)
    {
        format = fmt;
        direction = dir;
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
