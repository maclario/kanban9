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

public class HttpSubtasksTest {
    String baseUri = "http://localhost:8080/subtasks";
    EpicTask epic1 = new EpicTask("epic_t1", "epic_d1");
    TaskManager manager;
    HttpTaskServer server;
    HttpRequest request;
    HttpResponse<String> response;
    HttpClient client;
    Gson gson;
    URI url;

    public HttpSubtasksTest() {
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
    public void correctSubtasksListWasReceivedFromServerTest() throws IOException, InterruptedException {
        Subtask sub1 = new Subtask(null, "sub_t1", "sub_d1", TaskStatus.NEW,
                Duration.ofMinutes(30), LocalDateTime.of(2024, 6, 20, 17, 0), 1);
        Subtask sub2 = new Subtask(null, "sub_t2", "sub_d2", TaskStatus.NEW,
                Duration.ofMinutes(30), LocalDateTime.of(2024, 6, 20, 18, 30), 1);
        Subtask sub3 = new Subtask(null, "sub_t3", "sub_d3", TaskStatus.NEW,
                Duration.ofMinutes(30), LocalDateTime.of(2024, 6, 20, 20, 0), 1);
        manager.createEpicTask(epic1);
        manager.createSubtask(sub1);
        manager.createSubtask(sub2);
        manager.createSubtask(sub3);

        url = URI.create(baseUri);
        request = HttpRequest.newBuilder()
                .uri(url)
                .GET()
                .build();

        response = client.send(request, HttpResponse.BodyHandlers.ofString());
        List<Subtask> subtasksFromManager = manager.getAllSubtasks();
        List<Subtask> subtasksFromResponse = gson.fromJson(response.body(), new TypeToken<List<Subtask>>(){}.getType());

        assertEquals(200, response.statusCode());
        assertEquals(subtasksFromManager.size(), subtasksFromResponse.size(), "Размеры списков не совпадают.");
        assertEquals(subtasksFromManager.get(0), subtasksFromResponse.get(0), "Задача не совпадает.");
        assertEquals(subtasksFromManager.get(1), subtasksFromResponse.get(1), "Задача не совпадает.");
        assertEquals(subtasksFromManager.get(2), subtasksFromResponse.get(2), "Задача не совпадает.");
    }

    @Test
    public void correctSubtaskWasReceivedFromServerTest() throws IOException, InterruptedException {
        Subtask sub1 = new Subtask(null, "sub_t1", "sub_d1", TaskStatus.NEW,
                Duration.ofMinutes(30), LocalDateTime.of(2024, 6, 20, 17, 0), 1);
        manager.createEpicTask(epic1);
        manager.createSubtask(sub1);

        url = URI.create(baseUri + "/" + sub1.getId());
        request = HttpRequest.newBuilder()
                .uri(url)
                .GET()
                .build();

        response = client.send(request, HttpResponse.BodyHandlers.ofString());
        Subtask subFromResponse = gson.fromJson(response.body(), Subtask.class);

        assertEquals(200, response.statusCode());
        assertEquals(sub1, subFromResponse, "Задача не совпадает.");
    }

    @Test
    public void shouldReturn404IfSubtaskNotExistTest() throws IOException, InterruptedException {
        url = URI.create(baseUri + "/53");
        request = HttpRequest.newBuilder()
                .uri(url)
                .GET()
                .build();

        response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(404, response.statusCode());
    }

    @Test
    public void subtaskWasAddedToManagerWhenRequestedWithPostMethodTest() throws IOException, InterruptedException {
        int oldSizeOfTasksList = manager.getAllSubtasks().size();
        Subtask sub1 = new Subtask(null, "sub_t1", "sub_d1", TaskStatus.NEW,
                Duration.ofMinutes(30), LocalDateTime.of(2024, 6, 20, 17, 0), 1);
        manager.createEpicTask(epic1);

        url = URI.create(baseUri + "/" + sub1.getId());
        String jsonSub1 = gson.toJson(sub1);
        request = HttpRequest.newBuilder()
                .uri(url)
                .POST(HttpRequest.BodyPublishers.ofString(jsonSub1))
                .build();

        response = client.send(request, HttpResponse.BodyHandlers.ofString());

        sub1.setId(manager.getId());
        List<Subtask> subtasksFromManager = manager.getAllSubtasks();

        assertEquals(201, response.statusCode());
        assertEquals(oldSizeOfTasksList + 1, subtasksFromManager.size(), "Количество задач должно было увеличиться на 1.");
        assertEquals(sub1, subtasksFromManager.getFirst(), "Задачи не совпадают.");
    }

    @Test
    public void shouldReturn406IfNewSubtaskHasTimeIntersectionTest() throws IOException, InterruptedException {
        Task task1 = new Task(null, "task_t1", "task_d1", TaskStatus.NEW,
                Duration.ofMinutes(60), LocalDateTime.of(2024, 6, 20, 17, 0));

        manager.createEpicTask(epic1);
        manager.createTask(task1);

        Subtask sub1 = new Subtask(null, "sub_t1", "sub_d1", TaskStatus.NEW,
                Duration.ofMinutes(30), LocalDateTime.of(2024, 6, 20, 17, 20), epic1.getId());
        String jsonSub1 = gson.toJson(sub1);

        url = URI.create(baseUri);
        request = HttpRequest.newBuilder()
                .uri(url)
                .POST(HttpRequest.BodyPublishers.ofString(jsonSub1))
                .build();

        response = client.send(request, HttpResponse.BodyHandlers.ofString());
        List<Subtask> subtasksFromManager = manager.getAllSubtasks();

        assertEquals(406, response.statusCode());
        assertEquals(0, subtasksFromManager.size(), "Список подзадач должен остаться пуст.");
    }

    @Test
    public void updateSubtaskWhenRequestedWithPostMethodTest() throws IOException, InterruptedException {
        Subtask sub1 = new Subtask(null, "sub_t1", "sub_d1", TaskStatus.NEW,
                Duration.ofMinutes(30), LocalDateTime.of(2024, 6, 20, 17, 0), 1);
        manager.createEpicTask(epic1);
        manager.createSubtask(sub1);

        Subtask sub2 = new Subtask(sub1.getId(), "sub_t2", "sub_d2", TaskStatus.NEW,
                Duration.ofMinutes(30), LocalDateTime.of(2024, 6, 20, 18, 30), epic1.getId());

        url = URI.create(baseUri + "/" + sub1.getId());
        String jsonSub1 = gson.toJson(sub2);
        request = HttpRequest.newBuilder()
                .uri(url)
                .POST(HttpRequest.BodyPublishers.ofString(jsonSub1))
                .build();

        response = client.send(request, HttpResponse.BodyHandlers.ofString());
        Subtask subtaskInManager = manager.getSubtask(sub1.getId());

        assertEquals(200, response.statusCode());
        assertEquals(sub2, subtaskInManager, "Задача не совпадает.");
    }

    @Test
    public void shouldReturn406IfUpdateTaskHasTimeIntersectionTest() throws IOException, InterruptedException {
        Subtask sub1 = new Subtask(null, "sub_t1", "sub_d1", TaskStatus.NEW,
                Duration.ofMinutes(60), LocalDateTime.of(2024, 6, 20, 17, 0), 1);
        manager.createEpicTask(epic1);
        manager.createSubtask(sub1);

        Subtask sub2 = new Subtask(sub1.getId(), "sub_t2", "sub_d2", TaskStatus.NEW,
                Duration.ofMinutes(30), LocalDateTime.of(2024, 6, 20, 17, 30), 1);

        url = URI.create(baseUri + "/" + sub1.getId());
        String jsonSub1 = gson.toJson(sub2);
        request = HttpRequest.newBuilder()
                .uri(url)
                .POST(HttpRequest.BodyPublishers.ofString(jsonSub1))
                .build();

        response = client.send(request, HttpResponse.BodyHandlers.ofString());
        Subtask subtaskInManager = manager.getSubtask(sub1.getId());

        assertEquals(406, response.statusCode());
        assertEquals(sub1, subtaskInManager, "Задача не должна была измениться.");
    }

    @Test
    public void subtaskDeleteTest() throws IOException, InterruptedException {
        Subtask sub1 = new Subtask(null, "sub_t1", "sub_d1", TaskStatus.NEW,
                Duration.ofMinutes(60), LocalDateTime.of(2024, 6, 20, 17, 0), 1);
        manager.createEpicTask(epic1);
        manager.createSubtask(sub1);

        url = URI.create(baseUri + "/" + sub1.getId());
        request = HttpRequest.newBuilder()
                .uri(url)
                .DELETE()
                .build();

        response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertEquals(0, manager.getAllSubtasks().size(), "После удаления задачи список должен стать пустым.");
    }

    @Test
    public void shouldReturn404IfSubtaskForDeleteDoesNotExist() throws IOException, InterruptedException {
        Subtask sub1 = new Subtask(null, "sub_t1", "sub_d1", TaskStatus.NEW,
                Duration.ofMinutes(60), LocalDateTime.of(2024, 6, 20, 17, 0), 1);
        manager.createEpicTask(epic1);
        manager.createSubtask(sub1);

        url = URI.create(baseUri + "/113");
        request = HttpRequest.newBuilder()
                .uri(url)
                .DELETE()
                .build();

        response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(404, response.statusCode());
    }

}
