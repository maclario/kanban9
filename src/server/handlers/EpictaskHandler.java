package server.handlers;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import exceptions.InvalidReceivedTimeException;
import service.TaskManager;

import java.io.IOException;
import java.util.NoSuchElementException;

public class EpictaskHandler extends BaseHttpHandler implements HttpHandler {

    public EpictaskHandler(TaskManager taskManager, Gson gson) {
        super(taskManager, gson);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        jtbd = defineJTBD(exchange);

        switch (jtbd) {
            case GET_BY_ID:
                try {
                    sendText(exchange, gson.toJson(taskManager.getEpicTask(id)));
                } catch (NoSuchElementException e) {
                    sendNotFound(exchange);
                } catch (Exception e) {
                    sendInternalServerError(exchange);
                }
                break;

            case GET:
                try {
                    String allEpics = gson.toJson(taskManager.getAllEpicTasks());
                    sendText(exchange, allEpics);
                } catch (Exception e) {
                    sendInternalServerError(exchange);
                }
                break;

            case GET_SUBS_BY_ID:
                try {
                    String subtasksOfEpic = gson.toJson(taskManager.getSubtasksOfEpic(id));
                    sendText(exchange, subtasksOfEpic);
                } catch (NullPointerException e) {
                    sendNotFound(exchange);
                } catch (Exception e) {
                    sendInternalServerError(exchange);
                }
                break;

            case POST:
                try {
                    taskManager.createEpicTask(receivedEpic);
                    sendCreated(exchange);
                } catch (InvalidReceivedTimeException e) {
                    sendHasInteractions(exchange);
                } catch (Exception e) {
                    sendInternalServerError(exchange);
                }
                break;

            case DELETE_BY_ID:
                try {
                    taskManager.deleteEpicTask(id);
                    sendOk(exchange);
                } catch (NullPointerException e) {
                    sendNotFound(exchange);
                } catch (Exception e) {
                    sendInternalServerError(exchange);
                }
                break;

            default:
                sendNotFound(exchange);
        }
    }

}
