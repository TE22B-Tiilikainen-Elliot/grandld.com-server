package org.example;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.io.File;
import java.nio.file.Files;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.BufferedReader;
import java.io.FileReader;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;

public class Main {

    // Create an atomic counter to track button presses
    private static final AtomicInteger buttonClickCounter = new AtomicInteger(0);

    public static void main(String[] args) {
        try {
            // Load the saved click count from the file on server start
            int savedCount = loadCountFromFile();
            buttonClickCounter.set(savedCount);

            // Create an HTTP server that listens on port 8080
            HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
            System.out.println("Web server is listening on port 8080");

            // Create a context for serving the main page (index.html)
            server.createContext("/", new MyHttpHandler());

            // Create a context for handling the button click request
            server.createContext("/button-click", new ButtonClickHandler());

            // Start the server
            server.setExecutor(null); // creates a default executor
            server.start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Handler for serving the main HTML page (index.html)
    static class MyHttpHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            File file = new File("index.html");

            if (!file.exists()) {
                String errorResponse = "404 (Not Found)\n";
                exchange.sendResponseHeaders(404, errorResponse.length());
                OutputStream output = exchange.getResponseBody();
                output.write(errorResponse.getBytes());
                output.close();
                return;
            }

            byte[] response = Files.readAllBytes(file.toPath());
            exchange.getResponseHeaders().set("Content-Type", "text/html");
            exchange.sendResponseHeaders(200, response.length);

            OutputStream output = exchange.getResponseBody();
            output.write(response);
            output.close();
        }
    }

    // Handler for registering and responding to button clicks
    static class ButtonClickHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                // Handle preflight request for CORS
                exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, OPTIONS");
                exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
                exchange.sendResponseHeaders(204, -1); // No Content for OPTIONS requests
                return;
            }

            if ("POST".equals(exchange.getRequestMethod())) {
                // Add CORS header to allow requests from any origin
                exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");

                // Increment the counter and return the new value
                int currentCount = buttonClickCounter.incrementAndGet();
                String response = String.valueOf(currentCount);
                saveCountToFile(currentCount);

                // Send the response with the current count
                exchange.sendResponseHeaders(200, response.length());
                OutputStream output = exchange.getResponseBody();
                output.write(response.getBytes());
                output.close();
            } else {
                exchange.sendResponseHeaders(405, -1); // Method Not Allowed
            }
        }
    }

    // Method to save the count to a text file (overwrite)
    public static void saveCountToFile(int count) {
        String filePath = "count.txt"; // Specify the file path

        // Overwrite the file with the latest count
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, false))) {
            writer.write(String.valueOf(count));
        } catch (IOException e) {
            e.printStackTrace(); // Handle potential IO exceptions
        }
    }

    // Method to load the count from the file on server start
    public static int loadCountFromFile() {
        String filePath = "count.txt";
        File file = new File(filePath);

        if (!file.exists()) {
            return 0; // If the file does not exist, start with a count of 0
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line = reader.readLine();
            return line != null ? Integer.parseInt(line) : 0;
        } catch (IOException | NumberFormatException e) {
            e.printStackTrace(); // Handle any errors
            return 0; // If there's an error, default to 0
        }
    }
}