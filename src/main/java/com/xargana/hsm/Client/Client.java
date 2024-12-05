package com.xargana.hsm;

import java.io.*;
import java.net.*;
import java.util.*;
import com.google.gson.*;

public class Client {
    private static String username;
    private static String serverHost = "127.0.0.1";
    private static final int PORT = 2687;
    private static final int BUFFER_SIZE = 8192;

    public static void main(String[] args) {
        try (
            Socket socket = new Socket(serverHost, PORT);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"), BUFFER_SIZE);
            PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), BUFFER_SIZE), true);
            BufferedReader console = new BufferedReader(new InputStreamReader(System.in))
        ) {
            socket.setTcpNoDelay(true);
            socket.setKeepAlive(true);

            // Handle server's username request
            String initialPrompt = JsonParser.parseString(in.readLine()).getAsJsonObject().get("data").getAsString();
            System.out.println(initialPrompt);
            
            username = console.readLine().trim();
            out.println(username);

            updateConsoleTitle("Connected as " + username);

            // Thread to read server messages
            Thread readerThread = new Thread(() -> {
                try {
                    String serverResponse;
                    while ((serverResponse = in.readLine()) != null) {
                        handleServerResponse(serverResponse);
                    }
                } catch (IOException e) {
                    System.err.println("Connection to server lost: " + e.getMessage());
                    System.exit(1);
                }
            });
            readerThread.setDaemon(true);
            readerThread.start();

            // Main loop to send messages
            String message;
            while (true) {
                message = console.readLine();
                if (message == null || message.equalsIgnoreCase("/quit")) {
                    break;
                }
                if (message.startsWith("/")) {
                    handleCommand(out, message);
                } else {
                    sendJson(out, "message", message);
                    // Clear the console line and print the new message
                    clearConsoleLine();
                    System.out.println("[" + username + "]: " + message);
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
        if (command.startsWith("/dm ") || command.equals("/server")) {
            sendJson(out, "command", command);
            if (command.startsWith("/dm ")) {
                updateConsoleTitle("DM mode with " + command.substring(4).trim());
            } else {
                updateConsoleTitle("Connected to server chat");
            }
        } else {
            System.out.println("Available commands: /dm <username>, /server, /quit");
        }
    }

    private static void handleServerResponse(String response) {
        try {
            JsonObject json = JsonParser.parseString(response).getAsJsonObject();
            String type = json.get("type").getAsString();

            switch (type) {
                case "message" -> {
                    // add dm check later
                    String sender = json.get("from").getAsString();
                    String content = json.get("data").getAsString();
                    System.out.println("[" + sender + "]: " + content);
                }
                case "notification" -> {
                    String notification = json.get("data").getAsString();
                    System.out.println("[Server]: " + notification);
                }
                case "error" -> {
                    String error = json.get("data").getAsString();
                    System.err.println("[Error]: " + error);
                }
                default -> System.out.println("[Unknown]: " + json.get("data").getAsString());
            }
        } catch (Exception e) {
            System.err.println("Error processing server message: " + e.getMessage());
        }
    }

    private static void updateConsoleTitle(String title) {
        System.out.print("\033]0;" + title + "\007");
        System.out.flush();
    }

    private static void clearConsoleLine() {
        System.out.print("\r"); // Move cursor to the start of the line
        System.out.print("                                        "); // Print spaces to clear the line
        System.out.print("\r"); // Move cursor back to the start again
    }
}
