package server.handlers;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import service.TaskManager;

import java.io.IOException;

public class PrioritizedHandler extends BaseHttpHandler implements HttpHandler {

    public PrioritizedHandler(TaskManager taskManager, Gson gson) {
        super(taskManager, gson);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String requestMethod = exchange.getRequestMethod();

        if (requestMethod.equals("GET")) {
            try {
                String priorTasks = gson.toJson(taskManager.getPrioritizedTasks());
                sendText(exchange, priorTasks);
            } catch (Exception e) {
                sendInternalServerError(exchange);
            }
        } else {
            sendBadRequest(exchange);
        }
    }

}
