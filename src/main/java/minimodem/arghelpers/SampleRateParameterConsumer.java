package minimodem.arghelpers;

import picocli.CommandLine;
import java.util.Stack;

public class SampleRateParameterConsumer implements CommandLine.IParameterConsumer {
    public void consumeParameters(Stack<String> args, CommandLine.Model.ArgSpec argSpec,
                                  CommandLine.Model.CommandSpec commandSpec) {
        String arg = args.pop();
        int value = 0;
        try {
            value = Integer.parseInt(arg);
        } catch (Exception ignored) {
        }
        if (value <= 0) {
            throw new CommandLine.ParameterException(commandSpec.commandLine(),
                    String.format("Invalid value '%s' for option '--samplerate': " +
                            "value may be integer > 0.", arg));
        }
        argSpec.setValue(value);
    }
}