package org.example;

import java.io.IOException;


public class Main {
    public static void main(String[] args) {
        final var server = new Server(64);

        // Добавление обработчиков
        server.addHandler("GET", "/messages", (request, responseStream) -> {
            try {
                String lastParam = request.getQueryParam("last");
                // Обработка GET запроса на /messages
                String responseBody = "GET request to /messages";
                if (lastParam != null) {
                    responseBody += ", last=" + lastParam;
                }
                responseStream.write(responseBody.getBytes());
                responseStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        server.addHandler("POST", "/messages", (request, responseStream) -> {
            try {
                // Обработка POST запроса на /messages
                String responseBody = "POST request to /messages";
                responseStream.write(responseBody.getBytes());
                responseStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        server.listen(9999);
    }
}
