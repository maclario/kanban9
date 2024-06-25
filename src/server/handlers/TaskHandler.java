package server.handlers;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import exceptions.InvalidReceivedTimeException;
import exceptions.TaskNotFoundException;
import model.Task;
import service.TaskManager;

import java.io.IOException;

public class TaskHandler extends BaseHttpHandler {

    public TaskHandler(TaskManager taskManager, Gson gson) {
        super(taskManager, gson);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String[] pathParts = getPathParts(exchange);
        String endpoint = defineEndpoint(exchange);

        switch (endpoint) {
            case "GET_BY_ID":
                try {
                    Integer id = Integer.parseInt(pathParts[2]);
                    sendText(exchange, gson.toJson(taskManager.getTask(id)));
                } catch (TaskNotFoundException e) {
                    sendNotFound(exchange);
                } catch (Exception e) {
                    sendInternalServerError(exchange);
                }
                break;

            case "GET":
                try {
                    sendText(exchange, gson.toJson(taskManager.getAllTasks()));
                } catch (Exception e) {
                    sendInternalServerError(exchange);
                }
                break;

            case "POST_BY_ID":
                try {
                    Task receivedTask = gson.fromJson(contentFromRequestBody, Task.class);
                    taskManager.updateTask(receivedTask);
                    sendOk(exchange);
                } catch (InvalidReceivedTimeException e) {
                    sendHasInteractions(exchange);
                } catch (Exception e) {
                    sendInternalServerError(exchange);
                }
                break;

            case "POST":
                try {
                    Task receivedTask = gson.fromJson(contentFromRequestBody, Task.class);
                    taskManager.createTask(receivedTask);
                    sendCreated(exchange);
                } catch (InvalidReceivedTimeException e) {
                    sendHasInteractions(exchange);
                } catch (Exception e) {
                    sendInternalServerError(exchange);
                }
                break;

            case "DELETE_BY_ID":
                try {
                    Integer id = Integer.parseInt(pathParts[2]);
                    taskManager.deleteTask(id);
                    sendOk(exchange);
                } catch (TaskNotFoundException e) {
                    sendNotFound(exchange);
                } catch (Exception e) {
                    sendInternalServerError(exchange);
                }
                break;

            default:
                sendBadRequest(exchange);
        }
    }

}
