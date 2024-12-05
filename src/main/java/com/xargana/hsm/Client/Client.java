package com.xargana.hsm;

import java.io.*;
import java.net.*;
import java.util.*;
import com.google.gson.*;

public class Client {
    private static String username;
    private static String serverHost = "127.0.0.1";
    private static final int PORT = 2687;

    public static void main(String[] args) {
        try (Socket socket = new Socket(serverHost, PORT);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader console = new BufferedReader(new InputStreamReader(System.in))) {

            // Ask for username
            System.out.println("Enter your username:");
            username = console.readLine().trim();
            sendJson(out, "connect", username);

            updateConsoleTitle("Connected as " + username);

            // Thread to read server messages
            Thread readerThread = new Thread(() -> {
                try {
                    String serverResponse;
                    while ((serverResponse = in.readLine()) != null) {
                        handleServerResponse(serverResponse);
                    }
                } catch (IOException e) {
                    System.err.println("Error reading from server: " + e.getMessage());
                }
            });
            readerThread.start();

            // Main loop to send messages
            String message;
            while (true) {
                System.out.print("> ");
                message = console.readLine();
                if (message.startsWith("/")) {
                    handleCommand(out, message);
                } else {
                    sendJson(out, "message", message);
                }
            }
        } catch (IOException e) {
            System.err.println("Error connecting to server: " + e.getMessage());
        }
    }

    private static void sendJson(PrintWriter out, String type, String data) {
        JsonObject json = new JsonObject();
        json.addProperty("type", type);
        json.addProperty("data", data);
        out.println(json.toString());
    }

    private static void handleCommand(PrintWriter out, String command) {
        if (command.startsWith("/dm ")) {
            sendJson(out, "command", command);
            updateConsoleTitle("DM mode with " + command.substring(4).trim());
        } else if (command.equals("/server")) {
            sendJson(out, "command", command);
            updateConsoleTitle("Connected to server chat");
        } else {
            System.out.println("Unknown command.");
        }
    }

    private static void handleServerResponse(String response) {
    JsonObject json = JsonParser.parseString(response).getAsJsonObject();
    String type = json.get("type").getAsString();

    switch (type) {
        case "message":
            String sender = json.get("from").getAsString(); // Sender's username
            String content = json.get("data").getAsString(); // Message content
            System.out.println("[" + sender + "]: " + content);
            break;
        case "notification":
            String notification = json.get("data").getAsString();
            System.out.println("[Server]: " + notification);
            break;
        case "error":
            String error = json.get("data").getAsString();
            System.err.println("[Error]: " + error);
            break;
        default:
            System.out.println("[Unknown]: " + json.get("data").getAsString());
    }
}

    private static void updateConsoleTitle(String title) {
        System.out.print("\033]0;" + title + "\007");
    }
}
