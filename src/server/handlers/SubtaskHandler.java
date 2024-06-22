package server.handlers;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import exceptions.InvalidReceivedTimeException;
import service.TaskManager;

import java.io.IOException;
import java.util.NoSuchElementException;

public class SubtaskHandler extends BaseHttpHandler implements HttpHandler {

    public SubtaskHandler(TaskManager taskManager, Gson gson) {
        super(taskManager, gson);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        jtbd = defineJTBD(exchange);

        switch (jtbd) {
            case GET_BY_ID:
                try {
                    sendText(exchange, gson.toJson(taskManager.getSubtask(id)));
                } catch (NoSuchElementException e) {
                    sendNotFound(exchange);
                } catch (Exception e) {
                    sendInternalServerError(exchange);
                }
                break;

            case GET:
                try {
                    sendText(exchange, gson.toJson(taskManager.getAllSubtasks()));
                } catch (Exception e) {
                    sendInternalServerError(exchange);
                }
                break;

            case POST_BY_ID:
                try {
                    taskManager.updateSubtask(receivedSub);
                    sendOk(exchange);
                } catch (InvalidReceivedTimeException e) {
                    sendHasInteractions(exchange);
                } catch (NoSuchElementException e) {
                    sendNotFound(exchange);
                } catch (Exception e) {
                    sendInternalServerError(exchange);
                }
                break;

            case POST:
                try {
                    taskManager.createSubtask(receivedSub);
                    sendCreated(exchange);
                } catch (InvalidReceivedTimeException e) {
                    sendHasInteractions(exchange);
                } catch (Exception e) {
                    sendInternalServerError(exchange);
                }
                break;

            case DELETE_BY_ID:
                try {
                        //System.out.println("Мне пришла Айдишка: " + id);
                        //System.out.println("Список сабов до удаления: " + taskManager.getAllSubtasks());
                        //System.out.println("Хочу удалить задачу: " + taskManager.getSubtask(id));
                    taskManager.deleteSubtask(id);
                        //System.out.println("Результат после удаления: " + taskManager.getAllSubtasks());
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
