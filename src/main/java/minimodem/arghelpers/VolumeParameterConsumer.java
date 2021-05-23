/*
 * minimodem4j
 * picocli argument parser helper class
 * Serves "--volume"
 */
package minimodem.arghelpers;

import picocli.CommandLine;
import java.util.Stack;

public class VolumeParameterConsumer implements CommandLine.IParameterConsumer {
    public void consumeParameters(Stack<String> args, CommandLine.Model.ArgSpec argSpec,
                                  CommandLine.Model.CommandSpec commandSpec) {

        String arg = args.pop();
        float value = 0.0f;
        if (arg.equals("E")) {
            value = Float.MIN_VALUE;
        } else {
            try {
                value = Float.parseFloat(arg);
            } catch (Exception ignored) {
            }
            if (value <= 0.0f) {
                throw new CommandLine.ParameterException(commandSpec.commandLine(),
                        String.format("Invalid value '%s' for option '--volume': " +
                                "value may be either positive or E for EPSILON.", arg));
            }
        }
        argSpec.setValue(value);
    }
}
