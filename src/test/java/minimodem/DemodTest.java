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

public class DemodTest {
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

    private Receiver setupReceiever(String fnIn, Minimodem modem) {
        SaAudioFile saIn = new SaAudioFile();
        assert saIn.open(new File(fnIn),
                modem.floatSamples?PCM_FLOAT:PCM_SIGNED,
                SA_RECEIVE,
                modem.sampleRate,
                modem.nChannels,
                modem.bfskMsbFirst);
        Receiver r = new Receiver(saIn, modem);
        return r;
    }

    @Test
    public void rxDemodTest() {
        genTestData("tmp.wav", "a");
        Minimodem modem = setupRxModem("tmp.wav");
        assert (modem.configure()==0);
        modem.receive();
    }



}