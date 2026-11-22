import java.io.IOException;

public class LineTooLongException extends IOException {
  public LineTooLongException(int maxChars) {
    super("Line exceeded maximum allowed length of " + maxChars + " characters");
  }
}
