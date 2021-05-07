package minimodem.samsonov.net;

public interface EncoderDecoder {
    public int encode(int[] databitsOutp, byte charOut);
    public int decode(byte[] dataout, int dataoutSize, long bits, int nDatabits);
}
