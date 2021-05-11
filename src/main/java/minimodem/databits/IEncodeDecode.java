package minimodem.databits;

public interface IEncodeDecode {
    public int encode(int[] databitsOutp, byte charOut);
    public int decode(byte[] dataout, int dataoutSize, long bits, int nDatabits);
}
