/**
 * minimodem4j
 * picocli argument parser helper class
 * Serves parameters of Float type that shall be positive
 */
package minimodem.arghelpers;

import picocli.CommandLine;
import java.util.Stack;

public class PositiveFloatParameterConsumer implements CommandLine.IParameterConsumer {
    private String name;
    protected PositiveFloatParameterConsumer(String nm) {
        name = nm;
    }
    public void consumeParameters(Stack<String> args, CommandLine.Model.ArgSpec argSpec,
                                  CommandLine.Model.CommandSpec commandSpec) {

        String arg = args.pop();
        float value = 0.0f;
        try {
            value = Float.parseFloat(arg);
        } catch (Exception ignored) {
        }
        if (value <= 0.0f) {
            throw new CommandLine.ParameterException(commandSpec.commandLine(),
                    String.format("Invalid value '%s' for option '%s': " +
                            "value may be positive float only.", arg, name));
        }
        argSpec.setValue(value);
    }
}