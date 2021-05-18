/**
 * minimodem4j
 * picocli argument parser helper class
 * Serves "--auto-carrier"
 */
package minimodem.arghelpers;

import java.util.Stack;
import picocli.CommandLine;

public class AutoDetectCarrierParameterConsumer implements CommandLine.IParameterConsumer {
    public void consumeParameters(Stack<String> args, CommandLine.Model.ArgSpec argSpec,
                                  CommandLine.Model.CommandSpec commandSpec) {
        argSpec.setValue(0.001f);
    }
}