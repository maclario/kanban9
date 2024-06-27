package server.handlers;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import exceptions.InvalidReceivedTimeException;
import exceptions.TaskNotFoundException;
import model.Subtask;
import service.TaskManager;

import java.io.IOException;

public class SubtaskHandler extends BaseHttpHandler {

    public SubtaskHandler(TaskManager taskManager, Gson gson) {
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
                    sendText(exchange, gson.toJson(taskManager.getSubtask(id)));
                } catch (TaskNotFoundException e) {
                    sendNotFound(exchange);
                } catch (Exception e) {
                    sendInternalServerError(exchange);
                }
                break;

            case "GET":
                try {
                    sendText(exchange, gson.toJson(taskManager.getAllSubtasks()));
                } catch (Exception e) {
                    sendInternalServerError(exchange);
                }
                break;

            case "POST_BY_ID":
                try {
                    Subtask receivedSubtask = gson.fromJson(contentFromRequestBody, Subtask.class);
                    taskManager.updateSubtask(receivedSubtask);
                    sendOk(exchange);
                } catch (InvalidReceivedTimeException e) {
                    sendHasInteractions(exchange);
                } catch (Exception e) {
                    sendInternalServerError(exchange);
                }
                break;

            case "POST":
                try {
                    Subtask receivedSubtask = gson.fromJson(contentFromRequestBody, Subtask.class);
                    taskManager.createSubtask(receivedSubtask);
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
                    taskManager.deleteSubtask(id);
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
