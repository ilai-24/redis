/**
 * Thrown when a raw input line is not a valid command.
 * The message is plain text; the network layer decides how to present it
 * to the client (for example by prefixing it with "-ERR ").
 */
public class CommandException extends RuntimeException {

    public CommandException(String message) {
        super(message);
    }
}
