import java.util.List;
import java.util.Objects;

public class CommandExecutor {
    private final StorageEngine storage;

    public CommandExecutor(StorageEngine storage) {
        this.storage = Objects.requireNonNull(storage, "storage cannot be a null");
    }

    public String execute(Command command) {
        /**
         * receives a command and execute it
         * @param command the command needed to execute
         * @return result of the command
         */
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
            case "DEL" -> storage.del(args.get(0)) ? "1" : "0";
            default -> throw new IllegalStateException("no handler for command " + command.name());
        };
    }
    }