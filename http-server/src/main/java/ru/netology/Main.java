package ru.netology;

public class Main {
    public static void main(String[] args) {
        Server server = new Server();
        server.addHandler(Server.GET, "/messages", (request, responseStream) -> {
            server.sendResponseWithoutContent(responseStream, "200 Get");
        });

        server.addHandler(Server.POST, "/messages", (request, responseStream) -> {
            server.sendResponseWithoutContent(responseStream, "200 Post");
        });

        server.run();
    }
}


