import com.google.gson.JsonObject;
import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
    private static final int PORT = 2687;
    private static final Map<String, ClientHandler> clients = new HashMap<>();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server started on port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(new ClientHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            System.err.println("Error starting server: " + e.getMessage());
        }
    }

    static class ClientHandler implements Runnable {
        private final Socket socket;
        private String username;
        private String activeRecipient = null; // Tracks the current DM recipient
        private PrintWriter out;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
            ) {
                this.out = out;

                // Request username
                out.println("{\"type\":\"notification\",\"data\":\"Enter your username:\"}");
                this.username = in.readLine().trim();
                synchronized (clients) {
                    clients.put(username, this);
                }
                broadcast(createJson("notification", null, username + " has joined the server."), null);

                // Main loop to handle commands and messages
                String message;
                while ((message = in.readLine()) != null) {
                    JsonObject json = JsonParser.parseString(message).getAsJsonObject();
                    String type = json.get("type").getAsString();

                    if ("command".equals(type)) {
                        handleCommand(json.get("data").getAsString());
                    } else if ("message".equals(type)) {
                        handleMessage(json.get("data").getAsString());
                    }
                }
            } catch (IOException e) {
                System.err.println("Error handling client " + username + ": " + e.getMessage());
            } finally {
                disconnect();
            }
        }

        private void handleCommand(String command) {
            if (command.startsWith("/dm ")) {
                String targetUser = command.substring(4).trim();
                synchronized (clients) {
                    if (clients.containsKey(targetUser)) {
                        activeRecipient = targetUser;
                        out.println(createJson("notification", null, "Now messaging " + targetUser + " privately."));
                    } else {
                        out.println(createJson("error", null, "User " + targetUser + " not found."));
                    }
                }
            } else if (command.equals("/server")) {
                activeRecipient = null;
                out.println(createJson("notification", null, "Returned to server chat."));
            } else {
                out.println(createJson("error", null, "Unknown command: " + command));
            }
        }

        private void handleMessage(String message) {
            if (activeRecipient != null) {
                // Private message
                synchronized (clients) {
                    ClientHandler recipientHandler = clients.get(activeRecipient);
                    if (recipientHandler != null) {
                        recipientHandler.sendMessage(createJson("message", username, message));
                    } else {
                        out.println(createJson("error", null, "User " + activeRecipient + " is no longer available."));
                        activeRecipient = null;
                    }
                }
            } else {
                // Public broadcast
                broadcast(createJson("message", username, message), username);
            }
        }

        private void sendMessage(String jsonMessage) {
            out.println(jsonMessage);
        }

        private void broadcast(String jsonMessage, String excludeUser) {
            synchronized (clients) {
                for (ClientHandler client : clients.values()) {
                    if (!client.username.equals(excludeUser) && client.activeRecipient == null) {
                        client.sendMessage(jsonMessage);
                    }
                }
            }
        }

        private void disconnect() {
            synchronized (clients) {
                clients.remove(username);
            }
            broadcast(createJson("notification", null, username + " has left the server."), null);
            try {
                socket.close();
            } catch (IOException e) {
                System.err.println("Error closing socket for " + username + ": " + e.getMessage());
            }
        }

        private String createJson(String type, String from, String data) {
            JsonObject json = new JsonObject();
            json.addProperty("type", type);
            if (from != null) json.addProperty("from", from);
            json.addProperty("data", data);
            return json.toString();
        }
    }
}
