package server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import model.EpicTask;
import model.Subtask;
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

public class HttpHistoryTest {
    String baseUri = "http://localhost:8080/history";
    TaskManager manager;
    HttpTaskServer server;
    HttpClient client;
    HttpRequest request;
    Gson gson;
    URI url;

    public HttpHistoryTest() {
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
    public void correctHistoryListWasReceivedFromServerTest() throws IOException, InterruptedException {
        Task task1 = new Task(null, "task_t1", "task_d1", TaskStatus.NEW,
                Duration.ofMinutes(30), LocalDateTime.of(2024, 6, 20, 17, 0));
        manager.createTask(task1);

        EpicTask epic2 = new EpicTask("epic_t2", "epic_d2");
        manager.createEpicTask(epic2);

        Subtask sub3 = new Subtask(null, "sub_t3", "sub_d3", TaskStatus.NEW,
                Duration.ofMinutes(30), LocalDateTime.of(2024, 6, 22, 17, 0), epic2.getId());
        manager.createSubtask(sub3);

        int task1Id = task1.getId();
        int epic2Id = epic2.getId();
        int sub3Id = sub3.getId();

        manager.getEpicTask(epic2Id);
        manager.getSubtask(sub3Id);
        manager.getTask(task1Id);

        url = URI.create(baseUri);
        request = HttpRequest.newBuilder()
                .uri(url)
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        List<Task> HistoryListFromServer = gson.fromJson(response.body(), new TypeToken<List<Task>>(){}.getType());

        assertEquals(200, response.statusCode());
        assertEquals(3, HistoryListFromServer.size(), "Размер списка не совпадает.");
        assertEquals(epic2Id, HistoryListFromServer.get(0).getId(), "Задача epic2 должна быть первой.");
        assertEquals(sub3Id, HistoryListFromServer.get(1).getId(), "Задача sub3 должна быть второй.");
        assertEquals(task1Id, HistoryListFromServer.get(2).getId(), "Задача task1 должна быть последней.");

    }

}
