import java.io.*;
import java.net.*;

public class Client {
    private static String HOST;
    private static final int PORT = 2678;

    public static void main(String[] args) {
        try (BufferedReader console = new BufferedReader(new InputStreamReader(System.in))) {
            // Ask for server IP
            System.out.print("Enter server IP: ");
            HOST = console.readLine();

            try (Socket socket = new Socket(HOST, PORT);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

                // Ask for and send username
                System.out.println(in.readLine()); // "Enter your username:"
                String username = console.readLine();
                out.println(username);

                // Thread to read messages from server
                Thread readerThread = new Thread(() -> {
                    try {
                        String serverMessage;
                        while ((serverMessage = in.readLine()) != null) {
                            System.out.println(serverMessage);
                        }
                    } catch (IOException e) {
                        System.err.println("Error reading from server: " + e.getMessage());
                    }
                });
                readerThread.start();

                // Main thread to send messages to server
                String message;
                while (true) {
                    message = console.readLine();
                    if ("exit".equalsIgnoreCase(message)) break;
                    out.println(message);
                }

            } catch (IOException e) {
                System.err.println("Error connecting to server: " + e.getMessage());
            }

        } catch (IOException e) {
            System.err.println("Error with user input: " + e.getMessage());
        }
    }
}
