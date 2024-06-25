package server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import model.EpicTask;
import model.Subtask;
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

public class HttpEpicsTest {
    String baseUri = "http://localhost:8080/epics";
    TaskManager manager;
    HttpTaskServer server;
    HttpRequest request;
    HttpResponse<String> response;
    HttpClient client;
    Gson gson;
    URI url;

    public HttpEpicsTest() {
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
    public void correctEpicsListWasReceivedFromServerTest() throws IOException, InterruptedException {
        EpicTask epic1 = new EpicTask("epic_t1", "epic_d1");
        EpicTask epic2 = new EpicTask("epic_t2", "epic_d2");
        EpicTask epic3 = new EpicTask("epic_t3", "epic_d3");
        manager.createEpicTask(epic1);
        manager.createEpicTask(epic2);
        manager.createEpicTask(epic3);

        url = URI.create(baseUri);
        request = HttpRequest.newBuilder()
                .uri(url)
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        List<EpicTask> epicsFromManager = manager.getAllEpicTasks();
        List<EpicTask> epicsFromResponse = gson.fromJson(response.body(), new TypeToken<List<EpicTask>>(){}.getType());

        assertEquals(200, response.statusCode());
        assertEquals(epicsFromManager.size(), epicsFromResponse.size(), "Размеры списков не совпадают.");
        assertEquals(epicsFromManager.get(0), epicsFromResponse.get(0), "Задача не совпадает.");
        assertEquals(epicsFromManager.get(1), epicsFromResponse.get(1), "Задача не совпадает.");
        assertEquals(epicsFromManager.get(2), epicsFromResponse.get(2), "Задача не совпадает.");
    }

    @Test
    public void correctEpicWasReceivedFromServerTest() throws IOException, InterruptedException {
        EpicTask epic1 = new EpicTask("epic_t1", "epic_d1");
        manager.createEpicTask(epic1);

        url = URI.create(baseUri + "/" + epic1.getId());
        request = HttpRequest.newBuilder()
                .uri(url)
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        EpicTask epicFromResponse = gson.fromJson(response.body(), EpicTask.class);

        assertEquals(200, response.statusCode());
        assertEquals(epic1, epicFromResponse, "Задачи не совпадают.");
    }

    @Test
    public void shouldReturn404IfEpicNotExistTest() throws IOException, InterruptedException {
        url = URI.create(baseUri + "/88");
        request = HttpRequest.newBuilder()
                .uri(url)
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(404, response.statusCode());
    }

    @Test
    public void epicWasAddedToManagerWhenRequestedWithPostMethodTest() throws IOException, InterruptedException {
        int oldSizeOfEpicsList = manager.getAllEpicTasks().size();
        EpicTask epic1 = new EpicTask("epic_t1", "epic_d1");

        url = URI.create(baseUri);
        String jsonEpic1 = gson.toJson(epic1);
        request = HttpRequest.newBuilder()
                .uri(url)
                .POST(HttpRequest.BodyPublishers.ofString(jsonEpic1))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        epic1.setId(manager.getId());
        List<EpicTask> epicsFromManager = manager.getAllEpicTasks();

        assertEquals(201, response.statusCode());
        assertEquals(oldSizeOfEpicsList  + 1, epicsFromManager.size(), "Количество задач должно было увеличиться на 1.");
        assertEquals(epic1, epicsFromManager.getFirst(), "Задачи не совпадают.");
    }

    @Test
    public void correctSubtasksListOfEpicWasReceivedFromServerTest() throws IOException, InterruptedException {
        EpicTask epic1 = new EpicTask("epic_t1", "epic_d1");
        manager.createEpicTask(epic1);
        int epicId = epic1.getId();

        Subtask sub2 = new Subtask(null, "sub_t2", "sub_d2", TaskStatus.NEW,
                Duration.ofMinutes(30), LocalDateTime.of(2024, 6, 20, 15, 30), epicId);
        Subtask sub3 = new Subtask(null, "sub_t3", "sub_d3", TaskStatus.NEW,
                Duration.ofMinutes(30), LocalDateTime.of(2024, 6, 22, 12, 0), epicId);
        manager.createSubtask(sub2);
        manager.createSubtask(sub3);

        url = URI.create(baseUri + "/" + epicId + "/subtasks");
        request = HttpRequest.newBuilder()
                .uri(url)
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        List<Subtask> subtasksOfEpicFromManager = manager.getSubtasksOfEpic(epicId);
        List<Subtask> subtasksOfEpicFromServer = gson.fromJson(response.body(), new TypeToken<List<Subtask>>(){}.getType());

        assertEquals(200, response.statusCode());
        assertEquals(subtasksOfEpicFromManager.size(), subtasksOfEpicFromServer.size(), "Количество подзадач не совпадает.");
        assertEquals(subtasksOfEpicFromManager.get(0), subtasksOfEpicFromServer.get(0), "Подзадача не совпадает.");
        assertEquals(subtasksOfEpicFromManager.get(1), subtasksOfEpicFromServer.get(1), "Подзадача не совпадает.");
    }

    @Test
    public void epicDeleteTest() throws IOException, InterruptedException {
        EpicTask epic1 = new EpicTask("epic_t1", "epic_d1");
        manager.createEpicTask(epic1);
        int epicId = epic1.getId();
        int oldSizeOfEpicsList = manager.getAllEpicTasks().size();

        url = URI.create(baseUri + "/" + epicId);
        request = HttpRequest.newBuilder()
                .uri(url)
                .DELETE()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        int sizeOfEpicListAfterRequest = manager.getAllEpicTasks().size();

        assertEquals(200, response.statusCode());
        assertEquals(oldSizeOfEpicsList - 1, sizeOfEpicListAfterRequest, "Список эпиков должен быть пуст.");
    }

    @Test
    public void shouldReturn404IfEpicForDeleteDoesNotExist() throws IOException, InterruptedException {
        url = URI.create(baseUri + "/1");
        request = HttpRequest.newBuilder()
                .uri(url)
                .DELETE()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(404, response.statusCode());
    }

}
