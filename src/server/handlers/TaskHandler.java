package server.handlers;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import exceptions.InvalidReceivedTimeException;
import service.TaskManager;

import java.io.IOException;
import java.util.NoSuchElementException;

public class TaskHandler extends BaseHttpHandler implements HttpHandler {

    public TaskHandler(TaskManager taskManager, Gson gson) {
        super(taskManager, gson);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        jtbd = defineJTBD(exchange);

        switch (jtbd) {
            case GET_BY_ID:
                try {
                    sendText(exchange, gson.toJson(taskManager.getTask(id)));
                } catch (NoSuchElementException e) {
                    sendNotFound(exchange);
                } catch (Exception e) {
                    sendInternalServerError(exchange);
                }
                break;

            case GET:
                try {
                    sendText(exchange, gson.toJson(taskManager.getAllTasks()));
                } catch (Exception exception) {
                    sendInternalServerError(exchange);
                }
                break;

            case POST_BY_ID:
                try {
                    taskManager.updateTask(receivedTask);
                    sendOk(exchange);
                } catch (InvalidReceivedTimeException e) {
                    sendHasInteractions(exchange);
                } catch (NullPointerException e) {
                    sendNotFound(exchange);
                } catch (Exception e) {
                    sendInternalServerError(exchange);
                }
                break;

            case POST:
                try {
                    taskManager.createTask(receivedTask);
                    sendCreated(exchange);
                } catch (InvalidReceivedTimeException e) {
                    sendHasInteractions(exchange);
                } catch (Exception e) {
                    sendInternalServerError(exchange);
                }
                break;

            case DELETE_BY_ID:
                try {
                    taskManager.deleteTask(id);
                    sendOk(exchange);
                } catch (NullPointerException e) {
                    sendNotFound(exchange);
                } catch (Exception e) {
                    sendInternalServerError(exchange);
                }
                break;

            default:
                sendMethodNotAllowed(exchange);
        }
    }

}
