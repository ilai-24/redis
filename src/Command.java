import java.util.*;
/**
 * A parsed, validated command.
 * @param name the command name, normalized to upper case ("GET", "SET", "DEL", "INCR" or "PING")
 * @param args the command arguments, in order; never null and never modifiable
 */
public record Command(String name, List<String> args) {
    public Command{
        args=List.copyOf(args);
    }
}
