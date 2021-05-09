package minimodem.samsonov.net;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import picocli.CommandLine;

/**
 * Unit test for simple App.
 */
public class CommandLineTest
{
    @Test
    public void shouldHappen() {
        Minimodem minimodem = new Minimodem();
        CommandLine cmd = new CommandLine (minimodem);
        String[] args = {"--tx", "300"};
        CommandLine.ParseResult parseResult = cmd.parseArgs(args);
        assert minimodem.bfskDataRate == 0f;
    }
}

