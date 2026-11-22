import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/*
 * A TCP server for the line-based protocol: one command per line in,one reply line out. Every client is served on its own thread and all
 * clients share one {@link StorageEngine}.
*/

public class Server implements AutoCloseable {

    public static final int DEFAULT_PORT = 6380; // to not collide with real Redis

    private static final String EOL = "\r\n";

    private final int port;
    private final int maxLineChars;
    private final CommandParser parser = new CommandParser();
    private final CommandExecutor executor = new CommandExecutor(new StorageEngine());
    private final ExecutorService pool = Executors.newCachedThreadPool();
    private final Set<Socket> clients = ConcurrentHashMap.newKeySet();

    private volatile ServerSocket serverSocket;
    private volatile boolean closed;

    /** A server on {@code port} with the default maximum line length. */
    public Server(int port) {
        this(port, CommandParser.MAX_INPUT_BYTES);
    }

    /**
     * @param port         the port to listen on, or 0 to let the system pick a free one
     * @param maxLineChars the longest line a client may send; a longer one gets an error and is disconnected
     */
    public Server(int port, int maxLineChars) {
        this.port = port;
        this.maxLineChars = maxLineChars;
    }

    /** Binds the port and starts accepting clients in the background; returns immediately. */
    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        pool.submit(this::acceptLoop);
    }

    /** The port the server is really listening on (useful when it was created with port 0). */
    public int getPort() {
        return serverSocket.getLocalPort();
    }

    /** Stops accepting, disconnects all clients and releases the threads. */
    @Override
    public void close() {
        closed = true;
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException ignored) {
            // closing anyway
        }
        for (Socket client : clients) {
            try {
                client.close();
            } catch (IOException ignored) {
                // closing anyway
            }
        }
        pool.shutdown();
    }

    private void acceptLoop() {
        while (!closed) {
            try {
                Socket clientSocket = serverSocket.accept();
                clients.add(clientSocket);
                pool.submit(() -> handleClient(clientSocket));
            } catch (IOException e) {
                if (!closed) {
                    System.err.println("Server error: " + e.getMessage());
                }
                return;
            }
        }
    }

    private void handleClient(Socket clientSocket) {
        try (
                clientSocket;
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
                PrintWriter out = new PrintWriter(
                        clientSocket.getOutputStream(), false, StandardCharsets.UTF_8)
        ) {
            try {
                String line;
                while ((line = readLimitedLine(in)) != null) {
                    reply(out, processRequest(line));
                }
            } catch (LineTooLongException e) {
                // we are in the middle of a line, so we cannot resync: tell the client and disconnect
                reply(out, "-ERR " + e.getMessage());
            }
        } catch (IOException e) {
            if (!closed) {
                System.out.println("Client disconnected: " + e.getMessage());
            }
        } finally {
            clients.remove(clientSocket);
        }
    }

    private static void reply(PrintWriter out, String response) {
        out.print(response + EOL);
        out.flush();
    }

    /**
     * Like BufferedReader.readLine(), but gives up as soon as the line is longer than
     * {@code maxLineChars}, instead of buffering an unlimited amount of data.
     * Lines end with '\n'; a '\r' before it is left for the parser to strip.
     *
     * @return the line without the '\n', or null at end of stream (an unfinished last line is dropped)
     * @throws LineTooLongException if the line is longer than the allowed maximum
     */
    private String readLimitedLine(BufferedReader in) throws IOException {
        StringBuilder line = new StringBuilder(256);

        int c;
        while ((c = in.read()) != -1) {
            if (c == '\n') {
                return line.toString();
            }
            if (line.length() >= maxLineChars) {
                throw new LineTooLongException(maxLineChars);
            }
            line.append((char) c);
        }

        // End of stream in the middle of a line: the connection broke before the client finished
        // the command, so the unfinished line is dropped instead of being executed.
        return null;
    }

    private String processRequest(String message) {
        try {
            return executor.execute(parser.parse(message));
        } catch (CommandException e) {
            return "-ERR " + e.getMessage();
        } catch (RuntimeException e) {
            System.err.println("Internal error while handling a command: " + e);
            return "-ERR internal error";
        }
    }

    public static void main(String[] args) {
        Server server = new Server(DEFAULT_PORT);
        try {
            server.start();
        } catch (IOException e) {
            System.err.println("Could not start the server on port " + DEFAULT_PORT + ": " + e.getMessage());
            return;
        }
        Runtime.getRuntime().addShutdownHook(new Thread(server::close));
        System.out.println("Redis server is running and listening on port " + server.getPort());
    }
}