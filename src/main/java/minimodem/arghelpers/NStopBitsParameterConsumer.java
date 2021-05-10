package minimodem.arghelpers;

import java.util.Stack;
import picocli.CommandLine;

public class NStopBitsParameterConsumer implements CommandLine.IParameterConsumer {
        public void consumeParameters(@org.jetbrains.annotations.NotNull Stack<String> args, CommandLine.Model.ArgSpec argSpec,
                                      CommandLine.Model.CommandSpec commandSpec) {

            String arg = args.pop();
            float value = 0.0f;
            try {
                value = Float.parseFloat(arg);
            } catch (Exception ignored) {
            }
            if (value < 0.0f) {
                throw new CommandLine.ParameterException(commandSpec.commandLine(),
                        String.format("Invalid value '%s' for option '--stopbits': " +
                                "value may be non-negative float only.", arg));
            }
            argSpec.setValue(value);
        }
}
