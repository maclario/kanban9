package server.handlers;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import exceptions.InvalidReceivedTimeException;
import exceptions.TaskNotFoundException;
import model.EpicTask;
import service.TaskManager;

import java.io.IOException;

public class EpictaskHandler extends BaseHttpHandler {

    public EpictaskHandler(TaskManager taskManager, Gson gson) {
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
                    sendText(exchange, gson.toJson(taskManager.getEpicTask(id)));
                } catch (TaskNotFoundException e) {
                    sendNotFound(exchange);
                } catch (Exception e) {
                    sendInternalServerError(exchange);
                }
                break;

            case "GET":
                try {
                    String allEpics = gson.toJson(taskManager.getAllEpicTasks());
                    sendText(exchange, allEpics);
                } catch (Exception e) {
                    sendInternalServerError(exchange);
                }
                break;

            case "GET_SUBS_BY_ID":
                try {
                    Integer id = Integer.parseInt(pathParts[2]);
                    String subtasksOfEpic = gson.toJson(taskManager.getSubtasksOfEpic(id));
                    sendText(exchange, subtasksOfEpic);
                } catch (TaskNotFoundException e) {
                    sendNotFound(exchange);
                } catch (Exception e) {
                    sendInternalServerError(exchange);
                }
                break;

            case "POST":
                try {
                    EpicTask receivedEpic = gson.fromJson(contentFromRequestBody, EpicTask.class);
                    taskManager.createEpicTask(receivedEpic);
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
                    taskManager.deleteEpicTask(id);
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
