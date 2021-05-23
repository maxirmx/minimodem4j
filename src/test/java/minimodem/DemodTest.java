package minimodem;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.*;
import java.nio.charset.StandardCharsets;

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

    private void runTest(String td){
        genTestDataF("tmp.wav", td);
        Minimodem modem = setupRxModem("tmp.wav");
        assert (modem.configure()==0);
        PrintStream originalOut = System.out;
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));
        modem.receive();
        assertEquals(td, outContent.toString());
        System.setOut(originalOut);
    }

    @Test
    public void rxDemodTest1() {
        runTest("a");
    }

    @Test
    public void rxDemodTest2() {
        runTest("12345 vyshel zaychik pogulyat");
    }

    @Test
    public void rxDemodTestN() {
        runTest("12345 вышел зайчик погулять");
    }
    @Test
    public void rxDemodTestM() {
        runTest("’");
    }

}