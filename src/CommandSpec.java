/**
 * Describes the shape of a supported command
 *
 * @param name                   the command name in upper case
 * @param argCount               the exact number of arguments the command takes
 * @param lastArgTakesRestOfLine if true, the last argument is everything left on the line,spaces included (used by SET for its value)
 */

public record CommandSpec(String name, int argCount, boolean lastArgTakesRestOfLine) {
}
