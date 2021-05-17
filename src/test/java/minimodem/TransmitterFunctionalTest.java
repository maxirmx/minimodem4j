package minimodem;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.*;

public class TransmitterFunctionalTest {
    boolean compareFiles(File file1, File file2) {
        if (file1.length() != file2.length()) {
            return false;
        }

        try (InputStream in1 = new BufferedInputStream(new FileInputStream(file1));
             InputStream in2 = new BufferedInputStream(new FileInputStream(file2));) {
            int value1, value2, i;
            i=0;
            do {
                //since we're buffered read() isn't expensive
                value1 = in1.read();
                value2 = in2.read();
                i++;
                if (value1 != value2) {
                    return false;
                }
            } while (value1 >= 0);

            //since we already checked that the file sizes are equal
            //if we're here we reached the end of both files without a mismatch
            return true;
        } catch (Exception ignored) {
        }
        return false;
    }


    private Minimodem setupModem(String fn) {
        Minimodem minimodem = new Minimodem();
        CommandLine cmd = new CommandLine(minimodem);
        final String[] args = {"--tx", "300", "-f", fn};
        cmd.parseArgs(args);
        assert (minimodem.configure()==0);
        return minimodem;
    }

    private void runTest(File fIn, File fSample) {
        String fnOut = fIn.getParent() + "/tmp.wav";
        File fOut = new File(fnOut);
        fOut.deleteOnExit();
        try {
            System.setIn(new FileInputStream(fIn));
        } catch (FileNotFoundException e) {
            assert false;
        }
        Minimodem minimodem = setupModem(fnOut);
        assert (minimodem.transmit()==0);
        assert (compareFiles(fSample, fOut));
    }

    @Test
    public void Test1()  {
        File fIn = new File(this.getClass().getResource("/Test1/test_output.txt").getFile());
        File fSample = new File(this.getClass().getResource("/Test1/test_input.wav").getFile());
        runTest(fIn, fSample);
    }

    @Test
    public void Test2() {
        File fIn = new File(this.getClass().getResource("/Test2/test_output.txt").getFile());
        File fSample = new File(this.getClass().getResource("/Test2/test_input.wav").getFile());
        runTest(fIn, fSample);
    }

//    @Test
//    public void Test3() {
//        File fIn = new File(this.getClass().getResource("/Test3/test_output.txt").getFile());
//        File fSample = new File(this.getClass().getResource("/Test3/test_input.wav").getFile());
//        runTest(fIn, fSample);
//    }

    @Test
    public void Test4() {
        File fIn = new File(this.getClass().getResource("/Test4/test_output.txt").getFile());
        File fSample = new File(this.getClass().getResource("/Test4/test_input.wav").getFile());
        runTest(fIn, fSample);
    }

}
