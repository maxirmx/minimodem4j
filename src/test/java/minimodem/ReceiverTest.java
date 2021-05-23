package minimodem;
import minimodem.simpleaudio.SaAudioFile;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static javax.sound.sampled.AudioFormat.Encoding.PCM_FLOAT;
import static javax.sound.sampled.AudioFormat.Encoding.PCM_SIGNED;
import static minimodem.simpleaudio.SaDirection.SA_RECEIVE;

public class ReceiverTest {
    private Minimodem setupRxModem(String fn) {
        Minimodem minimodem = new Minimodem();
        CommandLine cmd = new CommandLine(minimodem);
        final String[] args = {"--rx", "300", "-f", fn};
        cmd.parseArgs(args);
        assert (minimodem.configure() == 0);
        return minimodem;
    }

    private Minimodem setupTxModemF(String fn) {
        Minimodem minimodem = new Minimodem();
        CommandLine cmd = new CommandLine(minimodem);
        final String[] args = {"--tx", "300", "-f", fn, "--float-samples"};
        cmd.parseArgs(args);
        assert (minimodem.configure() == 0);
        return minimodem;
    }

    private Minimodem setupTxModemS1(String fn) {
        Minimodem minimodem = new Minimodem();
        CommandLine cmd = new CommandLine(minimodem);
        final String[] args = {"--tx", "300", "-f", fn};
        cmd.parseArgs(args);
        assert (minimodem.configure() == 0);
        return minimodem;
    }


    private void genTestData(String fnOut, String data) {
        File fOut = new File(fnOut);
        fOut.deleteOnExit();
        InputStream in = System.in;
        System.setIn(new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8)));
        Minimodem minimodem = setupTxModemF(fnOut);
        assert (minimodem.transmit()==0);
        System.setIn(in);
    }

    private Receiver setupReceiver(String fnIn, Minimodem modem) {
        SaAudioFile saIn = new SaAudioFile();
        assert saIn.open(new File(fnIn),
                modem.floatSamples?PCM_FLOAT:PCM_SIGNED,
                SA_RECEIVE,
                modem.sampleRate,
                Minimodem.nChannels,
                modem.bfskMsbFirst);
        Receiver r = new Receiver(saIn, modem);
        return r;
    }

    @Test
    public void rxConfigureTest() {
        genTestData("tmp.wav", "a");
        Minimodem modem = setupRxModem("tmp.wav");
        assert (modem.configure()==0);
        Receiver rx = setupReceiver("tmp.wav", modem);
        rx.configure(null);
        assert rx.nSamplesOverscan == 80;
        assert Receiver.byteArray2String(rx.expectDataString).equals("10dddddddd1\\0");
    }

    @Test
    public void rxRefillBufTest1() {
        genTestData("tmp.wav", "a");
        Minimodem modem = setupRxModem("tmp.wav");
        assert (modem.configure()==0);
        Receiver rx = setupReceiver("tmp.wav", modem);
        rx.configure(null);
        rx.refillBuf();
        assert rx.samplesNValid == 2000;
        rx.sampleBuf.put(500,-2.5f);
        rx.shiftSampleBuf(500);
        assert rx.sampleBuf.get(0) == -2.5f;
    }

    @Test
    public void rxRefillBufTest2() {
        genTestData("tmp.wav", "a");
        Minimodem modem = setupRxModem("tmp.wav");
        assert (modem.configure()==0);
        Receiver rx = setupReceiver("tmp.wav", modem);
        rx.configure(null);
        rx.refillBuf();
        assert rx.samplesNValid == 2000;
        rx.advance = 1024+512;
        rx.refillBuf();
        assert rx.samplesNValid == 704;
    /* 2000+240 samples --> 8960 bytes --> 0x2300 bytes (pos 40 = 0x28) of the file */
    }

}