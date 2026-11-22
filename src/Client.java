import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

    /**
     * A minimal console client for the server, like a tiny redis-cli.
     * <p>Every line you type is sent to the server, and the one-line reply is printed.
     * Type {@code quit} (or {@code exit}) to leave.
     */
    public class Client {

        private static final String HOST = "localhost";
        private static final int PORT = 6380; // must match Server.PORT

        public static void main(String[] args) {
            try (
                    Socket socket = new Socket(HOST, PORT);
                    BufferedReader fromServer = new BufferedReader(
                            new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                    PrintWriter toServer = new PrintWriter(
                            socket.getOutputStream(), true, StandardCharsets.UTF_8);
                    Scanner keyboard = new Scanner(System.in)
            ) {
                System.out.println("Connected to " + HOST + ":" + PORT + ". Type 'quit' to leave.");

                while (true) {
                    System.out.print("> ");
                    if (!keyboard.hasNextLine()) {
                        break; // end of input (Ctrl+D / Ctrl+Z)
                    }
                    String line = keyboard.nextLine();
                    if (line.equalsIgnoreCase("quit") || line.equalsIgnoreCase("exit")) {
                        break;
                    }

                    toServer.println(line);

                    String reply = fromServer.readLine();
                    if (reply == null) {
                        System.out.println("Server closed the connection.");
                        break;
                    }
                    System.out.println(reply);
                }
            } catch (IOException e) {
                System.err.println("Connection error: " + e.getMessage());
            }
        }
    }

