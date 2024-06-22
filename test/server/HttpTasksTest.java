package server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import exceptions.InvalidReceivedTimeException;
import model.Task;
import model.TaskStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import server.adapters.DurationAdapter;
import server.adapters.LocalDateTimeAdapter;
import server.type_tokens.TaskListTypeToken;
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

public class HttpTasksTest {
    String baseUri = "http://localhost:8080/tasks";
    TaskManager manager;
    HttpTaskServer server;
    HttpClient client;
    HttpRequest request;
    Gson gson;
    URI url;

    public HttpTasksTest() {
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
    public void correctTasksListWasReceivedFromServerTest() throws IOException, InvalidReceivedTimeException, InterruptedException {
        Task task1 = new Task(1, "task_t1", "task_d1", TaskStatus.NEW,
                Duration.ofMinutes(30), LocalDateTime.of(2024, 6, 20, 17, 0));
        Task task2 = new Task(2, "task_t2", "task_d2", TaskStatus.NEW,
                Duration.ofMinutes(30), LocalDateTime.of(2024, 6, 20, 18, 30));
        Task task3 = new Task(3, "task_t3", "task_d3", TaskStatus.NEW,
                Duration.ofMinutes(30), LocalDateTime.of(2024, 6, 20, 20, 0));
        manager.createTask(task1);
        manager.createTask(task2);
        manager.createTask(task3);

        url = URI.create(baseUri);
        request = HttpRequest.newBuilder()
                .uri(url)
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        List<Task> tasksFromManager = manager.getAllTasks();
        List<Task> tasksFromResponse = gson.fromJson(response.body(), new TaskListTypeToken().getType());

        assertEquals(200, response.statusCode());
        assertEquals(tasksFromManager.size(), tasksFromResponse.size(), "Размеры списков не совпадают.");
        assertEquals(tasksFromManager.get(0), tasksFromResponse.get(0), "Задача не совпадает.");
        assertEquals(tasksFromManager.get(1), tasksFromResponse.get(1), "Задача не совпадает.");
        assertEquals(tasksFromManager.get(2), tasksFromResponse.get(2), "Задача не совпадает.");
    }

    @Test
    public void correctTaskWasReceivedFromServerTest() throws IOException, InterruptedException {
        Task task2 = new Task(2, "task_t2", "task_d2", TaskStatus.NEW,
                Duration.ofMinutes(30), LocalDateTime.of(2024, 6, 20, 18, 30));
        manager.createTask(task2);

        url = URI.create(baseUri + "/" + task2.getId());
        request = HttpRequest.newBuilder()
                .uri(url)
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        Task taskFromResponse = gson.fromJson(response.body(), Task.class);

        assertEquals(200, response.statusCode());
        assertEquals(task2, taskFromResponse, "Задача не совпадает.");
    }

    @Test
    public void shouldReturn404IfTaskNotExist() throws IOException, InterruptedException {
        url = URI.create(baseUri + "/1");
        request = HttpRequest.newBuilder()
                .uri(url)
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(404, response.statusCode());
    }

    @Test
    public void taskWasAddedToManagerWhenRequestedWithPostMethod() throws IOException, InterruptedException {
        int oldSizeOfTasksList = manager.getAllTasks().size();
        Task task1 = new Task(null, "task_t1", "task_d1", TaskStatus.NEW,
                Duration.ofMinutes(30), LocalDateTime.of(2024, 6, 20, 17, 0));

        url = URI.create(baseUri);
        String jsonTask1 = gson.toJson(task1);
        request = HttpRequest.newBuilder()
                .uri(url)
                .POST(HttpRequest.BodyPublishers.ofString(jsonTask1))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        task1.setId(manager.getId());
        List<Task> tasksFromManager = manager.getAllTasks();

        assertEquals(201, response.statusCode());
        assertEquals(oldSizeOfTasksList + 1, tasksFromManager.size(), "Количество задач должно было увеличиться на 1.");
        assertEquals(task1, tasksFromManager.getFirst(), "Задачи не совпадают.");
    }

    @Test
    public void shouldReturn406IfNewTaskHasTimeIntersection() throws IOException, InterruptedException {
        Task task1 = new Task(1, "task_t1", "task_d1", TaskStatus.NEW,
                Duration.ofMinutes(30), LocalDateTime.of(2024, 6, 20, 17, 0));
        Task task2 = new Task(2, "task_t2", "task_d2", TaskStatus.NEW,
                Duration.ofMinutes(30), LocalDateTime.of(2024, 6, 20, 17, 15));
        manager.createTask(task1);

        url = URI.create(baseUri);
        String jsonTask2 = gson.toJson(task2);
        request = HttpRequest.newBuilder()
                .uri(url)
                .POST(HttpRequest.BodyPublishers.ofString(jsonTask2))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(406, response.statusCode());
    }

    @Test
    public void updateTaskWhenReceivedPostMethodWithIdTest() throws IOException, InterruptedException {
        Task currTask1 = new Task(1, "task_t1", "task_d1", TaskStatus.NEW,
                Duration.ofMinutes(30), LocalDateTime.of(2024, 6, 20, 17, 0));
        Task updatedTask1 = new Task(1, "task_new_t2", "task_new_d2", TaskStatus.IN_PROGRESS,
                Duration.ofMinutes(45), LocalDateTime.of(2024, 6, 20, 20, 0));
        manager.createTask(currTask1);

        url = URI.create(baseUri + "/" + currTask1.getId());
        String jsonTask2 = gson.toJson(updatedTask1);
        request = HttpRequest.newBuilder()
                .uri(url)
                .POST(HttpRequest.BodyPublishers.ofString(jsonTask2))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertEquals(updatedTask1, manager.getTask(1), "Задача не совпадает.");
    }

    @Test
    public void shouldReturn406IfUpdateTaskHasTimeIntersection() throws IOException, InterruptedException {
        Task currTask1 = new Task(1, "task_t1", "task_d1", TaskStatus.NEW,
                Duration.ofMinutes(30), LocalDateTime.of(2024, 6, 20, 17, 0));
        Task currTask2 = new Task(2, "task_t2", "task_d2", TaskStatus.NEW,
                Duration.ofMinutes(60), LocalDateTime.of(2024, 6, 20, 20, 0));
        Task updatedTask1 = new Task(1, "task_new_t2", "task_new_d2", TaskStatus.NEW,
                Duration.ofMinutes(30), LocalDateTime.of(2024, 6, 20, 20, 15));
        manager.createTask(currTask1);
        manager.createTask(currTask2);

        url = URI.create(baseUri + "/" + currTask1.getId());
        String jsonTask2 = gson.toJson(updatedTask1);
        request = HttpRequest.newBuilder()
                .uri(url)
                .POST(HttpRequest.BodyPublishers.ofString(jsonTask2))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(406, response.statusCode());
    }

    @Test
    public void taskDeleteTest() throws IOException, InterruptedException {
        Task task1 = new Task(1, "task_t1", "task_d1", TaskStatus.NEW,
                Duration.ofMinutes(30), LocalDateTime.of(2024, 6, 20, 17, 0));
        manager.createTask(task1);

        url = URI.create(baseUri + "/" + task1.getId());
        request = HttpRequest.newBuilder()
                .uri(url)
                .DELETE()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        assertEquals(0, manager.getAllTasks().size(), "Список задач должен быть пуст.");
    }

    @Test
    public void shouldReturn404IfTaskForDeleteDoesNotExist() throws IOException, InterruptedException {
        Task currTask1 = new Task(1, "task_t1", "task_d1", TaskStatus.NEW,
                Duration.ofMinutes(30), LocalDateTime.of(2024, 6, 20, 17, 0));
        Task currTask2 = new Task(2, "task_t2", "task_d2", TaskStatus.NEW,
                Duration.ofMinutes(60), LocalDateTime.of(2024, 6, 20, 20, 0));
        manager.createTask(currTask1);
        manager.createTask(currTask2);

        url = URI.create(baseUri + "/7");
        request = HttpRequest.newBuilder()
                .uri(url)
                .DELETE()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(404, response.statusCode());
    }

}