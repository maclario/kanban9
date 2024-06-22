package server;

import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpServer;
import com.google.gson.Gson;
import server.adapters.DurationAdapter;
import server.adapters.LocalDateTimeAdapter;
import server.handlers.*;
import service.TaskManager;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.time.LocalDateTime;

public class HttpTaskServer {
    private Gson gson;
    private TaskManager taskManager;
    private HttpServer httpServer;
    private static final String HOSTNAME = "localhost";
    private static final int PORT = 8080;

    public HttpTaskServer(TaskManager taskManager) {
        this.taskManager = taskManager;
        gson = new GsonBuilder()
                .registerTypeAdapter(Duration.class, new DurationAdapter())
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                .create();
        try {
            httpServer = HttpServer.create(new InetSocketAddress(HOSTNAME, PORT), 0);
            httpServer.createContext("/tasks", new TaskHandler(taskManager, gson));
            httpServer.createContext("/subtask", new SubtaskHandler(taskManager, gson));
            httpServer.createContext("/epics", new EpictaskHandler(taskManager, gson));
            httpServer.createContext("/history", new HistoryHandler(taskManager, gson));
            httpServer.createContext("/prioritized", new PrioritizedHandler(taskManager, gson));
        } catch (IOException e) {
            throw new RuntimeException("FAIL: Ошибка создания http-сервера на порте " + PORT + ".", e);
        }
    }

    public void start() {
        httpServer.start();
        System.out.println("OK: Сервер на порте " + PORT + " запущен.");
    }

    public void stop() {
        httpServer.stop(0);
        System.out.println("OK: Сервер на порте " + PORT + " остановлен.");
    }

}
