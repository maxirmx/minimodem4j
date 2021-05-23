/*
 * minimodem4j
 * picocli argument parser helper class
 * Serves "--mark"
 */
package minimodem.arghelpers;

public class MarkFreqParameterConsumer extends PositiveFloatParameterConsumer {
    public MarkFreqParameterConsumer() {
        super("--mark");
    }
}
