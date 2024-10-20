package ru.netology;

import java.net.ServerSocket;
import java.net.Socket;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

public class Server {

    public static final String GET = "GET";
    public static final String POST = "POST";
    public static final String code404 = "404 Not Found";
    public static final String code400 = "400 Bad Request";
    public static final String code500 = "500 Internal Server Error";
    public Map<String, Map<String, MyHandler>> handlers = new ConcurrentHashMap<>();

    public Server() {
    }

    @FunctionalInterface
    public interface MyHandler<T> {
        void handle(Request request, BufferedOutputStream responseStream) throws IOException;
    }

    public void addHandler(String method, String path, MyHandler handler) {
        if (handlers.containsKey(method)) {
            handlers.get(method).put(path, handler);
        } else {
            handlers.put(method, new ConcurrentHashMap<>(Map.of(path, handler)));
        }
    }

    public void run() {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try (final var serverSocket = new ServerSocket(9999)) {
            while (true) {
                final var socket = serverSocket.accept();
                pool.execute(() -> {
                    newConnect(socket);
                });
            }
        } catch (IOException e) {
            e.printStackTrace();
            pool.shutdown();
        }
    }

    public Request parseRequest(BufferedInputStream in) throws IOException, URISyntaxException {
        final var limit = 4096;
        in.mark(limit);
        final var buffer = new byte[limit];
        final var read = in.read(buffer);

        final var requestLineDelimiter = new byte[]{'\r', '\n'};
        // ищем request line

        final var requestLineEnd = indexOf(buffer, requestLineDelimiter, 0, read);
        // читаем request line
        final var requestLine = new String(Arrays.copyOf(buffer, requestLineEnd));
        final var parts = requestLine.split(" ");

        if (parts.length != 3) {
            // just close socket
            return null;
        }

        // ищем заголовки
        final var headersDelimiter = new byte[]{'\r', '\n', '\r', '\n'};
        final var headersStart = requestLineEnd + requestLineDelimiter.length;
        final var headersEnd = indexOf(buffer, headersDelimiter, headersStart, read);
        if (headersEnd == -1) {
            return null;
        }

        // отматываем на начало буфера
        in.reset();
        // пропускаем requestLine
        in.skip(headersStart);

        final var headersBytes = in.readNBytes(headersEnd - headersStart);
        final var headers = Arrays.asList(new String(headersBytes).split("\r\n"));

        String body = "";
        // для GET тела нет
        if (!parts[0].equals(GET)) {
            in.skip(headersDelimiter.length);
            // вычитываем Content-Length, чтобы прочитать body
            final var contentLength = extractHeader(headers, "Content-Length");
            if (contentLength.isPresent()) {
                final var length = Integer.parseInt(contentLength.get());
                final var bodyBytes = in.readNBytes(length);

                body = new String(bodyBytes);
                System.out.println(body);
            }
        }

        return new Request(parts[0], parts[1], headers, body);
    }

    public void newConnect(Socket socket) {
        final var validPaths = List.of("/index.html", "/spring.svg", "/spring.png", "/resources.html", "/styles.css", "/app.js", "/links.html", "/forms.html", "/classic.html", "/events.html", "/events.js");
        try (
                final var in = new BufferedInputStream(socket.getInputStream());
                final var out = new BufferedOutputStream(socket.getOutputStream());
        ) {

            Request request = parseRequest(in);
            if (request == null) {
                sendResponseWithoutContent(out, code400);
                return;
            }

            try {
                var methodMap = handlers.get(request.getMethod());
                if (methodMap == null) {
                    sendResponseWithoutContent(out, code404);
                    return;
                }
                var handler = methodMap.get(request.getPath());
                if (handler == null) {
                    if (request.getMethod().equals(GET)) {
                        if (!validPaths.contains(request.getPath())) {
                            sendResponseWithoutContent(out, code404);
                            return;
                        } else {
                            final var filePath = Path.of(".", "public", request.getPath());
                            final var mimeType = Files.probeContentType(filePath);

                            final var length = Files.size(filePath);
                            out.write((
                                    "HTTP/1.1 200 OK\r\n" +
                                            "Content-Type: " + mimeType + "\r\n" +
                                            "Content-Length: " + length + "\r\n" +
                                            "Connection: close\r\n" +
                                            "\r\n"
                            ).getBytes());
                            Files.copy(filePath, out);
                            out.flush();
                        }
                    } else {
                        sendResponseWithoutContent(out, code404);
                        return;
                    }
                }
                handler.handle(request, out);
            } catch (Exception e) {
                sendResponseWithoutContent(out, code500);
            }
        } catch (Exception e) {
            Thread.currentThread().interrupt();
        }
    }

    void sendResponseWithoutContent(BufferedOutputStream out, String text) throws IOException {
        out.write((
                "HTTP/1.1 " + text + "\r\n" +
                        "Content-Length: 0\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        out.flush();
    }

    private Optional<String> extractHeader(List<String> headers, String header) {
        return headers.stream()
                .filter(o -> o.startsWith(header))
                .map(o -> o.substring(o.indexOf(" ")))
                .map(String::trim)
                .findFirst();
    }

    private static int indexOf(byte[] array, byte[] target, int start, int max) {
        outer:
        for (int i = start; i < max - target.length + 1; i++) {
            for (int j = 0; j < target.length; j++) {
                if (array[i + j] != target[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }
}
