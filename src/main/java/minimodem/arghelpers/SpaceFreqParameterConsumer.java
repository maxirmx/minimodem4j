/*
 * minimodem4j
 * picocli argument parser helper class
 * Serves "--space"
 */
package minimodem.arghelpers;

public class SpaceFreqParameterConsumer extends PositiveFloatParameterConsumer{
    protected SpaceFreqParameterConsumer() {
        super("--space");
    }
}
