import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    public static final int PORT=6380;//to not collide with real Redis
    private static CommandParser parser=new CommandParser();
    private static StorageEngine storageEngine= new StorageEngine();
    private static CommandExecutor executer=new CommandExecutor(storageEngine);

    public static void main(String[] args){
        ExecutorService pool= Executors.newCachedThreadPool();
        try(ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Redis server is running and listening on port " + PORT);
            while (true){
                Socket clientSocket=serverSocket.accept();
                pool.submit(() -> handleClient(clientSocket));
            }
        } catch (IOException e) {
            System.err.print("Server error: "+ e.getMessage());
        }
        finally {
            pool.shutdown();
        }
    }

    private static void handleClient(Socket clientSocket) {
        try (
                clientSocket;
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
                PrintWriter out = new PrintWriter(
                        clientSocket.getOutputStream(), true, StandardCharsets.UTF_8)
        ) {
            String line;
            while ((line = in.readLine()) != null) {
                String response;
                try {
                    response = processRequest(line);
                } catch (CommandException e) {
                    response = "-ERR " + e.getMessage();
                }
                out.println(response);
            }
        } catch (IOException e) {
            System.out.println("Client disconnected: " + e.getMessage());
        }
    }

    private static String processRequest(String message) {
        Command command=parser.parse(message);
        return executer.execute(command);
    }
}


