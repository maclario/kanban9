package server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import model.Task;
import model.TaskStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import server.adapters.DurationAdapter;
import server.adapters.LocalDateTimeAdapter;
import service.InMemoryTaskManager;
import service.TaskManager;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class HttpPrioritizedTest {
    String baseUri = "http://localhost:8080/prioritized";
    TaskManager manager;
    HttpTaskServer server;
    HttpClient client;
    HttpRequest request;
    Gson gson;
    URI url;

    public HttpPrioritizedTest() {
        manager = new InMemoryTaskManager();
        server = new HttpTaskServer(manager);
        gson = new GsonBuilder()
                .registerTypeAdapter(Duration.class, new DurationAdapter())
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                .create();
    }

    @BeforeEach
    public void setUpBeforeEach() {
        manager.deleteAllTasks();
        manager.deleteAllSubtasks();
        manager.deleteAllEpicTasks();
        client = HttpClient.newHttpClient();
        server.start();
    }

    @AfterEach
    public void shutDownAfterEach() {
        server.stop();
    }

    @Test
    public void correctPriorTaskListWasReceivedFromServerTest() throws IOException, InterruptedException {
        Task task1 = new Task(1, "task_t1", "task_d1", TaskStatus.NEW,
                Duration.ofMinutes(30), LocalDateTime.of(2024, 6, 3, 12, 0));
        Task task2 = new Task(2, "task_t2", "task_d2", TaskStatus.NEW,
                Duration.ofMinutes(30), LocalDateTime.of(2024, 6, 1, 12, 0));
        Task task3 = new Task(3, "task_t3", "task_d3", TaskStatus.NEW,
                Duration.ofMinutes(30), LocalDateTime.of(2024, 6, 2, 12, 0));

        manager.createTask(task1);
        manager.createTask(task2);
        manager.createTask(task3);

        url = URI.create(baseUri);
        request = HttpRequest.newBuilder()
                .uri(url)
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        List<Task> prioritizedTasksFromServer = gson.fromJson(response.body(), new TypeToken<List<Task>>(){}.getType());

        assertEquals(200, response.statusCode());
        assertEquals(3, prioritizedTasksFromServer.size(), "Размер списка не совпадает.");
        assertEquals(task2, prioritizedTasksFromServer.get(0), "Задача task2 должна быть первой, т.к. имеет dayOfMonth = 1.");
        assertEquals(task3, prioritizedTasksFromServer.get(1), "Задача task3 должна быть второй, т.к. имеет dayOfMonth = 2.");
        assertEquals(task1, prioritizedTasksFromServer.get(2), "Задача task1 должна быть первой, т.к. имеет dayOfMonth = 3.");
    }

}
