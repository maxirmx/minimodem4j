/*
 * minimodem4j
 * picocli argument parser helper class
 * Serves "--bandwidth"
 */
package minimodem.arghelpers;

public class BandwidthParameterConsumer extends PositiveFloatParameterConsumer {
    public BandwidthParameterConsumer() {
        super("--bandwidth");
    }
}

