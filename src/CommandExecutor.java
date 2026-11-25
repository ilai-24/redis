import java.util.List;
import java.util.Objects;
/**
 * Executes parsed commands against a {@link StorageEngine}.
 * <p>The executor trusts the parser: it assumes every {@link Command} it receives
 * already has the right number of arguments.
 */
public class CommandExecutor {
    private final StorageEngine storage;

    public CommandExecutor(StorageEngine storage) {
        this.storage = Objects.requireNonNull(storage, "storage cannot be a null");
    }

    /**
     * Executes one command.
     *
     * @param command a command produced by {@link CommandParser}
     * @return the reply to send to the client: {@code OK} for SET, the value or {@code (nil)}
     * for GET, {@code 1} or {@code 0} (deleted or not) for DEL, the new number for INCR,
     * and {@code PONG} for PING (or the message, if PING was given one)
     * @throws CommandException      if INCR is used on a value that is not a whole number,or if the increment would overflow
     * @throws IllegalStateException if the command name has no handler here, which means a command was added to the parser but not to this class
     */

    public String execute(Command command) {
        List<String> args = command.args();
        return switch (command.name()) {
            case "SET" -> {
                storage.set(args.get(0), args.get(1));
                yield "OK";
            }
            case "GET" -> {
                String value = storage.get(args.get(0));
                yield value == null ? "(nil)" : value;
            }
            case "PING" -> args.isEmpty() ? "PONG" : args.get(0);
            case "INCR" -> incr(args.get(0));
            case "DEL" -> storage.del(args.get(0)) ? "1" : "0";
            default -> throw new IllegalStateException("no handler for command " + command.name());
        };
    }

    /** Runs INCR and turns the storage errors into protocol errors, so the storage stays protocol-free. */
    public String incr(String key){
        try {
            return Long.toString(storage.incr(key));
        }catch (NumberFormatException e) {
            throw new CommandException("value is not an integer or out of range");
        } catch (ArithmeticException e) {
            throw new CommandException("increment or decrement would overflow");
        }
    }
    }