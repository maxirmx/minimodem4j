package minimodem;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import picocli.CommandLine;

/**
 * Unit tests for picocli implementation.
 */
public class CommandLineTest
{
    private Minimodem processCmdLine(String[] args) {
        Minimodem minimodem = new Minimodem();
        CommandLine cmd = new CommandLine (minimodem);
        CommandLine.ParseResult parseResult = cmd.parseArgs(args);
        return minimodem;
    }

// Are we alive ?
    @Test
    public void AliveTest() {
        assert true;
    }

// Autodetect carrier ("--auto-carrier") - set option forces assignment to carrierAutodetectThreshold
    @Test
    public void AutodetectCarrierTest() {
        final String[] args0 = {"--tx", "300"};
        Minimodem minimodem = processCmdLine(args0);
        assert minimodem.carrierAutodetectThreshold == 0.0f;
        final String[] args1 = {"--tx", "-a", "300"};
        minimodem = processCmdLine(args1);
        assert minimodem.carrierAutodetectThreshold == 0.001f;
    }

// txAmplitude ("--volume") parameter tests  - float or 'E' acceptable
    @Test
    public void VolumeTest() {
        // #0 default value
        final String[] args0 = {"--tx", "300"};
        Minimodem minimodem = processCmdLine(args0);
        assert minimodem.txAmplitude == 1.0f;
        // #1 EPSILON
        final String[] args1 = {"--tx", "300", "-v", "E"};
        minimodem = processCmdLine(args1);
        assert minimodem.txAmplitude == Float.MIN_VALUE;
        // #2 float value
        final String[] args2 = {"--tx", "300", "--volume", "2"};
        minimodem = processCmdLine(args2);
        assert minimodem.txAmplitude == 2.0f;
        // #3 Invalid value
        final String[] args3 = {"--tx", "300", "-v", "X"};
        try {
            processCmdLine(args3);
            assert false;
        } catch(Exception ignored) {
        }
        // #4 Negative value
        final String[] args4 = {"--tx", "300", "-v=-1.0"};
        try {
            processCmdLine(args4);
            assert false;
        } catch(Exception ignored) {
        }
    }

// NStartBits ("--startbits") parameter tests  - bound checks
    @Test
    public void NStartBitsTest() {
        final String[] args1 = {"--tx", "300", "--startbits", "4"};
        Minimodem minimodem = processCmdLine(args1);
        assert minimodem.bfskNStartBits == 4;

        final String[] args2 = {"--tx", "300", "--startbits", "40"};
        try {
            processCmdLine(args2);
            assert false;
        } catch(Exception ignored) {
        }
    }

    // NStopBits ("--stopbits") parameter tests  - bound checks
    @Test
    public void NStopBitsTest() {
        final String[] args1 = {"--tx", "300", "--stopbits", "4.0"};
        Minimodem minimodem = processCmdLine(args1);
        assert minimodem.bfskNStopBits == 4.0f;

        final String[] args2 = {"--tx", "300", "--stopbits", "0.0"};
        minimodem = processCmdLine(args2);
        assert minimodem.bfskNStopBits == 0.0f;

        final String[] args3 = {"--tx", "300", "--stopbits=-4"};
        try {
            processCmdLine(args3);
            assert false;
        } catch(Exception ignored) {
        }
    }

    // Mark Frequency ("--mark") parameter tests  - bound checks
    @Test
    public void MarkFrequencyTest() {
        final String[] args1 = {"--tx", "300", "--mark", "400.0"};
        Minimodem minimodem = processCmdLine(args1);
        assert minimodem.bfskMarkF == 400.0f;

        final String[] args2 = {"--tx", "300", "--mark", "0.0"};
        try {
            processCmdLine(args2);
            assert false;
        } catch(Exception ignored) {
        }

        final String[] args3 = {"--tx", "300", "--mark=-400.0"};
        try {
            processCmdLine(args3);
            assert false;
        } catch(Exception ignored) {
        }
    }

    // Space Frequency ("--space") parameter tests  - bound checks
    @Test
    public void SpaceFrequencyTest() {
        final String[] args1 = {"--tx", "300", "--space", "400.0"};
        Minimodem minimodem = processCmdLine(args1);
        assert minimodem.bfskSpaceF == 400.0f;

        final String[] args2 = {"--tx", "300", "--space", "0.0"};
        try {
            processCmdLine(args2);
            assert false;
        } catch(Exception ignored) {
        }

        final String[] args3 = {"--tx", "300", "--space=-400.0"};
        try {
            processCmdLine(args3);
            assert false;
        } catch(Exception ignored) {
        }
    }

    // Sample rate ("--samplerate") parameter tests  - bound checks
    @Test
    public void SampleRateTest() {
        final String[] args0 = {"--tx", "300"};
        Minimodem minimodem = processCmdLine(args0);
        assert minimodem.sampleRate == 48000;

        final String[] args1 = {"--tx", "300", "--samplerate", "24000"};
        minimodem = processCmdLine(args1);
        assert minimodem.sampleRate == 24000;

        final String[] args2 = {"--tx", "300", "--samplerate=-400"};
        try {
            processCmdLine(args2);
            assert false;
        } catch(Exception ignored) {
        }
    }

    // USOS ("--usos") parameter tests
    @Test
    public void USOSTest() {
        final String[] args0 = {"--tx", "300"};
        Minimodem minimodem = processCmdLine(args0);
        assert minimodem.baudotUSOS;

        final String[] args1 = {"--tx", "300", "--usos", "1"};
        minimodem = processCmdLine(args1);
        assert minimodem.baudotUSOS;

        final String[] args2 = {"--rx", "300", "--usos", "0"};
        minimodem = processCmdLine(args2);
        assert (!minimodem.baudotUSOS);

        final String[] args3 = {"--tx", "300", "--usos=2"};
        try {
            processCmdLine(args3);
            assert false;
        } catch (Exception ignored)
        {
        }

    }
}

