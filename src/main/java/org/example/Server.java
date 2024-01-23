package org.example;

import java.io.*;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {

    private static final int PORT = 9999;
    private static final List<String> VALID_PATHS = List.of("/index.html", "/spring.svg", "/spring.png", "/resources.html", "/styles.css", "/app.js", "/links.html", "/forms.html", "/classic.html", "/events.html", "/events.js");

    private final ExecutorService threadPool;

    public Server(int poolSize) {
        this.threadPool = Executors.newFixedThreadPool(poolSize);
    }

    public void start() {
        try (final var serverSocket = new ServerSocket(PORT)) {
            while (true) {
                final var socket = serverSocket.accept();
                threadPool.execute(() -> handleConnection(socket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleConnection(java.net.Socket socket) {
        try (
                final var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                final var out = new BufferedOutputStream(socket.getOutputStream())
        ) {
            // read only request line for simplicity
            // must be in form GET /path HTTP/1.1
            final var requestLine = in.readLine();
            final var parts = requestLine.split(" ");

            if (parts.length != 3) {
                // just close socket
                return;
            }

            final var path = parts[1];
            if (!VALID_PATHS.contains(path)) {
                sendNotFoundResponse(out);
                return;
            }

            final var filePath = Path.of(".", "public", path);
            final var mimeType = Files.probeContentType(filePath);

            // special case for classic
            if (path.equals("/classic.html")) {
                handleClassicHtmlRequest(filePath, out);
            } else {
                handleStaticFileRequest(filePath, mimeType, out);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendNotFoundResponse(BufferedOutputStream out) throws IOException {
        out.write((
                "HTTP/1.1 404 Not Found\r\n" +
                        "Content-Length: 0\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        out.flush();
    }

    private void handleClassicHtmlRequest(Path filePath, BufferedOutputStream out) throws IOException {
        final var template = Files.readString(filePath);
        final var content = template.replace(
                "{time}",
                LocalDateTime.now().toString()
        ).getBytes();
        sendOkResponseWithContent(200, Files.probeContentType(filePath), content.length, out, content);
    }

    private void handleStaticFileRequest(Path filePath, String mimeType, BufferedOutputStream out) throws IOException {
        final var length = Files.size(filePath);
        sendOkResponseWithFile(200, mimeType, length, out, filePath);
    }

    private void sendOkResponseWithContent(int statusCode, String mimeType, long contentLength, BufferedOutputStream out, byte[] content) throws IOException {
        out.write((
                "HTTP/1.1 " + statusCode + " OK\r\n" +
                        "Content-Type: " + mimeType + "\r\n" +
                        "Content-Length: " + contentLength + "\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        out.write(content);
        out.flush();
    }

    private void sendOkResponseWithFile(int statusCode, String mimeType, long contentLength, BufferedOutputStream out, Path filePath) throws IOException {
        out.write((
                "HTTP/1.1 " + statusCode + " OK\r\n" +
                        "Content-Type: " + mimeType + "\r\n" +
                        "Content-Length: " + contentLength + "\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        Files.copy(filePath, out);
        out.flush();
    }


}
