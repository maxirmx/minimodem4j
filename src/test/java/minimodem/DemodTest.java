package minimodem;
import minimodem.simpleaudio.SaAudioFile;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.*;
import java.nio.charset.StandardCharsets;

import static javax.sound.sampled.AudioFormat.Encoding.PCM_FLOAT;
import static javax.sound.sampled.AudioFormat.Encoding.PCM_SIGNED;
import static minimodem.simpleaudio.SaDirection.SA_RECEIVE;
import static org.junit.jupiter.api.Assertions.assertEquals;

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

    private Minimodem setupTxModemS(String fn) {
        Minimodem minimodem = new Minimodem();
        CommandLine cmd = new CommandLine(minimodem);
        final String[] args = {"--tx", "300", "-f", fn};
        cmd.parseArgs(args);
        assert (minimodem.configure() == 0);
        return minimodem;
    }


    private void genTestDataF(String fnOut, String data) {
        File fOut = new File(fnOut);
        fOut.deleteOnExit();
        InputStream in = System.in;
        System.setIn(new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8)));
        Minimodem minimodem = setupTxModemF(fnOut);
        assert (minimodem.transmit()==0);
        System.setIn(in);
    }

    private void genTestDataS(String fnOut, String data) {
        File fOut = new File(fnOut);
        fOut.deleteOnExit();
        InputStream in = System.in;
        System.setIn(new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8)));
        Minimodem minimodem = setupTxModemS(fnOut);
        assert (minimodem.transmit()==0);
        System.setIn(in);
    }

    @Test
    public void rxDemodTest1() {
        genTestDataF("tmp.wav", "a");
        Minimodem modem = setupRxModem("tmp.wav");
        assert (modem.configure()==0);
        PrintStream originalOut = System.out;
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));
        modem.receive();
        assertEquals("a", outContent.toString());
        System.setOut(originalOut);
    }

    @Test
    public void rxDemodTest2() {
        String td = "12345 vyshel zaychik pogulyat";
        genTestDataF("tmp.wav", td);
        Minimodem modem = setupRxModem("tmp.wav");
        assert (modem.configure()==0);
        PrintStream originalOut = System.out;
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));
        modem.receive();
        assertEquals(td,outContent.toString());
        System.setOut(originalOut);
    }

    @Test
    public void rxDemodTestM() {
        String td = "Z";
        genTestDataS("tmp.wav", td);
        Minimodem modem = setupRxModem("tmp.wav");
        assert (modem.configure()==0);
        PrintStream originalOut = System.out;
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));
        modem.receive();
        assertEquals(td,outContent.toString());
        System.setOut(originalOut);
    }


}