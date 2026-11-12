import java.util.Locale;

public class CommandParser{
    /**
     * checks if commend is legit
        @param rawInput
        @return String[]tokens if legit exception otherwise
     */
    private static final int MAX_BULK_STRING_SIZE = 10 * 1024 * 1024; // 10 MB

    public String[] parseAndValidate(String rawInput) {
        if (rawInput == null) {
            throw new IllegalArgumentException("Input cannot be null");
        }
        if (rawInput.length() >= MAX_BULK_STRING_SIZE) {
            throw new IllegalArgumentException("Input too big");
        }

        String cleanInput = rawInput.trim();
        if (cleanInput.isEmpty()) {
            throw new IllegalArgumentException("Empty command");
        }

        String[] tokens = cleanInput.split("\\s+");
        String command = tokens[0].toUpperCase(Locale.ROOT);

        switch (command) {
            case "SET":
                if (tokens.length != 3) {
                    throw new IllegalArgumentException("ERR wrong number of arguments for 'set' command");
                }
                break;
            case "GET":
                if (tokens.length != 2) {
                    throw new IllegalArgumentException("ERR wrong number of arguments for 'get' command");
                }
                break;
            case "DEL":
                if (tokens.length != 2) {
                    throw new IllegalArgumentException("ERR wrong number of arguments for 'del' command");
                }
                break;
            default:
                throw new IllegalArgumentException("ERR unknown command '" + tokens[0] + "'");
        }

        return tokens;
    }
}
