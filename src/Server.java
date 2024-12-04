import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class Server {
    private static final int PORT = 2678;
    private static final Map<UUID, ClientHandler> clients = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server is listening on port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                clients.put(clientHandler.getClientId(), clientHandler);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            System.err.println("Error starting server: " + e.getMessage());
        }
    }

    static void broadcast(String message, UUID senderId) {
        for (Map.Entry<UUID, ClientHandler> entry : clients.entrySet()) {
            if (!entry.getKey().equals(senderId)) {
                entry.getValue().sendMessage(message);
            }
        }
    }

    static void removeClient(UUID clientId) {
        clients.remove(clientId);
    }
}

class ClientHandler implements Runnable {
    private final Socket socket;
    private final UUID clientId;
    private String username;
    private PrintWriter out;

    public ClientHandler(Socket socket) {
        this.socket = socket;
        this.clientId = UUID.randomUUID();
    }

    public UUID getClientId() {
        return clientId;
    }

    @Override
    public void run() {
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))
        ) {
            out = new PrintWriter(socket.getOutputStream(), true);

            // Get username
            out.println("Enter your username:");
            username = in.readLine();
            System.out.println(username + " (" + clientId + ") has connected.");
            Server.broadcast(username + " has joined the chat!", clientId);

            // Handle messages
            String message;
            while ((message = in.readLine()) != null) {
                System.out.println(username + ": " + message);
                Server.broadcast(username + ": " + message, clientId);
            }
        } catch (IOException e) {
            System.err.println("Connection error: " + e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                System.err.println("Error closing socket: " + e.getMessage());
            }
            System.out.println(username + " (" + clientId + ") has disconnected.");
            Server.broadcast(username + " has left the chat.", clientId);
            Server.removeClient(clientId);
        }
    }

    void sendMessage(String message) {
        out.println(message);
    }
}
