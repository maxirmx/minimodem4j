package minimodem.samsonov.net;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import picocli.CommandLine;

/**
 * Unit test for simple App.
 */
public class CommandLineTest
{
    private Minimodem processCmdLine(String[] args) {
        Minimodem minimodem = new Minimodem();
        CommandLine cmd = new CommandLine (minimodem);
        CommandLine.ParseResult parseResult = cmd.parseArgs(args);
        return minimodem;
    }

    @Test
    public void shouldHappen() {
        final String[] args = {"--tx", "300"};
        Minimodem minimodem = processCmdLine(args);
        assert minimodem.bfskDataRate == 0f;
    }

    @Test
    public void AutodetectCarrierTest() {
        final String[] args0 = {"--tx", "300"};
        Minimodem minimodem = processCmdLine(args0);
        assert minimodem.carrierAutodetectThreshold == 0.0f;
        final String[] args1 = {"--tx", "-a", "300"};
        minimodem = processCmdLine(args1);
        assert minimodem.carrierAutodetectThreshold == 0.01f;
    }
}

