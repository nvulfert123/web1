package org.example;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private final Map<String, Map<String, Handler>> handlers = new HashMap<>();

    private final int PORT = 9999;
    private final List<String> VALID_PATH = List.of("/index.html", "/spring.svg", "/spring.png", "/resources.html",
            "/styles.css", "/app.js", "/links.html", "/forms.html", "/classic.html", "/events.html", "/events.js");
    private final ExecutorService threadPool;

    public Server(int poolSize) {
        this.threadPool = Executors.newFixedThreadPool(poolSize);
    }

    public void start() {
        try (final var serverSocket = new ServerSocket(PORT)) {
            while (true) {
                final var socket = serverSocket.accept();
                threadPool.execute(() -> handleConnection(socket));
                final var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                final var out = new BufferedOutputStream(socket.getOutputStream());
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void handleConnection(Socket socket) {
        try (final var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             final var out = new BufferedOutputStream(socket.getOutputStream())) {

            // read only request line for simplicity
            // must be in form GET /path HTTP/1.1
            final var requestLine = in.readLine();
            final var parts = requestLine.split(" ");

            if (parts.length != 3) {
                // just close socket
                return;
            }

            final var method = parts[0];
            final var path = parts[1];

            // Retrieve the handler for the method and path
            Handler handler = handlers.getOrDefault(method, Collections.emptyMap()).get(path);
            if (handler != null) {
                // Create a Request object if needed and call handle method of the handler
                Request request = new Request(method, path);
                handler.handle(request, out);
            } else {
                sendNotFoundResponse(out);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void handleConnection1(Socket socket) {
        try (final var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             final var out = new BufferedOutputStream(socket.getOutputStream());) {
            // read only request line for simplicity
            // must be in form GET /path HTTP/1.1
            final var requestLine = in.readLine();
            final var parts = requestLine.split(" ");

            if (parts.length != 3) {
                // just close socket
                return;
            }

            final var path = parts[1];
            if (!VALID_PATH.contains(path)) {
                sendNotFoundResponse(out);
                return;
            }

            final var filePath = Path.of(".", "public", path);
            final var mimeType = Files.probeContentType(filePath);

            if (path.equals("/classic.html")) {
                handleClassicHtmlRequest(filePath, out);
            } else {
                handleClassicFileRequest(filePath, mimeType, out);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }

    private void handleClassicFileRequest(Path filePath, String mimeType, BufferedOutputStream out) throws IOException {
        final var length = Files.size(filePath);
        sendOkRepsonseWithFile(200, mimeType, length, out, filePath);
    }

    private void sendOkRepsonseWithFile(int statusCode, String mimeType, long length, BufferedOutputStream out, Path filePath) throws IOException {
        out.write((
                "HTTP/1.1 " + statusCode + " OK\r\n" +
                        "Content-Type: " + mimeType + "\r\n" +
                        "Content-Length: " + length + "\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        Files.copy(filePath, out);
        out.flush();
    }

    private void handleClassicHtmlRequest(Path filePath, BufferedOutputStream out) throws IOException {
        final var template = Files.readString(filePath);
        final var content = template.replace(
                "{time}",
                LocalDateTime.now().toString()
        ).getBytes();
        sendOkRepsonseWithContent(200, Files.probeContentType(filePath), content.length, out, content);
    }

    private void sendOkRepsonseWithContent(int statusCode, String mimeType, int length, BufferedOutputStream out, byte[] content) throws IOException {
        out.write((
                "HTTP/1.1 " + statusCode + " OK\r\n" +
                        "Content-Type: " + mimeType + "\r\n" +
                        "Content-Length: " + content.length + "\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        out.write(content);
        out.flush();
    }

    public void sendNotFoundResponse(BufferedOutputStream out) throws IOException {
        out.write((
                "HTTP/1.1 404 Not Found\r\n" +
                        "Content-Length: 0\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        out.flush();

    }

    public void addHandler(String method, String path, Handler handler) {
        handlers.computeIfAbsent(method, k -> new HashMap<>()).put(path, handler);
    }


    public void listen(int i) {
    }
}
