/*
 * minimodem4j
 * picocli argument parser helper class
 * Serves "--usos"
 */
package minimodem.arghelpers;

import picocli.CommandLine;
import java.util.Stack;

public class USoSParameterConsumer implements CommandLine.IParameterConsumer {
    public void consumeParameters(Stack<String> args, CommandLine.Model.ArgSpec argSpec,
                                  CommandLine.Model.CommandSpec commandSpec) {
        String arg = args.pop();
        int usos = 2;
        try {
            usos = Integer.parseInt(arg);
        } catch (Exception ignored) {
        }
        if (usos!=0 && usos!=1) {
            throw new CommandLine.ParameterException(commandSpec.commandLine(),
                    String.format("Invalid value '%s' for option '--usos': " +
                            "value may be 0 or 1", arg));
        }
        boolean val = (usos!=0);
        argSpec.setValue(val);
    }
}
