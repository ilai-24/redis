/**
 * Describes the shape of a supported command.
 *
 * @param name                   the command name in upper case
 * @param minArgs                the fewest arguments the command accepts
 * @param maxArgs                the most arguments the command accepts
 * @param lastArgTakesRestOfLine if true, the last argument is everything left on the line, spaces included (used by SET for its value)
 * @throws IllegalArgumentException if {@code minArgs} is negative or bigger than {@code maxArgs}
 */
public record CommandSpec(String name, int minArgs,int maxArgs, boolean lastArgTakesRestOfLine) {
    public CommandSpec{
        if (minArgs<0||minArgs>maxArgs){
            throw new IllegalArgumentException("expected 0 <= minArgs <= maxArgs but got " + minArgs + " and " + maxArgs);
        }
    }
}
