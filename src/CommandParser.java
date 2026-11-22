import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Parses one line of text into a {@link Command}.
 *
 * <p>Supported commands:
 * <pre>
 *   GET key
 *   SET key value      (value is the rest of the line, spaces allowed)
 *   DEL key
 * </pre>
 * Command names are case-insensitive; keys and values are case-sensitive.
 * The shape of each command lives in the {@link #COMMANDS} table, so adding a
 * command means adding one line there.
 */
public class CommandParser {

    public static final int MAX_INPUT_BYTES = 1024 * 1024; // 1MB

    private static final Map<String, CommandSpec> COMMANDS = Map.of(
            "GET", new CommandSpec("GET", 1, false),
            "SET", new CommandSpec("SET", 2, true),
            "DEL", new CommandSpec("DEL", 1, false));

    /**
     * Parses a raw text line into a normalized {@link Command}.
     *
     * @param rawInput raw line received from the client
     * @return parsed command with an upper-case name and exactly the right number of arguments
     * @throws CommandException if the input is null, empty, too big, an unknown command,
     *                          or has the wrong number of arguments
     */
    public  Command parse(String rawInput) {
        if (rawInput == null) {
            throw new CommandException("input cannot be null");
        }
        // Cheap length check first: chars <= bytes, so a huge string is rejected before we pay for encoding it
        if (rawInput.length() > MAX_INPUT_BYTES
                || rawInput.getBytes(StandardCharsets.UTF_8).length > MAX_INPUT_BYTES) {
            throw new CommandException("input too big");
        }

        String input = rawInput.strip();
        if (input.isEmpty()) {
            throw new CommandException("empty command");
        }

        if (input.indexOf('\r') >= 0 || input.indexOf('\n') >= 0) {
            throw new CommandException("invalid character in command");
        }

        // Split off only the command name and the rest of the line stays untouched so that SET can keep the spaces inside its value.
        int nameEnd = indexOfWhitespace(input);
        String rawName = nameEnd == -1 ? input : input.substring(0, nameEnd);
        String rest = nameEnd == -1 ? "" : input.substring(nameEnd).stripLeading();

        String name = rawName.toUpperCase(Locale.ROOT);
        CommandSpec spec = COMMANDS.get(name);
        if (spec == null) {
            throw new CommandException("unknown command '" + rawName + "'");
        }

        return new Command(name, parseArgs(spec, rest));
    }

    /**
     * Splits {@code rest} into exactly {@code spec.argCount()} arguments.
     * If the spec says so, the last argument takes everything that is left on the line.
     */
    private List<String> parseArgs(CommandSpec spec, String rest) {
        List<String> args = new ArrayList<>(spec.argCount());
        String remaining = rest;

        for (int i = 0; i < spec.argCount(); i++) {
            if (remaining.isEmpty()) {
                throw wrongArgs(spec);
            }
            boolean isLast = i == spec.argCount() - 1;
            if (isLast && spec.lastArgTakesRestOfLine()) {
                args.add(remaining);
                remaining = "";
            } else {
                int end = indexOfWhitespace(remaining);
                args.add(end == -1 ? remaining : remaining.substring(0, end));
                remaining = end == -1 ? "" : remaining.substring(end).stripLeading();
            }
        }

        if (!remaining.isEmpty()) {
            throw wrongArgs(spec);
        }
        return args;
    }

    private static CommandException wrongArgs(CommandSpec spec) {
        return new CommandException(
                "wrong number of arguments for '" + spec.name().toLowerCase(Locale.ROOT) + "' command");
    }

    /** Returns the index of the first whitespace character in {@code s}, or -1 if there is none. */
    private static int indexOfWhitespace(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (Character.isWhitespace(s.charAt(i))) {
                return i;
            }
        }
        return -1;
    }
}
